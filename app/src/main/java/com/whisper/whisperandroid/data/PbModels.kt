package com.whisper.whisperandroid.data


data class PbAuthReq(val identity: String, val password: String)
data class PbAuthResp(val token: String, val record: PbUser)
data class PbUser(val id: String, val email: String?, val username: String?, val displayName: String?, val pubKey: String?)
data class PbCreateRoomReq(val type: String, val createdBy: String)
data class PbRecordId(val id: String)

data class PbParticipantReq(
    val room: String,
    val user: String,
    val encRoomKey: Map<String, String> // {"algo":"sealbox","ciphertext":"...","nonce":"..."} (nonce optional for sealbox)
)

data class PbMessageReq(
    val room: String,
    val sender: String,
    val ciphertext: String,
    val nonce: String,
    val algo: String = "xchacha20poly1305"
)

data class PbListResp<T>(val page: Int, val perPage: Int, val totalItems: Int, val items: List<T>)
data class PbMessage(
    val id: String,
    val room: String,
    val sender: String,
    val ciphertext: String,
    val nonce: String,
    val algo: String,
    val created: String
)
