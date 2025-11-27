package com.whisper.whisperandroid.ui.recovery

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whisper.whisperandroid.core.ServiceLocator
import com.whisper.whisperandroid.data.generateRecoveryWords
import com.whisper.whisperandroid.data.hashRecoveryPhrase
import com.whisper.whisperandroid.data.normalizeRecoveryWords
import kotlinx.coroutines.launch

@Composable
fun RecoveryPhraseScreen(
    onDone: () -> Unit
) {
    val backend = ServiceLocator.backend
    val scope = rememberCoroutineScope()

    // Generate the 5 words once when screen is first shown
    val words = remember { generateRecoveryWords(5) }

    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Your Recovery Phrase",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                "Write down these 5 words in order and keep them safe.\n" +
                        "If you forget your password, you will need them to reset it.\n" +
                        "If you lose them, we cannot recover your account.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                words.joinToString("   "),
                style = MaterialTheme.typography.titleMedium
            )

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                enabled = !saving,
                onClick = {
                    saving = true
                    error = null

                    scope.launch {
                        try {
                            val phrase = normalizeRecoveryWords(words)
                            val hash = hashRecoveryPhrase(phrase)
                            backend.updateMyRecoveryHash(hash)
                            onDone()
                        } catch (e: Exception) {
                            error = e.localizedMessage ?: "Failed to save recovery phrase."
                        } finally {
                            saving = false
                        }
                    }
                }
            ) {
                Text(if (saving) "Saving…" else "I have saved these words")
            }
        }
    }
}

