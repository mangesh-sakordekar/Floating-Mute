package com.pandadevs.floatingtools

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

class StickyNotesService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: SharedPreferences

    // ✅ Store multiple note instances
    private val noteViews = mutableListOf<View>()
    private val noteParams = mutableListOf<WindowManager.LayoutParams>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("floating_notes", Context.MODE_PRIVATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        //createStickyNote()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ✅ Create a new note each time the service is started
        createStickyNote()
        return START_NOT_STICKY
    }

    private fun createStickyNote() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val floatingView = inflater.inflate(R.layout.floating_sticky_note, null)

        val layoutType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE

        val screenWidth = resources.displayMetrics.widthPixels
        val params = WindowManager.LayoutParams(
            screenWidth / 2,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = (100..400).random() // randomize placement for each note
        params.y = (200..600).random()

        val editText = floatingView.findViewById<EditText>(R.id.stickyNoteEditText)
        val closeButton = floatingView.findViewById<ImageButton>(R.id.closeButton)

        var isActive = false

        // Make EditText focusable when clicked
        editText.setOnTouchListener { _, _ ->
            isActive = true
            closeButton.setImageResource(R.drawable.ic_check)
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            windowManager.updateViewLayout(floatingView, params)
            false
        }

        // Close button
        closeButton.setOnClickListener {
            if (isActive) {
                isActive = false
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(floatingView, params)
                closeButton.setImageResource(R.drawable.ic_close)
            } else {
                windowManager.removeView(floatingView)
                noteViews.remove(floatingView)
                noteParams.remove(params)
                if (noteViews.isEmpty()) stopSelf()
            }
        }

        makeDraggable(floatingView, params)
        windowManager.addView(floatingView, params)

        noteViews.add(floatingView)
        noteParams.add(params)
    }

    private fun makeDraggable(view: View, params: WindowManager.LayoutParams) {
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
                        windowManager.updateViewLayout(view, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ Remove all notes on destroy
        for (view in noteViews) {
            windowManager.removeView(view)
        }
        noteViews.clear()
        noteParams.clear()
    }
}
