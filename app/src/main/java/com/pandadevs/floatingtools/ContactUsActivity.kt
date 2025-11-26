package com.pandadevs.floatingtools

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pandadevs.floatingtools.databinding.ActivityContactUsBinding
import android.content.Intent
import android.net.Uri
import com.google.android.gms.ads.AdRequest

class ContactUsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactUsBinding

    private val linkedInURL = "https://www.linkedin.com/in/mangesh-sakordekar/"
    private val instagramURL = "https://www.instagram.com/panda.devs/"
    private val githubURL = "https://github.com/mangesh-sakordekar/Floating-Mute"

    private val mailURL = "mailto:pandadevs.apps@gmail.com?subject=Floating%20Tools%20Feedback"

    private val playStoreURL = "https://play.google.com/"

    private val fdroidURL = "https://f-droid.org/"

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
            openUrlInBrowser(mailURL)
        }

        binding.imgGPlay.setOnClickListener {
            openUrlInBrowser(playStoreURL)
        }

        binding.imgFDroid.setOnClickListener {
            openUrlInBrowser(fdroidURL)
        }

        val adRequest = AdRequest.Builder().build()
        binding.contactUsAdView.loadAd(adRequest)
    }

    private fun openUrlInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}
