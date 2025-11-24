package com.example.floatingtools

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.TextView
import androidx.core.app.NotificationCompat
import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import android.os.*


class StopwatchButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var stopwatchText: TextView

    private var isRunning = false
    private var startTime = 0L
    private var elapsedTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()


        // Inflate floating button layout
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_stopwatch_button, null)

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

        params.gravity = Gravity.TOP or Gravity.START
        params.x = dpToPx(0)
        params.y = dpToPx(290)

        stopwatchText = floatingView.findViewById(R.id.stopwatchTime)
        floatingView.alpha = 0.3f

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)


        enableDragAndSnap(stopwatchText, params)

        startForegroundService()
    }

    // -----------------------------
    // Toggle
    // -----------------------------
    private fun toggleStopwatch() {
        if (isRunning) {
            // Stop
            handler.removeCallbacks(timerRunnable!!)
            elapsedTime += SystemClock.elapsedRealtime() - startTime
            isRunning = false
        } else {
            // Start
            startTime = SystemClock.elapsedRealtime()
            timerRunnable = object : Runnable {
                override fun run() {
                    val total = elapsedTime + (SystemClock.elapsedRealtime() - startTime)
                    val minutes = (total / 1000) / 60
                    val seconds = (total / 1000) % 60
                    val millis = (total % 1000) // show 2-digit centiseconds

                    stopwatchText.text = String.format("%02d:%02d.%03d", minutes, seconds, millis)

                    handler.postDelayed(this, 50) // update every 50ms for smooth display
                }
            }
            handler.post(timerRunnable!!)
            isRunning = true
        }
    }

    private fun resetStopwatch() {
        handler.removeCallbacks(timerRunnable!!)
        isRunning = false
        elapsedTime = 0L
        stopwatchText.text = "00:00.000"
    }

    // -----------------------------
    // Drag + Snap + Click Handling
    // -----------------------------
    @Suppress("ClickableViewAccessibility")
    private fun enableDragAndSnap(button: View, params: WindowManager.LayoutParams) {
        val edgeMargin = dpToPx(0)
        val clickThreshold = dpToPx(10)
        val longPressTimeout = ViewConfiguration.getLongPressTimeout()

        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var isLongPress = false

        val longPressHandler = Handler(Looper.getMainLooper())
        val longPressRunnable = Runnable {
            resetStopwatch()
            isLongPress = true
        }

        button.setOnTouchListener { v, event ->
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isLongPress = false
                    longPressHandler.postDelayed(longPressRunnable, longPressTimeout.toLong())
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()

                    // If moved too far, cancel long press
                    if (kotlin.math.abs(dx) > clickThreshold || kotlin.math.abs(dy) > clickThreshold) {
                        longPressHandler.removeCallbacks(longPressRunnable)
                    }

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
                    longPressHandler.removeCallbacks(longPressRunnable)

                    if (!isLongPress) {
                        val totalDx = (event.rawX - touchStartX).toInt()
                        val totalDy = (event.rawY - touchStartY).toInt()

                        if (kotlin.math.abs(totalDx) < clickThreshold && kotlin.math.abs(totalDy) < clickThreshold) {
                            // Small movement → treat as click
                            toggleStopwatch()
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
                    }
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    false
                }

                else -> false
            }
        }
    }


    // -----------------------------
    // Foreground Service
    // -----------------------------
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

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
        // Send a broadcast to MainActivity
        val intent = Intent("SERVICE_DESTROYED")
        intent.putExtra("message", "Stopwatch Button")
        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
