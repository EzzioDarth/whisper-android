package com.whisper.whisperandroid.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whisper.whisperandroid.core.PbConfig
import com.whisper.whisperandroid.core.ServiceLocator
import com.whisper.whisperandroid.data.PbRoom
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ChatScreen(
    onBackToAuth: () -> Unit = {},
    onStartNewChat: () -> Unit = {},
    onShowStats: () -> Unit = {},
    onOpenGroup: (String) -> Unit,
    onStartNewGroup: () -> Unit
) {
    val backend = ServiceLocator.backend
    val scope = rememberCoroutineScope()

    var eraseLoading by remember { mutableStateOf(false) }
    var eraseError by remember { mutableStateOf<String?>(null) }
    var eraseResult by remember { mutableStateOf<String?>(null) }

    var rooms by remember { mutableStateOf<List<PbRoom>>(emptyList()) }
    var roomsLoading by remember { mutableStateOf(true) }
    var roomsError by remember { mutableStateOf<String?>(null) }

    // If token is null, bounce back to auth
    LaunchedEffect(Unit) {
        if (backend.token == null) {
            onBackToAuth()
        }
    }

    // Load rooms where I'm a member (direct + group)
    LaunchedEffect(Unit) {
        try {
            roomsLoading = true
            roomsError = null
            rooms = backend.listMyRooms()
        } catch (e: Exception) {
            roomsError = e.localizedMessage ?: "Failed to load rooms"
        } finally {
            roomsLoading = false
        }
    }

    val user = backend.currentUser
    val who = when {
        user?.displayName?.isNotBlank() == true -> user.displayName
        user?.username?.isNotBlank() == true -> user.username
        user?.email?.isNotBlank() == true -> user.email
        else -> user?.id ?: "Unknown user"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "✅ Logged in successfully!",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Hello, $who",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))

            // Row with "New chat" and "New group"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onStartNewChat
                ) {
                    Text("Start new chat")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onStartNewGroup
                ) {
                    Text("New group")
                }
            }

            Button(onClick = onShowStats) {
                Text("View my stats")
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "PocketBase connection active.\nBackend: ${PbConfig.BASE}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            // Erase all my messages
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
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            eraseResult?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(24.dp))

            // 🔹 Group chats section
            Text(
                "Group chats",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            when {
                roomsLoading -> {
                    CircularProgressIndicator()
                }
                roomsError != null -> {
                    Text(
                        text = roomsError!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    val groupRooms = rooms.filter { it.type == "group" }

                    if (groupRooms.isEmpty()) {
                        Text(
                            "No groups yet. Tap “New group” to create one.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            groupRooms.forEach { room ->
                                val label = "Group ${room.id.take(6)}"

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenGroup(room.id) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${room.members.size} participants",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Sign out
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

