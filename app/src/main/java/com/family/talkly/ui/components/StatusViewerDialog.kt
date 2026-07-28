package com.family.talkly.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.family.talkly.data.models.StatusItem
import com.family.talkly.data.models.UserStatusGroup
import com.family.talkly.ui.theme.PrimaryDarkPurple
import com.family.talkly.ui.theme.SecondaryLightSage
import com.family.talkly.util.ImageUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusViewerDialog(
    statusGroups: List<UserStatusGroup>,
    initialGroupIndex: Int = 0,
    onDismiss: () -> Unit,
    onMarkStatusSeen: (statusId: String) -> Unit,
    onAddStatusClick: (() -> Unit)? = null,
    onToggleLikeStatus: ((statusId: String) -> Unit)? = null,
    onSendStatusReply: ((targetUserId: String, replyText: String) -> Unit)? = null
) {
    if (statusGroups.isEmpty()) {
        onDismiss()
        return
    }

    val context = LocalContext.current
    var groupIndex by remember { mutableIntStateOf(initialGroupIndex.coerceIn(0, statusGroups.lastIndex)) }
    val currentGroup = statusGroups[groupIndex]

    var statusIndex by remember { mutableIntStateOf(0) }
    val currentStatus = currentGroup.statuses.getOrNull(statusIndex) ?: currentGroup.statuses.first()

    var progress by remember(groupIndex, statusIndex) { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var isLikedByMe by remember(currentStatus.id) {
        mutableStateOf(currentStatus.likes.any { it.userId == "self" })
    }

    // Mark current status as seen
    LaunchedEffect(currentStatus.id) {
        onMarkStatusSeen(currentStatus.id)
    }

    // Timer loop for auto-advancing progress
    LaunchedEffect(groupIndex, statusIndex, isPaused, showAnalyticsDialog) {
        if (!isPaused && !showAnalyticsDialog) {
            val totalSteps = 100
            val stepDelay = 50L // 5 seconds total (100 * 50ms)
            while (progress < 1f) {
                delay(stepDelay)
                progress += 1f / totalSteps
            }
            // Auto advance
            if (statusIndex < currentGroup.statuses.lastIndex) {
                statusIndex++
            } else if (groupIndex < statusGroups.lastIndex) {
                groupIndex++
                statusIndex = 0
            } else {
                onDismiss()
            }
        }
    }

    fun formatStatusTime(timestamp: Long): String {
        val diffMinutes = (System.currentTimeMillis() - timestamp) / (60 * 1000)
        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffMinutes < 24 * 60 -> "${diffMinutes / 60}h ago"
            else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val bgColor = try {
            Color(android.graphics.Color.parseColor(currentStatus.backgroundColorHex))
        } catch (e: Exception) {
            PrimaryDarkPurple
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.3f) {
                                // Tap left -> previous
                                if (statusIndex > 0) {
                                    statusIndex--
                                } else if (groupIndex > 0) {
                                    groupIndex--
                                    statusIndex = statusGroups[groupIndex].statuses.lastIndex
                                }
                            } else {
                                // Tap right -> next
                                if (statusIndex < currentGroup.statuses.lastIndex) {
                                    statusIndex++
                                } else if (groupIndex < statusGroups.lastIndex) {
                                    groupIndex++
                                    statusIndex = 0
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            // Background Photo Status
            if (currentStatus.photoUrl != null) {
                AsyncImage(
                    model = ImageUtils.getProfileImageModel(currentStatus.photoUrl),
                    contentDescription = "Status image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Bottom Gradient Overlay for text readability & interaction controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                )
            }

            // Main Text Content Display
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 110.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentStatus.photoUrl == null) {
                    Text(
                        text = currentStatus.textContent ?: "",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 24.sp,
                            lineHeight = 32.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                } else if (!currentStatus.textContent.isNull0rBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 70.dp)
                    ) {
                        Text(
                            text = currentStatus.textContent ?: "",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Top Progress Bars & User Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Progress Bar Segments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentGroup.statuses.forEachIndexed { idx, item ->
                        val itemProgress = when {
                            idx < statusIndex -> 1f
                            idx == statusIndex -> progress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = itemProgress,
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = SecondaryLightSage,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // User Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(PrimaryDarkPurple)
                                .border(1.5.dp, SecondaryLightSage, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentGroup.userAvatarUrl != null) {
                                AsyncImage(
                                    model = currentGroup.userAvatarUrl,
                                    contentDescription = currentGroup.userName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = currentGroup.userName.take(2).uppercase(),
                                    color = SecondaryLightSage,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = currentGroup.userName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            )
                            Text(
                                text = formatStatusTime(currentStatus.timestamp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentGroup.userId == "self" && onAddStatusClick != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(SecondaryLightSage)
                                    .clickable {
                                        onDismiss()
                                        onAddStatusClick()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add status",
                                        tint = PrimaryDarkPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Add More",
                                        color = PrimaryDarkPurple,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close status",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Actions & Analytics Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                if (currentGroup.userId == "self") {
                    // Own Status: Viewers & Loves Count Pill Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .border(1.dp, SecondaryLightSage.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .clickable {
                                showAnalyticsDialog = true
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Views",
                                tint = SecondaryLightSage,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentStatus.viewers.size} Views",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Likes",
                                tint = Color(0xFFFF2D55),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentStatus.likes.size} Loves",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // Other User's Status: Love Reaction + Message Reply Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Love Reaction Button
                        IconButton(
                            onClick = {
                                 isLikedByMe = !isLikedByMe
                                onToggleLikeStatus?.invoke(currentStatus.id)
                                val msg = if (isLikedByMe) "Heart reaction added ❤️" else "Reaction removed"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Love Reaction",
                                tint = if (isLikedByMe) Color(0xFFFF2D55) else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Message Reply Box
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Send message or comment...", color = Color.White.copy(0.6f)) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SecondaryLightSage,
                                unfocusedBorderColor = Color.White.copy(0.4f),
                                focusedContainerColor = Color.Black.copy(0.5f),
                                unfocusedContainerColor = Color.Black.copy(0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            trailingIcon = {
                                if (replyText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            if (replyText.isNotBlank()) {
                                                onSendStatusReply?.invoke(currentGroup.userId, replyText)
                                                Toast.makeText(context, "Message sent! 📩", Toast.LENGTH_SHORT).show()
                                                replyText = ""
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send Reply",
                                            tint = SecondaryLightSage
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Own Status Viewers & Reactions Analytics Dialog
            if (showAnalyticsDialog) {
                StatusAnalyticsModal(
                    status = currentStatus,
                    onDismiss = { showAnalyticsDialog = false }
                )
            }
        }
    }
}

@Composable
private fun StatusAnalyticsModal(
    status: StatusItem,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Viewers, 1: Likes

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryDarkPurple)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Story Analytics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = SecondaryLightSage,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SecondaryLightSage,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text("Viewed by (${status.viewers.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        },
                        selectedContentColor = SecondaryLightSage,
                        unselectedContentColor = Color.White.copy(0.6f)
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text("Likes (${status.likes.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        },
                        selectedContentColor = SecondaryLightSage,
                        unselectedContentColor = Color.White.copy(0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    if (status.viewers.isEmpty()) {
                        Text(
                            text = "No views yet",
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(24.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.height(200.dp)
                        ) {
                            items(status.viewers) { viewer ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(SecondaryLightSage),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = viewer.userName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDarkPurple
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = viewer.userName,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = viewer.timeAgo,
                                            color = Color.White.copy(0.6f),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = SecondaryLightSage,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (status.likes.isEmpty()) {
                        Text(
                            text = "No reactions yet",
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(24.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.height(200.dp)
                        ) {
                            items(status.likes) { liker ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF2D55)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = liker.userName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = liker.userName,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFFF2D55),
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

private fun String?.isNull0rBlank(): Boolean = this == null || this.isBlank()
