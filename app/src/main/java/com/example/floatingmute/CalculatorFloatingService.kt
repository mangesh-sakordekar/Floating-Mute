package com.example.floatingtools

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat
import android.os.Handler
import android.os.Looper

class CalculatorFloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var display: TextView

    private lateinit var prefs: SharedPreferences

    // calculator state
    private var currentInput = ""
    private var currentOperator: Char? = null
    private var firstOperand: Double? = null

    override fun onCreate() {
        super.onCreate()

        prefs = getSharedPreferences("floating_notes", Context.MODE_PRIVATE)
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_calculator, null)
        display = floatingView.findViewById(R.id.display)
        //val closeButton = floatingView.findViewById<ImageButton>(R.id.closeButton)

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
            // allow the view to receive touch events while letting other windows below interact as well
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        enableDrag(floatingView, params)


        display.setText(prefs.getString("calc_text", "0"))

        // Wire numeric buttons
        val numIds = listOf(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
            R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
            R.id.btn_8, R.id.btn_9
        )
        for (id in numIds) {
            floatingView.findViewById<Button>(id).setOnClickListener { v ->
                val txt = (v as Button).text.toString()
                appendNumber(txt)
            }
        }

        // dot
        floatingView.findViewById<Button>(R.id.btn_dot).setOnClickListener {
            if (!currentInput.contains(".")) {
                if (currentInput.isEmpty()) currentInput = "0."
                else currentInput += "."
                updateDisplay(currentInput)
            }
        }

        // operators
        floatingView.findViewById<Button>(R.id.btn_add).setOnClickListener { onOperator('+') }
        floatingView.findViewById<Button>(R.id.btn_sub).setOnClickListener { onOperator('-') }
        floatingView.findViewById<Button>(R.id.btn_mul).setOnClickListener { onOperator('*') }
        floatingView.findViewById<Button>(R.id.btn_div).setOnClickListener { onOperator('/') }

        // equals
        floatingView.findViewById<Button>(R.id.btn_eq).setOnClickListener { computeResult() }

        // clear (AC)
        floatingView.findViewById<Button>(R.id.btn_clear).setOnClickListener {
            currentInput = ""
            firstOperand = null
            currentOperator = null
            updateDisplay("0")
        }

        // backspace
        floatingView.findViewById<Button>(R.id.btn_back).setOnClickListener {
            if (currentInput.isNotEmpty()) {
                currentInput = currentInput.dropLast(1)
                updateDisplay(if (currentInput.isEmpty()) "0" else currentInput)
            }
        }

        startForegroundService()
    }

    private fun appendNumber(digit: String) {
        // prevent leading zeros like "000"
        if (digit == "0" && currentInput == "0") return
        if (currentInput == "0" && digit != ".") currentInput = digit else currentInput += digit
        updateDisplay(currentInput)
    }

    private fun onOperator(op: Char) {
        if (currentInput.isEmpty() && firstOperand == null) {
            // nothing typed; do nothing
            return
        }

        computeResult()
        if (firstOperand == null) {
            // set first operand
            firstOperand = currentInput.takeIf { it.isNotEmpty() }?.toDouble() ?: 0.0
            currentInput = ""
            currentOperator = op
        } else {
            // if operator pressed after having firstOperand, compute intermediate result if input present
            if (currentInput.isNotEmpty()) {
                computeResult()
                currentOperator = op
            } else {
                currentOperator = op
            }
        }
    }

    private fun computeResult() {
        if (firstOperand == null && currentInput.isEmpty()) return

        val b = if (currentInput.isNotEmpty()) currentInput.toDouble() else firstOperand ?: 0.0
        val a = firstOperand ?: 0.0
        val op = currentOperator

        val result = when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> {
                if (b == 0.0) {
                    // divide by zero
                    updateDisplay("Error")
                    currentInput = ""
                    firstOperand = null
                    currentOperator = null
                    return
                } else a / b
            }
            null -> b // If no operator, just show typed value
            else -> b
        }

        // Show result and prepare for next input
        val textResult = if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
        updateDisplay(textResult)
        // Put result into currentInput so user can continue calculations
        currentInput = textResult
        firstOperand = null
        currentOperator = null
    }

    private fun updateDisplay(text: String) {
        // ensure UI update on main thread
        Handler(Looper.getMainLooper()).post {
            display.text = text
        }
        prefs.edit().putString("calc_text", text).apply()
    }

    private fun enableDrag(view: View, params: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    // update on main thread
                    Handler(Looper.getMainLooper()).post { windowManager.updateViewLayout(view, params) }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            windowManager.removeView(floatingView)
        } catch (e: Exception) { /* ignore if already removed */ }
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null

    private fun startForegroundService() {
        val channelId = "FloatingToolsChannel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Floating Tools Service", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Floating Calculator")
            .setSmallIcon(R.drawable.ic_settings)
            .build()
        startForeground(200, notification)
    }
}
