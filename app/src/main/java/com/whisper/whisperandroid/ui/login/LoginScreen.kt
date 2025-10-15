package com.whisper.whisperandroid.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.whisper.whisperandroid.core.ServiceLocator

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun signIn() {
        if (loading) return
        error = null
        loading = true

        scope.launch {
            try {
                val backend = ServiceLocator.backend
                val session = backend.login(email.trim(), password)
                backend.ensureKeypairAndUploadPubKey()
                loading = false
                onLoginSuccess()
            } catch (e: Exception) {
                loading = false
                error = e.localizedMessage ?: "Login failed"
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Whisper — Login", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                enabled = email.isNotBlank() && password.length >= 6 && !loading,
                onClick = { signIn() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (loading) "Signing in..." else "Sign In") }

            TextButton(onClick = onGoToRegister, enabled = !loading) {
                Text("No account? Create one")
            }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        }

        if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
    }
}
