package com.example.floatingmute

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.media.AudioManager

class VolumeObserver(
    private val context: Context,
    private val onVolumeChanged: (newVolume: Int) -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)

        val newVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        onVolumeChanged(newVolume)
    }
}
