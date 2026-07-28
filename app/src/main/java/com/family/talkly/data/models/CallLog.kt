package com.family.talkly.data.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

enum class CallType {
    AUDIO,
    VIDEO
}

data class CallLog(
    val id: String,
    val memberId: String,
    val memberName: String,
    val direction: CallDirection,
    val callType: CallType,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedDuration: String
        get() {
            if (direction == CallDirection.MISSED || durationSeconds == 0) return "Missed"
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        }
}
