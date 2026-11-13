package com.reelradar.app.domain.model

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class UserSettings(
    val dailyLimit: Int = 50,
    val minDwellTimeMs: Long = 1500L,
    val softWarningEnabled: Boolean = true,
    val breathingPauseEnabled: Boolean = true,
    val hardLockEnabled: Boolean = false,
    val mindfulGateEnabled: Boolean = false,
    val overlayEnabled: Boolean = true,
    val pinProtectionEnabled: Boolean = false,
    val pinHash: String = "",
    val maxSnoozesPerDay: Int = 2,
    val darkTheme: Boolean = true
)
