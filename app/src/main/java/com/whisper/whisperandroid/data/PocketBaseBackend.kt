package com.whisper.whisperandroid.data

package data.pb

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import data.crypto.Crypto
import data.crypto.SecureStore
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.coroutines.resumeWithException
import android.content.Context
import com.whisper.whisperandroid.core.SecureStore
import com.whisper.whisperandroid.data.Crypto

class PocketBaseBackend(
    ctx: Context,
    baseUrl: String
) : ChatBackend {

    private val secure = SecureStore(ctx)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(OkHttpClient())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
    private val api = retrofit.create(`PbService.kt`::class.java)

    private var token: String? = null
    private var me: PbUser? = null
    private var realtime: PbRealtime? = null

    override suspend fun login(email: String, pass: String): UserSession {
        val auth = call { api.auth(PbAuthReq(email, pass)) }
        token = auth.token
        me = auth.record
        return UserSession(auth.record.id, auth.token)
    }

    override suspend fun ensureKeypairAndUploadPubKey() {
        val hasSk = secure.getBytes("sk") != null
        if (!hasSk) {
            val (pk, sk) = Crypto.generateKeypair()
            secure.putBytes("pk", pk); secure.putBytes("sk", sk)
            // upload pubKey + displayName untouched if already set
            val body = mapOf("pubKey" to Crypto.b64(pk))
            call { api.updateUser(me!!.id, body) }
        } else if (me?.pubKey.isNullOrBlank()) {
            val pk = secure.getBytes("pk")!!
            call { api.updateUser(me!!.id, mapOf("pubKey" to Crypto.b64(pk))) }
        }
    }

    override suspend fun createRoom(participantIds: List<String>): String {
        val bearer = "Bearer ${token!!}"
        val roomId = call { api.createRoom(bearer, PbCreateRoomReq(type = if (participantIds.size==2) "direct" else "group", createdBy = me!!.id)) }.id

        // generate room key and distribute
        val roomKey = Crypto.random32()
        secure.putBytes("rk:$roomId", roomKey)

        // fetch needed public keys (assume passed-in list already includes me)
        participantIds.forEach { uid ->
            val pk = if (uid == me!!.id) secure.getBytes("pk")!! else fetchUserPubKey(uid)
            val sealed = Crypto.seal(roomKey, pk)
            val enc = mapOf("algo" to "sealbox", "ciphertext" to Crypto.b64(sealed))
            call { api.addParticipant(bearer, PbParticipantReq(roomId, uid, enc)) }
        }
        return roomId
    }

    override suspend fun joinRoom(roomId: String) {
        // get my encRoomKey via list messages? better: fetch participants where room=roomId & user=me
        // Use a one-off REST call (Retrofit not defined here), so simplest: rely on UI to have inserted earlier.
        // In practice you'd add an endpoint call; here we assume you have the encRoomKey already fetched in your VM.
        // Placeholder: no-op (or throw) — wire with your participant fetch function.
    }

    override suspend fun sendMessage(roomId: String, plaintext: String) {
        val bearer = "Bearer ${token!!}"
        val rk = secure.getBytes("rk:$roomId") ?: error("no room key cached")
        val nonce = Crypto.random24()
        val cipher = Crypto.encrypt(rk, plaintext.toByteArray(Charsets.UTF_8), nonce)
        call { api.sendMessage(bearer, PbMessageReq(roomId, me!!.id, Crypto.b64(cipher), Crypto.b64(nonce))) }
    }

    override fun subscribeMessages(roomId: String, onMessage: (ChatMessage) -> Unit): java.io.Closeable {
        val rt = PbRealtime(baseUrl = (retrofBase()), token = token!!)
        realtime = rt
        rt.connect(
            onEvent = { evt ->
                // events: {"event":"*","record":{...}}
                val rec = evt.optJSONObject("record") ?: return@connect
                val id = rec.optString("id")
                val sender = rec.optString("sender")
                val nonceB64 = rec.optString("nonce")
                val ctB64 = rec.optString("ciphertext")
                val room = rec.optString("room")
                val rk = secure.getBytes("rk:$room") ?: return@connect
                try {
                    val plain = Crypto.decrypt(rk, Crypto.unb64(ctB64), Crypto.unb64(nonceB64))
                    onMessage(ChatMessage(id, room, sender, String(plain, Charsets.UTF_8)))
                } catch (_: Throwable) { /* ignore corrupt */ }
            },
            onClosed = { },
            onFailure = { _ -> }
        )
        rt.subscribeMessages(roomId)
        return java.io.Closeable { rt.close() }
    }

    override suspend fun saveFcmToken(token: String) {
        val bearer = "Bearer ${this.token!!}"
        call { api.addParticipant(bearer, /* wrong endpoint placeholder; create a device_tokens service similarly */ throw NotImplementedError() }
    }

    // Helpers
    private suspend fun <T> call(block: () -> retrofit2.Call<T>): T =
        suspendCancellableCoroutine { cont ->
            val c = block()
            c.enqueue(object : retrofit2.Callback<T> {
                override fun onResponse(call: retrofit2.Call<T>, response: retrofit2.Response<T>) {
                    if (response.isSuccessful && response.body()!=null) cont.resume(response.body()!!)
                    else cont.resumeWithException(RuntimeException("HTTP ${response.code()} ${response.errorBody()?.string()}"))
                }
                override fun onFailure(call: retrofit2.Call<T>, t: Throwable) { cont.resumeWithException(t) }
            })
            cont.invokeOnCancellation { c.cancel() }
        }

    private fun retrofBase(): String = (retrofit.baseUrl().toString().removeSuffix("/"))
    private suspend fun fetchUserPubKey(userId: String): ByteArray {
        // Minimal fetch via PocketBase GET /api/collections/users/records/{id}
        // For brevity, do it with OkHttp here:
        val url = "${retrofBase()}/api/collections/users/records/$userId"
        val req = okhttp3.Request.Builder().url(url).header("Authorization","Bearer ${token!!}").build()
        val resp = OkHttpClient().newCall(req).execute()
        if (!resp.isSuccessful) throw RuntimeException("fetch user failed ${resp.code}")
        val body = resp.body?.string() ?: ""
        val pub = Regex("\"pubKey\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: error("no pubKey")
        return data.crypto.Crypto.unb64(pub)
    }
}

/** Your app-level abstractions **/
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
