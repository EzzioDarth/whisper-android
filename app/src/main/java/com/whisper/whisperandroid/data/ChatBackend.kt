package com.whisper.whisperandroid.data

import com.google.api.Page
import retrofit2.http.Query

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
    val token: String?
    val currentUser: PbUser?
    suspend fun login(email: String, password: String): UserSession
    suspend fun ensureKeypairAndUploadPubKey()
    fun signOut()
    // keep the rest minimal for now so we compile
    suspend fun listContacts(query: String? = null, page: Int = 1, perPage: Int = 50): List<PbUser>
}
