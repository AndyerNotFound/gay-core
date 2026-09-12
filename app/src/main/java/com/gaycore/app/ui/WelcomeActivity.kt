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

    private val theme = ThemeEngine(null)                            

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.applyToWindow(this)
        render()
        showCrashIfAny()
    }

                                   
    private fun showCrashIfAny() {
        val log = com.gaycore.app.CrashHandler.peek(this) ?: return
        val dlg = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.crash_title))
            .setMessage(getString(R.string.crash_hint) + "\n\n" + log)
            .setPositiveButton(R.string.crash_ok) { _, _ -> com.gaycore.app.CrashHandler.clear(this) }
            .setCancelable(false)
            .create()
        dlg.setOnShowListener {
        }
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
        children.add(JsonObject().apply { addProperty("type", "spacer"); addProperty("height", 20) })

                     
        children.add(JsonObject().apply { addProperty("type", "text"); addProperty("text", "用户服务器"); addProperty("style", "title3") })
        if (userServers.isEmpty()) {
            children.add(JsonObject().apply { addProperty("type", "text"); addProperty("text", "暂无用户服务器"); addProperty("style", "caption") })
        }
        for (s in userServers) children.add(serverCardNode(s, "user"))
        children.add(JsonObject().apply { addProperty("type", "button"); addProperty("text", getString(R.string.add_server)); addProperty("style", "outlined"); addProperty("icon", "add"); add("action", JsonObject().apply { addProperty("type", "intent"); addProperty("endpoint", "./add:user") }) })
        children.add(JsonObject().apply { addProperty("type", "spacer"); addProperty("height", 16) })

                     
        children.add(JsonObject().apply { addProperty("type", "text"); addProperty("text", "管理服务器"); addProperty("style", "title3") })
        if (adminServers.isEmpty()) {
            children.add(JsonObject().apply { addProperty("type", "text"); addProperty("text", "暂无管理服务器"); addProperty("style", "caption") })
        }
        for (s in adminServers) children.add(serverCardNode(s, "admin"))
        children.add(JsonObject().apply { addProperty("type", "button"); addProperty("text", getString(R.string.add_server)); addProperty("style", "outlined"); addProperty("icon", "add"); add("action", JsonObject().apply { addProperty("type", "intent"); addProperty("endpoint", "./add:admin") }) })

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

    private fun showEditDialog(s: ServerEntry) {
        val et = android.widget.EditText(this).apply {
            setText(s.name)
            hint = "服务器名称"
            setTextColor(theme.color(this@WelcomeActivity, "onSurface"))
            setHintTextColor(theme.color(this@WelcomeActivity, "onSurfaceVariant"))
        }
        val pad = dp(this, 16f)
        val container = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(pad, pad / 2, pad, 0); addView(et) }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("编辑「${s.name}」")
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
