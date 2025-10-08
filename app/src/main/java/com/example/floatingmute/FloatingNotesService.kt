package com.example.floatingtools

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.widget.doAfterTextChanged
import android.view.inputmethod.InputMethodManager
import kotlin.math.abs

class FloatingNotesService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var prefs: SharedPreferences

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("floating_notes", Context.MODE_PRIVATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showFloatingNote()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingNote() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_note, null)

        val layoutType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 200
        params.y = 300

        val editText = floatingView!!.findViewById<EditText>(R.id.noteEditText)
        val eraseButton = floatingView!!.findViewById<ImageButton>(R.id.eraseButton)
        val checkButton = floatingView!!.findViewById<ImageButton>(R.id.doneButton)

        editText.setText(prefs.getString("note_text", ""))

        editText.setOnTouchListener { _, _ ->
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            windowManager.updateViewLayout(floatingView, params)
            false
        }
        /*
        editText.setOnTouchListener { _, _ ->
            if ((params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0) {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                windowManager.updateViewLayout(floatingView, params)
                editText.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }
            false
        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(floatingView, params)
            }
        }*/

        editText.doAfterTextChanged {
            prefs.edit().putString("note_text", it.toString()).apply()
            editText.post {
                editText.measure(
                    View.MeasureSpec.makeMeasureSpec(editText.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.UNSPECIFIED
                )
                params.height = editText.measuredHeight + 180
                windowManager.updateViewLayout(floatingView, params)
            }
        }

        eraseButton.setOnClickListener {
            editText.setText("")
        }

        checkButton.setOnClickListener {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            windowManager.updateViewLayout(floatingView, params)
        }

        enableDragAndSnap(floatingView!!, params)
        windowManager.addView(floatingView, params)
    }

    // Replacement for makeDraggable()
    @Suppress("ClickableViewAccessibility")
    private fun enableDragAndSnap(view: View, params: WindowManager.LayoutParams) {
        val edgeMargin = dpToPx(0)
        val clickThreshold = dpToPx(10)

        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f

        view.setOnTouchListener { v, event ->
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

                    if (abs(totalDx) < clickThreshold && abs(totalDy) < clickThreshold) {
                        // Treat as click — focus the note
                        v.performClick()
                    } else {
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager.removeView(floatingView)
            floatingView = null
        }
    }
}
