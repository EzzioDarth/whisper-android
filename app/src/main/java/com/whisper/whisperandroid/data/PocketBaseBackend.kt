package com.whisper.whisperandroid.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream





class PocketBaseBackend(
    private val appCtx: Context,
    baseUrl: String
) : ChatBackend {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val logger: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}
    private val client: OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(logger) // type is Interceptor, so no ambiguity now
    .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.ensureEndsWithSlash())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()

    private val api = retrofit.create(PbServices::class.java)

    @Volatile private var _token: String? = null
    @Volatile private var _me: PbUser? = null

    override val token: String? get() = _token
    override  val currentUser: PbUser? get() = _me
    override suspend fun sendMessageWithAttachment(
    roomId: String,
    ciphertext: String,
    nonce: String?,
    attachmentUri: Uri
): PbMessage {
    val t = token ?: error("Not authenticated")
    val meId = currentUser?.id ?: error("No current user")

    val algo = if (!nonce.isNullOrBlank() && nonce != "none") {
        "xchacha20poly1305"
    } else {
        "plaintext"
    }

    val resolver = appCtx.contentResolver
    val tempFile = File.createTempFile("msg_att_", null, appCtx.cacheDir)

    var srcFile: File? = null

    when (attachmentUri.scheme) {
        "content" -> {
            resolver.openInputStream(attachmentUri).use { input ->
                FileOutputStream(tempFile).use { output ->
                    if (input != null) input.copyTo(output)
                }
            }
        }
        "file" -> {
            srcFile = File(attachmentUri.path ?: error("File URI has no path"))
            srcFile.copyTo(tempFile, overwrite = true)
        }
        else -> {
            // fallback: try using content resolver anyway
            resolver.openInputStream(attachmentUri).use { input ->
                FileOutputStream(tempFile).use { output ->
                    if (input != null) input.copyTo(output)
                }
            }
        }
    }

    val originalName =
        if (attachmentUri.scheme == "content") {
            resolver.query(attachmentUri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } else {
            srcFile?.name
        } ?: tempFile.name

    val mimeType = resolver.getType(attachmentUri) ?: "application/octet-stream"
    val fileBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
    val filePart = MultipartBody.Part.createFormData("attachment", originalName, fileBody)

    fun textPart(value: String): RequestBody =
        value.toRequestBody("text/plain".toMediaTypeOrNull())

    return api.sendMessageMultipart(
        bearer = "Bearer $t",
        room = textPart(roomId),
        sender = textPart(meId),
        ciphertext = textPart(ciphertext),
        nonce = textPart(nonce ?: "none"),
        algo = textPart(algo),
        attachment = filePart
    )
}




    override suspend fun login(email: String, password: String): UserSession {
        // PocketBase expects "identity" + "password"
        val body = api.auth(PbAuthReq(identity = email, password = password))
        _token = body.token
        _me = body.record
        return UserSession(userId = body.record.id, token = body.token)
    }
        override suspend fun register(email: String, username: String, password: String): PbUser {
        // PocketBase expects password + passwordConfirm
        val body = mapOf(
            "email"            to email,
            "username"         to username,
            "password"         to password,
            "passwordConfirm"  to password
        )

        val user = api.registerUser(body)

        // Optional but nice: automatically log the user in after registration
        login(email, password)

        return user
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
    val pairKey = if (meId < peerId) "${meId}_${peerId}" else "${peerId}_${meId}"

    val existing = api.listRooms(
        bearer = "Bearer $t",
        filter = """pairKey="$pairKey""""
    ).items.firstOrNull()
    if (existing != null) return existing

    val body = mapOf(
        "pairKey" to pairKey,
        "type"    to "direct",
        "createdBy" to meId,
        "aId"     to meId,
        "bId"     to peerId
    )
    return api.createRoom("Bearer $t", body)
}




    override suspend fun listMessages(roomId: String): List<PbMessage> {
    val t = token ?: throw IllegalStateException("Not authenticated")
    val meId = currentUser?.id ?: throw IllegalStateException("No current user")

    // Filter by relation equals the room id
    val resp = api.listMessages(
        bearer = "Bearer $t",
        filter = """room="$roomId"""",
        sort   = "created"
    )
    return resp.items
}


    override suspend fun sendMessage(roomId: String, ciphertext: String, nonce: String?): PbMessage {
    val t = token ?: error("Not authenticated")
    val meId = currentUser?.id ?: error("No current user")

    val algo = if (!nonce.isNullOrBlank() && nonce != "none") "xchacha20poly1305" else "plaintext"

    val body = mapOf(
        "room"       to roomId,          // matches PB schema
        "sender"     to meId,            // matches PB schema
        "ciphertext" to ciphertext,
        "nonce"      to (nonce ?: "none"),
        "algo"       to algo
    )
    return api.sendMessage("Bearer $t", body)
}
    override suspend fun eraseAllMyMessages() {
        val t = token ?: error("Not authenticated")
        val meId = currentUser?.id ?: error("No current user")

        var pageNum = 1
        val perPage = 200

        while (true) {
            val resp = api.listMessages(
                bearer = "Bearer $t",
                filter = """sender="$meId"""",
                sort = "created",
                page = pageNum,
                perPage = perPage
            )

            val items = resp.items
            if (items.isEmpty()) break

            for (m in items) {
                api.deleteMessage("Bearer $t", m.id)
            }

            // If we got less than a full page, we're done
            if (items.size < perPage) break

            pageNum++
        }
    }




}

private fun String.ensureEndsWithSlash(): String = if (endsWith("/")) this else this + "/"
