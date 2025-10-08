package com.example.floatingtools

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

class StickyNotesService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var prefs: SharedPreferences

    private var isActive = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("floating_notes", Context.MODE_PRIVATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showFloatingNote()
    }

    private fun showFloatingNote() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_sticky_note, null)

        val layoutType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE

        val screenWidth = resources.displayMetrics.widthPixels
        params = WindowManager.LayoutParams(
            screenWidth/2, //WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 200
        params.y = 300

        val editText = floatingView!!.findViewById<EditText>(R.id.stickyNoteEditText)
        val closeButton = floatingView!!.findViewById<ImageButton>(R.id.closeButton)


        // Make EditText focusable when clicked
        editText.setOnTouchListener { _, _ ->
            isActive = true
            closeButton.setImageResource(R.drawable.ic_check)
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            windowManager.updateViewLayout(floatingView, params)
            false
        }


        // Close button
        closeButton.setOnClickListener {
            if(isActive){
                isActive = false
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(floatingView, params)
                closeButton.setImageResource(R.drawable.ic_close)
            }
            else {
                stopSelf()
            }
        }

        // Drag support
        makeDraggable(floatingView!!)

        windowManager.addView(floatingView, params)
    }

    private fun makeDraggable(view: View) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - touchX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager.removeView(floatingView)
            floatingView = null
        }
    }
}
