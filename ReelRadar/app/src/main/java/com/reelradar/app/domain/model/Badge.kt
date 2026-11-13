package com.reelradar.app.domain.model

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val iconEmoji: String = "🎖️"
)

object BadgesList {
    fun calculateBadges(streakInfo: StreakInfo, dailyRecords: List<DailyRecord>): List<Badge> {
        val totalDaysUnderLimit = streakInfo.daysUnderLimit
        val currentStreak = streakInfo.currentStreak
        val bestStreak = streakInfo.bestStreak

        val hasMonkMode = dailyRecords.any { it.count in 1..15 && it.count <= it.dailyLimit }
        val hasNightOwlHealer = dailyRecords.any { record ->
            // Check if 11 PM (23) and 12-4 AM counts are zero
            val lateNightCount = record.hourlyCounts.getOrElse(23) { 0 } +
                record.hourlyCounts.getOrElse(0) { 0 } +
                record.hourlyCounts.getOrElse(1) { 0 } +
                record.hourlyCounts.getOrElse(2) { 0 }
            lateNightCount == 0 && record.count > 0
        }

        return listOf(
            Badge(
                id = "first_step",
                title = "First Step",
                description = "Completed your first day under your daily reel limit",
                isUnlocked = totalDaysUnderLimit >= 1,
                iconEmoji = "🌱"
            ),
            Badge(
                id = "zen_seeker",
                title = "Zen Seeker",
                description = "Achieved a 3-day mindful scroll streak",
                isUnlocked = bestStreak >= 3,
                iconEmoji = "🧘"
            ),
            Badge(
                id = "iron_will",
                title = "Iron Will",
                description = "Maintained a 7-day streak under your reel limit",
                isUnlocked = bestStreak >= 7,
                iconEmoji = "🛡️"
            ),
            Badge(
                id = "master_of_focus",
                title = "Master of Focus",
                description = "Legendary 30-day streak under your daily limit",
                isUnlocked = bestStreak >= 30,
                iconEmoji = "👑"
            ),
            Badge(
                id = "monk_mode",
                title = "Monk Mode",
                description = "Scrolled fewer than 15 reels in an entire day",
                isUnlocked = hasMonkMode,
                iconEmoji = "⚡"
            ),
            Badge(
                id = "night_owl_healer",
                title = "Sleep Champion",
                description = "Zero reels scrolled during late night hours (11 PM - 3 AM)",
                isUnlocked = hasNightOwlHealer,
                iconEmoji = "🌙"
            )
        )
    }
}
