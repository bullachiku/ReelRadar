package com.reelradar.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.reelradar.app.data.local.db.converter.Converters
import com.reelradar.app.data.local.db.dao.DailyRecordDao
import com.reelradar.app.data.local.db.dao.SessionDao
import com.reelradar.app.data.local.db.entity.DailyRecordEntity
import com.reelradar.app.data.local.db.entity.ReelSessionEntity

@Database(
    entities = [DailyRecordEntity::class, ReelSessionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ReelRadarDatabase : RoomDatabase() {
    abstract val dailyRecordDao: DailyRecordDao
    abstract val sessionDao: SessionDao
}
