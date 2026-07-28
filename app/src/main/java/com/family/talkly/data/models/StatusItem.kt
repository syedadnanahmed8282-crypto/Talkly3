package com.family.talkly.data.models

import java.util.UUID

data class StatusViewer(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val timeAgo: String = "Just now"
)

data class StatusLiker(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null
)

data class StatusItem(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val textContent: String? = null,
    val photoUrl: String? = null,
    val isVideo: Boolean = false,
    val backgroundColorHex: String = "#321C3B",
    val timestamp: Long = System.currentTimeMillis(),
    val isSeen: Boolean = false,
    val viewers: List<StatusViewer> = emptyList(),
    val likes: List<StatusLiker> = emptyList()
) {
    fun isExpired(timeOffsetMs: Long = 0L): Boolean {
        val now = System.currentTimeMillis() + timeOffsetMs
        val twentyFourHoursMs = 24 * 60 * 60 * 1000L
        return (now - timestamp) > twentyFourHoursMs
    }
}

data class UserStatusGroup(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val statuses: List<StatusItem>,
    val hasUnseen: Boolean = statuses.any { !it.isSeen }
)
