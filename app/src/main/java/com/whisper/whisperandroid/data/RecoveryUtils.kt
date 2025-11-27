package com.whisper.whisperandroid.data

import java.security.MessageDigest

// Simple demo list — you can expand this
private val WORDS = listOf(
    "whisper", "crypto", "shadow", "ember", "silver",
    "forest", "signal", "mirror", "desert", "storm",
    "oasis", "galaxy", "fusion", "cipher", "vector",
    "tunnel", "quantum", "aurora", "echo", "phoenix"
)

fun generateRecoveryWords(count: Int = 5): List<String> {
    return (1..count).map {
        WORDS.random()
    }
}

fun normalizeRecoveryWords(words: List<String>): String =
    words.joinToString(" ") { it.trim().lowercase() }

fun hashRecoveryPhrase(phrase: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(phrase.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

