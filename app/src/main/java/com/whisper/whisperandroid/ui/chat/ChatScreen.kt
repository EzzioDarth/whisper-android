package com.whisper.whisperandroid.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val createdAtMs: Long = 0L
)

@Composable
fun ChatScreen(
    onBackToAuth: () -> Unit = {}
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val uid = auth.currentUser?.uid ?: "anon"

    // Firestore listener lifecycle
    DisposableEffect(Unit) {
        val registration: ListenerRegistration = db.collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                messages = snap.documents.map { d ->
                    ChatMessage(
                        id = d.id,
                        text = d.getString("text") ?: "",
                        senderId = d.getString("senderId") ?: "",
                        createdAtMs = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                }
            }


        onDispose {
            registration.remove()
        }
    }

    fun send() {
        val t = input.trim()
        if (t.isEmpty()) return
        sending = true
        db.collection("messages").add(
            mapOf(
                "text" to t,
                "senderId" to uid,
                "createdAt" to Timestamp.now()
            )
        ).addOnCompleteListener {
            sending = false
            if (it.isSuccessful) input = ""
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Whisper — Chat", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = false
        ) {
            items(messages, key = { it.id }) { m ->
                val mine = m.senderId == uid
                Surface(
                    tonalElevation = if (mine) 2.dp else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = (if (mine) "You: " else "Them: ") + m.text,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Message") },
                enabled = !sending
            )
            Button(onClick = { send() }, enabled = input.isNotBlank() && !sending) {
                Text(if (sending) "..." else "Send")
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = {
            auth.signOut()
            onBackToAuth()
        }) { Text("Sign out") }
    }
}
