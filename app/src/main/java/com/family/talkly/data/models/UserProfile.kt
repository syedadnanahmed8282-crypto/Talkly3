package com.family.talkly.data.models

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val phoneSuffix: String = "",
    val profilePicUrl: String = "",
    val bio: String = "Available on Talkly 💬",
    val createdAt: Long = System.currentTimeMillis()
)
