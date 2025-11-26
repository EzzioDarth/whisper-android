package com.whisper.whisperandroid.data
import com.squareup.moshi.Json

data class PbAuthReq(
    val identity: String, // PocketBase expects "identity"
    val password: String,
)

data class PbUser(
    val id: String,
    val email: String? = null,
    val username: String? = null,
    val displayName: String? = null,
    val pubKey: String? = null
)

data class PbAuthResp(
    val token: String,
    val record: PbUser
)
data class PbListResp<T>(
    val page: Int,
    val perPage: Int,
    val totalItems: Int,
    val items: List<T>
)
data class PbRoom(
    val id: String,
    val pairKey: String,
    val type: String? = null,
    val aId: String? = null,
    val bId: String? = null,
    val members: List<String> = emptyList()
)
data class PbRoomListResp(
    val items: List<PbRoom>
)
data class PbMessage(
    val id: String,
    @Json(name = "room") val room: String,
    @Json(name = "sender") val sender : String,
    val ciphertext: String,
    val nonce: String?,
    val created: String,
    val algo: String?,
    @Json(name = "attachment") val attachment: String? = null
)
data class PbRecordId(val id: String)
