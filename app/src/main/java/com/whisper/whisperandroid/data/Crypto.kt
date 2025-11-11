package com.whisper.whisperandroid.data

import android.util.Base64
import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.AEAD
import java.security.MessageDigest
import java.security.SecureRandom

object Crypto {
    private val sodium = LazySodiumAndroid(SodiumAndroid())
    private val rng = SecureRandom()

    fun random24(): ByteArray = ByteArray(24).also { rng.nextBytes(it) }

    // Demo-only: deterministic per-room key from user ids
    fun deriveRoomKey(meId: String, peerId: String): ByteArray {
        val ids = if (meId < peerId) "$meId:$peerId" else "$peerId:$meId"
        val salt = "whisper-v0-roomkey"
        return MessageDigest.getInstance("SHA-256")
            .digest((ids + "|" + salt).toByteArray(Charsets.UTF_8)) // 32 bytes
    }

    fun encryptXChaCha(plain: String, key: ByteArray): Pair<String, String> {
        val nonce = random24()
        val m = plain.toByteArray(Charsets.UTF_8)

        val c = ByteArray(m.size + AEAD.XCHACHA20POLY1305_IETF_ABYTES)
        val cLen = longArrayOf(0L)

        val ok = sodium.cryptoAeadXChaCha20Poly1305IetfEncrypt(
            c, cLen,
            m, m.size.toLong(),
            null, 0L,             // associated data
            null,                 // nsec (unused)
            nonce, key
        )
        if (!ok) error("Encrypt failed")

        val ctB64 = Base64.encodeToString(c, Base64.NO_WRAP)
        val nonceB64 = Base64.encodeToString(nonce, Base64.NO_WRAP)
        return ctB64 to nonceB64
    }

    fun decryptXChaCha(ctB64: String, nonceB64: String, key: ByteArray): String {
        val ct = Base64.decode(ctB64, Base64.NO_WRAP)
        val nonce = Base64.decode(nonceB64, Base64.NO_WRAP)

        val out = ByteArray(ct.size - AEAD.XCHACHA20POLY1305_IETF_ABYTES)
        val outLen = longArrayOf(0L)

        val ok = sodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
            out, outLen,
            null,                 // nsec (unused)
            ct, ct.size.toLong(),
            null, 0L,             // associated data
            nonce, key
        )
        if (!ok) error("Decrypt failed")

        return out.toString(Charsets.UTF_8)
    }
}


