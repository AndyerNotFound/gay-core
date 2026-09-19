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
    


    var vaultPassEnc: String? = null,
    var uid: String = "",            
    var bootstrapJson: String? = null, 
    val addedAt: Long = System.currentTimeMillis(),
) {
    fun token(): String? = tokenEnc?.let { KeyStoreCrypto.decrypt(it) }
    fun adminKey(): String? = adminKeyEnc?.let { KeyStoreCrypto.decrypt(it) }
    
    fun vaultPass(): String? = vaultPassEnc?.let { KeyStoreCrypto.decrypt(it) }
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
        

        fun entryId(baseUrl: String, mode: String, token: String? = null): String {
            val site = siteId(baseUrl)
            if (mode == "admin") return site + ":admin"
            val th = if (token != null) sha8(token) else ""
            return site + ":user" + if (th.isEmpty()) "" else ":" + th
        }
        fun sha8(raw: String): String =
            MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }.take(8)
    }

    fun list(): List<ServerEntry> {
        val raw = prefs.getString("servers", null) ?: return emptyList()
        return try {
            val t = object : com.google.gson.reflect.TypeToken<List<ServerEntry>>() {}.type
            val l: MutableList<ServerEntry> = (gson.fromJson(raw, t) ?: emptyList<ServerEntry>()).toMutableList()
            l.forEach { fixup(it) }
            

            var migrated = false
            val remap = HashMap<String, String>()
            for (i in l.indices) {
                val e = l[i]
                if (!e.id.contains(":")) {
                    val newId = entryId(e.baseUrl, e.mode, e.token())
                    migrateSitePrefs(e.id, newId)
                    remap[e.id] = newId
                    l[i] = e.copy(id = newId)
                    migrated = true
                }
            }
            val split = splitSharedEntries(l)
            if (migrated || split) {
                
                val cu = prefs.getString("current_user", null)
                val ca = prefs.getString("current_admin", null)
                val ed = prefs.edit()
                if (cu != null && remap.containsKey(cu)) ed.putString("current_user", remap[cu])
                if (ca != null && remap.containsKey(ca)) ed.putString("current_admin", remap[ca])
                if (ca != null && splitRemap.containsKey(ca)) ed.putString("current_admin", splitRemap[ca])
                ed.putString("servers", gson.toJson(l)).apply()
            }
            l
        } catch (_: Exception) { emptyList() }
    }

    
    private val splitRemap = HashMap<String, String>()

    





    private fun splitSharedEntries(l: MutableList<ServerEntry>): Boolean {
        var changed = false
        var i = 0
        while (i < l.size) {
            val e = l[i]
            if (e.adminKeyEnc == null || e.id.endsWith(":admin")) { i++; continue }
            val adminId = entryId(e.baseUrl, "admin", null)
            if (l.any { it.id == adminId }) {
                
                l[i] = e.copy(adminKeyEnc = null)
                splitRemap[e.id] = adminId
                changed = true
                i++
                continue
            }
            
            l[i] = e.copy(adminKeyEnc = null)
            l.add(e.copy(id = adminId, mode = "admin", tokenEnc = null, uid = ""))
            splitRemap[e.id] = adminId
            changed = true
            i++
        }
        return changed
    }

    
    private fun migrateSitePrefs(oldId: String, newId: String) {
        try {
            val src = app.getSharedPreferences("gc_site_$oldId", Context.MODE_PRIVATE)
            val all = src.all
            if (all.isEmpty()) return
            val dst = app.getSharedPreferences("gc_site_$newId", Context.MODE_PRIVATE)
            val ed = dst.edit()
            for ((k, v) in all) {
                when (v) {
                    is String -> ed.putString(k, v)
                    is Boolean -> ed.putBoolean(k, v)
                    is Int -> ed.putInt(k, v)
                    is Long -> ed.putLong(k, v)
                    is Float -> ed.putFloat(k, v)
                }
            }
            ed.apply()
        } catch (_: Exception) { }
    }

    
    @Suppress("SENSELESS_COMPARISON")
    private fun fixup(e: ServerEntry) {
        if (e.branch == null) e.branch = "default"
        if (e.adminInstance == null) e.adminInstance = ""
        if (e.name == null) e.name = ""
        if (e.uid == null) e.uid = ""
    }


    
    fun listByMode(mode: String) = list().filter {
        if (mode == "admin") it.id.endsWith(":admin")
        else it.id.contains(":user") || (!it.id.endsWith(":admin") && it.tokenEnc != null)
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
    fun customSeed(id: String): Int? { val v = sitePrefs(id).getInt("custom_seed", 0); return if (v == 0) null else v }
    fun saveCustomSeed(id: String, seed: Int) { sitePrefs(id).edit().putInt("custom_seed", seed).apply() }
    fun clearCustomSeed(id: String) { sitePrefs(id).edit().remove("custom_seed").apply() }

    
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

    






    fun homeLayoutV2(id: String): Pair<List<String>?, List<String>>? {
        val s = sitePrefs(id).getString("home_layout_v2", null)
        if (s != null) {
            return try {
                val j = gson.fromJson(s, JsonObject::class.java)
                val order = j.get("order")?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asString }
                val hidden = j.get("hidden")?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asString } ?: emptyList()
                Pair(order, hidden)
            } catch (_: Exception) { null }
        }
        val oh = widgetHidden(id)
        val oo = widgetOrder(id)
        if (oh.isEmpty() && oo == null) return null
        return Pair(oo?.map { if (it.startsWith("widget:")) it else "widget:" + it },
            oh.map { if (it.startsWith("widget:")) it else "widget:" + it })
    }
    fun saveHomeLayout(id: String, order: List<String>?, hidden: List<String>) {
        val j = JsonObject()
        j.add("order", gson.toJsonTree(order ?: emptyList<String>()))
        j.add("hidden", gson.toJsonTree(hidden))
        sitePrefs(id).edit().putString("home_layout_v2", j.toString()).apply()
    }

    





    class HomeLayout(
        val items: List<HomeSlot>,
        val hidden: List<String>,
        val decos: Map<String, DecoCard>,
    )

    
    private fun legacySpanToCols(span: Int?): Int = when {
        span == null -> 5
        span <= 6 -> 2
        else -> 5
    }

    fun homeLayoutV3(id: String): HomeLayout? {
        val s = sitePrefs(id).getString("home_layout_v3", null)
        if (s != null) {
            return try {
                val j = gson.fromJson(s, JsonObject::class.java)
                val arr = j.get("items")?.takeIf { it.isJsonArray }?.asJsonArray
                val items = ArrayList<HomeSlot>()
                if (arr != null) {
                    for (e in arr) {
                        val o = e.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                        val i = o.get("id")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
                        if (i.isEmpty()) continue
                        val c = o.get("c")?.takeIf { it.isJsonPrimitive }?.asInt
                            ?: legacySpanToCols(o.get("span")?.takeIf { it.isJsonPrimitive }?.asInt)
                        val r = o.get("r")?.takeIf { it.isJsonPrimitive }?.asInt ?: 1
                        val a = o.get("a")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: true
                        val col = o.get("col")?.takeIf { it.isJsonPrimitive }?.asInt ?: -1
                        val row = o.get("row")?.takeIf { it.isJsonPrimitive }?.asInt ?: -1
                        items.add(HomeSlot(i, c.coerceIn(1, 5), r.coerceIn(1, 5), a, col, row))
                    }
                }
                val hidden = j.get("hidden")?.takeIf { it.isJsonArray }?.asJsonArray?.map { it.asString } ?: emptyList()
                val decos = HashMap<String, DecoCard>()
                val dj = j.get("decos")?.takeIf { it.isJsonObject }?.asJsonObject
                if (dj != null) {
                    for ((k, v) in dj.entrySet()) {
                        if (k.isEmpty()) continue
                        val o = v.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                        val kd = o.get("k")?.takeIf { it.isJsonPrimitive }?.asString ?: "emoji"
                        val tx = o.get("t")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                        decos[k] = DecoCard(k, kd, tx)
                    }
                }
                HomeLayout(items, hidden, decos)
            } catch (_: Exception) {
                null
            }
        }
        
        val v2 = homeLayoutV2(id) ?: return null
        val items = (v2.first ?: emptyList()).map { HomeSlot(it, 5, 1) }
        return HomeLayout(items, v2.second, emptyMap())
    }

    fun saveHomeLayoutV3(id: String, items: List<HomeSlot>, hidden: List<String>, decos: Map<String, DecoCard>) {
        val arr = com.google.gson.JsonArray()
        for (it0 in items) {
            val o = JsonObject()
            o.addProperty("id", it0.id)
            o.addProperty("c", it0.c)
            o.addProperty("r", it0.r)
            o.addProperty("a", it0.auto)
            o.addProperty("col", it0.col)
            o.addProperty("row", it0.row)
            arr.add(o)
        }
        val dj = JsonObject()
        for ((k, v) in decos) {
            val o = JsonObject()
            o.addProperty("k", v.kind)
            o.addProperty("t", v.text)
            dj.add(k, o)
        }
        val j = JsonObject()
        j.add("items", arr)
        j.add("hidden", gson.toJsonTree(hidden))
        j.add("decos", dj)
        sitePrefs(id).edit().putString("home_layout_v3", j.toString()).apply()
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

    
    private val uiPrefs: SharedPreferences
        get() = app.getSharedPreferences("gaycore_ui", Context.MODE_PRIVATE)

    fun lastThemeId(): String? = uiPrefs.getString("theme_id", null)
    fun lastDarkMode(): String = uiPrefs.getString("dark_mode", "system") ?: "system"
    fun lastCustomSeed(): Int? = uiPrefs.getInt("custom_seed", 0).takeIf { it != 0 }
    fun lastDynamicColors(): Boolean = uiPrefs.getBoolean("dynamic_colors", false)

    
    fun startupThemeSet(): Boolean = uiPrefs.getBoolean("startup_theme_set", false)
    fun startupDark(): String = uiPrefs.getString("startup_dark", "system") ?: "system"
    fun startupSeed(): Int? = uiPrefs.getInt("startup_seed", 0).takeIf { it != 0 }
    fun startupDynamic(): Boolean = uiPrefs.getBoolean("startup_dynamic", false)
    fun saveStartupTheme(darkMode: String, customSeed: Int?, dynamicColors: Boolean) {
        uiPrefs.edit().apply {
            putBoolean("startup_theme_set", true)
            putString("startup_dark", darkMode)
            putBoolean("startup_dynamic", dynamicColors)
            if (customSeed != null) putInt("startup_seed", customSeed) else remove("startup_seed")
        }.apply()
    }

    fun saveLastUi(themeId: String?, darkMode: String, customSeed: Int?, dynamicColors: Boolean) {
        uiPrefs.edit().apply {
            if (themeId != null) putString("theme_id", themeId)
            putString("dark_mode", darkMode)
            if (customSeed != null) putInt("custom_seed", customSeed) else remove("custom_seed")
            putBoolean("dynamic_colors", dynamicColors)
        }.apply()
    }

    


    private val seqPrefs: SharedPreferences
        get() = app.getSharedPreferences("gc_intent_seq", Context.MODE_PRIVATE)

    fun nextSeqByToken(token: String, scope: String): Long {
        val p = seqPrefs
        val k = "seq_" + sha8(token) + "_" + scope
        val v = p.getLong(k, 0) + 1
        p.edit().putLong(k, v).apply()
        return v
    }

    fun correctSeqByToken(token: String, scope: String, current: Long) {
        seqPrefs.edit().putLong("seq_" + sha8(token) + "_" + scope, current).apply()
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
