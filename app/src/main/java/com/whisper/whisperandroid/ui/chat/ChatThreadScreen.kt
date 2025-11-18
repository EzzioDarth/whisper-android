@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.whisper.whisperandroid.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.whisper.whisperandroid.core.ServiceLocator
import com.whisper.whisperandroid.data.Crypto
import com.whisper.whisperandroid.data.PbMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import android.content.Intent
//import android.net.Uri
import com.whisper.whisperandroid.core.PbConfig
import android.media.MediaRecorder
import android.Manifest
import java.io.File

@Composable
fun ChatThreadScreen(
    peerId: String,
    onBack: () -> Unit
) {
    val backend = ServiceLocator.backend
    val realtime = ServiceLocator.realtime
    val scope = rememberCoroutineScope()
    val meId = backend.currentUser?.id
    val listState = rememberLazyListState()

    var roomId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<PbMessage>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    // 🔹 Attachment state
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }

    // File picker launcher
    val pickAttachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachmentUri = uri
    }
    val context = LocalContext.current

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
        val audioPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                val rec = MediaRecorder()

                rec.setAudioSource(MediaRecorder.AudioSource.MIC)
                rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                rec.setAudioEncodingBitRate(128000)
                rec.setAudioSamplingRate(44100)
                rec.setOutputFile(file.absolutePath)

                rec.prepare()
                rec.start()

                recorder = rec
                recordingFile = file
                isRecording = true
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Failed to start recording"
            }
        } else {
            error = "Microphone permission denied"
        }
    }



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

    // Poll for new messages every 2 seconds (fallback)
    LaunchedEffect(roomId) {
        val rid = roomId ?: return@LaunchedEffect
        while (isActive) {
            try {
                val fresh = backend.listMessages(rid)
                val known = messages.associateBy { it.id }
                messages = (messages + fresh.filter { it.id !in known }).distinctBy { it.id }
            } catch (_: Exception) {
            }
            delay(2000)
        }
    }

    // Realtime subscription for this room
    LaunchedEffect(roomId) {
        val rid = roomId ?: return@LaunchedEffect
        realtime.connect()
        realtime.subscribeRoomMessages(rid) { rec: JSONObject ->
            // NOTE: your schema uses "room" and "sender"
            val att = rec.optString("attachment", null)

            val m = PbMessage(
                id = rec.optString("id"),
                room = rec.optString("room"),
                sender = rec.optString("sender"),
                ciphertext = rec.optString("ciphertext"),
                nonce = rec.optString("nonce"),
                algo = rec.optString("algo"),
                created = rec.optString("created"),
                attachment = att.takeIf { !it.isNullOrBlank() }
            )
            androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
                if (messages.none { it.id == m.id }) {
                    messages = messages + m
                }
            }
        }
    }

    // Unsubscribe when leaving this screen
    DisposableEffect(roomId) {
        onDispose { roomId?.let { realtime.unsubscribeRoom(it) } }
    }
    DisposableEffect(Unit) {
        onDispose {
            recorder?.let {
                try { it.stop() } catch (_: Exception) {}
                it.release()
            }
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // 🔹 Helper: send current message + optional attachment
    fun sendCurrentMessage() {
        val rid = roomId ?: return
        val me = meId ?: return
        if (input.isBlank() && attachmentUri == null) return

        // Encrypt text if present, otherwise send dummy space
        val (ct, nonceB64) = if (input.isNotBlank()) {
            val key = Crypto.deriveRoomKey(me, peerId)
            Crypto.encryptXChaCha(input, key)
        } else {
            " " to "none"
        }

        scope.launch {
            try {
                val sent = if (attachmentUri != null) {
                    backend.sendMessageWithAttachment(rid, ct, nonceB64, attachmentUri!!)
                } else {
                    backend.sendMessage(rid, ct, nonceB64)
                }
                input = ""
                attachmentUri = null
                messages = messages + sent
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Send failed"
            }
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

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { m ->
                val isMe = (m.sender == meId)

                // Decrypt for display if needed
                val displayText = remember(m.id, m.ciphertext, m.nonce, m.algo) {
                    val algo = m.algo ?: "plaintext"
                    val hasNonce = !m.nonce.isNullOrBlank() && m.nonce != "none"
                    if (algo == "xchacha20poly1305" && hasNonce && meId != null) {
                        runCatching {
                            val key = Crypto.deriveRoomKey(meId, peerId)
                            Crypto.decryptXChaCha(m.ciphertext, m.nonce!!, key)
                        }.getOrElse { "[decryption failed]" }
                    } else {
                        m.ciphertext
                    }
                }

                MessageBubble(
                    text = displayText,
                    time = prettyTime(m.created),
                    isMe = isMe
                )

                // 🔹 Show basic attachment indicator if present
                if (!m.attachment.isNullOrBlank()) {
    val context = LocalContext.current
    Text(
        text = "📎 Attachment: ${m.attachment}",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .padding(top = 2.dp)
            .clickable {
                // Build PocketBase file URL
                val base = PbConfig.BASE.trimEnd('/')
                val token = backend.token

                val url = buildString {
                    append(base)
                    append("/api/files/messages/")
                    append(m.id)
                    append("/")
                    append(m.attachment)
                    if (!token.isNullOrBlank()) {
                        append("?token=")
                        append(token)
                    }
                }

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
    )
}


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
                keyboardActions = KeyboardActions(
                    onSend = { sendCurrentMessage() }
                )
            )

            // 📎 Attachment button
            IconButton(onClick = { pickAttachmentLauncher.launch("*/*") }) {
                Text("Attach")
            }
            IconButton(onClick = {
                if (!isRecording) {
                    // start recording (permission will trigger recorder)
                    audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    // stop and send
                    val rec = recorder
                    val file = recordingFile
                    if (rec != null && file != null) {
                        try { rec.stop() } catch (_: Exception) {}
                        rec.release()
                        recorder = null
                        isRecording = false

                        val rid = roomId ?: return@IconButton

                        scope.launch {
                            try {
                                val uri = Uri.fromFile(file)
                                val (ct, nonce) = " " to "none"  // empty text
                                val sent = backend.sendMessageWithAttachment(rid, ct, nonce, uri)
                                messages = messages + sent
                                recordingFile = null
                            } catch (e: Exception) {
                                error = e.localizedMessage ?: "Failed to send voice message"
                            }
                        }
                    }
                }
            }) {
                Text(if (isRecording) "Stop" else "Voice")
            }

            val canSend = roomId != null && (input.isNotBlank() || attachmentUri != null)

            Button(
                enabled = canSend,
                onClick = { sendCurrentMessage() }
            ) {
                Text("Send")
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

// Very forgiving timestamp: try ISO-8601, else fall back to HH:mm slice
private fun prettyTime(created: String): String {
    return try {
        val t = OffsetDateTime.parse(created)
        "%02d:%02d".format(t.hour, t.minute)
    } catch (_: DateTimeParseException) {
        if (created.length >= 16) created.substring(11, 16) else created
    }
}

