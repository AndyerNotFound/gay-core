package com.gaycore.app.data

import com.google.gson.JsonObject

                                                                

data class ServerInfo(
    val name: String? = null,
    val description: String? = null,
    val icon: String? = null,
    val announcement: String? = null,
    val contact: String? = null,
    val website: String? = null,
)

data class BranchInfo(val name: String, val uid: Long)

data class AuthCaps(
    val cardkey: Boolean = false,
    val user: Boolean = false,
    val gatewayKey: Boolean = false,
    val registration: RegistrationInfo? = null,
)

data class RegistrationInfo(
    val enable: Boolean = false,
    val minPasswordLen: Int = 8,
    val captchaProvider: String = "none",
    val captchaSiteKey: String = "",
    val emailVerify: Boolean = false,
)

data class UserDebug(val forUid: String, val until: Long)

data class LayoutConfig(
    val version: Int = 1,
    val bottomBar: List<String>? = null,                                                  
    val hidden: List<String>? = null,
    val homeOrder: List<String>? = null,
    val theme: String? = null,
)

data class ThemeInfo(
    val id: String,
    val name: String,
    val builtin: Boolean = false,
    val locked: Boolean = false,
    val dark: Map<String, String>? = null,
    val light: Map<String, String>? = null,
    val cssVars: Map<String, String>? = null,
)

data class PluginInfo(
    val id: String,
    val name: String,
    val version: String = "?",
    val author: String = "",
    val description: String = "",
    val icon: String? = null,
    val type: String = "business",
    val builtin: Boolean = false,
    val sha256: String = "",
    val permissions: List<String> = emptyList(),
    val provides: List<String> = emptyList(),
    val appUi: AppUi? = null,
    val userPage: String? = null,
    val hasAdminPage: Boolean = false,
)

                                         
data class AppUi(
    val personal: UiRef? = null,
    val home: UiRef? = null,
    val bottomBar: UiRef? = null,
    val settings: UiRef? = null,
    val pages: List<UiRef>? = null,
    val widgets: List<WidgetInfo>? = null,
)

                                                    
data class WidgetInfo(
    val id: String = "",
    val title: String = "",
    val icon: String = "",
    val ui: String = "",
    val cols: Int = 12,
    val rows: Int = 1,
)

data class UiRef(
    val title: String = "",
    val icon: String = "",
    val ui: String = "",
    val id: String = "",
)

data class Bootstrap(
    val ok: Boolean = false,
    val core: String? = null,
    val appApi: Int = 0,
    val branch: String = "default",
    val uid: Long = 1,
    val serverInfo: ServerInfo? = null,
    val userDebug: UserDebug? = null,
    val layout: LayoutConfig? = null,
    val branches: List<BranchInfo> = emptyList(),
    val auth: AuthCaps = AuthCaps(),
    val themes: List<ThemeInfo> = emptyList(),
    val plugins: List<PluginInfo> = emptyList(),
) {
                                                  
    fun userSystemPlugin(): PluginInfo? =
        plugins.firstOrNull { it.provides.contains("user-system") && it.appUi?.personal != null }

                       
    fun homePlugins(): List<PluginInfo> = plugins.filter { it.appUi?.home != null }

                                                       
    fun allWidgets(): List<Pair<String, WidgetInfo>> = plugins.flatMap { p ->
        (p.appUi?.widgets ?: emptyList()).map { p.id to it }
    }

                    
    fun bottomBarPlugins(): List<PluginInfo> = plugins.filter { it.appUi?.bottomBar != null }

    fun pluginById(id: String): PluginInfo? = plugins.firstOrNull { it.id == id }
}

              
sealed class TabItem {
    abstract val id: String
    abstract val title: String
    abstract val iconName: String

    data class Builtin(override val id: String, override val title: String, override val iconName: String) : TabItem()
    data class Plugin(override val id: String, override val title: String, override val iconName: String, val pluginId: String, val ui: String) : TabItem()
}

                                          
typealias GcPage = JsonObject

                                      

                                           
data class AdminUser(
    val uid: String = "",
    val username: String = "",
    val name: String = "",
    val nickname: String = "",
    val email: String = "",
    val note: String = "",
    val avatar: String = "",
    val banned: Boolean = false,
    val banReason: String = "",
    val createdAt: String = "",
    val keyCount: Int = 0,
    val totalQuota: Long = 0,
    val totalUsed: Long = 0,
)

                                              
data class AdminKey(
    val key: String = "",
    val name: String = "",
    val enable: Boolean = true,
    val quotaTokens: Long = 0,                    
    val usedTokens: Long = 0,
    val uid: String = "",
    val models: List<String> = emptyList(),
    val branches: List<String> = emptyList(),
    val expiresAt: String = "",
    val note: String = "",
    val createdAt: String = "",
) {
    fun masked(): String = if (key.length > 12) key.take(8) + "…" + key.takeLast(4) else key
    fun quotaLabel(): String = when (quotaTokens) {
        -1L -> "不限"
        0L -> "零额度"
        else -> fmtTokens(quotaTokens)
    }
    companion object {
        fun fmtTokens(v: Long): String = when {
            v >= 100_000_000 -> String.format("%.1f亿", v / 100000000.0)
            v >= 10_000 -> String.format("%.1f万", v / 10000.0)
            else -> v.toString()
        }
    }
}
