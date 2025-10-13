package com.example.floatingtools

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.floatingtools.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

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

        // You can also dynamically set the text here if needed
    }
}
