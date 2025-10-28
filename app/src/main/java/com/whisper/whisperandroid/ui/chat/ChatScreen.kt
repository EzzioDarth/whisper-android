package com.whisper.whisperandroid.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whisper.whisperandroid.core.ServiceLocator

@Composable
fun ChatScreen(
    onBackToAuth: () -> Unit = {}
) {
    // Access the current PocketBase session
    val backend = ServiceLocator.backend

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
                text = "PocketBase connection active.\n" +
                        "Backend: ${"http://10.0.2.2:8090"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = { onBackToAuth() }) {
                Text("Sign out")
            }
        }
    }
}
