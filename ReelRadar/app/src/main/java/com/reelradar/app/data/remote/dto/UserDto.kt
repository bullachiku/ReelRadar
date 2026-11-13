package com.reelradar.app.data.remote.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserDto(
    @DocumentId val uid: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class DailyStatsDto(
    @DocumentId val date: String = "",
    val count: Int = 0,
    val estimatedSeconds: Long = 0,
    val hourlyCounts: List<Int> = List(24) { 0 },
    val dailyLimit: Int = 50,
    @ServerTimestamp val lastUpdated: Date? = null
)

data class FriendshipDto(
    @DocumentId val id: String = "",
    val uids: List<String> = emptyList(),
    val status: String = "accepted", // "pending", "accepted"
    val user1Username: String = "",
    val user2Username: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
