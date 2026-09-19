package com.gaycore.app.data

import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext














object Vault {

    
    suspend fun locked(s: ServerEntry): Boolean? = withContext(Dispatchers.IO) {
        try {
            val j = Api.get("${s.adminBase}/plugins/vault/status")
            val v = j.get("locked")
            if (v != null && !v.isJsonNull) v.asBoolean else null
        } catch (_: Exception) {
            null
        }
    }

    



    suspend fun ensureUnlocked(s: ServerEntry): String? = withContext(Dispatchers.IO) {
        val isLocked = locked(s) ?: return@withContext null
        if (!isLocked) return@withContext null
        val pass = s.vaultPass()
        if (pass.isNullOrBlank()) return@withContext "网关已锁定：请在「站点信息 → 网关口令」填写口令"
        val ak = s.adminKey()
        if (ak.isNullOrBlank()) return@withContext "网关已锁定：缺少管理密钥，无法自动解锁"
        try {
            val r = Api.adminPost(s, "/plugins/vault/admin/unlock", JsonObject().apply {
                addProperty("pass", pass)
                addProperty("adminKey", ak)
            })
            val ok = r.get("ok")?.takeIf { !it.isJsonNull }?.asBoolean == true
            if (ok) null else (r.get("error")?.takeIf { !it.isJsonNull }?.asString ?: "解锁失败")
        } catch (e: Exception) {
            "自动解锁失败: " + (e.message ?: "")
        }
    }

    
    suspend fun unlockWith(s: ServerEntry, pass: String): String? = withContext(Dispatchers.IO) {
        val ak = s.adminKey() ?: return@withContext "缺少管理密钥，无法解锁"
        try {
            val r = Api.adminPost(s, "/plugins/vault/admin/unlock", JsonObject().apply {
                addProperty("pass", pass)
                addProperty("adminKey", ak)
            })
            val ok = r.get("ok")?.takeIf { !it.isJsonNull }?.asBoolean == true
            if (ok) null else (r.get("error")?.takeIf { !it.isJsonNull }?.asString ?: "解锁失败")
        } catch (e: Exception) {
            "解锁失败: " + (e.message ?: "")
        }
    }
}
