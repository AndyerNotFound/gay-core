package com.gaycore.app.sdui
















object SduiCapabilities {

    
    const val GCUI_VERSION = 1

    
    val COMPONENTS = listOf(
        "column", "row", "card", "text", "button", "input", "switch", "progress",
        "kv", "image", "icon", "divider", "spacer", "radio",
        "chip", "badge", "segmented", "avatar", "list",
        "iconButton", "hscroll", "listRow", "metricTile", "metricCard",
        
        "pagination", "checkbox", "form", "chart",
    )

    
    val SHAPES = listOf("pill", "full", "extraLarge", "large", "medium", "small", "none")

    
    val TEXT_STYLES = listOf("title", "title3", "body", "caption", "mono")

    
    val BUTTON_STYLES = listOf("filled", "tonal", "outlined", "text")

    
    val BADGE_TONES = listOf("success", "error", "warning", "primary", "tertiary", "neutral")

    
    val INPUT_TYPES = listOf("text", "number", "password")

    

    val ICONS: List<String> get() = SduiIcons.names

    





    val ACTIONS = listOf("intent", "open", "copy", "toast", "close", "refresh", "setState", "adminApi", "client", "toggleSelect")

    
    val ADMIN_ACTIONS = listOf("adminApi", "client")

    
    val CLIENT_ACTIONS = listOf("switchInstance", "openNative", "refreshShell")

    
    val NAV_MODES = listOf("push", "replace", "pop")

    fun supports(component: String): Boolean = COMPONENTS.contains(component)

    fun supportsIcon(icon: String): Boolean = ICONS.contains(icon.removePrefix("msym:"))

    



    fun headers(): Map<String, String> = mapOf(
        "X-GCUI-Version" to GCUI_VERSION.toString(),
        "X-GCUI-Components" to COMPONENTS.joinToString(","),
        "X-GCUI-Icons" to ICONS.joinToString(","),
        

        "X-GCUI-Nav" to NAV_MODES.joinToString(","),
        
        "X-GCUI-Admin" to "1",
        "X-GCUI-Admin-Actions" to ADMIN_ACTIONS.joinToString(","),
    )

    
    fun toJsonString(): String = buildString {
        append("{\"gcui\":").append(GCUI_VERSION)
        append(",\"components\":[").append(COMPONENTS.joinToString(",") { "\"$it\"" }).append("]")
        append(",\"icons\":[").append(ICONS.joinToString(",") { "\"$it\"" }).append("]")
        append(",\"badgeTones\":[").append(BADGE_TONES.joinToString(",") { "\"$it\"" }).append("]")
        append(",\"shapes\":[").append(SHAPES.joinToString(",") { "\"$it\"" }).append("]")
        append("}")
    }
}
