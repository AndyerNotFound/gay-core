package com.gaycore.app.ui

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.gaycore.app.App
import com.gaycore.app.R
import com.gaycore.app.data.Api
import com.gaycore.app.data.Bootstrap
import com.gaycore.app.data.LayoutConfig
import com.gaycore.app.data.ServerEntry
import com.gaycore.app.data.TabItem
import com.gaycore.app.data.ThemeInfo
import com.gaycore.app.sdui.LayoutMerger
import com.gaycore.app.theme.ThemeEngine
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

   
                                                         
  
                                             
                                                                   
   
class UserMainActivity : ShellActivity() {

    private lateinit var server: ServerEntry
    private var bootstrap: Bootstrap? = null
                                                         
    private var lastBootstrapAt = 0L
    private var lastPluginTabIds: List<String> = emptyList()

    private val controllers = mutableMapOf<String, SduiController>()
    private var currentThemeId: String? = null
    private var recents = mutableListOf<String>()

                
    private var modelCache: List<String>? = null
    private var modelQuery = ""
    private var modelGrid = false
    private var pluginQuery = ""
    private var pluginTab = 0

    companion object {
        const val P_HOME = "home"
        const val P_MODELS = "models"
        const val P_PLUGINS = "plugins"
        const val P_USAGE = "usage"
        const val P_PERSONAL = "personal"
        const val P_NOTICE = "notice"
        const val P_THEME = "theme"
        const val P_SETTINGS = "settings"
    }

                                                        

    override fun onCreate(savedInstanceState: Bundle?) {
        val store = App.of(this).store
        val s = store.get(intent.getStringExtra("serverId") ?: store.currentUserServer ?: "")
        if (s == null) {
            super.onCreate(savedInstanceState)
            finish()
            return
        }
        server = s
        bootstrap = s.bootstrap()
        try {
            super.onCreate(savedInstanceState)
        } catch (e: Throwable) {
            showFatal(e)
            return
        }
        lastPluginTabIds = pluginTabIds(bootstrap)
        lastBootstrapAt = System.currentTimeMillis()
                                        
        reloadBootstrap()
    }

                                                 
    private fun pluginTabIds(b: Bootstrap?): List<String> =
        if (b == null) emptyList()
        else LayoutMerger.mergeTabs(b.layout, App.of(this).store.localLayout(server.id), b)
            .filterIsInstance<TabItem.Plugin>().map { it.id }

       
                      
      
                                                     
                                               
       
    private fun reloadBootstrap() {
        lifecycleScope.launch(Dispatchers.IO) {
            val b = try { Api.bootstrap(server.baseUrl, server.branch) } catch (_: Exception) { null }
            if (b != null) App.of(this@UserMainActivity).store.updateBootstrap(server.id, b)
            withContext(Dispatchers.Main) {
                if (b == null) { refreshCurrentPage(); return@withContext }                     
                val oldIds = lastPluginTabIds
                bootstrap = b
                lastBootstrapAt = System.currentTimeMillis()
                val newIds = pluginTabIds(b)
                val newTheme = App.of(this@UserMainActivity).store.themeId(server.id) ?: b.layout?.theme
                if (oldIds != newIds || newTheme != currentThemeId) {
                    lastPluginTabIds = newIds
                                                           
                    rebuildShell(false)
                } else {
                    refreshCurrentPage()
                }
            }
        }
    }

    override fun onShellResume() {
                                      
        if (System.currentTimeMillis() - lastBootstrapAt < 30_000) return
        reloadBootstrap()
    }

    private fun showFatal(e: Throwable) {
        val te = ThemeEngine(null)
        te.applyToWindow(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(te.color(this@UserMainActivity, "background"))
        }
        DemoKit.put(col, DemoKit.txt(this, te, "界面构建失败", 20f, true, "error"))
        DemoKit.put(col, DemoKit.txt(this, te, e.javaClass.simpleName + ": " + (e.message ?: ""), 13f, false, "onSurfaceVariant"), 8)
        DemoKit.put(col, DemoKit.button(this, te, getString(R.string.back), "tonal") { finish() }, 16)
        setContentView(pageScroll(col))
    }

                                                        

    override fun createTheme(): ThemeEngine {
        val store = App.of(this).store
        currentThemeId = store.themeId(server.id) ?: bootstrap?.layout?.theme
        val list = bootstrap?.themes ?: emptyList()
        return ThemeEngine(
            list.firstOrNull { it.id == currentThemeId } ?: list.firstOrNull(),
            store.darkMode(server.id),
            store.dynamicColors(server.id),
        )
    }

    override fun shellItems(): List<ShellItem> {
                                      
        val all = listOf(
            ShellItem(P_HOME, "首页", "服务点与常用入口", R.drawable.ic_home),
            ShellItem(P_MODELS, "模型", "本卡可用模型", R.drawable.ic_apps),
            ShellItem(P_PLUGINS, "插件", "服务点插件中心", R.drawable.ic_star),
            ShellItem(P_USAGE, "使用", "额度与用量", R.drawable.ic_wallet),
            ShellItem(P_PERSONAL, "个人中心", "账号与卡密", R.drawable.ic_person),
            ShellItem(P_NOTICE, "公告", "服务点公告", R.drawable.ic_info),
            ShellItem(P_THEME, "主题", "外观与配色", R.drawable.ic_palette),
            ShellItem(P_SETTINGS, "设置", "账号与关于", R.drawable.ic_settings),
        )
        val b = bootstrap ?: return all
        val store = App.of(this).store
                                                        
                                                      
                                                   
                                                             
                                               
        val tabs = LayoutMerger.mergeTabs(b.layout, null, b)
        if (tabs.isEmpty()) return all
        val byId = all.associateBy { it.id }
        val out = ArrayList<ShellItem>()
        for (t in tabs) {
            when (t) {
                is TabItem.Builtin -> byId[t.id]?.let { out.add(it) }
                is TabItem.Plugin -> out.add(
                    ShellItem("plugin:" + t.pluginId, t.title, "插件页面", DemoKit.iconRes(t.iconName)),
                )
            }
        }
        return out.ifEmpty { all }
    }

       
                      
                                                 
                                             
       
    private fun modelPageId(): String =
        shellItems().firstOrNull { it.id == "plugin:model-square" }?.id ?: P_MODELS

    override fun buildPage(id: String): View = when {
        id == P_HOME -> homePage()
        id == P_MODELS -> modelsPage()
        id == P_PLUGINS -> pluginsPage()
        id == P_USAGE -> usagePage()
        id == P_PERSONAL -> personalPage()
        id == P_NOTICE -> noticePage()
        id == P_THEME -> themePage()
        id == P_SETTINGS -> settingsPage()
        id.startsWith("plugin:") -> pluginTabPage(id.removePrefix("plugin:"))
        else -> missingPage(id)
    }

    override fun onRefresh(id: String) {
        if (id == P_MODELS) modelCache = null
        controllers.clear()
                                                       
        reloadBootstrap()
    }

    override fun fabFor(id: String): FabSpec? =
        if (id == P_HOME) FabSpec(R.drawable.ic_edit, "编辑首页布局") { openHomeLayoutEditor() } else null

    override fun topActionFor(id: String): TopAction = when (id) {
        P_HOME -> TopAction(R.drawable.ic_edit, "编辑首页布局") { openHomeLayoutEditor() }
        else -> TopAction(R.drawable.ic_refresh, "刷新") { onRefresh(id) }
    }

    override fun identity(): Identity? {
        val si = bootstrap?.serverInfo
        val name = si?.name?.takeIf { it.isNotBlank() } ?: server.name.ifEmpty { server.baseUrl }
        val sub = server.baseUrl + (if (server.branch != "default" && server.branch.isNotEmpty()) " / " + server.branch else "")
        val tag = if (server.uid.isNotEmpty()) "UID: " + server.uid else "已登录"
        return Identity(name.take(1).uppercase(), name, sub, tag)
    }

    override fun recents(): MutableList<String> = recents

    override fun onToggleDarkMode() {
        val store = App.of(this).store
        val cur = store.darkMode(server.id)
        store.saveDarkMode(server.id, if (cur == "dark") "light" else "dark")
                                      
        rebuildShell(true)
    }

    override fun onSwitchServer() {
        confirm("切换服务器", "返回服务器列表？当前账号信息会保留，可重新进入。") { finish() }
    }

    private var childIsPlugin = false

    override fun onShellRebuilt() {
                                                            
        controllers.clear()
    }

    override fun onChildOpened() {
        childIsPlugin = false
        controllers.remove("child")
    }

    override fun onChildBack(): Boolean {
        if (!childIsPlugin) return false
        val ctl = controllers["child"] ?: return false
        return ctl.back()
    }

    override fun banner(): View? {
        val ud = bootstrap?.userDebug ?: return null
        if (ud.forUid != server.uid && server.uid.isNotEmpty()) return null
        return DemoKit.txt(this, theme, getString(R.string.debug_banner), 12.5f, true, "onError").apply {
            setBackgroundColor(theme.color(this@UserMainActivity, "error"))
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
    }

                                                      

    private fun apiBase(): String {
        val pfx = if (server.branch == "default" || server.branch.isEmpty()) "" else "/" + server.branch
        return server.baseUrl + pfx
    }

    private fun JsonObject.arr(k: String): JsonArray? = get(k)?.takeIf { it.isJsonArray }?.asJsonArray
    private fun JsonObject.obj(k: String): JsonObject? = get(k)?.takeIf { it.isJsonObject }?.asJsonObject
    private fun JsonObject.str(k: String): String = get(k)?.takeIf { !it.isJsonNull }?.asString ?: ""

    private fun copyIt(label: String, text: String) {
        DemoKit.copy(this, label, text)
        tip(getString(R.string.copied))
    }

                         
    private fun <T> fill(box: LinearLayout, fetch: () -> T, render: (LinearLayout, T) -> Unit) {
        box.removeAllViews()
        DemoKit.put(box, DemoKit.txt(this, theme, "读取中…", 13f, false, "onSurfaceVariant"))
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val data = fetch()
                withContext(Dispatchers.Main) {
                    box.removeAllViews()
                    render(box, data)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    box.removeAllViews()
                    DemoKit.put(box, DemoKit.txt(this@UserMainActivity, theme, "读取失败: " + (e.message ?: "未知错误"), 13f, false, "error"))
                }
            }
        }
    }

                                                      

    private fun homePage(): View {
        val col = DemoKit.pageColumn(this, theme)
        DemoKit.put(col, serverCard(), 2)

        val metricBox = DemoKit.box(this)
        DemoKit.put(col, metricBox, 18)
        fill(metricBox, { Api.get(server.baseUrl + "/credits", Api.authHeaders(server)) }) { host, r ->
            val unlimited = r.get("unlimited")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
            val quota = r.str("quotaTokens")
            val used = r.str("usedTokens")
            val remain = if (unlimited) "∞" else r.str("remainingTokens")
            DemoKit.put(host, DemoKit.sectionTitle(this, theme, "额度概览", "来自服务端 /credits"))
            DemoKit.put(
                host,
                DemoKit.metricRow(
                    this,
                    DemoKit.metricTile(this, theme, fmt(quota), "总额度", "◈") { showPage(P_USAGE) },
                    DemoKit.metricTile(this, theme, fmt(used), "已用", "↻") { showPage(P_USAGE) },
                    DemoKit.metricTile(this, theme, fmt(remain), "剩余", "◷") { showPage(P_USAGE) },
                ),
                10,
            )
        }

                  
        DemoKit.put(col, DemoKit.sectionTitle(this, theme, "快速入口", "常用操作"), 20)
        val quick = LinearLayout(this).apply { gravity = Gravity.CENTER }
        quick.addView(DemoKit.actionTile(this, theme, "模型广场", R.drawable.ic_apps) { showPage(modelPageId()) }, LinearLayout.LayoutParams(0, dp(88), 1f))
        quick.addView(DemoKit.actionTile(this, theme, "插件中心", R.drawable.ic_star) { showPage(P_PLUGINS) }, LinearLayout.LayoutParams(0, dp(88), 1f).apply { marginStart = dp(8) })
        quick.addView(DemoKit.actionTile(this, theme, "个人中心", R.drawable.ic_person) { showPage(P_PERSONAL) }, LinearLayout.LayoutParams(0, dp(88), 1f).apply { marginStart = dp(8) })
        DemoKit.put(col, quick, 10)

                              
        val b = bootstrap
        if (b != null) {
            val order = LayoutMerger.mergeHomeOrder(b.layout, App.of(this).store.localLayout(server.id), b)
            for (pid in order) {
                val p = b.pluginById(pid) ?: continue
                val home = p.appUi?.home ?: continue
                DemoKit.put(col, DemoKit.sectionTitle(this, theme, home.title.ifEmpty { p.name }, "来自插件 " + p.id), 20)
                val card = DemoKit.panel(this, theme, 16)
                val ctl = controllers.getOrPut("home:" + pid) {
                    SduiController(this, this, server, pid, theme, onOpenOverlay = { p2, path, t -> openPluginChild(p2, path, t) })
                }
                ctl.loadInto(card, home.ui)
                DemoKit.put(col, card, 10)
            }
        }

                
        val notices = myNotices()
        if (notices.isNotEmpty()) {
            val latest = notices.last()
            DemoKit.put(col, DemoKit.sectionTitle(this, theme, "公告", "共 " + notices.size + " 条 · 点卡片看全部"), 20)
            val card = DemoKit.panel(this, theme, 16, ripple = true).apply { setOnClickListener { showPage(P_NOTICE) } }
            val meta = ArrayList<String>()
            if (latest.time.isNotEmpty()) meta.add(latest.time)
            if (latest.target.isNotEmpty()) meta.add("发给你的通知")
            if (meta.isNotEmpty()) DemoKit.put(card, DemoKit.txt(this, theme, meta.joinToString("  ·  "), 10.5f, true, if (latest.target.isNotEmpty()) "primary" else "onSurfaceVariant"))
            DemoKit.put(card, DemoKit.txt(this, theme, latest.body, 13f, false, "onSurface"), if (meta.isEmpty()) 0 else 7)
            DemoKit.put(col, card, 10)
        }
        return pageScroll(col)
    }

    private fun serverCard(): View {
        val card = DemoKit.gradientPanel(this, theme, 16)
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(DemoKit.iconBadge(this, theme, R.drawable.ic_home, null, 46), LinearLayout.LayoutParams(dp(46), dp(46)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val si = bootstrap?.serverInfo
        DemoKit.put(info, DemoKit.txt(this, theme, "服务点", 11f, false, "onSurfaceVariant"))
        DemoKit.put(info, DemoKit.txt(this, theme, si?.name?.takeIf { it.isNotBlank() } ?: server.name.ifEmpty { "未命名服务点" }, 19f, true), 3)
        DemoKit.put(info, DemoKit.txt(this, theme, "● 已连接  " + apiBase(), 11.5f, true, "success"), 5)
        row.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(14) })
        DemoKit.put(card, row)
        si?.description?.takeIf { it.isNotBlank() }?.let {
            DemoKit.put(card, DemoKit.txt(this, theme, it, 12.5f, false, "onSurfaceVariant"), 12)
        }
        return card
    }

                                                        

    private fun modelsPage(): View {
        val col = DemoKit.pageColumn(this, theme)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(col, box)
        val cached = modelCache
        if (cached != null) renderModels(box, cached) else fill(box, {
            val r = Api.get(apiBase() + "/v1/models", Api.authHeaders(server))
            r.arr("data")?.mapNotNull { o ->
                o.takeIf { it.isJsonObject }?.asJsonObject?.str("id")?.takeIf { it.isNotBlank() }
            } ?: emptyList()
        }) { _, all ->
            modelCache = all
            renderModels(box, all)
        }
        return pageScroll(col)
    }

    private var modelListHost: LinearLayout? = null
    private var modelAll: List<String> = emptyList()

    private fun renderModels(box: LinearLayout, all: List<String>) {
        box.removeAllViews()
        modelAll = all
        val bar = DemoKit.searchBar(this, theme, "搜索模型名称…", onQuery = { q ->
            modelQuery = q
            redrawModels()
        })
        bar.input.setText(modelQuery)
        DemoKit.put(box, bar.view, 2)
        val tools = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        tools.addView(DemoKit.chip(this, theme, if (modelGrid) "列表" else "网格", R.drawable.ic_grid, modelGrid) {
            modelGrid = !modelGrid
            redrawModels()
        })
        tools.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        tools.addView(DemoKit.txt(this, theme, "共 " + all.size + " 个模型", 11f, false, "onSurfaceVariant"))
        DemoKit.put(box, tools, 12)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        modelListHost = list
        DemoKit.put(box, list)
        redrawModels()
    }

    private fun redrawModels() {
        val list = modelListHost ?: return
        val all = modelAll
        list.removeAllViews()
        val q = modelQuery.trim()
        val filtered = if (q.isEmpty()) all else all.filter { it.contains(q, true) }
        if (filtered.isEmpty()) {
            DemoKit.put(list, DemoKit.txt(this, theme, "没有匹配的模型", 13f, false, "onSurfaceVariant"))
            return
        }
        val shown = filtered.take(80)
        if (modelGrid) {
            var i = 0
            while (i < shown.size) {
                val row = LinearLayout(this).apply { gravity = Gravity.TOP }
                for (c in 0 until 2) {
                    if (i >= shown.size) break
                    val m = shown[i]
                    val card = DemoKit.panel(this, theme, 14, ripple = true).apply {
                        isClickable = true
                        isFocusable = true
                        setOnClickListener { copyIt("model", m) }
                    }
                    DemoKit.put(card, DemoKit.iconBadge(this, theme, R.drawable.ic_apps, null, 36), 0, dp(36), dp(36))
                    DemoKit.put(card, DemoKit.txt(this, theme, m, 12.5f, true), 10)
                    DemoKit.put(card, DemoKit.txt(this, theme, "点按复制名称", 10.5f, false, "onSurfaceVariant"), 4)
                    row.addView(card, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { if (c == 1) marginStart = dp(8) })
                    i++
                }
                DemoKit.put(list, row, 10)
            }
        } else {
            for (m in shown) {
                val card = DemoKit.panel(this, theme, 16)
                val line = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
                line.addView(DemoKit.avatar(this, m.take(1).uppercase(), theme.color(this, "primary"), 38), LinearLayout.LayoutParams(dp(38), dp(38)))
                line.addView(DemoKit.txt(this, theme, m, 14f, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })
                line.addView(DemoKit.chip(this, theme, "复制", R.drawable.ic_copy) { copyIt("model", m) })
                DemoKit.put(card, line)
                DemoKit.put(list, card, 8)
            }
        }
        if (filtered.size > shown.size) {
            DemoKit.put(list, DemoKit.txt(this, theme, "… 共 " + filtered.size + " 个，仅显示前 " + shown.size, 11.5f, false, "onSurfaceVariant"), 10)
        }
    }

                                                        

    private fun pluginsPage(): View {
        val col = DemoKit.pageColumn(this, theme)
        val all = bootstrap?.plugins ?: emptyList()
        val bar = DemoKit.searchBar(this, theme, "搜索插件…", onQuery = { q ->
            pluginQuery = q
            rebuildPluginListInto(all)
        })
        DemoKit.put(col, bar.view, 2)
        DemoKit.put(col, DemoKit.segmented(this, theme, listOf("全部", "内置", "外部"), pluginTab) { pluginTab = it; rebuildPluginListInto(all) }, 12)
        val listHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        pluginListHost = listHost
        DemoKit.put(col, listHost)
        rebuildPluginListInto(all)
        return pageScroll(col)
    }

    private var pluginListHost: LinearLayout? = null

    private fun rebuildPluginListInto(all: List<com.gaycore.app.data.PluginInfo>) {
        val host = pluginListHost ?: return
        host.removeAllViews()
        val q = pluginQuery.trim()
        val filtered = all.filter { p ->
            val tabOk = when (pluginTab) {
                1 -> p.builtin
                2 -> !p.builtin
                else -> true
            }
            tabOk && (q.isEmpty() || p.name.contains(q, true) || p.id.contains(q, true) || p.description.contains(q, true))
        }
        if (filtered.isEmpty()) {
            DemoKit.put(host, DemoKit.txt(this, theme, "没有匹配的插件", 13f, false, "onSurfaceVariant"), 12)
            return
        }
        for (p in filtered) {
            val card = DemoKit.panel(this, theme, 16)
            val line = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            line.addView(
                DemoKit.iconBadge(this, theme, DemoKit.iconRes(p.icon), null, 44),
                LinearLayout.LayoutParams(dp(44), dp(44)),
            )
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            DemoKit.put(info, DemoKit.txt(this, theme, p.name, 15f, true))
            DemoKit.put(info, DemoKit.txt(this, theme, p.description.ifBlank { p.id }, 11.5f, false, "onSurfaceVariant"), 3)
            line.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })
            line.addView(DemoKit.badge(this, theme, if (p.builtin) "内置" else "外部", if (p.builtin) "primary" else "neutral"))
            DemoKit.put(card, line)

            val foot = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            foot.addView(DemoKit.txt(this, theme, "v" + p.version + " · SHA256 已校验", 10.5f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val page = p.appUi?.pages?.firstOrNull() ?: p.appUi?.personal ?: p.appUi?.home ?: p.appUi?.bottomBar
            if (page != null) {
                foot.addView(DemoKit.chip(this, theme, "打开", R.drawable.ic_chevron_right) {
                    openPluginChild(p.id, page.ui, page.title.ifEmpty { p.name })
                })
            }
            foot.addView(DemoKit.chip(this, theme, "详情", R.drawable.ic_info, false) {
                UiKit.infoDialog(
                    this, theme, p.name + " v" + p.version,
                    (p.description.ifBlank { "无描述" }) +
                        "\n\nID: " + p.id +
                        "\n作者: " + p.author.ifBlank { "未知" } +
                        "\n权限: " + (if (p.permissions.isEmpty()) "无" else p.permissions.joinToString(", ")) +
                        "\nSHA256: " + p.sha256.take(32) + "…",
                )
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(8) })
            DemoKit.put(card, foot, 12)
            DemoKit.put(host, card, 8)
        }
    }

    private fun pluginTabPage(pid: String): View {
        val p = bootstrap?.pluginById(pid)
        val bb = p?.appUi?.bottomBar
        if (p == null || bb == null) return missingPage(pid)
        val col = DemoKit.pageColumn(this, theme)
        val card = DemoKit.panel(this, theme, 16)
        val ctl = controllers.getOrPut("tab:" + pid) {
            SduiController(this, this, server, pid, theme, onOpenOverlay = { p2, path, t -> openPluginChild(p2, path, t) })
        }
        ctl.loadInto(card, bb.ui)
        DemoKit.put(col, card)
        return pageScroll(col)
    }

    private fun missingPage(id: String): View {
        val col = DemoKit.pageColumn(this, theme)
        val card = DemoKit.panel(this, theme, 16)
        DemoKit.put(card, DemoKit.txt(this, theme, "插件已下线: " + id, 14f, true))
        DemoKit.put(card, DemoKit.txt(this, theme, "该入口由插件提供，插件已被服务端停用。", 12.5f, false, "onSurfaceVariant"), 6)
        DemoKit.put(col, card)
        return pageScroll(col)
    }

    private fun openPluginChild(pid: String, uiPath: String, title: String) {
        openChild(title) { host ->
            val card = DemoKit.panel(this, theme, 16)
            val ctl = SduiController(this, this, server, pid, theme)
            controllers["child"] = ctl
            childIsPlugin = true
            ctl.loadInto(card, uiPath)
            DemoKit.put(host, card)
        }
    }

                                                      

    private fun usagePage(): View {
        val col = DemoKit.pageColumn(this, theme)
        val box = DemoKit.box(this)
        DemoKit.put(col, box, 2)
        fill(box, { Api.get(server.baseUrl + "/credits", Api.authHeaders(server)) }) { host, r ->
            val unlimited = r.get("unlimited")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
            val quota = r.get("quotaTokens")?.takeIf { !it.isJsonNull }?.asLong ?: 0L
            val used = r.get("usedTokens")?.takeIf { !it.isJsonNull }?.asLong ?: 0L
            val remain = r.get("remainingTokens")?.takeIf { !it.isJsonNull }?.asLong ?: 0L
            val name = r.str("name").ifEmpty { "—" }
            val uid = r.str("uid")
            DemoKit.put(host, DemoKit.sectionTitle(this, theme, "使用情况", (if (uid.isNotEmpty()) "UID " + uid + " · " else "") + name))
            DemoKit.put(
                host,
                DemoKit.metricRow(
                    this,
                    DemoKit.metricTile(this, theme, if (unlimited) "∞" else fmtTok(quota), "总额度", "◈"),
                    DemoKit.metricTile(this, theme, fmtTok(used), "已用 tokens", "↻"),
                    DemoKit.metricTile(this, theme, if (unlimited) "∞" else fmtTok(remain), "剩余", "◷"),
                ),
                12,
            )
            val card = DemoKit.panel(this, theme, 16)
            DemoKit.put(card, DemoKit.txt(this, theme, "额度进度", 16f, true))
            if (unlimited) {
                DemoKit.put(card, DemoKit.txt(this, theme, "该卡为不限额度，仅统计用量。", 12.5f, false, "onSurfaceVariant"), 6)
                DemoKit.put(card, DemoKit.progressRow(this, theme, "已用", used, (used * 2).coerceAtLeast(1), fmtTok(used)), 14)
            } else if (quota <= 0) {
                DemoKit.put(card, DemoKit.txt(this, theme, "当前卡密为零额度，无法发起请求，请联系管理员充值。", 12.5f, false, "error"), 6)
            } else {
                val pct = (used.toDouble() / quota.toDouble() * 100).roundToInt()
                DemoKit.put(card, DemoKit.progressRow(this, theme, "已用 / 总额度", used, quota, pct.toString() + "%"), 14)
            }
            DemoKit.put(host, card, 12)

            val info = DemoKit.panel(this, theme, 16)
            DemoKit.put(info, DemoKit.txt(this, theme, "账号信息", 16f, true))
            DemoKit.put(info, DemoKit.valueRow(this, theme, "到期", r.str("expiresAt").ifEmpty { "长期有效" }), 12)
            DemoKit.put(info, DemoKit.valueRow(this, theme, "卡名", name), 10)
            DemoKit.put(host, info, 12)

            val models = r.arr("models")
            if (models != null && models.size() > 0) {
                DemoKit.put(host, DemoKit.sectionTitle(this, theme, "允许模型", "共 " + models.size() + " 个"), 20)
                val card2 = DemoKit.panel(this, theme, 16)
                val chips = (0 until models.size()).mapNotNull { i ->
                    models.get(i)?.takeIf { !it.isJsonNull }?.asString?.let { m ->
                        DemoKit.chip(this, theme, m, null, false) { copyIt("model", m) }
                    }
                }
                DemoKit.put(card2, DemoKit.chipWrap(chips))
                DemoKit.put(host, card2, 10)
            } else {
                DemoKit.put(host, DemoKit.sectionTitle(this, theme, "允许模型", "该卡可用全部模型"), 20)
            }
        }
        return pageScroll(col)
    }

                                                        

    private fun personalPage(): View {
        val col = DemoKit.pageColumn(this, theme)
        val usp = bootstrap?.userSystemPlugin()
        if (usp == null || usp.appUi?.personal == null) {
            val card = DemoKit.panel(this, theme, 16)
            DemoKit.put(card, DemoKit.txt(this, theme, getString(R.string.personal_missing), 13.5f, false, "onSurfaceVariant"))
            DemoKit.put(col, card, 2)
            return pageScroll(col)
        }
        val card = DemoKit.panel(this, theme, 16)
        val ctl = controllers.getOrPut("personal") {
            SduiController(this, this, server, usp.id, theme, inlineNav = true)
        }
        ctl.loadInto(card, usp.appUi!!.personal!!.ui)
        DemoKit.put(col, card, 2)
        return pageScroll(col)
    }

                                                      

                                           
    private fun myNotices(): List<Notices.Item> =
        Notices.visibleTo(Notices.parse(bootstrap?.serverInfo?.announcement), server.uid)

    private fun noticePage(): View {
        val col = DemoKit.pageColumn(this, theme)
        val si = bootstrap?.serverInfo
        val list = myNotices()
        if (list.isNotEmpty()) {
            DemoKit.put(col, DemoKit.sectionTitle(this, theme, "服务点公告", "共 " + list.size + " 条"), 2)
            val cards = ArrayList<View>()
            list.forEach { e ->
                val card = DemoKit.panel(this, theme, 16)
                val meta = ArrayList<String>()
                if (e.time.isNotEmpty()) meta.add(e.time)
                if (e.target.isNotEmpty()) meta.add("发给你的通知")
                if (meta.isNotEmpty()) DemoKit.put(card, DemoKit.txt(this, theme, meta.joinToString("  ·  "), 10.5f, true, if (e.target.isNotEmpty()) "primary" else "onSurfaceVariant"))
                DemoKit.put(card, DemoKit.txt(this, theme, e.body, 14f, true), if (meta.isEmpty()) 0 else 7)
                DemoKit.put(col, card, 8)
                cards.add(card)
            }
            animateInStaggered(cards, 26)
        } else {
            val card = DemoKit.panel(this, theme, 16)
            DemoKit.put(card, DemoKit.txt(this, theme, "服务点暂无公告。", 13f, false, "onSurfaceVariant"))
            DemoKit.put(col, card, 2)
        }
        val info = DemoKit.panel(this, theme, 16)
        DemoKit.put(info, DemoKit.txt(this, theme, "联系方式", 16f, true))
        var has = false
        si?.contact?.takeIf { it.isNotBlank() }?.let {
            DemoKit.put(info, DemoKit.valueRow(this, theme, "联系", it), 12); has = true
        }
        si?.website?.takeIf { it.isNotBlank() }?.let { url ->
            has = true
            val row = DemoKit.valueRow(this, theme, "官网", url)
            if (url.startsWith("http")) {
                row.setOnClickListener {
                    try { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
                    catch (_: Exception) { tip("无法打开链接") }
                }
            }
            DemoKit.put(info, row, 12)
        }
        DemoKit.put(info, DemoKit.valueRow(this, theme, "地址", server.baseUrl), 12)
        if (!has) DemoKit.put(info, DemoKit.txt(this, theme, "服务点未填写联系方式。", 12.5f, false, "onSurfaceVariant"), 12)
        DemoKit.put(col, info, 14)
        return pageScroll(col)
    }

                                                      

    private fun themePage(): View {
        val store = App.of(this).store
        val col = DemoKit.pageColumn(this, theme)
        DemoKit.put(col, DemoKit.sectionTitle(this, theme, "主题与配色", "选择服务点提供的主题"), 2)

        val mode = DemoKit.panel(this, theme, 16)
        DemoKit.put(mode, DemoKit.txt(this, theme, "显示模式", 16f, true))
        val modes = listOf("跟随系统" to "system", "深色" to "dark", "浅色" to "light")
        val cur = modes.indexOfFirst { it.second == store.darkMode(server.id) }.coerceAtLeast(0)
        DemoKit.put(mode, DemoKit.segmented(this, theme, modes.map { it.first }, cur) { i ->
            store.saveDarkMode(server.id, modes[i].second)
            rebuildShell(true)
        }, 12)
        DemoKit.put(col, mode, 12)

        val themes = bootstrap?.themes ?: emptyList()
        if (themes.isNotEmpty()) {
            DemoKit.put(col, DemoKit.sectionTitle(this, theme, "配色方案", "共 " + themes.size + " 套"), 20)
            val cardHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val curId = store.themeId(server.id) ?: bootstrap?.layout?.theme ?: "theme-md3"
            themes.forEach { t: ThemeInfo ->
                val card = DemoKit.panel(this, theme, 16, ripple = true).apply {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        store.saveThemeId(server.id, t.id)
                        rebuildShell(true)
                    }
                }
                val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
                row.addView(DemoKit.avatar(this, t.name.take(1), theme.color(this, "primary"), 34), LinearLayout.LayoutParams(dp(34), dp(34)))
                row.addView(DemoKit.txt(this, theme, t.name, 14f, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })
                if (t.id == curId) row.addView(DemoKit.badge(this, theme, "使用中", "success"))
                else if (t.locked) row.addView(DemoKit.badge(this, theme, "默认", "neutral"))
                DemoKit.put(card, row)
                DemoKit.put(cardHost, card, 8)
            }
            DemoKit.put(col, cardHost)
        }

        if (android.os.Build.VERSION.SDK_INT >= 31) {
            DemoKit.put(col, DemoKit.sectionTitle(this, theme, "动态取色", "Material You"), 20)
            val card = DemoKit.panel(this, theme, 16)
            DemoKit.put(
                card,
                DemoKit.switchRow(this, theme, "从壁纸取色", "自动提取壁纸主色调，覆盖主题配色", store.dynamicColors(server.id)) { on ->
                    store.saveDynamicColors(server.id, on)
                    rebuildShell(true)
                },
            )
            DemoKit.put(col, card, 10)
        }

                        
        val themePlugins = (bootstrap?.plugins ?: emptyList()).filter { it.type == "theme" }
        if (themePlugins.isNotEmpty()) {
            DemoKit.put(col, DemoKit.sectionTitle(this, theme, "主题插件", "由插件提供更多外观"), 20)
            for (p in themePlugins) {
                val card = DemoKit.menuCard(this, theme, p.name, p.description.ifBlank { "打开主题设置" }, DemoKit.iconRes(p.icon)) {
                    openThemePlugin(p)
                }
                DemoKit.put(col, card, 8)
            }
        }
        return pageScroll(col)
    }

    private fun openThemePlugin(p: com.gaycore.app.data.PluginInfo) {
        val ui = p.appUi?.settings?.ui ?: p.appUi?.pages?.firstOrNull()?.ui
        if (ui == null) {
            tip("该主题插件没有提供设置页")
            return
        }
        openPluginChild(p.id, ui, p.name)
    }

                                                      

    private fun settingsPage(): View {
        val col = DemoKit.pageColumn(this, theme)
        val b = bootstrap
        DemoKit.put(col, DemoKit.sectionTitle(this, theme, "设置", "账号、站点与关于"), 2)

        val rows = listOf(
            DemoKit.settingsRow(this, theme, R.drawable.ic_person, "个人资料", "昵称 / 头像 / 卡密 / 改密码") { openChild("个人资料") { buildProfilePage(it) } },
            DemoKit.settingsRow(this, theme, R.drawable.ic_info, "站点信息", "地址 / 联系方式" + if ((b?.branches?.size ?: 0) > 1) " / 切换分支" else "") { openChild("站点信息") { buildSitePage(it) } },
            DemoKit.settingsRow(this, theme, R.drawable.ic_star, "插件管理", "共 " + (b?.plugins?.size ?: 0) + " 个插件") { openChild("插件管理") { buildPluginManagePage(it) } },
            DemoKit.settingsRow(this, theme, R.drawable.ic_edit, "首页组件布局", "调整首页插件的顺序与显隐") { openChild("首页组件布局") { buildWidgetLayoutPage(it) } },
            DemoKit.settingsRow(this, theme, R.drawable.ic_bug, "漏洞报告", "生成诊断信息并反馈") { openChild("漏洞报告") { buildReportPage(it) } },
            DemoKit.settingsRow(this, theme, R.drawable.ic_settings, "关于", "版本 / 构建信息") { openChild("关于") { buildAboutPage(it) } },
            DemoKit.settingsRow(this, theme, R.drawable.ic_block, "免责声明", "使用须知与风险提示", danger = true) { openChild("免责声明") { buildDisclaimerPage(it) } },
        )
        DemoKit.put(col, DemoKit.settingsGroup(this, theme, rows), 12)

        DemoKit.put(
            col,
            DemoKit.button(this, theme, "切换账号", "outlined") {
                confirm("切换账号", "确定退出当前账号？服务器信息会保留，可重新登录。") {
                    App.of(this).store.get(server.id)?.let { e ->
                        e.tokenEnc = null
                        e.uid = ""
                        App.of(this).store.save(e)
                    }
                    finish()
                }
            },
            18,
        )
        return pageScroll(col)
    }

                                                   
    private fun buildProfilePage(host: LinearLayout) {
        ProfilePage.build(
            this, theme, server, host, asAdmin = false,
            onLogout = {
                confirm("切换账号", "确定退出当前账号？服务器信息会保留，可重新登录。") {
                    App.of(this).store.get(server.id)?.let { e ->
                        e.tokenEnc = null
                        e.uid = ""
                        App.of(this).store.save(e)
                    }
                    finish()
                }
            },
            onOpenSiteInfo = { openChild("站点信息") { buildSitePage(it) } },
        )
    }

    private fun buildSitePage(host: LinearLayout) {
        val si = bootstrap?.serverInfo
        val card = DemoKit.panel(this, theme, 16)
        DemoKit.put(card, DemoKit.txt(this, theme, "站点信息", 16f, true))
        DemoKit.put(card, DemoKit.valueRow(this, theme, "地址", server.baseUrl), 12)
        DemoKit.put(card, DemoKit.valueRow(this, theme, "分支", server.branch), 10)
        DemoKit.put(card, DemoKit.valueRow(this, theme, "核心", bootstrap?.core ?: "—"), 10)
                                                  
        val b0 = bootstrap
        val ptabs = if (b0 == null) emptyList() else LayoutMerger.mergeTabs(b0.layout, App.of(this).store.localLayout(server.id), b0)
            .filterIsInstance<TabItem.Plugin>()
        DemoKit.put(card, DemoKit.valueRow(this, theme, "插件", (b0?.plugins?.size ?: 0).toString() + " 个"), 10)
        DemoKit.put(
            card,
            DemoKit.valueRow(this, theme, "侧边栏插件页", if (ptabs.isEmpty()) "无" else ptabs.joinToString(", ") { it.title }),
            10,
        )
        if (b0 != null && ptabs.isEmpty() && (b0.plugins.any { it.appUi?.bottomBar != null })) {
            DemoKit.put(
                card,
                DemoKit.txt(this, theme, "服务端说有插件页，但当前布局里没排上 —— 点右上角刷新重新同步一次。", 11.5f, false, "warning"),
                8,
            )
        }
        si?.contact?.takeIf { it.isNotBlank() }?.let { DemoKit.put(card, DemoKit.valueRow(this, theme, "联系", it), 10) }
        si?.website?.takeIf { it.isNotBlank() }?.let { DemoKit.put(card, DemoKit.valueRow(this, theme, "官网", it), 10) }
        DemoKit.put(host, card, 2)

        val branches = bootstrap?.branches ?: emptyList()
        if (branches.size > 1) {
            val card2 = DemoKit.panel(this, theme, 16)
            DemoKit.put(card2, DemoKit.sectionTitle(this, theme, "切换分支", "同一服务点的不同线路"))
            val chips = branches.map { br ->
                DemoKit.chip(this, theme, br.name, null, br.name == server.branch) {
                    App.of(this).store.get(server.id)?.let { e ->
                        e.branch = br.name
                        App.of(this).store.save(e)
                    }
                    tip("已切换到 " + br.name)
                    recreate()
                }
            }
            DemoKit.put(card2, DemoKit.chipWrap(chips), 12)
            DemoKit.put(host, card2, 14)
        }
    }

    private fun buildPluginManagePage(host: LinearLayout) {
        val plugins = bootstrap?.plugins ?: emptyList()
        if (plugins.isEmpty()) {
            val card = DemoKit.panel(this, theme, 16)
            DemoKit.put(card, DemoKit.txt(this, theme, "该服务点未启用插件。", 13f, false, "onSurfaceVariant"))
            DemoKit.put(host, card, 2)
            return
        }
        for (p in plugins) {
            val card = DemoKit.panel(this, theme, 16)
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(DemoKit.iconBadge(this, theme, DemoKit.iconRes(p.icon), null, 40), LinearLayout.LayoutParams(dp(40), dp(40)))
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            DemoKit.put(info, DemoKit.txt(this, theme, p.name, 14.5f, true))
            DemoKit.put(info, DemoKit.txt(this, theme, "v" + p.version + (if (p.builtin) " · 内置" else " · 外部"), 11.5f, false, "onSurfaceVariant"), 3)
            row.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10) })
            DemoKit.put(card, row)
            if (p.description.isNotBlank()) {
                DemoKit.put(card, DemoKit.txt(this, theme, p.description, 12f, false, "onSurfaceVariant"), 8)
            }
            val page = p.appUi?.pages?.firstOrNull() ?: p.appUi?.personal ?: p.appUi?.home ?: p.appUi?.bottomBar
            if (page != null) {
                DemoKit.put(card, DemoKit.chip(this, theme, "打开页面", R.drawable.ic_chevron_right) {
                    openPluginChild(p.id, page.ui, page.title.ifEmpty { p.name })
                }, 12)
            }
            DemoKit.put(host, card, 8)
        }
    }

    private fun buildWidgetLayoutPage(host: LinearLayout) {
        val b = bootstrap ?: return
        val widgets = b.allWidgets()
        if (widgets.isEmpty()) {
            val card = DemoKit.panel(this, theme, 16)
            DemoKit.put(card, DemoKit.txt(this, theme, "该服务点的插件没有声明首页组件。", 13f, false, "onSurfaceVariant"))
            DemoKit.put(host, card, 2)
            return
        }
        val store = App.of(this).store
        val allIds = widgets.map { it.first + ":" + it.second.id }
        val hidden = store.widgetHidden(server.id).toMutableList()
        val order = ((store.widgetOrder(server.id)?.filter { it in allIds } ?: emptyList()) + allIds.filter { it !in (store.widgetOrder(server.id) ?: emptyList()) }).toMutableList()

        fun titleOf(id: String): String {
            val w = widgets.firstOrNull { it.first + ":" + it.second.id == id } ?: return id
            return w.second.title.ifEmpty { w.second.id }
        }

        val card = DemoKit.panel(this, theme, 16)
        DemoKit.put(card, DemoKit.txt(this, theme, "调整顺序与显隐", 16f, true))
        DemoKit.put(card, DemoKit.txt(this, theme, "用 ▲ ▼ 调序，右侧开关控制显示。保存后首页立即生效。", 12f, false, "onSurfaceVariant"), 6)
        val rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(card, rows, 12)

        lateinit var rebuild: () -> Unit
        rebuild = {
            rows.removeAllViews()
            order.forEachIndexed { idx, id ->
                val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
                val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                DemoKit.put(info, DemoKit.txt(this, theme, titleOf(id), 13.5f, true))
                DemoKit.put(info, DemoKit.txt(this, theme, if (id in hidden) "已隐藏" else "显示中", 11f, false, "onSurfaceVariant"), 2)
                row.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                val up = DemoKit.iconButton(this, theme, R.drawable.ic_chevron_up, "上移") {
                    if (idx > 0) {
                        val it0 = order.removeAt(idx)
                        order.add(idx - 1, it0)
                        rebuild()
                    }
                }
                up.alpha = if (idx == 0) 0.3f else 1f
                up.isEnabled = idx > 0
                row.addView(up, LinearLayout.LayoutParams(dp(32), dp(32)))
                val down = DemoKit.iconButton(this, theme, R.drawable.ic_chevron_down, "下移") {
                    if (idx < order.size - 1) {
                        val it0 = order.removeAt(idx)
                        order.add(idx + 1, it0)
                        rebuild()
                    }
                }
                down.alpha = if (idx == order.size - 1) 0.3f else 1f
                down.isEnabled = idx < order.size - 1
                row.addView(down, LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(4) })
                val sw = com.google.android.material.materialswitch.MaterialSwitch(this).apply {
                    isChecked = id !in hidden
                    setOnCheckedChangeListener { _, checked ->
                        if (checked) hidden.remove(id) else if (id !in hidden) hidden.add(id)
                    }
                }
                row.addView(sw, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(6) })
                DemoKit.put(rows, row, 10)
            }
        }
        rebuild()
        DemoKit.put(host, card, 2)

        val acts = LinearLayout(this)
        acts.addView(DemoKit.button(this, theme, "全部显示", "outlined") { hidden.clear(); rebuild() }, LinearLayout.LayoutParams(0, dp(40), 1f))
        acts.addView(
            DemoKit.button(this, theme, "保存", "filled") {
                store.saveWidgetLayout(server.id, order.toList(), hidden.toList())
                tip(getString(R.string.layout_saved))
                closeChild()
                showPage(P_HOME, Nav.NONE)
            },
            LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginStart = dp(10) },
        )
        DemoKit.put(host, acts, 14)
    }

                           
    private fun openHomeLayoutEditor() {
        openChild("首页组件布局") { buildWidgetLayoutPage(it) }
    }

    private fun buildReportPage(host: LinearLayout) {
        val card = DemoKit.panel(this, theme, 16)
        DemoKit.put(card, DemoKit.txt(this, theme, "诊断信息", 16f, true))
        val info = buildString {
            append("App      ").append(com.gaycore.app.BuildConfig.VERSION_NAME)
            append("\nAndroid  ").append(android.os.Build.VERSION.RELEASE).append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")")
            append("\n机型     ").append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL)
            append("\n服务端   ").append(server.baseUrl)
            append("\n分支     ").append(server.branch)
            append("\n核心     ").append(bootstrap?.core ?: "?")
        }
        val tv = DemoKit.txt(this, theme, info, 12.5f, false, "onSurfaceVariant").apply { setTextIsSelectable(true) }
        DemoKit.put(card, tv, 12)
        DemoKit.put(card, DemoKit.button(this, theme, "复制诊断信息", "tonal") { copyIt("diag", info) }, 14)
        DemoKit.put(host, card, 2)

        val card2 = DemoKit.panel(this, theme, 16)
        DemoKit.put(card2, DemoKit.txt(this, theme, "怎么反馈", 16f, true))
        DemoKit.put(
            card2,
            DemoKit.txt(this, theme, "1. 复制上面的诊断信息\n2. 连同问题截图 / 复现步骤一起发给服务点管理员\n3. 联系方式见「设置 → 站点信息」", 12.5f, false, "onSurfaceVariant"),
            8,
        )
        DemoKit.put(host, card2, 14)
    }

    private fun buildAboutPage(host: LinearLayout) {
        val card = DemoKit.panel(this, theme, 16)
        DemoKit.put(card, DemoKit.txt(this, theme, "Gay Core", 16f, true))
        DemoKit.put(card, DemoKit.txt(this, theme, "Material 3 原生界面 · 服务端驱动 UI", 12.5f, false, "onSurfaceVariant"), 4)
        DemoKit.put(card, DemoKit.valueRow(this, theme, "版本", com.gaycore.app.BuildConfig.VERSION_NAME), 12)
        DemoKit.put(card, DemoKit.valueRow(this, theme, "核心", bootstrap?.core ?: "—"), 10)
        DemoKit.put(card, DemoKit.valueRow(this, theme, "App API", (bootstrap?.appApi ?: 0).toString()), 10)
        DemoKit.put(card, DemoKit.valueRow(this, theme, "服务点", server.baseUrl), 10)
        DemoKit.put(host, card, 2)
        DemoKit.put(
            host,
            DemoKit.button(this, theme, "切换服务器", "outlined") { closeChild(); onSwitchServer() },
            14,
        )
    }

    private fun buildDisclaimerPage(host: LinearLayout) {
        val card = DemoKit.panel(this, theme, 16)
        DemoKit.put(card, DemoKit.txt(this, theme, "免责声明", 16f, true))
        val lines = listOf(
            "⚠ 本项目仅供实验学习，请勿用于真实交易业务。",
            "· 网关会把你的请求转发到你配置的上游服务商，请自行确认对方的使用条款与隐私政策。",
            "· 卡密 / 密钥请妥善保管，泄露可能导致额度被盗用。",
            "· 本地模型与插件均来自第三方，安装前请自行确认来源可信。",
            "· 作者不对因使用本项目产生的任何直接或间接损失负责。",
        )
        for (t in lines) DemoKit.put(card, DemoKit.txt(this, theme, t, 12.5f, false, "onSurfaceVariant"), 10)
        DemoKit.put(host, card, 2)
    }

                                                      

    private fun fmt(v: String): String = if (v.isBlank() || v == "0") "—" else fmtTok(v.toLongOrNull() ?: 0L)

    private fun fmtTok(v: Long): String = when {
        v >= 100_000_000 -> String.format("%.1f亿", v / 100000000.0)
        v >= 10_000 -> String.format("%.1f万", v / 10000.0)
        else -> v.toString()
    }
}
