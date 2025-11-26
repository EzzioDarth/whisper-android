package com.whisper.whisperandroid.data

import com.google.api.Page
import retrofit2.http.Query
import android.net.Uri



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
    suspend fun register(email: String, username: String, password: String): PbUser

    suspend fun listContacts(query: String? = null, page: Int = 1, perPage: Int = 50): List<PbUser>
    suspend fun findUserByEmail(email: String): PbUser?
    suspend fun openOrCreateDirectRoom(peerId: String): PbRoom
    suspend fun listMessages(roomId: String): List<PbMessage>
    suspend fun sendMessage(roomId: String, ciphertext: String, nonce: String? = null): PbMessage
    suspend fun eraseAllMyMessages()
    suspend fun sendMessageWithAttachment(
        roomId: String,
        ciphertext: String,
        nonce: String?,
        attachmentUri: Uri
    ): PbMessage
    suspend fun countMyMessagesBetween(
        fromIso: String? = null,
        toIso: String? = null
    ): Int
    suspend fun countMyMessagesInRoom(
        roomId: String,
        fromIso: String? = null,
        toIso: String? = null,
        onlyWithAttachment: Boolean = false
    ): Int
    suspend fun listMyRooms(): List<PbRoom>

suspend fun createGroupRoom(
    name: String,
    memberIds: List<String>
): PbRoom
	
    
}
