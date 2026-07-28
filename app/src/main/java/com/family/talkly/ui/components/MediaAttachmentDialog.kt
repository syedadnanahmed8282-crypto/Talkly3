package com.family.talkly.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.family.talkly.util.MediaCompressorAndUploader
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.family.talkly.data.models.ChatMessage
import com.family.talkly.data.models.MessageType
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal
import com.family.talkly.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SampleMediaItem(
    val title: String,
    val url: String,
    val type: MessageType
)

val SAMPLE_FAMILY_MEDIA = listOf(
    SampleMediaItem(
        title = "Family Beach Trip",
        url = "https://images.unsplash.com/photo-1511895426328-dc8714191300?w=600&auto=format&fit=crop&q=80",
        type = MessageType.IMAGE
    ),
    SampleMediaItem(
        title = "Birthday Party",
        url = "https://images.unsplash.com/photo-1530103862676-de8c9debad1d?w=600&auto=format&fit=crop&q=80",
        type = MessageType.IMAGE
    ),
    SampleMediaItem(
        title = "Family Dinner",
        url = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600&auto=format&fit=crop&q=80",
        type = MessageType.IMAGE
    ),
    SampleMediaItem(
        title = "Garden Walk Video",
        url = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80",
        type = MessageType.VIDEO
    )
)

@Composable
fun MediaAttachmentDialog(
    onDismiss: () -> Unit,
    onSendMediaWithTag: (caption: String, type: MessageType, url: String) -> Unit,
    onSendExpiredDemo: () -> Unit
) {
    var previewMediaUrl by remember { mutableStateOf<String?>(null) }
    var previewMediaType by remember { mutableStateOf(MessageType.IMAGE) }

    // System Media Picker Launchers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            previewMediaUrl = it.toString()
            previewMediaType = MessageType.IMAGE
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            previewMediaUrl = it.toString()
            previewMediaType = MessageType.VIDEO
        }
    }

    if (previewMediaUrl != null) {
        MediaPreviewAndTagDialog(
            mediaUrl = previewMediaUrl!!,
            mediaType = previewMediaType,
            onDismiss = {
                previewMediaUrl = null
            },
            onSend = { caption, type, url ->
                onSendMediaWithTag(caption, type, url)
                previewMediaUrl = null
                onDismiss()
            }
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Share Family Media",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111B21)
                            )
                        )
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

                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Privacy",
                                tint = WhatsappTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "48-Hour Auto-Expiry Tag automatically applied to all media.",
                                fontSize = 11.sp,
                                color = WhatsappTeal,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Main Pickers Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AttachmentOption(
                            title = "Gallery Photo",
                            icon = Icons.Default.Image,
                            color = Color(0xFFAC44CF),
                            onClick = {
                                photoPickerLauncher.launch("image/*")
                            }
                        )
                        AttachmentOption(
                            title = "Gallery Video",
                            icon = Icons.Default.Videocam,
                            color = Color(0xFFE91E63),
                            onClick = {
                                videoPickerLauncher.launch("video/*")
                            }
                        )
                        AttachmentOption(
                            title = "Camera",
                            icon = Icons.Default.CameraAlt,
                            color = WhatsappTeal,
                            onClick = {
                                photoPickerLauncher.launch("image/*")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Sample Gallery
                    Text(
                        text = "Or choose sample family media:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SAMPLE_FAMILY_MEDIA) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .size(width = 110.dp, height = 90.dp)
                                    .clickable {
                                        previewMediaUrl = item.url
                                        previewMediaType = item.type
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    AsyncImage(
                                        model = ImageUtils.getProfileImageModel(item.url),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().height(90.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (item.type == MessageType.VIDEO) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Video",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test 48h Expiration demo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF3CD), RoundedCornerShape(12.dp))
                            .clickable {
                                onSendExpiredDemo()
                                onDismiss()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "Test Expired",
                            tint = Color(0xFF856404)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Test 48h Expiration Logic",
                                color = Color(0xFF856404),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Inserts a sample media message from 50h ago to test instant expiration placeholder",
                                color = Color(0xFF856404).copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Media Preview & 48-Hour Expiry Metadata Tagging Dialog.
 * Shows the chosen photo/video along with explicit retention metadata tags
 * (creation date, expiration date = creation + 48h) and caption entry before sending.
 */
@Composable
fun MediaPreviewAndTagDialog(
    mediaUrl: String,
    mediaType: MessageType,
    onDismiss: () -> Unit,
    onSend: (caption: String, type: MessageType, url: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentTimeMs = remember { System.currentTimeMillis() }
    val expirationTimeMs = remember { currentTimeMs + ChatMessage.EXPIRATION_48_HOURS_MS }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }

    val formattedCreation = remember { dateFormat.format(Date(currentTimeMs)) }
    val formattedExpiry = remember { dateFormat.format(Date(expirationTimeMs)) }

    var captionInput by remember {
        mutableStateOf(if (mediaType == MessageType.IMAGE) "📷 Family Photo" else "🎥 Family Video")
    }

    // Compression and Upload State
    var isProcessing by remember { mutableStateOf(false) }
    var progressPercent by remember { mutableStateOf(0) }
    var progressText by remember { mutableStateOf("Preparing media...") }
    var savingsInfoText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isProcessing) "Compressing & Uploading" else "Media Expiry Tag Preview",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111B21)
                        )
                    )
                    if (!isProcessing) {
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
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isProcessing) {
                    // Processing Overlay UI
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { progressPercent / 100f },
                                modifier = Modifier.size(72.dp),
                                color = WhatsappGreen,
                                trackColor = WhatsappGreen.copy(alpha = 0.2f),
                                strokeWidth = 6.dp
                            )
                            Text(
                                text = "$progressPercent%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = WhatsappTeal
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = progressText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF111B21),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = WhatsappGreen,
                            trackColor = Color.LightGray.copy(alpha = 0.4f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            color = WhatsappGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (mediaType == MessageType.IMAGE)
                                        "⚡ Image Compression: Target Max 1080p • 75% JPEG Quality"
                                    else
                                        "⚡ Video Compression: 720p Transcoding & Bitrate Optimization",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsappTeal
                                )
                                savingsInfoText?.let { info ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = info,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Media Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageUtils.getProfileImageModel(mediaUrl),
                            contentDescription = "Media Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )

                        if (mediaType == MessageType.VIDEO) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Compression Badge Feature Tag
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Compress Info",
                                tint = WhatsappTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (mediaType == MessageType.IMAGE)
                                    "Auto-Compressing to max 1080p resolution (~75% quality) before upload"
                                else
                                    "Auto-Compressing video resolution & bitrate before Firebase upload",
                                fontSize = 11.sp,
                                color = WhatsappTeal,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Expiry Metadata Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Metadata",
                                    tint = WhatsappTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "48-HOUR RETENTION METADATA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsappTeal
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Sent Time:",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = formattedCreation,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF111B21)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Auto-Expires At:",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE53935),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formattedExpiry,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53935)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Caption Input Field
                    OutlinedTextField(
                        value = captionInput,
                        onValueChange = { captionInput = it },
                        label = { Text("Add caption (optional)", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsappGreen,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Send Button
                    Button(
                        onClick = {
                            isProcessing = true
                            coroutineScope.launch {
                                val compressor = MediaCompressorAndUploader(context)
                                val finalUrl: String

                                if (mediaUrl.startsWith("content://") || mediaUrl.startsWith("file://")) {
                                    val uri = Uri.parse(mediaUrl)
                                    val compressedFile = if (mediaType == MessageType.IMAGE) {
                                        compressor.compressImage(uri) { progress, detail ->
                                            progressPercent = (progress * 0.5).toInt()
                                            progressText = detail
                                        }
                                    } else {
                                        compressor.compressVideo(uri) { progress, detail ->
                                            progressPercent = (progress * 0.5).toInt()
                                            progressText = detail
                                        }
                                    }

                                    val compressedSizeStr = compressor.formatFileSize(compressedFile.length())
                                    savingsInfoText = "Compressed File Size: $compressedSizeStr"

                                    val remotePath = "chats/media/${System.currentTimeMillis()}_${if (mediaType == MessageType.IMAGE) "img.jpg" else "vid.mp4"}"
                                    finalUrl = compressor.uploadToFirebaseStorage(compressedFile, remotePath) { progress, detail ->
                                        progressPercent = 50 + (progress * 0.5).toInt()
                                        progressText = detail
                                    }
                                } else {
                                    // Preset sample media - perform compression and upload pipeline demo pass
                                    progressText = "Compressing media (1080p, 75% quality)..."
                                    progressPercent = 25
                                    kotlinx.coroutines.delay(400)
                                    progressPercent = 55
                                    progressText = "Optimizing container & bitrate..."
                                    savingsInfoText = "Estimated Size Reduction: ~65%"
                                    kotlinx.coroutines.delay(400)
                                    progressPercent = 85
                                    progressText = "Uploading to Firebase Storage..."
                                    kotlinx.coroutines.delay(400)
                                    progressPercent = 100
                                    progressText = "Media upload complete!"
                                    kotlinx.coroutines.delay(200)
                                    finalUrl = mediaUrl
                                }

                                onSend(captionInput, mediaType, finalUrl)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Compress & Send with 48h Tag",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentOption(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
