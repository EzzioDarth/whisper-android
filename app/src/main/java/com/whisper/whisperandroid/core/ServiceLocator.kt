package com.whisper.whisperandroid.core

import android.app.Application
import com.whisper.whisperandroid.BuildConfig
import com.whisper.whisperandroid.data.ChatBackend
import com.whisper.whisperandroid.data.PocketBaseBackend

object ServiceLocator {
    private lateinit var appRef: Application
    lateinit var backend: ChatBackend
        private set

    fun init(app: Application) {
        appRef = app
        backend = if (BuildConfig.USE_PB) {
            PocketBaseBackend(appRef, PbConfig.BASE)
        } else {
            PocketBaseBackend(appRef, PbConfig.BASE) // keep PB for now
        }
    }
}
