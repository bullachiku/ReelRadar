package com.reelradar.app.domain.repository

import com.reelradar.app.domain.model.DailyRecord
import com.reelradar.app.domain.model.ReelSession
import com.reelradar.app.domain.model.StreakInfo
import kotlinx.coroutines.flow.Flow

interface ReelRepository {
    fun getTodayRecord(): Flow<DailyRecord>
    fun getRecordForDate(date: String): Flow<DailyRecord?>
    fun getAllRecords(): Flow<List<DailyRecord>>
    fun getRecordsInRange(startDate: String, endDate: String): Flow<List<DailyRecord>>
    suspend fun incrementReelCount(dwellTimeMs: Long, hourOfDay: Int): DailyRecord
    suspend fun recordSession(session: ReelSession)
    fun getLongestSession(): Flow<ReelSession?>
    fun getStreakInfo(): Flow<StreakInfo>
    suspend fun exportDataToCsv(): String
    suspend fun clearAllLocalData()
    suspend fun syncPendingDeltasToCloud(userId: String)
}
