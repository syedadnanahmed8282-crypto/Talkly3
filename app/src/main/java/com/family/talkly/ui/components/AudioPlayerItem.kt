package com.family.talkly.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.talkly.data.models.ChatMessage
import com.family.talkly.ui.theme.LocalIsDarkTheme
import com.family.talkly.ui.theme.WhatsappDarkBubble
import com.family.talkly.ui.theme.WhatsappDarkSurface
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappLightGreen
import com.family.talkly.ui.theme.WhatsappTeal
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AudioPlayerItem(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPrepared by remember { mutableStateOf(false) }

    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(message.mediaUrl) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                Log.w("AudioPlayerItem", "Error disposing mediaPlayer: ${e.localizedMessage}")
            }
        }
    }

    LaunchedEffect(message.mediaUrl) {
        if (!message.mediaUrl.isNullOrEmpty()) {
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(context, Uri.parse(message.mediaUrl))
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener { mp ->
                    isPrepared = true
                    durationMs = mp.duration.toLong()
                }
                mediaPlayer.setOnCompletionListener {
                    isPlaying = false
                    currentPositionMs = 0L
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerItem", "Error setting data source: ${e.localizedMessage}", e)
            }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            try {
                if (mediaPlayer.isPlaying) {
                    currentPositionMs = mediaPlayer.currentPosition.toLong()
                } else {
                    isPlaying = false
                }
            } catch (e: Exception) {
                isPlaying = false
            }
            delay(200)
        }
    }

    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val isSelf = message.senderId == "self"
    val isDarkTheme = LocalIsDarkTheme.current

    val containerColor = if (isDarkTheme) {
        if (isSelf) WhatsappDarkBubble else WhatsappDarkSurface
    } else {
        if (isSelf) Color(0xFFDCF8C6) else Color.White
    }

    val titleColor = if (isDarkTheme) WhatsappLightGreen else WhatsappTeal
    val subTextColor = if (isDarkTheme) Color(0xFFCCCCCC) else Color.DarkGray

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(
            width = 0.5.dp,
            color = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic Avatar Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (isDarkTheme) WhatsappLightGreen.copy(alpha = 0.2f) else WhatsappTeal.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Note",
                    tint = if (isDarkTheme) WhatsappLightGreen else WhatsappTeal,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play / Pause Button
            Surface(
                shape = CircleShape,
                color = WhatsappGreen,
                modifier = Modifier
                    .size(36.dp)
                    .clickable {
                        if (!isPrepared) return@clickable
                        try {
                            if (isPlaying) {
                                mediaPlayer.pause()
                                isPlaying = false
                            } else {
                                mediaPlayer.start()
                                isPlaying = true
                            }
                        } catch (e: Exception) {
                            Log.e("AudioPlayerItem", "Error toggling playback: ${e.localizedMessage}")
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Waveform / Progress bar & duration
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Message",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    Text(
                        text = formatTimeMs(if (isPlaying || currentPositionMs > 0) currentPositionMs else durationMs),
                        fontSize = 11.sp,
                        color = subTextColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = if (isDarkTheme) WhatsappLightGreen else WhatsappGreen,
                    trackColor = if (isDarkTheme) Color.White.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.25f),
                )
            }
        }
    }
}

private fun formatTimeMs(timeMs: Long): String {
    if (timeMs <= 0) return "0:00"
    val totalSeconds = (timeMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
