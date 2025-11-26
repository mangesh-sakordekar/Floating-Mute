package com.pandadevs.floatingtools

import android.os.Bundle
import androidx.core.text.HtmlCompat
import androidx.appcompat.app.AppCompatActivity
import com.pandadevs.floatingtools.databinding.ActivityHowToUseBinding
import com.google.android.gms.ads.AdRequest

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

        val txt_launch_guide = getString(R.string.how_to_use_launching)
        val launching_text  = HtmlCompat.fromHtml(txt_launch_guide, HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.txtHTULaunch.text = launching_text

        val txt_tools_guide = getString(R.string.how_to_use_tools)
        val tools_text  = HtmlCompat.fromHtml(txt_tools_guide, HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.txtHTUTools.text = tools_text

        val adRequest = AdRequest.Builder().build()
        binding.howToUseAdView.loadAd(adRequest)
    }
}
