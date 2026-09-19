package com.gaycore.app.ui

















object Notices {

    data class Item(val time: String, val target: String, val body: String) {
        fun encode(): String {
            val sb = StringBuilder()
            if (time.isNotEmpty()) sb.append('[').append(time).append("] ")
            if (target.isNotEmpty()) sb.append('@').append(target).append(' ')
            sb.append(body)
            return sb.toString()
        }
    }

    fun parse(raw: String?): MutableList<Item> {
        val out = ArrayList<Item>()
        if (raw.isNullOrBlank()) return out
        for (line in raw.split('\n')) {
            var t = line.trim()
            if (t.isEmpty()) continue
            var time = ""
            if (t.startsWith("[")) {
                val close = t.indexOf(']')
                if (close > 0) {
                    time = t.substring(1, close).trim()
                    t = t.substring(close + 1).trim()
                }
            }
            var target = ""
            if (t.startsWith("@")) {
                val sp = t.indexOfFirst { it == ' ' }
                if (sp > 0) {
                    target = t.substring(1, sp).trim()
                    t = t.substring(sp + 1).trim()
                }
            }
            if (t.isEmpty()) continue
            out.add(Item(time, target, t))
        }
        return out
    }

    fun encode(list: List<Item>): String = list.joinToString("\n") { it.encode() }

    
    fun visibleTo(list: List<Item>, uid: String): List<Item> =
        list.filter { it.target.isEmpty() || (uid.isNotEmpty() && it.target == uid) }

    const val MAX_LENGTH = 2000
}
