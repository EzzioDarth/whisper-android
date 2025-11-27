package com.whisper.whisperandroid.ui.recovery

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.whisper.whisperandroid.data.hashRecoveryPhrase
import com.whisper.whisperandroid.data.normalizeRecoveryWords
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val httpClient = remember { OkHttpClient() }

    var email by remember { mutableStateOf("") }
    var wordsInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (loading) return
        error = null
        info = null

        val words = wordsInput.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size != 5) {
            error = "Please enter exactly 5 words (separated by spaces)."
            return
        }
        if (newPassword.length < 6) {
            error = "New password must be at least 6 characters."
            return
        }
        if (newPassword != confirmPassword) {
            error = "Passwords do not match."
            return
        }

        loading = true

        scope.launch {
            try {
                // 1) Normalize and hash the phrase exactly like when we stored it
                val phrase = normalizeRecoveryWords(words)
                val phraseHash = hashRecoveryPhrase(phrase)

                // 2) Build JSON body (same as your curl)
                val json = """
                    {
                      "email": "${email.trim()}",
                      "phraseHash": "$phraseHash",
                      "newPassword": "$newPassword"
                    }
                """.trimIndent()

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("http://100.84.142.65:3001/recover")
                    .post(body)
                    .build()

                // 3) Call backend on IO thread
                val resp = withContext(Dispatchers.IO) {
                    httpClient.newCall(request).execute()
                }

                val respBody = resp.body?.string()

                if (!resp.isSuccessful) {
                    error = "Recovery failed: ${resp.code} ${respBody ?: ""}"
                    loading = false
                    return@launch
                }

                // Optional: you can parse respBody and check for {"ok":true},
                // but for now any 2xx from your backend means success.
                info = "Password updated. Please log in with your new password."
                loading = false

                // Small delay or direct navigation – your choice.
                // For now, go back to login immediately.
                onBackToLogin()

            } catch (e: Exception) {
                loading = false
                error = e.localizedMessage ?: "Recovery failed."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Forgot Password",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                "Enter your email, your 5-word recovery phrase, and choose a new password.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = wordsInput,
                onValueChange = { wordsInput = it },
                label = { Text("Recovery words (5, space-separated)") },
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm new password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            if (info != null) {
                Text(info!!, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBackToLogin,
                    enabled = !loading
                ) {
                    Text("Back to login")
                }

                Button(
                    onClick = { submit() },
                    enabled = !loading && email.isNotBlank()
                ) {
                    Text(if (loading) "Resetting..." else "Reset password")
                }
            }
        }
    }
}

