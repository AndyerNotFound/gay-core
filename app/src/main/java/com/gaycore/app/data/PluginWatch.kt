package com.gaycore.app.data

import android.content.SharedPreferences
import com.google.gson.JsonObject
import com.google.gson.JsonParser











object PluginWatch {

    data class Change(
        val kind: String,       
        val id: String,
        val name: String,
        val old: String,
        val now: String,
        val danger: Boolean = false,
    )

    private const val KEY = "plugin_snapshot_v1"

    private fun shortSha(s: String): String = if (s.length > 12) s.substring(0, 12) else s
    private fun permStr(p: PluginInfo): String = p.permissions.sorted().joinToString(",")

    
    fun diff(prefs: SharedPreferences, plugins: List<PluginInfo>): Pair<List<Change>, JsonObject> {
        val prev: JsonObject = try {
            JsonParser.parseString(prefs.getString(KEY, "{}") ?: "{}").asJsonObject
        } catch (_: Exception) { JsonObject() }

        val out = ArrayList<Change>()
        for (pl in plugins) {
            val node = prev.get(pl.id)?.takeIf { it.isJsonObject }?.asJsonObject
            if (node == null) {
                out.add(Change("added", pl.id, pl.name, "", pl.version))
                continue
            }
            val ov = node.get("v")?.takeIf { !it.isJsonNull }?.asString ?: ""
            val os = node.get("s")?.takeIf { !it.isJsonNull }?.asString ?: ""
            val op = node.get("p")?.takeIf { !it.isJsonNull }?.asString ?: ""
            val ns = shortSha(pl.sha256)
            val np = permStr(pl)
            if (ov.isNotEmpty() && ov != pl.version) out.add(Change("version", pl.id, pl.name, ov, pl.version))
            
            if (os.isNotEmpty() && ns.isNotEmpty() && os != ns && ov == pl.version) {
                out.add(Change("sha", pl.id, pl.name, os, ns, danger = true))
            }
            if (op.isNotEmpty() && op != np) {
                out.add(Change("permission", pl.id, pl.name, op, np, danger = true))
            }
        }
        val ids = plugins.map { it.id }.toSet()
        for (k in prev.keySet()) {
            if (k !in ids) {
                val nm = prev.get(k)?.takeIf { it.isJsonObject }?.asJsonObject?.get("n")?.asString ?: ""
                out.add(Change("removed", k, nm, "", ""))
            }
        }
        val next = JsonObject()
        for (pl in plugins) {
            next.add(pl.id, JsonObject().apply {
                addProperty("v", pl.version)
                addProperty("s", shortSha(pl.sha256))
                addProperty("p", permStr(pl))
                addProperty("n", pl.name)
            })
        }
        return Pair(out, next)
    }

    
    fun commit(prefs: SharedPreferences, snap: JsonObject) {
        prefs.edit().putString(KEY, snap.toString()).apply()
    }

    
    fun line(c: Change): String = when (c.kind) {
        "added" -> "新增插件 · " + c.name + "（v" + c.now + "）"
        "removed" -> "移除插件 · " + c.name.ifEmpty { c.id }
        "version" -> "版本更新 · " + c.name + "  v" + c.old + " → v" + c.now
        "sha" -> "内容变更但版本号未变 · " + c.name + "（v" + c.now + "，摘要 " + c.old + " → " + c.now + "）"
        "permission" -> "权限声明变化 · " + c.name
        else -> c.kind + " · " + c.name
    }
}
