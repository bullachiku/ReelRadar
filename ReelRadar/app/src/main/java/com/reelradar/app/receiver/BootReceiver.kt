package com.reelradar.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.reelradar.app.data.local.datastore.PreferencesManager
import com.reelradar.app.data.sync.SyncManager
import com.reelradar.app.service.ReelOverlayService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // 1. Reschedule cloud sync
            syncManager.schedulePeriodicSync()

            // 2. Restart Floating Overlay if enabled
            CoroutineScope(Dispatchers.IO).launch {
                val settings = preferencesManager.userSettingsFlow.first()
                if (settings.overlayEnabled) {
                    val serviceIntent = Intent(context, ReelOverlayService::class.java).apply {
                        action = ReelOverlayService.ACTION_START_OVERLAY
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}
