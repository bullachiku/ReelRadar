package com.reelradar.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.reelradar.app.core.util.Constants
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ReelRadarApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncManager: com.reelradar.app.data.sync.SyncManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        syncManager.schedulePeriodicSync()
    }


    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Limits Warning Channel
            val limitsChannel = NotificationChannel(
                Constants.CHANNEL_LIMITS_ID,
                getString(R.string.notification_channel_limits),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_limits_desc)
                enableVibration(true)
            }

            // Foreground Service Channel
            val serviceChannel = NotificationChannel(
                Constants.CHANNEL_SERVICE_ID,
                getString(R.string.notification_channel_service),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_service_desc)
            }

            notificationManager.createNotificationChannel(limitsChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }
}
