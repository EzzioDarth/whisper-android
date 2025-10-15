package com.whisper.whisperandroid.core


import android.app.Application
import com.whisper.whisperandroid.BuildConfig
import com.whisper.whisperandroid.data.ChatBackend
import com.whisper.whisperandroid.data.pb.PocketBaseBackend
// If you still have a Firebase backend class, import it; else keep PB only.
//import com.whisper.whisperandroid.data.firebase.FirebaseBackend

object ServiceLocator {
    private lateinit var app: Application
    lateinit var backend: ChatBackend
        private set

    fun init(app: Application) {
        this.app = app
        backend = if (BuildConfig.USE_PB) {
            PocketBaseBackend(app, PbConfig.BASE)
        } else {
            // FirebaseBackend(app)
            PocketBaseBackend(app, PbConfig.BASE) // fallback to PB if no Firebase class
        }
    }
}
