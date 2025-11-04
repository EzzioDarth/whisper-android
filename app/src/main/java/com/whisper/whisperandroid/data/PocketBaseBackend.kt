package com.whisper.whisperandroid.data

import android.content.Context

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory



class PocketBaseBackend(
    private val appCtx: Context,
    baseUrl: String
) : ChatBackend {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.ensureEndsWithSlash())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(OkHttpClient())
        .build()

    private val api = retrofit.create(PbServices::class.java)

    @Volatile private var _token: String? = null
    @Volatile private var _me: PbUser? = null

    override val token: String? get() = _token
    override  val currentUser: PbUser? get() = _me

    override suspend fun login(email: String, password: String): UserSession {
        // PocketBase expects "identity" + "password"
        val body = api.auth(PbAuthReq(identity = email, password = password))
        _token = body.token
        _me = body.record
        return UserSession(userId = body.record.id, token = body.token)
    }


    override suspend fun ensureKeypairAndUploadPubKey() {
        // no-op for now; just compile. We’ll add keygen/upload next step.
    }
    override fun signOut() {
        _token = null
        _me = null
    }
    override suspend fun listContacts(query: String?, page: Int,
                                      perPage: Int): List<PbUser> {
        val t = token ?: throw IllegalStateException("Not authenticated")
        val meId = currentUser?.id
        val baseFilter = if (meId != null) """id != "$meId"""" else null
        val searchFilter = query?.takeIf { it.isNotBlank() }?.let { q ->
            """(displayName ~ "$q" || username ~ "$q" || email ~ "$q")"""
        }
        val finalFilter = listOfNotNull(baseFilter,
            searchFilter).joinToString(" && ").ifBlank {null}
        val resp = api.listUsers(
            "Bearer $t",
            page,
            perPage,
            finalFilter
        )
        return resp.items

    }
    override suspend fun findUserByEmail(email: String): PbUser? {
        val t = token ?: throw IllegalStateException("Not authenticated")
        val resp = api.listUsers(
            "Bearer $t",
            1,
            1,
            """email="$email""""
        )
        return resp.items.firstOrNull()
    }

    private fun roomPairKey(a: String, b: String): String {
        return if (a < b) "${a}_${b}" else "${b}_${a}"
    }

    override suspend fun openOrCreateDirectRoom(peerId: String): PbRoom {
        val t = token ?: error("Not authenticated")
        val meId = currentUser?.id ?: error("No current user")
        val key = roomPairKey(meId, peerId)

        // Try to find existing room
        val found = api.listRooms(
            "Bearer $t",
            """pairKey="$key""""
        ).items.firstOrNull()
        if (found != null) return found

        // Create new room
        return api.createRoom(
            "Bearer $t",
            mapOf(
                "pairKey" to key,
                "type" to "direct",
                "aId" to meId,
                "bId" to peerId
            )
        )
    }

    override suspend fun listMessages(roomId: String): List<PbMessage> {
        val t = token ?: throw IllegalStateException("Not authenticated")
        return api.listMessages(
            "Bearer $t",
            """roomId="$roomId"""",
            "created"
        ).items
    }

    override suspend fun sendMessage(roomId: String, ciphertext: String, nonce: String?): PbMessage {
        val t = token ?: throw IllegalStateException("Not authenticated")
        val meId = currentUser?.id ?: throw IllegalStateException("No current user")
        return api.sendMessage(
            bearer = "Bearer $t",
            body = mapOf(
                "roomId" to roomId,
                "senderId" to meId,
                "ciphertext" to ciphertext,
                "nonce" to nonce
            )
        )
    }

}

private fun String.ensureEndsWithSlash() = if (endsWith("/")) this else this + "/"
