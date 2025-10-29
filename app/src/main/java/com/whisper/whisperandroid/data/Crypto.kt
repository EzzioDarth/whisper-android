package com.whisper.whisperandroid.data

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.AEAD
import com.goterl.lazysodium.interfaces.Box
import java.security.SecureRandom
import android.util.Base64

object Crypto {
    private val sodium = LazySodiumAndroid(SodiumAndroid())
    private val rng = SecureRandom()

    fun generateKeypair(): Pair<ByteArray, ByteArray> {
        val pk = ByteArray(Box.PUBLICKEYBYTES)
        val sk = ByteArray(Box.SECRETKEYBYTES)
        sodium.cryptoBoxKeypair(pk, sk)
        return pk to sk
    }

    fun random32(): ByteArray = ByteArray(32).also { rng.nextBytes(it) }
    fun random24(): ByteArray = ByteArray(24).also { rng.nextBytes(it) }

    fun seal(roomKey: ByteArray, recipientPk: ByteArray): ByteArray {
        val sealed = ByteArray(roomKey.size + Box.SEALBYTES)
        //                 ⬇️ convert Int length to Long
        sodium.cryptoBoxSeal(sealed, roomKey, roomKey.size.toLong(), recipientPk)
        return sealed
    }

    fun openSeal(sealed: ByteArray, myPk: ByteArray, mySk: ByteArray): ByteArray {
        val opened = ByteArray(sealed.size - Box.SEALBYTES)
        //                                   ⬇️ convert Int length to Long
        check(sodium.cryptoBoxSealOpen(opened, sealed, sealed.size.toLong(), myPk, mySk)) {
            "open seal failed"
        }
        return opened
    }

    fun encrypt(roomKey: ByteArray, plaintext: ByteArray, nonce24: ByteArray): ByteArray {
        val out = ByteArray(plaintext.size + AEAD.XCHACHA20POLY1305_IETF_ABYTES)
        //                                                  ⬇️ length as Long   ⬇️ adLen as Long
        sodium.cryptoAeadXChaCha20Poly1305IetfEncrypt(
            out, null, plaintext, plaintext.size.toLong(), null, 0L, null, nonce24, roomKey
        )
        return out
    }

    fun decrypt(roomKey: ByteArray, ciphertext: ByteArray, nonce24: ByteArray): ByteArray {
        val out = ByteArray(ciphertext.size - AEAD.XCHACHA20POLY1305_IETF_ABYTES)
        //                                                ⬇️ length as Long   ⬇️ adLen as Long
        check(
            sodium.cryptoAeadXChaCha20Poly1305IetfDecrypt(
                out, null, null, ciphertext, ciphertext.size.toLong(), null, 0L, nonce24, roomKey
            )
        ) { "decrypt failed" }
        return out
    }

    fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)
}
