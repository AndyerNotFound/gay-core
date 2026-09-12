package com.gaycore.app.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.gaycore.app.BuildConfig
import com.gaycore.app.R
import com.gaycore.app.theme.ThemeEngine
import kotlin.math.min
import kotlin.math.roundToInt

   
                        
  
                                             
                                 
  
                                   
   
class UiDemoActivity : BaseActivity() {

    private lateinit var theme: ThemeEngine
    private lateinit var root: FrameLayout
    private lateinit var shellHost: FrameLayout
    private lateinit var content: FrameLayout
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var drawer: LinearLayout
    private lateinit var drawerMenu: LinearLayout
    private lateinit var scrim: View
    private lateinit var convoPanel: LinearLayout
    private lateinit var convoScrim: View
    private lateinit var topAction: FrameLayout
    private lateinit var fab: com.google.android.material.button.MaterialButton
    private var drawerWidth = 0
    private var convoWidth = 0

    private var drawerOpen = false
    private var page = Page.HOME
    private var darkMode = true
    private var selectedAccent = Accent.VIOLET
    private var playgroundMessage: String? = null
    private var modelFilter = 0
    private var pluginTab = 0
    private var modelGrid = false
    private var userTab = 0
    private var noticeLang = 0
    private var userEditMode = false
    private var convoOpen = false
    private var selectedUser = 0

                
    private var homeEditMode = false
    private val homeSections = mutableListOf("server", "tickets", "metrics", "quick", "notice")
    private val homeHidden = mutableSetOf<String>()

                                        
    private var pageView: View? = null
    private var prevPage = Page.HOME

                   
    private enum class Nav {
        UP_IN,                              
        DOWN_IN,                         
        CHILD_IN,             
        CHILD_OUT,                  
        NONE,              
    }

                                 
    private data class DemoUser(
        val uid: String, val balance: String, val up: String, val down: String,
        val requests: String, val note: String, val banned: Boolean = false,
    )

    private val demoUsers = listOf(
        DemoUser("UID 114514", "¥11.45", "↑125M", "↓2M", ">142", "账号异常登录排查"),
        DemoUser("UID 32338", "¥6.20", "↑86M", "↓1M", ">96", "流量配额咨询"),
        DemoUser("UID 219174", "¥3.85", "↑41M", "↓0M", ">57", "模型响应超时反馈"),
    )

    private val demoConvos = mutableListOf(
        "查询活跃用户 SQL", "部署 CloudFlare 反代", "工单 114514 排查", "模型参数对比",
    )

                               
    private data class DemoTicket(val uid: String, val title: String, val time: String, val tone: String)

    private val demoTickets = listOf(
        DemoTicket("UID 114514", "账号异常登录排查", "2分钟前", "primary"),
        DemoTicket("UID 32338", "流量配额咨询", "1小时前", "success"),
        DemoTicket("UID 219174", "模型响应超时反馈", "3小时前", "warning"),
    )

                                                                   
    private data class DemoInstance(
        val name: String, val port: Int, val running: Boolean,
        val requests: Int, val errors: Int, val tls: Boolean,
    )

    private val demoInstances = listOf(
        DemoInstance("default", 16384, true, 1248, 12, true),
        DemoInstance("AgnesAI", 16385, true, 862, 3, false),
        DemoInstance("SOTA-Model", 16386, true, 415, 0, false),
        DemoInstance("free-k3", 16389, false, 0, 0, true),
    )

                         
    private val noticeEn = Pair("Opening", listOf("This is a placeholder paragraph for the announcement body.", "The wavy lines in the sketch are replaced with sample text here."))
    private val noticeCn = Pair("开篇", listOf("这是一段公告正文的占位文字，用于展示排版效果。", "草图中的手写波浪线部分，此处以示例文本代替。"))

    private enum class Page(val title: String, val sub: String, val icon: Int, val inMenu: Boolean = true) {
        HOME("首页", "服务器概览", R.drawable.ic_home),
        MODELS("模型", "模型广场", R.drawable.ic_apps),
        PLUGINS("插件", "插件中心", R.drawable.ic_star),
        USAGE("使用", "用量与排行", R.drawable.ic_wallet),
        PLAYGROUND("Playground", "对话调试", R.drawable.ic_chat),
        USERS("用户管理", "用户与卡密", R.drawable.ic_group),
        INSTANCES("实例", "网关实例", R.drawable.ic_grid),
        PROFILE("个人中心", "资料与卡密", R.drawable.ic_person),
        NOTICE("公告", "公告编辑", R.drawable.ic_edit),
        THEME("主题", "外观与配色", R.drawable.ic_palette),
        SETTINGS("设置", "服务器与隐私", R.drawable.ic_settings),
        MORE("更多", "关于与帮助", R.drawable.ic_more_vert),
        USERDETAIL("用户详情", "资料与操作", R.drawable.ic_person, inMenu = false),
    }

    private enum class Accent(val label: String, val primary: String, val container: String) {
        VIOLET("紫藤", "#7C5CFC", "#E7DEFF"),
        BLUE("晴空", "#2878D8", "#D8E8FF"),
        GREEN("薄荷", "#137B65", "#CDEFE1"),
        ORANGE("琥珀", "#A85B00", "#FFE1B8"),
        RED("莓果", "#B32645", "#FFD9E1"),
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        darkMode = savedInstanceState?.getBoolean("darkMode") ?: true
        selectedAccent = Accent.values()[savedInstanceState?.getInt("accent", Accent.VIOLET.ordinal) ?: Accent.VIOLET.ordinal]
        page = Page.values()[savedInstanceState?.getInt("page", Page.HOME.ordinal) ?: Page.HOME.ordinal]
        modelFilter = savedInstanceState?.getInt("modelFilter", 0) ?: 0
        pluginTab = savedInstanceState?.getInt("pluginTab", 0) ?: 0
        modelGrid = savedInstanceState?.getBoolean("modelGrid") ?: false
        buildShell()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    convoOpen -> closeConvo()
                    drawerOpen -> closeDrawer()
                    page == Page.USERDETAIL -> { page = Page.USERS; renderPage() }
                    page != Page.HOME -> { page = Page.HOME; renderPage() }
                    else -> finish()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("darkMode", darkMode)
        outState.putInt("accent", selectedAccent.ordinal)
        outState.putInt("page", page.ordinal)
        outState.putInt("modelFilter", modelFilter)
        outState.putInt("pluginTab", pluginTab)
        outState.putBoolean("modelGrid", modelGrid)
        outState.putInt("userTab", userTab)
        outState.putInt("noticeLang", noticeLang)
        outState.putInt("selectedUser", selectedUser)
        super.onSaveInstanceState(outState)
    }

    private fun dp(value: Int): Int = UiKit.dp(this, value)
    private fun dp(value: Float): Int = UiKit.dp(this, value)
    private fun tc(name: String): Int = theme.color(this, name)

    private fun txt(t: String, sizeSp: Float = 15f, bold: Boolean = false, colorName: String = "onSurface"): TextView =
        UiKit.text(this, t, sizeSp, bold, tc(colorName))

                                                      

                                                       
    private fun buildShell() {
        theme = makeTheme()
        theme.applyToWindow(this)
        root = FrameLayout(this)
        shellHost = FrameLayout(this)
        root.addView(shellHost, FrameLayout.LayoutParams(-1, -1))

                                       
        fab = UiKit.button(this, theme, "+", "filled")
        fab.textSize = 22f
        fab.setPadding(0, 0, 0, dp(2))
        fab.contentDescription = "新增"
        fab.setOnClickListener { toast("Demo 操作：已模拟新增") }
        root.addView(fab, FrameLayout.LayoutParams(dp(56), dp(56), Gravity.END or Gravity.BOTTOM).apply {
            setMargins(0, 0, dp(20), dp(28))
        })

                          
        scrim = View(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0f
            visibility = View.GONE
            isClickable = true
            isFocusable = true
            setOnClickListener { closeDrawer() }
        }
        root.addView(scrim, FrameLayout.LayoutParams(-1, -1))

                                         
        drawerWidth = min(dp(300), (resources.displayMetrics.widthPixels * 0.84f).roundToInt())
        drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            elevation = dp(8).toFloat()
            translationX = -drawerWidth.toFloat()
        }
        root.addView(drawer, FrameLayout.LayoutParams(drawerWidth, -1))

                                              
        convoScrim = View(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0f
            visibility = View.GONE
            isClickable = true
            isFocusable = true
            setOnClickListener { closeConvo() }
        }
        root.addView(convoScrim, FrameLayout.LayoutParams(-1, -1))
        convoWidth = min(dp(244), (resources.displayMetrics.widthPixels * 0.72f).roundToInt())
        convoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            elevation = dp(8).toFloat()
            translationX = -convoWidth.toFloat()
        }
        root.addView(convoPanel, FrameLayout.LayoutParams(convoWidth, -1))

        setContentView(root)
        applyTheme(false)
    }

       
                 
                                                 
                                 
                               
       
    private fun applyTheme(animate: Boolean) {
        theme = makeTheme()
        theme.applyToWindow(this)
        root.setBackgroundColor(tc("background"))

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(tc("background"))
        }
        column.addView(buildTopBar(), LinearLayout.LayoutParams(-1, dp(72)))
        content = FrameLayout(this)
        column.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        val oldCol = if (shellHost.childCount > 0) shellHost.getChildAt(0) else null
        pageView = null
        if (animate && oldCol != null) {
            column.alpha = 0f
            shellHost.addView(column, FrameLayout.LayoutParams(-1, -1))
            column.animate().alpha(1f).setDuration(260).start()
            oldCol.animate().alpha(0f).setDuration(200).withEndAction { shellHost.removeView(oldCol) }.start()
        } else {
            shellHost.removeAllViews()
            shellHost.addView(column, FrameLayout.LayoutParams(-1, -1))
        }

        drawer.removeAllViews()
        fillDrawer(drawer)
        convoPanel.removeAllViews()
        fillConvoPanel(convoPanel)

        fab.background = Md3.shape(this, tc("primary"), Md3.SHAPE_FULL)
        fab.setTextColor(tc("onPrimary"))

        renderPage(Nav.NONE)
    }

    private fun makeTheme(): ThemeEngine {
        val accent = selectedAccent
        val over = mapOf(
            "primary" to if (darkMode) lighten(accent.primary) else accent.primary,
            "onPrimary" to if (darkMode) "#20133F" else "#FFFFFF",
            "primaryContainer" to if (darkMode) darken(accent.primary) else accent.container,
            "onPrimaryContainer" to if (darkMode) "#F4EEFF" else "#241246",
            "secondary" to if (darkMode) "#D5C6F4" else accent.primary,
            "secondaryContainer" to if (darkMode) "#392E50" else accent.container,
        )
        val info = com.gaycore.app.data.ThemeInfo(
            id = "ui-demo",
            name = "UI Demo",
            dark = if (darkMode) over else null,
            light = if (!darkMode) over else null,
        )
        return ThemeEngine(info, if (darkMode) "dark" else "light", false)
    }

    private fun lighten(hex: String): String = adjust(hex, 1.22f)
    private fun darken(hex: String): String = adjust(hex, 0.42f)
    private fun adjust(hex: String, factor: Float): String {
        val c = Color.parseColor(hex)
        fun channel(v: Int) = (v * factor).roundToInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", channel(Color.red(c)), channel(Color.green(c)), channel(Color.blue(c)))
    }

    private fun buildTopBar(): View {
        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(8), dp(6), dp(6))
            setBackgroundColor(tc("background"))
        }
        bar.addView(iconButton(R.drawable.ic_menu, "打开侧边栏") { openDrawer() }, LinearLayout.LayoutParams(dp(44), dp(48)))
        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleView = txt("", 20f, true)
        subtitleView = txt("", 11.5f, false, "onSurfaceVariant")
        titles.addView(titleView)
        titles.addView(subtitleView, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(1) })
        bar.addView(titles, LinearLayout.LayoutParams(0, -1, 1f).apply { marginStart = dp(6) })
                                                    
        topAction = FrameLayout(this)
        bar.addView(topAction, LinearLayout.LayoutParams(dp(44), dp(48)))
        updateTopBarAction()
        return bar
    }

                       
    private fun updateTopBarAction() {
        if (!::topAction.isInitialized) return
        topAction.removeAllViews()
        val iconRes: Int
        val desc: String
        val click: () -> Unit
        if (page == Page.HOME) {
            iconRes = if (homeEditMode) R.drawable.ic_check else R.drawable.ic_edit
            desc = if (homeEditMode) "完成编辑" else "编辑布局"
            click = {
                val entering = !homeEditMode
                homeEditMode = entering
                                       
                renderPage(if (entering) Nav.UP_IN else Nav.DOWN_IN)
            }
        } else {
            iconRes = R.drawable.ic_refresh
            desc = "刷新"
            click = { toast("Demo 数据已刷新") }
        }
        topAction.addView(iconButton(iconRes, desc, click), FrameLayout.LayoutParams(dp(44), dp(48)))
    }

    private fun fillDrawer(col: LinearLayout) {
        col.setBackgroundColor(tc("surfaceContainer"))

                                    
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(18))
            setBackgroundColor(tc("surfaceContainerHigh"))
        }
        val av = avatar("W", selectedAccent.primary, 50)
        av.isClickable = true
        av.isFocusable = true
        av.setOnClickListener { page = Page.PROFILE; renderPage(); closeDrawer() }
        head.addView(av, LinearLayout.LayoutParams(dp(50), dp(50)))
        head.addView(txt("WetherFlar", 17f, true), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })
        head.addView(txt("waterflar@gmail.com", 11.5f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3) })
        head.addView(txt("UID: Admin", 12f, true, "onSurfaceVariant"), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(10) })
        col.addView(head, LinearLayout.LayoutParams(-1, -2))

                      
        drawerMenu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val menuScroll = ScrollView(this).apply { isFillViewport = false }
        menuScroll.addView(drawerMenu, FrameLayout.LayoutParams(-1, -2))
        val pad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(8))
        }
        pad.addView(menuScroll, LinearLayout.LayoutParams(-1, -2))
        col.addView(pad, LinearLayout.LayoutParams(-1, 0, 1f))

                              
        val foot = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(18))
        }
        val switchSrv = UiKit.button(this, theme, "切换服务器", "outlined")
        switchSrv.setOnClickListener { toast("Demo 操作：切换服务器") }
        foot.addView(switchSrv, LinearLayout.LayoutParams(0, dp(44), 1f))
        val themeBtn = FrameLayout(this).apply {
            background = rounded(tc("surfaceContainerHighest"), 100)
            isClickable = true
            isFocusable = true
            contentDescription = "切换明暗"
            setOnClickListener { darkMode = !darkMode; rebuildTheme() }
        }
        val themeIcon = ImageView(this).apply {
                                          
            setImageResource(if (darkMode) R.drawable.ic_light_mode else R.drawable.ic_dark_mode)
            setImageTintList(ColorStateList.valueOf(tc("onSurfaceVariant")))
        }
        themeBtn.addView(themeIcon, FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
        foot.addView(themeBtn, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(10) })
        col.addView(foot, LinearLayout.LayoutParams(-1, -2))

        rebuildDrawerMenu()
    }

    private fun rebuildDrawerMenu() {
        if (!::drawerMenu.isInitialized) return
        drawerMenu.removeAllViews()
        for (p in Page.values()) {
            if (!p.inMenu) continue
            val selected = p == page
            val item = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(13), dp(16), dp(13))
                isClickable = true
                isFocusable = true
                background = rounded(if (selected) tc("primaryContainer") else Color.TRANSPARENT, 100)
                setOnClickListener {
                    page = p
                    renderPage()
                    closeDrawer()
                }
            }
            val ic = ImageView(this).apply {
                setImageResource(p.icon)
                setImageTintList(ColorStateList.valueOf(if (selected) tc("primary") else tc("onSurfaceVariant")))
            }
            item.addView(ic, LinearLayout.LayoutParams(dp(22), dp(22)))
            val label = txt(p.title, 14f, selected, if (selected) "primary" else "onSurfaceVariant")
            item.addView(label, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) })
            drawerMenu.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(2) })
        }
    }

    private fun openDrawer() {
        if (convoOpen) closeConvo()
        drawerOpen = true
        scrim.visibility = View.VISIBLE
        scrim.animate().alpha(0.6f).setDuration(320).setInterpolator(DecelerateInterpolator()).start()
        drawer.animate().translationX(0f).setDuration(340).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun closeDrawer() {
        if (!drawerOpen) return
        drawerOpen = false
        val dw = if (drawer.width > 0) drawer.width else dp(300)
        drawer.animate().translationX(-dw.toFloat()).setDuration(260).setInterpolator(AccelerateInterpolator()).start()
        scrim.animate().alpha(0f).setDuration(250).withEndAction { scrim.visibility = View.GONE }.start()
    }

                                                          

    private fun fillConvoPanel(col: LinearLayout) {
        col.setBackgroundColor(tc("surfaceContainer"))
        val head = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(10), dp(12))
        }
        head.addView(txt("最近对话", 15f, true), LinearLayout.LayoutParams(0, -2, 1f))
        val close = iconButton(R.drawable.ic_close, "收起") { closeConvo() }
        head.addView(close, LinearLayout.LayoutParams(dp(34), dp(34)))
        col.addView(head, LinearLayout.LayoutParams(-1, -2))
        col.addView(divider(), LinearLayout.LayoutParams(-1, dp(1)))

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        if (demoConvos.isEmpty()) {
            list.addView(txt("(暂无对话)", 12.5f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(-1, -2))
        }
        demoConvos.forEachIndexed { index, title ->
            val item = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(8), dp(12))
                background = roundedOutline(
                    if (index == 0) tc("primaryContainer") else tc("surfaceContainerLowest"),
                    if (index == 0) tc("primary") else tc("outlineVariant"),
                    12,
                )
                isClickable = true
                isFocusable = true
                setOnClickListener { toast("已切换到：$title"); closeConvo() }
            }
            item.addView(
                txt(title, 12.5f, index == 0, if (index == 0) "primary" else "onSurfaceVariant"),
                LinearLayout.LayoutParams(0, -2, 1f),
            )
            val del = TextView(this).apply {
                text = "✕"
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(tc("onSurfaceVariant"))
                background = roundedOutline(Color.TRANSPARENT, tc("outlineVariant"), 8)
                isClickable = true
                isFocusable = true
                contentDescription = "删除对话"
                setOnClickListener {
                    demoConvos.removeAt(index)
                    buildConvoPanelInto()
                    toast("已删除对话：$title")
                }
            }
            item.addView(del, LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginStart = dp(6) })
            list.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }
        col.addView(list, LinearLayout.LayoutParams(-1, -2))
    }

                           
    private fun buildConvoPanelInto() {
        if (!::convoPanel.isInitialized) return
        convoPanel.removeAllViews()
        fillConvoPanel(convoPanel)
    }

    private fun openConvo() {
        if (!::convoPanel.isInitialized) return
        if (drawerOpen) closeDrawer()
        convoOpen = true
        convoScrim.visibility = View.VISIBLE
        convoScrim.animate().alpha(0.5f).setDuration(280).setInterpolator(DecelerateInterpolator()).start()
        convoPanel.animate().translationX(0f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun closeConvo() {
        if (!convoOpen) return
        convoOpen = false
        val cw = if (convoPanel.width > 0) convoPanel.width else dp(244)
        convoPanel.animate().translationX(-cw.toFloat()).setDuration(240).setInterpolator(AccelerateInterpolator()).start()
        convoScrim.animate().alpha(0f).setDuration(220).withEndAction { convoScrim.visibility = View.GONE }.start()
    }

    private fun divider(): View = View(this).apply {
        setBackgroundColor(tc("outlineVariant"))
        layoutParams = LinearLayout.LayoutParams(-1, dp(1))
    }

                              
    private fun autoNav(from: Page, to: Page): Nav {
        if (from == to) return Nav.NONE
        if (to == Page.USERDETAIL) return Nav.CHILD_IN
        if (from == Page.USERDETAIL) return Nav.CHILD_OUT
        val menu = Page.values().filter { it.inMenu }
        val a = menu.indexOf(from)
        val b = menu.indexOf(to)
        if (a < 0 || b < 0) return Nav.NONE
        return if (b > a) Nav.UP_IN else Nav.DOWN_IN
    }

                                    
    private fun swapContent(newView: View, nav: Nav) {
        val lp = FrameLayout.LayoutParams(-1, -1)
        val old = pageView
                                   
        var i = 0
        while (i < content.childCount) {
            if (content.getChildAt(i) === old) i++ else content.removeViewAt(i)
        }
        if (old == null || nav == Nav.NONE) {
            if (old != null) content.removeView(old)
            content.addView(newView, lp)
            pageView = newView
            return
        }
        val w = (if (content.width > 0) content.width else resources.displayMetrics.widthPixels).toFloat()
        val h = dp(26).toFloat()
        when (nav) {
            Nav.UP_IN, Nav.DOWN_IN -> {
                val up = nav == Nav.UP_IN
                newView.alpha = 0f
                newView.translationY = if (up) h else -h
                content.addView(newView, lp)
                newView.animate().alpha(1f).translationY(0f)
                    .setDuration(280).setInterpolator(DecelerateInterpolator()).start()
                old.animate().alpha(0f).translationY(if (up) -h else h)
                    .setDuration(240).setInterpolator(AccelerateInterpolator())
                    .withEndAction { if (old.parent === content) content.removeView(old) }.start()
            }
            Nav.CHILD_IN -> {
                newView.translationX = w
                content.addView(newView, lp)
                newView.animate().translationX(0f)
                    .setDuration(300).setInterpolator(DecelerateInterpolator()).start()
                                             
                old.animate().translationX(-w * 0.24f)
                    .setDuration(300).setInterpolator(DecelerateInterpolator()).start()
            }
            Nav.CHILD_OUT -> {
                i = 0
                while (i < content.childCount) {
                    if (content.getChildAt(i) === old) i++ else content.removeViewAt(i)
                }
                content.removeView(old)
                content.addView(newView, lp)
                content.addView(old, FrameLayout.LayoutParams(-1, -1))
                old.translationX = 0f
                old.animate().translationX(w)
                    .setDuration(300).setInterpolator(AccelerateInterpolator())
                    .withEndAction { if (old.parent === content) content.removeView(old) }.start()
            }
            Nav.NONE -> {}
        }
        pageView = newView
    }

    private fun renderPage(nav: Nav? = null) {
        if (!::content.isInitialized) return
        if (page != Page.HOME) homeEditMode = false
        val target = nav ?: autoNav(prevPage, page)
        titleView.text = if (page == Page.USERDETAIL) demoUsers[selectedUser].uid else page.title
        subtitleView.text = when (page) {
            Page.HOME -> "${page.sub} · ${selectedAccent.label}"
            Page.PLAYGROUND -> "本地交互预览，不会发送请求"
            Page.USERDETAIL -> "用户资料与操作"
            Page.USERS -> if (userTab == 0) "用户列表 · 长按或点 i 查看详情" else "卡密列表"
            Page.INSTANCES -> "单进程托管 ${demoInstances.size} 个实例 · ${demoInstances.count { it.running }} 个运行中"
            Page.NOTICE -> "中英双语公告草稿"
            else -> page.sub
        }
        val body = when (page) {
            Page.HOME -> homePage()
            Page.MODELS -> modelsPage()
            Page.PLUGINS -> pluginsPage()
            Page.USAGE -> usagePage()
            Page.PLAYGROUND -> playgroundPage()
            Page.USERS -> usersPage()
            Page.INSTANCES -> instancesPage()
            Page.USERDETAIL -> userDetailPage()
            Page.NOTICE -> noticePage()
            Page.PROFILE -> profilePage()
            Page.THEME -> themePage()
            Page.SETTINGS -> settingsPage()
            Page.MORE -> morePage()
        }
        swapContent(body, target)
        prevPage = page
        fab.visibility = if (page == Page.HOME || page == Page.MODELS || page == Page.PLUGINS || page == Page.USERS) View.VISIBLE else View.GONE
        rebuildDrawerMenu()
        updateTopBarAction()
    }

                                                        

    private fun pageScroll(contentView: View): ScrollView {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(24))
        }
        scroll.addView(contentView, FrameLayout.LayoutParams(-1, -2))
        return scroll
    }

    private fun pageColumn(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(16))
        setBackgroundColor(tc("background"))
    }

    private fun put(parent: LinearLayout, child: View, gap: Int = 0, width: Int = -1, height: Int = -2, weight: Float = 0f) {
        val lp = if (weight > 0f) LinearLayout.LayoutParams(0, height, weight) else LinearLayout.LayoutParams(width, height)
        if (gap > 0 && parent.childCount > 0) lp.topMargin = dp(gap)
        parent.addView(child, lp)
    }

                                                      

    private fun homeSectionName(id: String): String = when (id) {
        "server" -> "服务器资料卡"
        "tickets" -> "工单"
        "metrics" -> "服务状态"
        "quick" -> "快速入口"
        "notice" -> "公告"
        else -> id
    }

    private fun homeSectionIcon(id: String): Int = when (id) {
        "server" -> R.drawable.ic_home
        "tickets" -> R.drawable.ic_chat
        "metrics" -> R.drawable.ic_grid
        "quick" -> R.drawable.ic_apps
        "notice" -> R.drawable.ic_info
        else -> R.drawable.ic_apps
    }

                                     
    private fun moveHomeSection(index: Int, delta: Int) {
        val to = index + delta
        if (to < 0 || to >= homeSections.size) return
        val item = homeSections.removeAt(index)
        homeSections.add(to, item)
        renderPage(Nav.NONE)
    }

                         
    private fun openTicket(uid: String) {
        val idx = demoUsers.indexOfFirst { it.uid == uid }
        if (idx >= 0) openUserDetail(idx) else toast("Demo：该工单暂无可关联用户")
    }

    private fun homePage(): View {
        if (homeEditMode) return homeLayoutEditor()
        val col = pageColumn()
        var shown = 0
        for (id in homeSections) {
            if (id in homeHidden) continue
            val gap = if (shown == 0) 2 else 20
            shown++
            when (id) {
                "server" -> put(col, serverCard(), gap)
                "tickets" -> {
                    put(col, sectionTitle("工单", "${demoTickets.size} 条待处理 · 点卡片进入处理"), gap)
                    demoTickets.forEach { put(col, ticketRow(it), 8) }
                }
                "metrics" -> {
                    put(col, sectionTitle("服务状态", "运行中 · 点卡片查看用量"), gap)
                    val metrics = LinearLayout(this).apply { gravity = Gravity.CENTER }
                    metrics.addView(metricTile("114M", "上行", "↑") { page = Page.USAGE; renderPage() }, LinearLayout.LayoutParams(0, dp(104), 1f))
                    metrics.addView(metricTile("21M", "下行", "↓") { page = Page.USAGE; renderPage() }, LinearLayout.LayoutParams(0, dp(104), 1f).apply { marginStart = dp(8) })
                    metrics.addView(metricTile("116", "延迟 ms", "◷") { page = Page.USAGE; renderPage() }, LinearLayout.LayoutParams(0, dp(104), 1f).apply { marginStart = dp(8) })
                    put(col, metrics, 10)
                }
                "quick" -> {
                    put(col, sectionTitle("快速入口", "常用操作"), gap)
                    val actions = LinearLayout(this).apply { gravity = Gravity.CENTER }
                    actions.addView(actionTile("模型广场", R.drawable.ic_apps) { page = Page.MODELS; renderPage() }, LinearLayout.LayoutParams(0, dp(88), 1f))
                    actions.addView(actionTile("Playground", R.drawable.ic_chat) { page = Page.PLAYGROUND; renderPage() }, LinearLayout.LayoutParams(0, dp(88), 1f).apply { marginStart = dp(8) })
                    actions.addView(actionTile("插件中心", R.drawable.ic_star) { page = Page.PLUGINS; renderPage() }, LinearLayout.LayoutParams(0, dp(88), 1f).apply { marginStart = dp(8) })
                    put(col, actions, 10)
                }
                "notice" -> {
                    put(col, sectionTitle("公告", "2026/09/01 · 点卡片去编辑"), gap)
                    val data = if (noticeLang == 0) noticeEn else noticeCn
                    put(col, noticeCard("2026/09/01 10:01", data.first, data.second.firstOrNull() ?: "") {
                        page = Page.NOTICE
                        renderPage()
                    }, 8)
                }
            }
        }
        if (shown == 0) {
            put(col, txt("所有板块都被隐藏了 —— 点右上角 ✎ 重新打开。", 13f, false, "onSurfaceVariant"), 40, -1, -2)
        }
        return pageScroll(col)
    }

                                    
    private fun homeLayoutEditor(): View {
        val col = pageColumn()
        put(col, sectionTitle("编辑首页布局", "调整板块顺序与显示状态"), 2)
        val tips = panel(16)
        put(tips, txt("用 ▲ ▼ 调整顺序，右侧开关控制显示 / 隐藏。修改立即生效，点右上角 ✓ 完成。", 12.5f, false, "onSurfaceVariant"), 0, -1, -2)
        put(col, tips, 12)

        homeSections.forEachIndexed { index, id ->
            val card = panel(16)
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(iconBadge(homeSectionIcon(id), selectedAccent.primary, 34), LinearLayout.LayoutParams(dp(34), dp(34)))
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            put(info, txt(homeSectionName(id), 14f, true), 0, -2, -2)
            put(info, txt(if (id in homeHidden) "已隐藏" else "显示中", 11f, false, "onSurfaceVariant"), 2, -2, -2)
            row.addView(info, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })

            val up = iconButton(R.drawable.ic_chevron_up, "上移") { moveHomeSection(index, -1) }
            up.alpha = if (index == 0) 0.3f else 1f
            up.isEnabled = index > 0
            row.addView(up, LinearLayout.LayoutParams(dp(30), dp(30)))

            val down = iconButton(R.drawable.ic_chevron_down, "下移") { moveHomeSection(index, 1) }
            down.alpha = if (index == homeSections.size - 1) 0.3f else 1f
            down.isEnabled = index < homeSections.size - 1
            row.addView(down, LinearLayout.LayoutParams(dp(30), dp(30)).apply { marginStart = dp(4) })

            val sw = Switch(this).apply {
                isChecked = id !in homeHidden
                setOnCheckedChangeListener { _, checked ->
                    if (checked) homeHidden.remove(id) else homeHidden.add(id)
                    renderPage(Nav.NONE)
                }
            }
            row.addView(sw, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
            put(card, row, 0, -1, -2)
            put(col, card, 8)
        }

        val acts = LinearLayout(this)
        acts.addView(smallButton("全部显示", "outlined") { homeHidden.clear(); renderPage(Nav.NONE) }, LinearLayout.LayoutParams(0, dp(44), 1f))
        acts.addView(smallButton("完成", "filled") { homeEditMode = false; renderPage(Nav.DOWN_IN) }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(10) })
        put(col, acts, 16)
        return pageScroll(col)
    }

    private fun serverCard(): View {
        val card = panel(16)
        val gd = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(tc("primaryContainer"), tc("surfaceContainer")),
        )
        gd.cornerRadius = dp(16).toFloat()
        card.background = gd
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(iconBadge(R.drawable.ic_refresh, selectedAccent.primary, 46), LinearLayout.LayoutParams(dp(46), dp(46)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        put(info, txt("服务器名称", 11f, false, "onSurfaceVariant"), 0, -2, -2)
        put(info, txt("WetherFlar站", 19f, true), 3, -2, -2)
        put(info, txt("● 正常运行", 11.5f, true, "success"), 5, -2, -2)
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) })
        put(card, row, 0, -1, -2)
        return card
    }

    private fun ticketRow(t: DemoTicket): View {
        val card = panel(14).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { openTicket(t.uid) }
        }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val dot = View(this).apply { background = rounded(toneColor(t.tone), 100) }
        row.addView(dot, LinearLayout.LayoutParams(dp(9), dp(9)))
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        put(body, txt(t.uid, 13.5f, true), 0, -2, -2)
        put(body, txt(t.title, 11.5f, false, "onSurfaceVariant"), 2, -2, -2)
        row.addView(body, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        row.addView(txt(t.time, 11f, false, "onSurfaceVariant"))
        val chev = ImageView(this).apply {
            setImageResource(R.drawable.ic_chevron_right)
            setImageTintList(ColorStateList.valueOf(tc("outline")))
        }
        row.addView(chev, LinearLayout.LayoutParams(dp(18), dp(18)).apply { marginStart = dp(4) })
        put(card, row, 0, -1, -2)
        return card
    }

    private fun toneColor(tone: String): Int = when (tone) {
        "success" -> tc("success")
        "warning" -> tc("tertiary")
        "error" -> tc("error")
        else -> tc("primary")
    }

    private fun noticeCard(date: String, title: String, body: String, onClick: (() -> Unit)? = null): View {
        val card = panel(16)
        put(card, txt(date, 10.5f, true, "onSurfaceVariant"), 0, -2, -2)
        put(card, txt(title, 15f, true), 7, -2, -2)
        put(card, txt(body, 12f, false, "onSurfaceVariant"), 6, -2, -2)
        if (onClick != null) {
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener { onClick() }
            val hint = LinearLayout(this).apply { gravity = Gravity.END }
            val chev = ImageView(this).apply {
                setImageResource(R.drawable.ic_chevron_right)
                setImageTintList(ColorStateList.valueOf(tc("outline")))
            }
            hint.addView(chev, LinearLayout.LayoutParams(dp(18), dp(18)))
            put(card, hint, 8, -1, -2)
        }
        return card
    }

                                                        

    private fun modelsPage(): View {
        val col = pageColumn()
        put(col, searchBar("搜索模型名称…"), 2)
        put(col, segmented(listOf("全部", "对话", "视觉", "推理"), modelFilter) { modelFilter = it; renderPage() }, 12)

        val tools = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        tools.addView(chip(if (modelGrid) "列表" else "网格", R.drawable.ic_grid, modelGrid) { modelGrid = !modelGrid; renderPage() })
        tools.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        tools.addView(txt("共 4 个模型", 11f, false, "onSurfaceVariant"))
        put(col, tools, 12)

        val all = listOf(
            Triple("deepseek-chat", "DeepSeek", "适合日常对话与代码"),
            Triple("Qwen/Qwen3-8B", "SiliconFlow", "轻量、快速、低成本"),
            Triple("gpt-4o-mini", "OpenAI", "通用多模态模型"),
            Triple("claude-3-5-sonnet", "Anthropic", "长上下文与复杂推理"),
        )
        val accents = listOf(Accent.VIOLET, Accent.BLUE, Accent.GREEN, Accent.ORANGE)
        val prices = listOf("¥1 / M", "¥0.25 / M", "¥8 / M", "¥15 / M")
        val tags = listOf("稳定", "快速", "视觉", "长上下文")
        val tones = listOf("success", "primary", "warning", "neutral")
        if (modelGrid) {
            var i = 0
            while (i < all.size) {
                val row = LinearLayout(this).apply { gravity = Gravity.TOP }
                for (c in 0 until 2) {
                    if (i >= all.size) break
                    val item = modelGridCard(all[i].first, all[i].second, prices[i], tags[i], tones[i], accents[i])
                    row.addView(item, LinearLayout.LayoutParams(0, -2, 1f).apply { if (c == 1) marginStart = dp(8) })
                    i++
                }
                put(col, row, 10)
            }
        } else {
            for (idx in all.indices) {
                put(col, modelCard(all[idx].first, all[idx].second, all[idx].third, prices[idx], tags[idx], tones[idx], accents[idx]), 8)
            }
        }
        return pageScroll(col)
    }

    private fun modelCard(model: String, provider: String, desc: String, price: String, tag: String, tone: String, accent: Accent): View {
        val card = panel(16)
        val line = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        line.addView(avatar(model.take(1).uppercase(), accent.primary, 42), LinearLayout.LayoutParams(dp(42), dp(42)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        put(info, txt(model, 15f, true), 0, -2, -2)
        put(info, txt("$provider · $desc", 11.5f, false, "onSurfaceVariant"), 3, -2, -2)
        line.addView(info, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        line.addView(badge(tag, tone))
        put(card, line, 0, -1, -2)
        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        bottom.addView(txt("输入 $price", 12f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(0, -2, 1f))
        bottom.addView(smallButton("试用", "tonal") { page = Page.PLAYGROUND; renderPage() }, LinearLayout.LayoutParams(dp(78), dp(38)))
        put(card, bottom, 12, -1, -2)
        return card
    }

    private fun modelGridCard(model: String, provider: String, price: String, tag: String, tone: String, accent: Accent): View {
        val card = panel(14)
        card.addView(iconBadge(R.drawable.ic_apps, accent.primary, 36), LinearLayout.LayoutParams(dp(36), dp(36)))
        put(card, txt(model, 13f, true), 10, -1, -2)
        put(card, badge(tag, tone), 6, -2, -2)
        put(card, txt(provider, 11f, false, "onSurfaceVariant"), 6, -1, -2)
        put(card, txt(price, 11.5f, true, "primary"), 4, -1, -2)
        return card
    }

                                                        

    private fun pluginsPage(): View {
        val col = pageColumn()
        put(col, searchBar("搜索插件…"), 2)
        put(col, segmented(listOf("全部", "内置", "外部"), pluginTab) { pluginTab = it; renderPage() }, 12)
        put(col, sectionTitle("插件中心", "已安装 4 个插件"), 12)
        put(col, pluginCard("用户系统", "账号、资料与卡密管理", "auth-user", R.drawable.ic_person, Accent.VIOLET, true), 8)
        put(col, pluginCard("每日签到", "签到领取额度，连续签到有奖励", "signin", R.drawable.ic_check, Accent.GREEN, true), 8)
        put(col, pluginCard("用量统计", "查看请求、额度与渠道分析", "usage-stats", R.drawable.ic_wallet, Accent.BLUE, true), 8)
        put(col, pluginCard("主题工坊", "切换主题与自定义颜色", "theme", R.drawable.ic_palette, Accent.ORANGE, false), 8)
        return pageScroll(col)
    }

    private fun pluginCard(name: String, desc: String, id: String, icon: Int, accent: Accent, enabled: Boolean): View {
        val card = panel(16)
        val line = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        line.addView(iconBadge(icon, accent.primary), LinearLayout.LayoutParams(dp(44), dp(44)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        put(info, txt(name, 15f, true), 0, -2, -2)
        put(info, txt(desc, 11.5f, false, "onSurfaceVariant"), 3, -2, -2)
        line.addView(info, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        line.addView(badge(if (enabled) "已启用" else "已停用", if (enabled) "success" else "neutral"))
        put(card, line, 0, -1, -2)
        val foot = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        foot.addView(txt("版本 1.0.0 · SHA256 已校验", 10.5f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(0, -2, 1f))
        foot.addView(chip("打开", R.drawable.ic_chevron_right, false) { toast("Demo：打开插件 $id") })
        put(card, foot, 12, -1, -2)
        return card
    }

                                                        

    private fun usagePage(): View {
        val col = pageColumn()
        put(col, sectionTitle("使用情况", "最近 7 天"), 2)
        val summary = LinearLayout(this).apply { gravity = Gravity.CENTER }
        summary.addView(metricTile("72.4K", "已用 tokens", "◈"), LinearLayout.LayoutParams(0, dp(104), 1f))
        summary.addView(metricTile("1,248", "总请求", "↻"), LinearLayout.LayoutParams(0, dp(104), 1f).apply { marginStart = dp(8) })
        put(col, summary, 12)

        val chart = panel(16)
        put(chart, txt("请求趋势", 16f, true), 0, -1, -2)
        put(chart, txt("按天统计", 12f, false, "onSurfaceVariant"), 3, -1, -2)
        put(chart, barChart(), 16, -1, -2)
        put(col, chart, 12)

        put(col, sectionTitle("渠道排行", "按请求数排序"), 18)
        put(col, rankRow("DeepSeek", "1,024", "82%", Accent.VIOLET, 0.82f), 8)
        put(col, rankRow("SiliconFlow", "186", "15%", Accent.BLUE, 0.15f), 8)
        put(col, rankRow("OpenAI", "38", "3%", Accent.GREEN, 0.03f), 8)
        return pageScroll(col)
    }

                                                              

    private fun playgroundPage(): View {
        val col = pageColumn()
        val model = panel(16)
        put(model, txt("当前模型", 12f, false, "onSurfaceVariant"), 0, -1, -2)
        val modelRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        modelRow.addView(txt("deepseek-chat", 17f, true), LinearLayout.LayoutParams(0, -2, 1f))
        modelRow.addView(badge("在线", "success"))
        put(model, modelRow, 5, -1, -2)
        val tools = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        tools.addView(chip("推理强度 High", null, true) { toast("Demo：切换推理强度") })
        tools.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        tools.addView(chip("最近对话", R.drawable.ic_chat, false) { openConvo() })
        put(model, tools, 12, -1, -2)
        put(col, model, 2)

        val chat = panel(16)
        chat.minimumHeight = dp(260)
        put(chat, txt("消息预览", 15f, true), 0, -1, -2)
        put(chat, chatBubble("你好，我是 Gay Core 的 Demo 模型。", false), 16)
        put(chat, chatBubble(playgroundMessage ?: "在下方输入内容，点击发送即可模拟一次响应。", true), 10)
        put(col, chat, 12)

        val input = EditText(this).apply {
            hint = "输入一条消息…"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            maxLines = 4
            setTextColor(tc("onSurface"))
            setHintTextColor(tc("onSurfaceVariant"))
            background = roundedOutline(tc("surfaceContainerHighest"), tc("outlineVariant"), 16)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        put(col, input, 6)
        put(col, smallButton("发送（模拟）", "filled") {
            playgroundMessage = input.text.toString().trim().ifEmpty { "Demo 已收到你的消息。" }
            renderPage()
        }, 8)
        return pageScroll(col)
    }

    private fun chatBubble(text: String, mine: Boolean): View {
        val tv = txt(text, 13.5f, false, if (mine) "onPrimaryContainer" else "onSurface")
        tv.setPadding(dp(12), dp(10), dp(12), dp(10))
        tv.background = rounded(if (mine) tc("primaryContainer") else tc("surfaceContainerHighest"), 16)
        val wrap = LinearLayout(this).apply { gravity = if (mine) Gravity.END else Gravity.START }
        wrap.addView(tv, LinearLayout.LayoutParams((resources.displayMetrics.widthPixels * 0.76f).roundToInt(), -2))
        return wrap
    }

                                                        

    private fun usersPage(): View {
        val col = pageColumn()
        put(col, segmented(listOf("用户", "卡密"), userTab) { userTab = it; renderPage() }, 2)

        if (userTab == 1) {
            put(col, searchBar("搜索卡号或卡名…"), 12)
            put(col, txt("共 4 张卡", 11f, false, "onSurfaceVariant"), 10, -1, -2)
            put(col, keyCard("主卡", "sk-gc••••••••9f2a", "72.4K / 不限", "不限额度", "primary"), 8)
            put(col, keyCard("子卡 · 114514", "sk-sub••••••••3c71", "12.0K / 50K", "已启用", "success"), 8)
            put(col, keyCard("测试卡", "sk-test••••••••1a8c", "0 / 10K", "零额度", "neutral"), 8)
            put(col, keyCard("荒野卡", "sk-orphan••••••4d20", "—", "未绑定", "warning"), 8)
        } else {
            put(col, searchBar("搜索 UID 或昵称…"), 12)
            put(col, txt("共 ${demoUsers.size} 条记录 · 长按卡片或点 i 查看详情", 11f, false, "onSurfaceVariant"), 10, -1, -2)
            demoUsers.forEachIndexed { index, u -> put(col, userCard(u, index), 10) }
        }
        return pageScroll(col)
    }

    private fun userCard(u: DemoUser, index: Int): View {
        val card = panel(16).apply {
            isClickable = true
            isFocusable = true
            setOnLongClickListener { openUserDetail(index); true }
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(avatar(u.uid.takeLast(2), selectedAccent.primary, 28), LinearLayout.LayoutParams(dp(28), dp(28)))
        top.addView(txt(u.uid, 15f, true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        if (u.banned) top.addView(badge("已封禁", "error"))
        top.addView(iconButton(R.drawable.ic_info, "详情") { openUserDetail(index) }, LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(6) })
        put(card, top, 0, -1, -2)

        val stats = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        stats.addView(txt(u.balance, 13f, true), LinearLayout.LayoutParams(-2, -2))
        stats.addView(txt("   ${u.up}   ${u.down}   ${u.requests}", 12f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(0, -2, 1f))
        put(card, stats, 10, -1, -2)

        val acts = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        acts.addView(iconButton(R.drawable.ic_chat, "推送通知") { toast("Demo：单独推送通知") }, LinearLayout.LayoutParams(dp(32), dp(32)))
        acts.addView(iconButton(R.drawable.ic_wallet, "调整余额") { toast("Demo：调整余额") }, LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(8) })
        acts.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        acts.addView(iconButton(R.drawable.ic_block, "封禁用户") { toast("Demo：封禁用户") }, LinearLayout.LayoutParams(dp(32), dp(32)))
        acts.addView(iconButton(R.drawable.ic_delete, "删除用户") { toast("Demo：删除用户") }, LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginStart = dp(8) })
        put(card, acts, 12, -1, -2)
        return card
    }

    private fun openUserDetail(index: Int) {
        selectedUser = index
        userEditMode = false
        page = Page.USERDETAIL
        renderPage()
    }

                                                        

    private fun userDetailPage(): View {
        val u = demoUsers[selectedUser]
        val col = pageColumn()

        val head = panel(16)
        val hrow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        hrow.addView(avatar(u.uid.takeLast(2), selectedAccent.primary, 56), LinearLayout.LayoutParams(dp(56), dp(56)))
        val htxt = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        put(htxt, txt(u.uid, 19f, true), 0, -2, -2)
        put(htxt, badge(if (u.banned) "已封禁" else "正常", if (u.banned) "error" else "success"), 8, -2, -2)
        hrow.addView(htxt, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        hrow.addView(chip(if (userEditMode) "取消" else "编辑", R.drawable.ic_edit, userEditMode) {
            userEditMode = !userEditMode
            renderPage()
        })
        put(head, hrow, 0, -1, -2)
        put(col, head, 2)

        val fields = panel(16)
        put(fields, txt("基本资料", 15f, true), 0, -1, -2)
        if (userEditMode) {
            put(fields, txt("点按下方字段直接修改，改完点「保存」。", 11.5f, false, "onSurfaceVariant"), 5, -1, -2)
        }
        put(fields, editableField("UID", u.uid, userEditMode), 14, -1, -2)
        put(fields, editableField("余额", u.balance, userEditMode), 12, -1, -2)
        put(fields, editableField("备注", u.note, userEditMode), 12, -1, -2)
        put(fields, editableField("状态", if (u.banned) "已封禁" else "正常", userEditMode), 12, -1, -2)
        if (userEditMode) {
            val saveRow = LinearLayout(this)
            saveRow.addView(smallButton("取消", "outlined") { userEditMode = false; renderPage() }, LinearLayout.LayoutParams(0, dp(44), 1f))
            saveRow.addView(smallButton("保存", "filled") { userEditMode = false; renderPage(); toast("Demo：已保存 ${u.uid}") }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(10) })
            put(fields, saveRow, 16, -1, -2)
        }
        put(col, fields, 14)

        put(col, txt("用户操作", 12f, true, "onSurfaceVariant"), 20, -1, -2)
        put(col, menuCard("重置密码", "向该用户发送重置链接", R.drawable.ic_key) { toast("Demo：重置密码") }, 8)
        put(col, menuCard("修改用户邮箱", "当前未绑定邮箱", R.drawable.ic_edit) { toast("Demo：修改邮箱") }, 8)
        put(col, menuCard("强制下线", "使其所有会话立即失效", R.drawable.ic_refresh) { toast("Demo：强制下线") }, 8)
        put(col, menuCard("重置流量", "上行 / 下行清零", R.drawable.ic_wallet) { toast("Demo：重置流量") }, 8)
        put(col, menuCard("调整到期时间", "当前：长期有效", R.drawable.ic_schedule) { toast("Demo：调整到期时间") }, 8)
        put(col, dangerCard("封禁用户", "封禁后该用户无法登录，可随时解封", R.drawable.ic_block) { toast("Demo：封禁用户") }, 8)
        return pageScroll(col)
    }

    private fun editableField(label: String, value: String, editable: Boolean): View {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(txt(label, 12.5f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(dp(52), -2))
        if (editable) {
            val et = EditText(this).apply {
                setText(value)
                textSize = 13.5f
                setTextColor(tc("onSurface"))
                background = roundedOutline(tc("surfaceContainerHighest"), tc("primary"), 10)
                setPadding(dp(10), dp(8), dp(10), dp(8))
            }
            row.addView(et, LinearLayout.LayoutParams(0, -2, 1f))
        } else {
            row.addView(txt(value, 13.5f, false, "onSurface"), LinearLayout.LayoutParams(0, -2, 1f))
        }
        return row
    }

    private fun dangerCard(title: String, desc: String, icon: Int, onClick: () -> Unit): View {
        val card = panel(16).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val box = FrameLayout(this).apply { background = rounded(tc("errorContainer"), 14) }
        val iv = ImageView(this).apply {
            setImageResource(icon)
            setImageTintList(ColorStateList.valueOf(tc("error")))
        }
        box.addView(iv, FrameLayout.LayoutParams(dp(19), dp(19), Gravity.CENTER))
        row.addView(box, LinearLayout.LayoutParams(dp(42), dp(42)))
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        put(text, txt(title, 14f, true, "error"), 0, -2, -2)
        put(text, txt(desc, 11.5f, false, "onSurfaceVariant"), 3, -2, -2)
        row.addView(text, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        val chev = ImageView(this).apply {
            setImageResource(R.drawable.ic_chevron_right)
            setImageTintList(ColorStateList.valueOf(tc("error")))
        }
        row.addView(chev, LinearLayout.LayoutParams(dp(20), dp(20)))
        put(card, row, 0, -1, -2)
        return card
    }

                                                        

    private fun noticePage(): View {
        val col = pageColumn()
        put(col, sectionTitle("公告草稿", if (noticeLang == 0) "英文版" else "中文版"), 2)
        put(col, noticeEditor(), 12)
        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        actions.addView(smallButton("+ 时间", "outlined") { toast("Demo：已插入今天日期") }, LinearLayout.LayoutParams(0, dp(44), 1f))
        actions.addView(smallButton("+ 公告", "filled") { toast("Demo：已新建公告草稿") }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(10) })
        put(col, actions, 16)
        return pageScroll(col)
    }

    private fun noticeEditor(): View {
        val card = panel(16)

        val tabs = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        listOf("EN", "中文").forEachIndexed { i, label ->
            val sel = i == noticeLang
            val t = TextView(this).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(5), dp(12), dp(5))
                background = rounded(if (sel) tc("primaryContainer") else tc("surfaceContainerHighest"), 100)
                setTextColor(if (sel) tc("primary") else tc("onSurfaceVariant"))
                if (sel) setTypeface(null, Typeface.BOLD)
                isClickable = true
                isFocusable = true
                setOnClickListener { noticeLang = i; renderPage() }
            }
            tabs.addView(t, LinearLayout.LayoutParams(-2, -2).apply { if (i > 0) marginStart = dp(6) })
        }
        tabs.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        tabs.addView(txt("2026/09/01 10:01", 11f, false, "onSurfaceVariant"))
        put(card, tabs, 0, -1, -2)

        val data = if (noticeLang == 0) noticeEn else noticeCn
        val title = EditText(this).apply {
            setText(data.first)
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(tc("onSurface"))
            background = null
            setPadding(0, dp(12), 0, 0)
        }
        put(card, title, 10, -1, -2)
        data.second.forEach { para ->
            val body = EditText(this).apply {
                setText(para)
                textSize = 13f
                setTextColor(tc("onSurfaceVariant"))
                background = null
                setPadding(0, dp(8), 0, 0)
            }
            put(card, body, 6, -1, -2)
        }

        val tools = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        listOf(R.drawable.ic_edit to "编辑", R.drawable.ic_schedule to "时间", R.drawable.ic_delete to "删除").forEach { pair ->
            tools.addView(iconButton(pair.first, pair.second) { toast("Demo：工具栏 ${pair.second}") }, LinearLayout.LayoutParams(dp(34), dp(34)))
        }
        tools.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        tools.addView(chip("保存", R.drawable.ic_check, true) { toast("Demo：公告已保存") })
        put(card, tools, 16, -1, -2)
        return card
    }

                                                        

    private fun instancesPage(): View {
        val col = pageColumn()
        put(col, sectionTitle("网关实例", "单进程托管全部实例，各自独立端口与路径前缀"), 2)
        val running = demoInstances.count { it.running }
        val reqs = demoInstances.sumOf { it.requests }
        val errs = demoInstances.sumOf { it.errors }
        val metrics = LinearLayout(this).apply { gravity = Gravity.CENTER }
        metrics.addView(metricTile("${demoInstances.size}", "实例总数", "▤"), LinearLayout.LayoutParams(0, dp(104), 1f))
        metrics.addView(metricTile("$running", "运行中", "●"), LinearLayout.LayoutParams(0, dp(104), 1f).apply { marginStart = dp(8) })
        metrics.addView(metricTile("$reqs", "总请求", "↻"), LinearLayout.LayoutParams(0, dp(104), 1f).apply { marginStart = dp(8) })
        put(col, metrics, 10)

        put(col, sectionTitle("实例列表", "共 ${demoInstances.size} 个 · 错误 $errs"), 20)
        demoInstances.forEach { put(col, instanceCard(it), 8) }

        put(col, sectionTitle("批量操作", "对所有实例生效"), 20)
        val acts = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        acts.addView(smallButton("全部启动", "outlined") { toast("Demo：已模拟启动全部实例") }, LinearLayout.LayoutParams(0, dp(44), 1f))
        acts.addView(smallButton("全部停止", "outlined") { toast("Demo：已模拟停止全部实例") }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = dp(10) })
        put(col, acts, 10)
        put(col, smallButton("＋ 新建实例", "filled") { toast("Demo：新建实例") }, 10)
        return pageScroll(col)
    }

    private fun instanceCard(ins: DemoInstance): View {
        val card = panel(16)
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val dot = View(this).apply { background = rounded(if (ins.running) tc("success") else tc("outline"), 100) }
        top.addView(dot, LinearLayout.LayoutParams(dp(9), dp(9)))
        top.addView(txt(ins.name, 15f, true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        if (ins.tls) top.addView(badge("TLS", "primary"), LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(6) })
        top.addView(badge(if (ins.running) "运行中" else "已停止", if (ins.running) "success" else "neutral"))
        put(card, top, 0, -1, -2)
        put(card, txt("端口 ${ins.port}   ·   路径前缀 /${ins.name}/", 11.5f, false, "onSurfaceVariant"), 9, -1, -2)
        put(
            card,
            txt("${ins.requests} 请求   ·   ${ins.errors} 错误   ·   ${if (ins.running) "内存 43MB" else "未占用内存"}", 12f, false, "onSurfaceVariant"),
            4, -1, -2,
        )
        val acts = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        acts.addView(
            chip(if (ins.running) "停止" else "启动", if (ins.running) R.drawable.ic_block else R.drawable.ic_check, false) {
                toast("Demo：${if (ins.running) "已停止" else "已启动"} ${ins.name}")
            },
        )
        acts.addView(
            chip("编辑", R.drawable.ic_edit, false) { toast("Demo：编辑实例 ${ins.name}") },
            LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) },
        )
        acts.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        acts.addView(chip("打开面板", R.drawable.ic_chevron_right, false) { toast("Demo：打开 ${ins.name} 管理面板") })
        put(card, acts, 12, -1, -2)
        return card
    }

                                                        

    private fun profilePage(): View {
        val col = pageColumn()
        put(col, profileHeader(), 2)
        put(col, sectionTitle("我的卡密", "共 2 张卡"), 20)
        put(col, keyCard("主卡", "sk-gc••••••••9f2a", "72.4K / 不限", "不限额度", "primary"), 8)
        put(col, keyCard("测试卡", "sk-test••••••••1a8c", "0 / 10K", "零额度", "neutral"), 8)
        put(col, sectionTitle("账号操作", ""), 20)
        put(col, menuCard("编辑个人资料", "昵称、头像和备注", R.drawable.ic_edit) { toast("Demo 操作：打开资料编辑") }, 8)
        put(col, menuCard("退出当前服务器", "仅 Demo 演示，不会删除任何数据", R.drawable.ic_close) { toast("Demo 操作：已模拟退出") }, 8)
        return pageScroll(col)
    }

    private fun profileHeader(): View {
        val card = panel(16)
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(avatar("W", selectedAccent.primary, 62), LinearLayout.LayoutParams(dp(62), dp(62)))
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        put(text, txt("WetherFlar", 22f, true), 0, -2, -2)
        put(text, txt("用户 · Demo 账户", 13f, false, "onSurfaceVariant"), 4, -2, -2)
        put(text, badge("Admin", "primary"), 8, -2, -2)
        row.addView(text, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        row.addView(chip("编辑", R.drawable.ic_edit, false) { toast("Demo 操作：编辑资料") })
        put(card, row, 0, -1, -2)
        return card
    }

    private fun keyCard(name: String, key: String, quota: String, state: String, tone: String): View {
        val card = panel(16)
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(iconBadge(R.drawable.ic_key, selectedAccent.primary), LinearLayout.LayoutParams(dp(42), dp(42)))
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        put(text, txt(name, 14f, true), 0, -2, -2)
        put(text, txt(key, 12f, false, "onSurfaceVariant"), 3, -2, -2)
        row.addView(text, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        row.addView(badge(state, tone))
        put(card, row, 0, -1, -2)
        val foot = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        foot.addView(txt(quota, 12.5f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(0, -2, 1f))
        foot.addView(chip("复制", R.drawable.ic_copy, false) { toast("Demo：已模拟复制卡密") })
        put(card, foot, 12, -1, -2)
        return card
    }

                                                      

    private fun themePage(): View {
        val col = pageColumn()
        put(col, sectionTitle("主题与配色", "预览中的修改只保存在内存"), 2)
        val mode = panel(16)
        put(mode, txt("显示模式", 16f, true), 0, -1, -2)
        val modeRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        modeRow.addView(txt(if (darkMode) "深色模式" else "浅色模式", 14f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(0, -2, 1f))
        val toggle = Switch(this).apply { isChecked = darkMode }
        toggle.setOnCheckedChangeListener { _, checked -> darkMode = checked; rebuildTheme() }
        modeRow.addView(toggle)
        put(mode, modeRow, 8, -1, -2)
        put(col, mode, 12)

        put(col, txt("强调色", 16f, true), 12, -1, -2)
        val accents = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        for (accent in Accent.values()) {
            val swatch = TextView(this).apply {
                text = if (accent == selectedAccent) "✓" else ""
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                textSize = 18f
                background = rounded(accent.primary, 100)
                setOnClickListener { selectedAccent = accent; rebuildTheme() }
                contentDescription = accent.label
                isClickable = true
                isFocusable = true
            }
            accents.addView(swatch, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(10) })
        }
        put(col, accents, 8, -1, -2)
        put(col, previewPanel(), 20)
        return pageScroll(col)
    }

    private fun previewPanel(): View {
        val card = panel(16)
        put(card, txt("组件预览", 15f, true), 0, -1, -2)
        put(card, txt("主按钮、次按钮、进度和状态标签", 12f, false, "onSurfaceVariant"), 4, -1, -2)
        val badges = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        badges.addView(badge("运行中", "success"))
        badges.addView(badge("已禁用", "error"), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
        badges.addView(badge("待机", "neutral"), LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(6) })
        put(card, badges, 12, -1, -2)
        val buttons = LinearLayout(this)
        buttons.addView(smallButton("主按钮", "filled") { toast("Demo") }, LinearLayout.LayoutParams(0, dp(42), 1f))
        buttons.addView(smallButton("描边", "outlined") { toast("Demo") }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginStart = dp(8) })
        put(card, buttons, 12, -1, -2)
        put(card, progressRow("组件完成度", 78, 100, "78%"), 12, -1, -2)
        return card
    }

                                                           

    private fun settingsPage(): View {
        val col = pageColumn()
        put(col, sectionTitle("设置", "Demo 版界面占位"), 2)
        put(col, settingRow("服务器连接", "http://demo.local:16384", R.drawable.ic_refresh) { toast("Demo 操作：连接设置") }, 8)
        put(col, settingRow("隐私数据过滤", "已开启", R.drawable.ic_check) { toast("Demo 操作：隐私设置") }, 8)
        put(col, settingRow("后台保活", "已关闭", R.drawable.ic_info) { toast("Demo 操作：后台设置") }, 8)
        put(col, settingRow("通知与提醒", "默认", R.drawable.ic_schedule) { toast("Demo 操作：通知设置") }, 8)
        put(col, settingRow("关于 Gay Core", "版本 ${BuildConfig.VERSION_NAME}", R.drawable.ic_info) { toast("这是 UI 设计 Demo") }, 8)
        val note = panel(16)
        put(note, txt("这是独立预览", 15f, true), 0, -1, -2)
        put(note, txt("当前页面使用假数据，不会读取真实服务器，也不会修改生产配置。", 13f, false, "onSurfaceVariant"), 6, -1, -2)
        put(col, note, 20)
        return pageScroll(col)
    }

    private fun morePage(): View {
        val col = pageColumn()
        put(col, sectionTitle("更多", "管理你的 Gay Core"), 2)
        put(col, menuCard("个人中心", "查看资料、卡密和登录状态", R.drawable.ic_person) { page = Page.PROFILE; renderPage() }, 8)
        put(col, menuCard("Playground", "试用模型并观察交互状态", R.drawable.ic_chat) { page = Page.PLAYGROUND; renderPage() }, 8)
        put(col, menuCard("主题与配色", "选择深浅色和强调色", R.drawable.ic_palette) { page = Page.THEME; renderPage() }, 8)
        put(col, menuCard("设置", "服务器、隐私和实验功能", R.drawable.ic_settings) { page = Page.SETTINGS; renderPage() }, 8)
        put(col, aboutPanel(), 24)
        return pageScroll(col)
    }

    private fun aboutPanel(): View {
        val card = panel(16)
        put(card, txt("Gay Core UI Demo", 16f, true), 0, -1, -2)
        put(card, txt("Material Design 3 原生实现 · 侧边栏导航", 13f, false, "onSurfaceVariant"), 4, -1, -2)
        put(card, txt("v${BuildConfig.VERSION_NAME} · 不连接服务器", 12f, false, "onSurfaceVariant"), 12, -1, -2)
        return card
    }

                                                        

    private fun sectionTitle(title: String, subtitle: String): View {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        put(texts, txt(title, 18f, true), 0, -1, -2)
        if (subtitle.isNotEmpty()) put(texts, txt(subtitle, 11.5f, false, "onSurfaceVariant"), 2, -1, -2)
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        return row
    }

    private fun segmented(labels: List<String>, selected: Int, onSelect: (Int) -> Unit): View {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(tc("surfaceContainerHighest"), 100)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        labels.forEachIndexed { index, label ->
            val sel = index == selected
            val tv = TextView(this).apply {
                text = label
                textSize = 13.5f
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(9), dp(8), dp(9))
                setTextColor(if (sel) tc("onSurface") else tc("onSurfaceVariant"))
                if (sel) setTypeface(null, Typeface.BOLD)
                background = rounded(if (sel) tc("surfaceContainerLowest") else Color.TRANSPARENT, 100)
                isClickable = true
                isFocusable = true
                setOnClickListener { onSelect(index) }
            }
            wrap.addView(tv, LinearLayout.LayoutParams(0, -2, 1f))
        }
        return wrap
    }

    private fun chip(label: String, iconRes: Int? = null, selected: Boolean = false, onClick: (() -> Unit)? = null): TextView {
        val tv = TextView(this).apply {
            text = label
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = if (selected) roundedOutline(tc("primaryContainer"), tc("primary"), 100)
            else roundedOutline(Color.TRANSPARENT, tc("outline"), 100)
            setTextColor(if (selected) tc("primary") else tc("onSurfaceVariant"))
            if (selected) setTypeface(null, Typeface.BOLD)
            if (onClick != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        }
        if (iconRes != null) {
            val d = androidx.core.content.ContextCompat.getDrawable(this, iconRes)?.mutate()
            if (d != null) {
                val sz = dp(16)
                d.setBounds(0, 0, sz, sz)
                d.setTint(tv.currentTextColor)
                tv.setCompoundDrawables(d, null, null, null)
                tv.compoundDrawablePadding = dp(5)
            }
        }
        return tv
    }

    private fun badge(text: String, tone: String): TextView {
        val (bg, fg) = when (tone) {
            "success" -> "successContainer" to "success"
            "error" -> "errorContainer" to "error"
            "warning" -> "tertiaryContainer" to "onTertiaryContainer"
            "primary" -> "primaryContainer" to "primary"
            "tertiary" -> "tertiaryContainer" to "tertiary"
            else -> "surfaceContainerHighest" to "onSurfaceVariant"
        }
        return TextView(this).apply {
            this.text = text
            textSize = 10.5f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(9), dp(4), dp(9), dp(4))
            setTextColor(tc(fg))
            background = rounded(tc(bg), 100)
        }
    }

    private fun metricTile(value: String, label: String, glyph: String, onClick: (() -> Unit)? = null): View {
        val card = panel(16)
        card.gravity = Gravity.CENTER
        val g = txt(glyph, 16f, true, "primary")
        put(card, g, 0, -2, -2)
        put(card, txt(value, 19f, true), 8, -2, -2)
        put(card, txt(label, 11f, false, "onSurfaceVariant"), 3, -2, -2)
        if (onClick != null) {
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener { onClick() }
        }
        return card
    }

    private fun metricCard(value: String, label: String, icon: Int): View {
        val card = panel(14)
        put(card, iconBadge(icon, selectedAccent.primary, 28), 0, dp(28), dp(28))
        put(card, txt(value, 21f, true), 7, -1, -2)
        put(card, txt(label, 11f, false, "onSurfaceVariant"), 2, -1, -2)
        return card
    }

    private fun actionTile(label: String, icon: Int, onClick: () -> Unit): View {
        val card = panel(14).apply {
            setOnClickListener { onClick() }
            isClickable = true
            isFocusable = true
            gravity = Gravity.CENTER
        }
        put(card, iconBadge(icon, selectedAccent.primary, 30), 0, dp(30), dp(30))
        put(card, txt(label, 11.5f, true), 6, -2, -2)
        return card
    }

    private fun searchBar(hint: String): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(tc("surfaceContainer"), 100)
            setPadding(dp(14), 0, dp(6), 0)
        }
        val icon = ImageView(this).apply {
            setImageResource(R.drawable.ic_search)
            setImageTintList(ColorStateList.valueOf(tc("onSurfaceVariant")))
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(18), dp(18)))
        val input = EditText(this).apply {
            this.hint = hint
            setSingleLine(true)
            textSize = 14f
            setTextColor(tc("onSurface"))
            setHintTextColor(tc("onSurfaceVariant"))
            background = null
        }
        row.addView(input, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(8) })
        row.addView(iconButton(R.drawable.ic_filter, "筛选") { toast("Demo：筛选条件") }, LinearLayout.LayoutParams(dp(42), dp(48)))
        return row
    }

    private fun rankRow(name: String, count: String, ratio: String, accent: Accent, value: Float): View {
        val card = panel(16)
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(avatar(name.take(1), accent.primary, 34), LinearLayout.LayoutParams(dp(34), dp(34)))
        row.addView(txt(name, 14f, true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        row.addView(txt(count, 13f, true))
        row.addView(txt("  $ratio", 11f, false, "onSurfaceVariant"))
        put(card, row, 0, -1, -2)
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            this.progress = (value * 100).roundToInt()
            setProgressTintList(ColorStateList.valueOf(Color.parseColor(accent.primary)))
            setProgressBackgroundTintList(ColorStateList.valueOf(tc("surfaceContainerHighest")))
        }
        put(card, progress, 8, -1, -2)
        return card
    }

    private fun barChart(): View {
        val row = LinearLayout(this).apply { gravity = Gravity.BOTTOM }
        val data = listOf(38, 54, 42, 76, 68, 91, 64)
        data.forEachIndexed { index, value ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            val bar = View(this).apply { background = rounded(selectedAccent.primary, 8) }
            item.addView(bar, LinearLayout.LayoutParams(-1, dp((value * 1.35f).roundToInt())).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            })
            item.addView(txt("${index + 1}", 10f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(-1, dp(20)))
            row.addView(item, LinearLayout.LayoutParams(0, dp(150), 1f))
        }
        return row
    }

    private fun progressRow(label: String, value: Int, max: Int, detail: String): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(txt(label, 12f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(txt(detail, 12f, true))
        put(col, row, 0, -1, -2)
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            this.max = max
            this.progress = value.coerceIn(0, max)
            setProgressTintList(ColorStateList.valueOf(tc("primary")))
            setProgressBackgroundTintList(ColorStateList.valueOf(tc("surfaceContainerHighest")))
        }
        put(col, progress, 8, -1, -2)
        return col
    }

    private fun menuCard(title: String, desc: String, icon: Int, onClick: () -> Unit): View {
        val card = panel(16).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(iconBadge(icon, selectedAccent.primary), LinearLayout.LayoutParams(dp(42), dp(42)))
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        put(text, txt(title, 14f, true), 0, -2, -2)
        put(text, txt(desc, 11.5f, false, "onSurfaceVariant"), 3, -2, -2)
        row.addView(text, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) })
        val chev = ImageView(this).apply {
            setImageResource(R.drawable.ic_chevron_right)
            setImageTintList(ColorStateList.valueOf(tc("outline")))
        }
        row.addView(chev, LinearLayout.LayoutParams(dp(20), dp(20)))
        put(card, row, 0, -1, -2)
        return card
    }

    private fun settingRow(title: String, value: String, icon: Int, onClick: () -> Unit): View =
        menuCard(title, value, icon, onClick)

    private fun pill(text: String, accent: Accent, strong: Boolean): TextView {
        val tv = txt(text, 10.5f, strong, if (strong) "onPrimary" else "onSurfaceVariant")
        tv.gravity = Gravity.CENTER
        tv.setPadding(dp(10), dp(4), dp(10), dp(4))
        tv.background = rounded(if (strong) Color.parseColor(accent.primary) else tc("surfaceContainerHighest"), 100)
        return tv
    }

    private fun iconButton(icon: Int, description: String, onClick: () -> Unit): View {
        return ImageButton(this).apply {
            setImageResource(icon)
            setBackgroundColor(Color.TRANSPARENT)
            setImageTintList(ColorStateList.valueOf(tc("onSurfaceVariant")))
            contentDescription = description
            setOnClickListener { onClick() }
            isClickable = true
            isFocusable = true
        }
    }

    private fun iconBadge(icon: Int, color: String, size: Int = 36): View {
        val frame = FrameLayout(this).apply { background = rounded(if (darkMode) darken(color) else lighten(color), 14) }
        val image = ImageView(this).apply {
            setImageResource(icon)
            setImageTintList(ColorStateList.valueOf(if (darkMode) Color.WHITE else Color.parseColor(color)))
        }
        frame.addView(image, FrameLayout.LayoutParams(dp(size * 0.55f), dp(size * 0.55f), Gravity.CENTER))
        return frame
    }

    private fun avatar(text: String, color: String, size: Int): View {
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            textSize = if (size > 50) 22f else 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = rounded(color, 100)
        }
    }

    private fun smallButton(text: String, style: String, onClick: () -> Unit): com.google.android.material.button.MaterialButton =
        UiKit.button(this, theme, text, style).apply { setOnClickListener { onClick() } }

    private fun panel(radius: Int = 20): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = roundedOutline(tc("surfaceContainerLowest"), tc("outlineVariant"), radius)
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun rounded(color: String, radius: Int): GradientDrawable = rounded(Color.parseColor(color), radius)

    private fun roundedOutline(fill: Int, stroke: Int, radius: Int = 20, strokeWidth: Int = 1): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(strokeWidth), stroke)
        }

    private fun toast(message: String) = UiKit.toast(this, message)

    private fun rebuildTheme() {
                                                  
        applyTheme(true)
    }
}
