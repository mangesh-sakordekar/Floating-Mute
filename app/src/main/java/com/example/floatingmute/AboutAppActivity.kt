package com.example.floatingtools

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.example.floatingtools.databinding.ActivityAboutAppBinding
import com.google.android.gms.ads.AdRequest

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

        val txt_about_app = getString(R.string.about_app)
        val launching_text  = HtmlCompat.fromHtml(txt_about_app, HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.txtAAAboutApp.text = launching_text

        val txt_upcoming_features = getString(R.string.about_app_future)
        val feature_text  = HtmlCompat.fromHtml(txt_upcoming_features, HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.txtAAUpcomingFeatures.text = feature_text
        // You can also dynamically set the text here if needed

        val adRequest = AdRequest.Builder().build()
        binding.aboutAppAdView.loadAd(adRequest)
    }

}
