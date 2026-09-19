package com.gaycore.app.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.gaycore.app.R
import com.gaycore.app.theme.ThemeEngine
import com.google.android.material.button.MaterialButton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt










abstract class ShellActivity : BaseActivity() {

    

    data class ShellItem(val id: String, val title: String, val sub: String, val iconRes: Int)
    data class TopAction(val iconRes: Int, val desc: String, val onClick: () -> Unit)
    data class FabSpec(val iconRes: Int, val desc: String, val onClick: () -> Unit)
    data class Identity(val avatarText: String, val title: String, val subtitle: String, val tag: String = "", val avatarUrl: String = "")

    protected enum class Nav { UP_IN, DOWN_IN, CHILD_IN, CHILD_OUT, NONE }

    

    protected lateinit var theme: ThemeEngine

    
    override fun tintTheme(): ThemeEngine? = if (::theme.isInitialized) theme else null

    private lateinit var root: FrameLayout
    



    class SwipeHostLayout(ctx: android.content.Context) : FrameLayout(ctx) {
        var childClaimedTouch = false
        override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            childClaimedTouch = disallowIntercept
            super.requestDisallowInterceptTouchEvent(disallowIntercept)
        }
    }
    private lateinit var shellHost: SwipeHostLayout
    protected lateinit var content: FrameLayout
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var drawer: LinearLayout
    private lateinit var drawerMenu: LinearLayout
    private lateinit var scrim: View
    private lateinit var convoPanel: LinearLayout
    private lateinit var convoScrim: View
    private lateinit var topAction: FrameLayout
    private lateinit var menuBtn: FrameLayout
    private lateinit var fab: MaterialButton
    private var drawerWidth = 0
    private var convoWidth = 0

    private var drawerMenuAnimated = false
    private var fabShown = false

    protected var drawerOpen = false
        private set
    private var convoOpen = false

    protected var currentPageId: String = ""
        private set
    private var prevPageId = ""
    private var pageView: View? = null

    
    private class ChildPage(
        val title: String,
        val build: (LinearLayout) -> Unit,
        
        val topIcon: Int = 0,
        val topDesc: String = "",
        val onTop: (() -> Unit)? = null,
        
        val footer: ((LinearLayout) -> Unit)? = null,
    )

    private val childStack = ArrayList<ChildPage>()
    private var childHost: View? = null

    

    
    protected abstract fun createTheme(): ThemeEngine

    
    protected abstract fun shellItems(): List<ShellItem>

    
    protected abstract fun buildPage(id: String): View

    
    protected open fun topActionFor(id: String): TopAction =
        TopAction(R.drawable.ic_refresh, "刷新") { onRefresh(id) }

    
    protected open fun topActionOrNull(id: String): TopAction? = topActionFor(id)

    protected open fun onRefresh(id: String) {}

    
    protected open fun inlineBackAvailable(): Boolean = false

    
    protected open fun onInlineBack(): Boolean = false

    




    protected open fun onShellResume() {}

    
    protected open fun fabFor(id: String): FabSpec? = null

    
    protected open fun identity(): Identity? = null

    
    protected open fun onSwitchServer() {}

    
    protected open fun onToggleDarkMode() {}

    
    protected open fun recents(): MutableList<String> = mutableListOf()

    protected open fun onRecentPick(title: String) {}

    
    protected open fun banner(): View? = null

    
    protected open fun onBackExtra(): Boolean = false

    
    protected open fun onChildBack(): Boolean = false

    
    protected open fun onChildOpening() {}

    
    protected open fun onChildOpened() {}

    
    protected open fun onShellRebuilt() {}

    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildShell()
        setupBackHandler()
    }

    override fun onResume() {
        super.onResume()
        onShellResume()
    }

    

    private fun buildShell() {
        theme = createTheme()
        theme.applyToWindow(this)

        root = FrameLayout(this)
        shellHost = SwipeHostLayout(this)
        root.addView(shellHost, FrameLayout.LayoutParams(-1, -1))

        
        fab = UiKit.button(this, theme, "", "filled")
        fab.setIconResource(R.drawable.ic_add)
        fab.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        fab.iconPadding = 0
        fab.gravity = Gravity.CENTER
        fab.setPadding(0, 0, 0, 0)
        fab.iconSize = dp(24)
        fab.contentDescription = "新增"
        fab.visibility = View.GONE
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

        
        drawerWidth = min(dp(300), (resources.displayMetrics.widthPixels * 0.86f).roundToInt())
        drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            elevation = dp(8).toFloat()
            

            translationX = -(drawerWidth + dp(24)).toFloat()
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
        convoWidth = min(dp(248), (resources.displayMetrics.widthPixels * 0.74f).roundToInt())
        convoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            elevation = dp(8).toFloat()
            translationX = -(convoWidth + dp(24)).toFloat()
        }
        root.addView(convoPanel, FrameLayout.LayoutParams(convoWidth, -1))

        setContentView(root)
        applyTheme(false)
    }

    



    protected fun applyTheme(animate: Boolean) {
        val preferred = currentPageId
        theme = createTheme()
        theme.applyToWindow(this)
        root.setBackgroundColor(theme.color(this, "background"))

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.color(this@ShellActivity, "background"))
        }
        column.addView(buildTopBar(), LinearLayout.LayoutParams(-1, dp(72)))
        val bn = banner()
        if (bn != null) column.addView(bn, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
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

        childStack.clear()
        childHost = null
        pageView = null
        val items = shellItems()
        val target = if (items.any { it.id == preferred }) preferred else (items.firstOrNull()?.id ?: "")
        prevPageId = target
        showPage(target, Nav.NONE)
        onShellRebuilt()
    }

    private fun buildTopBar(): View {
        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(8), dp(6), dp(6))
            setBackgroundColor(theme.color(this@ShellActivity, "background"))
        }
        menuBtn = FrameLayout(this)
        bar.addView(menuBtn, LinearLayout.LayoutParams(dp(44), dp(48)))
        paintMenuButton()
        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleView = DemoKit.txt(this, theme, "", 20f, true)
        subtitleView = DemoKit.txt(this, theme, "", 11.5f, false, "onSurfaceVariant")
        titles.addView(titleView)
        titles.addView(subtitleView, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(1) })
        bar.addView(titles, LinearLayout.LayoutParams(0, -1, 1f).apply { marginStart = dp(6) })
        topAction = FrameLayout(this)
        bar.addView(topAction, LinearLayout.LayoutParams(dp(44), dp(48)))
        return bar
    }

    
    private fun paintMenuButton() {
        if (!::menuBtn.isInitialized) return
        menuBtn.removeAllViews()
        val child = isChildOpen()
        

        val inline = !child && inlineBackAvailable()
        val back = child || inline
        menuBtn.addView(
            DemoKit.iconButton(
                this, theme,
                if (back) R.drawable.ic_arrow_back else R.drawable.ic_menu,
                if (back) "返回" else "打开侧边栏",
            ) {
                when {
                    
                    child -> if (!onChildBack()) closeChild()
                    inline -> onInlineBack()
                    else -> openDrawer()
                }
            },
            FrameLayout.LayoutParams(dp(44), dp(48)),
        )
    }

    private fun updateTopBarAction() {
        if (!::topAction.isInitialized) return
        topAction.removeAllViews()
        if (isChildOpen()) {
            
            val cp = childStack.lastOrNull()
            if (cp != null && cp.topIcon != 0 && cp.onTop != null) {
                topAction.addView(
                    DemoKit.iconButton(this, theme, cp.topIcon, cp.topDesc) { cp.onTop.invoke() },
                    FrameLayout.LayoutParams(dp(44), dp(48)),
                )
            } else {
                topAction.addView(
                    DemoKit.iconButton(this, theme, R.drawable.ic_refresh, "刷新") { rebuildChild(false) },
                    FrameLayout.LayoutParams(dp(44), dp(48)),
                )
            }
            return
        }
        val a = topActionOrNull(currentPageId) ?: return
        topAction.addView(
            DemoKit.iconButton(this, theme, a.iconRes, a.desc, a.onClick),
            FrameLayout.LayoutParams(dp(44), dp(48)),
        )
    }

    




    private fun syncTopBar() {
        paintMenuButton()
        syncTitles()
        updateTopBarAction()
    }

    private fun updateFab() {
        if (!::fab.isInitialized) return
        val spec = if (isChildOpen()) null else fabFor(currentPageId)
        if (spec == null) {
            if (fabShown) {
                fabShown = false
                fab.animate().scaleX(0.6f).scaleY(0.6f).alpha(0f).setDuration(160)
                    .setInterpolator(AccelerateInterpolator())
                    .withEndAction { fab.visibility = View.GONE }.start()
            } else fab.visibility = View.GONE
        } else {
            if (!fabShown) {
                fabShown = true
                fab.visibility = View.VISIBLE
                fab.scaleX = 0.6f
                fab.scaleY = 0.6f
                fab.alpha = 0f
                fab.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(200)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.4f)).start()
            }
            fab.visibility = View.VISIBLE
            fab.setIconResource(spec.iconRes)
            fab.contentDescription = spec.desc
            fab.background = Md3.shape(this, theme.color(this, "primary"), Md3.SHAPE_FULL)
            fab.iconTint = ColorStateList.valueOf(theme.color(this, "onPrimary"))
            fab.backgroundTintList = ColorStateList.valueOf(theme.color(this, "primary"))
            fab.setOnClickListener { spec.onClick() }
        }
    }

    

    private fun fillDrawer(col: LinearLayout) {
        col.setBackgroundColor(theme.color(this, "surfaceContainer"))

        val id = identity()
        if (id != null) {
            val head = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(24), dp(20), dp(18))
                setBackgroundColor(theme.color(this@ShellActivity, "surfaceContainerHigh"))
            }
            val av = if (id.avatarUrl.isNotEmpty()) {
                
                DemoKit.avatarAuto(this, theme, id.avatarUrl, id.avatarText, 50)
            } else {
                DemoKit.avatarSoft(this, theme, id.avatarText, 50)
            }
            head.addView(av, LinearLayout.LayoutParams(dp(50), dp(50)))
            head.addView(DemoKit.txt(this, theme, id.title, 17f, true), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(12) })
            if (id.subtitle.isNotEmpty()) {
                head.addView(DemoKit.txt(this, theme, id.subtitle, 11.5f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3) })
            }
            if (id.tag.isNotEmpty()) {
                head.addView(DemoKit.txt(this, theme, id.tag, 12f, true, "onSurfaceVariant"), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(10) })
            }
            col.addView(head, LinearLayout.LayoutParams(-1, -2))
        }

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
        switchSrv.setOnClickListener { closeDrawer(); onSwitchServer() }
        foot.addView(switchSrv, LinearLayout.LayoutParams(0, dp(44), 1f))
        val themeBtn = FrameLayout(this).apply {
            background = DemoKit.rounded(this@ShellActivity, theme.color(this@ShellActivity, "surfaceContainerHighest"), 100)
            isClickable = true
            isFocusable = true
            contentDescription = "切换明暗"
            setOnClickListener { onToggleDarkMode() }
        }
        val dark = DemoKit.isDark(this, theme)
        val themeIcon = ImageView(this).apply {
            setImageResource(if (dark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode)
            setImageTintList(ColorStateList.valueOf(theme.color(this@ShellActivity, "onSurfaceVariant")))
            
            alpha = 0f
            rotation = -90f
            animate().alpha(1f).rotation(0f).setDuration(260)
                .setInterpolator(DecelerateInterpolator()).start()
        }
        themeBtn.addView(themeIcon, FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
        foot.addView(themeBtn, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(10) })
        col.addView(foot, LinearLayout.LayoutParams(-1, -2))

        rebuildDrawerMenu()
    }

    private fun rebuildDrawerMenu() {
        if (!::drawerMenu.isInitialized) return
        drawerMenu.removeAllViews()
        val menuRows = ArrayList<View>()
        for (item in shellItems()) {
            val selected = item.id == currentPageId && !isChildOpen()
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(13), dp(16), dp(13))
                isClickable = true
                isFocusable = true
                background = DemoKit.rounded(
                    this@ShellActivity,
                    if (selected) theme.color(this@ShellActivity, "primaryContainer") else Color.TRANSPARENT,
                    100,
                )
                setOnClickListener {
                    if (isChildOpen()) closeChild()
                    showPage(item.id)
                    closeDrawer()
                }
            }
            val ic = ImageView(this).apply {
                setImageResource(item.iconRes)
                setImageTintList(
                    ColorStateList.valueOf(
                        theme.color(this@ShellActivity, if (selected) "primary" else "onSurfaceVariant"),
                    ),
                )
            }
            row.addView(ic, LinearLayout.LayoutParams(dp(22), dp(22)))
            row.addView(
                DemoKit.txt(this, theme, item.title, 14f, selected, if (selected) "primary" else "onSurfaceVariant"),
                LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) },
            )
            drawerMenu.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(2) })
            menuRows.add(row)
        }
        
        if (!drawerMenuAnimated) {
            drawerMenuAnimated = true
            animateInStaggered(menuRows, 20)
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
        drawer.animate().translationX(-(dw + dp(24)).toFloat()).setDuration(260).setInterpolator(AccelerateInterpolator()).start()
        scrim.animate().alpha(0f).setDuration(250).withEndAction { scrim.visibility = View.GONE }.start()
    }

    

    private fun fillConvoPanel(col: LinearLayout) {
        col.setBackgroundColor(theme.color(this, "surfaceContainer"))
        val head = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(10), dp(12))
        }
        head.addView(DemoKit.txt(this, theme, "最近对话", 15f, true), LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(
            DemoKit.iconButton(this, theme, R.drawable.ic_close, "收起") { closeConvo() },
            LinearLayout.LayoutParams(dp(34), dp(34)),
        )
        col.addView(head, LinearLayout.LayoutParams(-1, -2))
        col.addView(DemoKit.divider(this, theme), LinearLayout.LayoutParams(-1, dp(1)))

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        val items = recents()
        if (items.isEmpty()) {
            list.addView(DemoKit.txt(this, theme, "(暂无对话)", 12.5f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(-1, -2))
        }
        items.toList().forEachIndexed { index, title ->
            val item = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(8), dp(12))
                background = DemoKit.roundedOutline(
                    this@ShellActivity,
                    theme.color(this@ShellActivity, if (index == 0) "primaryContainer" else "surfaceContainerLowest"),
                    theme.color(this@ShellActivity, if (index == 0) "primary" else "outlineVariant"),
                    12,
                )
                isClickable = true
                isFocusable = true
                setOnClickListener { onRecentPick(title); closeConvo() }
            }
            item.addView(
                DemoKit.txt(this, theme, title, 12.5f, index == 0, if (index == 0) "primary" else "onSurfaceVariant"),
                LinearLayout.LayoutParams(0, -2, 1f),
            )
            val del = TextView(this).apply {
                text = "✕"
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                gravity = Gravity.CENTER
                setTextColor(theme.color(this@ShellActivity, "onSurfaceVariant"))
                background = DemoKit.roundedOutline(this@ShellActivity, Color.TRANSPARENT, theme.color(this@ShellActivity, "outlineVariant"), 8)
                isClickable = true
                isFocusable = true
                contentDescription = "删除对话"
                setOnClickListener {
                    recents().remove(title)
                    convoPanel.removeAllViews()
                    fillConvoPanel(convoPanel)
                }
            }
            item.addView(del, LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginStart = dp(6) })
            list.addView(item, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }
        col.addView(list, LinearLayout.LayoutParams(-1, -2))
    }

    protected fun openConvo() {
        if (!::convoPanel.isInitialized) return
        if (drawerOpen) closeDrawer()
        convoOpen = true
        convoPanel.removeAllViews()
        fillConvoPanel(convoPanel)
        convoScrim.visibility = View.VISIBLE
        convoScrim.animate().alpha(0.5f).setDuration(280).setInterpolator(DecelerateInterpolator()).start()
        convoPanel.animate().translationX(0f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
    }

    protected fun closeConvo() {
        if (!convoOpen) return
        convoOpen = false
        val cw = if (convoPanel.width > 0) convoPanel.width else dp(248)
        convoPanel.animate().translationX(-(cw + dp(24)).toFloat()).setDuration(240).setInterpolator(AccelerateInterpolator()).start()
        convoScrim.animate().alpha(0f).setDuration(220).withEndAction { convoScrim.visibility = View.GONE }.start()
    }

    










    private var swipeMode = 0          
    private var swipeActive = false    
    private var swipeStartX = 0f
    private var swipeStartY = 0f

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!::drawer.isInitialized || !::scrim.isInitialized || !::shellHost.isInitialized) {
            return super.dispatchTouchEvent(ev)
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                swipeMode = 0; swipeActive = false
                swipeStartX = ev.x; swipeStartY = ev.y
                



                shellHost.childClaimedTouch = false
                if (drawerOpen) {
                    swipeMode = 2
                } else if (!convoOpen) {
                    swipeMode = 1
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (swipeMode == 0) return super.dispatchTouchEvent(ev)
                val dx = ev.x - swipeStartX
                val dy = abs(ev.y - swipeStartY)
                if (!swipeActive) {
                    


                    val delivered = super.dispatchTouchEvent(ev)
                    if (shellHost.childClaimedTouch) {          
                        swipeMode = 0
                        return delivered
                    }
                    val slop = ViewConfiguration.get(this).scaledTouchSlop.toFloat() * 1.5f
                    val wanted = if (swipeMode == 1) dx > slop else dx < -slop
                    if (wanted && abs(dx) > dy) {
                        
                        if (swipeMode == 1 && contentTakesHorizontal(swipeStartX, swipeStartY, dx > 0)) {
                            swipeMode = 0
                            return delivered
                        }
                        swipeActive = true
                        drawer.animate().cancel(); scrim.animate().cancel()
                        if (convoOpen) closeConvo()
                        if (swipeMode == 1) {
                            drawerOpen = true
                            scrim.visibility = View.VISIBLE
                            scrim.alpha = 0f
                        }
                    } else if (dy > slop && dy > abs(dx)) {
                        swipeMode = 0    
                    }
                    return delivered
                }
                if (swipeActive) { dragDrawer(dx); return true }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (swipeActive) {
                    val w = max(drawer.width, dp(1)).toFloat()
                    val progress = ((ev.x - swipeStartX) / w).coerceIn(-1f, 1f)
                    swipeActive = false; swipeMode = 0
                    if (progress > 0.32f) openDrawer() else closeDrawer()
                    return true
                }
                swipeMode = 0
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    



    private fun contentTakesHorizontal(x: Float, y: Float, back: Boolean): Boolean {
        if (shellHost.childClaimedTouch) return true
        return hitHorizontalScroller(shellHost, x, y, back)
    }

    
    private fun hitHorizontalScroller(v: View, px: Float, py: Float, back: Boolean): Boolean {
        if (v.visibility != View.VISIBLE) return false
        if (px < v.left || px >= v.left + v.width || py < v.top || py >= v.top + v.height) return false
        if (v is android.widget.EditText) return true
        if (v.canScrollHorizontally(if (back) -1 else 1)) return true
        if (v !is ViewGroup) return false
        val cx = px - v.left + v.scrollX
        val cy = py - v.top + v.scrollY
        for (i in v.childCount - 1 downTo 0) {
            if (hitHorizontalScroller(v.getChildAt(i), cx, cy, back)) return true
        }
        return false
    }

    
    private fun dragDrawer(dx: Float) {
        val w = max(drawer.width, dp(1)).toFloat()
        val hidden = -(w + dp(24))
        val base = if (swipeMode == 1) hidden else 0f
        drawer.translationX = (base + dx).coerceIn(hidden, 0f)
        val open = (drawer.translationX / w).coerceIn(0f, 1f)
        scrim.alpha = 0.6f * open
    }

    

    private fun isChildOpen() = childStack.isNotEmpty()

    private fun autoNav(from: String, to: String): Nav {
        if (from == to) return Nav.NONE
        val items = shellItems()
        val a = items.indexOfFirst { it.id == from }
        val b = items.indexOfFirst { it.id == to }
        

        if (a < 0 || b < 0) return Nav.UP_IN
        return if (b > a) Nav.UP_IN else Nav.DOWN_IN
    }

    private fun swapContent(newView: View, nav: Nav) {
        if (!::content.isInitialized) return
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
                newView.animate().alpha(1f).translationY(0f).setDuration(280).setInterpolator(DecelerateInterpolator()).start()
                old.animate().alpha(0f).translationY(if (up) -h else h).setDuration(240).setInterpolator(AccelerateInterpolator())
                    .withEndAction { if (old.parent === content) content.removeView(old) }.start()
            }
            Nav.CHILD_IN -> {
                newView.translationX = w
                content.addView(newView, lp)
                newView.animate().translationX(0f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
                old.animate().translationX(-w * 0.24f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
            }
            Nav.CHILD_OUT -> {
                content.removeView(old)
                content.addView(newView, lp)
                content.addView(old, FrameLayout.LayoutParams(-1, -1))
                old.translationX = 0f
                old.animate().translationX(w).setDuration(300).setInterpolator(AccelerateInterpolator())
                    .withEndAction { if (old.parent === content) content.removeView(old) }.start()
            }
            Nav.NONE -> {}
        }
        pageView = newView
    }

    
    protected fun showPage(id: String, nav: Nav? = null) {
        if (!::content.isInitialized) return
        val items = shellItems()
        

            

            val pageId = if (items.any { it.id == id } || id in com.gaycore.app.sdui.LayoutMerger.BUILTIN_IDS) id
                         else (items.firstOrNull()?.id ?: return)
        currentPageId = pageId
        val anim = nav ?: autoNav(prevPageId, pageId)
        val body = try {
            buildPage(pageId)
        } catch (e: Throwable) {
            errorView("页面构建失败", e)
        }
        swapContent(body, anim)
        prevPageId = pageId
        syncTopBar()
        updateFab()
        rebuildDrawerMenu()
        retintLater()   
    }

    private fun syncTitles() {
        if (!::titleView.isInitialized) return
        val item = shellItems().firstOrNull { it.id == currentPageId }
            ?: if (currentPageId in com.gaycore.app.sdui.LayoutMerger.BUILTIN_IDS) {
                val (t, _) = com.gaycore.app.sdui.LayoutMerger.builtinTitles(currentPageId)
                ShellItem(currentPageId, t, "", 0)
            } else null
        val parent = item?.title ?: currentPageId
        val child = childStack.lastOrNull()
        if (child != null) {
            
            titleView.text = child.title
            subtitleView.text = if (parent.isNotEmpty()) "返回 $parent" else ""
            return
        }
        titleView.text = parent
        subtitleView.text = item?.sub ?: ""
    }

    

    protected fun retintAll() {
        try { UiKit.retintTree(window?.decorView, theme) } catch (_: Throwable) { }
    }

    
    protected fun retintLater() {
        try { content.post { retintAll() } } catch (_: Throwable) { }
    }

    
    protected fun refreshCurrentPage() {
        if (isChildOpen()) {
            childStack.lastOrNull()?.let { rebuildChild(false) }
            return
        }
        showPage(currentPageId, Nav.NONE)
    }

    
    protected fun refreshTopBar() { syncTopBar() }

    

    protected fun refreshDrawerMenu() { rebuildDrawerMenu() }

    
    protected fun rebuildShell(animate: Boolean = true) {
        applyTheme(animate)
        retintAll()
        
        

        rebuildDrawerMenu()
        syncTopBar()
        updateFab()
    }

    

    



    protected fun openChild(
        title: String,
        topIcon: Int = 0,
        topDesc: String = "",
        onTop: (() -> Unit)? = null,
        footer: ((LinearLayout) -> Unit)? = null,
        build: (LinearLayout) -> Unit,
    ) {
        if (!::content.isInitialized) return
        

        onChildOpening()
        childStack.add(ChildPage(title, build, topIcon, topDesc, onTop, footer))
        rebuildChild(true)
        onChildOpened()
    }

    private fun rebuildChild(animateIn: Boolean) {
        val top = childStack.lastOrNull() ?: return
        val host = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.color(this@ShellActivity, "background"))
        }
        
        val col = DemoKit.pageColumn(this, theme, bottomSpace = 40)
        try {
            top.build(col)
        } catch (e: Throwable) {
            DemoKit.put(col, errorText("加载失败: " + (e.message ?: e.javaClass.simpleName)))
        }
        host.addView(pageScroll(col), LinearLayout.LayoutParams(-1, 0, 1f))
        top.footer?.let { f ->
            val bar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            try { f(bar) } catch (e: Throwable) { DemoKit.put(bar, DemoKit.txt(this, theme, "底栏渲染失败: " + (e.message ?: ""), 12f, false, "error")) }
            host.addView(bar, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        childHost = host
        if (animateIn) {
            swapContent(host, Nav.CHILD_IN)
        } else {
            swapContent(host, Nav.NONE)
        }
        syncTopBar()
        updateFab()
    }

    
    protected fun refreshChild() { rebuildChild(false) }

    protected fun closeChild() {
        if (childStack.isEmpty()) return
        childStack.removeAt(childStack.size - 1)
        childHost = null
        if (childStack.isNotEmpty()) {
            rebuildChild(false)
            return
        }
        
        val body = try {
            buildPage(currentPageId)
        } catch (e: Throwable) {
            errorView("页面构建失败", e)
        }
        swapContent(body, Nav.CHILD_OUT)
        syncTopBar()
        updateFab()
        rebuildDrawerMenu()
    }

    protected fun childDepth(): Int = childStack.size

    

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    isChildOpen() -> { if (!onChildBack()) closeChild() }
                    inlineBackAvailable() -> { onInlineBack() }
                    drawerOpen -> closeDrawer()
                    convoOpen -> closeConvo()
                    onBackExtra() -> {}
                    currentPageId != shellItems().firstOrNull()?.id -> showPage(shellItems().first().id, Nav.DOWN_IN)
                    else -> finish()
                }
            }
        })
    }

    

    protected fun dp(v: Number): Int = DemoKit.dp(this, v)

    protected fun pageScroll(v: View): ScrollView = ScrollView(this).apply {
        isFillViewport = true
        clipToPadding = false
        addView(v, FrameLayout.LayoutParams(-1, -2))
    }

    protected fun errorText(msg: String): TextView =
        DemoKit.txt(this, theme, msg, 13f, false, "error").apply { setPadding(dp(16), dp(40), dp(16), dp(40)) }

    protected fun errorView(title: String, e: Throwable): View {
        val col = DemoKit.pageColumn(this, theme)
        DemoKit.put(col, DemoKit.txt(this, theme, title, 18f, true, "error"))
        DemoKit.put(col, DemoKit.txt(this, theme, e.javaClass.simpleName + ": " + (e.message ?: ""), 12.5f, false, "onSurfaceVariant"), 8)
        return pageScroll(col)
    }

    protected fun tip(message: String) = UiKit.toast(this, message)

    protected fun animateIn(v: View, fromDp: Int = 10) = DemoKit.animateIn(v, fromDp)

    protected fun animateInStaggered(views: List<View>, stepMs: Long = 24) = DemoKit.animateInStaggered(views, stepMs)

    protected fun confirm(title: String, message: String, onOk: () -> Unit) =
        UiKit.confirm(this, title, message, theme, onOk)
}
