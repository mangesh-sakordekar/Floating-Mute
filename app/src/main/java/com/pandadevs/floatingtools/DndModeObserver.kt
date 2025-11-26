package com.pandadevs.floatingtools

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings

class DndModeObserver(
    private val context: Context,
    private val onDndModeChanged: (Int) -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)

        val zenMode = Settings.Global.getInt(
            context.contentResolver,
            "zen_mode",
            0
        )
        onDndModeChanged(zenMode)
    }
}
