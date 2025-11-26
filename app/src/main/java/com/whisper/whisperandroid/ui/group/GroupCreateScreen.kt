@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.whisper.whisperandroid.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whisper.whisperandroid.core.ServiceLocator
import com.whisper.whisperandroid.data.PbUser
import kotlinx.coroutines.launch

@Composable
fun GroupCreateScreen(
    onBack: () -> Unit,
    onGroupCreated: (roomId: String) -> Unit
) {
    val backend = ServiceLocator.backend
    val scope = rememberCoroutineScope()

    var groupName by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<PbUser>>(emptyList()) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load contacts once
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            error = null
            contacts = backend.listContacts()
        } catch (e: Exception) {
            error = e.localizedMessage ?: "Failed to load contacts"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New group chat") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Group name") },
                singleLine = true
            )

            Text(
                "Select participants",
                style = MaterialTheme.typography.labelMedium
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(contacts) { user ->
                        val id = user.id
                        val isSelected = selectedIds.contains(id)
                        val label = user.username ?: user.email ?: user.id

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) {
                                        selectedIds + id
                                    } else {
                                        selectedIds - id
                                    }
                                }
                            )
                            Text(label)
                        }
                    }
                }
            }

            val canCreate = groupName.isNotBlank() && selectedIds.size >= 1 && !isLoading

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canCreate,
                onClick = {
                    scope.launch {
                        try {
                            error = null
                            val room = backend.createGroupRoom(
                                name = groupName,
                                memberIds = selectedIds.toList()
                            )
                            onGroupCreated(room.id)
                        } catch (e: Exception) {
                            error = e.localizedMessage ?: "Failed to create group"
                        }
                    }
                }
            ) {
                Text("Create group")
            }
        }
    }
}

