@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.whisper.whisperandroid.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.whisper.whisperandroid.core.ServiceLocator
import com.whisper.whisperandroid.data.PbMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

@Composable
fun ChatThreadScreen(
    peerId: String,
    onBack: () -> Unit
) {
    val backend = ServiceLocator.backend
    val scope = rememberCoroutineScope()
    val meId = backend.currentUser?.id

    var roomId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<PbMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Open (or get) the direct room, then initial load
    LaunchedEffect(peerId) {
        try {
            val room = backend.openOrCreateDirectRoom(peerId)
            roomId = room.id
            messages = backend.listMessages(room.id)
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Failed to open chat"
        }
    }

    // Poll for new messages every 2 seconds (simple realtime)
    LaunchedEffect(roomId) {
        val rid = roomId ?: return@LaunchedEffect
        while (isActive) {
            try {
                messages = backend.listMessages(rid)
            } catch (_: Exception) { /* keep UI alive */ }
            delay(2000)
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Chat") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (error != null) {
            Text(
                error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else if (roomId == null) {
            Text(
                "Opening room…",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Messages list with bubbles
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { m ->
                val isMe = (m.sender == meId)
                MessageBubble(
                    text = m.ciphertext,
                    time = prettyTime(m.created),
                    isMe = isMe
                )
                Spacer(Modifier.height(6.dp))
            }
        }

        // Input row
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Message") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        val rid = roomId
                        if (!input.isBlank() && rid != null) {
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
                    }
                )
            )
            val canSend = roomId != null && input.isNotBlank()
            Button(
                enabled = canSend,
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

@Composable
private fun MessageBubble(
    text: String,
    time: String,
    isMe: Boolean
) {
    val bg = if (isMe) MaterialTheme.colorScheme.primaryContainer
             else MaterialTheme.colorScheme.surfaceVariant
    val align = if (isMe) Arrangement.End else Arrangement.Start

    Row(Modifier.fillMaxWidth(), horizontalArrangement = align) {
        Column(
            Modifier
                .widthIn(max = 320.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(bg)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text(
                time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Very forgiving timestamp: try ISO-8601, else fall back to HH:mm slice
private fun prettyTime(created: String): String {
    return try {
        val t = OffsetDateTime.parse(created)
        "%02d:%02d".format(t.hour, t.minute)
    } catch (_: DateTimeParseException) {
        if (created.length >= 16) created.substring(11, 16) else created
    }
}

