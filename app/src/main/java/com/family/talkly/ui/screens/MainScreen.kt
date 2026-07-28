package com.family.talkly.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.family.talkly.data.firebase.FirebaseChatRepository
import com.family.talkly.data.models.CallDirection
import com.family.talkly.data.models.CallType
import com.family.talkly.data.models.DEFAULT_FAMILY_MEMBERS
import com.family.talkly.data.models.FamilyMember
import com.family.talkly.data.models.MessageType
import com.family.talkly.data.zego.CallState
import com.family.talkly.data.zego.ZegoCallEngineManager
import com.family.talkly.ui.components.IncomingCallDialog
import com.family.talkly.ui.theme.PrimaryDarkPurple
import com.family.talkly.ui.theme.SecondaryLightSage
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal

import com.family.talkly.data.models.UserProfile
import com.family.talkly.ui.theme.ThemeMode

@Composable
fun MainScreen(
    chatRepository: FirebaseChatRepository,
    zegoManager: ZegoCallEngineManager,
    currentUserProfile: UserProfile? = null,
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: ((ThemeMode) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onSaveProfile: ((name: String, bio: String, photoUrl: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeChatMember by remember { mutableStateOf<FamilyMember?>(null) }
    val familyMembers by chatRepository.familyMembers.collectAsState()
    val blockedUserIds by chatRepository.blockedUserIds.collectAsState()
    val statuses by chatRepository.statuses.collectAsState()
    val simulatedTimeOffsetMs by chatRepository.simulatedTimeOffsetMs.collectAsState()
    val messagesMap by chatRepository.messagesMap.collectAsState()

    val statusGroups = remember(statuses, familyMembers, simulatedTimeOffsetMs, currentUserProfile) {
        chatRepository.getGroupedActiveStatuses("self")
    }

    val callInfo by zegoManager.callState.collectAsState()
    val callLogs by zegoManager.callLogs.collectAsState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, currentUserProfile?.uid) {
        val uid = currentUserProfile?.uid
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (!uid.isNullOrBlank()) {
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_START,
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                        chatRepository.updateCurrentUserPresence(uid, isOnline = true)
                    }
                    androidx.lifecycle.Lifecycle.Event.ON_STOP,
                    androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                        chatRepository.updateCurrentUserPresence(uid, isOnline = false)
                    }
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (!uid.isNullOrBlank()) {
            chatRepository.updateCurrentUserPresence(uid, isOnline = true)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!uid.isNullOrBlank()) {
                chatRepository.updateCurrentUserPresence(uid, isOnline = false)
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(currentUserProfile?.uid) {
        if (currentUserProfile != null && currentUserProfile.uid.isNotBlank()) {
            chatRepository.startRealtimeMessageSync(currentUserProfile.uid)
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        zegoManager.onCallLogAdded = { log ->
            val callTypeLabel = if (log.callType == CallType.VIDEO) "Video call" else "Voice call"
            val durationText = if (log.durationSeconds > 0) {
                val mins = log.durationSeconds / 60
                val secs = log.durationSeconds % 60
                String.format(java.util.Locale.getDefault(), " (%02d:%02d)", mins, secs)
            } else ""
            val messageText = when (log.direction) {
                CallDirection.MISSED -> "📞 Missed $callTypeLabel"
                CallDirection.OUTGOING -> "📞 Outgoing $callTypeLabel$durationText"
                CallDirection.INCOMING -> "📞 Incoming $callTypeLabel$durationText"
            }
            chatRepository.sendMessage(
                memberId = log.memberId,
                textContent = messageText,
                type = MessageType.TEXT
            )
        }
    }

    var pendingCallMember by remember { mutableStateOf<FamilyMember?>(null) }
    var pendingCallType by remember { mutableStateOf<CallType?>(null) }

    val callPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false

        if (cameraGranted && micGranted) {
            val target = pendingCallMember
            val type = pendingCallType
            if (target != null && type != null) {
                zegoManager.startOutgoingCall(target, type)
            }
        } else {
            Toast.makeText(context, "Camera and Microphone permissions are required for calls", Toast.LENGTH_LONG).show()
        }
        pendingCallMember = null
        pendingCallType = null
    }

    fun startCallWithPermissions(target: FamilyMember, callType: CallType) {
        val isBlocked = blockedUserIds.contains(target.id) || chatRepository.isUserBlocked(target.id)
        if (isBlocked) {
            Toast.makeText(context, "Cannot call: User is blocked", Toast.LENGTH_SHORT).show()
            return
        }

        if (!target.isRegisteredOnTalkly || target.firebaseUid.isNullOrEmpty()) {
            Toast.makeText(context, "User not registered on Talkly", Toast.LENGTH_SHORT).show()
            return
        }

        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (hasCamera && hasMic) {
            zegoManager.startOutgoingCall(target, callType, isBlocked = isBlocked)
        } else {
            pendingCallMember = target
            pendingCallType = callType
            callPermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    // Handle Incoming Call Modal
    if (callInfo.state == CallState.INCOMING_RINGING && callInfo.targetMember != null) {
        IncomingCallDialog(
            member = callInfo.targetMember!!,
            callType = callInfo.callType,
            onAccept = { zegoManager.acceptCall() },
            onDecline = { zegoManager.declineCall() }
        )
    }

    // Full Screen Active Call Screen
    if (callInfo.state == CallState.ACTIVE || callInfo.state == CallState.OUTGOING_RINGING || callInfo.state == CallState.OUTGOING_CALLING) {
        CallScreen(
            callInfo = callInfo,
            onEndCall = { zegoManager.endCall() },
            onToggleMute = { zegoManager.toggleMute() },
            onToggleCamera = { zegoManager.toggleCamera() },
            onFlipCamera = { zegoManager.flipCamera() },
            onToggleSpeaker = { zegoManager.toggleSpeaker() }
        )
        return
    }

    // Detail Chat Screen for selected family member
    if (activeChatMember != null) {
        val memberId = activeChatMember!!.id
        val currentMember = familyMembers.firstOrNull { it.id == memberId } ?: activeChatMember!!
        val currentMessages = messagesMap[currentMember.id] ?: emptyList()

        ChatDetailScreen(
            member = currentMember,
            messages = currentMessages,
            simulatedTimeOffsetMs = simulatedTimeOffsetMs,
            onBack = { activeChatMember = null },
            onSendMessage = { text, type, mediaUrl, replyToId, replyToName, replyToText ->
                chatRepository.sendMessage(
                    memberId = currentMember.id,
                    textContent = text,
                    type = type,
                    mediaUrl = mediaUrl,
                    replyToMessageId = replyToId,
                    replyToSenderName = replyToName,
                    replyToText = replyToText
                )
                chatRepository.triggerSimulatedTypingReply(currentMember.id)
            },
            onToggleReaction = { messageId, reactionEmoji ->
                chatRepository.toggleMessageReaction(currentMember.id, messageId, reactionEmoji)
            },
            onToggleStarMessage = { messageId ->
                chatRepository.toggleStarMessage(currentMember.id, messageId)
            },
            onTogglePinMessage = { messageId ->
                chatRepository.togglePinMessage(currentMember.id, messageId)
            },
            onTogglePinMember = {
                chatRepository.togglePinMember(currentMember.id)
            },
            onTypingStateChanged = { isTyping ->
                // Simulate status sync
            },
            onToggleFastForward = { chatRepository.toggle48HourFastForward() },
            onAddExpiredDemo = { chatRepository.addExpiredMediaDemo(currentMember.id) },
            onStartCall = { callType ->
                startCallWithPermissions(currentMember, callType)
            },
            onReadMessages = {
                chatRepository.markMessagesAsRead(currentMember.id)
            },
            isInitiallyBlocked = blockedUserIds.contains(currentMember.id),
            onBlockUser = {
                chatRepository.blockUser(currentMember.id)
            },
            onUnblockUser = {
                chatRepository.unblockUser(currentMember.id)
            }
        )
        return
    }

    // Main Screen Content
    ChatListScreen(
        familyMembers = familyMembers,
        messagesMap = messagesMap,
        simulatedTimeOffsetMs = simulatedTimeOffsetMs,
        currentUserProfile = currentUserProfile,
        currentThemeMode = currentThemeMode,
        onThemeModeChange = onThemeModeChange,
        onLogout = onLogout,
        onSaveProfile = onSaveProfile,
        onSelectMember = { activeChatMember = it },
        onStartCall = { member, type ->
            startCallWithPermissions(member, type)
        },
        onTriggerIncomingDemo = { member ->
            zegoManager.triggerIncomingCall(member, CallType.VIDEO)
        },
        onTogglePinMember = { memberId ->
            chatRepository.togglePinMember(memberId)
        },
        onSearchUserByPhone = { phone, callback ->
            chatRepository.searchTalklyUserByPhone(phone, callback)
        },
        onAddContact = { name, phone, relation, bio, avatarUrl ->
            chatRepository.addNewContact(name, phone, relation, bio, avatarUrl)
        },
        onDeleteContact = { memberId ->
            chatRepository.deleteContact(memberId)
        },
        onClearDemoContacts = {
            chatRepository.clearDemoContacts()
        },
        statusGroups = statusGroups,
        onPostStatus = { textContent, photoUrl, bgHex ->
            val userId = currentUserProfile?.uid?.ifBlank { "self" } ?: "self"
            val userName = currentUserProfile?.name?.ifBlank { "You" } ?: "You"
            val userAvatar = currentUserProfile?.profilePicUrl?.ifBlank { null }
            chatRepository.postStatus(
                userId = userId,
                userName = userName,
                userAvatarUrl = userAvatar,
                textContent = textContent,
                photoUrl = photoUrl,
                backgroundColorHex = bgHex
            )
        },
        onMarkStatusSeen = { statusId ->
            val userId = currentUserProfile?.uid?.ifBlank { "self" } ?: "self"
            val userName = currentUserProfile?.name?.ifBlank { "You" } ?: "You"
            val userAvatar = currentUserProfile?.profilePicUrl?.ifBlank { null }
            chatRepository.markStatusAsSeen(
                statusId = statusId,
                viewerUserId = userId,
                viewerUserName = userName,
                viewerUserAvatar = userAvatar
            )
        },
        onToggleLikeStatus = { statusId ->
            val userId = currentUserProfile?.uid?.ifBlank { "self" } ?: "self"
            val userName = currentUserProfile?.name?.ifBlank { "You" } ?: "You"
            val userAvatar = currentUserProfile?.profilePicUrl?.ifBlank { null }
            chatRepository.toggleStatusLike(statusId, userId, userName, userAvatar)
        },
        onSendStatusReply = { targetUserId, replyText ->
            chatRepository.sendMessage(
                memberId = targetUserId,
                textContent = "💬 [Status Reply]: $replyText",
                type = com.family.talkly.data.models.MessageType.TEXT
            )
        },
        blockedUserIds = blockedUserIds,
        onBlockUser = { memberId ->
            chatRepository.blockUser(memberId)
        },
        onUnblockUser = { memberId ->
            chatRepository.unblockUser(memberId)
        }
    )
}
