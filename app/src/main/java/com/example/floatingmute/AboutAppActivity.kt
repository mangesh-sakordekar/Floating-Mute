package com.example.floatingtools

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.floatingtools.databinding.ActivityAboutAppBinding

class AboutAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Optional: Set toolbar title
        setSupportActionBar(binding.aboutAppToolbar)
        supportActionBar?.title = "About App"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back arrow

        // Handle back press on toolbar
        binding.aboutAppToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // You can also dynamically set the text here if needed
    }
}
