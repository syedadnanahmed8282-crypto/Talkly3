package com.family.talkly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.family.talkly.data.models.CallType
import com.family.talkly.data.models.FamilyMember
import com.family.talkly.ui.components.ContactProfileDetailsDialog
import com.family.talkly.ui.theme.PrimaryDarkPurple
import com.family.talkly.ui.theme.SecondaryLightSage
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal
import com.family.talkly.util.ImageUtils

import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import com.family.talkly.data.models.UserProfile
import com.family.talkly.ui.components.AddContactDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMembersScreen(
    familyMembers: List<FamilyMember>,
    onSelectMember: (FamilyMember) -> Unit,
    onStartCall: (FamilyMember, CallType) -> Unit,
    onTogglePresence: (FamilyMember) -> Unit,
    onSearchUserByPhone: ((phone: String, onResult: (UserProfile?) -> Unit) -> Unit)? = null,
    onAddContact: ((name: String, phone: String, relation: String, bio: String, avatarUrl: String?) -> Unit)? = null,
    onDeleteContact: ((String) -> Unit)? = null,
    onClearDemoContacts: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedContactForProfile by remember { mutableStateOf<FamilyMember?>(null) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Family Contacts",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = { showAddContactDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Contact",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add New Contact") },
                            leadingIcon = {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = WhatsappGreen)
                            },
                            onClick = {
                                showMenu = false
                                showAddContactDialog = true
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkPurple)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddContactDialog = true },
                containerColor = SecondaryLightSage,
                contentColor = PrimaryDarkPurple,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Contact"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryDarkPurple.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(WhatsappTeal, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FamilyRestroom,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Private Family Circle",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${familyMembers.size} connected family members • Tap profile to view details",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                items(familyMembers) { member ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { selectedContactForProfile = member },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable { selectedContactForProfile = member },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(WhatsappTeal),
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
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(if (member.isRecentlyActive) Color(0xFF25D366) else Color.Gray, CircleShape)
                                        .border(2.dp, Color.White, CircleShape)
                                        .align(Alignment.BottomEnd)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = member.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = if (member.isOnline) WhatsappGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { onTogglePresence(member) }
                                    ) {
                                        Text(
                                            text = if (member.isTyping) "Typing..." else if (member.isOnline) "Online" else "Offline",
                                            color = if (member.isOnline) WhatsappGreen else Color.Gray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (!member.isRegisteredOnTalkly) "User not registered on Talkly" else member.status,
                                    fontSize = 13.sp,
                                    color = if (!member.isRegisteredOnTalkly) Color(0xFFD32F2F) else Color.Gray,
                                    maxLines = 1,
                                    fontWeight = if (!member.isRegisteredOnTalkly) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = member.phone,
                                    fontSize = 12.sp,
                                    color = Color.Gray.copy(alpha = 0.8f)
                                )
                            }

                            Row {
                                IconButton(onClick = {
                                    if (member.isRegisteredOnTalkly) {
                                        onSelectMember(member)
                                    } else {
                                        android.widget.Toast.makeText(context, "User not registered on Talkly", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = "Chat",
                                        tint = if (member.isRegisteredOnTalkly) WhatsappTeal else Color.Gray
                                    )
                                }
                                IconButton(onClick = {
                                    if (member.isRegisteredOnTalkly) {
                                        onStartCall(member, CallType.VIDEO)
                                    } else {
                                        android.widget.Toast.makeText(context, "User not registered on Talkly", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Video",
                                        tint = if (member.isRegisteredOnTalkly) WhatsappTeal else Color.Gray
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
