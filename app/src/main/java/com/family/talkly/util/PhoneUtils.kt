package com.family.talkly.util

object PhoneUtils {
    /**
     * Cleans a phone number by removing spaces, dashes, brackets, plus signs, and any non-digit character.
     */
    fun cleanPhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "")
    }

    /**
     * Extracts the LAST 10 DIGITS (phoneSuffix) from any given phone number.
     * E.g., '+8801712345678' -> '1712345678', '01712345678' -> '1712345678'.
     */
    fun extractPhoneSuffix(phone: String): String {
        val clean = cleanPhoneNumber(phone)
        return if (clean.length > 10) {
            clean.takeLast(10)
        } else {
            clean
        }
    }
}
