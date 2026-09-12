package com.gaycore.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
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
) {
    private val store = App.of(ctx).store
    private val intentClient = IntentClient(store, server)

    val renderer = Renderer(ctx, themeEngine, object : Renderer.Host {
        override fun onAction(action: JsonObject) = this@SduiController.onAction(action)
        override fun userScope(): Map<String, Any?> = mapOf("user" to mapOf("uid" to server.uid))
    })

    var currentUiPath: String? = null
        private set
    private var containerRef: LinearLayout? = null
    private val navStack = ArrayDeque<String>()

    fun loadInto(container: LinearLayout, uiPath: String, showTitle: Boolean = false) {
        currentUiPath = uiPath
        containerRef = container
        container.removeAllViews()
        val pb = ProgressBar(ctx)
        container.addView(pb, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(ctx, 24f); bottomMargin = dp(ctx, 24f)
        })
        owner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val page = Api.fetchUi(server, pluginId, uiPath)
                                                                           
                val gv = page.get("gcui")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
                if (gv < 1) throw Exception("不是 gcui 页面")
                withContext(Dispatchers.Main) { renderInto(container, page, showTitle) }
            } catch (e: Exception) {
                                
                withContext(Dispatchers.Main) {
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
        container.removeAllViews()
        if (showTitle) {
            page.get("title")?.asString?.let {
                container.add(UiKit.text(ctx, it, 17f, true, themeEngine.color(ctx, "onSurface")))
                container.add(View(ctx).apply { layoutParams = LinearLayout.LayoutParams(1, dp(ctx, 8f)) })
            }
        }
        val content = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        container.addView(content)
        renderer.render(page, content)
    }

    private fun onAction(action: JsonObject) {
        val scope = mapOf(
            "state" to renderer.state,
            "input" to renderer.inputs,
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
                        val newPath = "/ui/" + target.removePrefix("page:")
                        val pageTitle = target.removePrefix("page:")
                        when {
                            inlineNav && containerRef != null -> {
                                currentUiPath?.let { navStack.addLast(it) }
                                loadInto(containerRef!!, newPath)
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
            "intent" -> {
                val run = {
                    val bodyRaw = action.getAsJsonObject("body")
                    val body = if (bodyRaw != null) Template.bindJson(bodyRaw, scope).asJsonObject else null
                    doIntent(action.get("endpoint")?.asString ?: "", body,
                        reloadAfter = (action.get("then")?.asString ?: "reload") == "reload",
                        container = containerRef)
                }
                run()
            }
                     
            "toast" -> onToast(Template.bind(action.get("text")?.asString ?: "", scope))
                         
            "refresh" -> containerRef?.let { c -> currentUiPath?.let { p -> loadInto(c, p) } }
                                                         
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
                containerRef?.let { c -> renderer.rerender(c) }
            }
            else -> onToast("未知动作: ${action.get("type")?.asString}")
        }
    }

                                         
    fun doIntent(endpoint: String, body: JsonObject?, reloadAfter: Boolean, container: LinearLayout?) {
        owner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val r = intentClient.intent(pluginId, endpoint, body)
                withContext(Dispatchers.Main) {
                    r.toast?.let(onToast); r.error?.let(onToast)
                    val ui = r.ui
                    val target = container ?: containerRef
                    when {
                        ui != null && target != null -> renderInto(target, ui, false)
                        reloadAfter && target != null -> currentUiPath?.let { loadInto(target, it) }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onToast(e.message ?: "意图失败") }
            }
        }
    }

                             
    fun reload(container: LinearLayout) {
        currentUiPath?.let { loadInto(container, it) }
    }

                                                      
    fun back(): Boolean {
        if (navStack.isEmpty()) return false
        val prev = navStack.removeLast()
        containerRef?.let { loadInto(it, prev) }
        return true
    }
}
