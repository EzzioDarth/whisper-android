package com.whisper.whisperandroid.core


import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64

class SecureStore(ctx: Context) {
    private val master = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        ctx, "secure_store", master, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    fun putBytes(key: String, bytes: ByteArray) = prefs.edit().putString(key, Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
    fun getBytes(key: String): ByteArray? = prefs.getString(key, null)?.let { Base64.decode(it, Base64.DEFAULT) }
}
