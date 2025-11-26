@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.whisper.whisperandroid.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.whisper.whisperandroid.core.ServiceLocator
import com.whisper.whisperandroid.data.PbMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

@Composable
fun GroupChatScreen(
    roomId: String,
    onBack: () -> Unit
) {
    val backend = ServiceLocator.backend
    val realtime = ServiceLocator.realtime
    val scope = rememberCoroutineScope()
    val meId = backend.currentUser?.id
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<PbMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // Initial load
    LaunchedEffect(roomId) {
        try {
            messages = backend.listMessages(roomId)
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Failed to load messages"
        }
    }

    // Polling for updates
    LaunchedEffect(roomId) {
        while (isActive) {
            try {
                val fresh = backend.listMessages(roomId)
                val known = messages.associateBy { it.id }
                messages = (messages + fresh.filter { it.id !in known }).distinctBy { it.id }
            } catch (_: Exception) { }
            delay(2000)
        }
    }

    // Realtime subscription (optional, same pattern as direct chat)
    LaunchedEffect(roomId) {
        realtime.connect()
        realtime.subscribeRoomMessages(roomId) { rec: JSONObject ->
            val m = PbMessage(
                id = rec.optString("id"),
                room = rec.optString("roomId", rec.optString("room")),
                sender = rec.optString("senderId", rec.optString("sender")),
                ciphertext = rec.optString("ciphertext"),
                nonce = rec.optString("nonce"),
                algo = rec.optString("algo"),
                created = rec.optString("created"),
                attachment = null // extend later if needed
            )
            androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                if (messages.none { it.id == m.id }) {
                    messages = messages + m
                }
            }
        }
    }

    DisposableEffect(roomId) {
        onDispose {
            realtime.unsubscribeRoom(roomId)
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // Helper: send message
    fun sendMessage() {
        val text = input.trim()
        if (text.isEmpty() || meId == null) return

        scope.launch {
            try {
                // For now, no group E2E – store plaintext with algo "plaintext"
                val sent = backend.sendMessage(
                    roomId = roomId,
                    ciphertext = text,
                    nonce = "none"
                )
                messages = messages + sent
                input = ""
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Send failed"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { m ->
                    val isMe = (m.sender == meId)

                    val displayText = remember(m.id, m.ciphertext) {
                        // no decryption yet for groups
                        m.ciphertext
                    }

                    MessageBubble(
                        text = displayText,
                        time = prettyTime(m.created),
                        isMe = isMe
                    )

                    Spacer(Modifier.height(6.dp))
                }
            }

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
                    keyboardActions = KeyboardActions(onSend = { sendMessage() })
                )

                Button(
                    enabled = input.isNotBlank(),
                    onClick = { sendMessage() }
                ) {
                    Text("Send")
                }
            }
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

private fun prettyTime(created: String): String {
    return try {
        val t = OffsetDateTime.parse(created)
        "%02d:%02d".format(t.hour, t.minute)
    } catch (_: DateTimeParseException) {
        if (created.length >= 16) created.substring(11, 16) else created
    }
}

