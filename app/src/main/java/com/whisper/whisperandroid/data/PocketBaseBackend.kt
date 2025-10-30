package com.whisper.whisperandroid.data

import android.content.Context
import com.google.api.Page
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Query
import retrofit2.Response.error

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
        val t = token ?: error("Not authenticated")
        val meId = currentUser?.id
        val baseFilter = if (meId != null) """id != "$meId"""" else null
        val searchFilter = query?.takeIf { it.isNotBlank() }?.let { q ->
            """(displayName ~ "$q" || username ~ "$q" || email ~ "$q")"""
        }
        val finalFilter = listOfNotNull(baseFilter,
            searchFilter).joinToString(" && ").ifBlank {null}
        val resp = api.listUsers(
            bearer = "Bearer $t",
            page = page,
            perPage =perPage,
            filter = finalFilter
        )
        return resp.items

    }
}

private fun String.ensureEndsWithSlash() = if (endsWith("/")) this else this + "/"
