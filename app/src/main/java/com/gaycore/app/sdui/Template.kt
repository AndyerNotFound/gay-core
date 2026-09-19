package com.gaycore.app.sdui

import com.google.gson.JsonElement
import com.google.gson.JsonObject




object Template {
    private val RE = Regex("""\{\{\s*([a-zA-Z0-9_.-]+)\s*\}\}""")

    
    fun bind(text: String, scope: Map<String, Any?>): String {
        if (!text.contains("{{")) return text
        return RE.replace(text) { m ->
            resolve(m.groupValues[1], scope)?.toString() ?: ""
        }
    }

    
    fun bindRaw(v: Any?, scope: Map<String, Any?>): Any? {
        if (v !is String) return v
        val m = RE.matchEntire(v.trim()) ?: return bind(v, scope)
        return resolve(m.groupValues[1], scope)
    }

    
    fun truthy(v: Any?): Boolean = when (v) {
        null -> false
        is Boolean -> v
        is Number -> v.toDouble() != 0.0
        is String -> v.isNotEmpty() && v != "false" && v != "0"
        else -> true
    }

    fun resolve(path: String, scope: Map<String, Any?>): Any? {
        val parts = path.split('.')
        var cur: Any? = scope[parts[0]] ?: return null
        for (i in 1 until parts.size) {
            cur = when (cur) {
                is JsonObject -> cur.get(parts[i])?.let { unwrap(it) }
                is Map<*, *> -> (cur as Map<*, *>)[parts[i]]
                else -> return null
            } ?: return null
        }
        return cur
    }

    fun unwrap(e: JsonElement?): Any? = when {
        e == null || e.isJsonNull -> null
        e.isJsonPrimitive -> {
            val p = e.asJsonPrimitive
            when {
                p.isBoolean -> p.asBoolean
                p.isNumber -> p.asNumber.let { if (it.toDouble() == it.toLong().toDouble()) it.toLong() else it.toDouble() }
                else -> p.asString
            }
        }
        else -> e
    }

    
    fun bindJson(el: JsonElement, scope: Map<String, Any?>): JsonElement {
        if (el.isJsonPrimitive) {
            val p = el.asJsonPrimitive
            if (p.isString) {
                val raw = bindRaw(p.asString, scope)
                return when (raw) {
                    is Boolean -> com.google.gson.JsonPrimitive(raw)
                    is Number -> com.google.gson.JsonPrimitive(raw)
                    is String -> com.google.gson.JsonPrimitive(raw)
                    
                    is List<*> -> com.google.gson.JsonArray().apply {
                        for (x in raw) add(com.google.gson.JsonPrimitive(x?.toString() ?: ""))
                    }
                    is JsonElement -> raw
                    else -> com.google.gson.JsonPrimitive("")
                }
            }
            return el
        }
        if (el.isJsonObject) {
            val o = JsonObject()
            for ((k, v) in el.asJsonObject.entrySet()) o.add(k, bindJson(v, scope))
            return o
        }
        if (el.isJsonArray) {
            val a = com.google.gson.JsonArray()
            for (v in el.asJsonArray) a.add(bindJson(v, scope))
            return a
        }
        return el
    }

    




    fun bindKeep(text: String, scope: Map<String, Any?>): String {
        if (!text.contains("{{")) return text
        return RE.replace(text) { m -> resolve(m.groupValues[1], scope)?.toString() ?: m.value }
    }

    fun bindRawKeep(v: Any?, scope: Map<String, Any?>): Any? {
        if (v !is String) return v
        val m = RE.matchEntire(v.trim()) ?: return bindKeep(v, scope)
        return resolve(m.groupValues[1], scope) ?: v
    }

    fun bindJsonKeep(el: JsonElement, scope: Map<String, Any?>): JsonElement {
        if (el.isJsonPrimitive) {
            val p = el.asJsonPrimitive
            if (p.isString) {
                val raw = bindRawKeep(p.asString, scope)
                return when (raw) {
                    is Boolean -> com.google.gson.JsonPrimitive(raw)
                    is Number -> com.google.gson.JsonPrimitive(raw)
                    is String -> com.google.gson.JsonPrimitive(raw)
                    is List<*> -> com.google.gson.JsonArray().apply {
                        for (x in raw) add(com.google.gson.JsonPrimitive(x?.toString() ?: ""))
                    }
                    is JsonElement -> raw
                    else -> com.google.gson.JsonPrimitive("")
                }
            }
            return el
        }
        if (el.isJsonObject) {
            val o = JsonObject()
            for ((k, v) in el.asJsonObject.entrySet()) o.add(k, bindJsonKeep(v, scope))
            return o
        }
        if (el.isJsonArray) {
            val a = com.google.gson.JsonArray()
            for (v in el.asJsonArray) a.add(bindJsonKeep(v, scope))
            return a
        }
        return el
    }
}
