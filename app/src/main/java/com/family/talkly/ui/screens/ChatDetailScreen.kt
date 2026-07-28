@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.family.talkly.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import com.family.talkly.util.AudioRecorder
import com.family.talkly.util.MediaCompressorAndUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.foundation.BorderStroke
import com.family.talkly.ui.theme.LocalIsDarkTheme
import com.family.talkly.ui.theme.WhatsappDarkBg
import com.family.talkly.ui.theme.WhatsappDarkBubble
import com.family.talkly.ui.theme.WhatsappDarkSurface
import com.family.talkly.ui.theme.WhatsappLightGreen
import com.family.talkly.ui.components.WallpaperSelectionDialog
import com.family.talkly.util.ImageUtils
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.family.talkly.data.models.CallType
import com.family.talkly.data.models.ChatMessage
import com.family.talkly.data.models.FamilyMember
import com.family.talkly.data.models.MessageType
import com.family.talkly.ui.components.ContactProfileDetailsDialog
import com.family.talkly.ui.components.FullMediaViewerDialog
import com.family.talkly.ui.components.MediaAttachmentDialog
import com.family.talkly.ui.components.MediaMessageItem
import com.family.talkly.ui.components.MessageLoadingState
import com.family.talkly.ui.theme.PrimaryDarkPurple
import com.family.talkly.ui.theme.ReceivedBubbleWhite
import com.family.talkly.ui.theme.SecondaryLightSage
import com.family.talkly.ui.theme.SentBubbleGreen
import com.family.talkly.ui.theme.WhatsappChatBg
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    member: FamilyMember,
    messages: List<ChatMessage>,
    simulatedTimeOffsetMs: Long,
    isLoadingMessages: Boolean = false,
    onBack: () -> Unit,
    onSendMessage: (
        textContent: String,
        type: MessageType,
        mediaUrl: String?,
        replyToId: String?,
        replyToName: String?,
        replyToText: String?
    ) -> Unit,
    onToggleReaction: (messageId: String, reactionEmoji: String) -> Unit = { _, _ -> },
    onToggleStarMessage: (messageId: String) -> Unit = {},
    onTogglePinMessage: (messageId: String) -> Unit = {},
    onTogglePinMember: () -> Unit = {},
    onTypingStateChanged: (Boolean) -> Unit,
    onToggleFastForward: () -> Unit,
    onAddExpiredDemo: () -> Unit,
    onStartCall: (CallType) -> Unit,
    onReadMessages: () -> Unit = {},
    isInitiallyBlocked: Boolean = false,
    onBlockUser: (() -> Unit)? = null,
    onUnblockUser: (() -> Unit)? = null
) {
    var textInput by remember { mutableStateOf("") }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var fullMediaViewerMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var reactionDialogMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showContactProfile by remember { mutableStateOf(false) }
    
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showStarredMessagesDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uploader = remember { MediaCompressorAndUploader(context) }
    val audioRecorder = remember { AudioRecorder(context) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableStateOf(0) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }
    var isUploadingAudio by remember { mutableStateOf(false) }

    // Recording timer loop
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSec = 0
            while (isRecording) {
                delay(1000)
                recordingDurationSec++
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = audioRecorder.startRecording()
            if (file != null) {
                currentAudioFile = file
                isRecording = true
            } else {
                Toast.makeText(context, "Could not start audio recording", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVoiceRecording() {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val file = audioRecorder.startRecording()
            if (file != null) {
                currentAudioFile = file
                isRecording = true
            } else {
                Toast.makeText(context, "Could not start audio recording", Toast.LENGTH_SHORT).show()
            }
        } else {
            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopAndSendRecording() {
        val file = audioRecorder.stopRecording()
        isRecording = false
        if (file == null || !file.exists() || file.length() == 0L) {
            Toast.makeText(context, "Voice recording was empty", Toast.LENGTH_SHORT).show()
            return
        }

        isUploadingAudio = true
        scope.launch(Dispatchers.IO) {
            val remotePath = "family_chats/${member.id}/voice_notes/vn_${System.currentTimeMillis()}.m4a"
            val finalUrl = uploader.uploadToFirebaseStorage(file, remotePath) { progress, status ->
                android.util.Log.d("ChatDetailScreen", "Voice note upload progress: $progress% - $status")
            }

            scope.launch(Dispatchers.Main) {
                isUploadingAudio = false
                val durationText = "${recordingDurationSec}s"
                onSendMessage(
                    "Voice Message ($durationText)",
                    MessageType.VOICE_NOTE,
                    finalUrl,
                    replyingToMessage?.id,
                    replyingToMessage?.senderName,
                    replyingToMessage?.textContent?.ifEmpty { "Voice Message" }
                )
                replyingToMessage = null
                recordingDurationSec = 0
                Toast.makeText(context, "Voice note sent!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun cancelRecording() {
        audioRecorder.cancelRecording()
        isRecording = false
        currentAudioFile = null
        recordingDurationSec = 0
        Toast.makeText(context, "Recording cancelled", Toast.LENGTH_SHORT).show()
    }
    var showMenu by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isBlocked by remember(isInitiallyBlocked) { mutableStateOf(isInitiallyBlocked) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showClearChatConfirmDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var localClearedMessages by remember { mutableStateOf(false) }

    val prefs = remember(context) { context.getSharedPreferences("talkly_prefs", Context.MODE_PRIVATE) }
    var wallpaperValue by remember(member.id) {
        mutableStateOf(
            prefs.getString("wallpaper_${member.id}", null)
                ?: prefs.getString("wallpaper_global", "#E5DDD5")
                ?: "#E5DDD5"
        )
    }

    val activeMessages = if (localClearedMessages) emptyList() else messages

    val displayedMessages = remember(activeMessages, isSearchActive, searchQuery) {
        if (isSearchActive && searchQuery.isNotBlank()) {
            activeMessages.filter {
                it.textContent.contains(searchQuery, ignoreCase = true)
            }
        } else {
            activeMessages
        }
    }

    val pinnedMessage = remember(activeMessages) {
        activeMessages.lastOrNull { it.isPinned }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Mark messages as read when opening or receiving new messages in chat screen
    LaunchedEffect(member.id, messages.size) {
        onReadMessages()
    }

    // Auto-scroll to latest message whenever new message arrives or user is typing
    LaunchedEffect(messages.size, member.isTyping) {
        if (messages.isNotEmpty()) {
            val targetIndex = messages.size - 1 + (if (member.isTyping) 1 else 0)
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Attachment Dialog
    if (showAttachmentDialog) {
        MediaAttachmentDialog(
            onDismiss = { showAttachmentDialog = false },
            onSendMediaWithTag = { caption, type, url ->
                onSendMessage(
                    caption, type, url,
                    replyingToMessage?.id,
                    replyingToMessage?.senderName,
                    replyingToMessage?.textContent?.ifEmpty { "Media photo/video" }
                )
                replyingToMessage = null
            },
            onSendExpiredDemo = {
                onAddExpiredDemo()
            }
        )
    }

    // Full Screen Media Viewer Dialog
    fullMediaViewerMessage?.let { mediaMsg ->
        FullMediaViewerDialog(
            message = mediaMsg,
            onDismiss = { fullMediaViewerMessage = null }
        )
    }

    // Reaction & Reply Long-Click Dialog
    reactionDialogMessage?.let { selectedMsg ->
        Dialog(onDismissRequest = { reactionDialogMessage = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "React to message",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = WhatsappTeal
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Reaction Emojis Row
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("❤️", "😢", "😡", "😮", "👎", "👍", "🔥").forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (selectedMsg.reaction == emoji) WhatsappGreen.copy(alpha = 0.2f) else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable {
                                        onToggleReaction(selectedMsg.id, emoji)
                                        reactionDialogMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons: Reply, Star, Pin & Cancel
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WhatsappGreen.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        replyingToMessage = selectedMsg
                                        reactionDialogMessage = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = "Reply",
                                        tint = WhatsappGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Reply",
                                        color = WhatsappGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF8E1),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onToggleStarMessage(selectedMsg.id)
                                        reactionDialogMessage = null
                                        Toast.makeText(
                                            context,
                                            if (selectedMsg.isStarred) "Unstarred message" else "Starred message ⭐",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (selectedMsg.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Star",
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (selectedMsg.isStarred) "Unstar" else "Star ⭐",
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WhatsappTeal.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onTogglePinMessage(selectedMsg.id)
                                        reactionDialogMessage = null
                                        Toast.makeText(
                                            context,
                                            if (selectedMsg.isPinned) "Unpinned message" else "Pinned to top 📌",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pin",
                                        tint = WhatsappTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (selectedMsg.isPinned) "Unpin" else "Pin 📌",
                                        color = WhatsappTeal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = { reactionDialogMessage = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (showContactProfile) {
        ContactProfileDetailsDialog(
            member = member,
            onDismiss = { showContactProfile = false },
            onStartChat = { showContactProfile = false },
            onStartCall = { _, callType ->
                showContactProfile = false
                onStartCall(callType)
            }
        )
    }

    if (showStarredMessagesDialog) {
        AlertDialog(
            onDismissRequest = { showStarredMessagesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Starred Messages", fontWeight = FontWeight.Bold, color = WhatsappTeal)
                }
            },
            text = {
                val starredList = activeMessages.filter { it.isStarred }
                if (starredList.isEmpty()) {
                    Text(
                        "No starred messages in this chat yet.\n\nLong-press any message and tap Star ⭐ to save important notes!",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(starredList, key = { it.id }) { msg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF8E1),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = msg.senderName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = WhatsappTeal
                                        )
                                        IconButton(
                                            onClick = { onToggleStarMessage(msg.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Unstar",
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.textContent.ifEmpty { "[Media message]" },
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.formattedTime,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStarredMessagesDialog = false }) {
                    Text("Close", color = WhatsappTeal, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showBlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            title = {
                Text(
                    text = "Block ${member.name}?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111B21)
                )
            },
            text = {
                Text(
                    text = "Blocked contacts will no longer be able to send you messages or call you.",
                    color = Color.DarkGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockConfirmDialog = false
                        isBlocked = true
                        onBlockUser?.invoke()
                        Toast.makeText(context, "${member.name} has been blocked", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Block", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showClearChatConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatConfirmDialog = false },
            title = {
                Text(
                    text = "Clear this chat?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111B21)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all messages in this conversation with ${member.name}?",
                    color = Color.DarkGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearChatConfirmDialog = false
                        localClearedMessages = true
                        Toast.makeText(context, "Chat messages cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear Chat", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showWallpaperDialog) {
        WallpaperSelectionDialog(
            currentValue = wallpaperValue,
            contactName = member.name,
            onDismiss = { showWallpaperDialog = false },
            onWallpaperSelected = { newValue, applyToAll ->
                showWallpaperDialog = false
                wallpaperValue = newValue
                if (applyToAll) {
                    prefs.edit()
                        .putString("wallpaper_global", newValue)
                        .putString("wallpaper_${member.id}", newValue)
                        .apply()
                } else {
                    prefs.edit()
                        .putString("wallpaper_${member.id}", newValue)
                        .apply()
                }
                Toast.makeText(context, "Wallpaper updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search messages...", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close search",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkPurple)
                )
            } else {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showContactProfile = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (member.avatarUrl != null) {
                                    AsyncImage(
                                        model = ImageUtils.getProfileImageModel(member.avatarUrl),
                                        contentDescription = member.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = member.name.take(2).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = com.family.talkly.util.TimeUtils.getFirstName(member.name),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    if (isMuted) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.VolumeMute,
                                            contentDescription = "Muted",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                val statusSubtext = when {
                                    !member.isRegisteredOnTalkly -> "Talkly তে রেজিস্টার্ড নন"
                                    isBlocked -> "ব্লকড"
                                    member.isTyping -> "টাইপিং..."
                                    member.isOnline -> "অনলাইন"
                                    else -> "লাস্ট সিন ${member.displayLastSeen}"
                                }
                                Text(
                                    text = statusSubtext,
                                    fontSize = 11.sp,
                                    fontWeight = if (member.isTyping && !isBlocked) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!member.isRegisteredOnTalkly || isBlocked) Color(0xFFFFCDD2) else if (member.isTyping) Color(0xFF25D366) else Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onBack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Messages",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            if (member.isRegisteredOnTalkly) onStartCall(CallType.AUDIO)
                            else Toast.makeText(context, "User not registered on Talkly", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Audio Call",
                                tint = if (member.isRegisteredOnTalkly) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                        }
                        IconButton(onClick = {
                            if (member.isRegisteredOnTalkly) onStartCall(CallType.VIDEO)
                            else Toast.makeText(context, "User not registered on Talkly", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video Call",
                                tint = if (member.isRegisteredOnTalkly) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("View Contact Info", fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = WhatsappTeal
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showContactProfile = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Starred Messages", fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showStarredMessagesDialog = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(if (member.isPinned) "Unpin Conversation" else "Pin Conversation", fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = null,
                                            tint = WhatsappTeal
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onTogglePinMember()
                                        Toast.makeText(
                                            context,
                                            if (member.isPinned) "Unpinned conversation" else "Pinned conversation to top 📌",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Wallpaper", fontWeight = FontWeight.Medium) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Wallpaper,
                                            contentDescription = null,
                                            tint = WhatsappTeal
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showWallpaperDialog = true
                                    }
                                )

                            DropdownMenuItem(
                                text = { Text(if (isMuted) "Unmute Notifications" else "Mute Notifications", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                        contentDescription = null,
                                        tint = if (isMuted) WhatsappGreen else Color.Gray
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    isMuted = !isMuted
                                    val msg = if (isMuted) "Muted notifications for ${member.name}" else "Unmuted notifications for ${member.name}"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isBlocked) "Unblock ${member.name}" else "Block Contact",
                                        fontWeight = FontWeight.Medium,
                                        color = if (isBlocked) WhatsappTeal else Color(0xFFD32F2F)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = if (isBlocked) WhatsappTeal else Color(0xFFD32F2F)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    if (isBlocked) {
                                        isBlocked = false
                                        onUnblockUser?.invoke()
                                        Toast.makeText(context, "${member.name} unblocked", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showBlockConfirmDialog = true
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Clear Chat", fontWeight = FontWeight.Medium, color = Color(0xFFD32F2F)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showClearChatConfirmDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkPurple)
            )
            }
        }
    ) { innerPadding ->
        val isDarkTheme = LocalIsDarkTheme.current

        val isWallpaperImage = wallpaperValue.startsWith("http://") ||
                wallpaperValue.startsWith("https://") ||
                wallpaperValue.startsWith("content://") ||
                wallpaperValue.startsWith("file://")

        val parsedWallpaperColor = remember(wallpaperValue, isDarkTheme) {
            if (wallpaperValue.startsWith("#")) {
                try {
                    Color(android.graphics.Color.parseColor(wallpaperValue))
                } catch (e: Exception) {
                    if (isDarkTheme) WhatsappDarkBg else WhatsappChatBg
                }
            } else {
                if (isDarkTheme) WhatsappDarkBg else WhatsappChatBg
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .then(
                    if (!isWallpaperImage) Modifier.background(parsedWallpaperColor)
                    else Modifier
                )
        ) {
            if (isWallpaperImage) {
                AsyncImage(
                    model = wallpaperValue,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = if (isDarkTheme) 0.38f else 0.12f))
                )
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Pinned Message Top Banner
            pinnedMessage?.let { pinned ->
                Surface(
                    color = Color(0xFFFFF9C4),
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val idx = displayedMessages.indexOfFirst { it.id == pinned.id }
                            if (idx >= 0) {
                                scope.launch { listState.animateScrollToItem(idx) }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned Message",
                                tint = WhatsappTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Pinned Announcement",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsappTeal
                                )
                                Text(
                                    text = pinned.textContent.ifEmpty { "[Media Attachment]" },
                                    fontSize = 12.sp,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { onTogglePinMessage(pinned.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Unpin",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }



            // Chat Messages List
            if (isLoadingMessages) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    MessageLoadingState(
                        message = "Loading messages...",
                        subMessage = "Fetching end-to-end encrypted chat history"
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    items(displayedMessages, key = { it.id }) { msg ->
                        val isSelf = msg.senderId == "self" ||
                                msg.receiverId == member.id ||
                                (!member.firebaseUid.isNullOrBlank() && msg.receiverId == member.firebaseUid) ||
                                (member.phone.isNotBlank() && (msg.receiverId == member.phone || com.family.talkly.util.PhoneUtils.extractPhoneSuffix(msg.receiverId) == com.family.talkly.util.PhoneUtils.extractPhoneSuffix(member.phone)))
                        var offsetX by remember { mutableFloatStateOf(0f) }
                        var showReadDetails by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(msg.id) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            if (offsetX > 60f) {
                                                // Swipe left to right triggered reply
                                                replyingToMessage = msg
                                            }
                                            offsetX = 0f
                                        },
                                        onHorizontalDrag = { _, dragAmount ->
                                            if (dragAmount > 0 || offsetX > 0) {
                                                offsetX = (offsetX + dragAmount).coerceIn(0f, 100f)
                                            }
                                        }
                                    )
                                }
                        ) {
                            // Reply indicator icon when swiping
                            if (offsetX > 10f) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = "Swipe Reply",
                                    tint = WhatsappGreen,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 8.dp)
                                        .size(24.dp)
                                )
                            }

                            val bubbleContainerColor = if (isDarkTheme) {
                                if (isSelf) WhatsappDarkBubble else WhatsappDarkSurface
                            } else {
                                if (isSelf) SentBubbleGreen else ReceivedBubbleWhite
                            }

                            val bubbleTextColor = if (isDarkTheme) Color(0xFFE9EDEF) else Color(0xFF111B21)
                            val subTextColor = if (isDarkTheme) Color(0xFF8696A0) else Color(0xFF667781)
                            val replyBgColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                            val replySenderColor = if (isDarkTheme) WhatsappLightGreen else WhatsappGreen
                            val replySubTextColor = if (isDarkTheme) Color(0xFFCCCCCC) else Color.DarkGray

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset { IntOffset(offsetX.roundToInt(), 0) },
                                horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
                            ) {
                                Box {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = bubbleContainerColor
                                        ),
                                        border = BorderStroke(
                                            width = 0.5.dp,
                                            color = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
                                        ),
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isSelf) 16.dp else 2.dp,
                                            bottomEnd = if (isSelf) 2.dp else 16.dp
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier
                                            .widthIn(min = 90.dp, max = 280.dp)
                                            .combinedClickable(
                                                onClick = {
                                                    showReadDetails = !showReadDetails
                                                },
                                                onLongClick = {
                                                    reactionDialogMessage = msg
                                                }
                                            )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            // Quoted Reply Preview inside Message Bubble
                                            if (msg.replyToSenderName != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = replyBgColor,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(3.dp)
                                                                .height(28.dp)
                                                                .background(replySenderColor, RoundedCornerShape(2.dp))
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Column {
                                                            Text(
                                                                text = msg.replyToSenderName,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = replySenderColor
                                                            )
                                                            Text(
                                                                text = msg.replyToText ?: "Media message",
                                                                fontSize = 11.sp,
                                                                color = replySubTextColor,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Media Content
                                            if (msg.mediaUrl != null || msg.isMediaExpired(simulatedTimeOffsetMs)) {
                                                MediaMessageItem(
                                                    message = msg,
                                                    simulatedTimeOffsetMs = simulatedTimeOffsetMs,
                                                    onMediaClick = {
                                                        if (!msg.isMediaExpired(simulatedTimeOffsetMs)) {
                                                            fullMediaViewerMessage = msg
                                                        }
                                                    }
                                                )
                                            }

                                            // Text Content
                                            if (msg.textContent.isNotEmpty()) {
                                                Text(
                                                    text = msg.textContent,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        color = bubbleTextColor,
                                                        fontSize = 15.sp
                                                    )
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Timestamp & Read Receipt Double Tick
                                            Row(
                                                modifier = Modifier.align(Alignment.End),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (msg.isStarred) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "Starred",
                                                        tint = Color(0xFFFFB300),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                }
                                                if (msg.isPinned) {
                                                    Icon(
                                                        imageVector = Icons.Default.PushPin,
                                                        contentDescription = "Pinned",
                                                        tint = if (isDarkTheme) WhatsappLightGreen else WhatsappTeal,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                }
                                                Text(
                                                    text = msg.formattedTime,
                                                    fontSize = 10.sp,
                                                    color = subTextColor
                                                )
                                                if (isSelf) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    when {
                                                        msg.isRead -> {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(16.dp)
                                                                    .background(Color(0xFF25D366), CircleShape),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = "S",
                                                                    color = Color.White,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    lineHeight = 10.sp
                                                                )
                                                            }
                                                        }
                                                        msg.isDelivered -> {
                                                            Icon(
                                                                imageVector = Icons.Default.DoneAll,
                                                                contentDescription = "Delivered",
                                                                tint = subTextColor,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                        else -> {
                                                            Icon(
                                                                imageVector = Icons.Default.Done,
                                                                contentDescription = "Sent",
                                                                tint = subTextColor,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (showReadDetails) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val detailsText = if (msg.isRead) {
                                                    "Seen at ${msg.formattedReadTime}"
                                                } else if (msg.isDelivered) {
                                                    "Delivered ${msg.formattedTime}"
                                                } else {
                                                    "Sent ${msg.formattedTime}"
                                                }
                                                Text(
                                                    text = detailsText,
                                                    fontSize = 9.sp,
                                                    color = subTextColor,
                                                    modifier = Modifier.align(Alignment.End)
                                                )
                                            }
                                        }
                                    }

                                    // Reaction Badge Pill Overlay
                                    msg.reaction?.let { reactEmoji ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isDarkTheme) WhatsappDarkSurface else Color.White,
                                            border = BorderStroke(0.5.dp, if (isDarkTheme) Color.White.copy(alpha = 0.20f) else Color.LightGray.copy(alpha = 0.5f)),
                                            tonalElevation = 4.dp,
                                            shadowElevation = 2.dp,
                                            modifier = Modifier
                                                .align(if (isSelf) Alignment.BottomStart else Alignment.BottomEnd)
                                                .offset(y = 8.dp, x = if (isSelf) (-6).dp else 6.dp)
                                                .clickable {
                                                    onToggleReaction(msg.id, reactEmoji)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = reactEmoji, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Live Typing Bubble Indicator
                    if (member.isTyping) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ReceivedBubbleWhite),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${member.name} is typing...",
                                            fontSize = 13.sp,
                                            color = WhatsappGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }

            // Quick Emoji Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("❤️", "👍", "🤗", "🍪", "🍲", "📸").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clickable {
                                onSendMessage(
                                    emoji, MessageType.TEXT, null,
                                    replyingToMessage?.id,
                                    replyingToMessage?.senderName,
                                    replyingToMessage?.textContent?.ifEmpty { "Media" }
                                )
                                replyingToMessage = null
                            }
                            .padding(4.dp)
                    )
                }
            }

            // Replying Banner Bar directly above input field
            AnimatedVisibility(
                visible = replyingToMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                replyingToMessage?.let { replyMsg ->
                    Surface(
                        color = Color(0xFFF0F2F5),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, Color.LightGray)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(36.dp)
                                        .background(WhatsappGreen, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Replying to ${replyMsg.senderName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhatsappGreen
                                    )
                                    Text(
                                        text = replyMsg.textContent.ifEmpty { "Media photo/video" },
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { replyingToMessage = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar or Blocked/Unregistered Contact Banner
            if (!member.isRegisteredOnTalkly) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "User not registered on Talkly",
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (isBlocked) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isBlocked = false
                            onUnblockUser?.invoke()
                            Toast.makeText(context, "${member.name} unblocked", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You blocked this contact. Tap to unblock.",
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (isUploadingAudio) {
                Surface(
                    color = Color.White,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = WhatsappGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Uploading voice note to Firebase Storage...",
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else if (isRecording) {
                Surface(
                    color = Color(0xFFFFF3F3),
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format("Recording %d:%02d", recordingDurationSec / 60, recordingDurationSec % 60),
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { cancelRecording() }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancel Recording",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            FloatingActionButton(
                                onClick = { stopAndSendRecording() },
                                containerColor = WhatsappGreen,
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Voice Note",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = Color.White,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showAttachmentDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach",
                                tint = WhatsappTeal
                            )
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = {
                                textInput = it
                                onTypingStateChanged(it.isNotBlank())
                            },
                            placeholder = { Text("Type family message...", fontSize = 14.sp, color = Color.Gray) },
                            textStyle = TextStyle(
                                color = Color(0xFF111B21),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (textInput.isNotBlank()) {
                                        onSendMessage(
                                            textInput, MessageType.TEXT, null,
                                            replyingToMessage?.id,
                                            replyingToMessage?.senderName,
                                            replyingToMessage?.textContent?.ifEmpty { "Media" }
                                        )
                                        textInput = ""
                                        replyingToMessage = null
                                        onTypingStateChanged(false)
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF111B21),
                                unfocusedTextColor = Color(0xFF111B21),
                                focusedContainerColor = Color(0xFFF0F2F5),
                                unfocusedContainerColor = Color(0xFFF0F2F5),
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                                focusedBorderColor = WhatsappGreen
                            ),
                            maxLines = 3
                        )

                        IconButton(onClick = { startVoiceRecording() }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Voice Note",
                                tint = WhatsappTeal
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        FloatingActionButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    onSendMessage(
                                        textInput, MessageType.TEXT, null,
                                        replyingToMessage?.id,
                                        replyingToMessage?.senderName,
                                        replyingToMessage?.textContent?.ifEmpty { "Media" }
                                    )
                                    textInput = ""
                                    replyingToMessage = null
                                    onTypingStateChanged(false)
                                } else {
                                    startVoiceRecording()
                                }
                            },
                            containerColor = WhatsappGreen,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (textInput.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                contentDescription = if (textInput.isNotBlank()) "Send" else "Record Audio",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}
