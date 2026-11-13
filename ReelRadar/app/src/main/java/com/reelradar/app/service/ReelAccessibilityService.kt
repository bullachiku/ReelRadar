package com.reelradar.app.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.reelradar.app.R
import com.reelradar.app.core.detector.DetectionResult
import com.reelradar.app.core.detector.ReelDetector
import com.reelradar.app.core.util.Constants
import com.reelradar.app.data.local.datastore.PreferencesManager
import com.reelradar.app.domain.repository.ReelRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class ReelAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var reelRepository: ReelRepository

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var remoteConfig: FirebaseRemoteConfig

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val detector = ReelDetector()

    private var isInstagramActive = false
    private var isReelsScreenActive = false
    private var sessionStartTime = 0L
    private var sessionReelCount = 0

    companion object {
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive = _isServiceActive.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceActive.value = true
        syncRemoteConfigRules()
    }

    private fun syncRemoteConfigRules() {
        serviceScope.launch {
            try {
                val minDwell = remoteConfig.getLong(Constants.KEY_MIN_DWELL_TIME_MS)
                val dedupeWindow = remoteConfig.getLong(Constants.KEY_DEDUPE_WINDOW_MS)
                val pagerIds = remoteConfig.getString(Constants.KEY_REELS_PAGER_VIEW_IDS)
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()

                if (minDwell > 0) detector.minDwellTimeMs = minDwell
                if (dedupeWindow > 0) detector.dedupeWindowMs = dedupeWindow
                if (pagerIds.isNotEmpty()) detector.targetPagerViewIds = pagerIds
            } catch (e: Exception) {
                // Safe fallback to defaults
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: ""
        if (pkgName != Constants.INSTAGRAM_PACKAGE_NAME) {
            if (isInstagramActive) {
                handleExitInstagram()
            }
            return
        }

        val currentTime = System.currentTimeMillis()

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                isInstagramActive = true
                val className = event.className?.toString() ?: ""
                checkIfReelsScreen(event.source, className)
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                isInstagramActive = true
                val source = event.source
                val viewId = source?.viewIdResourceName
                
                // Pure structural check: never inspect text or child contents!
                if (detector.isReelPager(viewId) || isReelsScreenActive) {
                    processScroll(currentTime)
                }
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Secondary check for Reel viewer container structure if needed
                if (!isReelsScreenActive) {
                    checkIfReelsScreen(event.source, "")
                }
            }
        }
    }

    private fun checkIfReelsScreen(rootNode: AccessibilityNodeInfo?, className: String) {
        if (className.contains("ClipsViewerActivity", ignoreCase = true) ||
            className.contains("ReelsViewer", ignoreCase = true)
        ) {
            markReelsActive()
            return
        }

        if (rootNode != null) {
            val viewId = rootNode.viewIdResourceName
            if (detector.isReelPager(viewId)) {
                markReelsActive()
                return
            }
        }
    }

    private fun markReelsActive() {
        if (!isReelsScreenActive) {
            isReelsScreenActive = true
            sessionStartTime = System.currentTimeMillis()
            sessionReelCount = 0
            detector.reset()

            // Trigger Mindful Gate if enabled
            serviceScope.launch {
                val settings = preferencesManager.userSettingsFlow.first()
                if (settings.mindfulGateEnabled) {
                    showMindfulGateOverlay()
                }
            }
        }
    }

    private fun processScroll(currentTime: Long) {
        val result = detector.processScrollEvent(currentTime)
        if (result is DetectionResult.CountValid) {
            sessionReelCount++
            serviceScope.launch {
                val hour = LocalDateTime.now().hour
                val updatedRecord = reelRepository.incrementReelCount(result.dwellDurationMs, hour)
                val settings = preferencesManager.userSettingsFlow.first()

                // Notify Overlay Service of new count
                updateFloatingOverlay(updatedRecord.count, updatedRecord.dailyLimit)

                // Check Limits & Escalating Friction
                if (updatedRecord.count == (updatedRecord.dailyLimit * 0.7f).toInt() && settings.softWarningEnabled) {
                    sendSoftWarningNotification(updatedRecord.count, updatedRecord.dailyLimit)
                } else if (updatedRecord.count >= updatedRecord.dailyLimit) {
                    triggerLimitFriction(updatedRecord.count, updatedRecord.dailyLimit, settings.hardLockEnabled)
                }
            }
        }
    }

    private fun handleExitInstagram() {
        isInstagramActive = false
        val exitResult = detector.onExitReels(System.currentTimeMillis())
        if (exitResult is DetectionResult.CountValid) {
            sessionReelCount++
            serviceScope.launch {
                val hour = LocalDateTime.now().hour
                reelRepository.incrementReelCount(exitResult.dwellDurationMs, hour)
            }
        }

        if (isReelsScreenActive && sessionStartTime > 0L) {
            val endTime = System.currentTimeMillis()
            val durationSecs = (endTime - sessionStartTime) / 1000
            serviceScope.launch {
                reelRepository.recordSession(
                    com.reelradar.app.domain.model.ReelSession(
                        date = java.time.LocalDate.now().toString(),
                        startTime = sessionStartTime,
                        endTime = endTime,
                        reelCount = sessionReelCount,
                        durationSeconds = durationSecs
                    )
                )
            }
        }
        isReelsScreenActive = false
        sessionStartTime = 0L
        sessionReelCount = 0
    }

    private fun sendSoftWarningNotification(current: Int, limit: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, Constants.CHANNEL_LIMITS_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.limit_warning_70_title))
            .setContentText(getString(R.string.limit_warning_70_msg, current, limit))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(Constants.NOTIFICATION_LIMIT_WARNING_ID, notification)
    }

    private fun triggerLimitFriction(current: Int, limit: Int, isHardLock: Boolean) {
        val intent = Intent(this, ReelOverlayService::class.java).apply {
            action = if (isHardLock) ReelOverlayService.ACTION_SHOW_HARD_LOCK else ReelOverlayService.ACTION_SHOW_BREATHING_PAUSE
            putExtra(ReelOverlayService.EXTRA_COUNT, current)
            putExtra(ReelOverlayService.EXTRA_LIMIT, limit)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun updateFloatingOverlay(count: Int, limit: Int) {
        val intent = Intent(this, ReelOverlayService::class.java).apply {
            action = ReelOverlayService.ACTION_UPDATE_COUNT
            putExtra(ReelOverlayService.EXTRA_COUNT, count)
            putExtra(ReelOverlayService.EXTRA_LIMIT, limit)
        }
        startService(intent)
    }

    private fun showMindfulGateOverlay() {
        val intent = Intent(this, ReelOverlayService::class.java).apply {
            action = ReelOverlayService.ACTION_SHOW_MINDFUL_GATE
        }
        startService(intent)
    }

    override fun onInterrupt() {
        _isServiceActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceActive.value = false
        serviceScope.cancel()
    }
}
