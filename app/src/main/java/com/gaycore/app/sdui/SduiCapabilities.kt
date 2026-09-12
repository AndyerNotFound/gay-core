package com.gaycore.app.sdui

   
                                   
  
          
                                                             
                                                        
                      
  
                                                   
  
        
                                             
                                               
                                                                             
   
object SduiCapabilities {

                               
    const val GCUI_VERSION = 1

                  
    val COMPONENTS = listOf(
        "column", "row", "card", "text", "button", "input", "switch", "progress",
        "kv", "image", "icon", "divider", "spacer", "radio",
        "chip", "badge", "segmented", "avatar", "list",
        "iconButton", "hscroll", "listRow", "metricTile", "metricCard",
    )

                  
    val SHAPES = listOf("pill", "extraLarge", "large", "medium", "small", "none")

                  
    val TEXT_STYLES = listOf("title", "title3", "body", "caption", "mono")

                  
    val BUTTON_STYLES = listOf("filled", "tonal", "outlined", "text")

                  
    val BADGE_TONES = listOf("success", "error", "warning", "primary", "tertiary", "neutral")

                  
    val INPUT_TYPES = listOf("text", "number", "password")

                                    
    val ICONS = listOf(
        "home", "person", "settings", "check", "apps", "add", "close", "refresh",
        "key", "info", "palette", "edit", "star", "group", "copy", "wallet", "bug",
        "search", "chevron", "chevron_up", "chevron_down", "arrow_back",
        "delete", "block", "sort", "filter", "grid", "chat", "schedule", "more",
        "menu", "light_mode", "dark_mode", "send", "bolt", "tune",
    )

               
    val ACTIONS = listOf("intent", "open", "copy", "toast", "refresh", "setState")

    fun supports(component: String): Boolean = COMPONENTS.contains(component)

    fun supportsIcon(icon: String): Boolean = ICONS.contains(icon.removePrefix("msym:"))

       
                    
                                            
       
    fun headers(): Map<String, String> = mapOf(
        "X-GCUI-Version" to GCUI_VERSION.toString(),
        "X-GCUI-Components" to COMPONENTS.joinToString(","),
        "X-GCUI-Icons" to ICONS.joinToString(","),
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
