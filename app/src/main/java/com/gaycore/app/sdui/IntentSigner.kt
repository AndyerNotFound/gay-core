package com.gaycore.app.sdui

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

                                                                                                    
                                               
object IntentSigner {
    fun sha256Hex(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    fun hmacSha256(key: String, msg: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(msg.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun sign(token: String, ts: Long, nonce: String, seq: Long, intentId: String, method: String, path: String, bodyStr: String): String {
        val bodyHash = sha256Hex(bodyStr)
        return hmacSha256(token, "$ts\n$nonce\n$seq\n$intentId\n${method.uppercase()}\n$path\n$bodyHash")
    }
}
