package com.reelradar.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.reelradar.app.data.local.datastore.PreferencesManager
import com.reelradar.app.data.local.db.dao.DailyRecordDao
import com.reelradar.app.data.local.db.dao.SessionDao
import com.reelradar.app.data.local.db.entity.DailyRecordEntity
import com.reelradar.app.data.local.db.entity.ReelSessionEntity
import com.reelradar.app.domain.model.DailyRecord
import com.reelradar.app.domain.model.ReelSession
import com.reelradar.app.domain.model.StreakInfo
import com.reelradar.app.domain.repository.ReelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReelRepositoryImpl @Inject constructor(
    private val dailyRecordDao: DailyRecordDao,
    private val sessionDao: SessionDao,
    private val preferencesManager: PreferencesManager,
    private val firestore: FirebaseFirestore
) : ReelRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun getTodayDateString(): String = LocalDate.now().format(dateFormatter)

    override fun getTodayRecord(): Flow<DailyRecord> {
        val today = getTodayDateString()
        return dailyRecordDao.getRecordByDate(today).map { entity ->
            entity?.toDomain() ?: DailyRecord(date = today)
        }
    }

    override fun getRecordForDate(date: String): Flow<DailyRecord?> {
        return dailyRecordDao.getRecordByDate(date).map { it?.toDomain() }
    }

    override fun getAllRecords(): Flow<List<DailyRecord>> {
        return dailyRecordDao.getAllRecords().map { list -> list.map { it.toDomain() } }
    }

    override fun getRecordsInRange(startDate: String, endDate: String): Flow<List<DailyRecord>> {
        return dailyRecordDao.getRecordsInRange(startDate, endDate).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun incrementReelCount(dwellTimeMs: Long, hourOfDay: Int): DailyRecord {
        val today = getTodayDateString()
        val currentEntity = dailyRecordDao.getRecordByDateSync(today)
        val settings = preferencesManager.userSettingsFlow.first()

        val secondsAdded = (dwellTimeMs / 1000).coerceAtLeast(1)
        val safeHour = hourOfDay.coerceIn(0, 23)

        val updatedEntity = if (currentEntity != null) {
            val updatedHourly = currentEntity.hourlyCounts.toMutableList()
            updatedHourly[safeHour] = updatedHourly.getOrElse(safeHour) { 0 } + 1
            currentEntity.copy(
                count = currentEntity.count + 1,
                estimatedSeconds = currentEntity.estimatedSeconds + secondsAdded,
                hourlyCounts = updatedHourly,
                dailyLimit = settings.dailyLimit,
                lastUpdated = System.currentTimeMillis(),
                isSynced = false
            )
        } else {
            val initialHourly = MutableList(24) { 0 }
            initialHourly[safeHour] = 1
            DailyRecordEntity(
                date = today,
                count = 1,
                estimatedSeconds = secondsAdded,
                hourlyCounts = initialHourly,
                dailyLimit = settings.dailyLimit,
                lastUpdated = System.currentTimeMillis(),
                isSynced = false
            )
        }

        dailyRecordDao.insertOrUpdate(updatedEntity)
        return updatedEntity.toDomain()
    }

    override suspend fun recordSession(session: ReelSession) {
        sessionDao.insertSession(
            ReelSessionEntity(
                date = session.date,
                startTime = session.startTime,
                endTime = session.endTime,
                reelCount = session.reelCount,
                durationSeconds = session.durationSeconds
            )
        )
    }

    override fun getLongestSession(): Flow<ReelSession?> {
        return sessionDao.getLongestSession().map { entity ->
            entity?.let {
                ReelSession(
                    id = it.id,
                    date = it.date,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    reelCount = it.reelCount,
                    durationSeconds = it.durationSeconds
                )
            }
        }
    }

    override fun getStreakInfo(): Flow<StreakInfo> {
        return dailyRecordDao.getAllRecords().map { records ->
            if (records.isEmpty()) return@map StreakInfo()

            var currentStreak = 0
            var bestStreak = 0
            var tempStreak = 0
            var daysUnderLimit = 0

            val sorted = records.sortedByDescending { it.date }
            val today = getTodayDateString()

            for (record in sorted) {
                val metGoal = record.count <= record.dailyLimit
                if (metGoal) {
                    daysUnderLimit++
                    tempStreak++
                    if (tempStreak > bestStreak) bestStreak = tempStreak
                } else {
                    tempStreak = 0
                }
            }

            // Calculate active streak from today backwards
            for (record in sorted) {
                if (record.date == today && record.count > record.dailyLimit) {
                    break
                }
                if (record.count <= record.dailyLimit) {
                    currentStreak++
                } else {
                    break
                }
            }

            StreakInfo(
                currentStreak = currentStreak,
                bestStreak = bestStreak.coerceAtLeast(currentStreak),
                totalDaysTracked = records.size,
                daysUnderLimit = daysUnderLimit
            )
        }
    }

    override suspend fun exportDataToCsv(): String {
        val records = dailyRecordDao.getAllRecords().first()
        val builder = StringBuilder()
        builder.append("Date,ReelCount,EstimatedMinutes,DailyLimit,GoalMet\n")
        for (r in records.sortedBy { it.date }) {
            val mins = r.estimatedSeconds / 60
            val met = if (r.count <= r.dailyLimit) "YES" else "NO"
            builder.append("${r.date},${r.count},$mins,${r.dailyLimit},$met\n")
        }
        return builder.toString()
    }

    override suspend fun clearAllLocalData() {
        dailyRecordDao.deleteAll()
        sessionDao.deleteAll()
        preferencesManager.clearAll()
    }

    override suspend fun syncPendingDeltasToCloud(userId: String) {
        if (userId.isBlank()) return
        val unsynced = dailyRecordDao.getUnsyncedRecords()
        if (unsynced.isEmpty()) return

        val syncedDates = mutableListOf<String>()

        for (record in unsynced) {
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("daily")
                .document(record.date)

            val remoteDoc = docRef.get().await()
            val remoteCount = remoteDoc.getLong("count")?.toInt() ?: 0

            // Conflict resolution: max count wins to preserve true scroll progression
            val resolvedCount = maxOf(record.count, remoteCount)
            val resolvedSecs = maxOf(record.estimatedSeconds, remoteDoc.getLong("estimatedSeconds") ?: 0L)

            val data = mapOf(
                "date" to record.date,
                "count" to resolvedCount,
                "estimatedSeconds" to resolvedSecs,
                "hourlyCounts" to record.hourlyCounts,
                "dailyLimit" to record.dailyLimit,
                "lastUpdated" to System.currentTimeMillis()
            )

            docRef.set(data, SetOptions.merge()).await()
            syncedDates.add(record.date)
        }

        if (syncedDates.isNotEmpty()) {
            dailyRecordDao.markAsSynced(syncedDates)
        }
    }

    private fun DailyRecordEntity.toDomain() = DailyRecord(
        date = date,
        count = count,
        estimatedSeconds = estimatedSeconds,
        hourlyCounts = hourlyCounts,
        dailyLimit = dailyLimit,
        lastUpdated = lastUpdated
    )
}
