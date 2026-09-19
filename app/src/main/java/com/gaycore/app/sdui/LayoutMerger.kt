package com.gaycore.app.sdui

import com.gaycore.app.data.Bootstrap
import com.gaycore.app.data.LayoutConfig
import com.gaycore.app.data.TabItem



object LayoutMerger {

    


    const val MAX_TABS = 12
    val DEFAULT_ORDER = listOf("home", "personal", "settings")

    
    fun builtinTitles(id: String): Pair<String, String> = when (id) { 
        "home" -> "首页" to "home"
        "models" -> "模型" to "apps"
        "plugins" -> "插件" to "star"
        "usage" -> "使用" to "wallet"
        "personal" -> "个人" to "person"
        "notice" -> "公告" to "info"
        "theme" -> "主题" to "palette"
        "settings" -> "设置" to "settings"
        else -> id to "apps"
    }

    val BUILTIN_IDS = setOf("home", "models", "plugins", "usage", "personal", "notice", "theme", "settings")

    fun mergeTabs(server: LayoutConfig?, local: LayoutConfig?, bootstrap: Bootstrap?): List<TabItem> {
        val order = local?.bottomBar ?: server?.bottomBar ?: DEFAULT_ORDER
        val hidden = (local?.hidden ?: server?.hidden) ?: emptyList()

        val out = mutableListOf<TabItem>()
        for (raw in order) {
            if (raw in hidden) continue
            when {
                raw in BUILTIN_IDS -> {
                    val (t, ic) = builtinTitles(raw)
                    out.add(TabItem.Builtin(raw, t, ic))
                }
                raw.startsWith("plugin:") -> {
                    val pid = raw.removePrefix("plugin:")
                    val p = bootstrap?.pluginById(pid)
                    val bb = p?.appUi?.bottomBar
                    if (p != null && bb != null) {
                        out.add(TabItem.Plugin(raw, bb.title.ifEmpty { p.name }, bb.icon.ifEmpty { "apps" }, pid, bb.ui))
                    } else {
                        
                        out.add(TabItem.Builtin(raw, "缺失", "close"))
                    }
                }
            }
        }
        


        val capped = out.take(MAX_TABS).toMutableList()
        if (capped.none { it.id == "home" }) capped.add(0, TabItem.Builtin("home", "首页", "home"))
        if (capped.none { it.id == "settings" }) capped.add(TabItem.Builtin("settings", "设置", "settings"))
        return capped
    }

    
    fun mergeHomeOrder(server: LayoutConfig?, local: LayoutConfig?, bootstrap: Bootstrap?): List<String> {
        val declared = bootstrap?.homePlugins()?.map { it.id } ?: emptyList()
        val order = local?.homeOrder ?: server?.homeOrder ?: emptyList()
        val out = mutableListOf<String>()
        for (id in order) if (id in declared && id !in out) out.add(id)
        for (id in declared) if (id !in out) out.add(id)
        return out
    }

    
    fun validate(order: List<String>): String? =
        if (order.size > MAX_TABS) "最多 $MAX_TABS 个入口 (当前 ${order.size})" else null
}
