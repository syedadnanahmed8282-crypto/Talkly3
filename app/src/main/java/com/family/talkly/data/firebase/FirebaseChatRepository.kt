package com.family.talkly.data.firebase

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.family.talkly.data.models.ChatMessage
import com.family.talkly.data.models.DEFAULT_FAMILY_MEMBERS
import com.family.talkly.data.models.FamilyMember
import com.family.talkly.data.models.MessageType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.family.talkly.data.models.StatusItem
import com.family.talkly.data.models.UserStatusGroup
import com.family.talkly.data.models.StatusViewer
import com.family.talkly.data.models.StatusLiker

class FirebaseChatRepository(private val context: Context) {

    companion object {
        const val TAG = "Talkly_FirebaseChat"
        const val FIREBASE_PROJECT_ID = "familycallapp-e6b21"
        private const val CONTACTS_PREFS = "talkly_saved_contacts_prefs"
        private const val KEY_SAVED_CONTACTS_JSON = "saved_contacts_json"
        private const val KEY_DEMO_CLEARED = "demo_contacts_cleared"
        private const val KEY_STATUSES_JSON = "talkly_statuses_json"
        private const val KEY_BLOCKED_USERS = "talkly_blocked_user_ids"
    }

    private var firestore: FirebaseFirestore? = null
    private var membersListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null
    private var currentSyncedUserId: String? = null
    private val contactPrefs = context.getSharedPreferences(CONTACTS_PREFS, Context.MODE_PRIVATE)

    // Real-time family members presence and status
    private val _familyMembers = MutableStateFlow<List<FamilyMember>>(emptyList())
    val familyMembers: StateFlow<List<FamilyMember>> = _familyMembers.asStateFlow()

    // Blocked Users state
    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    // Time offset for live testing 48-hour expiration logic
    private val _simulatedTimeOffsetMs = MutableStateFlow(0L)
    val simulatedTimeOffsetMs: StateFlow<Long> = _simulatedTimeOffsetMs.asStateFlow()

    // Message maps by family member id
    private val _messagesMap = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messagesMap: StateFlow<Map<String, List<ChatMessage>>> = _messagesMap.asStateFlow()

    // Statuses flow (24-hour disappearing updates)
    private val _statuses = MutableStateFlow<List<StatusItem>>(emptyList())
    val statuses: StateFlow<List<StatusItem>> = _statuses.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
            }
            firestore = FirebaseFirestore.getInstance()
            Log.i(TAG, "Initialized Firebase Firestore for project $FIREBASE_PROJECT_ID")
            setupFirestorePresenceListener()
            setupFirestoreUsersVerificationListener()
            setupFirestoreStatusesListener()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Firestore init fallback mode: ${e.localizedMessage}")
        }
        loadInitialFamilyMembers()
        seedInitialFamilyChats()
        loadStatuses()
        loadBlockedUsers()
    }

    fun loadBlockedUsers() {
        val set = contactPrefs.getStringSet(KEY_BLOCKED_USERS, emptySet()) ?: emptySet()
        _blockedUserIds.value = set
    }

    fun blockUser(userId: String) {
        val updated = _blockedUserIds.value.toMutableSet()
        updated.add(userId)
        _blockedUserIds.value = updated
        contactPrefs.edit().putStringSet(KEY_BLOCKED_USERS, updated).apply()
    }

    fun unblockUser(userId: String) {
        val updated = _blockedUserIds.value.toMutableSet()
        updated.remove(userId)
        _blockedUserIds.value = updated
        contactPrefs.edit().putStringSet(KEY_BLOCKED_USERS, updated).apply()
    }

    fun isUserBlocked(userId: String): Boolean {
        return _blockedUserIds.value.contains(userId)
    }

    private fun loadInitialFamilyMembers() {
        val savedJson = contactPrefs.getString(KEY_SAVED_CONTACTS_JSON, null)
        val demoCleared = contactPrefs.getBoolean(KEY_DEMO_CLEARED, false)

        val list = mutableListOf<FamilyMember>()

        if (!demoCleared) {
            list.addAll(DEFAULT_FAMILY_MEMBERS)
        }

        if (!savedJson.isNullOrBlank()) {
            try {
                val jsonArray = org.json.JSONArray(savedJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val member = FamilyMember(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        relation = obj.optString("relation", "Contact"),
                        avatarUrl = if (obj.has("avatarUrl") && !obj.isNull("avatarUrl")) obj.getString("avatarUrl") else null,
                        status = obj.optString("status", "Available for call 💬"),
                        phone = obj.getString("phone"),
                        isOnline = obj.optBoolean("isOnline", false),
                        isTyping = false,
                        lastSeen = obj.optString("lastSeen", "Recently"),
                        lastActiveTimestamp = obj.optLong("lastActiveTimestamp", 0L),
                        unreadCount = obj.optInt("unreadCount", 0),
                        isPinned = obj.optBoolean("isPinned", false),
                        isRegisteredOnTalkly = obj.optBoolean("isRegisteredOnTalkly", false),
                        firebaseUid = if (obj.has("firebaseUid") && !obj.isNull("firebaseUid")) obj.getString("firebaseUid") else null
                    )
                    // Avoid duplicate IDs
                    if (list.none { it.id == member.id }) {
                        list.add(member)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse saved contacts JSON: ${e.message}")
            }
        }

        _familyMembers.value = list
    }

    private fun saveContactsToPrefs() {
        try {
            val jsonArray = org.json.JSONArray()
            _familyMembers.value.forEach { member ->
                val obj = org.json.JSONObject().apply {
                    put("id", member.id)
                    put("name", member.name)
                    put("relation", member.relation)
                    put("avatarUrl", member.avatarUrl)
                    put("status", member.status)
                    put("phone", member.phone)
                    put("isOnline", member.isOnline)
                    put("lastSeen", member.lastSeen)
                    put("lastActiveTimestamp", member.lastActiveTimestamp)
                    put("unreadCount", member.unreadCount)
                    put("isPinned", member.isPinned)
                    put("isRegisteredOnTalkly", member.isRegisteredOnTalkly)
                    put("firebaseUid", member.firebaseUid)
                }
                jsonArray.put(obj)
            }
            contactPrefs.edit()
                .putString(KEY_SAVED_CONTACTS_JSON, jsonArray.toString())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving contacts to prefs: ${e.message}")
        }
    }

    fun searchTalklyUserByPhone(phone: String, onResult: (com.family.talkly.data.models.UserProfile?) -> Unit) {
        val targetSuffix = com.family.talkly.util.PhoneUtils.extractPhoneSuffix(phone)
        val cleanPhone = com.family.talkly.util.PhoneUtils.cleanPhoneNumber(phone)
        if (targetSuffix.isBlank()) {
            onResult(null)
            return
        }

        try {
            firestore?.collection("users")
                ?.whereEqualTo("phoneSuffix", targetSuffix)
                ?.get()
                ?.addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val doc = snapshot.documents.first()
                        val docUid = doc.id
                        val name = doc.getString("name") ?: "Talkly User"
                        val rawPhone = doc.getString("phoneNumber") ?: phone
                        val docSuffix = doc.getString("phoneSuffix") ?: targetSuffix
                        val pic = doc.getString("profilePicUrl") ?: ""
                        val bio = doc.getString("bio") ?: "Available on Talkly 💬"
                        val profile = com.family.talkly.data.models.UserProfile(
                            uid = docUid,
                            name = name,
                            phoneNumber = rawPhone,
                            phoneSuffix = docSuffix,
                            profilePicUrl = pic,
                            bio = bio
                        )
                        onResult(profile)
                    } else {
                        // Fallback check across all users if phoneSuffix was not yet stored on older user documents
                        firestore?.collection("users")
                            ?.get()
                            ?.addOnSuccessListener { fullSnapshot ->
                                if (fullSnapshot != null && !fullSnapshot.isEmpty) {
                                    for (doc in fullSnapshot.documents) {
                                        val rawUserPhone = doc.getString("phoneNumber") ?: ""
                                        val docSuffix = doc.getString("phoneSuffix") ?: com.family.talkly.util.PhoneUtils.extractPhoneSuffix(rawUserPhone)
                                        val docCleanPhone = com.family.talkly.util.PhoneUtils.cleanPhoneNumber(rawUserPhone)
                                        val docUid = doc.id

                                        if ((targetSuffix.isNotBlank() && docSuffix == targetSuffix) ||
                                            (cleanPhone.isNotBlank() && (docCleanPhone.contains(cleanPhone) || cleanPhone.contains(docCleanPhone))) ||
                                            docUid == cleanPhone
                                        ) {
                                            val name = doc.getString("name") ?: "Talkly User"
                                            val pic = doc.getString("profilePicUrl") ?: ""
                                            val bio = doc.getString("bio") ?: "Available on Talkly 💬"
                                            val profile = com.family.talkly.data.models.UserProfile(
                                                uid = docUid,
                                                name = name,
                                                phoneNumber = if (rawUserPhone.isNotBlank()) rawUserPhone else phone,
                                                phoneSuffix = docSuffix,
                                                profilePicUrl = pic,
                                                bio = bio
                                            )
                                            onResult(profile)
                                            return@addOnSuccessListener
                                        }
                                    }
                                }
                                onResult(null)
                            }
                            ?.addOnFailureListener { onResult(null) }
                    }
                }
                ?.addOnFailureListener {
                    onResult(null)
                } ?: onResult(null)
        } catch (e: Exception) {
            Log.w(TAG, "Search user exception: ${e.localizedMessage}")
            onResult(null)
        }
    }

    fun addNewContact(
        name: String,
        phone: String,
        relation: String = "Family Member",
        bio: String = "Available for call 💬",
        avatarUrl: String? = null,
        onComplete: ((FamilyMember) -> Unit)? = null
    ) {
        val cleanPhone = phone.trim()
        val phoneSuffix = com.family.talkly.util.PhoneUtils.extractPhoneSuffix(cleanPhone)
        val customId = "contact_${phoneSuffix.ifBlank { cleanPhone.replace("+", "").replace(" ", "") }}"

        val newMember = FamilyMember(
            id = customId,
            name = name.trim(),
            relation = relation.ifBlank { "Family Member" },
            avatarUrl = avatarUrl,
            status = bio.ifBlank { "Available on Talkly 💬" },
            phone = cleanPhone,
            isOnline = false,
            isTyping = false,
            lastSeen = "Recently",
            unreadCount = 0,
            isPinned = false
        )

        val currentList = _familyMembers.value.toMutableList()
        // Remove existing if duplicate by ID or suffix
        currentList.removeAll { 
            it.id == customId || 
            it.phone == cleanPhone ||
            (phoneSuffix.isNotBlank() && com.family.talkly.util.PhoneUtils.extractPhoneSuffix(it.phone) == phoneSuffix)
        }
        currentList.add(0, newMember) // Put at top
        _familyMembers.value = currentList

        saveContactsToPrefs()

        // Sync to Firestore 'family_members'
        try {
            firestore?.collection("family_members")
                ?.document(customId)
                ?.set(
                    mapOf(
                        "id" to customId,
                        "name" to name,
                        "relation" to relation,
                        "phone" to cleanPhone,
                        "phoneSuffix" to phoneSuffix,
                        "status" to bio,
                        "avatarUrl" to avatarUrl,
                        "isOnline" to true
                    )
                )
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync contact failed: ${e.localizedMessage}")
        }

        onComplete?.invoke(newMember)
    }

    fun deleteContact(memberId: String) {
        val updatedList = _familyMembers.value.filter { it.id != memberId }
        _familyMembers.value = updatedList
        saveContactsToPrefs()

        try {
            firestore?.collection("family_members")?.document(memberId)?.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting contact from Firestore: ${e.message}")
        }
    }

    fun clearDemoContacts() {
        contactPrefs.edit().putBoolean(KEY_DEMO_CLEARED, true).apply()
        val demoIds = setOf("mom", "dad", "grandma", "brother", "sister")
        val filteredList = _familyMembers.value.filter { it.id !in demoIds }
        _familyMembers.value = filteredList
        saveContactsToPrefs()
    }

    private fun setupFirestorePresenceListener() {
        try {
            membersListener = firestore?.collection("family_members")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for family_members: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val updatedMembers = _familyMembers.value.map { member ->
                            val doc = snapshot.documents.firstOrNull { it.id == member.id }
                            if (doc != null) {
                                val online = doc.getBoolean("isOnline") ?: false
                                val typing = doc.getBoolean("isTyping") ?: false
                                val seen = doc.getString("lastSeen") ?: member.lastSeen
                                val lastActiveTs = doc.getLong("lastActiveTimestamp") ?: member.lastActiveTimestamp

                                val now = System.currentTimeMillis()
                                val isRecent = lastActiveTs > 0L && (now - lastActiveTs) <= (3 * 60 * 1000L)
                                val effectiveOnline = online && isRecent

                                member.copy(
                                    isOnline = effectiveOnline,
                                    isTyping = if (!effectiveOnline) false else typing,
                                    lastSeen = seen,
                                    lastActiveTimestamp = lastActiveTs
                                )
                            } else {
                                member
                            }
                        }
                        _familyMembers.value = updatedMembers
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set up Firestore snapshot listener: ${e.localizedMessage}")
        }
    }

    fun setMemberTyping(memberId: String, isTyping: Boolean) {
        val currentList = _familyMembers.value.map { member ->
            if (member.id == memberId) {
                member.copy(isTyping = isTyping)
            } else {
                member
            }
        }
        _familyMembers.value = currentList

        try {
            firestore?.collection("family_members")
                ?.document(memberId)
                ?.set(mapOf("isTyping" to isTyping, "isOnline" to true), com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Firestore setTyping error: ${e.localizedMessage}")
        }
    }

    fun setMemberPresence(memberId: String, isOnline: Boolean, lastSeen: String = if (isOnline) "Online" else "Just now") {
        val now = System.currentTimeMillis()
        val formattedSeen = if (isOnline) "Online" else com.family.talkly.util.TimeUtils.formatLastSeenTime(now, lastSeen)
        val currentList = _familyMembers.value.map { member ->
            if (member.id == memberId) {
                member.copy(
                    isOnline = isOnline,
                    lastSeen = formattedSeen,
                    lastActiveTimestamp = now,
                    isTyping = if (!isOnline) false else member.isTyping
                )
            } else {
                member
            }
        }
        _familyMembers.value = currentList
        saveContactsToPrefs()

        try {
            firestore?.collection("family_members")
                ?.document(memberId)
                ?.set(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastSeen" to formattedSeen,
                        "lastActiveTimestamp" to now,
                        "isTyping" to if (!isOnline) false else false
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
        } catch (e: Exception) {
            Log.w(TAG, "Firestore setPresence error: ${e.localizedMessage}")
        }
    }

    fun updateCurrentUserPresence(uid: String?, isOnline: Boolean) {
        if (uid.isNullOrBlank()) return
        val now = System.currentTimeMillis()
        val formattedSeen = if (isOnline) "Online" else com.family.talkly.util.TimeUtils.formatLastSeenTime(now)

        val updatedMembers = _familyMembers.value.map { m ->
            if (m.id == uid || m.firebaseUid == uid || m.phone == uid) {
                m.copy(
                    isOnline = isOnline,
                    lastSeen = formattedSeen,
                    lastActiveTimestamp = now,
                    isTyping = if (!isOnline) false else m.isTyping
                )
            } else {
                m
            }
        }
        _familyMembers.value = updatedMembers
        saveContactsToPrefs()

        try {
            val presenceData = mapOf(
                "isOnline" to isOnline,
                "lastSeen" to formattedSeen,
                "lastActiveTimestamp" to now,
                "isTyping" to false
            )
            firestore?.collection("users")?.document(uid)?.set(presenceData, com.google.firebase.firestore.SetOptions.merge())
            firestore?.collection("family_members")?.document(uid)?.set(presenceData, com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Error updating user presence in Firestore: ${e.localizedMessage}")
        }
    }

    fun toggleMemberPresence(memberId: String) {
        val member = _familyMembers.value.firstOrNull { it.id == memberId } ?: return
        val newOnline = !member.isOnline
        setMemberPresence(memberId, newOnline, if (newOnline) "Online" else "Today at 10:15 AM")
    }

    private var usersListener: ListenerRegistration? = null

    private fun setupFirestoreUsersVerificationListener() {
        try {
            usersListener = firestore?.collection("users")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for users collection: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val registeredDocsBySuffix = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()
                        val registeredDocsByFullPhone = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()

                        for (doc in snapshot.documents) {
                            val rawPhone = doc.getString("phoneNumber") ?: ""
                            val suffix = doc.getString("phoneSuffix") ?: com.family.talkly.util.PhoneUtils.extractPhoneSuffix(rawPhone)
                            val cleanPhone = com.family.talkly.util.PhoneUtils.cleanPhoneNumber(rawPhone)

                            if (suffix.isNotBlank()) {
                                registeredDocsBySuffix[suffix] = doc
                            }
                            if (cleanPhone.isNotBlank()) {
                                registeredDocsByFullPhone[cleanPhone] = doc
                            }
                            registeredDocsByFullPhone[doc.id] = doc
                        }

                        val updatedMembers = _familyMembers.value.map { member ->
                            val memberSuffix = com.family.talkly.util.PhoneUtils.extractPhoneSuffix(member.phone)
                            val cleanMemberPhone = com.family.talkly.util.PhoneUtils.cleanPhoneNumber(member.phone)

                            var matchedDoc = registeredDocsBySuffix[memberSuffix]
                                ?: registeredDocsByFullPhone[cleanMemberPhone]
                                ?: registeredDocsByFullPhone[member.id]

                            if (matchedDoc == null && memberSuffix.length >= 6) {
                                for (doc in snapshot.documents) {
                                    val docPhone = doc.getString("phoneNumber") ?: ""
                                    val docDigits = docPhone.filter { it.isDigit() }
                                    val docSuffix = doc.getString("phoneSuffix") ?: if (docDigits.length >= 10) docDigits.takeLast(10) else docDigits
                                    val docLast10 = if (docDigits.length >= 10) docDigits.takeLast(10) else docDigits

                                    if ((docSuffix.isNotBlank() && docSuffix == memberSuffix) ||
                                        (docLast10.isNotBlank() && docLast10 == memberSuffix) ||
                                        (memberSuffix.isNotBlank() && (docLast10.endsWith(memberSuffix) || memberSuffix.endsWith(docLast10)))
                                    ) {
                                        matchedDoc = doc
                                        break
                                    }
                                }
                            }

                            val isMatch = (matchedDoc != null)
                            Log.d("ContactSync", "Checking contact: ${member.phone} | Suffix: $memberSuffix | Found Match: $isMatch")

                            if (matchedDoc != null) {
                                val uid = matchedDoc.id
                                val bio = matchedDoc.getString("bio") ?: member.status
                                val pic = matchedDoc.getString("profilePicUrl") ?: member.avatarUrl
                                val realName = matchedDoc.getString("name") ?: member.name
                                val online = matchedDoc.getBoolean("isOnline") ?: false
                                val lastActiveTs = matchedDoc.getLong("lastActiveTimestamp")
                                    ?: matchedDoc.getLong("lastSeenTimestamp")
                                    ?: member.lastActiveTimestamp
                                val seen = matchedDoc.getString("lastSeen") ?: member.lastSeen

                                val now = System.currentTimeMillis()
                                val isRecent = lastActiveTs > 0L && (now - lastActiveTs) <= (3 * 60 * 1000L)
                                val effectiveOnline = online && isRecent

                                member.copy(
                                    name = if (realName.isNotBlank()) realName else member.name,
                                    isRegisteredOnTalkly = true,
                                    firebaseUid = uid,
                                    avatarUrl = if (!pic.isNullOrBlank()) pic else member.avatarUrl,
                                    status = if (bio.isBlank()) "Available on Talkly 💬" else bio,
                                    isOnline = effectiveOnline,
                                    lastActiveTimestamp = lastActiveTs,
                                    lastSeen = seen
                                )
                            } else {
                                member.copy(
                                    isRegisteredOnTalkly = false,
                                    firebaseUid = null,
                                    status = "User not registered on Talkly"
                                )
                            }
                        }
                        _familyMembers.value = updatedMembers
                        saveContactsToPrefs()
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set up users verification listener: ${e.localizedMessage}")
        }
    }

    private fun String?.isNull_or_empty_str(s: String?): Boolean = s == null || s.isEmpty()

    fun deleteChatHistory(memberId: String) {
        val updatedMap = _messagesMap.value.toMutableMap()
        updatedMap.remove(memberId)
        _messagesMap.value = updatedMap

        try {
            firestore?.collection("family_chats")
                ?.document(memberId)
                ?.collection("messages")
                ?.get()
                ?.addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        doc.reference.delete()
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing chat history in Firestore: ${e.localizedMessage}")
        }
    }

    fun triggerSimulatedTypingReply(memberId: String) {
        // Disabled per requirements: No automated mock replies, bot responses, or local fallback test logic
    }

    private fun seedInitialFamilyChats() {
        // Disabled per requirements: No fake mock chats seeded locally
        _messagesMap.value = emptyMap()
    }

    fun getMessagesForMember(memberId: String): List<ChatMessage> {
        return _messagesMap.value[memberId] ?: emptyList()
    }

    private fun isUserSelf(senderId: String): Boolean {
        val myUid = currentSyncedUserId
        if (senderId == "self") return true
        if (!myUid.isNullOrBlank() && (senderId == myUid || senderId == "contact_$myUid")) return true
        return false
    }

    fun markMessagesAsRead(memberId: String) {
        val currentMessages = _messagesMap.value[memberId] ?: return
        var updatedAny = false

        val targetUid = _familyMembers.value.firstOrNull { it.id == memberId }?.firebaseUid ?: memberId
        val senderUid = currentSyncedUserId ?: "self"

        val updatedMessages = currentMessages.map { msg ->
            if (!isUserSelf(msg.senderId) && !msg.isRead) {
                updatedAny = true
                val now = System.currentTimeMillis()
                val readMsg = msg.copy(isRead = true, readAtTimestamp = now, isDelivered = true)

                // Sync read status to Firestore
                try {
                    if (!senderUid.isNullOrBlank() && senderUid != "self") {
                        firestore?.collection("family_chats")
                            ?.document(senderUid)
                            ?.collection("messages")
                            ?.document(msg.id)
                            ?.update(
                                mapOf(
                                    "isRead" to true,
                                    "readAtTimestamp" to now,
                                    "isDelivered" to true
                                )
                            )
                    }

                    if (targetUid.isNotBlank() && targetUid != senderUid) {
                        firestore?.collection("family_chats")
                            ?.document(targetUid)
                            ?.collection("messages")
                            ?.document(msg.id)
                            ?.update(
                                mapOf(
                                    "isRead" to true,
                                    "readAtTimestamp" to now,
                                    "isDelivered" to true
                                )
                            )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating read receipt in Firestore: ${e.localizedMessage}")
                }

                readMsg
            } else {
                msg
            }
        }

        if (updatedAny) {
            val updatedMap = _messagesMap.value.toMutableMap()
            updatedMap[memberId] = updatedMessages
            _messagesMap.value = updatedMap
        }

        // Reset unread count for member in list and Firestore
        val member = _familyMembers.value.firstOrNull { it.id == memberId }
        if (member != null && member.unreadCount > 0) {
            val updatedMembers = _familyMembers.value.map { m ->
                if (m.id == memberId) m.copy(unreadCount = 0) else m
            }
            _familyMembers.value = updatedMembers

            try {
                firestore?.collection("family_members")
                    ?.document(memberId)
                    ?.update("unreadCount", 0)
            } catch (e: Exception) {
                Log.w(TAG, "Error resetting unread count in Firestore: ${e.localizedMessage}")
            }
        }
    }

    fun toggleMessageReaction(memberId: String, messageId: String, reactionEmoji: String) {
        val currentMessages = _messagesMap.value[memberId] ?: return
        val updatedMessages = currentMessages.map { msg ->
            if (msg.id == messageId) {
                val newReaction = if (msg.reaction == reactionEmoji) null else reactionEmoji
                val updatedMsg = msg.copy(reaction = newReaction)
                
                try {
                    firestore?.collection("family_chats")
                        ?.document(memberId)
                        ?.collection("messages")
                        ?.document(messageId)
                        ?.update("reaction", newReaction)
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating reaction in Firestore: ${e.localizedMessage}")
                }
                
                updatedMsg
            } else {
                msg
            }
        }
        
        val updatedMap = _messagesMap.value.toMutableMap()
        updatedMap[memberId] = updatedMessages
        _messagesMap.value = updatedMap
    }

    fun toggleStarMessage(memberId: String, messageId: String) {
        val currentMessages = _messagesMap.value[memberId] ?: return
        val updatedMessages = currentMessages.map { msg ->
            if (msg.id == messageId) {
                val newStarred = !msg.isStarred
                val updatedMsg = msg.copy(isStarred = newStarred)
                try {
                    firestore?.collection("family_chats")
                        ?.document(memberId)
                        ?.collection("messages")
                        ?.document(messageId)
                        ?.update("isStarred", newStarred)
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating star in Firestore: ${e.localizedMessage}")
                }
                updatedMsg
            } else {
                msg
            }
        }
        val updatedMap = _messagesMap.value.toMutableMap()
        updatedMap[memberId] = updatedMessages
        _messagesMap.value = updatedMap
    }

    fun togglePinMessage(memberId: String, messageId: String) {
        val currentMessages = _messagesMap.value[memberId] ?: return
        val updatedMessages = currentMessages.map { msg ->
            if (msg.id == messageId) {
                val newPinned = !msg.isPinned
                val updatedMsg = msg.copy(isPinned = newPinned)
                try {
                    firestore?.collection("family_chats")
                        ?.document(memberId)
                        ?.collection("messages")
                        ?.document(messageId)
                        ?.update("isPinned", newPinned)
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating pin message in Firestore: ${e.localizedMessage}")
                }
                updatedMsg
            } else {
                msg
            }
        }
        val updatedMap = _messagesMap.value.toMutableMap()
        updatedMap[memberId] = updatedMessages
        _messagesMap.value = updatedMap
    }

    fun togglePinMember(memberId: String) {
        val updatedMembers = _familyMembers.value.map { member ->
            if (member.id == memberId) {
                val newPinned = !member.isPinned
                try {
                    firestore?.collection("family_members")
                        ?.document(memberId)
                        ?.update("isPinned", newPinned)
                } catch (e: Exception) {
                    Log.w(TAG, "Error pinning member in Firestore: ${e.localizedMessage}")
                }
                member.copy(isPinned = newPinned)
            } else {
                member
            }
        }
        _familyMembers.value = updatedMembers
    }

    fun startRealtimeMessageSync(currentUserId: String?) {
        if (currentUserId.isNullOrBlank()) return
        if (currentSyncedUserId == currentUserId && messagesListener != null) return

        messagesListener?.remove()
        currentSyncedUserId = currentUserId

        try {
            messagesListener = firestore?.collection("family_chats")
                ?.document(currentUserId)
                ?.collection("messages")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for messages: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val currentMap = _messagesMap.value.toMutableMap()

                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val senderId = doc.getString("senderId") ?: ""
                                val senderName = doc.getString("senderName") ?: "Talkly User"
                                val receiverId = doc.getString("receiverId") ?: ""
                                val textContent = doc.getString("textContent") ?: ""
                                val mediaUrl = doc.getString("mediaUrl")
                                val typeStr = doc.getString("messageType") ?: "TEXT"
                                val type = try { MessageType.valueOf(typeStr) } catch (e: Exception) { MessageType.TEXT }
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val isRead = doc.getBoolean("isRead") ?: false
                                val isDelivered = doc.getBoolean("isDelivered") ?: false
                                val readAtTimestamp = doc.getLong("readAtTimestamp")
                                val isStarred = doc.getBoolean("isStarred") ?: false
                                val isPinned = doc.getBoolean("isPinned") ?: false

                                val message = ChatMessage(
                                    id = id,
                                    senderId = senderId,
                                    senderName = senderName,
                                    receiverId = receiverId,
                                    messageType = type,
                                    textContent = textContent,
                                    mediaUrl = mediaUrl,
                                    timestamp = timestamp,
                                    isDelivered = isDelivered,
                                    isRead = isRead,
                                    readAtTimestamp = readAtTimestamp,
                                    isStarred = isStarred,
                                    isPinned = isPinned
                                )

                                val isFromSelf = isUserSelf(senderId) || senderId == currentUserId

                                if (!isFromSelf && !isDelivered) {
                                    try {
                                        firestore?.collection("family_chats")
                                            ?.document(currentUserId)
                                            ?.collection("messages")
                                            ?.document(id)
                                            ?.update("isDelivered", true)

                                        if (senderId.isNotBlank() && senderId != "self" && senderId != currentUserId) {
                                            firestore?.collection("family_chats")
                                                ?.document(senderId)
                                                ?.collection("messages")
                                                ?.document(id)
                                                ?.update("isDelivered", true)
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Error updating delivery status in Firestore: ${e.localizedMessage}")
                                    }
                                }

                                val rawOtherId = if (isFromSelf) receiverId else senderId
                                if (rawOtherId.isBlank()) continue

                                // Map rawOtherId to matching FamilyMember.id so sent/received messages share the exact same key
                                val matchedMember = _familyMembers.value.firstOrNull { member ->
                                    member.id == rawOtherId ||
                                    (!member.firebaseUid.isNullOrBlank() && member.firebaseUid == rawOtherId) ||
                                    (member.phone.isNotBlank() && (member.phone == rawOtherId || com.family.talkly.util.PhoneUtils.extractPhoneSuffix(member.phone) == com.family.talkly.util.PhoneUtils.extractPhoneSuffix(rawOtherId)))
                                }
                                val otherPartyId = matchedMember?.id ?: rawOtherId

                                ensureContactInChatList(otherPartyId)

                                val existingMsgs = (currentMap[otherPartyId] ?: emptyList()).toMutableList()
                                val existingIndex = existingMsgs.indexOfFirst { it.id == id }
                                if (existingIndex >= 0) {
                                    existingMsgs[existingIndex] = message
                                } else {
                                    existingMsgs.add(message)
                                }
                                existingMsgs.sortBy { it.timestamp }
                                currentMap[otherPartyId] = existingMsgs

                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing chat message doc ${doc.id}: ${e.message}")
                            }
                        }

                        _messagesMap.value = currentMap
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error starting realtime message sync: ${e.localizedMessage}")
        }
    }

    fun ensureContactInChatList(memberOrUidOrPhone: String) {
        if (memberOrUidOrPhone.isBlank()) return
        val suffix = com.family.talkly.util.PhoneUtils.extractPhoneSuffix(memberOrUidOrPhone)
        val existing = _familyMembers.value.firstOrNull { member ->
            member.id == memberOrUidOrPhone ||
            member.firebaseUid == memberOrUidOrPhone ||
            member.phone == memberOrUidOrPhone ||
            (suffix.isNotBlank() && com.family.talkly.util.PhoneUtils.extractPhoneSuffix(member.phone) == suffix)
        }

        if (existing == null) {
            firestore?.collection("users")
                ?.document(memberOrUidOrPhone)
                ?.get()
                ?.addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val uid = doc.id
                        val name = doc.getString("name") ?: "Talkly User"
                        val phone = doc.getString("phoneNumber") ?: memberOrUidOrPhone
                        val pic = doc.getString("profilePicUrl")
                        val bio = doc.getString("bio") ?: "Available on Talkly 💬"

                        val newMember = FamilyMember(
                            id = uid,
                            name = name,
                            relation = "Contact",
                            avatarUrl = pic,
                            status = bio,
                            phone = phone,
                            isOnline = true,
                            isRegisteredOnTalkly = true,
                            firebaseUid = uid
                        )

                        val currentList = _familyMembers.value.toMutableList()
                        if (currentList.none { it.id == uid || it.firebaseUid == uid }) {
                            currentList.add(0, newMember)
                            _familyMembers.value = currentList
                            saveContactsToPrefs()
                        }
                    } else {
                        val fallbackMember = FamilyMember(
                            id = memberOrUidOrPhone,
                            name = if (memberOrUidOrPhone.startsWith("+") || memberOrUidOrPhone.all { it.isDigit() }) memberOrUidOrPhone else "Talkly User",
                            relation = "Contact",
                            phone = memberOrUidOrPhone,
                            isOnline = true,
                            isRegisteredOnTalkly = true,
                            firebaseUid = if (!memberOrUidOrPhone.startsWith("contact_") && !memberOrUidOrPhone.contains(" ")) memberOrUidOrPhone else null
                        )
                        val currentList = _familyMembers.value.toMutableList()
                        if (currentList.none { it.id == memberOrUidOrPhone }) {
                            currentList.add(0, fallbackMember)
                            _familyMembers.value = currentList
                            saveContactsToPrefs()
                        }
                    }
                }
                ?.addOnFailureListener {
                    val fallbackMember = FamilyMember(
                        id = memberOrUidOrPhone,
                        name = memberOrUidOrPhone,
                        relation = "Contact",
                        phone = memberOrUidOrPhone,
                        isOnline = true,
                        isRegisteredOnTalkly = true
                    )
                    val currentList = _familyMembers.value.toMutableList()
                    if (currentList.none { it.id == memberOrUidOrPhone }) {
                        currentList.add(0, fallbackMember)
                        _familyMembers.value = currentList
                        saveContactsToPrefs()
                    }
                }
        }
    }

    fun sendMessage(
        memberId: String,
        textContent: String,
        type: MessageType = MessageType.TEXT,
        mediaUrl: String? = null,
        forcedTimestamp: Long = System.currentTimeMillis(),
        replyToMessageId: String? = null,
        replyToSenderName: String? = null,
        replyToText: String? = null
    ) {
        ensureContactInChatList(memberId)

        val senderUid = currentSyncedUserId ?: "self"
        val newMessage = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderId = senderUid,
            senderName = "You",
            receiverId = memberId,
            messageType = type,
            textContent = textContent,
            mediaUrl = mediaUrl,
            timestamp = forcedTimestamp,
            replyToMessageId = replyToMessageId,
            replyToSenderName = replyToSenderName,
            replyToText = replyToText
        )

        val currentList = (_messagesMap.value[memberId] ?: emptyList()).toMutableList()
        currentList.add(newMessage)

        val updatedMap = _messagesMap.value.toMutableMap()
        updatedMap[memberId] = currentList
        _messagesMap.value = updatedMap

        // Sync to Firebase Firestore for both receiver and sender
        val targetUid = _familyMembers.value.firstOrNull { it.id == memberId }?.firebaseUid ?: memberId
        try {
            // Write to Receiver's collection
            firestore?.collection("family_chats")
                ?.document(targetUid)
                ?.collection("messages")
                ?.document(newMessage.id)
                ?.set(newMessage)

            // Write to Sender's collection
            if (!senderUid.isNullOrBlank() && senderUid != "self") {
                firestore?.collection("family_chats")
                    ?.document(senderUid)
                    ?.collection("messages")
                    ?.document(newMessage.id)
                    ?.set(newMessage)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync skipped: ${e.localizedMessage}")
        }
    }

    fun toggle48HourFastForward() {
        if (_simulatedTimeOffsetMs.value == 0L) {
            // Fast forward 50 hours into future
            _simulatedTimeOffsetMs.value = 50 * 60 * 60 * 1000L
        } else {
            // Reset to real time
            _simulatedTimeOffsetMs.value = 0L
        }
    }

    fun addExpiredMediaDemo(memberId: String) {
        val fiftyHoursAgo = System.currentTimeMillis() - (50 * 60 * 60 * 1000L)
        sendMessage(
            memberId = memberId,
            textContent = "Demo photo uploaded 50 hours ago",
            type = MessageType.IMAGE,
            mediaUrl = "https://images.unsplash.com/photo-1511895426328-dc8714191300?w=600&auto=format&fit=crop&q=80",
            forcedTimestamp = fiftyHoursAgo
        )
    }

    // --- 24-HOUR DISAPPEARING STATUS METHODS ---

    private var statusesListener: ListenerRegistration? = null

    private fun setupFirestoreStatusesListener() {
        try {
            statusesListener = firestore?.collection("statuses")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for statuses collection: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val remoteStatuses = mutableListOf<StatusItem>()
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.id
                                val userId = doc.getString("userId") ?: ""
                                val userName = doc.getString("userName") ?: "User"
                                val userAvatarUrl = doc.getString("userAvatarUrl")
                                val textContent = doc.getString("textContent")
                                val photoUrl = doc.getString("photoUrl")
                                val isVideo = doc.getBoolean("isVideo") ?: false
                                val backgroundColorHex = doc.getString("backgroundColorHex") ?: "#321C3B"
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                // Parse viewers list
                                val viewersRaw = doc.get("viewers") as? List<Map<String, Any?>> ?: emptyList()
                                val viewers = viewersRaw.mapNotNull { vMap ->
                                    val vUid = vMap["userId"] as? String ?: return@mapNotNull null
                                    val vName = vMap["userName"] as? String ?: "User"
                                    val vPic = vMap["userAvatarUrl"] as? String
                                    val vTime = vMap["timeAgo"] as? String ?: "Recently"
                                    StatusViewer(vUid, vName, vPic, vTime)
                                }

                                // Parse likes list
                                val likesRaw = doc.get("likes") as? List<Map<String, Any?>> ?: emptyList()
                                val likes = likesRaw.mapNotNull { lMap ->
                                    val lUid = lMap["userId"] as? String ?: return@mapNotNull null
                                    val lName = lMap["userName"] as? String ?: "User"
                                    val lPic = lMap["userAvatarUrl"] as? String
                                    StatusLiker(lUid, lName, lPic)
                                }

                                val item = StatusItem(
                                    id = id,
                                    userId = userId,
                                    userName = userName,
                                    userAvatarUrl = userAvatarUrl,
                                    textContent = textContent,
                                    photoUrl = photoUrl,
                                    isVideo = isVideo,
                                    backgroundColorHex = backgroundColorHex,
                                    timestamp = timestamp,
                                    isSeen = false,
                                    viewers = viewers,
                                    likes = likes
                                )
                                if (!item.isExpired(_simulatedTimeOffsetMs.value)) {
                                    remoteStatuses.add(item)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error parsing status doc ${doc.id}: ${e.localizedMessage}")
                            }
                        }

                        if (remoteStatuses.isNotEmpty()) {
                            val currentList = _statuses.value
                            val seenIds = currentList.filter { it.isSeen }.map { it.id }.toSet()

                            val merged = remoteStatuses.map { remote ->
                                if (seenIds.contains(remote.id)) remote.copy(isSeen = true) else remote
                            }.sortedByDescending { it.timestamp }

                            _statuses.value = merged
                            saveStatusesToPrefs()
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set up statuses listener: ${e.localizedMessage}")
        }
    }

    private fun loadStatuses() {
        val savedStatusesJson = contactPrefs.getString(KEY_STATUSES_JSON, null)
        val loadedList = mutableListOf<StatusItem>()

        if (!savedStatusesJson.isNullOrBlank()) {
            try {
                val jsonArray = org.json.JSONArray(savedStatusesJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)

                    val viewersList = mutableListOf<StatusViewer>()
                    if (obj.has("viewers") && !obj.isNull("viewers")) {
                        val vArray = obj.getJSONArray("viewers")
                        for (vIdx in 0 until vArray.length()) {
                            val vObj = vArray.getJSONObject(vIdx)
                            viewersList.add(
                                StatusViewer(
                                    userId = vObj.getString("userId"),
                                    userName = vObj.getString("userName"),
                                    userAvatarUrl = if (vObj.has("userAvatarUrl") && !vObj.isNull("userAvatarUrl")) vObj.getString("userAvatarUrl") else null,
                                    timeAgo = vObj.optString("timeAgo", "Just now")
                                )
                            )
                        }
                    }

                    val likesList = mutableListOf<StatusLiker>()
                    if (obj.has("likes") && !obj.isNull("likes")) {
                        val lArray = obj.getJSONArray("likes")
                        for (lIdx in 0 until lArray.length()) {
                            val lObj = lArray.getJSONObject(lIdx)
                            likesList.add(
                                StatusLiker(
                                    userId = lObj.getString("userId"),
                                    userName = lObj.getString("userName"),
                                    userAvatarUrl = if (lObj.has("userAvatarUrl") && !lObj.isNull("userAvatarUrl")) lObj.getString("userAvatarUrl") else null
                                )
                            )
                        }
                    }

                    val status = StatusItem(
                        id = obj.getString("id"),
                        userId = obj.getString("userId"),
                        userName = obj.getString("userName"),
                        userAvatarUrl = if (obj.has("userAvatarUrl") && !obj.isNull("userAvatarUrl")) obj.getString("userAvatarUrl") else null,
                        textContent = if (obj.has("textContent") && !obj.isNull("textContent")) obj.getString("textContent") else null,
                        photoUrl = if (obj.has("photoUrl") && !obj.isNull("photoUrl")) obj.getString("photoUrl") else null,
                        backgroundColorHex = obj.optString("backgroundColorHex", "#321C3B"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isSeen = obj.optBoolean("isSeen", false),
                        viewers = viewersList,
                        likes = likesList
                    )
                    if (!status.isExpired(_simulatedTimeOffsetMs.value)) {
                        loadedList.add(status)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing saved statuses: ${e.message}")
            }
        }

        _statuses.value = loadedList
    }

    private fun saveStatusesToPrefs() {
        try {
            val jsonArray = org.json.JSONArray()
            _statuses.value.forEach { status ->
                val obj = org.json.JSONObject().apply {
                    put("id", status.id)
                    put("userId", status.userId)
                    put("userName", status.userName)
                    put("userAvatarUrl", status.userAvatarUrl)
                    put("textContent", status.textContent)
                    put("photoUrl", status.photoUrl)
                    put("backgroundColorHex", status.backgroundColorHex)
                    put("timestamp", status.timestamp)
                    put("isSeen", status.isSeen)

                    val viewersArr = org.json.JSONArray()
                    status.viewers.forEach { v ->
                        viewersArr.put(org.json.JSONObject().apply {
                            put("userId", v.userId)
                            put("userName", v.userName)
                            put("userAvatarUrl", v.userAvatarUrl)
                            put("timeAgo", v.timeAgo)
                        })
                    }
                    put("viewers", viewersArr)

                    val likesArr = org.json.JSONArray()
                    status.likes.forEach { l ->
                        likesArr.put(org.json.JSONObject().apply {
                            put("userId", l.userId)
                            put("userName", l.userName)
                            put("userAvatarUrl", l.userAvatarUrl)
                        })
                    }
                    put("likes", likesArr)
                }
                jsonArray.put(obj)
            }
            contactPrefs.edit().putString(KEY_STATUSES_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving statuses to prefs: ${e.message}")
        }
    }

    fun postStatus(
        userId: String = "self",
        userName: String = "You",
        userAvatarUrl: String? = null,
        textContent: String? = null,
        photoUrl: String? = null,
        backgroundColorHex: String = "#321C3B"
    ) {
        val newStatus = StatusItem(
            id = "status_${System.currentTimeMillis()}",
            userId = userId,
            userName = userName,
            userAvatarUrl = userAvatarUrl,
            textContent = textContent,
            photoUrl = photoUrl,
            backgroundColorHex = backgroundColorHex,
            timestamp = System.currentTimeMillis(),
            isSeen = true, // own status is seen by self
            viewers = emptyList(), // Real viewers added when viewed by other registered users
            likes = emptyList()    // Real likes added when liked by other registered users
        )

        val currentList = _statuses.value.toMutableList()
        currentList.add(0, newStatus)
        _statuses.value = currentList
        saveStatusesToPrefs()

        // Sync status to Firestore
        try {
            firestore?.collection("statuses")?.document(newStatus.id)?.set(newStatus)
        } catch (e: Exception) {
            Log.w(TAG, "Firestore status sync error: ${e.localizedMessage}")
        }
    }

    fun toggleStatusLike(
        statusId: String,
        currentUserId: String = "self",
        currentUserName: String = "You",
        currentUserAvatar: String? = null
    ) {
        val updated = _statuses.value.map { status ->
            if (status.id == statusId) {
                val existingLike = status.likes.firstOrNull { it.userId == currentUserId }
                val newLikes = if (existingLike != null) {
                    status.likes.filter { it.userId != currentUserId }
                } else {
                    status.likes + StatusLiker(currentUserId, currentUserName, currentUserAvatar)
                }
                status.copy(likes = newLikes)
            } else {
                status
            }
        }
        _statuses.value = updated
        saveStatusesToPrefs()

        try {
            val updatedStatus = updated.firstOrNull { it.id == statusId }
            if (updatedStatus != null) {
                firestore?.collection("statuses")?.document(statusId)?.set(updatedStatus)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing status like to Firestore: ${e.localizedMessage}")
        }
    }

    fun markStatusAsSeen(
        statusId: String,
        viewerUserId: String = "self",
        viewerUserName: String = "You",
        viewerUserAvatar: String? = null
    ) {
        val targetStatus = _statuses.value.firstOrNull { it.id == statusId } ?: return
        var statusUpdated = false

        val updatedList = _statuses.value.map { status ->
            if (status.id == statusId) {
                val alreadyInViewers = status.viewers.any { it.userId == viewerUserId }
                val newViewers = if (!alreadyInViewers && viewerUserId != status.userId && viewerUserId != "self") {
                    statusUpdated = true
                    status.viewers + StatusViewer(viewerUserId, viewerUserName, viewerUserAvatar, "Just now")
                } else {
                    status.viewers
                }
                status.copy(isSeen = true, viewers = newViewers)
            } else {
                status
            }
        }
        _statuses.value = updatedList
        saveStatusesToPrefs()

        if (statusUpdated) {
            try {
                val updatedStatus = updatedList.firstOrNull { it.id == statusId }
                if (updatedStatus != null) {
                    firestore?.collection("statuses")?.document(statusId)?.set(updatedStatus)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error updating status viewer in Firestore: ${e.localizedMessage}")
            }
        }
    }

    fun getGroupedActiveStatuses(currentUserId: String = "self"): List<UserStatusGroup> {
        val activeStatuses = _statuses.value.filter { !it.isExpired(_simulatedTimeOffsetMs.value) }
        val groupedMap = activeStatuses.groupBy { it.userId }

        val groups = groupedMap.map { (uId, statusList) ->
            val firstItem = statusList.first()
            val isSelfGroup = (uId == currentUserId || uId == "self")
            UserStatusGroup(
                userId = uId,
                userName = if (isSelfGroup) "My Status" else firstItem.userName,
                userAvatarUrl = firstItem.userAvatarUrl,
                statuses = statusList.sortedBy { it.timestamp }
            )
        }.toMutableList()

        // Sort so "My Status" is first, then users with unseen status, then recent
        groups.sortWith { g1, g2 ->
            val isG1Self = (g1.userId == currentUserId || g1.userId == "self")
            val isG2Self = (g2.userId == currentUserId || g2.userId == "self")
            when {
                isG1Self -> -1
                isG2Self -> 1
                g1.hasUnseen && !g2.hasUnseen -> -1
                !g1.hasUnseen && g2.hasUnseen -> 1
                else -> (g2.statuses.lastOrNull()?.timestamp ?: 0L).compareTo(g1.statuses.lastOrNull()?.timestamp ?: 0L)
            }
        }

        return groups
    }
}
