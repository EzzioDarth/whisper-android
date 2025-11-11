package com.whisper.whisperandroid.data

import android.util.Log
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

class PbRealtime(
    private val baseUrl: String,
    private val tokenProvider: () -> String?
) : WebSocketListener() {

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var ws: WebSocket? = null
    private var isOpen = AtomicBoolean(false)

    // simple listeners per roomId
    private val messageListeners = mutableMapOf<String, (JSONObject) -> Unit>()

    fun connect() {
        if (isOpen.get()) return
        val url = baseUrl.trimEnd('/') + "/api/realtime"
        val req = Request.Builder()
            .url(url)
            .build()
        ws = client.newWebSocket(req, this)
    }

    fun close() {
        isOpen.set(false)
        ws?.close(1000, "bye")
        ws = null
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        isOpen.set(true)
        val t = tokenProvider() ?: return
        // auth (PocketBase accepts Authorization header on subscribe too,
        // but many builds expect an "auth" message first—send both to be safe)
        val auth = JSONObject().apply {
            put("id", "auth1")
            put("type", "auth")
            put("token", t)
        }
        webSocket.send(auth.toString())
        Log.d("PB-RT", "ws open + auth sent")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val obj = JSONObject(text)
            // Expect messages like: { "event":"create"|"update"|..., "record":{...}, "collection":"messages" , ... }
            val collection = obj.optString("collection")
            if (collection == "messages") {
                val rec = obj.optJSONObject("record") ?: return
                val roomId = rec.optString("roomId")
                messageListeners[roomId]?.invoke(rec)
            }
        } catch (_: Exception) {
            Log.w("PB-RT", "non-json ws: $text")
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        onMessage(webSocket, bytes.utf8())
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        isOpen.set(false)
        Log.d("PB-RT", "ws closed: $code $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        isOpen.set(false)
        Log.e("PB-RT", "ws fail", t)
    }

    /**
     * Subscribe to messages of a specific room.
     * Many PB versions accept topic = "messages" with a filter.
     */
    fun subscribeRoomMessages(roomId: String, onEvent: (JSONObject) -> Unit) {
        messageListeners[roomId] = onEvent
        val t = tokenProvider() ?: ""
        val sub = JSONObject().apply {
            put("id", "sub-${roomId}")
            put("type", "subscribe")
            put("collection", "messages")
            put("filter", """roomId="$roomId"""")
            if (t.isNotBlank()) put("token", t) // some builds accept token inline
        }
        ws?.send(sub.toString())
    }

    fun unsubscribeRoom(roomId: String) {
        messageListeners.remove(roomId)
        val unsub = JSONObject().apply {
            put("id", "unsub-${roomId}")
            put("type", "unsubscribe")
            put("collection", "messages")
            put("filter", """roomId="$roomId"""")
        }
        ws?.send(unsub.toString())
    }
}

