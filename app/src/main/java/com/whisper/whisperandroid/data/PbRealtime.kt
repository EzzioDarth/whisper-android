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

            // We only care about the "messages" collection
            val collection = obj.optString("collection")
            if (collection != "messages") return

            val rec = obj.optJSONObject("record") ?: return

            // In your schema the field is literally called "room"
            val roomId = rec.optString("room")
            if (roomId.isNullOrEmpty()) return

            messageListeners[roomId]?.invoke(rec)
        } catch (e: Exception) {
            Log.w("PB-RT", "non-json ws or parse error: $text", e)
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
     */
    fun subscribeRoomMessages(roomId: String, onEvent: (JSONObject) -> Unit) {
        messageListeners[roomId] = onEvent

        val t = tokenProvider() ?: ""
        val sub = JSONObject().apply {
            put("id", "sub-$roomId")
            put("type", "subscribe")
            put("collection", "messages")

            // IMPORTANT: in your schema the field is "room", not "roomId"
            put("filter", """room="$roomId"""")

            if (t.isNotBlank()) put("token", t)
        }
        ws?.send(sub.toString())
    }

    fun unsubscribeRoom(roomId: String) {
        messageListeners.remove(roomId)
        val unsub = JSONObject().apply {
            put("id", "unsub-$roomId")
            put("type", "unsubscribe")
            put("collection", "messages")
            put("filter", """room="$roomId"""")
        }
        ws?.send(unsub.toString())
    }
}

