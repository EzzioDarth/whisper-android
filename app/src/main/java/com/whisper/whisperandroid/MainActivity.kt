package com.whisper.whisperandroid

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.whisper.whisperandroid.ui.login.LoginScreen
import com.whisper.whisperandroid.ui.register.RegisterScreen
import com.whisper.whisperandroid.ui.theme.WhisperandroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhisperandroidTheme {
                Surface(color = MaterialTheme.colorScheme.background) {

                    // simple screen state
                    var screen by remember { mutableStateOf("login") }
                    // values: "login", "register", "chat"

                    when (screen) {
                        "login" -> com.whisper.whisperandroid.ui.login.LoginScreen(
                            onLoginSuccess = { screen = "chat" },
                            onGoToRegister = { screen = "register" }
                        )

                        "register" -> com.whisper.whisperandroid.ui.register.RegisterScreen(
                            onRegistered = { screen = "chat" },
                            onGoToLogin = { screen = "login" }
                        )

                        "chat" -> com.whisper.whisperandroid.ui.chat.ChatScreen(
                            onBackToAuth = { screen = "login" }
                        )
                    }
                }
            }
        }

    }
}
