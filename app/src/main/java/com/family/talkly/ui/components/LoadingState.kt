package com.family.talkly.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.family.talkly.ui.theme.WhatsappGreen
import com.family.talkly.ui.theme.WhatsappTeal

/**
 * Centralized LoadingState component for displaying progress indicators
 * while fetching messages, authenticating users, or performing background tasks.
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    title: String = "Loading...",
    message: String? = null,
    icon: ImageVector? = Icons.Default.Chat,
    progressColor: Color = WhatsappGreen,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    isCardStyle: Boolean = false
) {
    if (isCardStyle) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LoadingIndicatorCore(
                    title = title,
                    message = message,
                    icon = icon,
                    progressColor = progressColor,
                    textColor = Color(0xFF111B21)
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                LoadingIndicatorCore(
                    title = title,
                    message = message,
                    icon = icon,
                    progressColor = progressColor,
                    textColor = if (backgroundColor == WhatsappTeal) Color.White else Color(0xFF111B21)
                )
            }
        }
    }
}

@Composable
private fun LoadingIndicatorCore(
    title: String,
    message: String?,
    icon: ImageVector?,
    progressColor: Color,
    textColor: Color
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(72.dp)
    ) {
        CircularProgressIndicator(
            color = progressColor,
            strokeWidth = 4.dp,
            modifier = Modifier.fillMaxSize()
        )
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = progressColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = title,
        color = textColor,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    if (!message.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message!!,
            color = textColor.copy(alpha = 0.75f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

/**
 * Specialized LoadingState for Authentication processes.
 */
@Composable
fun AuthLoadingState(
    message: String = "Authenticating...",
    subMessage: String? = "Connecting securely to Talkly Family Messenger"
) {
    LoadingState(
        title = message,
        message = subMessage,
        icon = Icons.Default.Lock,
        progressColor = Color.White,
        backgroundColor = WhatsappTeal,
        isCardStyle = false
    )
}

/**
 * Specialized LoadingState for fetching or loading Chat Messages.
 */
@Composable
fun MessageLoadingState(
    message: String = "Fetching messages...",
    subMessage: String? = "Syncing end-to-end encrypted family conversation"
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = WhatsappGreen,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = WhatsappTeal
                )
                if (subMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subMessage,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * LoadingOverlay wrapper component that presents content and overlays
 * a progress indicator screen when [isLoading] is true.
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    message: String = "Please wait...",
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                LoadingState(
                    title = message,
                    message = null,
                    icon = Icons.Default.FamilyRestroom,
                    progressColor = WhatsappGreen,
                    isCardStyle = true
                )
            }
        }
    }
}
