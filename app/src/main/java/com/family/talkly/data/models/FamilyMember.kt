package com.family.talkly.data.models

import com.family.talkly.util.TimeUtils

data class FamilyMember(
    val id: String,
    val name: String,
    val relation: String,
    val avatarUrl: String? = null,
    val status: String = "Available for video call",
    val phone: String,
    val isOnline: Boolean = true,
    val isTyping: Boolean = false,
    val lastSeen: String = "Just now",
    val lastActiveTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isRegisteredOnTalkly: Boolean = false,
    val firebaseUid: String? = null
) {
    /**
     * Active badge should stay visible if user is currently online
     * OR if user was active within the last 45 minutes (45 * 60 * 1000 ms).
     */
    val isRecentlyActive: Boolean
        get() {
            if (isOnline) return true
            if (lastActiveTimestamp <= 0L) return false
            val diff = System.currentTimeMillis() - lastActiveTimestamp
            return diff in 0L..(45 * 60 * 1000L)
        }

    val displayLastSeen: String
        get() = TimeUtils.formatLastSeenTime(lastActiveTimestamp, lastSeen)
}

val DEFAULT_FAMILY_MEMBERS = listOf(
    FamilyMember(
        id = "safwan",
        name = "Safwan",
        relation = "Friend",
        status = "আচ্ছা সকালে কথা হবে",
        phone = "+880 1700-000001",
        isOnline = true,
        isTyping = false,
        lastSeen = "6:30 PM",
        unreadCount = 0
    ),
    FamilyMember(
        id = "israfel",
        name = "Md Israfel Hosen",
        relation = "Contact",
        status = "Tap to view",
        phone = "+880 1700-000002",
        isOnline = false,
        isTyping = false,
        lastSeen = "Sat",
        unreadCount = 0
    ),
    FamilyMember(
        id = "jolil",
        name = "Jolil",
        relation = "Contact",
        status = "Missed Video Call",
        phone = "+880 1700-000003",
        isOnline = false,
        isTyping = false,
        lastSeen = "Fri",
        unreadCount = 0
    ),
    FamilyMember(
        id = "samim",
        name = "সামিম",
        relation = "Contact",
        status = "Missed Audio Call",
        phone = "+880 1700-000004",
        isOnline = false,
        isTyping = false,
        lastSeen = "Mon",
        unreadCount = 0
    ),
    FamilyMember(
        id = "akhter",
        name = "md Akhter Høssain° •:...",
        relation = "Contact",
        status = "Tap to view",
        phone = "+880 1700-000005",
        isOnline = false,
        isTyping = false,
        lastSeen = "Jul 10",
        unreadCount = 0
    ),
    FamilyMember(
        id = "osman",
        name = "Osman Vi",
        relation = "Contact",
        status = "Missed Audio Call",
        phone = "+880 1700-000006",
        isOnline = false,
        isTyping = false,
        lastSeen = "Jun 30",
        unreadCount = 0
    ),
    FamilyMember(
        id = "mohammad_raiu",
        name = "Mohammad Raiu Mha...",
        relation = "Contact",
        status = "Tap to view",
        phone = "+880 1700-000007",
        isOnline = false,
        isTyping = false,
        lastSeen = "Jun 29",
        unreadCount = 0
    ),
    FamilyMember(
        id = "dr_rashed",
        name = "Dr. Rashed",
        relation = "Doctor",
        status = "Medical updates",
        phone = "+880 1700-000008",
        isOnline = true,
        unreadCount = 3
    ),
    FamilyMember(
        id = "monju",
        name = "Monju",
        relation = "Friend",
        status = "Available",
        phone = "+880 1700-000009",
        isOnline = true,
        unreadCount = 1
    ),
    FamilyMember(
        id = "sk_farid",
        name = "Sk F A R I D",
        relation = "Friend",
        status = "At work",
        phone = "+880 1700-000010",
        isOnline = true,
        unreadCount = 2
    )
)
