package com.pandadevs.floatingtools

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.pandadevs.floatingtools.databinding.ActivityAboutAppBinding
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

        binding.AAAppIcon.setOnClickListener { showDialog() }

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

    private fun showDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_acknowledgement, null)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_NoActionBar)
            .setView(dialogView)
            .create()

        val txt_acks = getString(R.string.acknowledgements)
        val ack_text  = HtmlCompat.fromHtml(txt_acks, HtmlCompat.FROM_HTML_MODE_LEGACY)
        dialogView.findViewById<TextView>(R.id.ackText).text = ack_text
        dialog.window?.setBackgroundDrawableResource(R.drawable.floating_calculator_bg)

        dialogView.findViewById<TextView>(R.id.ackDone).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
