package com.reelradar.app.core.util

object Constants {
    const val INSTAGRAM_PACKAGE_NAME = "com.instagram.android"
    
    // Remote Config Keys
    const val KEY_MIN_DWELL_TIME_MS = "min_dwell_time_ms"
    const val KEY_DEDUPE_WINDOW_MS = "dedupe_window_ms"
    const val KEY_INSTAGRAM_PKG = "instagram_package_name"
    const val KEY_REELS_PAGER_VIEW_IDS = "reels_pager_view_ids"
    const val KEY_REELS_TAB_VIEW_IDS = "reels_tab_view_ids"
    const val KEY_BREATHING_PAUSE_SECONDS = "breathing_pause_seconds"
    const val KEY_SOFT_WARNING_RATIO = "soft_warning_ratio"

    // Default Values
    const val DEFAULT_MIN_DWELL_TIME_MS = 1500L
    const val DEFAULT_DEDUPE_WINDOW_MS = 1200L
    const val DEFAULT_DAILY_LIMIT = 50
    const val DEFAULT_BREATHING_PAUSE_SECONDS = 10
    const val DEFAULT_SOFT_WARNING_RATIO = 0.7f
    const val DEFAULT_AVG_DWELL_SECONDS = 12 // estimated average seconds per reel

    // Notification Channels
    const val CHANNEL_LIMITS_ID = "reelradar_limits_channel"
    const val CHANNEL_SERVICE_ID = "reelradar_service_channel"
    const val NOTIFICATION_SERVICE_ID = 1001
    const val NOTIFICATION_LIMIT_WARNING_ID = 1002
    const val NOTIFICATION_LIMIT_REACHED_ID = 1003

    // DataStore & Sync
    const val PREFERENCES_NAME = "reelradar_prefs"
    const val SYNC_WORK_NAME = "ReelRadarSyncWork"
    const val SYNC_INTERVAL_HOURS = 4L
}
