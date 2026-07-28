package com.family.talkly.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.family.talkly.data.models.UserProfile
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onSearchUserByPhone: (phone: String, onResult: (UserProfile?) -> Unit) -> Unit,
    onAddContact: (name: String, phone: String, relation: String, bio: String, avatarUrl: String?) -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var relationInput by remember { mutableStateOf("Family Member") }
    var bioInput by remember { mutableStateOf("Available on Talkly 💬") }
    var avatarUrlInput by remember { mutableStateOf<String?>(null) }

    var isSearching by remember { mutableStateOf(false) }
    var searchStatusMessage by remember { mutableStateOf<String?>(null) }
    var foundUser by remember { mutableStateOf<UserProfile?>(null) }
    var showRelationDropdown by remember { mutableStateOf(false) }

    val relationOptions = listOf("Family Member", "Mother", "Father", "Brother", "Sister", "Grandma", "Grandpa", "Friend", "Spouse", "Other")

    fun triggerUserSearch() {
        val cleanPhone = phoneInput.trim()
        if (cleanPhone.length < 5) {
            searchStatusMessage = "Please enter a valid phone number"
            return
        }

        isSearching = true
        searchStatusMessage = "Searching Talkly database..."
        foundUser = null

        onSearchUserByPhone(cleanPhone) { profile ->
            isSearching = false
            if (profile != null) {
                foundUser = profile
                nameInput = profile.name
                bioInput = profile.bio.ifBlank { "Available on Talkly 💬" }
                avatarUrlInput = profile.profilePicUrl.ifBlank { null }
                searchStatusMessage = "✅ Talkly user found: ${profile.name}"
            } else {
                foundUser = null
                searchStatusMessage = "ℹ️ User not registered on Talkly yet. Contact will be saved as offline member."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(WhatsappTeal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add Talkly Contact",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = WhatsappTeal
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Found User Banner Card
                if (foundUser != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = WhatsappGreen.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(WhatsappTeal),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!foundUser!!.profilePicUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = foundUser!!.profilePicUrl,
                                        contentDescription = foundUser!!.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = foundUser!!.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = foundUser!!.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF111B21)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = WhatsappGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = foundUser!!.phoneNumber,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Phone Input Field with Search Action
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = {
                        phoneInput = it
                        searchStatusMessage = null
                    },
                    label = { Text("Phone Number / User Mobile") },
                    placeholder = { Text("+8801700000000") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = WhatsappTeal)
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = WhatsappTeal,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = { triggerUserSearch() }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = WhatsappTeal
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsappTeal,
                        focusedLabelColor = WhatsappTeal
                    )
                )

                if (searchStatusMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = searchStatusMessage!!,
                        fontSize = 12.sp,
                        color = if (foundUser != null) WhatsappGreen else Color.DarkGray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name Input Field
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Contact Name") },
                    placeholder = { Text("e.g. Brother Rahat") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, tint = WhatsappTeal)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsappTeal,
                        focusedLabelColor = WhatsappTeal
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Relation Selection Field
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = relationInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Relation / Tag") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.FamilyRestroom, contentDescription = null, tint = WhatsappTeal)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRelationDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = Color.LightGray,
                            disabledLabelColor = WhatsappTeal,
                            disabledTextColor = Color.Black
                        )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showRelationDropdown = true }
                    )

                    DropdownMenu(
                        expanded = showRelationDropdown,
                        onDismissRequest = { showRelationDropdown = false }
                    ) {
                        relationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    relationInput = option
                                    showRelationDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bio / Status Field
                OutlinedTextField(
                    value = bioInput,
                    onValueChange = { bioInput = it },
                    label = { Text("Bio / Status Note") },
                    placeholder = { Text("Available for call 💬") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsappTeal,
                        focusedLabelColor = WhatsappTeal
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleanName = nameInput.trim()
                            val cleanPhone = phoneInput.trim()
                            if (cleanName.isNotBlank() && cleanPhone.isNotBlank()) {
                                onAddContact(
                                    cleanName,
                                    cleanPhone,
                                    relationInput,
                                    bioInput,
                                    avatarUrlInput
                                )
                                onDismiss()
                            }
                        },
                        enabled = nameInput.isNotBlank() && phoneInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Contact", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
