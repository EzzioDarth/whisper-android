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
                    var showRegister by remember { mutableStateOf(false) }

                    if (showRegister) {
                        RegisterScreen(
                            onRegistered = {
                                Toast.makeText(this, "Account created! Logged in.", Toast.LENGTH_SHORT).show()
                                // TODO: navigate to ChatScreen next (Step 1.5). For now, go back to login or stay here.
                                showRegister = false
                            },
                            onGoToLogin = { showRegister = false }
                        )
                    } else {
                        LoginScreen(
                            onLoginSuccess = {
                                Toast.makeText(this, "Logged in!", Toast.LENGTH_SHORT).show()
                                // TODO: navigate to ChatScreen (Step 1.5)
                            },
                            onGoToRegister = { showRegister = true }
                        )
                    }
                }
            }
        }
    }
}
