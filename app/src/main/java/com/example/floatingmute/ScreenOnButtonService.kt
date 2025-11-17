package com.example.floatingtools

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import android.content.SharedPreferences

class ScreenOnButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var modeIcon: ImageView

    private var isScreenOnForced = false

    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()


        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_screenon_button, null)

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
        params.y = dpToPx(320)

        modeIcon = floatingView.findViewById(R.id.screenOnButton)
        modeIcon.alpha = 0.3f

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        enableDragAndSnap(modeIcon, params)

        startForegroundService()
        if(prefs.getBoolean("flag_launchTools", true)){
            toggleMode()
        }
    }

    private fun toggleMode() {
        if (isScreenOnForced) {
            // Restore normal behavior
            windowManager.removeViewImmediate(floatingView)
            windowManager.addView(floatingView, (floatingView.layoutParams as WindowManager.LayoutParams).apply {
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            })
            isScreenOnForced = false
            modeIcon.setImageResource(R.drawable.ic_screen_off) // your off icon
        } else {
            // Force keep screen on
            windowManager.removeViewImmediate(floatingView)
            windowManager.addView(floatingView, (floatingView.layoutParams as WindowManager.LayoutParams).apply {
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            })
            isScreenOnForced = true
            modeIcon.setImageResource(R.drawable.ic_screen_on) // your on icon
        }

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
                        if (params.y > screenHeight - 150) {
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
        if(isScreenOnForced){
            toggleMode()
        }
        floatingView?.let { windowManager.removeView(it) }
        // Send a broadcast to MainActivity
        val intent = Intent("SERVICE_DESTROYED")
        intent.putExtra("message", "Screen On Button")
        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?) = null

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
