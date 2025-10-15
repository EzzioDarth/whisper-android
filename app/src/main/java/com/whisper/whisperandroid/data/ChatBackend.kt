package com.whisper.whisperandroid.data



interface ChatBackend {
    suspend fun login(email: String, pass: String): UserSession
    suspend fun ensureKeypairAndUploadPubKey()
    suspend fun createRoom(participantIds: List<String>): String
    suspend fun joinRoom(roomId: String)
    suspend fun sendMessage(roomId: String, plaintext: String)
    fun subscribeMessages(roomId: String, onMessage: (ChatMessage) -> Unit): java.io.Closeable
    suspend fun saveFcmToken(token: String)
}

data class UserSession(val userId: String, val jwt: String)
data class ChatMessage(val id: String, val roomId: String, val senderId: String, val text: String)
