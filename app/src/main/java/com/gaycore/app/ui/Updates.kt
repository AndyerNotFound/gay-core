package com.gaycore.app.ui

import android.content.SharedPreferences
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.gaycore.app.data.PluginInfo
import com.gaycore.app.data.PluginWatch
import com.gaycore.app.theme.ThemeEngine








object Updates {

    fun check(act: AppCompatActivity, theme: ThemeEngine, prefs: SharedPreferences, plugins: List<PluginInfo>) {
        if (plugins.isEmpty()) return
        
        if (!prefs.contains("plugin_snapshot_v1")) {
            try { PluginWatch.commit(prefs, PluginWatch.diff(prefs, plugins).second) } catch (_: Exception) { }
            return
        }
        val pair = try { PluginWatch.diff(prefs, plugins) } catch (_: Exception) { return }
        val changes = pair.first
        
        try { PluginWatch.commit(prefs, pair.second) } catch (_: Exception) { }
        if (changes.isEmpty()) return

        val danger = changes.any { it.danger }
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DemoKit.dp(act, 18), DemoKit.dp(act, 8), DemoKit.dp(act, 18), DemoKit.dp(act, 4))
        }
        DemoKit.put(
            col,
            DemoKit.txt(
                act, theme,
                if (danger) "发现插件变更，其中有需要你确认的安全项：" else "服务端插件有变化：",
                12.5f, false, if (danger) "error" else "onSurfaceVariant",
            ),
        )
        for (c in changes) {
            DemoKit.put(
                col,
                DemoKit.txt(act, theme, (if (c.danger) "⚠  " else "•  ") + PluginWatch.line(c), 12.5f, c.danger, if (c.danger) "error" else "onSurface"),
                7,
            )
        }
        DemoKit.put(
            col,
            DemoKit.txt(act, theme, "若不清楚这些变更的来源，请先到管理端的「插件」页核对作者与权限，再决定是否继续使用。", 11f, false, "onSurfaceVariant"),
            12,
        )
        DemoKit.put(
            col,
            DemoKit.button(act, theme, "查看插件详情", "outlined") {
                UiKit.infoDialog(act, theme, "当前服务端插件", detail(plugins))
            },
            6,
        )
        UiKit.customDialog(act, theme, if (danger) "插件安全提醒" else "插件已更新", col, "知道了") { }
    }

    private fun detail(plugins: List<PluginInfo>): String {
        val sb = StringBuilder()
        for (p in plugins) {
            sb.append(p.name).append("  v").append(p.version).append('\n')
            sb.append("  id=").append(p.id)
            if (p.author.isNotEmpty()) sb.append("  作者 ").append(p.author)
            sb.append('\n')
            if (p.sha256.isNotEmpty()) {
                sb.append("  sha256=").append(p.sha256.take(16)).append("…\n")
            }
            if (p.permissions.isNotEmpty()) {
                sb.append("  权限: ").append(p.permissions.joinToString(", ")).append('\n')
            }
            sb.append('\n')
        }
        return sb.toString().trimEnd()
    }

    
    fun textCol(act: AppCompatActivity, theme: ThemeEngine, text: String): View {
        val col = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(col, DemoKit.txt(act, theme, text, 12.5f, false, "onSurfaceVariant"))
        return col
    }
}
