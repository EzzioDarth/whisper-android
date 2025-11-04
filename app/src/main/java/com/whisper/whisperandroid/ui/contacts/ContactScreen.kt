package com.whisper.whisperandroid.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whisper.whisperandroid.core.ServiceLocator
import com.whisper.whisperandroid.data.PbUser
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import  androidx.compose.ui.Alignment

@Composable
fun ContactsScreen(
    onBack: () -> Unit = {},
    onSelectContact: (PbUser) -> Unit = {}
) {
    val backend = ServiceLocator.backend
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var users by remember { mutableStateOf<List<PbUser>>(emptyList()) }

    fun load() {
        loading = true
        error = null
        scope.launch {
            try {
                users = backend.listContacts(query = query.ifBlank { null })
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Failed to load contacts"
            } finally {
                loading = false
            }
        }
    }

    // Initial load
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {              
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("Contacts", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                label = { Text("Search contacts") },
                singleLine = true
            )
            Button(onClick = { load() }, enabled = !loading) {
                Text(if (loading) "..." else "Search")
            }
        }

        Spacer(Modifier.height(12.dp))

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(users) { u ->
                ContactRow(user = u, onClick = { onSelectContact(u) })
            }
        }
    }
}

@Composable
private fun ContactRow(user: PbUser, onClick: () -> Unit) {
    val title = when {
        !user.displayName.isNullOrBlank() -> user.displayName
        !user.username.isNullOrBlank() -> user.username
        !user.email.isNullOrBlank() -> user.email
        else -> user.id
    }
    Card(
        Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (!user.email.isNullOrBlank()) {
                Text(user.email, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
