package com.gaycore.app.ui

import android.content.Intent
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.gaycore.app.App
import com.gaycore.app.R
import com.gaycore.app.data.ServerEntry
import com.gaycore.app.theme.ThemeEngine
import com.gaycore.app.ui.UiKit.add
import com.gaycore.app.ui.UiKit.dp
import com.google.gson.JsonObject


class WelcomeActivity : BaseActivity() {

    
    private val theme: ThemeEngine by lazy {
        val st = App.of(this).store
        if (st.startupThemeSet()) {
            ThemeEngine(null, st.startupDark(), st.startupDynamic(), st.startupSeed())
        } else {
            val last = st.list().maxByOrNull { e -> e.addedAt }
            val list = last?.bootstrap()?.themes ?: emptyList()
            val info = list.firstOrNull { it.id == st.lastThemeId() } ?: list.firstOrNull()
            ThemeEngine(info, st.lastDarkMode(), st.lastDynamicColors(), st.lastCustomSeed())
        }
    }

    override fun tintTheme(): ThemeEngine? = theme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.applyToWindow(this)
        render()
        showCrashIfAny()
    }

    
    private fun showCrashIfAny() {
        val log = com.gaycore.app.CrashHandler.peek(this) ?: return
        val dlg = UiKit.ThemedDialogBuilder(this, theme)
            .setTitle(getString(R.string.crash_title))
            .setMessage(getString(R.string.crash_hint) + "\n\n" + log)
            .setPositiveButton(R.string.crash_ok) { _, _ -> com.gaycore.app.CrashHandler.clear(this) }
            .setCancelable(false)
            .create()
        dlg.setOnShowListener { UiKit.tintDialog(dlg, theme) }
        dlg.show()
    }

    override fun onResume() {
        super.onResume()
        render() 
    }

    private fun render() {
        val store = App.of(this).store
        val userServers = store.listByMode("user")
        val adminServers = store.listByMode("admin")

        val children = com.google.gson.JsonArray()
        
        children.add(JsonObject().apply { addProperty("type", "text"); addProperty("text", getString(R.string.welcome_title)); addProperty("style", "title") })
        children.add(JsonObject().apply { addProperty("type", "text"); addProperty("text", getString(R.string.welcome_subtitle)); addProperty("style", "caption") })
        children.add(JsonObject().apply {
            addProperty("type", "button")
            addProperty("text", getString(R.string.ui_demo_entry))
            addProperty("style", "tonal")
            addProperty("icon", "palette")
            add("action", JsonObject().apply { addProperty("type", "intent"); addProperty("endpoint", "./ui-demo") })
        })
        children.add(JsonObject().apply {
            addProperty("type", "button")
            addProperty("text", "启动页主题")
            addProperty("style", "text")
            addProperty("icon", "palette")
            add("action", JsonObject().apply { addProperty("type", "intent"); addProperty("endpoint", "./theme") })
        })
        children.add(JsonObject().apply { addProperty("type", "spacer"); addProperty("height", 20) })

        
        children.add(JsonObject().apply { addProperty("type", "text"); addProperty("text", "用户服务器"); addProperty("style", "title3") })
        if (userServers.isEmpty()) {
            children.add(JsonObject().apply { addProperty("type", "text"); addProperty("text", "暂无用户服务器"); addProperty("style", "caption") })
        }
        for (n in serverRows(userServers, "user")) children.add(n)
        children.add(JsonObject().apply { addProperty("type", "button"); addProperty("text", getString(R.string.add_server)); addProperty("style", "outlined"); addProperty("icon", "add"); add("action", JsonObject().apply { addProperty("type", "intent"); addProperty("endpoint", "./add:user") }) })
        children.add(JsonObject().apply { addProperty("type", "spacer"); addProperty("height", 16) })

        
        children.add(JsonObject().apply { addProperty("type", "text"); addProperty("text", "管理服务器"); addProperty("style", "title3") })
        if (adminServers.isEmpty()) {
            children.add(JsonObject().apply { addProperty("type", "text"); addProperty("text", "暂无管理服务器"); addProperty("style", "caption") })
        }
        for (n in serverRows(adminServers, "admin")) children.add(n)
        children.add(JsonObject().apply { addProperty("type", "button"); addProperty("text", getString(R.string.add_server)); addProperty("style", "outlined"); addProperty("icon", "add"); add("action", JsonObject().apply { addProperty("type", "intent"); addProperty("endpoint", "./add:admin") }) })

        children.add(JsonObject().apply { addProperty("type", "spacer"); addProperty("height", 10) })
        children.add(JsonObject().apply {
            addProperty("type", "button"); addProperty("text", "服务器诊断（排查用）"); addProperty("style", "text")
            add("action", JsonObject().apply { addProperty("type", "intent"); addProperty("endpoint", "./diagnose") })
        })

        val page = JsonObject().apply { addProperty("gcui", 1); add("root", JsonObject().apply { addProperty("type", "column"); addProperty("gap", 8); addProperty("padding", 16); add("children", children) }) }

        val renderer = com.gaycore.app.sdui.Renderer(this, theme, object : com.gaycore.app.sdui.Renderer.Host {
            override fun onAction(action: JsonObject) {
                when (action.get("type")?.asString) {
                    "intent" -> {
                        val ep = action.get("endpoint")?.asString ?: ""
                        val id = action.getAsJsonObject("body")?.get("id")?.asString
                        val confirm = action.get("confirm")?.asString
                        val run = {
                            when {
                                ep.startsWith("./add:") -> {
                                    val mode = ep.removePrefix("./add:")
                                    startActivity(Intent(this@WelcomeActivity, AddServerActivity::class.java).putExtra("mode", mode))
                                }
                                ep == "./ui-demo" -> startActivity(Intent(this@WelcomeActivity, UiDemoActivity::class.java))
                                ep == "./diagnose" -> showDiagnostics()
                                ep == "./theme" -> showThemeDialog()
                                ep.contains("open") && id != null -> {
                                    val mode = action.getAsJsonObject("body")?.get("mode")?.asString ?: "user"
                                    val s = store.get(id)
                                    if (s != null) {
                                        val cls = if (mode == "admin") AdminMainActivity::class.java else UserMainActivity::class.java
                                        if (mode == "admin") store.currentAdminServer = s.id else store.currentUserServer = s.id
                                        startActivity(Intent(this@WelcomeActivity, cls).putExtra("serverId", s.id))
                                    }
                                }
                                ep.contains("edit") && id != null -> { store.get(id)?.let { showEditDialog(it) } }
                            }
                        }
                        if (!confirm.isNullOrEmpty()) UiKit.confirm(this@WelcomeActivity, getString(R.string.confirm), confirm, theme) { run() } else run()
                    }
                }
            }
            override fun userScope(): Map<String, Any?> = emptyMap()
        })
        val content = android.widget.LinearLayout(this)
        
        try {
            renderer.render(page, content)
        } catch (e: Throwable) {
            content.addView(
                UiKit.text(this, "启动页渲染失败: " + (e.message ?: e.javaClass.simpleName), 13f, false, theme.color(this, "error")),
            )
        }
        if (content.childCount == 0) {
            content.addView(UiKit.text(this, "启动页没有可显示的内容（服务器列表为空且渲染未产出节点）", 13f, false, theme.color(this, "onSurfaceVariant")))
        }
        val scroll = UiKit.scroll(this, content)
        val root = android.widget.FrameLayout(this)
        root.setBackgroundColor(theme.color(this, "background"))
        root.addView(scroll, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
        setContentView(root)
    }

    private fun serverCardNode(s: ServerEntry, mode: String): JsonObject {
        val b = s.bootstrap()
        val title = s.name.ifEmpty { b?.serverInfo?.name?.takeIf { it.isNotBlank() } ?: s.baseUrl }
        val url = s.baseUrl + (if (s.branch != "default" && s.branch.isNotEmpty()) " / " + s.branch else "")
        val modeTags = mutableListOf<String>()
        if (s.tokenEnc != null) modeTags.add("用户")
        if (s.adminKeyEnc != null) modeTags.add("管理")
        val tagStr = if (modeTags.isNotEmpty()) modeTags.joinToString(" · ") else ""
        val cardChildren = com.google.gson.JsonArray().apply {
            add(JsonObject().apply { addProperty("type", "text"); addProperty("text", title); addProperty("style", "title3") })
            add(JsonObject().apply { addProperty("type", "text"); addProperty("text", url); addProperty("style", "caption") })
            if (tagStr.isNotEmpty()) add(JsonObject().apply { addProperty("type", "text"); addProperty("text", tagStr); addProperty("style", "caption") })
        }
        return JsonObject().apply {
            addProperty("type", "card")
            add("children", cardChildren)
            add("action", JsonObject().apply { addProperty("type", "intent"); addProperty("endpoint", "./open"); add("body", JsonObject().apply { addProperty("id", s.id); addProperty("mode", mode) }) })
            add("longAction", JsonObject().apply { addProperty("type", "intent"); addProperty("endpoint", "./edit"); add("body", JsonObject().apply { addProperty("id", s.id) }) })
        }
    }

    
    private fun serverRows(list: List<ServerEntry>, mode: String): List<JsonObject> {
        val out = ArrayList<JsonObject>()
        var i = 0
        while (i < list.size) {
            if (i == list.size - 1) {
                out.add(serverCardNode(list[i], mode))       
                i++
            } else {
                val row = JsonObject().apply { addProperty("type", "row"); addProperty("gap", 8) }
                val kids = com.google.gson.JsonArray()
                val n1 = serverCardNode(list[i], mode); n1.addProperty("weight", 1); kids.add(n1)
                val n2 = serverCardNode(list[i + 1], mode); n2.addProperty("weight", 1); kids.add(n2)
                row.add("children", kids)
                out.add(row)
                i += 2
            }
        }
        return out
    }

    
    

    private fun showThemeDialog() {
        val st = App.of(this).store
        var dark = if (st.startupThemeSet()) st.startupDark() else "system"
        var dyn = if (st.startupThemeSet()) st.startupDynamic() else false
        var seed: Int? = if (st.startupThemeSet()) st.startupSeed() else null

        val host = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL }
        val pad = DemoKit.dp(this, 8)
        host.setPadding(pad, pad, pad, pad)

        val modes = listOf("dark" to "深色", "light" to "浅色", "system" to "跟随系统")
        lateinit var rebuild: () -> Unit
        rebuild = {
            host.removeAllViews()
            DemoKit.put(host, DemoKit.txt(this, theme, "显示模式", 13f, false, "onSurfaceVariant"))
            DemoKit.put(host, DemoKit.segmented(this, theme, modes.map { it.second }, modes.indexOfFirst { it.first == dark }.coerceAtLeast(0)) { idx ->
                dark = modes[idx].first; rebuild()
            }, 12)
            DemoKit.put(host, DemoKit.switchRow(this, theme, "Material You 动态取色", "跟随系统壁纸生成配色 (Android 12+)", dyn) { on ->
                dyn = on
                if (on) seed = null  
                rebuild()
            }, 10)
            val seedSub = if (dyn) "当前由 Material You 生效 —— 选任意颜色会关闭它"
                else if (seed != null) "正在使用自定义主色 (点「跟随主题」恢复)"
                else "选一个颜色自动生成整套配色"
            DemoKit.put(host, DemoKit.sectionTitle(this, theme, "自定义主色", seedSub), 10)
            DemoKit.put(host, DemoKit.seedPalette(this, theme, seed) { s ->
                seed = s
                if (s != null) dyn = false  
                rebuild()
            }, 10)
        }
        rebuild()

        UiKit.customDialog(this, theme, "启动页主题", host, "保存") {
            st.saveStartupTheme(dark, seed, dyn)
            recreate()
        }
    }

    private fun showDiagnostics() {
        val st = App.of(this).store
        val all = st.list().sortedWith(compareBy({ it.mode }, { it.name }))
        val sb = StringBuilder()
        sb.append("共 ").append(all.size).append(" 条本地服务器记录\n")
        sb.append("当前用户端: ").append(st.currentUserServer?.takeLast(9) ?: "—").append("\n")
        sb.append("当前管理端: ").append(st.currentAdminServer?.takeLast(9) ?: "—").append("\n\n")
        for (e in all) {
            sb.append("[").append(e.id).append("]\n")
            sb.append("  端: ").append(if (e.mode == "admin") "管理端" else "用户端").append("\n")
            sb.append("  显示名: ").append(e.name.ifEmpty { "(空)" }).append("\n")
            sb.append("  地址: ").append(e.baseUrl).append("\n")
            sb.append("  分支: ").append(e.branch).append("   管理实例: ").append(e.adminInstance ?: "").append("\n")
            sb.append("  卡密: ").append(if (e.token() != null) "有" else "无")
                .append("   管理密钥: ").append(if (e.adminKey() != null) "有" else "无").append("\n\n")
        }
        sb.append("提示: 用户端/管理端应是两条独立记录(id 分别以 :user / :admin 结尾)。\n")
        sb.append("若这里只有一条而两端都能进, 说明两端在用同一条 —— 请把本页截图反馈。")
        UiKit.infoDialog(this, theme, "服务器诊断", sb.toString())
    }

    private fun showEditDialog(s: ServerEntry) {
        val et = android.widget.EditText(this).apply {
            setText(s.name)
            hint = "本机显示名"
            setTextColor(theme.color(this@WelcomeActivity, "onSurface"))
            setHintTextColor(theme.color(this@WelcomeActivity, "onSurfaceVariant"))
        }
        val pad = dp(this, 16f)
        val tip = android.widget.TextView(this).apply {
            text = "这里改的是【本机显示名】—— 只影响这台手机上的显示，不会改动服务点名称，也不影响另一端。\n服务点的正式名称在「管理端 → 设置 → 服务点资料」里改（那是服务端全局配置，所有客户端都会变）。"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(theme.color(this@WelcomeActivity, "onSurfaceVariant"))
            setPadding(0, dp(this@WelcomeActivity, 6), 0, 0)
        }
        val container = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(pad, pad / 2, pad, 0); addView(et); addView(tip) }
        UiKit.ThemedDialogBuilder(this, theme)
            .setTitle("编辑「${s.name}」· " + (if (s.mode == "admin") "管理端" else "用户端"))
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                s.name = et.text.toString().trim().ifEmpty { s.name }
                App.of(this).store.save(s)
                render()
            }
            .setNeutralButton("删除") { _, _ ->
                UiKit.confirm(this, "移除服务器", "确定移除「${s.name}」? 本地数据将被清除。", theme) {
                    App.of(this).store.remove(s.id)
                    render()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun open(s: ServerEntry) {
        val store = App.of(this).store
        val isUser = s.tokenEnc != null
        val cls = if (isUser) UserMainActivity::class.java else AdminMainActivity::class.java
        if (isUser) store.currentUserServer = s.id else store.currentAdminServer = s.id
        startActivity(Intent(this, cls).putExtra("serverId", s.id))
    }
}
