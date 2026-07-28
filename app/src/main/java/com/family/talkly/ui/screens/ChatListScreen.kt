@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.family.talkly.ui.screens

import com.family.talkly.util.ImageUtils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import com.family.talkly.ui.theme.LocalIsDarkTheme
import com.family.talkly.ui.theme.PrimaryDarkPurple
import com.family.talkly.ui.theme.SecondaryLightSage
import com.family.talkly.ui.theme.ThemeMode
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal
import com.family.talkly.data.models.CallType
import com.family.talkly.data.models.ChatMessage
import com.family.talkly.data.models.DEFAULT_FAMILY_MEMBERS
import com.family.talkly.data.models.FamilyMember
import com.family.talkly.data.models.UserProfile
import androidx.compose.material.icons.filled.Block
import com.family.talkly.ui.components.BlockedContactsDialog
import com.family.talkly.ui.components.ContactProfileDetailsDialog
import com.family.talkly.ui.components.UserProfileDetailsDialog
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal

import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.mutableIntStateOf
import com.family.talkly.data.models.StatusItem
import com.family.talkly.data.models.UserStatusGroup
import com.family.talkly.ui.components.PostStatusDialog
import com.family.talkly.ui.components.StatusViewerDialog
import com.family.talkly.ui.components.AddContactDialog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(
    familyMembers: List<FamilyMember>,
    messagesMap: Map<String, List<ChatMessage>>,
    simulatedTimeOffsetMs: Long,
    currentUserProfile: UserProfile? = null,
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: ((ThemeMode) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onSaveProfile: ((name: String, bio: String, photoUrl: String) -> Unit)? = null,
    onSelectMember: (FamilyMember) -> Unit,
    onStartCall: (FamilyMember, CallType) -> Unit,
    onTriggerIncomingDemo: (FamilyMember) -> Unit,
    onTogglePinMember: ((String) -> Unit)? = null,
    onSearchUserByPhone: ((phone: String, onResult: (UserProfile?) -> Unit) -> Unit)? = null,
    onAddContact: ((name: String, phone: String, relation: String, bio: String, avatarUrl: String?) -> Unit)? = null,
    onDeleteContact: ((String) -> Unit)? = null,
    onDeleteChatHistory: ((String) -> Unit)? = null,
    onClearDemoContacts: (() -> Unit)? = null,
    statusGroups: List<UserStatusGroup> = emptyList(),
    onPostStatus: ((textContent: String?, photoUrl: String?, backgroundColorHex: String) -> Unit)? = null,
    onMarkStatusSeen: ((statusId: String) -> Unit)? = null,
    onToggleLikeStatus: ((statusId: String) -> Unit)? = null,
    onSendStatusReply: ((targetUserId: String, replyText: String) -> Unit)? = null,
    blockedUserIds: Set<String> = emptySet(),
    onBlockUser: ((String) -> Unit)? = null,
    onUnblockUser: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedHeaderTab by remember { mutableIntStateOf(0) } // 0: Chats, 1: Saved Contacts
    var showMenu by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showBlockedContactsDialog by remember { mutableStateOf(false) }
    var showPostStatusDialog by remember { mutableStateOf(false) }
    var activeViewerGroupIndex by remember { mutableStateOf<Int?>(null) }
    var selectedContactForProfile by remember { mutableStateOf<FamilyMember?>(null) }
    var memberToDeleteHistory by remember { mutableStateOf<FamilyMember?>(null) }

    val isDark = LocalIsDarkTheme.current

    if (showAddContactDialog && onSearchUserByPhone != null && onAddContact != null) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onSearchUserByPhone = { phone, callback ->
                onSearchUserByPhone(phone, callback)
            },
            onAddContact = { name, phone, relation, bio, avatarUrl ->
                onAddContact(name, phone, relation, bio, avatarUrl)
            }
        )
    }

    if (showBlockedContactsDialog) {
        BlockedContactsDialog(
            allMembers = familyMembers,
            blockedUserIds = blockedUserIds,
            onDismiss = { showBlockedContactsDialog = false },
            onUnblockUser = { id -> onUnblockUser?.invoke(id) },
            onBlockUser = { id -> onBlockUser?.invoke(id) }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = WhatsappTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose App Theme", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    val options = listOf(
                        Triple(ThemeMode.LIGHT, "Light Mode ☀️", "Bright visual theme"),
                        Triple(ThemeMode.DARK, "Dark Mode 🌙", "Eye-safe dark canvas"),
                        Triple(ThemeMode.SYSTEM, "System Default 📱", "Match device system setting")
                    )
                    options.forEach { (mode, label, subtitle) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeModeChange?.invoke(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentThemeMode == mode),
                                onClick = {
                                    onThemeModeChange?.invoke(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = WhatsappTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = label,
                                    fontWeight = if (currentThemeMode == mode) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close", color = WhatsappTeal, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showProfileDialog && currentUserProfile != null) {
        UserProfileDetailsDialog(
            userProfile = currentUserProfile,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { name, bio, photoUrl ->
                onSaveProfile?.invoke(name, bio, photoUrl)
            },
            onLogout = onLogout
        )
    }

    if (selectedContactForProfile != null) {
        ContactProfileDetailsDialog(
            member = selectedContactForProfile!!,
            onDismiss = { selectedContactForProfile = null },
            onStartChat = { member ->
                selectedContactForProfile = null
                onSelectMember(member)
            },
            onStartCall = { member, callType ->
                selectedContactForProfile = null
                onStartCall(member, callType)
            },
            onDeleteContact = { id ->
                selectedContactForProfile = null
                onDeleteContact?.invoke(id)
            }
        )
    }

    if (memberToDeleteHistory != null) {
        AlertDialog(
            onDismissRequest = { memberToDeleteHistory = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F)
                )
            },
            title = {
                Text(
                    text = "Delete Chat History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all chat history with ${memberToDeleteHistory?.name}? This action cannot be undone.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetId = memberToDeleteHistory?.id
                        if (targetId != null) {
                            onDeleteChatHistory?.invoke(targetId)
                            Toast.makeText(context, "Chat history deleted", Toast.LENGTH_SHORT).show()
                        }
                        memberToDeleteHistory = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDeleteHistory = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showPostStatusDialog) {
        PostStatusDialog(
            userUid = currentUserProfile?.uid ?: "self",
            onDismiss = { showPostStatusDialog = false },
            onPostStatus = { text, photoUrl, bgHex ->
                onPostStatus?.invoke(text, photoUrl, bgHex)
                Toast.makeText(context, "Status shared! Disappears in 24 hours.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (activeViewerGroupIndex != null && statusGroups.isNotEmpty()) {
        StatusViewerDialog(
            statusGroups = statusGroups,
            initialGroupIndex = activeViewerGroupIndex!!,
            onDismiss = { activeViewerGroupIndex = null },
            onMarkStatusSeen = { statusId -> onMarkStatusSeen?.invoke(statusId) },
            onAddStatusClick = { showPostStatusDialog = true },
            onToggleLikeStatus = { statusId -> onToggleLikeStatus?.invoke(statusId) },
            onSendStatusReply = { targetUserId, replyText -> onSendStatusReply?.invoke(targetUserId, replyText) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // User Avatar Profile on Left
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SecondaryLightSage)
                                .clickable { showProfileDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentUserProfile?.profilePicUrl?.isNotBlank() == true) {
                                AsyncImage(
                                    model = ImageUtils.getProfileImageModel(currentUserProfile.profilePicUrl),
                                    contentDescription = "My Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = (currentUserProfile?.name?.take(1) ?: "U").uppercase(),
                                    color = PrimaryDarkPurple,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        // Middle Header Tabs (Chat & Contacts)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(80.dp)
                        ) {
                            // Chat Tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedHeaderTab = 0 }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Chats Tab",
                                    tint = if (selectedHeaderTab == 0) SecondaryLightSage else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (selectedHeaderTab == 0) {
                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(3.dp)
                                            .background(SecondaryLightSage, RoundedCornerShape(2.dp))
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                            }

                            // Contacts Tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedHeaderTab = 1 }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = "Contacts Tab",
                                    tint = if (selectedHeaderTab == 1) SecondaryLightSage else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (selectedHeaderTab == 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(3.dp)
                                            .background(SecondaryLightSage, RoundedCornerShape(2.dp))
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(36.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = SecondaryLightSage
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add New Contact") },
                            leadingIcon = {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PrimaryDarkPurple)
                            },
                            onClick = {
                                showMenu = false
                                showAddContactDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Blocked Contacts") },
                            leadingIcon = {
                                Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFD32F2F))
                            },
                            onClick = {
                                showMenu = false
                                showBlockedContactsDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Demo Contacts") },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Gray)
                            },
                            onClick = {
                                showMenu = false
                                onClearDemoContacts?.invoke()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("App Theme (${currentThemeMode.name})") },
                            leadingIcon = {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = PrimaryDarkPurple)
                            },
                            onClick = {
                                showMenu = false
                                showThemeDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("My Profile & Phone") },
                            leadingIcon = {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryDarkPurple)
                            },
                            onClick = {
                                showMenu = false
                                showProfileDialog = true
                            }
                        )
                        if (onLogout != null) {
                            DropdownMenuItem(
                                text = { Text("Log Out Session", color = Color.Red) },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red)
                                },
                                onClick = {
                                    showMenu = false
                                    onLogout()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkPurple)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PrimaryDarkPurple)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (selectedHeaderTab == 0) {
                    // Family Quick Status / Stories Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryDarkPurple)
                            .padding(vertical = 12.dp)
                    ) {
                        val selfGroup = statusGroups.firstOrNull { it.userId == "self" }
                        val hasMyStatus = selfGroup != null && selfGroup.statuses.isNotEmpty()
                        val contactGroups = remember(statusGroups) {
                            statusGroups
                                .filter { it.userId != "self" && it.statuses.isNotEmpty() }
                                .sortedByDescending { it.hasUnseen }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "My Status" item
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        if (hasMyStatus) {
                                            val idx = statusGroups.indexOf(selfGroup)
                                            if (idx >= 0) activeViewerGroupIndex = idx
                                        } else {
                                            showPostStatusDialog = true
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier.size(62.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryDarkPurple)
                                                .border(
                                                    width = if (hasMyStatus) 2.5.dp else 1.5.dp,
                                                    color = SecondaryLightSage,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (currentUserProfile?.profilePicUrl?.isNotBlank() == true) {
                                                AsyncImage(
                                                    model = currentUserProfile.profilePicUrl,
                                                    contentDescription = "My Status",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text(
                                                    text = (currentUserProfile?.name?.take(1) ?: "U").uppercase(),
                                                    color = SecondaryLightSage,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                        // Plus Badge for posting status
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(SecondaryLightSage, CircleShape)
                                                .border(1.5.dp, PrimaryDarkPurple, CircleShape)
                                                .clickable { showPostStatusDialog = true }
                                                .align(Alignment.BottomEnd),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add status",
                                                tint = PrimaryDarkPurple,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "My Status",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Contact Active Statuses (sorted: unseen first, active only)
                            items(contactGroups) { group ->
                                val member = familyMembers.firstOrNull { it.id == group.userId }
                                val greenGlow = Color(0xFF00FF66) // Vibrant green light ring
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        val idx = statusGroups.indexOf(group)
                                        if (idx >= 0) activeViewerGroupIndex = idx
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier.size(62.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (group.hasUnseen) {
                                            // Glowing soft background ring
                                            Box(
                                                modifier = Modifier
                                                    .size(62.dp)
                                                    .background(greenGlow.copy(alpha = 0.25f), CircleShape)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryDarkPurple)
                                                .border(
                                                    width = if (group.hasUnseen) 3.dp else 1.dp,
                                                    color = if (group.hasUnseen) greenGlow else Color.White.copy(alpha = 0.3f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (group.userAvatarUrl != null || member?.avatarUrl != null) {
                                                AsyncImage(
                                                    model = ImageUtils.getProfileImageModel(group.userAvatarUrl ?: member?.avatarUrl),
                                                    contentDescription = group.userName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text(
                                                    text = group.userName.take(2).uppercase(),
                                                    color = SecondaryLightSage,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                        if (group.hasUnseen) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(greenGlow, CircleShape)
                                                    .border(1.5.dp, PrimaryDarkPurple, CircleShape)
                                                    .align(Alignment.TopEnd)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = group.userName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (group.hasUnseen) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = if (group.hasUnseen) greenGlow else Color.White.copy(alpha = 0.7f)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.width(62.dp)
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = SecondaryLightSage.copy(alpha = 0.2f))

                    // Family Conversations List
                    val sortedMembers = remember(familyMembers, messagesMap) {
                        familyMembers.sortedWith(
                            compareByDescending<FamilyMember> { it.isPinned }
                                .thenByDescending { member ->
                                    messagesMap[member.id]?.lastOrNull()?.timestamp ?: 0L
                                }
                        )
                    }

                    if (sortedMembers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = SecondaryLightSage,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No active chats yet",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(sortedMembers) { member ->
                                val memberMessages = messagesMap[member.id] ?: emptyList()
                                val lastMessage = memberMessages.lastOrNull()

                                FamilyChatRow(
                                    member = member,
                                    lastMessage = lastMessage,
                                    simulatedTimeOffsetMs = simulatedTimeOffsetMs,
                                    isDarkTheme = isDark,
                                    onClick = { onSelectMember(member) },
                                    onLongClick = { memberToDeleteHistory = member },
                                    onAvatarClick = { selectedContactForProfile = member },
                                    onAudioCall = { onStartCall(member, CallType.AUDIO) },
                                    onVideoCall = { onStartCall(member, CallType.VIDEO) }
                                )
                                Divider(
                                    color = SecondaryLightSage.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(start = 76.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Saved Contacts Interface
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // Header card for Saved Contacts
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = PrimaryDarkPurple.copy(alpha = if (isDark) 0.3f else 0.08f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(PrimaryDarkPurple, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.People,
                                            contentDescription = null,
                                            tint = SecondaryLightSage,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Saved Contacts",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${familyMembers.size} contacts available",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showAddContactDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryDarkPurple,
                                        contentColor = SecondaryLightSage
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Add Contact",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Contacts List
                        if (familyMembers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = SecondaryLightSage,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No saved contacts yet",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showAddContactDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SecondaryLightSage,
                                            contentColor = PrimaryDarkPurple
                                        )
                                    ) {
                                        Text("Add First Contact", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                items(familyMembers) { member ->
                                    val isUserBlocked = blockedUserIds.contains(member.id)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { selectedContactForProfile = member },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isUserBlocked) {
                                                Color(0xFFD32F2F).copy(alpha = if (isDark) 0.15f else 0.08f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        border = if (isUserBlocked) {
                                            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f))
                                        } else null
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Avatar
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryDarkPurple),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (member.avatarUrl?.isNotBlank() == true) {
                                                    AsyncImage(
                                                        model = member.avatarUrl,
                                                        contentDescription = member.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Text(
                                                        text = member.name.take(2).uppercase(),
                                                        color = SecondaryLightSage,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                                // Online dot badge if not blocked and active within 45 mins
                                                if (member.isRecentlyActive && !isUserBlocked) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .background(WhatsappGreen, CircleShape)
                                                            .border(1.5.dp, PrimaryDarkPurple, CircleShape)
                                                            .align(Alignment.BottomEnd)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Name & Relationship/Phone info
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = member.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (isUserBlocked) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .background(
                                                                    Color(0xFFD32F2F).copy(alpha = 0.15f),
                                                                    RoundedCornerShape(6.dp)
                                                                )
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "Blocked",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFD32F2F)
                                                            )
                                                        }
                                                    } else if (member.relation.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .background(
                                                                    SecondaryLightSage.copy(alpha = 0.2f),
                                                                    RoundedCornerShape(6.dp)
                                                                )
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = member.relation,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = PrimaryDarkPurple
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (member.phone.isNotBlank()) member.phone else member.status,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Quick Action Buttons or Unblock Button
                                            if (isUserBlocked) {
                                                TextButton(
                                                    onClick = { onUnblockUser?.invoke(member.id) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Unblock",
                                                        color = WhatsappTeal,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = { onSelectMember(member) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Chat,
                                                            contentDescription = "Chat",
                                                            tint = PrimaryDarkPurple,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { onStartCall(member, CallType.AUDIO) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Call,
                                                            contentDescription = "Audio Call",
                                                            tint = PrimaryDarkPurple,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { onStartCall(member, CallType.VIDEO) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Videocam,
                                                            contentDescription = "Video Call",
                                                            tint = PrimaryDarkPurple,
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
                    }
                }
            }

            // Dual Floating Action Buttons matching layout requirements
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Bottom-Left Plus (+) FAB
                FloatingActionButton(
                    onClick = { showAddContactDialog = true },
                    containerColor = SecondaryLightSage,
                    contentColor = PrimaryDarkPurple,
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Contact"
                    )
                }

                // Bottom-Right Search FAB
                FloatingActionButton(
                    onClick = { showAddContactDialog = true },
                    containerColor = SecondaryLightSage,
                    contentColor = PrimaryDarkPurple,
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }
            }
        }
    }
}

@Composable
private fun FamilyMemberAvatarStory(
    member: FamilyMember,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.size(62.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PrimaryDarkPurple)
                    .border(1.5.dp, SecondaryLightSage, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (member.avatarUrl != null) {
                    AsyncImage(
                        model = member.avatarUrl,
                        contentDescription = member.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = member.name.take(2).uppercase(),
                        color = SecondaryLightSage,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            // Sage Green badge for unread story count
            if (member.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(SecondaryLightSage, CircleShape)
                        .border(1.5.dp, PrimaryDarkPurple, CircleShape)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.unreadCount.toString(),
                        color = PrimaryDarkPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = member.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color.White
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FamilyChatRow(
    member: FamilyMember,
    lastMessage: ChatMessage?,
    simulatedTimeOffsetMs: Long,
    isDarkTheme: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(PrimaryDarkPurple)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with real-time Online/Offline indicator badge
        Box(
            modifier = Modifier
                .size(54.dp)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PrimaryDarkPurple)
                    .border(1.5.dp, SecondaryLightSage.copy(alpha = 0.5f), CircleShape),
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
                        color = SecondaryLightSage,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
            // Online Status Checkmark / Dot Indicator (visible if active within 45 mins)
            if (member.isRecentlyActive) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(0xFF25D366), CircleShape)
                        .border(1.5.dp, PrimaryDarkPurple, CircleShape)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Online",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            } else {
                // Offline dotted/dashed grey indicator ring
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(1.5.dp, SecondaryLightSage.copy(alpha = 0.5f), CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center Chat Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (member.isPinned) {
                        Text(text = "📌 ", fontSize = 14.sp)
                    }
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val displayTime = if (lastMessage != null) lastMessage.formattedTime else member.lastSeen
                Text(
                    text = if (member.isTyping) "typing..." else displayTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (member.isTyping) SecondaryLightSage else Color(0xFFB0BEC5),
                        fontWeight = if (member.isTyping) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (member.isTyping) {
                    Text(
                        text = "typing...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SecondaryLightSage,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    val rawText = lastMessage?.textContent ?: member.status
                    val isMissedCall = rawText.contains("Missed", ignoreCase = true)

                    if (isMissedCall) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallMissed,
                                contentDescription = "Missed Call",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = rawText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFFEF5350),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        val previewText = when {
                            lastMessage != null && lastMessage.isMediaExpired(simulatedTimeOffsetMs) -> "⚠️ Media expired after 48h"
                            lastMessage?.mediaUrl != null -> "📷 Photo / Media"
                            lastMessage != null -> lastMessage.textContent
                            else -> member.status
                        }

                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (lastMessage?.isMediaExpired(simulatedTimeOffsetMs) == true) Color(0xFFFFD54F) else Color(0xFFB0BEC5),
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (member.unreadCount > 0 && !member.isTyping) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(SecondaryLightSage, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.unreadCount.toString(),
                            color = PrimaryDarkPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Phone Call Action Button in Sage Green (#ACC7B4)
        IconButton(
            onClick = onAudioCall,
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Audio Call",
                tint = SecondaryLightSage,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
