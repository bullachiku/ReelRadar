package com.reelradar.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_records")
data class DailyRecordEntity(
    @PrimaryKey val date: String, // Format: "yyyy-MM-dd"
    val count: Int = 0,
    val estimatedSeconds: Long = 0,
    val hourlyCounts: List<Int> = List(24) { 0 },
    val dailyLimit: Int = 50,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
