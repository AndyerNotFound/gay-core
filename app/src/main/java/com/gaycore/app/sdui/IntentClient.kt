package com.gaycore.app.sdui

import com.gaycore.app.data.Api
import com.gaycore.app.data.ServerEntry
import com.gaycore.app.data.ServerStore
import com.google.gson.JsonObject
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec



class IntentClient(private val store: ServerStore, private val server: ServerEntry) {

    
    private fun seqFromBody(raw: String): Long? = try {
        val o = com.google.gson.JsonParser.parseString(raw).asJsonObject
        o.get("currentSeq")?.takeIf { !it.isJsonNull }?.asLong
    } catch (_: Exception) { null }

    class IntentResult(val json: JsonObject, val ok: Boolean) {
        val toast: String? get() = json.get("toast")?.takeIf { !it.isJsonNull }?.asString
        val error: String? get() = json.get("error")?.takeIf { !it.isJsonNull }?.asString
        val ui: JsonObject? get() = json.get("ui")?.takeIf { it.isJsonObject }?.asJsonObject
        
        val back: Boolean get() = json.get("back")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
    }

    private fun randomHex(bytes: Int): String {
        val b = ByteArray(bytes); java.security.SecureRandom().nextBytes(b)
        return b.joinToString("") { "%02x".format(it) }
    }

    



    fun intent(pluginId: String, endpoint: String, body: JsonObject?): IntentResult {
        val token = server.token() ?: throw Api.ApiException(401, "未登录")
        val path = if (endpoint.startsWith("./")) {
            "${server.fullBasePath()}/plugins/$pluginId${endpoint.removePrefix(".")}"
        } else if (endpoint.startsWith("/")) {
            "${server.fullBasePath()}$endpoint"
        } else {
            "${server.fullBasePath()}/plugins/$pluginId/$endpoint"
        }
        return doSigned(token, pluginId, path, body, retryOnSeq = true)
    }

    private fun doSigned(token: String, pluginId: String, path: String, body: JsonObject?, retryOnSeq: Boolean): IntentResult {
        val bodyStr = body?.toString() ?: ""
        val seq = store.nextSeqByToken(token, pluginId)
        val ts = System.currentTimeMillis()
        val nonce = randomHex(16)
        val intentId = UUID.randomUUID().toString()
        val sign = IntentSigner.sign(token, ts, nonce, seq, intentId, "POST", path, bodyStr)
        val url = server.baseUrl + path
        val headers = mapOf(
            "Authorization" to "Bearer $token",
            "X-GC-Timestamp" to ts.toString(),
            "X-GC-Nonce" to nonce,
            "X-GC-Seq" to seq.toString(),
            "X-GC-Intent-Id" to intentId,
            "X-GC-Sign" to sign,
        )
        val json = try {
            Api.post(url, com.google.gson.JsonParser.parseString(bodyStr.ifEmpty { "{}" }).asJsonObject, headers)
        } catch (e: Api.ApiException) {
            

            if (retryOnSeq) {
                val cur = seqFromBody(e.rawBody)
                if (cur != null) {
                    store.correctSeqByToken(token, pluginId, cur)
                    return doSigned(token, pluginId, path, body, retryOnSeq = false)
                }
            }
            throw e
        }
        val ok = json.get("ok")?.asBoolean ?: false
        
        if (!ok && retryOnSeq && json.has("currentSeq")) {
            store.correctSeqByToken(token, pluginId, json.get("currentSeq").asLong)
            return doSigned(token, pluginId, path, body, retryOnSeq = false)
        }
        return IntentResult(json, ok)
    }
}


fun ServerEntry.fullBasePath(): String =
    if (branch == "default" || branch.isEmpty()) "" else "/$branch"
