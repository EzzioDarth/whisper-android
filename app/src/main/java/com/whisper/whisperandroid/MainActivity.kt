package com.whisper.whisperandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.whisper.whisperandroid.core.ServiceLocator
import com.whisper.whisperandroid.ui.chat.ChatScreen
import com.whisper.whisperandroid.ui.login.LoginScreen
import com.whisper.whisperandroid.ui.theme.WhisperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(application)

        setContent {
            WhisperTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "login") {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { nav.navigate("chat") },
                                onGoToRegister = { /* we'll wire later */ }
                            )
                        }
                        composable("chat") {
                            ChatScreen(
                                onBackToAuth = {
                                    nav.popBackStack()
                                }
                            ) }
                        // composable("register") { RegisterScreen(onRegisterSuccess = { nav.navigate("chat") }) }
                    }
                }
            }
        }
    }
}
