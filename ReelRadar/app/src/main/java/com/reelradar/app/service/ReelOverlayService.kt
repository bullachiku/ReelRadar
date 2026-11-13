package com.reelradar.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.reelradar.app.R
import com.reelradar.app.core.util.Constants
import com.reelradar.app.data.local.datastore.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReelOverlayService : Service() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null

    // Overlay Views
    private var floatingView: View? = null
    private var floatingCountText: TextView? = null
    private var breathingView: View? = null
    private var hardLockView: View? = null
    private var mindfulGateView: View? = null

    private var currentCount = 0
    private var currentLimit = 50

    companion object {
        const val ACTION_START_OVERLAY = "action_start_overlay"
        const val ACTION_UPDATE_COUNT = "action_update_count"
        const val ACTION_SHOW_BREATHING_PAUSE = "action_show_breathing_pause"
        const val ACTION_SHOW_HARD_LOCK = "action_show_hard_lock"
        const val ACTION_SHOW_MINDFUL_GATE = "action_show_mindful_gate"
        const val ACTION_HIDE_OVERLAY = "action_hide_overlay"

        const val EXTRA_COUNT = "extra_count"
        const val EXTRA_LIMIT = "extra_limit"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(Constants.NOTIFICATION_SERVICE_ID, buildForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            return START_NOT_STICKY
        }

        val action = intent?.action ?: ACTION_START_OVERLAY
        currentCount = intent?.getIntExtra(EXTRA_COUNT, currentCount) ?: currentCount
        currentLimit = intent?.getIntExtra(EXTRA_LIMIT, currentLimit) ?: currentLimit

        when (action) {
            ACTION_START_OVERLAY, ACTION_UPDATE_COUNT -> {
                serviceScope.launch {
                    val settings = preferencesManager.userSettingsFlow.first()
                    if (settings.overlayEnabled) {
                        showOrUpdateFloatingHUD(currentCount, currentLimit)
                    } else {
                        removeFloatingHUD()
                    }
                }
            }
            ACTION_SHOW_BREATHING_PAUSE -> showBreathingPauseOverlay()
            ACTION_SHOW_HARD_LOCK -> showHardLockOverlay()
            ACTION_SHOW_MINDFUL_GATE -> showMindfulGatePrompt()
            ACTION_HIDE_OVERLAY -> removeFloatingHUD()
        }

        return START_STICKY
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, Constants.CHANNEL_SERVICE_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("ReelRadar Active")
            .setContentText("Monitoring mindful screen time in background")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    // 1. Draggable Floating HUD
    @SuppressLint("ClickableViewAccessibility")
    private fun showOrUpdateFloatingHUD(count: Int, limit: Int) {
        if (floatingView == null) {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 30
                y = 250
            }

            // Create pill layout programmatically for clean zero-asset dependency
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(28, 14, 28, 14)
                val bg = GradientDrawable().apply {
                    setColor(Color.parseColor("#E60D0F18")) // 90% dark radar bg
                    cornerRadius = 60f
                    setStroke(3, if (count >= limit) Color.parseColor("#FF7675") else Color.parseColor("#6C5CE7"))
                }
                background = bg
            }

            val tv = TextView(this).apply {
                text = "$count / $limit"
                setTextColor(Color.WHITE)
                textSize = 13f
                paint.isFakeBoldText = true
            }
            floatingCountText = tv
            container.addView(tv)

            // Touch & Drag Handling
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            container.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(container, params)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        serviceScope.launch {
                            preferencesManager.saveOverlayPosition(params.x, params.y)
                        }
                        true
                    }
                    else -> false
                }
            }

            floatingView = container
            try {
                windowManager?.addView(floatingView, params)
            } catch (e: Exception) {
                // Handle permission race
            }
        } else {
            floatingCountText?.text = "$count / $limit"
            val bg = (floatingView as? LinearLayout)?.background as? GradientDrawable
            bg?.setStroke(3, if (count >= limit) Color.parseColor("#FF7675") else Color.parseColor("#6C5CE7"))
        }
    }

    private fun removeFloatingHUD() {
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {}
            floatingView = null
        }
    }

    // 2. 10-Second Breathing Pause Overlay
    private fun showBreathingPauseOverlay() {
        if (breathingView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F20F111A"))
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "Take a Breath"
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            paint.isFakeBoldText = true
        }

        val message = TextView(this).apply {
            text = "You reached your limit of $currentLimit reels.\nInhale deeply... Exhale slowly."
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 36)
        }

        val countdownText = TextView(this).apply {
            text = "10s"
            setTextColor(Color.parseColor("#00CEC9"))
            textSize = 42f
            gravity = Gravity.CENTER
            paint.isFakeBoldText = true
        }

        val closeButton = Button(this).apply {
            text = "Leave Instagram"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#6C5CE7"))
            visibility = View.GONE
            setOnClickListener {
                goToHomeScreen()
                dismissBreathingPause()
            }
        }

        val continueButton = Button(this).apply {
            text = "Continue anyway"
            setTextColor(Color.parseColor("#AAAAAA"))
            setBackgroundColor(Color.TRANSPARENT)
            visibility = View.GONE
            setOnClickListener {
                dismissBreathingPause()
            }
        }

        root.addView(title)
        root.addView(message)
        root.addView(countdownText)
        root.addView(closeButton)
        root.addView(continueButton)

        breathingView = root
        try {
            windowManager?.addView(breathingView, params)
        } catch (e: Exception) {
            return
        }

        // 10s countdown
        object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = millisUntilFinished / 1000
                countdownText.text = "${sec}s"
            }

            override fun onFinish() {
                countdownText.text = "Breathe."
                closeButton.visibility = View.VISIBLE
                continueButton.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun dismissBreathingPause() {
        breathingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {}
            breathingView = null
        }
    }

    // 3. Optional Hard Lock Screen Overlay with Snooze
    private fun showHardLockOverlay() {
        if (hardLockView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FA0D0F18"))
            setPadding(64, 64, 64, 64)
        }

        val lockTitle = TextView(this).apply {
            text = "Reels Locked"
            setTextColor(Color.parseColor("#FF7675"))
            textSize = 26f
            gravity = Gravity.CENTER
            paint.isFakeBoldText = true
        }

        val lockMsg = TextView(this).apply {
            text = "Hard limit reached ($currentLimit reels). Your future self thanks you for stopping here."
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 40)
        }

        val exitBtn = Button(this).apply {
            text = "Exit Instagram"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#6C5CE7"))
            setOnClickListener {
                goToHomeScreen()
                dismissHardLock()
            }
        }

        val snoozeBtn = Button(this).apply {
            text = "Snooze 5 Minutes"
            setTextColor(Color.parseColor("#FD79A8"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                serviceScope.launch {
                    val allowed = preferencesManager.incrementSnoozeCount()
                    if (allowed) {
                        dismissHardLock()
                    } else {
                        text = "No snoozes remaining today"
                        isEnabled = false
                    }
                }
            }
        }

        root.addView(lockTitle)
        root.addView(lockMsg)
        root.addView(exitBtn)
        root.addView(snoozeBtn)

        hardLockView = root
        try {
            windowManager?.addView(hardLockView, params)
        } catch (e: Exception) {}
    }

    private fun dismissHardLock() {
        hardLockView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {}
            hardLockView = null
        }
    }

    // 4. Mindful Gate Prompt
    private fun showMindfulGatePrompt() {
        if (mindfulGateView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#F5161926"))
                cornerRadius = 32f
                setStroke(2, Color.parseColor("#6C5CE7"))
            }
            background = bg
            setPadding(48, 36, 48, 36)
        }

        val gateTitle = TextView(this).apply {
            text = "Set an Intention"
            setTextColor(Color.WHITE)
            textSize = 18f
            paint.isFakeBoldText = true
        }

        val gateSub = TextView(this).apply {
            text = "How many reels do you intend to watch this session?"
            setTextColor(Color.parseColor("#BBBBBB"))
            textSize = 13f
            setPadding(0, 10, 0, 24)
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btn10 = createGateChoiceButton("10 Reels") { dismissMindfulGate() }
        val btn25 = createGateChoiceButton("25 Reels") { dismissMindfulGate() }
        val btnExit = createGateChoiceButton("Cancel") {
            goToHomeScreen()
            dismissMindfulGate()
        }

        buttonRow.addView(btn10)
        buttonRow.addView(btn25)
        buttonRow.addView(btnExit)

        container.addView(gateTitle)
        container.addView(gateSub)
        container.addView(buttonRow)

        mindfulGateView = container
        try {
            windowManager?.addView(mindfulGateView, params)
        } catch (e: Exception) {}
    }

    private fun createGateChoiceButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2B3048"))
            setOnClickListener { onClick() }
        }
    }

    private fun dismissMindfulGate() {
        mindfulGateView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {}
            mindfulGateView = null
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingHUD()
        dismissBreathingPause()
        dismissHardLock()
        dismissMindfulGate()
        serviceScope.cancel()
    }
}
