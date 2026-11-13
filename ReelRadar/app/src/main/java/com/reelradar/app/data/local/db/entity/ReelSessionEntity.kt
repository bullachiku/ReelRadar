package com.reelradar.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reel_sessions")
data class ReelSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // Format: "yyyy-MM-dd"
    val startTime: Long,
    val endTime: Long,
    val reelCount: Int,
    val durationSeconds: Long
)
