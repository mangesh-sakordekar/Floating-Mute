package com.example.floatingtools

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.floatingtools.databinding.ActivityContactUsBinding

class ContactUsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactUsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Optional: Set toolbar title
        setSupportActionBar(binding.contactUsToolbar)
        supportActionBar?.title = "Contact Us"
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back arrow

        // Handle back press on toolbar
        binding.contactUsToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // You can also dynamically set the text here if needed
    }
}
