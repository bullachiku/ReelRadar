package com.reelradar.app.domain.model

data class FriendUser(
    val uid: String,
    val username: String,
    val displayName: String,
    val todayCount: Int = 0,
    val dailyLimit: Int = 50,
    val status: String = "accepted", // "pending", "accepted"
    val friendshipId: String = ""
)
