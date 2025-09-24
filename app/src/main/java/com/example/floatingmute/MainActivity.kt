package com.example.floatingtools

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.media.projection.MediaProjectionManager
import android.content.Context
import android.net.Uri
import android.widget.ImageButton
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private val SCREENSHOT_REQUEST_CODE = 1001
    private var _bannerAd: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startMuteButton = findViewById<ImageButton>(R.id.startButton)
        val stopMuteButton = findViewById<ImageButton>(R.id.stopButton)

        val startScreenshotButton = findViewById<ImageButton>(R.id.startScreenshotButton)
        val stopScreenshotButton = findViewById<ImageButton>(R.id.stopScreenshotButton)

        val startBrightnessButton = findViewById<ImageButton>(R.id.startBrightnessButton)
        val stopBrightnessButton = findViewById<ImageButton>(R.id.stopBrightnessButton)

        val startFlashlightButton = findViewById<ImageButton>(R.id.startFlashlightButton)
        val stopFlashlightButton = findViewById<ImageButton>(R.id.stopFlashlightButton)

        MobileAds.initialize(this@MainActivity)
        loadBannerAd()

        // Start Mute Button Service
        startMuteButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, FloatingButtonService::class.java))
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
        }

        // Stop Mute Button Service
        stopMuteButton.setOnClickListener {
            stopService(Intent(this, FloatingButtonService::class.java))
        }

        // Start Brightness Button Service
        startBrightnessButton.setOnClickListener {
            checkAndRequestWriteSettings()
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, BrightnessButtonService::class.java))
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
        }

        // Stop Brightness Button Service
        stopBrightnessButton.setOnClickListener {
            stopService(Intent(this, BrightnessButtonService::class.java))
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
        }

        // Stop Screenshot Button Service
        stopScreenshotButton.setOnClickListener {
            stopService(Intent(this, ScreenshotButtonService::class.java))
        }

        // Start Mute Button Service
        startFlashlightButton.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, FlashlightButtonService::class.java))
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                requestOverlayPermission()
            }
        }

        // Stop Mute Button Service
        stopFlashlightButton.setOnClickListener {
            stopService(Intent(this, FlashlightButtonService::class.java))
        }
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
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun checkAndRequestWriteSettings() {
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            Toast.makeText(this, "Please allow Modify System Settings", Toast.LENGTH_LONG).show()
        }
    }
}
