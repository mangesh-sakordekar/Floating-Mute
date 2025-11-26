package com.pandadevs.floatingtools

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pandadevs.floatingtools.databinding.ActivitySettingsBinding
import android.content.SharedPreferences
import com.google.android.gms.ads.AdRequest

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val OVERLAY_REQUEST_CODE = 1002
    private val WRITE_REQUEST_CODE = 1003
    private val DND_REQUEST_CODE = 1004

    private val CAMERA_PERMISSION_REQUEST = 2001

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Optional: Set toolbar title
        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back arrow

        // Handle back press on toolbar
        binding.settingsToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        prefs = getSharedPreferences("floating_notes", Context.MODE_PRIVATE)

        val overlaySwitch = findViewById<Switch>(R.id.overlaySwitch)
        val writeSwitch = findViewById<Switch>(R.id.writeSwitch)
        val notificationSwitch = findViewById<Switch>(R.id.notificationSwitch)
        val cameraSwitch = findViewById<Switch>(R.id.cameraSwitch)

        val launchToolsSwitch = findViewById<Switch>(R.id.launchToolSwitch)
        launchToolsSwitch.isChecked = prefs.getBoolean("flag_launchTools", true)

        val snapToEdgeSwitch = findViewById<Switch>(R.id.snapToEdgeSwitch)
        snapToEdgeSwitch.isChecked = prefs.getBoolean("flag_snapToEdge", true)

        val bottomEdgeSwitch = findViewById<Switch>(R.id.bottomEdgeSwitch)
        bottomEdgeSwitch.isChecked = prefs.getBoolean("flag_bottomEdge", true)

        updateSwitch(overlaySwitch, Settings.canDrawOverlays(this))
        updateSwitch(writeSwitch, Settings.System.canWrite(this))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        updateSwitch(notificationSwitch, notificationManager.isNotificationPolicyAccessGranted)
        updateSwitch(cameraSwitch,
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)


        overlaySwitch.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Overlay permission already granted!", Toast.LENGTH_SHORT).show()
            } else {
                requestOverlayPermission()
            }
        }

        writeSwitch.setOnClickListener {
            if (Settings.System.canWrite(this)) {
                Toast.makeText(this, "Write Settings permission already granted", Toast.LENGTH_SHORT).show()
            }
            else{
                requestWriteSettingsPermission()
            }
        }

        notificationSwitch.setOnClickListener {
            if (notificationManager.isNotificationPolicyAccessGranted){
                Toast.makeText(this, "Notification Policy permission already granted!", Toast.LENGTH_SHORT).show()
            }
            else{
                requestNotificationPermission()
            }

        }

        cameraSwitch.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission already granted", Toast.LENGTH_SHORT).show()
            }
            else{
                requestCameraPermission()
            }
        }

        launchToolsSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("flag_launchTools", isChecked).apply()
        }

        snapToEdgeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("flag_snapToEdge", isChecked).apply()
        }

        bottomEdgeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("flag_bottomEdge", isChecked).apply()
        }

        val adRequest = AdRequest.Builder().build()
        binding.settingsAdView.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()
        updateSwitch(findViewById<Switch>(R.id.overlaySwitch),
            Settings.canDrawOverlays(this))
        updateSwitch(findViewById<Switch>(R.id.writeSwitch),
            Settings.System.canWrite(this))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        updateSwitch(findViewById<Switch>(R.id.notificationSwitch),
            notificationManager.isNotificationPolicyAccessGranted)
        updateSwitch(findViewById<Switch>(R.id.cameraSwitch),
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED)

    }
    private fun updateSwitch(currentSwitch: Switch, condition: Boolean){
        if(condition){
            currentSwitch.isChecked = true
            currentSwitch.isEnabled = false
        }
        else{
            currentSwitch.isChecked = false
            currentSwitch.isEnabled = true
        }
    }

    // Handle screenshot permission result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_REQUEST_CODE){
            updateSwitch(findViewById<Switch>(R.id.overlaySwitch),
                Settings.canDrawOverlays(this))
        } else if (requestCode == WRITE_REQUEST_CODE){
            updateSwitch(findViewById<Switch>(R.id.writeSwitch),
                Settings.System.canWrite(this))
        } else if (requestCode == DND_REQUEST_CODE){
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            updateSwitch(findViewById<Switch>(R.id.notificationSwitch),
                notificationManager.isNotificationPolicyAccessGranted)
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_REQUEST_CODE)
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
                updateSwitch(findViewById<Switch>(R.id.cameraSwitch),
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED)
            }
        }
    }

    private fun requestWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityForResult(intent, WRITE_REQUEST_CODE)
    }

    private fun requestNotificationPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required if starting from a Service
        startActivityForResult(intent, DND_REQUEST_CODE)

    }


}
