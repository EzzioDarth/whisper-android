package com.whisper.whisperandroid.data

data class UserSession(
    val userId: String,
    val token: String
)

data class ChatMessage(
    val id: String,
    val roomId: String,
    val senderId: String,
    val ciphertext: String,
    val nonce: String,
    val created: String
)

interface ChatBackend {
    suspend fun login(email: String, password: String): UserSession
    suspend fun ensureKeypairAndUploadPubKey()
    // keep the rest minimal for now so we compile
}
