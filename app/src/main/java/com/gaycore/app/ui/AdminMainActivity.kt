package com.gaycore.app.ui

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.gaycore.app.App
import com.gaycore.app.R
import com.gaycore.app.data.Api
import com.gaycore.app.data.adminTarget
import com.gaycore.app.data.ServerEntry
import com.gaycore.app.theme.ThemeEngine
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

   
                                                                     
  
                                                                            
                                                   
   
class AdminMainActivity : ShellActivity() {

    private lateinit var server: ServerEntry
    private var pluginMenu: List<ShellItem> = emptyList()
    private var serverAnnouncement: String? = null

                                  
    private val prefs by lazy { getSharedPreferences("gaycore_admin_ui", Context.MODE_PRIVATE) }
    private fun darkModePref(): String = prefs.getString("dark_mode", "system") ?: "system"
    private fun dynamicPref(): Boolean = prefs.getBoolean("dynamic_colors", false)

    private val pages: AdminPages by lazy {
        AdminPages(this, { theme }, server, { openConvo() }) { spec ->
            openChild(spec.title, spec.topIcon, spec.topDesc, spec.onTop, spec.footer, spec.build)
        }.also { it.onRefreshChild = { refreshChild() } }
    }

    companion object {
        const val A_HOME = "home"
        const val A_INSTANCES = "instances"
        const val A_USERS = "users"
        const val A_CHANNELS = "channels"
        const val A_MODELS = "models"
        const val A_PLUGINS = "plugins"
        const val A_USAGE = "usage"
        const val A_PLAYGROUND = "playground"
        const val A_NOTICE = "notice"
        const val A_THEME = "theme"
        const val A_SETTINGS = "settings"
        const val A_PROFILE = "profile"
    }

                                                        

    override fun onCreate(savedInstanceState: Bundle?) {
        val store = App.of(this).store
        val s = store.get(intent.getStringExtra("serverId") ?: store.currentAdminServer ?: "")
        if (s == null) {
            super.onCreate(savedInstanceState)
            finish()
            return
        }
        server = s
        try {
            super.onCreate(savedInstanceState)
        } catch (e: Throwable) {
            showFatal(e)
            return
        }
        loadPluginMenu()
        loadServerInfo()
    }

                       
    private fun loadServerInfo() {
        io {
            val r = Api.adminGet(server, "/admin/api/server-info")
            val ann = r.objOrNull("serverInfo")?.str("announcement")
            withContext(Dispatchers.Main) {
                if (ann != serverAnnouncement) {
                    serverAnnouncement = ann
                    if (currentPageId == A_HOME) refreshCurrentPage()
                }
            }
        }
    }

    private fun showFatal(e: Throwable) {
        val te = ThemeEngine(null)
        te.applyToWindow(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(te.color(this@AdminMainActivity, "background"))
        }
        DemoKit.put(col, DemoKit.txt(this, te, "界面构建失败", 20f, true, "error"))
        DemoKit.put(col, DemoKit.txt(this, te, e.javaClass.simpleName + ": " + (e.message ?: ""), 13f, false, "onSurfaceVariant"), 8)
        DemoKit.put(col, DemoKit.button(this, te, "返回", "tonal") { finish() }, 16)
        setContentView(pageScroll(col))
    }

    private fun JsonObject.arrOrNull(k: String): JsonArray? = get(k)?.takeIf { it.isJsonArray }?.asJsonArray
    private fun JsonObject.objOrNull(k: String): JsonObject? = get(k)?.takeIf { it.isJsonObject }?.asJsonObject
    private fun JsonObject.str(k: String): String = get(k)?.takeIf { !it.isJsonNull }?.asString ?: ""
    private fun JsonObject.bool(k: String): Boolean = get(k)?.takeIf { !it.isJsonNull }?.asBoolean ?: false
    private fun JsonObject.long(k: String): Long = get(k)?.takeIf { !it.isJsonNull }?.asLong ?: 0L

    private suspend fun currentUid(): Long {
        val st = Api.adminGet(server, "/admin/api/status")
        val arr = st.arrOrNull("instances") ?: return 1L
        for (e in arr) {
            val o = e.takeIf { it.isJsonObject }?.asJsonObject ?: continue
            if (o.str("name") == server.adminTarget) return o.long("uid")
        }
        return arr.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject?.long("uid") ?: 1L
    }

    private fun io(block: suspend () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { tip(e.message ?: "操作失败") }
            }
        }
    }

    private fun <T> fill(box: LinearLayout, fetch: suspend () -> T, render: (T) -> Unit) {
        box.removeAllViews()
        DemoKit.put(box, DemoKit.txt(this, theme, "读取中…", 13f, false, "onSurfaceVariant"))
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val data = fetch()
                withContext(Dispatchers.Main) {
                    box.removeAllViews()
                    render(data)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    box.removeAllViews()
                    DemoKit.put(box, DemoKit.txt(this@AdminMainActivity, theme, "读取失败: " + (e.message ?: ""), 13f, false, "error"))
                }
            }
        }
    }

                                          
    private var lastPluginMenuAt = 0L

    private fun loadPluginMenu() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val u = currentUid()
                val arr = Api.adminGet(server, "/admin/api/plugins/$u").arrOrNull("plugins") ?: JsonArray()
                val items = ArrayList<ShellItem>()
                for (e in arr) {
                    val p = e.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                    val pid = p.str("id")
                    if (pid.isEmpty()) continue
                    if (!p.bool("hasAdminPage")) continue
                    if (!p.bool("enable")) continue
                    items.add(ShellItem("pp:" + pid, p.str("name").ifEmpty { pid }, "插件管理页", DemoKit.iconRes(p.str("icon"))))
                }
                withContext(Dispatchers.Main) {
                    lastPluginMenuAt = System.currentTimeMillis()
                    val changed = items.size != pluginMenu.size || items.zip(pluginMenu).any { it.first.id != it.second.id }
                    pluginMenu = items
                    if (changed) rebuildShell(false) else rebuildDrawerMenuOnly()
                }
            } catch (_: Exception) {
                                    
            }
        }
    }

    private fun rebuildDrawerMenuOnly() {
                                      
        rebuildShell(false)
    }

    override fun onShellRebuilt() {
                                                                 
    }

                                                        

    override fun createTheme(): ThemeEngine = ThemeEngine(null, darkModePref(), dynamicPref())

    override fun shellItems(): List<ShellItem> {
        val core = listOf(
            ShellItem(A_HOME, "首页", server.name.ifEmpty { server.baseUrl }, R.drawable.ic_home),
            ShellItem(A_INSTANCES, "实例", "单进程托管全部实例", R.drawable.ic_grid),
            ShellItem(A_USERS, "用户管理", "用户与卡密", R.drawable.ic_group),
            ShellItem(A_CHANNELS, "渠道", "上游渠道与故障切换", R.drawable.ic_apps),
            ShellItem(A_MODELS, "模型分组", "客户端名 → 上游名", R.drawable.ic_sort),
            ShellItem(A_PLUGINS, "插件", "开关与参数", R.drawable.ic_star),
            ShellItem(A_USAGE, "使用", "请求统计与排行", R.drawable.ic_wallet),
            ShellItem(A_PLAYGROUND, "PlayGround", "直接试玩网关", R.drawable.ic_chat),
            ShellItem(A_NOTICE, "公告", "服务点公告", R.drawable.ic_info),
            ShellItem(A_THEME, "主题", "外观与配色", R.drawable.ic_palette),
            ShellItem(A_SETTINGS, "设置", "服务点与危险操作", R.drawable.ic_settings),
            ShellItem(A_PROFILE, "个人中心", "我的资料与凭据", R.drawable.ic_person),
        )
        return core + pluginMenu
    }

    override fun buildPage(id: String): View = when {
        id == A_HOME -> homePage()
        id == A_INSTANCES -> instancesPage()
        id == A_USERS -> pages.buildUsers()
        id == A_CHANNELS -> pages.buildChannels()
        id == A_MODELS -> pages.buildModels()
        id == A_PLUGINS -> pages.buildPlugins()
        id == A_USAGE -> usagePage()
        id == A_PLAYGROUND -> pages.buildPlayground()
        id == A_NOTICE -> noticePage()
        id == A_THEME -> themePage()
        id == A_SETTINGS -> settingsPage()
        id == A_PROFILE -> profilePage()
        id.startsWith("pp:") -> pluginPage(id.removePrefix("pp:"))
        else -> errorPage("未知页面: " + id)
    }

    override fun topActionFor(id: String): TopAction = when (id) {
                                                
        A_MODELS -> TopAction(R.drawable.ic_grid, "快速分组") {
            openChild("快速分组", 0, "", null, { bar -> pages.quickGroupFooter(bar) }) { host ->
                pages.buildQuickGroup(host)
            }
        }
        else -> TopAction(R.drawable.ic_refresh, "刷新") { refreshCurrentPage() }
    }

    override fun onRefresh(id: String) {
        refreshCurrentPage()
                                          
        loadPluginMenu()
    }

    override fun onShellResume() {
        if (System.currentTimeMillis() - lastPluginMenuAt < 30_000) return
        loadPluginMenu()
        loadServerInfo()
    }

    override fun identity(): Identity {
        val name = server.name.ifEmpty { server.baseUrl }
        val ai = server.adminTarget
        return Identity(name.take(1).uppercase(), name, server.baseUrl, "管理实例 · " + ai)
    }

    override fun fabFor(id: String): FabSpec? = when (id) {
        A_INSTANCES -> FabSpec(R.drawable.ic_add, "新建实例") { createInstance() }
        A_CHANNELS -> FabSpec(R.drawable.ic_add, "新增渠道") { pages.editChannel(null) { refreshCurrentPage() } }
        else -> null
    }

    override fun recents(): MutableList<String> = pages.playConvos

    override fun onRecentPick(title: String) {
        showPage(A_PLAYGROUND)
        tip("已切换到：" + title)
    }

    override fun onSwitchServer() {
        confirm("切换服务器", "返回服务器列表？管理密钥会保留。") { finish() }
    }

    override fun onToggleDarkMode() {
        val next = if (darkModePref() == "dark") "light" else "dark"
        prefs.edit().putString("dark_mode", next).apply()
                                      
        rebuildShell(true)
    }

                                                           
    private fun profilePage(): View {
        val col = DemoKit.pageColumn(this, theme)
        DemoKit.put(col, DemoKit.sectionTitle(this, theme, "个人中心", "资料 / 凭据 / 额度"), 2)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(col, box)
        ProfilePage.build(
            this, theme, server, box, asAdmin = true,
            onLogout = { confirm("退出管理端", "返回服务器列表？管理密钥会保留。") { finish() } },
            onOpenSiteInfo = { showPage(A_SETTINGS) },
        )
        return pageScroll(col)
    }

    private fun errorPage(msg: String): View {
        val col = DemoKit.pageColumn(this, theme)
        val card = DemoKit.panel(this, theme, 16)
        DemoKit.put(card, DemoKit.txt(this, theme, msg, 13f, false, "error"))
        DemoKit.put(col, card, 2)
        return pageScroll(col)
    }

    private fun pluginPage(pid: String): View {
        val col = DemoKit.pageColumn(this, theme)
        val card = DemoKit.panel(this, theme, 16)
        if (!pages.canBuildNative(pid)) pages.buildPluginWeb(pid, "admin.html", card)
        else pages.buildNativePlugin(pid, card)
        DemoKit.put(col, card, 2)
        return pageScroll(col)
    }

                                                      

    private fun homePage(): View {
        val col = DemoKit.pageColumn(this, theme)
        DemoKit.put(col, serverCard(), 2)

        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(col, box, 18)
        fill(box, {
            val st = Api.adminGet(server, "/admin/api/status")
            val audit = try { Api.adminGet(server, "/admin/api/audit/${currentUid()}?limit=6") } catch (_: Exception) { null }
            st to audit
        }) { pair ->
            val st = pair.first
            val audit = pair.second
            val arr = st.arrOrNull("instances") ?: JsonArray()
            var running = 0
            var reqs = 0L
            var errs = 0L
            val list = ArrayList<JsonObject>()
            for (e in arr) {
                val o = e.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                list.add(o)
                if (o.bool("running")) running++
                reqs += o.long("requests")
                errs += o.long("errors")
            }
            DemoKit.put(
                box,
                DemoKit.metricRow(
                    this,
                    DemoKit.metricTile(this, theme, list.size.toString(), "实例总数", "▤") { showPage(A_INSTANCES) },
                    DemoKit.metricTile(this, theme, running.toString(), "运行中", "●") { showPage(A_INSTANCES) },
                    DemoKit.metricTile(this, theme, reqs.toString(), "总请求", "↻") { showPage(A_USAGE) },
                ),
                0,
            )
            val m2 = LinearLayout(this)
            m2.addView(DemoKit.metricTile(this, theme, errs.toString(), "错误", "⚠") { showPage(A_USAGE) }, LinearLayout.LayoutParams(0, dp(104), 1f))
            m2.addView(DemoKit.metricTile(this, theme, server.adminTarget, "当前实例", "⎇") { showPage(A_INSTANCES) }, LinearLayout.LayoutParams(0, dp(104), 1f).apply { marginStart = dp(8) })
            m2.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            DemoKit.put(box, m2, 8)

                                 
            DemoKit.put(box, DemoKit.sectionTitle(this, theme, "快速入口", "常用操作"), 20)
            val quick = LinearLayout(this).apply { gravity = Gravity.CENTER }
            quick.addView(DemoKit.actionTile(this, theme, "渠道", R.drawable.ic_apps) { showPage(A_CHANNELS) }, LinearLayout.LayoutParams(0, dp(88), 1f))
            quick.addView(DemoKit.actionTile(this, theme, "用户管理", R.drawable.ic_group) { showPage(A_USERS) }, LinearLayout.LayoutParams(0, dp(88), 1f).apply { marginStart = dp(8) })
            quick.addView(DemoKit.actionTile(this, theme, "PlayGround", R.drawable.ic_chat) { showPage(A_PLAYGROUND) }, LinearLayout.LayoutParams(0, dp(88), 1f).apply { marginStart = dp(8) })
            DemoKit.put(box, quick, 10)

                      
            DemoKit.put(box, DemoKit.sectionTitle(this, theme, "实例摘要", "共 " + list.size + " 个"), 20)
            if (list.isEmpty()) {
                val card = DemoKit.panel(this, theme, 16)
                DemoKit.put(card, DemoKit.txt(this, theme, "尚未获取到实例信息。", 12.5f, false, "onSurfaceVariant"))
                DemoKit.put(box, card, 8)
            } else {
                list.take(3).forEach { o -> DemoKit.put(box, instanceSummaryCard(o), 8) }
                if (list.size > 3) {
                    DemoKit.put(box, DemoKit.chip(this, theme, "查看全部 " + list.size + " 个", R.drawable.ic_chevron_right) { showPage(A_INSTANCES) }, 10)
                }
            }

                               
            val ann = serverAnnouncement
            if (!ann.isNullOrBlank()) {
                DemoKit.put(box, DemoKit.sectionTitle(this, theme, "公告", "用户端首页会展示这条"), 20)
                val nc = DemoKit.panel(this, theme, 16, ripple = true).apply {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { showPage(A_NOTICE) }
                }
                DemoKit.put(nc, DemoKit.txt(this, theme, "服务点公告", 10.5f, true, "onSurfaceVariant"))
                DemoKit.put(nc, DemoKit.txt(this, theme, ann, 13f, false, "onSurface"), 7)
                DemoKit.put(box, nc, 10)
            }

                      
            if (audit != null) {
                val entries = audit.arrOrNull("entries") ?: JsonArray()
                DemoKit.put(box, DemoKit.sectionTitle(this, theme, "审计日志", "最近 " + entries.size() + " 条"), 20)
                val card = DemoKit.panel(this, theme, 16)
                if (entries.size() == 0) {
                    DemoKit.put(card, DemoKit.txt(this, theme, "暂无审计记录。", 12.5f, false, "onSurfaceVariant"))
                } else {
                    for (i in 0 until entries.size()) {
                        val l = entries.get(i).takeIf { it.isJsonObject }?.asJsonObject ?: continue
                        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
                        row.addView(
                            DemoKit.txt(this, theme, auditText(l), 12f, false, "onSurface"),
                            LinearLayout.LayoutParams(0, -2, 1f),
                        )
                        row.addView(DemoKit.txt(this, theme, shortTime(l.str("ts")), 10.5f, false, "onSurfaceVariant"))
                        DemoKit.put(card, row, if (i == 0) 0 else 10)
                    }
                }
                DemoKit.put(box, card, 8)
            }
        }
        return pageScroll(col)
    }

                                                 
    private fun auditText(l: JsonObject): String {
        val action = l.str("action")
        val detail = StringBuilder()
        for ((k, v) in l.entrySet()) {
            if (k == "ts" || k == "action") continue
            val s = v.toString().trim('"')
            if (s.isEmpty() || s == "null") continue
            if (detail.isNotEmpty()) detail.append(" · ")
            detail.append(k).append('=').append(if (s.length > 24) s.take(24) + "…" else s)
        }
        return if (detail.isEmpty()) action else action + "  " + detail
    }

                              
    private fun timeOf(millis: Long): String {
        if (millis <= 0L) return "—"
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = millis
        return String.format(
            "%02d/%02d %02d:%02d",
            cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH),
            cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE),
        )
    }

    private fun shortTime(ts: String): String {
        if (ts.length < 16) return ts
                                                    
        return try {
            ts.substring(5, 10).replace('-', '/') + " " + ts.substring(11, 16)
        } catch (_: Exception) {
            ts
        }
    }

    private fun instanceSummaryCard(o: JsonObject): View {
        val card = DemoKit.panel(this, theme, 16, ripple = true).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { showPage(A_INSTANCES) }
        }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val dot = View(this).apply {
            background = DemoKit.rounded(
                this@AdminMainActivity,
                theme.color(this@AdminMainActivity, if (o.bool("running")) "success" else "outline"), 100,
            )
        }
        row.addView(dot, LinearLayout.LayoutParams(dp(9), dp(9)))
        row.addView(
            DemoKit.txt(this, theme, o.str("name").ifEmpty { "?" }, 14.5f, true),
            LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) },
        )
        row.addView(DemoKit.badge(this, theme, if (o.bool("running")) "运行中" else "已停止", if (o.bool("running")) "success" else "neutral"))
        DemoKit.put(card, row)
        DemoKit.put(
            card,
            DemoKit.txt(this, theme, "端口 " + o.long("port") + " · " + o.long("requests") + " 请求 · " + o.long("errors") + " 错误", 11.5f, false, "onSurfaceVariant"),
            9,
        )
        return card
    }

    private fun serverCard(): View {
        val card = DemoKit.gradientPanel(this, theme, 16)
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(DemoKit.iconBadge(this, theme, R.drawable.ic_home, null, 46), LinearLayout.LayoutParams(dp(46), dp(46)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(info, DemoKit.txt(this, theme, "管理服务点", 11f, false, "onSurfaceVariant"))
        DemoKit.put(info, DemoKit.txt(this, theme, server.name.ifEmpty { "未命名服务点" }, 19f, true), 3)
        DemoKit.put(info, DemoKit.txt(this, theme, "● 已连接  " + server.baseUrl, 11.5f, true, "success"), 5)
        row.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(14) })
        DemoKit.put(card, row)
        val foot = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        foot.addView(DemoKit.badge(this, theme, "管理员", "primary"))
        foot.addView(DemoKit.badge(this, theme, "实例 " + server.adminTarget, "neutral"), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
        val key = server.adminKey() ?: ""
        if (key.isNotEmpty()) {
            foot.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
            foot.addView(DemoKit.iconButton(this, theme, R.drawable.ic_copy, "复制管理密钥") {
                DemoKit.copy(this, "adminKey", key)
                tip(getString(R.string.copied))
            }, LinearLayout.LayoutParams(dp(32), dp(32)))
        }
        DemoKit.put(card, foot, 12)
        return card
    }

                                                        

    private fun instancesPage(): View {
        val col = DemoKit.pageColumn(this, theme)
        DemoKit.put(col, DemoKit.sectionTitle(this, theme, "网关实例", "单进程托管全部实例，各自独立端口与路径前缀"), 2)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(col, box, 10)
        fill(box, { Api.adminGet(server, "/admin/api/status") }) { st ->
            val arr = st.arrOrNull("instances") ?: JsonArray()
            var running = 0
            var reqs = 0L
            var errs = 0L
            val list = ArrayList<JsonObject>()
            for (e in arr) {
                val o = e.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                list.add(o)
                if (o.bool("running")) running++
                reqs += o.long("requests")
                errs += o.long("errors")
            }
            DemoKit.put(
                box,
                DemoKit.metricRow(
                    this,
                    DemoKit.metricTile(this, theme, list.size.toString(), "实例总数", "▤"),
                    DemoKit.metricTile(this, theme, running.toString(), "运行中", "●"),
                    DemoKit.metricTile(this, theme, reqs.toString(), "总请求", "↻"),
                ),
                0,
            )
            DemoKit.put(box, DemoKit.sectionTitle(this, theme, "实例列表", "共 " + list.size + " 个 · 错误 " + errs), 20)
            val cards = ArrayList<View>()
            for (o in list) {
                val c = instanceCard(o)
                cards.add(c)
                DemoKit.put(box, c, 8)
            }
            animateInStaggered(cards, 28)

            DemoKit.put(box, DemoKit.sectionTitle(this, theme, "批量操作", "对所有实例生效"), 20)
            val acts = LinearLayout(this)
            acts.addView(DemoKit.button(this, theme, "全部启动", "outlined") { batchEnable(true) }, LinearLayout.LayoutParams(0, dp(40), 1f))
            acts.addView(DemoKit.button(this, theme, "全部停止", "outlined") { batchEnable(false) }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginStart = dp(10) })
            DemoKit.put(box, acts, 10)
            DemoKit.put(box, DemoKit.button(this, theme, "新建实例", "filled") { createInstance() }, 10)
        }
        return pageScroll(col)
    }

       
                                                       
                                                                 
       
    private fun switchInstance(name: String, enabled: Boolean, running: Boolean) {
                                                               
                                                                                    
        if (name == server.adminTarget) { tip("已经是当前实例"); return }
        val warn = when {
            !enabled -> "\n\n⚠️ 该实例当前是「已停用」状态，切过去后管理接口会返回错误。"
            !running -> "\n\n⚠️ 该实例当前「未运行」，切过去后管理接口可能不可用。"
            else -> ""
        }
        confirm(
            "切换到「" + name + "」",
            "之后所有管理页面（渠道 / 模型分组 / 用户 / 插件 / 设置）都会定向到这个实例。" +
                "\n\n侧边栏顶部的「管理员 · " + name + "」就是当前实例的标识。" + warn,
        ) {
            server.adminInstance = name
            com.gaycore.app.App.of(this).store.save(server)
            rebuildShell(true)
            tip("已切换到 " + name)
            refreshCurrentPage()
        }
    }

    private fun batchEnable(enable: Boolean) {
        confirm(if (enable) "全部启动" else "全部停止", "将对所有实例生效，确定继续？") {
            io {
                val st = Api.adminGet(server, "/admin/api/status")
                val arr = st.arrOrNull("instances") ?: JsonArray()
                for (e in arr) {
                    val o = e.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                    val u = o.long("uid")
                    if (u <= 0) continue
                    try {
                        Api.adminPost(server, "/admin/api/instance/$u/enable", JsonObject().apply { addProperty("enable", enable) })
                    } catch (_: Exception) {
                    }
                }
                withContext(Dispatchers.Main) {
                    tip(if (enable) "已提交启动" else "已提交停止")
                    refreshCurrentPage()
                }
            }
        }
    }

    private fun instanceCard(o: JsonObject): View {
        val uid = o.long("uid")
        val name = o.str("name").ifEmpty { "?" }
        val enabled = o.bool("enabled")
        val running = o.bool("running")
        val isCurrent = name == server.adminTarget
        val card = DemoKit.panel(this, theme, 16)
        if (!isCurrent) {
                             
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener { switchInstance(name, enabled, running) }
        }

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val dot = View(this).apply {
            background = DemoKit.rounded(this@AdminMainActivity, theme.color(this@AdminMainActivity, if (running) "success" else "outline"), 100)
        }
        top.addView(dot, LinearLayout.LayoutParams(dp(9), dp(9)))
        top.addView(DemoKit.txt(this, theme, name, 15f, true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        if (isCurrent) {
            top.addView(DemoKit.badge(this, theme, "当前实例", "primary"), LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(6) })
        }
        top.addView(
            DemoKit.badge(
                this, theme,
                when {
                    !enabled -> "已停用"
                    running -> "运行中"
                    else -> "未运行"
                },
                if (running) "success" else "neutral",
            ),
        )
        DemoKit.put(card, top)

        DemoKit.put(card, DemoKit.txt(this, theme, "端口 " + o.long("port") + "   ·   uid " + uid + "   ·   路径前缀 /" + name + "/", 11.5f, false, "onSurfaceVariant"), 9)
        DemoKit.put(
            card,
            DemoKit.txt(this, theme, o.long("requests").toString() + " 请求   ·   " + o.long("errors") + " 错误   ·   " + (if (running) "内存共享" else "未占用内存"), 12f, false, "onSurfaceVariant"),
            4,
        )

        DemoKit.put(
            card,
            DemoKit.hscrollRow(
                this,
                listOfNotNull(
                    DemoKit.chip(this, theme, if (enabled) "停用" else "启用", if (enabled) R.drawable.ic_block else R.drawable.ic_check) {
                        io {
                            Api.adminPost(server, "/admin/api/instance/$uid/enable", JsonObject().apply { addProperty("enable", !enabled) })
                            withContext(Dispatchers.Main) { refreshCurrentPage() }
                        }
                    },
                    if (!isCurrent) DemoKit.chip(this, theme, "切换到此", R.drawable.ic_check) { switchInstance(name, enabled, running) } else null,
                    DemoKit.chip(this, theme, "改名", R.drawable.ic_edit) { renameInstance(uid, name) },
                    DemoKit.chip(this, theme, "重载", R.drawable.ic_refresh) {
                        io {
                            Api.adminPost(server, "/admin/api/instance/$uid/reload", JsonObject())
                            withContext(Dispatchers.Main) { tip("已重载 " + name) }
                        }
                    },
                    DemoKit.chip(this, theme, "详情", R.drawable.ic_info) { showInstanceDetail(o) },
                    DemoKit.chip(this, theme, "删除", R.drawable.ic_delete) {
                        UiKit.confirm(this, "删除实例", "确定删除「" + name + "」？配置将一并移除。") {
                            io {
                                Api.adminDelete(server, "/admin/api/instance/$uid")
                                withContext(Dispatchers.Main) {
                                    tip("已删除")
                                    refreshCurrentPage()
                                }
                            }
                        }
                    },
                ),
            ),
            12,
        )
        return card
    }

    private fun showInstanceDetail(o: JsonObject) {
        openChild("实例详情 · " + o.str("name")) { host ->
            val card = DemoKit.panel(this, theme, 16)
            val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            head.addView(DemoKit.iconBadge(this, theme, R.drawable.ic_grid, null, 44), LinearLayout.LayoutParams(dp(44), dp(44)))
            val ht = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            DemoKit.put(ht, DemoKit.txt(this, theme, o.str("name").ifEmpty { "?" }, 18f, true))
            DemoKit.put(ht, DemoKit.txt(this, theme, "uid " + o.long("uid") + " · 路径前缀 /" + o.str("name") + "/", 11.5f, false, "onSurfaceVariant"), 3)
            head.addView(ht, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
            head.addView(
                DemoKit.badge(
                    this, theme,
                    if (o.bool("running")) "运行中" else if (o.bool("enabled")) "未运行" else "已停用",
                    if (o.bool("running")) "success" else "neutral",
                ),
            )
            DemoKit.put(card, head)
            DemoKit.put(card, DemoKit.valueRow(this, theme, "端口", o.long("port").toString()), 16)
            DemoKit.put(card, DemoKit.valueRow(this, theme, "启用", if (o.bool("enabled")) "是" else "否"), 10)
            DemoKit.put(card, DemoKit.valueRow(this, theme, "运行中", if (o.bool("running")) "是" else "否"), 10)
            DemoKit.put(card, DemoKit.valueRow(this, theme, "请求数", o.long("requests").toString()), 10)
            DemoKit.put(card, DemoKit.valueRow(this, theme, "错误数", o.long("errors").toString()), 10)
            DemoKit.put(card, DemoKit.valueRow(this, theme, "内存", if (o.bool("running")) "与其他实例共享进程" else "未占用"), 10)
            DemoKit.put(host, card, 2)

            val raw = DemoKit.panel(this, theme, 16)
            DemoKit.put(raw, DemoKit.txt(this, theme, "原始字段", 15f, true))
            val sb = StringBuilder()
            for ((k, v) in o.entrySet()) sb.append(k).append(" = ").append(v).append('\n')
            val tv = DemoKit.txt(this, theme, sb.toString().trim(), 12f, false, "onSurfaceVariant")
            tv.setTextIsSelectable(true)
            tv.typeface = android.graphics.Typeface.MONOSPACE
            DemoKit.put(raw, tv, 10)
            DemoKit.put(host, raw, 14)

            DemoKit.put(host, DemoKit.button(this, theme, "重载该实例", "tonal") {
                io {
                    Api.adminPost(server, "/admin/api/instance/" + o.long("uid") + "/reload", JsonObject())
                    withContext(Dispatchers.Main) {
                        tip("已重载")
                        closeChild()
                    }
                }
            }, 16)
        }
    }

    private fun renameInstance(uid: Long, old: String) {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), 0)
        }
        val f = MdField(this, theme, "新名称", old, false, false)
        DemoKit.put(col, f, 10)
        UiKit.customDialog(this, theme, "重命名实例", col, "保存") {
            val nn = f.text.trim()
            if (nn.isEmpty()) {
                tip("名称不能为空")
                UiKit.keepOpen(); return@customDialog
            }
            io {
                Api.adminPost(server, "/admin/api/instance/$uid/rename", JsonObject().apply { addProperty("name", nn) })
                withContext(Dispatchers.Main) {
                                                                     
                                                                         
                    if (server.adminTarget == old) {
                        server.adminInstance = nn
                        com.gaycore.app.App.of(this@AdminMainActivity).store.save(server)
                        rebuildShell(true)
                        tip("已改名，当前实例已切换到 " + nn)
                    } else {
                        tip("已改名")
                    }
                    refreshCurrentPage()
                }
            }
        }
    }

    private fun createInstance() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), 0)
        }
        val fName = MdField(this, theme, "实例名（也是路径前缀）", "", false, false)
        val fPort = MdField(this, theme, "端口（留空自动分配）", "", true, false)
        val fKey = MdField(this, theme, "实例 adminKey（可空）", "", false, false)
        DemoKit.put(col, fName, 10)
        DemoKit.put(col, fPort, 10)
        DemoKit.put(col, fKey, 10)
        UiKit.customDialog(this, theme, "新建实例", col, "创建") {
            val name = fName.text.trim()
            if (name.isEmpty()) {
                tip("实例名不能为空")
                UiKit.keepOpen(); return@customDialog
            }
            io {
                val body = JsonObject()
                body.addProperty("name", name)
                val pv = fPort.text.trim().toIntOrNull()
                if (pv != null) {
                    val listen = JsonObject()
                    listen.addProperty("port", pv)
                    body.add("listen", listen)
                }
                val kv = fKey.text.trim()
                if (kv.isNotEmpty()) body.addProperty("adminKey", kv)
                Api.adminPost(server, "/admin/api/instance", body)
                withContext(Dispatchers.Main) {
                    tip("已创建")
                    refreshCurrentPage()
                }
            }
        }
    }

                                                          

    private class UsageData(
        val instances: List<Pair<String, Long>>,
        val errors: Long,
        val byChannel: List<Pair<String, Long>>,
    )

    private fun usagePage(): View {
        val col = DemoKit.pageColumn(this, theme)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(col, box, 2)
        fill(box, {
            val st = Api.adminGet(server, "/admin/api/status")
            val arr = st.arrOrNull("instances") ?: JsonArray()
            val perInst = ArrayList<Pair<String, Long>>()
            var errs = 0L
            val chan = LinkedHashMap<String, Long>()
            for (e in arr) {
                val o = e.takeIf { it.isJsonObject }?.asJsonObject ?: continue
                val nm = o.str("name")
                perInst.add(nm to o.long("requests"))
                errs += o.long("errors")
                                                   
                try {
                    val pfx = if (nm == "default" || nm.isEmpty()) "" else "/" + nm
                    val s2 = Api.get(server.baseUrl + pfx + "/status")
                    val bc = s2.objOrNull("stats")?.objOrNull("byChannel")
                    bc?.entrySet()?.forEach { (k, v) ->
                        val n = v.takeIf { it.isJsonPrimitive }?.asLong ?: 0L
                        chan[k] = (chan[k] ?: 0L) + n
                    }
                } catch (_: Exception) {
                }
            }
            UsageData(perInst, errs, chan.entries.sortedByDescending { it.value }.map { it.key to it.value })
        }) { d ->
            val total = d.instances.sumOf { it.second }
            val okRate = if (total + d.errors > 0) ((total * 100.0) / (total + d.errors)).roundToInt() else 100
            DemoKit.put(box, DemoKit.sectionTitle(this, theme, "使用情况", "全部实例汇总"), 0)
            DemoKit.put(
                box,
                DemoKit.metricRow(
                    this,
                    DemoKit.metricTile(this, theme, total.toString(), "总请求", "◈"),
                    DemoKit.metricTile(this, theme, d.errors.toString(), "错误", "⚠"),
                    DemoKit.metricTile(this, theme, okRate.toString() + "%", "成功率", "↻"),
                ),
                12,
            )

            val chart = DemoKit.panel(this, theme, 16)
            DemoKit.put(chart, DemoKit.txt(this, theme, "各实例请求量", 16f, true))
            DemoKit.put(chart, DemoKit.txt(this, theme, "按实例统计 · 来自 /admin/api/status", 12f, false, "onSurfaceVariant"), 3)
            if (d.instances.isEmpty()) {
                DemoKit.put(chart, DemoKit.txt(this, theme, "暂无数据。", 12.5f, false, "onSurfaceVariant"), 14)
            } else {
                DemoKit.put(
                    chart,
                    DemoKit.barChart(
                        this, theme,
                        d.instances.map { it.second.toInt() },
                        theme.color(this, "primary").let { String.format("#%06X", 0xFFFFFF and it) },
                        d.instances.map { it.first.take(4) },
                    ),
                    16,
                )
            }
            DemoKit.put(box, chart, 12)

            DemoKit.put(box, DemoKit.sectionTitle(this, theme, "渠道排行", "按请求数排序"), 20)
            if (d.byChannel.isEmpty()) {
                val card = DemoKit.panel(this, theme, 16)
                DemoKit.put(card, DemoKit.txt(this, theme, "还没有渠道请求记录（或实例未运行）。", 12.5f, false, "onSurfaceVariant"))
                DemoKit.put(box, card, 8)
            } else {
                val sum = d.byChannel.sumOf { it.second }.coerceAtLeast(1L)
                d.byChannel.take(8).forEachIndexed { i, pair ->
                    val ratio = (pair.second.toFloat() / sum).toFloat()
                    val hex = String.format("#%06X", 0xFFFFFF and theme.color(this, if (i == 0) "primary" else "secondary"))
                    DemoKit.put(
                        box,
                        DemoKit.rankRow(this, theme, pair.first, pair.second.toString(), (ratio * 100).roundToInt().toString() + "%", hex, ratio),
                        8,
                    )
                }
            }
        }
        return pageScroll(col)
    }

                                                      

    private fun noticePage(): View {
        val col = DemoKit.pageColumn(this, theme)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(col, box, 2)
        fill(box, { Api.adminGet(server, "/admin/api/server-info") }) { r ->
            renderNotice(box, r.objOrNull("serverInfo") ?: JsonObject())
        }
        return pageScroll(col)
    }

                                  
    private fun saveNotices(si: JsonObject, list: List<Notices.Item>, onOk: () -> Unit) {
        val text = Notices.encode(list)
        if (text.length > 2000) {
            tip("公告总长超过 2000 字（当前 " + text.length + "），请精简")
            return
        }
        io {
            Api.adminPost(
                server, "/admin/api/server-info",
                JsonObject().apply {
                    addProperty("name", si.str("name"))
                    addProperty("description", si.str("description"))
                    addProperty("icon", si.str("icon"))
                    addProperty("contact", si.str("contact"))
                    addProperty("website", si.str("website"))
                    addProperty("announcement", text)
                },
            )
            withContext(Dispatchers.Main) {
                serverAnnouncement = text
                tip("已保存")
                onOk()
            }
        }
    }

    private fun renderNotice(box: LinearLayout, si: JsonObject) {
        box.removeAllViews()
        val list = Notices.parse(si.str("announcement"))
        DemoKit.put(box, DemoKit.sectionTitle(this, theme, "服务点公告", "共 " + list.size + " 条 · 用户端首页与公告页会展示"), 0)
        if (list.isEmpty()) {
            val card = DemoKit.panel(this, theme, 16)
            DemoKit.put(card, DemoKit.txt(this, theme, "还没有公告。点下面「新增公告」或「添加时间线」。", 13f, false, "onSurfaceVariant"))
            DemoKit.put(box, card, 10)
        } else {
            val cards = ArrayList<View>()
            list.forEachIndexed { i, e ->
                val card = DemoKit.panel(this, theme, 16, ripple = true).apply {
                    setOnClickListener { editNotice(si, list, i) }
                    setOnLongClickListener { askDeleteNotice(si, list, i); true }
                }
                val meta = ArrayList<String>()
                if (e.time.isNotEmpty()) meta.add(e.time)
                if (e.target.isNotEmpty()) meta.add("定向 @" + e.target)
                if (meta.isNotEmpty()) {
                    val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
                    row.addView(DemoKit.txt(this, theme, meta.joinToString("  ·  "), 10.5f, true, "onSurfaceVariant"), LinearLayout.LayoutParams(0, -2, 1f))
                    if (e.target.isNotEmpty()) row.addView(DemoKit.badge(this, theme, "定向", "warning"))
                    DemoKit.put(card, row)
                }
                DemoKit.put(card, DemoKit.txt(this, theme, e.body, 14f, true), if (meta.isEmpty()) 0 else 7)
                DemoKit.put(box, card, 8)
                cards.add(card)
            }
            animateInStaggered(cards, 26)
        }
        DemoKit.put(box, DemoKit.txt(this, theme, "点卡片编辑 · 长按卡片删除 · 总长上限 2000 字", 11f, false, "onSurfaceVariant"), 12)

        val acts = LinearLayout(this)
        acts.addView(DemoKit.button(this, theme, "新增公告", "filled") { addNotice(si, list, false) }, LinearLayout.LayoutParams(0, dp(40), 1f))
        acts.addView(
            DemoKit.button(this, theme, "添加时间线", "outlined") { addNotice(si, list, true) },
            LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginStart = dp(10) },
        )
        DemoKit.put(box, acts, 12)

        val info = DemoKit.panel(this, theme, 16)
        DemoKit.put(info, DemoKit.txt(this, theme, "服务点资料", 16f, true))
        DemoKit.put(info, DemoKit.txt(this, theme, "名称 / 描述 / 联系方式在「设置 → 编辑服务点资料」里改", 11.5f, false, "onSurfaceVariant"), 4)
        DemoKit.put(info, DemoKit.valueRow(this, theme, "名称", si.str("name").ifEmpty { server.name }), 12)
        DemoKit.put(info, DemoKit.valueRow(this, theme, "描述", si.str("description").ifEmpty { "—" }), 10)
        DemoKit.put(info, DemoKit.valueRow(this, theme, "联系", si.str("contact").ifEmpty { "—" }), 10)
        DemoKit.put(info, DemoKit.valueRow(this, theme, "官网", si.str("website").ifEmpty { "—" }), 10)
        DemoKit.put(box, info, 16)
    }

                       
    private fun addNotice(si: JsonObject, list: List<Notices.Item>, timed: Boolean) {
        val col = dialogColumn()
        val f = MdField(this, theme, if (timed) "时间线内容（一行一条）" else "公告内容（一行一条）", "", false, false)
        DemoKit.put(col, f, 10)
        if (timed) {
            DemoKit.put(col, DemoKit.txt(this, theme, "会自动加上时间前缀 " + nowStamp(), 11.5f, false, "onSurfaceVariant"), 8)
        }
        UiKit.customDialog(this, theme, if (timed) "添加时间线" else "新增公告", col, "添加") {
            val text = f.text.trim()
            if (text.isEmpty()) {
                tip("内容不能为空")
                UiKit.keepOpen(); return@customDialog
            }
            val next = ArrayList(list)
            if (timed) next.add(Notices.Item(nowStamp(), "", text)) else next.add(Notices.Item("", "", text))
            saveNotices(si, next) { refreshCurrentPage() }
        }
    }

    private fun editNotice(si: JsonObject, list: List<Notices.Item>, index: Int) {
        val cur = list[index]
        val col = dialogColumn()
        val f = MdField(this, theme, "公告内容（一行一条）", cur.body, false, false)
        DemoKit.put(col, f, 10)
        if (cur.time.isNotEmpty()) {
            DemoKit.put(col, DemoKit.txt(this, theme, "时间前缀: " + cur.time, 11.5f, false, "onSurfaceVariant"), 8)
        }
        UiKit.customDialog(this, theme, "编辑公告", col, "保存") {
            val text = f.text.trim()
            if (text.isEmpty()) {
                tip("内容不能为空")
                UiKit.keepOpen(); return@customDialog
            }
            val next = ArrayList(list)
            next[index] = Notices.Item(cur.time, cur.target, text)
            saveNotices(si, next) { refreshCurrentPage() }
        }
    }

                           
    private fun askDeleteNotice(si: JsonObject, list: List<Notices.Item>, index: Int) {
        val it0 = list[index]
        val preview = (if (it0.target.isNotEmpty()) "（定向 @" + it0.target + "）" else "") + it0.body.take(40)
        UiKit.confirm(this, "删除该公告", "确定删除这条公告？\n\n" + preview) {
            val next = ArrayList(list)
            next.removeAt(index)
            saveNotices(si, next) { refreshCurrentPage() }
        }
    }

    private fun dialogColumn(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(8), dp(16), 0)
    }

    private fun nowStamp(): String {
        val c = java.util.Calendar.getInstance()
        return String.format(
            "%04d/%02d/%02d %02d:%02d",
            c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1,
            c.get(java.util.Calendar.DAY_OF_MONTH), c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE),
        )
    }

                                                      

    private fun themePage(): View {
        val col = DemoKit.pageColumn(this, theme)
        DemoKit.put(col, DemoKit.sectionTitle(this, theme, "主题与配色", if (dynamicPref()) "已启用 Material You 动态取色" else "管理端内置 MD3 配色"), 2)

        val mode = DemoKit.panel(this, theme, 16)
        DemoKit.put(mode, DemoKit.txt(this, theme, "显示模式", 16f, true))
        val modes = listOf("跟随系统" to "system", "深色" to "dark", "浅色" to "light")
        val cur = modes.indexOfFirst { it.second == darkModePref() }.coerceAtLeast(0)
        DemoKit.put(mode, DemoKit.segmented(this, theme, modes.map { it.first }, cur) { i ->
            prefs.edit().putString("dark_mode", modes[i].second).apply()
            rebuildShell(true)
        }, 12)
        DemoKit.put(col, mode, 12)

                                                     
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            val dy = DemoKit.panel(this, theme, 16)
            DemoKit.put(
                dy,
                DemoKit.switchRow(
                    this, theme, "动态取色 (Material You)",
                    "从壁纸提取种子色，生成整套配色（含表面/容器/描边），需要 Android 12+",
                    dynamicPref(),
                ) { on ->
                    prefs.edit().putBoolean("dynamic_colors", on).apply()
                    rebuildShell(true)
                },
            )
            DemoKit.put(col, dy, 12)
        }

        DemoKit.put(col, DemoKit.txt(this, theme, "组件预览", 16f, true), 12)
        val preview = DemoKit.panel(this, theme, 16)
        DemoKit.put(preview, DemoKit.txt(this, theme, "样式令牌实时预览", 15f, true))
        DemoKit.put(preview, DemoKit.txt(this, theme, "卡片、徽章、按钮与进度条在两种模式下的表现", 12f, false, "onSurfaceVariant"), 4)
        val badges = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        badges.addView(DemoKit.badge(this, theme, "运行中", "success"))
        badges.addView(DemoKit.badge(this, theme, "已停用", "error"), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
        badges.addView(DemoKit.badge(this, theme, "待机", "neutral"), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
        badges.addView(DemoKit.badge(this, theme, "主色", "primary"), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
        DemoKit.put(preview, badges, 12)
        val buttons = LinearLayout(this)
        buttons.addView(DemoKit.button(this, theme, "主按钮", "filled") { }, LinearLayout.LayoutParams(0, dp(40), 1f))
        buttons.addView(DemoKit.button(this, theme, "描边", "outlined") { }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginStart = dp(8) })
        DemoKit.put(preview, buttons, 12)
        DemoKit.put(preview, DemoKit.progressRow(this, theme, "组件完成度", 78, 100, "78%"), 12)
        DemoKit.put(col, preview, 10)

        DemoKit.put(col, DemoKit.sectionTitle(this, theme, "关于外观", ""), 20)
        val note = DemoKit.panel(this, theme, 16)
        DemoKit.put(note, DemoKit.txt(this, theme, "管理端不加载服务端主题插件（那是用户端的能力）；深浅色与动态取色保存在本机。", 12.5f, false, "onSurfaceVariant"))
        DemoKit.put(col, note, 10)
        return pageScroll(col)
    }

                                                      

    private fun settingsPage(): View {
        val col = DemoKit.pageColumn(this, theme)
        DemoKit.put(col, DemoKit.sectionTitle(this, theme, "设置", "服务点、调试与危险操作"), 2)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        DemoKit.put(col, box, 10)
        fill(box, {
            val info = try { Api.adminGet(server, "/admin/api/server-info") } catch (_: Exception) { null }
            val cc = try { Api.adminGet(server, "/admin/api/client-config/${currentUid()}") } catch (_: Exception) { null }
            val tm = try { Api.adminGet(server, "/admin/api/market/test-mode") } catch (_: Exception) { null }
            Triple(info, cc, tm)
        }) { t ->
            val info = t.first
            val cc = t.second
            val tm = t.third
            val si = info?.objOrNull("serverInfo")

            val card = DemoKit.panel(this, theme, 16)
            DemoKit.put(card, DemoKit.txt(this, theme, "服务点资料", 16f, true))
            DemoKit.put(card, DemoKit.valueRow(this, theme, "地址", server.baseUrl), 12)
            DemoKit.put(card, DemoKit.valueRow(this, theme, "管理实例", server.adminTarget), 10)
            DemoKit.put(card, DemoKit.valueRow(this, theme, "名称", si?.str("name").orEmpty().ifEmpty { server.name }), 10)
            DemoKit.put(card, DemoKit.button(this, theme, "编辑服务点资料", "tonal") { editServerInfo(si, false) }, 14)
            DemoKit.put(box, card, 0)

                        
            val ud = cc?.objOrNull("userDebug")
            val dcard = DemoKit.panel(this, theme, 16)
            DemoKit.put(dcard, DemoKit.txt(this, theme, "用户调试模式", 16f, true))
            DemoKit.put(
                dcard,
                DemoKit.txt(
                    this, theme,
                    if (ud != null) "已为 UID " + ud.str("uid") + " 开启（至 " + timeOf(ud.long("until")) + "）"
                    else "开启后指定用户可以看到调试信息，用于排查问题（限单用户 + 限时）。",
                    12.5f, false, "onSurfaceVariant",
                ),
                8,
            )
            val dacts = LinearLayout(this)
            dacts.addView(DemoKit.button(this, theme, "开启 / 续期", "filled") { openUserDebugDialog() }, LinearLayout.LayoutParams(0, dp(40), 1f))
            dacts.addView(
                DemoKit.button(this, theme, "关闭", "outlined") {
                    io {
                        Api.adminPost(
                            server, "/admin/api/client-config/${currentUid()}",
                            JsonObject().apply { add("userDebug", JsonObject()) },
                        )
                        withContext(Dispatchers.Main) {
                            tip("已关闭")
                            refreshCurrentPage()
                        }
                    }
                },
                LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginStart = dp(10) },
            )
            DemoKit.put(dcard, dacts, 14)
            DemoKit.put(box, dcard, 14)

                          
            val mcard = DemoKit.panel(this, theme, 16)
            DemoKit.put(
                mcard,
                DemoKit.switchRow(this, theme, "插件市场测试模式", "允许使用自定义来源安装插件（有安全风险）", tm?.bool("testMode") ?: false) { on ->
                    io {
                        Api.adminPost(server, "/admin/api/market/test-mode", JsonObject().apply { addProperty("enable", on) })
                        withContext(Dispatchers.Main) { tip(if (on) "已开启测试模式" else "已关闭测试模式") }
                    }
                },
            )
            DemoKit.put(box, mcard, 14)
        }

        DemoKit.put(col, DemoKit.sectionTitle(this, theme, "关于", "版本与说明"), 20)
        val about = DemoKit.panel(this, theme, 16)
        DemoKit.put(about, DemoKit.txt(this, theme, "Gay Core", 16f, true))
        DemoKit.put(about, DemoKit.txt(this, theme, "Material 3 原生界面 · 服务端驱动 UI", 12.5f, false, "onSurfaceVariant"), 4)
        DemoKit.put(about, DemoKit.valueRow(this, theme, "版本", com.gaycore.app.BuildConfig.VERSION_NAME), 12)
        DemoKit.put(about, DemoKit.valueRow(this, theme, "核心", "ai-gateway-core"), 10)
        DemoKit.put(about, DemoKit.valueRow(this, theme, "服务点", server.baseUrl), 10)
        DemoKit.put(col, about, 10)

        DemoKit.put(
            col,
            DemoKit.menuCard(this, theme, "免责声明", "使用须知与风险提示", R.drawable.ic_block, danger = true) {
                openChild("免责声明") { buildDisclaimer(it) }
            },
            10,
        )

        DemoKit.put(col, DemoKit.sectionTitle(this, theme, "危险操作", "请谨慎执行"), 20)
        DemoKit.put(
            col,
            DemoKit.dangerCard(this, theme, "停止全部实例", "网关将不再响应任何请求", R.drawable.ic_block) {
                confirm("停止全部实例", "确定停止全部实例？") { batchEnable(false) }
            },
            8,
        )
        DemoKit.put(
            col,
            DemoKit.dangerCard(this, theme, "移除该服务器", "本地管理密钥与缓存将被清除", R.drawable.ic_delete) {
                confirm("移除服务器", "确定移除「" + server.name + "」？本地数据将被清除。") {
                    App.of(this).store.remove(server.id)
                    finish()
                }
            },
            8,
        )
        return pageScroll(col)
    }

    private fun buildDisclaimer(host: LinearLayout) {
        val card = DemoKit.panel(this, theme, 16)
        DemoKit.put(card, DemoKit.txt(this, theme, "免责声明", 16f, true))
        val lines = listOf(
            "⚠ 本项目仅供实验学习，请勿用于真实交易业务。",
            "· 网关会把请求转发到你配置的上游服务商，请自行确认对方的使用条款与隐私政策。",
            "· 管理密钥 / 卡密请妥善保管，泄露可能导致额度被盗用。",
            "· 插件均来自第三方，安装前请自行确认来源可信；测试模式会放宽安装限制。",
            "· 作者不对因使用本项目产生的任何直接或间接损失负责。",
        )
        for (t in lines) DemoKit.put(card, DemoKit.txt(this, theme, t, 12.5f, false, "onSurfaceVariant"), 10)
        DemoKit.put(host, card, 2)
    }

    private fun editServerInfo(current: JsonObject?, focusAnnouncement: Boolean) {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), 0)
        }
        val fName = MdField(this, theme, "服务点名称", current?.str("name").orEmpty(), false, false)
        val fDesc = MdField(this, theme, "描述", current?.str("description").orEmpty(), false, false)
        val fAnn = MdField(this, theme, "公告", current?.str("announcement").orEmpty(), false, false)
        val fContact = MdField(this, theme, "联系方式", current?.str("contact").orEmpty(), false, false)
        val fSite = MdField(this, theme, "官网", current?.str("website").orEmpty(), false, false)
        for (v in listOf<View>(fName, fDesc, fAnn, fContact, fSite)) DemoKit.put(col, v, 10)
        UiKit.customDialog(this, theme, if (focusAnnouncement) "编辑公告" else "编辑服务点资料", col, "保存") {
            io {
                Api.adminPost(server, "/admin/api/server-info", JsonObject().apply {
                    addProperty("name", fName.text.trim())
                    addProperty("description", fDesc.text.trim())
                    addProperty("announcement", fAnn.text.trim())
                    addProperty("contact", fContact.text.trim())
                    addProperty("website", fSite.text.trim())
                })
                withContext(Dispatchers.Main) {
                    tip("已保存")
                    refreshCurrentPage()
                }
            }
        }
    }

    private fun openUserDebugDialog() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), 0)
        }
        val fUid = MdField(this, theme, "目标用户 UID", "", false, false)
        val fMin = MdField(this, theme, "有效分钟数（上限 1440）", "60", true, false)
        DemoKit.put(col, fUid, 10)
        DemoKit.put(col, fMin, 10)
        UiKit.customDialog(this, theme, "为用户开启调试模式", col, "开启") {
            val uid = fUid.text.trim()
            if (uid.isEmpty()) {
                tip("UID 不能为空")
                UiKit.keepOpen(); return@customDialog
            }
            val minutes = fMin.text.trim().toLongOrNull() ?: 60L
            io {
                Api.adminPost(
                    server, "/admin/api/client-config/${currentUid()}",
                    JsonObject().apply {
                        add("userDebug", JsonObject().apply {
                            addProperty("uid", uid)
                            addProperty("minutes", minutes)
                        })
                    },
                )
                withContext(Dispatchers.Main) {
                    tip("已开启")
                    refreshCurrentPage()
                }
            }
        }
    }
}
