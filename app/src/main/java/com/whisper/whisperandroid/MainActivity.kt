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
import com.whisper.whisperandroid.ui.chat.ChatThreadScreen
import com.whisper.whisperandroid.ui.contacts.ContactsScreen
import com.whisper.whisperandroid.ui.group.GroupChatScreen
import com.whisper.whisperandroid.ui.group.GroupCreateScreen
import com.whisper.whisperandroid.ui.login.LoginScreen
import com.whisper.whisperandroid.ui.register.RegisterScreen
import com.whisper.whisperandroid.ui.stats.StatsScreen
import com.whisper.whisperandroid.ui.theme.WhisperTheme
import com.whisper.whisperandroid.ui.recovery.RecoveryPhraseScreen
import com.whisper.whisperandroid.ui.recovery.ForgotPasswordScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(application)

        setContent {
            WhisperTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()

                    NavHost(navController = nav, startDestination = "login") {

                        // LOGIN
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    val backend = ServiceLocator.backend
                                    // if no recovery hash yet, force user to set phrase
                                    val needsRecovery =
                                        backend.currentUser?.recoveryHash.isNullOrBlank()

                                    if (needsRecovery) {
                                        nav.navigate("recoveryPhrase") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        nav.navigate("chat") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                },
                                onGoToRegister = {
                                    nav.navigate("register")
                                },
                                onForgotPassword = {
                                	nav.navigate("forgotPassword")
            			}
                            )
                        }

                        // RECOVERY PHRASE (one-time setup)
                        composable("recoveryPhrase") {
                            RecoveryPhraseScreen(
                                onDone = {
                                    nav.navigate("chat") {
                                        popUpTo("recoveryPhrase") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("forgotPassword") {
        			ForgotPasswordScreen(
           				onBackToLogin = { nav.popBackStack() }
        			)
    			}

                        composable("stats") {
                            StatsScreen(
                                onBack = { nav.popBackStack() }
                            )
                        }

                        composable("chat") {
                            ChatScreen(
                                onBackToAuth = {
                                    nav.navigate("login") {
                                        // remove chat from backstack so back doesn't go back into it
                                        popUpTo("chat") { inclusive = true }
                                    }
                                },
                                onStartNewChat = { nav.navigate("contacts") },
                                onShowStats = { nav.navigate("stats") },
                                onStartNewGroup = { nav.navigate("groupCreate") },
                                onOpenGroup = { roomId -> nav.navigate("group/$roomId") },
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    // after successful registration we’re already logged in
                                    nav.navigate("chat") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onBackToLogin = {
                                    nav.popBackStack()
                                }
                            )
                        }

                        composable("groupCreate") {
                            GroupCreateScreen(
                                onBack = { nav.popBackStack() },
                                onGroupCreated = { roomId ->
                                    nav.navigate("group/$roomId")
                                }
                            )
                        }

                        composable("group/{roomId}") { backStackEntry ->
                            val roomId =
                                backStackEntry.arguments?.getString("roomId") ?: return@composable
                            GroupChatScreen(
                                roomId = roomId,
                                onBack = { nav.popBackStack() }
                            )
                        }

                        composable("contacts") {
                            ContactsScreen(
                                onBack = { nav.popBackStack() },
                                onSelectContact = { selected ->
                                    nav.navigate("thread/${selected.id}")
                                }
                            )
                        }

                        composable("thread/{peerId}") { backStackEntry ->
                            val peerId =
                                backStackEntry.arguments?.getString("peerId") ?: return@composable
                            ChatThreadScreen(
                                peerId = peerId,
                                onBack = { nav.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

