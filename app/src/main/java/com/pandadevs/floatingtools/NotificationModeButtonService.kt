package com.pandadevs.floatingtools

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.view.*
import android.widget.ImageView
import androidx.core.app.NotificationCompat

class NotificationModeButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var modeIcon: ImageView
    private lateinit var audioManager: AudioManager

    private lateinit var prefs: SharedPreferences

    private lateinit var ringerObserver: RingerModeObserver

    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_mode_button, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        prefs = getSharedPreferences("floating_notes", Context.MODE_PRIVATE)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        ringerObserver = RingerModeObserver(this) { mode ->
            updateModeIcon()
        }
        ringerObserver.register()

        params.gravity = Gravity.TOP or Gravity.START
        params.x = dpToPx(0)
        params.y = dpToPx(140)

        modeIcon = floatingView.findViewById(R.id.notificationButton)
        modeIcon.alpha = 0.3f
        updateModeIcon()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        enableDragAndSnap(modeIcon, params)

        startForegroundService()
    }

    // -----------------------------------
    // Toggle between Silent → Vibrate → Ring
    // -----------------------------------
    private fun toggleMode() {
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                if (notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
                //audioManager.setStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
            AudioManager.RINGER_MODE_SILENT -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
        }
        updateModeIcon()
    }

    private fun updateModeIcon() {
        val iconRes = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> R.drawable.ic_ring
            AudioManager.RINGER_MODE_VIBRATE -> R.drawable.ic_vibrate
            AudioManager.RINGER_MODE_SILENT -> R.drawable.ic_silent
            else -> R.drawable.ic_ring
        }
        modeIcon.setImageResource(iconRes)
    }

    // -----------------------------------
    // Drag + Snap + Click Handling
    // -----------------------------------
    @Suppress("ClickableViewAccessibility")
    private fun enableDragAndSnap(button: View, params: WindowManager.LayoutParams) {
        val edgeMargin = dpToPx(0)
        val clickThreshold = dpToPx(10)

        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f

        button.setOnTouchListener { v, event ->
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()

                    params.x = startX + dx
                    params.y = startY + dy

                    // Clamp vertically
                    val maxY = screenHeight - v.height - edgeMargin
                    val minY = edgeMargin
                    params.y = params.y.coerceIn(minY, maxY)

                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val totalDx = (event.rawX - touchStartX).toInt()
                    val totalDy = (event.rawY - touchStartY).toInt()

                    if (kotlin.math.abs(totalDx) < clickThreshold && kotlin.math.abs(totalDy) < clickThreshold) {
                        toggleMode() // treat as click
                    } else {
                        if (prefs.getBoolean("flag_bottomEdge", true) && params.y > screenHeight - 250) {
                            stopSelf()
                        } else {
                            if(prefs.getBoolean("flag_snapToEdge", true)) {
                                val middleX = params.x + v.width / 2
                                val snapLeft = edgeMargin
                                val snapRight = screenWidth - v.width - edgeMargin
                                params.x = if (middleX >= screenWidth / 2) snapRight else snapLeft
                            }
                            windowManager.updateViewLayout(floatingView, params)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    // -----------------------------------
    // Foreground Service
    // -----------------------------------
    private fun startForegroundService() {
        val channelId = "FloatingToolsChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating Tools Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Floating Tools Running")
            .setSmallIcon(R.drawable.ic_settings)
            .build()

        startForeground(2, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }

        ringerObserver.unregister()

        // Send a broadcast to MainActivity
        val intent = Intent("SERVICE_DESTROYED")
        intent.putExtra("message", "Alert Button")
        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?) = null

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
