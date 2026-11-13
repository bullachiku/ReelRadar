package com.reelradar.app.core.detector

import com.reelradar.app.core.util.Constants

sealed class DetectionResult {
    data class CountValid(val dwellDurationMs: Long) : DetectionResult()
    data class IgnoredDwellTooShort(val dwellDurationMs: Long, val requiredDwellMs: Long) : DetectionResult()
    data class IgnoredRapidSwipe(val intervalMs: Long, val dedupeThresholdMs: Long) : DetectionResult()
    data class DwellStarted(val timestamp: Long) : DetectionResult()
    object None : DetectionResult()
}

class ReelDetector(
    var minDwellTimeMs: Long = Constants.DEFAULT_MIN_DWELL_TIME_MS,
    var dedupeWindowMs: Long = Constants.DEFAULT_DEDUPE_WINDOW_MS,
    var targetPagerViewIds: Set<String> = setOf(
        "clips_view_pager",
        "clips_video_container",
        "clips_viewer_container",
        "view_pager",
        "clips_viewer_view_pager"
    )
) {
    private var activeReelStartTime: Long = 0L
    private var lastScrollTime: Long = 0L

    fun isReelPager(viewId: String?): Boolean {
        if (viewId == null) return false
        val cleanId = viewId.substringAfterLast(":id/")
        return targetPagerViewIds.any { cleanId.contains(it, ignoreCase = true) }
    }

    /**
     * Called when a structural scroll/page-change event occurs in the Reels viewer.
     */
    fun processScrollEvent(currentTimeMs: Long): DetectionResult {
        // If this is the very first scroll observed in the session
        if (activeReelStartTime == 0L) {
            activeReelStartTime = currentTimeMs
            lastScrollTime = currentTimeMs
            return DetectionResult.DwellStarted(currentTimeMs)
        }

        val timeSinceLastScroll = currentTimeMs - lastScrollTime
        lastScrollTime = currentTimeMs

        // 1. Deduplicate rapid back-and-forth swipes
        if (timeSinceLastScroll < dedupeWindowMs) {
            return DetectionResult.IgnoredRapidSwipe(timeSinceLastScroll, dedupeWindowMs)
        }

        // 2. Dwell filter: verify user actually stayed >= minDwellTimeMs on the reel before swiping
        val dwellDuration = currentTimeMs - activeReelStartTime
        activeReelStartTime = currentTimeMs // Reset timer for the newly entered reel

        return if (dwellDuration >= minDwellTimeMs) {
            DetectionResult.CountValid(dwellDuration)
        } else {
            DetectionResult.IgnoredDwellTooShort(dwellDuration, minDwellTimeMs)
        }
    }

    /**
     * Called when user exits Instagram or switches away from Reels tab.
     */
    fun onExitReels(currentTimeMs: Long): DetectionResult {
        if (activeReelStartTime == 0L) return DetectionResult.None
        val dwellDuration = currentTimeMs - activeReelStartTime
        activeReelStartTime = 0L
        lastScrollTime = 0L

        return if (dwellDuration >= minDwellTimeMs) {
            DetectionResult.CountValid(dwellDuration)
        } else {
            DetectionResult.None
        }
    }

    fun reset() {
        activeReelStartTime = 0L
        lastScrollTime = 0L
    }
}
