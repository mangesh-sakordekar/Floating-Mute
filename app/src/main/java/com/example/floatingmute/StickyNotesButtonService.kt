package com.example.floatingtools

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageButton
import androidx.core.app.NotificationCompat
import android.content.SharedPreferences

class StickyNotesButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null

    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()


        // Inflate floating button layout
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_sticky_note_button, null)

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
        params.y = dpToPx(480)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        val button = floatingView!!.findViewById<ImageButton>(R.id.stickyNotesButton)
        button.alpha = 0.3f
        enableDragAndSnap(button, params)

        startForegroundService()
        if(prefs.getBoolean("flag_launchTools", true)){
            toggle()
        }
    }

    // -----------------------------
    // Toggle mute/unmute
    // -----------------------------
    private fun toggle() {
        startService(Intent(this, StickyNotesService::class.java))
    }

    // -----------------------------
    // Drag + Snap + Click Handling
    // -----------------------------
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
                        // Small movement → treat as click
                        toggle()
                    } else {

                        if (prefs.getBoolean("flag_bottomEdge", true) && params.y > screenHeight - 250){
                            stopSelf()
                        }
                        else {
                            // Snap to nearest horizontal edge
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
        intent.putExtra("message", "Sticky Notes Button")
        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
