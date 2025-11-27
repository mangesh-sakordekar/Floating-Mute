package com.pandadevs.floatingtools

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.display.DisplayManager
import android.os.*
import android.util.Size
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.app.NotificationCompat

class MirrorFloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var textureView: TextureView
    private lateinit var closeButton: ImageButton

    private var cameraDevice: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var previewSize: Size? = null
    private var cameraId: String? = null

    override fun onCreate() {
        super.onCreate()

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_mirror, null)
        textureView = floatingView.findViewById(R.id.cameraPreview)
        closeButton = floatingView.findViewById(R.id.closeButton)

        val layoutType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            360,
            480,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)

        enableDrag(floatingView, params)

        closeButton.setOnClickListener { stopSelf() }

        textureView.surfaceTextureListener = surfaceListener

        startForegroundService()

        val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        dm.registerDisplayListener(displayListener, null)
    }

    // ───────────────────── Camera Setup ─────────────────────
    private val surfaceListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
            applyTransform(w, h)
        }

        override fun onSurfaceTextureDestroyed(st: SurfaceTexture) = true
        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
    }

    private fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            for (id in manager.cameraIdList) {
                val c = manager.getCameraCharacteristics(id)
                if (c.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_FRONT
                ) {
                    cameraId = id

                    val map = c.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                    )!!

                    previewSize = map.getOutputSizes(SurfaceTexture::class.java)
                        .maxByOrNull { it.width * it.height } // best quality

                    adjustWindowToAspect(previewSize!!)

                    startBackgroundThread()

                    if (checkSelfPermission(android.Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        manager.openCamera(id, stateCallback, backgroundHandler)
                    }
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun adjustWindowToAspect(size: Size) {
        val params = floatingView.layoutParams as WindowManager.LayoutParams

        val aspect = size.width.toFloat() / size.height.toFloat()

        params.height = 360
        params.width = (360 / aspect).toInt()

        windowManager.updateViewLayout(floatingView, params)
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cd: CameraDevice) {
            cameraDevice = cd
            startPreview()
        }

        override fun onDisconnected(cd: CameraDevice) {
            cd.close()
        }

        override fun onError(cd: CameraDevice, error: Int) {
            cd.close()
        }
    }

    private fun startPreview() {
        val texture = textureView.surfaceTexture ?: return
        val size = previewSize ?: return

        texture.setDefaultBufferSize(size.width, size.height)

        val surface = Surface(texture)

        val builder =
            cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            }

        cameraDevice!!.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    this@MirrorFloatingService.session = session
                    session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                    applyTransform(textureView.width, textureView.height)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            backgroundHandler
        )
    }

    // No more cropping, only correct rotation
    private fun applyTransform(w: Int, h: Int) {
        Handler(Looper.getMainLooper()).post {
            val rotation = windowManager.defaultDisplay.rotation
            val matrix = Matrix()

            val cx = w / 2f
            val cy = h / 2f

            when (rotation) {
                Surface.ROTATION_90 -> matrix.postRotate(270f, cx, cy)
                Surface.ROTATION_180 -> matrix.postRotate(180f, cx, cy)
                Surface.ROTATION_270 -> matrix.postRotate(90f, cx, cy)
            }

            textureView.setTransform(matrix)
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(id: Int) {}
        override fun onDisplayRemoved(id: Int) {}
        override fun onDisplayChanged(id: Int) {
            if (textureView.isAvailable)
                applyTransform(textureView.width, textureView.height)
        }
    }

    // ───────────────────── Background Thread ─────────────────────

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("MirrorCameraThread").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
    }

    // ───────────────────── Drag Window ─────────────────────

    private fun enableDrag(view: View, params: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        view.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = ev.rawX
                    touchY = ev.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (ev.rawX - touchX).toInt()
                    params.y = startY + (ev.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }

                else -> false
            }
        }
    }

    // ───────────────────── Notification ─────────────────────

    private fun startForegroundService() {
        val channelId = "MirrorFloatingService"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating Mirror",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Floating Mirror")
            .setSmallIcon(R.drawable.ic_settings)
            .build()

        startForeground(2, notification)
    }

    // ───────────────────── Cleanup ─────────────────────

    override fun onDestroy() {
        super.onDestroy()

        windowManager.removeView(floatingView)
        cameraDevice?.close()
        session?.close()

        stopBackgroundThread()

        val dm = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        dm.unregisterDisplayListener(displayListener)

        val intent = Intent("SERVICE_DESTROYED")
        intent.putExtra("message", "Mirror Button")
        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(this)
            .sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?) = null
}
