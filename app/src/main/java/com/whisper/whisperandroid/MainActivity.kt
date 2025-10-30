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
import com.whisper.whisperandroid.ui.contacts.ContactsScreen

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
                                onBackToAuth = { nav.popBackStack()},
                                onStartNewChat = { nav.navigate("contacts")}

                            ) }
                        // composable("register") { RegisterScreen(onRegisterSuccess = { nav.navigate("chat") }) }


                        composable("contacts") {
                            ContactsScreen(
                                onBack = { nav.popBackStack() },
                                onSelectContact = { selected ->
                                    // TODO: open or create a chat with `selected.id`
                                    // nav.navigate("chat/${selected.id}")
                                    nav.popBackStack() // for now, just go back to ChatScreen
                                }
                            )
                        }


                    }
                }
            }
        }
    }
}
