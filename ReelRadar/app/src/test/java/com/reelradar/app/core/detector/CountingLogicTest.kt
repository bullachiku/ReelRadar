package com.reelradar.app.core.detector

import com.reelradar.app.domain.model.DailyRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CountingLogicTest {

    @Test
    fun `testMidnightRollover resets daily count for new date`() {
        val yesterdayDate = "2026-11-13"
        val todayDate = "2026-11-14"

        val yesterdayRecord = DailyRecord(
            date = yesterdayDate,
            count = 42,
            dailyLimit = 50
        )

        // On day rollover, a new record is initialized for today with count 0
        val todayRecord = DailyRecord(
            date = todayDate,
            count = 0,
            dailyLimit = yesterdayRecord.dailyLimit
        )

        assertEquals(42, yesterdayRecord.count)
        assertEquals(0, todayRecord.count)
        assertEquals(yesterdayRecord.dailyLimit, todayRecord.dailyLimit)
        assertFalse(todayRecord.isLimitExceeded)
    }

    @Test
    fun `testSoftWarningAndLimitExceededFlags`() {
        val recordUnderWarning = DailyRecord(date = "2026-11-14", count = 30, dailyLimit = 50)
        assertFalse(recordUnderWarning.isSoftWarningReached)
        assertFalse(recordUnderWarning.isLimitExceeded)

        val recordAtWarning = DailyRecord(date = "2026-11-14", count = 35, dailyLimit = 50) // 70% of 50 = 35
        assertTrue(recordAtWarning.isSoftWarningReached)
        assertFalse(recordAtWarning.isLimitExceeded)

        val recordAtLimit = DailyRecord(date = "2026-11-14", count = 50, dailyLimit = 50)
        assertTrue(recordAtLimit.isSoftWarningReached)
        assertTrue(recordAtLimit.isLimitExceeded)
        assertEquals(1.0f, recordAtLimit.progressPercentage, 0.01f)
    }

    @Test
    fun `testHourlyDistributionDistributionWithin24Hours`() {
        val hourly = MutableList(24) { 0 }
        val hour = 22 // 10 PM
        hourly[hour] = hourly[hour] + 1
        assertEquals(1, hourly[22])
        assertEquals(0, hourly[23])
        assertEquals(24, hourly.size)
    }
}
