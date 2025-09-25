package com.whisper.whisperandroid

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.whisper.whisperandroid.ui.login.LoginScreen
import com.whisper.whisperandroid.ui.theme.WhisperandroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhisperandroidTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    LoginScreen(
                        onLoginSuccess = {
                            Toast.makeText(this, "Logged in!", Toast.LENGTH_SHORT).show()

                        },
                        onGoToRegister = {
                            Toast.makeText(this, "Go to Register (Step 1.2)", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
