package com.family.talkly.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {

    /**
     * Returns the first name from a full name string, respecting common Bangladeshi/South Asian name prefixes.
     * e.g., "Md Israfel Hosen" -> "Md Israfel"
     *       "Dr. Rashed" -> "Dr. Rashed"
     *       "Safwan" -> "Safwan"
     *       "Md Akhter Høssain°" -> "Md Akhter"
     */
    fun getFirstName(fullName: String): String {
        val trimmed = fullName.trim()
        if (trimmed.isEmpty()) return ""
        val parts = trimmed.split("\\s+".toRegex())
        if (parts.size > 1) {
            val firstLower = parts[0].lowercase()
            if (firstLower in listOf("md", "md.", "dr", "dr.", "sk", "sk.", "shaikh", "syed", "mrs", "mr", "ms")) {
                return "${parts[0]} ${parts[1]}"
            }
        }
        return parts.first()
    }

    /**
     * Formats last seen timestamp into human-readable Bengali text.
     * e.g., "এখনই", "৫ মিনিট আগে", "আজ 10:15 AM", "গতকাল 08:30 PM", "28 Jul, 11:45 AM"
     */
    fun formatLastSeenTime(timestamp: Long, fallback: String = "সম্প্রতি"): String {
        if (timestamp <= 0) {
            return if (fallback.isNotBlank() && fallback != "Online") fallback else "সম্প্রতি"
        }
        val now = System.currentTimeMillis()
        val diffMs = now - timestamp
        if (diffMs < 0) return "এখনই"
        if (diffMs < 60 * 1000L) {
            return "এখনই"
        }
        val minutesAgo = diffMs / (60 * 1000L)
        if (minutesAgo < 60) {
            return "$minutesAgo মিনিট আগে"
        }
        val hoursAgo = minutesAgo / 60
        if (hoursAgo < 24) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            return "আজ ${timeFormat.format(Date(timestamp))}"
        }
        val daysAgo = hoursAgo / 24
        if (daysAgo == 1L) {
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            return "গতকাল ${timeFormat.format(Date(timestamp))}"
        }
        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
        return dateFormat.format(Date(timestamp))
    }
}
