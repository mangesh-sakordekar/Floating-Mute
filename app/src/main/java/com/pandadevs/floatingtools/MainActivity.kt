package com.pandadevs.floatingtools

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.IntentFilter
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout


class MainActivity : AppCompatActivity() {

    private val SCREENSHOT_REQUEST_CODE = 1001
    private val OVERLAY_REQUEST_CODE = 1002
    private val WRITE_REQUEST_CODE = 1003
    private val DND_REQUEST_CODE = 1004

    private val CAMERA_PERMISSION_REQUEST = 2001

    private var _bannerAd: AdView? = null

    private var isMenuOpen = false

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("floating_notes", Context.MODE_PRIVATE)

        val startMuteButton = findViewById<LinearLayout>(R.id.muteText)

        val startScreenshotButton = findViewById<LinearLayout>(R.id.screenshotText)

        val startBrightnessButton = findViewById<LinearLayout>(R.id.brightnessText)

        val startFlashlightButton = findViewById<LinearLayout>(R.id.flashlightText)

        val startStopwatchButton = findViewById<LinearLayout>(R.id.stopwatchText)

        val startNotificationButton = findViewById<LinearLayout>(R.id.notificationText)

        val startDNDButton = findViewById<LinearLayout>(R.id.dndText)

        val startScreenOnButton = findViewById<LinearLayout>(R.id.screenOnText)

        val startTimerButton = findViewById<LinearLayout>(R.id.countdownTimerText)

        val startMirrorButton = findViewById<LinearLayout>(R.id.mirrorText)

        val startCalculatorButton = findViewById<LinearLayout>(R.id.calculatorText)

        val startFontSizeButton = findViewById<LinearLayout>(R.id.fontSizeText)

        val startNotepadButton = findViewById<LinearLayout>(R.id.notepadText)

        val startStickyNotesButton = findViewById<LinearLayout>(R.id.stickyNotesText)

        val menuIcon = findViewById<ImageView>(R.id.menuIcon)
        val rightMenu = findViewById<LinearLayout>(R.id.rightMenuPanel)
        val closeBtn = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.closeServicesButton)


        MobileAds.initialize(this@MainActivity)
        loadBannerAd()

        rightMenu.setOnClickListener {
            menuIcon.performClick()
        }

        findViewById<LinearLayout>(R.id.howToUseOption).setOnClickListener {
            val intent = Intent(this, HowToUseActivity::class.java)
            startActivity(intent)
            menuIcon.performClick()
        }

        findViewById<LinearLayout>(R.id.aboutAppOption).setOnClickListener {
            val intent = Intent(this, AboutAppActivity::class.java)
            startActivity(intent)
            menuIcon.performClick()
        }

        findViewById<LinearLayout>(R.id.contactUsOption).setOnClickListener {
            val intent = Intent(this, ContactUsActivity::class.java)
            startActivity(intent)
            menuIcon.performClick()
        }

        findViewById<LinearLayout>(R.id.settingsOption).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            menuIcon.performClick()
        }


        menuIcon.setOnClickListener {
            val targetX = if (isMenuOpen) rightMenu.width.toFloat() else 0f
            rightMenu.animate()
                .translationX(targetX)
                .setDuration(300)
                .start()
            isMenuOpen = !isMenuOpen
        }

        closeBtn.setOnClickListener {
            // Stop all services here
            stopService(Intent(this, FloatingButtonService::class.java))
            stopService(Intent(this, BrightnessButtonService::class.java))
            stopService(Intent(this, NotificationModeButtonService::class.java))
            stopService(Intent(this, DNDButtonService::class.java))
            stopService(Intent(this, FontSizeButtonService::class.java))
            stopService(Intent(this, CountdownTimerButtonService::class.java))
            stopService(Intent(this, StopwatchButtonService::class.java))
            stopService(Intent(this, ScreenOnButtonService::class.java))
            stopService(Intent(this, FlashlightButtonService::class.java))
            stopService(Intent(this, MirrorFloatingService::class.java))
            stopService(Intent(this, CalculatorButtonService::class.java))
            stopService(Intent(this, NotepadButtonService::class.java))
            stopService(Intent(this, StickyNotesButtonService::class.java))
            stopService(Intent(this, ScreenshotButtonService::class.java))

            Toast.makeText(this, "Closed all tools.", Toast.LENGTH_SHORT).show()
        }


        // Start Mute Button Service
        startMuteButton.setOnClickListener {
            if(!isMyServiceRunning(FloatingButtonService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this, FloatingButtonService::class.java))
                    startMuteButton.setBackgroundResource(R.drawable.row_red_background)
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
            }
            else{
                stopService(Intent(this, FloatingButtonService::class.java))
            }
            loadBannerAd()
        }

        // Start Brightness Button Service
        startBrightnessButton.setOnClickListener {
            if(!isMyServiceRunning(BrightnessButtonService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    if (Settings.System.canWrite(this)) {
                        startService(Intent(this, BrightnessButtonService::class.java))
                        startBrightnessButton.setBackgroundResource(R.drawable.row_red_background)
                    } else {
                        Toast.makeText(
                            this,
                            "Write Settings permission required",
                            Toast.LENGTH_SHORT
                        ).show()
                        checkAndRequestWriteSettings()
                    }
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }

            }
            else{
                stopService(Intent(this, BrightnessButtonService::class.java))
            }
            loadBannerAd()
        }


        // Start Screenshot Button Service
        startScreenshotButton.setOnClickListener {
            if(!isMyServiceRunning(ScreenshotButtonService::class.java)) {
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
            else{
                stopService(Intent(this, ScreenshotButtonService::class.java))
            }
            loadBannerAd()
        }


        // Start Flashlight Service
        startFlashlightButton.setOnClickListener {
            if(!isMyServiceRunning(FlashlightButtonService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this, FlashlightButtonService::class.java))
                    startFlashlightButton.setBackgroundResource(R.drawable.row_red_background)
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
            }
            else{
                stopService(Intent(this, FlashlightButtonService::class.java))
            }
            loadBannerAd()
        }


        // Start Mute Button Service
        startStopwatchButton.setOnClickListener {
            if(!isMyServiceRunning(StopwatchButtonService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this, StopwatchButtonService::class.java))
                    startStopwatchButton.setBackgroundResource(R.drawable.row_red_background)
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
            }
            else{
                stopService(Intent(this, StopwatchButtonService::class.java))
            }
            loadBannerAd()
        }


        // Start Notification Button Service
        startNotificationButton.setOnClickListener {
            if(!isMyServiceRunning(NotificationModeButtonService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (notificationManager.isNotificationPolicyAccessGranted) {
                        startService(Intent(this, NotificationModeButtonService::class.java))
                        startNotificationButton.setBackgroundResource(R.drawable.row_red_background)
                    } else {
                        Toast.makeText(
                            this,
                            "Notification Policy permission required",
                            Toast.LENGTH_SHORT
                        ).show()
                        changeDoNotDisturbState()
                    }
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
            }
            else{
                stopService(Intent(this, NotificationModeButtonService::class.java))
            }
            loadBannerAd()
        }


        // Start DND Button Service
        startDNDButton.setOnClickListener {
            if(!isMyServiceRunning(DNDButtonService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (notificationManager.isNotificationPolicyAccessGranted) {
                        startService(Intent(this, DNDButtonService::class.java))
                        startDNDButton.setBackgroundResource(R.drawable.row_red_background)
                    } else {
                        Toast.makeText(
                            this,
                            "Notification Policy permission required",
                            Toast.LENGTH_SHORT
                        ).show()
                        changeDoNotDisturbState()
                    }
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
            }
            else{
                stopService(Intent(this, DNDButtonService::class.java))
            }
            loadBannerAd()
        }


        // Start Screen On Button Service
        startScreenOnButton.setOnClickListener {
            if(!isMyServiceRunning(ScreenOnButtonService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this, ScreenOnButtonService::class.java))
                    startScreenOnButton.setBackgroundResource(R.drawable.row_red_background)
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
            }
            else{
                stopService(Intent(this, ScreenOnButtonService::class.java))
            }
            loadBannerAd()
        }


        // Start Timer Button Service
        startTimerButton.setOnClickListener {
            if(!isMyServiceRunning(CountdownTimerButtonService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this, CountdownTimerButtonService::class.java))
                    startTimerButton.setBackgroundResource(R.drawable.row_red_background)
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
            }
            else{
                stopService(Intent(this, CountdownTimerButtonService::class.java))
            }
            loadBannerAd()
        }

        // Start Mirror Button Service
        startMirrorButton.setOnClickListener {
            if(!isMyServiceRunning(MirrorFloatingService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        startService(Intent(this, MirrorFloatingService::class.java))
                        startMirrorButton.setBackgroundResource(R.drawable.row_red_background)
                    } else {
                        Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT)
                            .show()
                        requestCameraPermission()
                    }
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
            }
            else{
                stopService(Intent(this, MirrorFloatingService::class.java))
            }
            loadBannerAd()
        }


        // Start Calculator Button Service
        startCalculatorButton.setOnClickListener {
            if(!isMyServiceRunning(CalculatorButtonService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this, CalculatorButtonService::class.java))
                    startCalculatorButton.setBackgroundResource(R.drawable.row_red_background)
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
            }
            else{
                stopService(Intent(this, CalculatorButtonService::class.java))
            }
            loadBannerAd()
        }


        // Start Font Size Button Service
        startFontSizeButton.setOnClickListener {
            if(!isMyServiceRunning(FontSizeButtonService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    if (Settings.System.canWrite(this)) {
                        startService(Intent(this, FontSizeButtonService::class.java))
                        startFontSizeButton.setBackgroundResource(R.drawable.row_red_background)
                    } else {
                        Toast.makeText(
                            this,
                            "Write Settings permission required",
                            Toast.LENGTH_SHORT
                        ).show()
                        checkAndRequestWriteSettings()
                    }
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
            }
            else{
                stopService(Intent(this, FontSizeButtonService::class.java))
            }
            loadBannerAd()
        }


        // Start Notes Button Service
        startNotepadButton.setOnClickListener {
            if(!isMyServiceRunning(NotepadButtonService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this, NotepadButtonService::class.java))
                    startNotepadButton.setBackgroundResource(R.drawable.row_red_background)
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
            }
            else{
                stopService(Intent(this, NotepadButtonService::class.java))
            }
            loadBannerAd()
        }


        // Start Sticky Notes Button Service
        startStickyNotesButton.setOnClickListener {
            if(!isMyServiceRunning(StickyNotesButtonService::class.java)) {
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this, StickyNotesButtonService::class.java))
                    startStickyNotesButton.setBackgroundResource(R.drawable.row_red_background)
                } else {
                    Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
            }
            else{
                stopService(Intent(this, StickyNotesButtonService::class.java))
            }
            loadBannerAd()
        }

        if(prefs.getBoolean("flag_firstLaunch", true)){
            showDialog()
            prefs.edit().putBoolean("flag_firstLaunch", false).apply()
        }

    }

    private fun showDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dailog_first_launch, null)
        dialogView.findViewById<TextView>(R.id.flOverlay).setOnClickListener { requestOverlayPermission() }
        dialogView.findViewById<TextView>(R.id.flWrite).setOnClickListener { checkAndRequestWriteSettings() }
        dialogView.findViewById<TextView>(R.id.flDND).setOnClickListener { changeDoNotDisturbState() }
        dialogView.findViewById<TextView>(R.id.flCamera).setOnClickListener { requestCameraPermission() }

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_NoActionBar)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.floating_calculator_bg)

        dialogView.findViewById<TextView>(R.id.flDone).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<TextView>(R.id.flLearnMore).setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, HowToUseActivity::class.java)
            startActivity(intent)
        }

        dialog.show()
    }
    private val serviceDestroyedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "SERVICE_DESTROYED") {
                val msg = intent.getStringExtra("message")
                // ✅ Update your UI element here
                when(msg){
                    "Mute Button" -> findViewById<LinearLayout>(R.id.muteText).setBackgroundResource(R.drawable.row_background)
                    "Brightness Button" -> findViewById<LinearLayout>(R.id.brightnessText).setBackgroundResource(R.drawable.row_background)
                    "Alert Button" -> findViewById<LinearLayout>(R.id.notificationText).setBackgroundResource(R.drawable.row_background)
                    "DND Button" -> findViewById<LinearLayout>(R.id.dndText).setBackgroundResource(R.drawable.row_background)
                    "Font Button" -> findViewById<LinearLayout>(R.id.fontSizeText).setBackgroundResource(R.drawable.row_background)
                    "Flashlight Button" -> findViewById<LinearLayout>(R.id.flashlightText).setBackgroundResource(R.drawable.row_background)
                    "Stopwatch Button" -> findViewById<LinearLayout>(R.id.stopwatchText).setBackgroundResource(R.drawable.row_background)
                    "Screen On Button" -> findViewById<LinearLayout>(R.id.screenOnText).setBackgroundResource(R.drawable.row_background)
                    "Timer Button" -> findViewById<LinearLayout>(R.id.countdownTimerText).setBackgroundResource(R.drawable.row_background)
                    "Mirror Button" -> findViewById<LinearLayout>(R.id.mirrorText).setBackgroundResource(R.drawable.row_background)
                    "Calculator Button" -> findViewById<LinearLayout>(R.id.calculatorText).setBackgroundResource(R.drawable.row_background)
                    "Notepad Button" -> findViewById<LinearLayout>(R.id.notepadText).setBackgroundResource(R.drawable.row_background)
                    "Sticky Notes Button" -> findViewById<LinearLayout>(R.id.stickyNotesText).setBackgroundResource(R.drawable.row_background)
                    "Screenshot Button" -> findViewById<LinearLayout>(R.id.screenshotText).setBackgroundResource(R.drawable.row_background)
                }

            }
        }
    }

    private fun updateButtonBackground(currentView: LinearLayout, flag: Boolean){
        if(flag){
            currentView.setBackgroundResource(R.drawable.row_red_background)
        }
        else{
            currentView.setBackgroundResource(R.drawable.row_background)
        }
    }

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(serviceDestroyedReceiver, IntentFilter("SERVICE_DESTROYED"))


        updateButtonBackground(findViewById(R.id.muteText), isMyServiceRunning(FloatingButtonService::class.java))
        updateButtonBackground(findViewById(R.id.brightnessText), isMyServiceRunning(BrightnessButtonService::class.java))
        updateButtonBackground(findViewById(R.id.notificationText), isMyServiceRunning(
            NotificationModeButtonService::class.java))
        updateButtonBackground(findViewById(R.id.dndText), isMyServiceRunning(DNDButtonService::class.java))
        updateButtonBackground(findViewById(R.id.fontSizeText), isMyServiceRunning(FontSizeButtonService::class.java))
        updateButtonBackground(findViewById(R.id.flashlightText), isMyServiceRunning(FlashlightButtonService::class.java))
        updateButtonBackground(findViewById(R.id.stopwatchText), isMyServiceRunning(StopwatchButtonService::class.java))
        updateButtonBackground(findViewById(R.id.screenOnText), isMyServiceRunning(ScreenOnButtonService::class.java))
        updateButtonBackground(findViewById(R.id.countdownTimerText), isMyServiceRunning(CountdownTimerButtonService::class.java))
        updateButtonBackground(findViewById(R.id.mirrorText), isMyServiceRunning(MirrorFloatingService::class.java))
        updateButtonBackground(findViewById(R.id.calculatorText), isMyServiceRunning(CalculatorButtonService::class.java))
        updateButtonBackground(findViewById(R.id.notepadText), isMyServiceRunning(NotepadButtonService::class.java))
        updateButtonBackground(findViewById(R.id.stickyNotesText), isMyServiceRunning(StickyNotesButtonService::class.java))
        updateButtonBackground(findViewById(R.id.screenshotText), isMyServiceRunning(ScreenshotButtonService::class.java))


    }


    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(serviceDestroyedReceiver)
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
            findViewById<LinearLayout>(R.id.screenshotText).setBackgroundResource(R.drawable.row_red_background)
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
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
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
            startActivity(intent)
            //Toast.makeText(this, "Please allow Modify System Settings", Toast.LENGTH_LONG).show()
        }
    }

    private fun changeDoNotDisturbState() {
        // Check if the permission is granted
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {

            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required if starting from a Service
            startActivity(intent)
        }
    }



    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.Companion.MAX_VALUE)) {
            if (serviceClass.getName() == service.service.getClassName()) {
                return true
            }
        }
        return false
    }
}
