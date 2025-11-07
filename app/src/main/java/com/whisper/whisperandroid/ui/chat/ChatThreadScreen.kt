package com.whisper.whisperandroid.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whisper.whisperandroid.core.ServiceLocator
import com.whisper.whisperandroid.data.PbMessage
import kotlinx.coroutines.launch

@Composable
fun ChatThreadScreen(
    peerId: String,
    onBack: () -> Unit
) {
    val backend = ServiceLocator.backend
    val scope = rememberCoroutineScope()
    var roomId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<PbMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(peerId) {
        scope.launch {
            try {
                val room = backend.openOrCreateDirectRoom(peerId)
                roomId = room.id
                messages = backend.listMessages(room.id)
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Failed to open chat"
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("Chat", style = MaterialTheme.typography.headlineSmall)
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(Modifier.weight(1f)) {
            items(messages) { m ->
                Text(m.ciphertext) // plaintext for now
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Message") }
            )
            Button(
                enabled = roomId != null && input.isNotBlank(),
                onClick = {
                    val rid = roomId ?: return@Button
                    scope.launch {
                        try {
                            val sent = backend.sendMessage(rid, input, null)
                            input = ""
                            messages = messages + sent
                        } catch (e: Exception) {
                            error = e.localizedMessage ?: "Send failed"
                        }
                    }
                }
            ) { Text("Send") }
        }
    }
}

