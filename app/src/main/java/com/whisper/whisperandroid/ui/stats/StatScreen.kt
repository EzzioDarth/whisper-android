package com.whisper.whisperandroid.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whisper.whisperandroid.core.ServiceLocator
import com.whisper.whisperandroid.data.PbUser
import java.time.OffsetDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit
) {
    val backend = ServiceLocator.backend

    // Global stats (optional)
    var allTime by remember { mutableStateOf<Int?>(null) }

    // Contacts for user dropdown
    var contacts by remember { mutableStateOf<List<PbUser>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<PbUser?>(null) }

    // Time range selector
    val timeOptions = listOf("Today", "This week", "This month", "This year")
    var selectedTime by remember { mutableStateOf(timeOptions[1]) } // default: This week

    // Per-user stats
    var textCount by remember { mutableStateOf<Int?>(null) }
    var mediaCount by remember { mutableStateOf<Int?>(null) }

    var error by remember { mutableStateOf<String?>(null) }

    // Load contacts + global stats once
    LaunchedEffect(Unit) {
        try {
            val all = backend.countMyMessagesBetween(fromIso = null, toIso = null)
            allTime = all

            val list = backend.listContacts()
            contacts = list
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Failed to load initial stats"
        }
    }

    // Helper: compute from/to based on selectedTime
    fun currentRangeIso(): Pair<String?, String?> {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        return when (selectedTime) {
            "Today" -> {
                val start = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
                start.toString() to now.toString()
            }
            "This week" -> {
                val start = now.minusDays(7)
                start.toString() to now.toString()
            }
            "This month" -> {
                val start = now.minusMonths(1)
                start.toString() to now.toString()
            }
            "This year" -> {
                val start = now.minusYears(1)
                start.toString() to now.toString()
            }
            else -> null to null
        }
    }

    // Recalculate per-user stats whenever user or time range changes
    LaunchedEffect(selectedUser, selectedTime) {
        val user = selectedUser ?: return@LaunchedEffect
        try {
            error = null
            textCount = null
            mediaCount = null

            val (fromIso, toIso) = currentRangeIso()

            // Get (or create) the direct room with this user
            val room = backend.openOrCreateDirectRoom(user.id)

            // total messages I sent in this room
            val total = backend.countMyMessagesInRoom(
                roomId = room.id,
                fromIso = fromIso,
                toIso = toIso,
                onlyWithAttachment = false
            )

            // messages with attachments in this room
            val media = backend.countMyMessagesInRoom(
                roomId = room.id,
                fromIso = fromIso,
                toIso = toIso,
                onlyWithAttachment = true
            )

            mediaCount = media
            textCount = (total - media).coerceAtLeast(0)
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Failed to load per-user stats"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            allTime?.let {
                Text(
                    "Messages sent (all time): $it",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            // Time range selector
            Text("Time range", style = MaterialTheme.typography.labelMedium)
            TimeRangeDropdownSimple(
                options = timeOptions,
                selected = selectedTime,
                onSelectedChange = { selectedTime = it }
            )

            Spacer(Modifier.height(8.dp))

            // User selector
            Text("User", style = MaterialTheme.typography.labelMedium)
            UserDropdownSimple(
                contacts = contacts,
                selectedUser = selectedUser,
                onUserSelected = { selectedUser = it }
            )

            Spacer(Modifier.height(16.dp))

            if (selectedUser == null) {
                Text(
                    "Select a user to see per-user stats.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (textCount == null || mediaCount == null) {
                CircularProgressIndicator()
            } else {
                val t = textCount ?: 0
                val m = mediaCount ?: 0
                val max = (maxOf(t, m, 1)).toFloat()

                // 🔹 limit bar height, add space for labels
                val barMaxHeight = 120.dp
                val chartTotalHeight = barMaxHeight + 40.dp

                Text(
                    "Messages you sent to ${selectedUser?.username ?: selectedUser?.email ?: "user"}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "Time range: $selectedTime",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(16.dp))

                // Simple 2-column bar chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartTotalHeight),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Text messages column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height((t / max * barMaxHeight.value).dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Text")
                        Text("$t", style = MaterialTheme.typography.bodySmall)
                    }

                    // Media messages column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height((m / max * barMaxHeight.value).dp)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Media")
                        Text("$m", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Media = messages with attachments (images, files, audio).\nText = messages without attachments.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TimeRangeDropdownSimple(
    options: List<String>,
    selected: String,
    onSelectedChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selected)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelectedChange(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun UserDropdownSimple(
    contacts: List<PbUser>,
    selectedUser: PbUser?,
    onUserSelected: (PbUser) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selectedUser?.username ?: selectedUser?.email ?: "Select user"

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(label)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            contacts.forEach { user ->
                val name = user.username ?: user.email ?: user.id
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onUserSelected(user)
                        expanded = false
                    }
                )
            }
        }
    }
}

