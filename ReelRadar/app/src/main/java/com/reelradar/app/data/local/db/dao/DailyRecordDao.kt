package com.reelradar.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reelradar.app.data.local.db.entity.DailyRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DailyRecordEntity)

    @Query("SELECT * FROM daily_records WHERE date = :date LIMIT 1")
    fun getRecordByDate(date: String): Flow<DailyRecordEntity?>

    @Query("SELECT * FROM daily_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDateSync(date: String): DailyRecordEntity?

    @Query("SELECT * FROM daily_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getRecordsInRange(startDate: String, endDate: String): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_records WHERE isSynced = 0")
    suspend fun getUnsyncedRecords(): List<DailyRecordEntity>

    @Query("UPDATE daily_records SET isSynced = 1 WHERE date IN (:dates)")
    suspend fun markAsSynced(dates: List<String>)

    @Query("DELETE FROM daily_records")
    suspend fun deleteAll()
}
