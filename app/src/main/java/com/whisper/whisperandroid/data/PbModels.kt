package com.whisper.whisperandroid.data

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

data class PbRecordId(val id: String)
