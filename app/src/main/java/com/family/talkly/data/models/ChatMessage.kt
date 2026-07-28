package com.family.talkly.data.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    VOICE_NOTE,
    CALL_LOG
}

data class ChatMessage(
    val id: String = "",
    val senderId: String = "self",
    val senderName: String = "You",
    val receiverId: String = "",
    val messageType: MessageType = MessageType.TEXT,
    val textContent: String = "",
    val mediaUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val callType: String? = null, // "AUDIO" or "VIDEO"
    val callDurationSec: Int = 0,
    val isDelivered: Boolean = false,
    val isRead: Boolean = false,
    val readAtTimestamp: Long? = null,
    val reaction: String? = null,
    val isStarred: Boolean = false,
    val isPinned: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToSenderName: String? = null,
    val replyToText: String? = null
) {
    companion object {
        const val EXPIRATION_48_HOURS_MS = 48 * 60 * 60 * 1000L // 172,800,000 ms
    }

    fun isMediaExpired(simulatedTimeOffsetMs: Long = 0L): Boolean {
        if (messageType != MessageType.IMAGE && messageType != MessageType.VIDEO && messageType != MessageType.VOICE_NOTE) {
            return false
        }
        val effectiveCurrentTime = System.currentTimeMillis() + simulatedTimeOffsetMs
        return (effectiveCurrentTime - timestamp) >= EXPIRATION_48_HOURS_MS
    }

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedReadTime: String
        get() {
            val t = readAtTimestamp ?: timestamp
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            return sdf.format(Date(t))
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
