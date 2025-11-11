package com.whisper.whisperandroid.core

import android.app.Application
import com.whisper.whisperandroid.BuildConfig
import com.whisper.whisperandroid.data.ChatBackend
import com.whisper.whisperandroid.data.PocketBaseBackend
import com.whisper.whisperandroid.data.PbRealtime  // make sure this import exists

object ServiceLocator {
    private lateinit var appRef: Application
    lateinit var backend: ChatBackend
        private set

    // Initialize the backend when the app starts
    fun init(app: Application) {
        appRef = app
        backend = if (BuildConfig.USE_PB) {
            PocketBaseBackend(appRef, PbConfig.BASE)
        } else {
            PocketBaseBackend(appRef, PbConfig.BASE) // using PB for now
        }
    }

    // Realtime socket connection shared app-wide
    val realtime: PbRealtime by lazy {
        PbRealtime(
            baseUrl = PbConfig.BASE, // match your PbConfig property name
            tokenProvider = { backend.token }
        )
    }
}

