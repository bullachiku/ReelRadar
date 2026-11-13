package com.reelradar.app.core.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReelDetectorTest {

    private lateinit var detector: ReelDetector

    @Before
    fun setup() {
        detector = ReelDetector(
            minDwellTimeMs = 1500L,
            dedupeWindowMs = 1200L,
            targetPagerViewIds = setOf("clips_view_pager", "clips_video_container")
        )
    }

    @Test
    fun `testFirstScroll starts dwell timer and returns DwellStarted`() {
        val result = detector.processScrollEvent(currentTimeMs = 1000L)
        assertTrue(result is DetectionResult.DwellStarted)
        assertEquals(1000L, (result as DetectionResult.DwellStarted).timestamp)
    }

    @Test
    fun `testRapidSwipe is ignored due to dedupe window`() {
        // Initial scroll
        detector.processScrollEvent(currentTimeMs = 1000L)

        // Rapid second scroll 400ms later (less than dedupeWindowMs 1200ms)
        val result = detector.processScrollEvent(currentTimeMs = 1400L)

        assertTrue(result is DetectionResult.IgnoredRapidSwipe)
        val ignored = result as DetectionResult.IgnoredRapidSwipe
        assertEquals(400L, ignored.intervalMs)
        assertEquals(1200L, ignored.dedupeThresholdMs)
    }

    @Test
    fun `testDwellTimeBelowThreshold is ignored`() {
        // Initial scroll
        detector.processScrollEvent(currentTimeMs = 1000L)

        // Swipe 1300ms later (passes dedupe 1200ms, but below minDwellTimeMs 1500ms)
        val result = detector.processScrollEvent(currentTimeMs = 2300L)

        assertTrue(result is DetectionResult.IgnoredDwellTooShort)
        val dwellResult = result as DetectionResult.IgnoredDwellTooShort
        assertEquals(1300L, dwellResult.dwellDurationMs)
        assertEquals(1500L, dwellResult.requiredDwellMs)
    }

    @Test
    fun `testValidDwellTime counts successfully`() {
        // Initial scroll starts dwell at 1000ms
        detector.processScrollEvent(currentTimeMs = 1000L)

        // User watches reel for 3500ms (>= 1500ms) and then scrolls at 4500ms
        val result = detector.processScrollEvent(currentTimeMs = 4500L)

        assertTrue(result is DetectionResult.CountValid)
        val countResult = result as DetectionResult.CountValid
        assertEquals(3500L, countResult.dwellDurationMs)
    }

    @Test
    fun `testExitInstagram with dwell above threshold is counted`() {
        // Enter reels at 1000ms
        detector.processScrollEvent(currentTimeMs = 1000L)

        // User watches for 2000ms, then leaves Instagram at 3000ms
        val exitResult = detector.onExitReels(currentTimeMs = 3000L)

        assertTrue(exitResult is DetectionResult.CountValid)
        assertEquals(2000L, (exitResult as DetectionResult.CountValid).dwellDurationMs)
    }

    @Test
    fun `testExitInstagram with short dwell is ignored`() {
        // Enter reels at 1000ms
        detector.processScrollEvent(currentTimeMs = 1000L)

        // User immediately exits 500ms later
        val exitResult = detector.onExitReels(currentTimeMs = 1500L)

        assertTrue(exitResult is DetectionResult.None)
    }

    @Test
    fun `testStructuralPagerViewIdMatching validates target nodes only`() {
        assertTrue(detector.isReelPager("com.instagram.android:id/clips_view_pager"))
        assertTrue(detector.isReelPager("clips_video_container"))
        assertFalse(detector.isReelPager("com.instagram.android:id/row_feed_caption"))
        assertFalse(detector.isReelPager("com.instagram.android:id/row_feed_comment_textview"))
        assertFalse(detector.isReelPager(null))
    }
}
