package com.gaycore.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.gaycore.app.App
import com.gaycore.app.R
import com.gaycore.app.data.Api
import com.gaycore.app.data.ServerEntry
import com.gaycore.app.sdui.IntentClient
import com.gaycore.app.sdui.Renderer
import com.gaycore.app.sdui.Template
import com.gaycore.app.theme.ThemeEngine
import com.gaycore.app.ui.UiKit.add
import com.gaycore.app.ui.UiKit.dp
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


































class SduiController(
    private val ctx: Context,
    private val owner: LifecycleOwner,
    val server: ServerEntry,
    val pluginId: String,
    private val themeEngine: ThemeEngine,
    private val onToast: (String) -> Unit = { UiKit.toast(ctx, it) },
    private val inlineNav: Boolean = false,
    private val onOpenOverlay: ((String, String, String) -> Unit)? = null,
    
    private val onNavChanged: (() -> Unit)? = null,
    



    private val admin: Boolean = false,
    




    private val adminApiScope: List<String> = emptyList(),
    
    private val onFallback: ((String) -> Unit)? = null,
    




    private val onClientAction: ((String, String) -> Unit)? = null,
    
    private val onClose: (() -> Unit)? = null,
) {
    private val store = App.of(ctx).store
    private val intentClient = IntentClient(store, server)

    

    private val rendererHost: Renderer.Host = object : Renderer.Host {
        override fun onAction(action: JsonObject) = this@SduiController.onAction(action)
        override fun userScope(): Map<String, Any?> = mapOf("user" to mapOf("uid" to server.uid))
        
        override fun onStateChanged() { rerenderNow() }
    }

    val renderer: Renderer = Renderer(ctx, themeEngine, rendererHost)

    
    private fun rerenderNow() {
        val c = containerRef ?: return
        renderer.rerender(c)
        
        try { UiKit.autoTint(c) { themeEngine } } catch (_: Throwable) {  }
    }

    private var containerRef: LinearLayout? = null

    
    private var keepScrollOnce = false

    
    private var pendingScrollY = -1

    



    private class IntentRef(val endpoint: String, val body: JsonObject?, val refreshable: Boolean)

    

    private class PageState(var path: String?, var json: JsonObject?, var intent: IntentRef? = null, var scrollY: Int = 0)

    
    private var current: PageState? = null
    
    val currentUiPath: String? get() = current?.path
    
    private val backStack = ArrayDeque<PageState>()
    
    private var loadToken = 0

    fun loadInto(container: LinearLayout, uiPath: String, showTitle: Boolean = false) {
        

        keepScrollOnce = pendingScrollY >= 0 || samePageBase(current?.path, uiPath)
        
        current = PageState(uiPath, null, null)
        containerRef = container
        container.removeAllViews()
        
        val pb = com.google.android.material.progressindicator.LinearProgressIndicator(ctx).apply {
            isIndeterminate = true
            trackColor = themeEngine.color(ctx, "surfaceContainerHighest")
            setIndicatorColor(themeEngine.color(ctx, "primary"))
            trackCornerRadius = dp(ctx, 4f)
        }
        container.addView(pb, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(ctx, 24f); bottomMargin = dp(ctx, 24f)
        })
        val token = ++loadToken
        owner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val page = if (admin) {
                    

                    kotlinx.coroutines.withTimeoutOrNull(SDUI_ADMIN_TIMEOUT_MS) {
                        Api.fetchAdminUi(server, pluginId, uiPath)
                    } ?: throw Exception("服务端界面响应超时")
                } else {
                    Api.fetchUi(server, pluginId, uiPath)
                }
                
                val gv = page.get("gcui")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
                if (gv < 1) throw Exception("不是 gcui 页面")
                
                if (admin && gv > com.gaycore.app.sdui.SduiCapabilities.GCUI_VERSION) {
                    throw Exception("服务端界面需要更新的 App (gcui $gv)")
                }
                withContext(Dispatchers.Main) {
                    if (token != loadToken) return@withContext   
                    renderInto(container, page, showTitle)
                }
            } catch (e: Exception) {
                
                withContext(Dispatchers.Main) {
                    if (token != loadToken) return@withContext
                    
                    val fb = onFallback
                    if (admin && fb != null) { fb(e.message ?: "加载失败"); return@withContext }
                    container.removeAllViews()
                    container.add(UiKit.text(ctx, ctx.getString(R.string.load_failed, e.message), 13.5f, false, themeEngine.color(ctx, "error")))
                    val retry = UiKit.button(ctx, themeEngine, ctx.getString(R.string.retry), "text")
                    retry.setOnClickListener { loadInto(container, uiPath, showTitle) }
                    container.add(retry, 4)
                }
            }
        }
    }

    private fun renderInto(container: LinearLayout, page: JsonObject, showTitle: Boolean) {
        current?.json = page
        

        val sv = renderer.nearestScroller(container)
        val savedY = if (pendingScrollY >= 0) pendingScrollY
                     else if (keepScrollOnce) renderer.scrollYOf(sv) else 0
        pendingScrollY = -1
        container.removeAllViews()
        if (showTitle) {
            page.get("title")?.asString?.let {
                container.add(UiKit.text(ctx, it, 17f, true, themeEngine.color(ctx, "onSurface")))
                

                container.add(DemoKit.FixedHeightView(ctx, dp(ctx, 8f)))
            }
        }
        val content = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        container.addView(content)
        try {
            renderer.render(page, content)
        

        try { UiKit.autoTint(content) { themeEngine } } catch (_: Throwable) {  }
        } catch (e: Exception) {
            
            val fb = onFallback
            if (admin && fb != null) { fb("渲染失败: " + (e.message ?: "?")); return }
            throw e
        }
        
        content.alpha = 0f
        content.translationY = dp(ctx, 10f).toFloat()
        content.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(
            android.view.animation.DecelerateInterpolator(),
        ).start()
        
        if (savedY > 0 && sv != null) sv.post { renderer.restoreScroll(sv, savedY) }
    }

    private fun onAction(action: JsonObject) {
        val scope = mapOf(
            "state" to renderer.state,
            "input" to renderer.inputs,
            

            "selected" to renderer.selected.toList(),
            "selectedCount" to renderer.selected.size,
            "user" to mapOf("uid" to server.uid),
        )
        
        val cm = action.get("confirm")?.asString
        if (!cm.isNullOrEmpty()) {
            UiKit.confirm(ctx, ctx.getString(R.string.confirm), Template.bind(cm, scope), themeEngine) {
                dispatch(action, scope)
            }
        } else {
            dispatch(action, scope)
        }
    }

    private fun dispatch(action: JsonObject, scope: Map<String, Any?>) {
        when (action.get("type")?.asString) {
            "copy" -> {
                val text = Template.bind(action.get("text")?.asString ?: "", scope)
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("gaycore", text))
                onToast(action.get("toast")?.asString ?: ctx.getString(R.string.copied))
            }
            "open" -> {
                val target = Template.bind(action.get("target")?.asString ?: "", scope)
                when {
                    target.startsWith("page:") -> {
                        val pagePath = target.removePrefix("page:")
                        
                        val newPath = if (pagePath.startsWith("/")) pagePath else "/ui/$pagePath"
                        val pageTitle = pagePath.substringBefore('?')
                        
                        val nav = action.get("nav")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                        val container = containerRef
                        when {
                            inlineNav && container != null -> {
                                val depthBefore = backStack.size
                                when {
                                    nav == "pop" -> {
                                        
                                        if (backStack.isNotEmpty()) backStack.removeLast()
                                        loadInto(container, newPath)
                                    }
                                    

                                    nav == "replace" || (nav != "push" && samePageBase(current?.path, newPath)) ->
                                        
                                        loadInto(container, newPath)
                                    

                                    samePageBase(current?.path, newPath) -> loadInto(container, newPath)
                                    else -> {
                                        pushCurrent()
                                        loadInto(container, newPath)
                                    }
                                }
                                if (backStack.size != depthBefore) onNavChanged?.invoke()
                            }
                            onOpenOverlay != null -> onOpenOverlay.invoke(pluginId, newPath, pageTitle)
                            else -> onToast("无法打开页面: $pageTitle (需在主界面内操作)")
                        }
                    }
                    target.startsWith("http://") || target.startsWith("https://") ->
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                    else -> onToast("未知页面: $target")
                }
            }
            

            "adminApi" -> {
                if (!admin) { onToast("该动作仅管理端页面可用"); return }
                val sc = actionScope(scope, action)
                val path = Template.bind(action.get("path")?.asString ?: "", sc)
                if (!adminPathAllowed(path)) { onToast("无权访问该接口: $path"); return }
                val method = action.get("method")?.asString ?: "POST"
                val bodyRaw = action.getAsJsonObject("body")
                val body = if (bodyRaw != null) Template.bindJson(bodyRaw, sc).asJsonObject else null
                val reloadAfter = (action.get("then")?.asString ?: "reload")
                owner.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val r = Api.adminCall(server, method, path, body)
                        withContext(Dispatchers.Main) {
                            



                            r.get("toast")?.let { elem ->
                                if (elem.isJsonPrimitive) onToast(elem.asString)
                                else if (elem.isJsonObject) elem.asJsonObject.get("message")?.asString?.let(onToast)
                            }
                            r.get("error")?.let { elem ->
                                if (elem.isJsonPrimitive) onToast(elem.asString)
                                else if (elem.isJsonObject) elem.asJsonObject.get("message")?.asString?.let(onToast)
                                else if (elem.isJsonArray) elem.asJsonObject?.get("message")?.asString?.let(onToast)
                            }
                            if (reloadAfter == "reload") containerRef?.let { c -> refreshInto(c) }
                            else if (reloadAfter == "close") onClose?.invoke()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onToast(e.message ?: "请求失败") }
                    }
                }
            }
            
            "client" -> {
                if (!admin) { onToast("该动作仅管理端页面可用"); return }
                val name = action.get("action")?.asString ?: ""
                val value = Template.bind(action.get("value")?.asString ?: "", actionScope(scope, action))
                val h = onClientAction
                if (h == null) onToast("当前外壳不支持该动作: $name") else h(name, value)
            }
            "intent" -> {
                val run = {
                    val sc = actionScope(scope, action)
                    val bodyRaw = action.getAsJsonObject("body")
                    val body = if (bodyRaw != null) Template.bindJson(bodyRaw, sc).asJsonObject else null
                    doIntent(action.get("endpoint")?.asString ?: "", body,
                        reloadAfter = (action.get("then")?.asString ?: "reload") == "reload",
                        container = containerRef,
                        
                        actionNav = actionNavHint(action))
                }
                run()
            }
            
            "toast" -> onToast(Template.bind(action.get("text")?.asString ?: "", scope))
            
            "close" -> { onClose?.invoke() }
            
            "refresh" -> containerRef?.let { c -> refreshInto(c) }
            
            "setState" -> {
                val patch = action.getAsJsonObject("patch")
                val m = HashMap<String, Any?>()
                if (patch != null) {
                    for ((k, v) in patch.entrySet()) {
                        if (!v.isJsonPrimitive) { m[k] = null; continue }
                        val p = v.asJsonPrimitive
                        m[k] = when {
                            p.isBoolean -> v.asBoolean
                            p.isNumber -> v.asNumber
                            else -> Template.bindRaw(v.asString, scope) ?: v.asString
                        }
                    }
                }
                renderer.setState(m)
                rerenderNow()
            }
            




            "toggleSelect" -> {
                val v = Template.bind(action.get("value")?.asString ?: "", actionScope(scope, action))
                if (v.isEmpty()) onToast("toggleSelect 缺 value")
                else {
                    if (!renderer.selected.remove(v)) renderer.selected.add(v)
                    


                    renderer.refreshSelectedDependent()
                }
            }
            else -> onToast("未知动作: ${action.get("type")?.asString}")
        }
    }

    




    private fun actionScope(scope: Map<String, Any?>, action: JsonObject): Map<String, Any?> {
        var m: MutableMap<String, Any?>? = null
        for ((k, v) in action.entrySet()) {
            if (k.startsWith("_") && v != null && v.isJsonPrimitive) {
                if (m == null) m = HashMap(scope)
                m[k] = Template.unwrap(v)
            }
        }
        return m ?: scope
    }

    



    fun doIntent(
        endpoint: String,
        body: JsonObject?,
        reloadAfter: Boolean,
        container: LinearLayout?,
        actionNav: String = "",
        forceReplace: Boolean = false,
    ) {
        owner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val r = intentClient.intent(pluginId, endpoint, body)
                withContext(Dispatchers.Main) {
                    r.toast?.let(onToast); r.error?.let(onToast)
                    val ui = r.ui
                    val target = container ?: containerRef
                    when {
                        ui != null && target != null -> {
                            loadToken++   
                            val depthBefore = backStack.size
                            
                            val ref = IntentRef(
                                endpoint, body,
                                r.json.get("refresh")?.takeIf { it.isJsonPrimitive }?.asString == "intent",
                            )
                            when (navModeOf(actionNav, r.json, ui, forceReplace)) {
                                NavMode.POP -> {
                                    

                                    if (backStack.isNotEmpty()) current = backStack.removeLast()
                                }
                                NavMode.RESET -> {
                                    
                                    backStack.clear()
                                    current?.path = null
                                    current?.intent = null
                                }
                                NavMode.REPLACE -> {
                                    

                                }
                                NavMode.PUSH -> {
                                    
                                    pushCurrent()
                                    current?.path = null
                                    current?.intent = ref
                                }
                            }
                            renderInto(target, ui, false)
                            if (backStack.size != depthBefore) onNavChanged?.invoke()
                        }
                        

                        target != null && wantsPop(actionNav, r.json) -> popAndRestore(target, reload = reloadAfter)
                        reloadAfter && target != null -> refreshInto(target)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onToast(e.message ?: "意图失败") }
            }
        }
    }

    
    fun reload(container: LinearLayout) = refreshInto(container)

    






    private fun refreshInto(container: LinearLayout) {
        val ref = current?.intent
        if (ref != null && ref.refreshable) {
            doIntent(ref.endpoint, ref.body, reloadAfter = false, container = container, forceReplace = true)
            return
        }
        val p = current?.path
        if (p != null) loadInto(container, p) else onToast("这一页是操作产生的，请用返回键回上一级")
    }

    
    private enum class NavMode { PUSH, REPLACE, POP, RESET }

    
    private fun actionNavHint(action: JsonObject): String {
        val nav = action.get("nav")?.takeIf { it.isJsonPrimitive }?.asString
        if (!nav.isNullOrEmpty()) return nav
        return if (action.get("pop")?.takeIf { it.isJsonPrimitive }?.asBoolean == true) "pop" else ""
    }

    
    private fun wantsPop(actionNav: String, result: JsonObject): Boolean {
        if (actionNav == "pop") return true
        if (result.get("back")?.takeIf { it.isJsonPrimitive }?.asBoolean == true) return true
        return result.get("nav")?.takeIf { it.isJsonPrimitive }?.asString == "pop"
    }

    



    private fun popAndRestore(container: LinearLayout, reload: Boolean) {
        if (backStack.isEmpty()) return
        loadToken++                       
        val prev = backStack.removeLast()
        current = prev
        
        pendingScrollY = prev.scrollY
        val json = prev.json
        when {
            !reload && json != null -> renderInto(container, json, false)
            prev.path != null -> loadInto(container, prev.path!!)
            prev.intent?.refreshable == true -> refreshInto(container)
            json != null -> renderInto(container, json, false)
            else -> onToast("已返回上一级")
        }
        onNavChanged?.invoke()
    }

    




    private fun navModeOf(actionNav: String, result: JsonObject, ui: JsonObject, forceReplace: Boolean): NavMode {
        if (forceReplace) return NavMode.REPLACE       
        val explicit = if (actionNav.isNotEmpty()) actionNav
        else result.get("nav")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
        when (explicit) {
            "push" -> return NavMode.PUSH
            "replace" -> return NavMode.REPLACE
            "pop" -> return NavMode.POP
            "reset" -> return NavMode.RESET
        }
        if (result.get("back")?.takeIf { it.isJsonPrimitive }?.asBoolean == true) return NavMode.POP
        val newTitle = ui.get("title")?.takeIf { it.isJsonPrimitive }?.asString
        val curTitle = current?.json?.get("title")?.takeIf { it.isJsonPrimitive }?.asString
        return if (!newTitle.isNullOrEmpty() && newTitle == curTitle) NavMode.REPLACE else NavMode.PUSH
    }

    



    fun back(): Boolean {
        val c = containerRef ?: return false
        if (backStack.isEmpty()) return false
        popAndRestore(c, reload = false)
        return true
    }

    
    fun canGoBack(): Boolean = backStack.isNotEmpty()

    
    fun resetNav() { backStack.clear() }

    
    private fun samePageBase(a: String?, b: String): Boolean =
        a != null && a.substringBefore('?') == b.substringBefore('?')

    



    private fun pushCurrent() {
        val c = current
        val p = c?.path ?: currentUiPath
        val topPath = backStack.lastOrNull()?.path
        if (p != null && topPath != null && samePageBase(topPath, p)) return
        backStack.addLast(PageState(p, c?.json, c?.intent, renderer.scrollYOf(renderer.nearestScroller(containerRef))))
    }

    



    private fun adminPathAllowed(path: String): Boolean {
        val p = if (path.startsWith("/")) path else "/$path"
        for (pre in adminApiScope) if (pre.isNotEmpty() && p.startsWith(pre)) return true
        return false
    }

    companion object {
        
        const val SDUI_ADMIN_TIMEOUT_MS = 4000L
    }
}
