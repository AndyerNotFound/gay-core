package com.gaycore.app.data

import com.gaycore.app.sdui.SduiCapabilities
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit


object Api {
    val gson = Gson()
    val client = OkHttpClient.Builder()
        .proxy(java.net.Proxy.NO_PROXY) 
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    
    class ApiException(val status: Int, message: String, val rawBody: String = "") : Exception(message)

    private fun branchPrefix(branch: String) = if (branch == "default" || branch.isEmpty()) "" else "/$branch"

    
    fun get(url: String, headers: Map<String, String> = emptyMap()): JsonObject {
        val rb = Request.Builder().url(url).get()
        for ((k, v) in headers) rb.header(k, v)
        return exec(rb.build())
    }

    fun post(url: String, body: JsonObject?, headers: Map<String, String> = emptyMap()): JsonObject {
        val rb = Request.Builder().url(url)
            .post((body?.toString() ?: "{}").toRequestBody("application/json".toMediaType()))
        for ((k, v) in headers) rb.header(k, v)
        return exec(rb.build())
    }

    
    fun delete(url: String, body: JsonObject? = null, headers: Map<String, String> = emptyMap()): JsonObject {
        val rb = Request.Builder().url(url)
            .delete((body?.toString() ?: "{}").toRequestBody("application/json".toMediaType()))
        for ((k, v) in headers) rb.header(k, v)
        return exec(rb.build())
    }

    fun adminDelete(s: ServerEntry, path: String, body: JsonObject? = null) =
        delete("${s.adminBase}$path", body, adminHeaders(s))

    private fun exec(req: Request): JsonObject {
        client.newCall(req).execute().use { res ->
            val text = res.body?.string() ?: ""
            if (!res.isSuccessful) {
                


                val msg = try {
                    val e = JsonParser.parseString(text).asJsonObject.get("error")
                    when {
                        e == null || e.isJsonNull -> null
                        e.isJsonPrimitive -> e.asString
                        e.isJsonObject -> e.asJsonObject.get("message")?.takeIf { !it.isJsonNull }?.asString
                        else -> e.toString()
                    }
                } catch (_: Exception) { null }
                throw ApiException(res.code, msg ?: ("HTTP " + res.code), text)
            }
            return try { JsonParser.parseString(text).asJsonObject }
            catch (_: Exception) { throw ApiException(res.code, "响应不是 JSON") }
        }
    }

    
    fun bootstrap(baseUrl: String, branch: String = "default") =
        gson.fromJson(get("$baseUrl${branchPrefix(branch)}/api/app/bootstrap"), Bootstrap::class.java)

    fun fetchUi(s: ServerEntry, pluginId: String, uiPath: String): JsonObject {
        val path = if (uiPath.startsWith("/")) uiPath else "/$uiPath"
        

        if (pluginId.isEmpty()) return get("${s.fullBase}$path", authHeaders(s) + SduiCapabilities.headers())
        
        return get("${s.fullBase}/plugins/$pluginId$path", authHeaders(s) + SduiCapabilities.headers())
    }

    






    fun fetchAdminUi(s: ServerEntry, pluginId: String, uiPath: String): JsonObject {
        val path = if (uiPath.startsWith("/")) uiPath else "/$uiPath"
        val url = if (path.startsWith("/admin/")) "${s.adminBase}$path"
        else "${s.adminBase}/plugins/$pluginId$path"
        return get(url, adminHeaders(s) + SduiCapabilities.headers())
    }

    



    fun adminCall(s: ServerEntry, method: String, path: String, body: JsonObject?): JsonObject {
        val p = if (path.startsWith("/")) path else "/$path"
        val url = "${s.adminBase}$p"
        return when (method.uppercase()) {
            "GET" -> get(url, adminHeaders(s))
            "DELETE" -> delete(url, body, adminHeaders(s))
            else -> post(url, body, adminHeaders(s))
        }
    }

    fun authHeaders(s: ServerEntry): Map<String, String> {
        val t = s.token()
        return if (t != null) mapOf("Authorization" to "Bearer $t") else emptyMap()
    }

    fun adminHeaders(s: ServerEntry): Map<String, String> {
        val t = s.adminKey()
        return if (t != null) mapOf("x-admin-key" to t) else emptyMap()
    }

    







    fun adminUserHeaders(s: ServerEntry): Map<String, String> {
        val h = adminHeaders(s).toMutableMap()
        s.adminKey()?.let { h["Authorization"] = "Bearer $it" }
        return h
    }

    
    fun adminGet(s: ServerEntry, path: String) = get("${s.adminBase}$path", adminHeaders(s))
    fun adminPost(s: ServerEntry, path: String, body: JsonObject?) = post("${s.adminBase}$path", body, adminHeaders(s))

    
    fun login(baseUrl: String, branch: String, username: String, password: String) =
        post("$baseUrl${branchPrefix(branch)}/auth/login", JsonObject().apply {
            addProperty("username", username); addProperty("password", password)
        })

    
    fun register(baseUrl: String, branch: String, username: String, password: String): JsonObject =
        post("$baseUrl${branchPrefix(branch)}/auth/register", JsonObject().apply {
            addProperty("username", username); addProperty("password", password)
        })

    
    fun registerInfo(baseUrl: String, branch: String): JsonObject =
        get("$baseUrl${branchPrefix(branch)}/auth/register")


    
    fun adminUsers(s: ServerEntry): List<AdminUser> =
        gson.fromJson(adminGet(s, "/plugins/auth-user/admin/users").getAsJsonArray("users"), Array<AdminUser>::class.java).toList()

    fun adminKeys(s: ServerEntry): List<AdminKey> =
        gson.fromJson(adminGet(s, "/plugins/auth-cardkey/admin/keys").getAsJsonArray("keys"), Array<AdminKey>::class.java).toList()

    
    fun adminUserAction(s: ServerEntry, body: JsonObject) = adminPost(s, "/plugins/auth-user/admin/users", body)

    
    fun adminCreateKey(s: ServerEntry, body: JsonObject) = adminPost(s, "/plugins/auth-cardkey/admin/keys", body)

    
    fun adminUpdateKey(s: ServerEntry, body: JsonObject) = adminPost(s, "/plugins/auth-cardkey/admin/keys-update", body)

    
    fun adminDeleteKey(s: ServerEntry, key: String): JsonObject {
        val rb = Request.Builder().url(s.fullBase + "/plugins/auth-cardkey/admin/keys")
            .delete(JsonObject().apply { addProperty("key", key) }.toString().toRequestBody("application/json".toMediaType()))
        for ((k, v) in adminHeaders(s)) rb.header(k, v)
        return exec(rb.build())
    }
    
    
    fun base(s: ServerEntry): String = s.baseUrl + gcPrefix(s.adminTarget)
}





private fun gcPrefix(branch: String) = if (branch == "default" || branch.isEmpty()) "" else "/$branch"



val ServerEntry.adminTarget: String
    get() {
        val ai = adminInstance as String?
        if (!ai.isNullOrEmpty()) return ai
        val b = branch as String?
        return if (b.isNullOrEmpty()) "default" else b
    }


val ServerEntry.adminBase: String get() = baseUrl + gcPrefix(adminTarget)



val ServerEntry.fullBase: String get() = baseUrl + gcPrefix(branch)
