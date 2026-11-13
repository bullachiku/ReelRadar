package com.reelradar.app.core.di

import android.content.Context
import androidx.room.Room
import com.reelradar.app.data.local.db.ReelRadarDatabase
import com.reelradar.app.data.local.db.dao.DailyRecordDao
import com.reelradar.app.data.local.db.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReelRadarDatabase {
        return Room.databaseBuilder(
            context,
            ReelRadarDatabase::class.java,
            "reel_radar.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideDailyRecordDao(database: ReelRadarDatabase): DailyRecordDao = database.dailyRecordDao

    @Provides
    @Singleton
    fun provideSessionDao(database: ReelRadarDatabase): SessionDao = database.sessionDao
}
