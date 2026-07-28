package com.family.talkly.ui.components

import android.content.Context
import android.net.Uri
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

data class WallpaperItem(
    val id: String,
    val name: String,
    val colorHex: String? = null,
    val imageUrl: String? = null,
    val isGalleryOption: Boolean = false
)

val PRESET_WALLPAPERS = listOf(
    WallpaperItem("default", "Classic Gray", colorHex = "#E5DDD5"),
    WallpaperItem("mint", "Soft Mint", colorHex = "#E2F0D9"),
    WallpaperItem("sky", "Sky Blue", colorHex = "#D9E2EC"),
    WallpaperItem("sand", "Warm Sand", colorHex = "#F7EBE1"),
    WallpaperItem("rose", "Soft Rose", colorHex = "#FCE4EC"),
    WallpaperItem("dark", "Midnight Slate", colorHex = "#0B141A"),
    WallpaperItem("mountain", "Mountain Mist", imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=600&q=80"),
    WallpaperItem("sunset", "Ocean Sunset", imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80"),
    WallpaperItem("galaxy", "Starry Galaxy", imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80"),
    WallpaperItem("emerald", "Emerald Forest", imageUrl = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=600&q=80"),
    WallpaperItem("gallery", "Choose Gallery Photo", isGalleryOption = true)
)

@Composable
fun WallpaperSelectionDialog(
    currentValue: String,
    contactName: String,
    onDismiss: () -> Unit,
    onWallpaperSelected: (value: String, applyToAll: Boolean) -> Unit
) {
    var selectedValue by remember { mutableStateOf(currentValue) }
    var applyToAllChats by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedValue = it.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = null,
                            tint = WhatsappTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Chat Wallpaper",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111B21)
                            )
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Preview Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                        // Background Render
                        when {
                            selectedValue.startsWith("http://") ||
                            selectedValue.startsWith("https://") ||
                            selectedValue.startsWith("content://") ||
                            selectedValue.startsWith("file://") -> {
                                AsyncImage(
                                    model = selectedValue,
                                    contentDescription = "Wallpaper Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(120.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .background(Color.Black.copy(alpha = 0.15f))
                                )
                            }
                            selectedValue.startsWith("#") -> {
                                val c = try {
                                    Color(android.graphics.Color.parseColor(selectedValue))
                                } catch (e: Exception) {
                                    Color(0xFFE5DDD5)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .background(c)
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .background(Color(0xFFE5DDD5))
                                )
                            }
                        }

                        // Preview Message Bubbles
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.Center)
                        ) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    text = "Hey! How does this wallpaper look?",
                                    fontSize = 11.sp,
                                    color = Color(0xFF111B21),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Color(0xFFE7FFDB),
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 1.dp,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "Looks great! Perfect readability 👍",
                                    fontSize = 11.sp,
                                    color = Color(0xFF111B21),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Background Theme or Photo",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Wallpaper Grid Options
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    items(PRESET_WALLPAPERS) { item ->
                        val isSelected = when {
                            item.isGalleryOption -> selectedValue.startsWith("content://") || selectedValue.startsWith("file://")
                            item.imageUrl != null -> selectedValue == item.imageUrl
                            item.colorHex != null -> selectedValue == item.colorHex
                            else -> selectedValue == "default" || selectedValue == "#E5DDD5"
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) WhatsappGreen else Color.LightGray.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    if (item.isGalleryOption) {
                                        galleryLauncher.launch("image/*")
                                    } else if (item.imageUrl != null) {
                                        selectedValue = item.imageUrl
                                    } else if (item.colorHex != null) {
                                        selectedValue = item.colorHex
                                    } else {
                                        selectedValue = "#E5DDD5"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.isGalleryOption) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF0F2F5))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Gallery",
                                        tint = WhatsappTeal,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Gallery", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WhatsappTeal)
                                }
                            } else if (item.imageUrl != null) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(64.dp)
                                )
                            } else if (item.colorHex != null) {
                                val col = try {
                                    Color(android.graphics.Color.parseColor(item.colorHex))
                                } catch (e: Exception) {
                                    Color(0xFFE5DDD5)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .background(col)
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(WhatsappGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Apply Scope Choice (This chat vs All chats)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyToAllChats = false }
                    ) {
                        RadioButton(
                            selected = !applyToAllChats,
                            onClick = { applyToAllChats = false },
                            colors = RadioButtonDefaults.colors(selectedColor = WhatsappGreen)
                        )
                        Text(
                            text = "Apply to this chat ($contactName)",
                            fontSize = 12.sp,
                            color = Color(0xFF111B21)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyToAllChats = true }
                    ) {
                        RadioButton(
                            selected = applyToAllChats,
                            onClick = { applyToAllChats = true },
                            colors = RadioButtonDefaults.colors(selectedColor = WhatsappGreen)
                        )
                        Text(
                            text = "Set as default for all chats",
                            fontSize = 12.sp,
                            color = Color(0xFF111B21)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            selectedValue = "#E5DDD5"
                        }
                    ) {
                        Text("Reset Default", color = Color.Gray, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onWallpaperSelected(selectedValue, applyToAllChats)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set Wallpaper", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
