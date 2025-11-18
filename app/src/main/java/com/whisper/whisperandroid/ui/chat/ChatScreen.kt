package com.whisper.whisperandroid.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whisper.whisperandroid.core.ServiceLocator
import com.whisper.whisperandroid.core.PbConfig
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch


@Composable
fun ChatScreen(
    onBackToAuth: () -> Unit = {},
    onStartNewChat: () -> Unit = {},
    onShowStats: () -> Unit = {}
) {
    // Access the current PocketBase session
    val backend = ServiceLocator.backend
    val scope = rememberCoroutineScope()
    var eraseLoading by remember { mutableStateOf(false) }
    var eraseError by remember { mutableStateOf<String?>(null) }
    var eraseResult by remember { mutableStateOf<String?>(null) }
    //safe guard to not load back the chat screen in case of null token
    //suggested by chatgpt
    LaunchedEffect(Unit) {
        if (backend.token == null) {
            onBackToAuth()
        }
    }
    //valid session this shows
    val user = backend.currentUser
    val who = when {
        user?.displayName?.isNotBlank() == true -> user.displayName
        user?.username?.isNotBlank() == true -> user.username
        user?.email?.isNotBlank() ==true -> user.email
        else -> user?.id ?: "Unknown user"
    }

    // We don’t have persistent user info yet,
    // so this is just a placeholder message.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✅ Logged in successfully!",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Hello, $who",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onStartNewChat) {
                Text("Start new chat")
            }
            Spacer(Modifier.height(16.dp))
Button(onClick = onShowStats) {
    Text("View my stats")
}

            Spacer(Modifier.height(12.dp))
            Text(
                text = "PocketBase connection active.\n" +
                        "Backend: ${PbConfig.BASE}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(32.dp))
            Button(
                enabled = !eraseLoading,
                onClick = {
                    eraseError = null
                    eraseResult = null
                    eraseLoading = true

                    scope.launch {
                        try {
                            backend.eraseAllMyMessages()
                            eraseResult = "All messages you sent have been erased from the server."
                        } catch (e: Exception) {
                            eraseError = e.localizedMessage ?: "Failed to erase messages."
                        } finally {
                            eraseLoading = false
                        }
                    }
                }
            ) {
                Text(if (eraseLoading) "Erasing…" else "Erase all my messages")
            }

            eraseError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            eraseResult?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "PocketBase connection active.\n" +
                        "Backend: ${PbConfig.BASE}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                backend.signOut()
                onBackToAuth()
            }
            ) {
                Text("Sign out")
            }
        }
    }
}
