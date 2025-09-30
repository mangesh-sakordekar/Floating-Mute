package com.example.floatingtools

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat
import android.app.Service
import android.graphics.Color
import android.graphics.Typeface
import android.os.*
import android.widget.EditText
import android.widget.NumberPicker
import android.view.GestureDetector

class CountdownTimerButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var timerText: TextView
    private lateinit var settingsButton: ImageButton

    private var isRunning = false

    private var resetTimeMillis: Long = 60 * 1000
    private var remainingTimeMillis: Long = 60 * 1000 // default 1 minutes
    private var countDownTimer: CountDownTimer? = null

    private lateinit var gestureDetector: GestureDetector

    override fun onCreate() {
        super.onCreate()


        // Inflate floating button layout
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_countdown_timer, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = dpToPx(0)
        params.y = dpToPx(160)

        timerText = floatingView.findViewById(R.id.timerText)
        settingsButton = floatingView.findViewById(R.id.settingsButton)
        floatingView.alpha = 0.3f

        settingsButton.setOnClickListener {
            showDurationDialog()
            true
        }



        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)


        enableDragAndSnap(floatingView, params)

        startForegroundService()
    }

    // -----------------------------
    // Toggle
    // -----------------------------
    private fun toggleStopwatch() {
        if (isRunning) {
            // Stop
            countDownTimer?.cancel()
            isRunning = false
        } else {
            // Start
            countDownTimer?.cancel()
            countDownTimer = object : CountDownTimer(remainingTimeMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingTimeMillis = millisUntilFinished
                    updateTimerText()
                }

                override fun onFinish() {
                    remainingTimeMillis = 0
                    updateTimerText()
                    isRunning = false // play icon
                    showFinishNotification()
                }
            }.start()

            isRunning = true
        }
    }

    private fun resetStopwatch() {
        countDownTimer?.cancel()
        isRunning = false
        remainingTimeMillis = resetTimeMillis
        updateTimerText()
    }

    private fun updateTimerText() {
        val hours = (remainingTimeMillis / 1000) / 3600
        val minutes = ((remainingTimeMillis / 1000) % 3600) / 60
        val seconds = (remainingTimeMillis / 1000) % 60
        timerText.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun setNumberPickerTextStyle(numberPicker: NumberPicker) {
        val count = numberPicker.childCount

        for (i in 0 until count) {
            val child = numberPicker.getChildAt(i)
            if (child is EditText) {
                child.setTextColor(Color.BLACK)
                child.textSize = 26f   // Make numbers larger
                child.typeface = Typeface.DEFAULT_BOLD
                child.isEnabled = false
            }
        }
        numberPicker.invalidate()
    }




    private fun showDurationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_timer_picker, null)
        val hoursPicker = dialogView.findViewById<NumberPicker>(R.id.hoursPicker)
        val minutesPicker = dialogView.findViewById<NumberPicker>(R.id.minutesPicker)
        val secondsPicker = dialogView.findViewById<NumberPicker>(R.id.secondsPicker)
        val setButton = dialogView.findViewById<ImageButton>(R.id.setButton)

        hoursPicker.minValue = 0
        hoursPicker.maxValue = 23
        minutesPicker.minValue = 0
        minutesPicker.maxValue = 59
        secondsPicker.minValue = 0
        secondsPicker.maxValue = 59
        minutesPicker.value = 1

        setNumberPickerTextStyle(hoursPicker)
        setNumberPickerTextStyle(minutesPicker)
        setNumberPickerTextStyle(secondsPicker)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_NoActionBar)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.window?.setType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
        )

        // Handle inline Set button click
        setButton.setOnClickListener {
            val hours = hoursPicker.value
            val minutes = minutesPicker.value
            val seconds = secondsPicker.value
            remainingTimeMillis = ((hours * 3600) + (minutes * 60) + seconds) * 1000L
            resetTimeMillis = remainingTimeMillis
            resetStopwatch()
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun showFinishNotification() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "FloatingToolsChannel")
            .setContentTitle("Countdown Finished")
            .setContentText("Your timer is up!")
            .setSmallIcon(R.drawable.ic_stop)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(1002, notification)
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
                            if (params.y > screenHeight - 150) {
                                stopSelf()
                            } else {
                                val middleX = params.x + v.width / 2
                                val snapLeft = edgeMargin
                                val snapRight = screenWidth - v.width - edgeMargin
                                params.x = if (middleX >= screenWidth / 2) snapRight else snapLeft
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
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
