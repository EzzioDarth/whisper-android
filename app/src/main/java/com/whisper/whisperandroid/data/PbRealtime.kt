package com.whisper.whisperandroid.data


import okhttp3.*
import okio.ByteString
import org.json.JSONObject

class PbRealtime(
    baseUrl: String,
    private val token: String
) {
    private val client = OkHttpClient()
    private val wsUrl = baseUrl.replaceFirst("http", "ws") + "/api/realtime"
    private var socket: WebSocket? = null

    fun connect(onEvent: (JSONObject) -> Unit, onClosed: () -> Unit, onFailure: (Throwable) -> Unit) {
        val req = Request.Builder().url(wsUrl).build()
        socket = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // auth
                val authMsg = JSONObject().apply {
                    put("type", "auth")
                    put("token", token)
                }
                webSocket.send(authMsg.toString())
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                onEvent(JSONObject(text))
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onEvent(JSONObject(bytes.utf8()))
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onClosed()
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onFailure(t)
            }
        })
    }

    fun subscribeMessages(roomId: String) {
        val sub = JSONObject().apply {
            put("id", "sub-messages-$roomId")
            put("type", "subscribe")
            put("topic", "messages")
            put("filter", "room=\"$roomId\"")
        }
        socket?.send(sub.toString())
    }

    fun close() { socket?.close(1000, "bye") }
}
