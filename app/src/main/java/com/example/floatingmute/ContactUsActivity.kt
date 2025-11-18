package com.example.floatingtools

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.floatingtools.databinding.ActivityContactUsBinding
import android.content.Intent
import android.net.Uri

class ContactUsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactUsBinding

    private val linkedInURL = "https://www.linkedin.com/in/mangesh-sakordekar/"
    private val instagramURL = "https://www.instagram.com/hazardous._10/"
    private val githubURL = "https://github.com/mangesh-sakordekar/Floating-Mute"

    private val mailUrl = "mailto:mangeshsakordekar@gmail.com?subject=Floating%20Tools%20Feedback"

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
        binding.imgLinkedIn.setOnClickListener {
            openUrlInBrowser(linkedInURL)
        }

        binding.imgInstagram.setOnClickListener {
            openUrlInBrowser(instagramURL)
        }

        binding.imgGithub.setOnClickListener {
            openUrlInBrowser(githubURL)
        }

        binding.imgMail.setOnClickListener {
            openUrlInBrowser(mailUrl)
        }
    }

    private fun openUrlInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
