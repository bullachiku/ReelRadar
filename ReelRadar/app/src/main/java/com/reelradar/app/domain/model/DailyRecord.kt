package com.reelradar.app.domain.model

data class DailyRecord(
    val date: String,
    val count: Int = 0,
    val estimatedSeconds: Long = 0,
    val hourlyCounts: List<Int> = List(24) { 0 },
    val dailyLimit: Int = 50,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val progressPercentage: Float
        get() = if (dailyLimit > 0) (count.toFloat() / dailyLimit).coerceIn(0f, 1f) else 0f

    val isLimitExceeded: Boolean
        get() = count >= dailyLimit

    val isSoftWarningReached: Boolean
        get() = count >= (dailyLimit * 0.7f).toInt()
}

data class ReelSession(
    val id: Long = 0,
    val date: String,
    val startTime: Long,
    val endTime: Long,
    val reelCount: Int,
    val durationSeconds: Long
)

data class StreakInfo(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalDaysTracked: Int = 0,
    val daysUnderLimit: Int = 0
)
