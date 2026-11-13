package com.reelradar.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reelradar.app.data.local.db.entity.ReelSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReelSessionEntity): Long

    @Query("SELECT * FROM reel_sessions WHERE date = :date ORDER BY startTime DESC")
    fun getSessionsForDate(date: String): Flow<List<ReelSessionEntity>>

    @Query("SELECT * FROM reel_sessions ORDER BY durationSeconds DESC LIMIT 1")
    fun getLongestSession(): Flow<ReelSessionEntity?>

    @Query("SELECT * FROM reel_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ReelSessionEntity>>

    @Query("DELETE FROM reel_sessions")
    suspend fun deleteAll()
}
