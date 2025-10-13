package com.example.floatingtools

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.floatingtools.databinding.ActivityHowToUseBinding

class HowToUseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHowToUseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHowToUseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Optional: Set toolbar title
        setSupportActionBar(binding.howToUseToolbar)
        supportActionBar?.title = "How to Use"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back arrow

        // Handle back press on toolbar
        binding.howToUseToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // You can also dynamically set the text here if needed
    }
}
