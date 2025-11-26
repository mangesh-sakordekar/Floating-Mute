package com.example.floatingmute

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager

class RingerModeObserver(
    private val context: Context,
    private val onRingerModeChanged: (Int) -> Unit
) {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val mode = audioManager.ringerMode
                onRingerModeChanged(mode)
            }
        }
    }

    fun register() {
        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        context.registerReceiver(receiver, filter)
    }

    fun unregister() {
        context.unregisterReceiver(receiver)
    }
}