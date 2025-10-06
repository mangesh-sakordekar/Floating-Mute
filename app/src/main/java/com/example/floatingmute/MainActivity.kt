package com.example.floatingtools

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds


class MainActivity : AppCompatActivity() {

    private val SCREENSHOT_REQUEST_CODE = 1001
    private val OVERLAY_REQUEST_CODE = 1002
    private val WRITE_REQUEST_CODE = 1003
    private val DND_REQUEST_CODE = 1004

    private val CAMERA_PERMISSION_REQUEST = 2001

    private var _bannerAd: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val overlayPermissionButton = findViewById<TextView>(R.id.overlayPermissionText)
        val writePermissionButton = findViewById<TextView>(R.id.writePermissionText)
        val dndPermissionButton = findViewById<TextView>(R.id.dndPermissionText)
        val cameraPermissionButton = findViewById<TextView>(R.id.cameraPermissionText)


        val startMuteButton = findViewById<TextView>(R.id.muteText)
        val stopMuteButton = findViewById<ImageButton>(R.id.stopButton)

        val startScreenshotButton = findViewById<TextView>(R.id.screenshotText)
        val stopScreenshotButton = findViewById<ImageButton>(R.id.stopScreenshotButton)

        val startBrightnessButton = findViewById<TextView>(R.id.brightnessText)
        val stopBrightnessButton = findViewById<ImageButton>(R.id.stopBrightnessButton)

        val startFlashlightButton = findViewById<TextView>(R.id.flashlightText)
        val stopFlashlightButton = findViewById<ImageButton>(R.id.stopFlashlightButton)

        val startStopwatchButton = findViewById<TextView>(R.id.stopwatchText)
        val stopStopwatchButton = findViewById<ImageButton>(R.id.stopStopwatchButton)

        val startNotificationButton = findViewById<TextView>(R.id.notificationText)
        val stopNotificationButton = findViewById<ImageButton>(R.id.stopNotificationButton)

        val startDNDButton = findViewById<TextView>(R.id.dndText)
        val stopDNDButton = findViewById<ImageButton>(R.id.stopDNDButton)

        val startScreenOnButton = findViewById<TextView>(R.id.screenOnText)
        val stopScreenOnButton = findViewById<ImageButton>(R.id.stopScreenOnButton)

        val startTimerButton = findViewById<TextView>(R.id.countdownTimerText)
        val stopTimerButton = findViewById<ImageButton>(R.id.stopTimerButton)

        val startMirrorButton = findViewById<TextView>(R.id.mirrorText)
        val stopMirrorButton = findViewById<ImageButton>(R.id.stopMirrorButton)

        val startCalculatorButton = findViewById<TextView>(R.id.calculatorText)
        val stopCalculatorButton = findViewById<ImageButton>(R.id.stopCalculatorButton)

        MobileAds.initialize(this@MainActivity)
        loadBannerAd()

        updateOverlayBackground()
        updateWriteBackground()
        updateDNDBackground()
        updateCameraBackground()

        overlayPermissionButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission already granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
            loadBannerAd()
        }

        writePermissionButton.setOnClickListener {
            if (Settings.System.canWrite(this)) {
                Toast.makeText(this, "Write Settings permission already granted", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "Write Settings permission required", Toast.LENGTH_SHORT).show()
                checkAndRequestWriteSettings()
            }
            loadBannerAd()
        }

        cameraPermissionButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission already granted", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                requestCameraPermission()
            }
            loadBannerAd()
        }

        dndPermissionButton.setOnClickListener {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.isNotificationPolicyAccessGranted){
                Toast.makeText(this, "Notification Policy permission already granted!", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "Notification Policy permission required", Toast.LENGTH_SHORT).show()
                changeDoNotDisturbState()
            }
            loadBannerAd()
        }

        // Start Mute Button Service
        startMuteButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, FloatingButtonService::class.java))
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
            loadBannerAd()
        }

        // Stop Mute Button Service
        stopMuteButton.setOnClickListener {
            stopService(Intent(this, FloatingButtonService::class.java))
            loadBannerAd()
        }

        // Start Brightness Button Service
        startBrightnessButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                if (Settings.System.canWrite(this)) {
                    startService(Intent(this, BrightnessButtonService::class.java))
                }
                else{
                    Toast.makeText(this, "Write Settings permission required", Toast.LENGTH_SHORT).show()
                    checkAndRequestWriteSettings()
                }
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
            loadBannerAd()
        }

        // Stop Brightness Button Service
        stopBrightnessButton.setOnClickListener {
            stopService(Intent(this, BrightnessButtonService::class.java))
            loadBannerAd()
        }

        // Start Screenshot Button Service
        startScreenshotButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                // Request MediaProjection permission before starting service
                val projectionManager =
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(
                    projectionManager.createScreenCaptureIntent(),
                    SCREENSHOT_REQUEST_CODE
                )
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
            loadBannerAd()
        }

        // Stop Screenshot Button Service
        stopScreenshotButton.setOnClickListener {
            stopService(Intent(this, ScreenshotButtonService::class.java))
            loadBannerAd()
        }

        // Start Mute Button Service
        startFlashlightButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, FlashlightButtonService::class.java))
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
            loadBannerAd()
        }

        // Stop Mute Button Service
        stopFlashlightButton.setOnClickListener {
            stopService(Intent(this, FlashlightButtonService::class.java))
            loadBannerAd()
        }

        // Start Mute Button Service
        startStopwatchButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, StopwatchButtonService::class.java))
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
            loadBannerAd()
        }

        // Stop Mute Button Service
        stopStopwatchButton.setOnClickListener {
            stopService(Intent(this, StopwatchButtonService::class.java))
            loadBannerAd()
        }

        // Start Notification Button Service
        startNotificationButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.isNotificationPolicyAccessGranted){
                    startService(Intent(this, NotificationModeButtonService::class.java))
                }
                else{
                    Toast.makeText(this, "Notification Policy permission required", Toast.LENGTH_SHORT).show()
                    changeDoNotDisturbState()
                }
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
            loadBannerAd()
        }

        // Stop Mute Button Service
        stopNotificationButton.setOnClickListener {
            stopService(Intent(this, NotificationModeButtonService::class.java))
            loadBannerAd()
        }

        // Start Notification Button Service
        startDNDButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.isNotificationPolicyAccessGranted){
                    startService(Intent(this, DNDButtonService::class.java))
                }
                else{
                    Toast.makeText(this, "Notification Policy permission required", Toast.LENGTH_SHORT).show()
                    changeDoNotDisturbState()
                }
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
            loadBannerAd()
        }

        // Stop Mute Button Service
        stopDNDButton.setOnClickListener {
            stopService(Intent(this, DNDButtonService::class.java))
            loadBannerAd()
        }

        // Start Screen On Button Service
        startScreenOnButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, ScreenOnButtonService::class.java))
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
            loadBannerAd()
        }

        // Stop Screen On Button Service
        stopScreenOnButton.setOnClickListener {
            stopService(Intent(this, ScreenOnButtonService::class.java))
            loadBannerAd()
        }

        // Start Mute Button Service
        startTimerButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, CountdownTimerButtonService::class.java))
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
            loadBannerAd()
        }

        // Stop Mute Button Service
        stopTimerButton.setOnClickListener {
            stopService(Intent(this, CountdownTimerButtonService::class.java))
            loadBannerAd()
        }

        // Start Mirror Button Service
        startMirrorButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {
                    startService(Intent(this, MirrorFloatingService::class.java))
                }
                else{
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                    requestCameraPermission()
                }
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
            loadBannerAd()
        }

        // Stop Mirror Button Service
        stopMirrorButton.setOnClickListener {
            stopService(Intent(this, MirrorFloatingService::class.java))
            loadBannerAd()
        }

        // Start Calculator Button Service
        startCalculatorButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, CalculatorButtonService::class.java))
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
            loadBannerAd()
        }

        // Stop Mute Button Service
        stopCalculatorButton.setOnClickListener {
            stopService(Intent(this, CalculatorButtonService::class.java))
            loadBannerAd()
        }
    }

    override fun onResume() {
        super.onResume()
        updateOverlayBackground()
        updateWriteBackground()
        updateDNDBackground()
        updateCameraBackground()
    }


    private fun loadBannerAd() {
        _bannerAd = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        _bannerAd?.loadAd(adRequest)
    }

    // Handle screenshot permission result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREENSHOT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            // Start Screenshot Service and pass projection data
            val intent = Intent(this, ScreenshotButtonService::class.java)
            intent.putExtra("resultCode", resultCode)
            intent.putExtra("data", data)
            startService(intent)
        } else if (requestCode == SCREENSHOT_REQUEST_CODE) {
            Toast.makeText(this, "Screenshot permission denied", Toast.LENGTH_SHORT).show()
        } else if (requestCode == OVERLAY_REQUEST_CODE){
            updateOverlayBackground()
        } else if (requestCode == WRITE_REQUEST_CODE){
            updateWriteBackground()
        } else if (requestCode == DND_REQUEST_CODE){
            updateDNDBackground()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, 1002)
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted → start service
                updateCameraBackground()
            } else {
                Toast.makeText(this, "Camera permission is required for mirror", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestWriteSettings() {
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityForResult(intent, WRITE_REQUEST_CODE)
            //Toast.makeText(this, "Please allow Modify System Settings", Toast.LENGTH_LONG).show()
        }
    }

    private fun changeDoNotDisturbState() {
        // Check if the permission is granted
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {

            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required if starting from a Service
            startActivityForResult(intent, DND_REQUEST_CODE)
        }
    }

    private fun updateOverlayBackground(){
        val overlayPermissionButton = findViewById<TextView>(R.id.overlayPermissionText)
        if(Settings.canDrawOverlays(this)){
            overlayPermissionButton.setBackgroundResource(R.drawable.row_green_background)
        }
        else{
            overlayPermissionButton.setBackgroundResource(R.drawable.row_red_background)
        }
    }

    private fun updateWriteBackground(){
        val writePermissionButton = findViewById<TextView>(R.id.writePermissionText)
        if(Settings.System.canWrite(this)){
            writePermissionButton.setBackgroundResource(R.drawable.row_green_background)
        }
        else{
            writePermissionButton.setBackgroundResource(R.drawable.row_red_background)
        }
    }

    private fun updateDNDBackground(){
        val dndPermissionButton = findViewById<TextView>(R.id.dndPermissionText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(notificationManager.isNotificationPolicyAccessGranted){
            dndPermissionButton.setBackgroundResource(R.drawable.row_green_background)
        }
        else{
            dndPermissionButton.setBackgroundResource(R.drawable.row_red_background)
        }
    }

    private fun updateCameraBackground(){
        val cameraPermissionButton = findViewById<TextView>(R.id.cameraPermissionText)
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED){
            cameraPermissionButton.setBackgroundResource(R.drawable.row_green_background)
        }
        else{
            cameraPermissionButton.setBackgroundResource(R.drawable.row_red_background)
        }
    }
}
