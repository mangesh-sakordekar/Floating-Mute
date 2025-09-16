package com.example.floatingmute


import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*


class ScreenshotButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null

    private var resultCode: Int = 0
    private var resultData: Intent? = null

    override fun onCreate() {
        super.onCreate()

        floatingView = LayoutInflater.from(this)
            .inflate(R.layout.floating_screenshot_button, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
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
        params.y = dpToPx(200)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        val button = floatingView!!.findViewById<ImageButton>(R.id.screenshotButton)
        button.alpha = 0.3f
        enableDragAndSnap(button, params)

        button.setOnClickListener {
            if (mediaProjection == null) {
                Toast.makeText(this, "Screen capture permission required", Toast.LENGTH_SHORT).show()
            } else {
                takeScreenshot()
            }
        }

        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            resultCode = it.getIntExtra("resultCode", 0)
            resultData = it.getParcelableExtra("data")
            if (resultCode != 0 && resultData != null) {
                val projectionManager =
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = projectionManager.getMediaProjection(resultCode, resultData!!)
                setupImageReader()
            }
        }
        return START_STICKY
    }

    private fun setupImageReader() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        mediaProjection?.createVirtualDisplay(
            "screenshot",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    private fun takeScreenshot() {
        val reader = imageReader ?: return
        val image = reader.acquireLatestImage() ?: return

        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val ftDir = File(picturesDir, "FloatingTools")

        if (!ftDir.exists()) {
            ftDir.mkdirs() // create the folder if it doesn't exist
        }

        val file = File(ftDir, "Screenshot_$timeStamp.png")

        try {
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }


            // Notify MediaStore / Gallery
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = android.net.Uri.fromFile(file)
            sendBroadcast(intent)
            Toast.makeText(this, "Screenshot saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save screenshot", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- Drag + Snap ----------------
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

                    // Clamp vertically within screen
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
                        v.performClick()
                    } else {
                        // Snap to nearest horizontal edge
                        val middleX = params.x + v.width / 2
                        val snapLeft = edgeMargin
                        val snapRight = screenWidth - v.width - edgeMargin
                        params.x = if (middleX >= screenWidth / 2) snapRight else snapLeft
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun startForegroundService() {
        val channelId = "ScreenshotButtonChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating Screenshot Button Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Screenshot Button Running")
            .setSmallIcon(R.drawable.ic_screenshot)
            .build()

        startForeground(2, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
        mediaProjection?.stop()
        imageReader?.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}

