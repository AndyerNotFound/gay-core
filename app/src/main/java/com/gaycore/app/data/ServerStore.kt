package com.gaycore.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.security.MessageDigest

                  
data class ServerEntry(
    val id: String,                                           
    val baseUrl: String,                   
    val mode: String,                             
    var name: String = "",
    var branch: String = "default",                                         
    var adminInstance: String? = null,                                                
                                                                                               
    var tokenEnc: String? = null,               
    var adminKeyEnc: String? = null,            
    var uid: String = "",                                
    var bootstrapJson: String? = null,                
    val addedAt: Long = System.currentTimeMillis(),
) {
    fun token(): String? = tokenEnc?.let { KeyStoreCrypto.decrypt(it) }
    fun adminKey(): String? = adminKeyEnc?.let { KeyStoreCrypto.decrypt(it) }
    fun bootstrap(): Bootstrap? = try {
        bootstrapJson?.let { Gson().fromJson(it, Bootstrap::class.java) }
    } catch (_: Exception) { null }
}

                                      
class ServerStore(ctx: Context) {
    private val app = ctx.applicationContext
    private val prefs: SharedPreferences = app.getSharedPreferences("gaycore_servers", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        fun siteId(baseUrl: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(baseUrl.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
        }
        fun normalizeUrl(raw: String): String {
            var u = raw.trim().removeSuffix("/")
            if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
            return u
        }
    }

    fun list(): List<ServerEntry> {
        val raw = prefs.getString("servers", null) ?: return emptyList()
        return try {
            val t = object : com.google.gson.reflect.TypeToken<List<ServerEntry>>() {}.type
            val l: List<ServerEntry> = gson.fromJson(raw, t) ?: emptyList()
            l.forEach { fixup(it) }
            l
        } catch (_: Exception) { emptyList() }
    }

                                                                           
    @Suppress("SENSELESS_COMPARISON")
    private fun fixup(e: ServerEntry) {
        if (e.branch == null) e.branch = "default"
        if (e.adminInstance == null) e.adminInstance = ""
        if (e.name == null) e.name = ""
        if (e.uid == null) e.uid = ""
    }


    fun listByMode(mode: String) = list().filter {
        if (mode == "admin") it.adminKeyEnc != null else it.tokenEnc != null
    }

    fun get(id: String) = list().firstOrNull { it.id == id }

    fun save(e: ServerEntry) {
        val l = list().filter { it.id != e.id }.toMutableList()
        l.add(e)
        prefs.edit().putString("servers", gson.toJson(l)).apply()
    }

    fun remove(id: String) {
        prefs.edit().putString("servers", gson.toJson(list().filter { it.id != id })).apply()
                                      
        siteDir(id).deleteRecursively()
        app.getSharedPreferences("gc_site_$id", Context.MODE_PRIVATE).edit().clear().apply()
    }

                                        
    fun siteDir(id: String): File = File(app.filesDir, "sites/$id").apply { mkdirs() }

                                                
    fun sitePrefs(id: String): SharedPreferences =
        app.getSharedPreferences("gc_site_$id", Context.MODE_PRIVATE)

                                              
    fun localLayout(id: String): LayoutConfig? {
        val s = sitePrefs(id).getString("layout_override", null) ?: return null
        return try { gson.fromJson(s, LayoutConfig::class.java) } catch (_: Exception) { null }
    }
    fun saveLocalLayout(id: String, l: LayoutConfig?) {
        sitePrefs(id).edit().apply {
            if (l == null) remove("layout_override") else putString("layout_override", gson.toJson(l))
        }.apply()
    }

                        
    fun themeId(id: String): String? = sitePrefs(id).getString("theme_id", null)
    fun saveThemeId(id: String, themeId: String) { sitePrefs(id).edit().putString("theme_id", themeId).apply() }

                                           
    fun darkMode(id: String): String = sitePrefs(id).getString("dark_mode", "system") ?: "system"
    fun saveDarkMode(id: String, mode: String) { sitePrefs(id).edit().putString("dark_mode", mode).apply() }
                                                    
    fun dynamicColors(id: String): Boolean = sitePrefs(id).getBoolean("dynamic_colors", false)
    fun saveDynamicColors(id: String, on: Boolean) { sitePrefs(id).edit().putBoolean("dynamic_colors", on).apply() }

                                                                                 
    fun widgetOrder(id: String): List<String>? {
        val s = sitePrefs(id).getString("widget_order", null) ?: return null
        return try { gson.fromJson(s, Array<String>::class.java).toList() } catch (_: Exception) { null }
    }
    fun widgetHidden(id: String): List<String> {
        val s = sitePrefs(id).getString("widget_hidden", null) ?: return emptyList()
        return try { gson.fromJson(s, Array<String>::class.java).toList() } catch (_: Exception) { emptyList() }
    }
    fun saveWidgetLayout(id: String, order: List<String>?, hidden: List<String>) {
        sitePrefs(id).edit().apply {
            if (order != null) putString("widget_order", gson.toJson(order)) else remove("widget_order")
            putString("widget_hidden", gson.toJson(hidden))
        }.apply()
    }

                                     
    fun nextSeq(id: String, scope: String): Long {
        val p = sitePrefs(id)
        val k = "seq_$scope"
        val v = p.getLong(k, 0) + 1
        p.edit().putLong(k, v).apply()
        return v
    }
    fun correctSeq(id: String, scope: String, current: Long) {
        sitePrefs(id).edit().putLong("seq_$scope", current).apply()
    }

                        
    var currentUserServer: String?
        get() = prefs.getString("current_user", null)
        set(v) = prefs.edit().putString("current_user", v).apply()
    var currentAdminServer: String?
        get() = prefs.getString("current_admin", null)
        set(v) = prefs.edit().putString("current_admin", v).apply()

                          
    fun updateBootstrap(id: String, b: Bootstrap) {
        val e = get(id) ?: return
        e.bootstrapJson = gson.toJson(b)
        if (e.name.isEmpty()) e.name = b.serverInfo?.name?.takeIf { it.isNotBlank() } ?: b.branch
        save(e)
    }
}
