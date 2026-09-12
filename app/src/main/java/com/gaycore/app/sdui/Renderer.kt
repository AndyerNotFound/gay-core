package com.gaycore.app.sdui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.text.InputType
import android.util.LruCache
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import com.gaycore.app.R
import com.gaycore.app.theme.ThemeEngine
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.URL
import java.util.concurrent.Executors

                                               
                                              
class Renderer(
    private val context: Context,
    val theme: ThemeEngine,
    private val host: Host,
) {
    interface Host {
        fun onAction(action: JsonObject)
        fun userScope(): Map<String, Any?>                               

                                                        
        fun onStateChanged() {}
    }

                                                     
    val inputs = mutableMapOf<String, String>()
    var state: JsonObject? = null
        private set

                                                
    var lastPage: JsonObject? = null
        private set

                                                               
    fun setState(patch: Map<String, Any?>) {
        val st = state ?: JsonObject().also { state = it }
        for ((k, v) in patch) {
            when (v) {
                null -> st.remove(k)
                is Boolean -> st.addProperty(k, v)
                is Number -> st.addProperty(k, v)
                else -> st.addProperty(k, v.toString())
            }
        }
    }

                                      
    fun rerender(container: ViewGroup) {
        val p = lastPage ?: return
        val saved = HashMap(inputs)
        val st = state
        val merged = deepCopy(p)
        if (st != null) merged.add("state", st)
        container.removeAllViews()
        state = merged.getAsJsonObject("state")
        val root = merged.getAsJsonObject("root") ?: return
        renderNode(root, baseScope())?.let { container.addView(it) }
        inputs.putAll(saved)
    }

    private val dp = context.resources.displayMetrics.density
    private fun d(v: Float) = (v * dp + 0.5f).toInt()
    private fun d(v: Int) = (v * dp + 0.5f).toInt()

                                                    
                                                                                        
    private fun shapeRadius(name: String?): Float = when (name) {
        "pill", "full" -> 1000f
        "extraLarge", "xl" -> d(16).toFloat()
        "medium" -> d(8).toFloat()
        "small" -> d(4).toFloat()
        "none" -> 0f
        else -> d(12).toFloat()
    }

                                                                         
    private fun shapeBg(
        fill: Int,
        radius: Float,
        strokeDp: Float = 0f,
        stroke: Int = android.graphics.Color.TRANSPARENT,
    ) = com.google.android.material.shape.MaterialShapeDrawable(
        com.google.android.material.shape.ShapeAppearanceModel.builder()
            .setAllCornerSizes(radius).build(),
    ).apply {
        fillColor = android.content.res.ColorStateList.valueOf(fill)
        if (strokeDp > 0f) {
            strokeWidth = strokeDp
            strokeColor = android.content.res.ColorStateList.valueOf(stroke)
        }
    }

                                           
    private fun num(node: JsonObject, key: String, scope: Map<String, Any?>, def: Float): Float {
        val raw = node.get(key)?.let { if (it.isJsonPrimitive) it.asString else null }
        return (Template.bindRaw(raw, scope) as? Number)?.toFloat()
            ?: node.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asFloat
            ?: def
    }

                                                             
    private val iconMap = mapOf(
        "home" to R.drawable.ic_home, "person" to R.drawable.ic_person, "settings" to R.drawable.ic_settings,
        "check" to R.drawable.ic_check, "apps" to R.drawable.ic_apps, "add" to R.drawable.ic_add,
        "close" to R.drawable.ic_close, "refresh" to R.drawable.ic_refresh, "key" to R.drawable.ic_key,
        "info" to R.drawable.ic_info, "palette" to R.drawable.ic_palette, "edit" to R.drawable.ic_edit,
        "star" to R.drawable.ic_star, "group" to R.drawable.ic_group, "copy" to R.drawable.ic_copy,
        "wallet" to R.drawable.ic_wallet, "bug" to R.drawable.ic_bug, "search" to R.drawable.ic_search,
        "chevron" to R.drawable.ic_chevron_right, "delete" to R.drawable.ic_delete,
        "block" to R.drawable.ic_block, "sort" to R.drawable.ic_sort, "filter" to R.drawable.ic_filter,
        "grid" to R.drawable.ic_grid, "chat" to R.drawable.ic_chat, "schedule" to R.drawable.ic_schedule,
        "more" to R.drawable.ic_more_vert, "menu" to R.drawable.ic_menu,
        "light_mode" to R.drawable.ic_light_mode, "dark_mode" to R.drawable.ic_dark_mode,
        "chevron_up" to R.drawable.ic_chevron_up, "chevron_down" to R.drawable.ic_chevron_down,
        "arrow_back" to R.drawable.ic_arrow_back,
    )

                  
    fun render(page: JsonObject, container: ViewGroup) {
        lastPage = page
        container.removeAllViews()
        inputs.clear()
                                                  
        val ver = page.get("gcui")?.takeIf { it.isJsonPrimitive }?.asInt ?: SduiCapabilities.GCUI_VERSION
        if (ver > SduiCapabilities.GCUI_VERSION) {
            container.addView(unsupportedPage(ver), ViewGroup.LayoutParams.MATCH_PARENT)
            return
        }
        state = page.getAsJsonObject("state")
        val root = page.getAsJsonObject("root") ?: return
        val scope = baseScope()
        renderNode(root, scope)?.let { container.addView(it) }
    }

                                        
    private fun unsupportedPage(ver: Int): View {
        val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        col.addView(text2("此页面需要更新的 Gay Core", style = "title3"))
        col.addView(text2("页面要求 gcui $ver，当前 App 支持 gcui ${SduiCapabilities.GCUI_VERSION}", style = "caption"))
        col.addView(text2("请升级 App 后再试。", style = "caption"))
        return col
    }

    private fun baseScope(): Map<String, Any?> =
        mapOf("state" to state, "user" to host.userScope())

    private fun scopeWithItem(scope: Map<String, Any?>, item: Any?): Map<String, Any?> =
        scope + ("item" to item)

                                       
    fun renderNode(node: JsonObject, scope: Map<String, Any?>): View? {
                        
        if (node.has("visible")) {
            val v = Template.bindRaw(node.get("visible").let { if (it.isJsonPrimitive) it.asString else null }, scope)
            if (!Template.truthy(v)) return null
        }
        val type = node.get("type")?.asString ?: return null
        return try {
            when (type) {
                "column" -> linear(node, scope, LinearLayout.VERTICAL)
                "row" -> linear(node, scope, LinearLayout.HORIZONTAL)
                "card" -> card(node, scope)
                "text" -> text(node, scope)
                "button" -> button(node, scope)
                "input" -> input(node, scope)
                "switch" -> switch(node, scope)
                "progress" -> progress(node, scope)
                "kv" -> kv(node, scope)
                "image" -> image(node, scope)
                "icon" -> icon(node, scope)
                "divider" -> divider()
                "spacer" -> Space(context).apply { layoutParams = ViewGroup.LayoutParams(1, d(node.get("height")?.asFloat ?: 8f)) }
                "radio" -> radio(node, scope)
                "chip" -> chip(node, scope)
                "badge" -> badge(node, scope)
                "segmented" -> segmented(node, scope)
                "avatar" -> avatar(node, scope)
                "list" -> list(node, scope)
                "iconButton" -> iconButton(node, scope)
                "hscroll" -> hscroll(node, scope)
                "listRow" -> listRow(node, scope)
                "metricTile" -> metricTile(node, scope)
                "metricCard" -> metricCard(node, scope)
                else -> text2("未知组件: $type", isError = true)
            }
        } catch (e: Exception) {
            text2("组件渲染失败: $type (${e.message})", isError = true)              
        }
    }

                                  
    private fun applyChildren(lp: LinearLayout, node: JsonObject, scope: Map<String, Any?>) {
        val gap = d(node.get("gap")?.asFloat ?: 0f)
        val pad = node.get("padding")?.asFloat
        if (pad != null) lp.setPadding(d(pad), d(pad), d(pad), d(pad))
        val children = node.getAsJsonArray("children") ?: return
        var first = true
        for (c in children) {
            if (!c.isJsonObject) continue
            val v = renderNode(c.asJsonObject, scope) ?: continue
            val co = c.asJsonObject
            val weight = co.get("weight")?.asFloat ?: 0f
            val lpp = if (weight > 0f) {
                if (lp.orientation == LinearLayout.HORIZONTAL) LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
                else LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, weight)
            } else {
                if (lp.orientation == LinearLayout.HORIZONTAL) LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                else LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            if (!first && gap > 0) {
                if (lp.orientation == LinearLayout.VERTICAL) lpp.topMargin = gap else lpp.marginStart = gap
            }
            first = false
            lp.addView(v, lpp)
        }
    }

    private fun linear(node: JsonObject, scope: Map<String, Any?>, orientation: Int): View {
        val lp = LinearLayout(context)
        lp.orientation = orientation
        if (orientation == LinearLayout.HORIZONTAL) lp.gravity = Gravity.CENTER_VERTICAL
        applyChildren(lp, node, scope)
        return lp
    }

    private fun card(node: JsonObject, scope: Map<String, Any?>): View {
                                              
                                                                                       
        val inner = LinearLayout(context)
        inner.orientation = LinearLayout.VERTICAL
        val variant = node.get("variant")?.asString ?: "filled"
        val shape = node.get("shape")?.asString ?: "extraLarge"
        inner.background = if (variant == "outlined") {
            shapeBg(
                theme.color(context, "surfaceContainerLowest"), shapeRadius(shape),
                d(1).toFloat(), theme.color(context, "outlineVariant"),
            )
        } else {
            shapeBg(theme.color(context, "surfaceContainer"), shapeRadius(shape))
        }
        inner.setPadding(d(16), d(14), d(16), d(14))
        val title = node.get("title")?.asString
        if (!title.isNullOrEmpty()) {
            inner.addView(text2(Template.bind(title, scope), style = "title3"))
            inner.addView(Space(context).apply { layoutParams = ViewGroup.LayoutParams(1, d(8)) })
        }
        applyChildren(inner, node, scope)
                                                
        val action = node.getAsJsonObject("action")
        if (action != null) {
            inner.isClickable = true; inner.isFocusable = true
            inner.setOnClickListener { host.onAction(deepCopy(action)) }
        }
        val longAction = node.getAsJsonObject("longAction")
        if (longAction != null) {
            inner.isClickable = true; inner.isFocusable = true
            inner.setOnLongClickListener { host.onAction(deepCopy(longAction)); true }
        }
        inner.layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return inner
    }

                                  
    private fun text2(t: String, style: String = "body", colorName: String? = null, isError: Boolean = false): TextView {
        val tv = TextView(context)
        tv.text = t
        when (style) {
            "title" -> { tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f); tv.setTypeface(Typeface.DEFAULT_BOLD); tv.setTextColor(theme.color(context, "onSurface")) }
            "title3" -> { tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); tv.setTypeface(Typeface.DEFAULT_BOLD); tv.setTextColor(theme.color(context, "onSurface")) }
            "caption" -> { tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f); tv.setTextColor(theme.color(context, "onSurfaceVariant")) }
            "mono" -> { tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f); tv.typeface = Typeface.MONOSPACE; tv.setTextColor(theme.color(context, "onSurface")) }
            else -> { tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f); tv.setTextColor(theme.color(context, "onSurface")) }
        }
                                                  
        if (colorName != null && colorName.startsWith("$")) tv.setTextColor(theme.color(context, colorName))
        if (isError) tv.setTextColor(theme.color(context, "error"))
        return tv
    }

    private fun text(node: JsonObject, scope: Map<String, Any?>): View {
        val raw = node.get("text")?.asString ?: ""
        return text2(
            Template.bind(raw, scope),
            style = node.get("style")?.asString ?: "body",
            colorName = node.get("color")?.asString,
        )
    }

    private fun kv(node: JsonObject, scope: Map<String, Any?>): View {
        val row = LinearLayout(context); row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val label = text2(Template.bind(node.get("label")?.asString ?: "", scope), style = "caption")
        val value = text2(Template.bind(node.get("value")?.asString ?: "", scope))
        row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(value)
        return row
    }

    private fun divider(): View {
        val v = View(context)
        v.setBackgroundColor(theme.color(context, "outline"))
        v.alpha = 0.3f
        v.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
        return v
    }

    private fun icon(node: JsonObject, scope: Map<String, Any?>): View {
        val iv = ImageView(context)
        val name = Template.bind(node.get("name")?.asString ?: "", scope).removePrefix("msym:")
        iv.setImageResource(iconMap[name] ?: R.drawable.ic_info)
        val size = d(node.get("size")?.asFloat ?: 24f)
        return wrapFixed(iv, size, size)
    }

                                                  
    private val imgCache = LruCache<String, Bitmap>(32)
    private val imgPool = Executors.newFixedThreadPool(3)
    private fun image(node: JsonObject, scope: Map<String, Any?>): View {
        val iv = ImageView(context)
        val url = Template.bind(node.get("url")?.asString ?: "", scope)
        val h = node.get("height")?.asFloat
        iv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, if (h != null) d(h) else ViewGroup.LayoutParams.WRAP_CONTENT)
        iv.adjustViewBounds = true
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        if (url.startsWith("http://") || url.startsWith("https://")) {
            imgCache.get(url)?.let { iv.setImageBitmap(it); return iv }
            iv.tag = url
            imgPool.execute {
                try {
                    val bmp = BitmapFactory.decodeStream(URL(url).openStream())
                    if (bmp != null) {
                        imgCache.put(url, bmp)
                        iv.post { if (iv.tag == url) iv.setImageBitmap(bmp) }
                    }
                } catch (_: Exception) {}
            }
        }
        return iv
    }

                                  
                                                                    
    private fun button(node: JsonObject, scope: Map<String, Any?>): View {
        val b = android.widget.Button(context)
        val label = Template.bind(node.get("text")?.asString ?: "", scope)
        b.text = label
        b.isAllCaps = false
        b.textSize = 14f
        b.minimumHeight = 0
        b.minimumWidth = 0
        b.stateListAnimator = null
        b.setPadding(d(20), d(10), d(20), d(10))
                                                      
        val radius = shapeRadius(node.get("shape")?.asString ?: "large")
        when (node.get("style")?.asString ?: "filled") {
            "tonal" -> {
                b.background = shapeBg(theme.color(context, "secondaryContainer"), radius)
                b.setTextColor(theme.color(context, "onSurface"))
            }
            "outlined" -> {
                b.background = shapeBg(android.graphics.Color.TRANSPARENT, radius, d(1).toFloat(), theme.color(context, "outline"))
                b.setTextColor(theme.color(context, "primary"))
            }
            "text" -> {
                b.background = null
                b.setTextColor(theme.color(context, "primary"))
            }
            else -> {
                b.background = shapeBg(theme.color(context, "primary"), radius)
                b.setTextColor(theme.color(context, "onPrimary"))
            }
        }
                                     
        val iconName = node.get("icon")?.asString?.removePrefix("msym:")
        var hasIcon = false
        if (!iconName.isNullOrEmpty()) {
            iconMap[iconName]?.let { resId ->
                val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId)?.mutate()
                if (drawable != null) {
                    val sz = d(node.get("iconSize")?.asFloat ?: 18f); drawable.setBounds(0, 0, sz, sz)
                    drawable.setTint(b.currentTextColor)
                    b.setCompoundDrawables(drawable, null, null, null)
                    b.compoundDrawablePadding = d(6)
                    hasIcon = true
                }
            }
        }
                                                      
        if (label.isBlank() && hasIcon) {
            val sz = d(num(node, "size", scope, 36f))
            b.minimumWidth = sz; b.minimumHeight = sz
            b.setPadding(d(6), d(6), d(6), d(6))
            return wrapFixed(b, sz, sz)                    
        }
        val action = node.getAsJsonObject("action")
        if (action != null) {
            b.setOnClickListener { host.onAction(deepCopy(action)) }
        }
        return b
    }

    private fun deepCopy(o: JsonObject): JsonObject = JsonObject().apply {
        for ((k, v) in o.entrySet()) add(k, v)
    }

    private fun input(node: JsonObject, scope: Map<String, Any?>): View {
                                                                
        val key = node.get("key")?.asString ?: return text2("input 缺 key", isError = true)
        val label = Template.bind(node.get("label")?.asString ?: "", scope)
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        if (label.isNotEmpty()) {
            val lab = text2(label, style = "caption")
            col.addView(lab)
            col.addView(Space(context).apply { layoutParams = ViewGroup.LayoutParams(1, d(4)) })
        }
        val et = EditText(context)
        et.setText(Template.bind(node.get("value")?.asString ?: "", scope))
        et.hint = label
        et.setTextColor(theme.color(context, "onSurface"))
        et.setHintTextColor(theme.color(context, "onSurfaceVariant"))
        et.background = shapeBg(
            theme.color(context, "surfaceContainerHighest"),
            shapeRadius(node.get("shape")?.asString ?: "large"),
            d(1).toFloat(),
            theme.color(context, "outlineVariant"),
        )
        et.setPadding(d(14), d(10), d(14), d(10))
                                                 
        val leadIcon = node.get("leadingIcon")?.asString?.removePrefix("msym:")
        if (!leadIcon.isNullOrEmpty()) {
            iconMap[leadIcon]?.let { res ->
                val dw = androidx.core.content.ContextCompat.getDrawable(context, res)?.mutate()
                if (dw != null) {
                    val sz = d(18); dw.setBounds(0, 0, sz, sz)
                    dw.setTint(theme.color(context, "onSurfaceVariant"))
                    et.setCompoundDrawables(dw, null, null, null)
                    et.compoundDrawablePadding = d(8)
                }
            }
        }
        when (node.get("inputType")?.asString) {
            "number" -> et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            "password" -> et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else -> et.inputType = InputType.TYPE_CLASS_TEXT
        }
                                               
        if (Template.truthy(Template.bindRaw(node.get("multiline")?.let { if (it.isJsonPrimitive) it.asString else null }, scope))) {
            et.inputType = et.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            et.gravity = Gravity.TOP or Gravity.START
            et.setLines(4)
            et.minHeight = d(num(node, "height", scope, 104f))
        }
        inputs[key] = et.text?.toString() ?: ""
        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { inputs[key] = s?.toString() ?: "" }
        })
        col.addView(et)
        return col
    }

    private fun switch(node: JsonObject, scope: Map<String, Any?>): View {
        val sw = MaterialSwitch(context)
        sw.text = Template.bind(node.get("label")?.asString ?: "", scope)
        sw.setTextColor(theme.color(context, "onSurface"))
        val checked = Template.bindRaw(node.get("checked")?.asString, scope)
        sw.isChecked = Template.truthy(checked)
        val action = node.getAsJsonObject("action")
        if (action != null) {
            sw.setOnCheckedChangeListener { _, isChecked ->
                val a = deepCopy(action)
                a.addProperty("_checked", isChecked)                                        
                inputs["_checked"] = isChecked.toString()
                host.onAction(a)
            }
        }
        return sw
    }

    private fun progress(node: JsonObject, scope: Map<String, Any?>): View {
        val p = LinearProgressIndicator(context)
        val value = (Template.bindRaw(node.get("value")?.asString, scope) as? Number)?.toInt() ?: node.get("value")?.asInt ?: 0
        val max = (Template.bindRaw(node.get("max")?.asString, scope) as? Number)?.toInt() ?: node.get("max")?.asInt ?: 100
        p.max = if (max > 0) max else 100
        p.setProgressCompat(value.coerceIn(0, p.max), false)
        return p
    }

                                  
    
                                                           
    private fun radio(node: JsonObject, scope: Map<String, Any?>): View {
        val rb = android.widget.RadioButton(context)
        rb.text = Template.bind(node.get("text")?.asString ?: "", scope)
        rb.isChecked = Template.truthy(Template.bindRaw(node.get("checked")?.asString, scope))
        rb.setTextColor(theme.color(context, "onSurface"))
        rb.setPadding(d(8), d(8), d(8), d(8))
        val action = node.getAsJsonObject("action")
        if (action != null) rb.setOnClickListener { host.onAction(deepCopy(action)) }
        return rb
    }

                                                     
                                                                                                     
                                                               
    private fun chip(node: JsonObject, scope: Map<String, Any?>): View {
        val tv = TextView(context)
        tv.text = Template.bind(node.get("text")?.asString ?: "", scope)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        tv.setPadding(d(14), d(8), d(14), d(8))
        tv.gravity = Gravity.CENTER
        tv.isClickable = true
        tv.isFocusable = true
        val iconRes = node.get("icon")?.asString?.removePrefix("msym:")?.let { iconMap[it] }

        fun paint(sel: Boolean) {
            if (sel) {
                tv.background = shapeBg(
                    theme.color(context, "primaryContainer"), 1000f,
                    d(1).toFloat(), theme.color(context, "primary"),
                )
                tv.setTextColor(theme.color(context, "primary"))
            } else {
                tv.background = shapeBg(
                    android.graphics.Color.TRANSPARENT, 1000f,
                    d(1).toFloat(), theme.color(context, "outline"),
                )
                tv.setTextColor(theme.color(context, "onSurfaceVariant"))
            }
            if (iconRes != null) {
                val dw = androidx.core.content.ContextCompat.getDrawable(context, iconRes)?.mutate()
                if (dw != null) {
                    val sz = d(16); dw.setBounds(0, 0, sz, sz)
                    dw.setTint(tv.currentTextColor)
                    tv.setCompoundDrawables(dw, null, null, null)
                    tv.compoundDrawablePadding = d(5)
                }
            }
        }
        val hasSelected = node.has("selected")
        var cur = Template.truthy(Template.bindRaw(node.get("selected")?.asString, scope))
        paint(cur)

        val action = node.getAsJsonObject("action")
        if (action != null) {
            tv.setOnClickListener {
                if (hasSelected) { cur = !cur; paint(cur); inputs["_selected"] = cur.toString() }
                val a = Template.bindJson(action, scope)
                val out = if (a.isJsonObject) a.asJsonObject else deepCopy(action)
                if (hasSelected) out.addProperty("_selected", cur)
                host.onAction(out)
            }
        }
        return tv
    }

                                   
                                                                                                     
    private fun badge(node: JsonObject, scope: Map<String, Any?>): View {
        val tv = TextView(context)
        tv.text = Template.bind(node.get("text")?.asString ?: "", scope)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        tv.setPadding(d(9), d(3), d(9), d(3))
                                                 
        val toneRaw = node.get("tone")?.let { if (it.isJsonPrimitive) it.asString else null }
        val tone = Template.bindRaw(toneRaw, scope)?.toString() ?: "neutral"
        val (bg, fg) = when (tone) {
            "success" -> "successContainer" to "success"
            "error" -> "errorContainer" to "error"
            "warning" -> "tertiaryContainer" to "onTertiaryContainer"
            "primary" -> "primaryContainer" to "primary"
            "tertiary" -> "tertiaryContainer" to "tertiary"
            else -> "surfaceContainerHighest" to "onSurfaceVariant"
        }
        tv.setTextColor(theme.color(context, fg))
        tv.background = shapeBg(theme.color(context, bg), 1000f)
        return tv
    }

                                
                                                                                                                                         
                                                                  
    private fun segmented(node: JsonObject, scope: Map<String, Any?>): View {
        val wrap = LinearLayout(context)
        wrap.orientation = LinearLayout.HORIZONTAL
        wrap.background = shapeBg(theme.color(context, "surfaceContainerHighest"), 1000f)
        wrap.setPadding(d(4), d(4), d(4), d(4))
        val options = node.getAsJsonArray("options") ?: return wrap
        val selectedVal = Template.bindRaw(node.get("selected")?.asString, scope)?.toString() ?: ""
        val action = node.getAsJsonObject("action")
        val views = mutableListOf<Pair<TextView, String>>()

        for (opt in options) {
            if (!opt.isJsonObject) continue
            val o = opt.asJsonObject
            val value = o.get("value")?.asString ?: ""
            val tv = TextView(context)
            tv.text = Template.bind(o.get("text")?.asString ?: "", scope)
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            tv.gravity = Gravity.CENTER
            tv.setPadding(d(10), d(9), d(10), d(9))
            tv.isClickable = true
            tv.isFocusable = true
            tv.setOnClickListener {
                views.forEach { (v, val2) -> paintSegment(v, val2 == value) }
                if (action != null) {
                    val childScope = scopeWithItem(scope, mapOf("value" to value, "text" to tv.text.toString()))
                    val a = Template.bindJson(action, childScope)
                    val out = if (a.isJsonObject) a.asJsonObject else deepCopy(action)
                    out.addProperty("_value", value)
                    inputs["_value"] = value
                    host.onAction(out)
                }
            }
            paintSegment(tv, value == selectedVal)
            views.add(tv to value)
            wrap.addView(tv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        return wrap
    }

    private fun paintSegment(tv: TextView, sel: Boolean) {
        if (sel) {
            tv.background = shapeBg(theme.color(context, "surfaceContainerLowest"), 1000f)
            tv.setTextColor(theme.color(context, "onSurface"))
            tv.setTypeface(Typeface.DEFAULT_BOLD)
            tv.elevation = d(1).toFloat()
        } else {
            tv.background = shapeBg(android.graphics.Color.TRANSPARENT, 1000f)
            tv.setTextColor(theme.color(context, "onSurfaceVariant"))
            tv.setTypeface(Typeface.DEFAULT)
            tv.elevation = 0f
        }
    }

                                             
                                                       
       
                                    
      
                                                        
                                                          
                                 
       
    private fun wrapFixed(inner: View, w: Int, h: Int): View {
        val box = FrameLayout(context)
        box.addView(inner, FrameLayout.LayoutParams(w, h, Gravity.CENTER_VERTICAL))
        return box
    }

    private fun avatar(node: JsonObject, scope: Map<String, Any?>): View {
        val dpSize0 = num(node, "size", scope, 44f)
        val size0 = d(dpSize0)
                                          
        val url = Template.bind(node.get("url")?.asString ?: node.get("image")?.asString ?: "", scope)
        if (url.startsWith("http://") || url.startsWith("https://")) {
            val iv = ImageView(context)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.setBackgroundColor(theme.color(context, "primaryContainer"))
            val fallback = Template.bind(node.get("text")?.asString ?: "", scope)
            com.gaycore.app.data.ImageLoader.load(url, iv, circular = true) {
                                  
                val tv = textAvatar(fallback, size0, dpSize0)
                (iv.parent as? android.view.ViewGroup)?.let { p0 ->
                    val i = p0.indexOfChild(iv)
                    if (i >= 0) { p0.removeViewAt(i); p0.addView(tv, i, iv.layoutParams) }
                }
            }
            return wrapFixed(iv, size0, size0)
        }
        return wrapFixed(textAvatar(Template.bind(node.get("text")?.asString ?: "", scope), size0, dpSize0), size0, size0)
    }

                         
    private fun textAvatar(text: String, size: Int, dpSize: Float): TextView {
        val tv = TextView(context)
        tv.text = text
        tv.gravity = Gravity.CENTER
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, dpSize * 0.34f)
        tv.setTypeface(Typeface.DEFAULT_BOLD)
        tv.setTextColor(theme.color(context, "onPrimary"))
        val gd = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            intArrayOf(theme.color(context, "primary"), theme.color(context, "tertiary")))
        gd.shape = android.graphics.drawable.GradientDrawable.OVAL
        tv.background = gd
        return tv
    }

                                                
                                                                                           
                                                                           
    private fun iconButton(node: JsonObject, scope: Map<String, Any?>): View {
        val container = node.get("container")?.asString ?: "none"
        val attr = when (container) {
            "filled", "tonal" -> com.google.android.material.R.attr.materialButtonStyle
            "outlined" -> com.google.android.material.R.attr.materialButtonOutlinedStyle
            else -> android.R.attr.borderlessButtonStyle
        }
        val b = com.google.android.material.button.MaterialButton(context, null, attr)
        b.text = ""
        b.isAllCaps = false
        b.iconPadding = 0
        b.insetTop = 0
        b.insetBottom = 0
        b.minWidth = 0
        b.minimumWidth = 0
        node.get("icon")?.asString?.removePrefix("msym:")?.let { iconMap[it] }?.let { b.setIconResource(it) }
        val tone = node.get("tone")?.asString ?: "default"
        val fg = when (tone) {
            "primary" -> "primary"
            "error" -> "error"
            else -> "onSurfaceVariant"
        }
        b.iconTint = android.content.res.ColorStateList.valueOf(theme.color(context, fg))
        if (container != "none") {
            val fill = when (tone) {
                "primary" -> "primaryContainer"
                "error" -> "errorContainer"
                else -> "surfaceContainerHighest"
            }
            b.backgroundTintList = android.content.res.ColorStateList.valueOf(theme.color(context, fill))
            b.strokeWidth = 0
        }
        val side = (num(node, "size", scope, 24f) + 24f).toInt()
        b.minimumHeight = d(side)
        b.contentDescription = Template.bind(node.get("desc")?.asString ?: "", scope)
        node.getAsJsonObject("action")?.let { a -> b.setOnClickListener { host.onAction(deepCopy(a)) } }
        return b
    }

                                             
                                                           
    private fun hscroll(node: JsonObject, scope: Map<String, Any?>): View {
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        applyChildren(row, node, scope)
        return android.widget.HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            addView(
                row,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT),
            )
        }
    }

                                                         
                                                                                
                                                                            
    private fun listRow(node: JsonObject, scope: Map<String, Any?>): View {
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(d(16), d(12), d(16), d(12))
        row.background = shapeBg(theme.color(context, "surfaceContainer"), shapeRadius("large"))
        val tone = node.get("tone")?.asString ?: "onSurface"
        node.get("icon")?.asString?.removePrefix("msym:")?.let { iconMap[it] }?.let { res ->
            row.addView(
                ImageView(context).apply {
                    setImageResource(res)
                    setColorFilter(theme.color(context, if (tone == "error") "error" else "onSurfaceVariant"))
                },
                LinearLayout.LayoutParams(d(22), d(22)).apply { marginEnd = d(14) },
            )
        }
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.addView(text2(Template.bind(node.get("text")?.asString ?: "", scope), style = "body",
            colorName = if (tone == "error") "\$error" else null))
        val desc = Template.bind(node.get("desc")?.asString ?: "", scope)
        if (desc.isNotEmpty()) col.addView(text2(desc, style = "caption"))
        row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val value = Template.bind(node.get("value")?.asString ?: "", scope)
        if (value.isNotEmpty()) {
            row.addView(text2(value, style = "caption"),
                LinearLayout.LayoutParams(-2, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = d(8) })
        }
        if (node.get("chevron")?.let { !it.isJsonPrimitive || it.asBoolean } != false) {
            row.addView(
                ImageView(context).apply {
                    setImageResource(R.drawable.ic_chevron_right)
                    setColorFilter(theme.color(context, "outline"))
                },
                LinearLayout.LayoutParams(d(18), d(18)),
            )
        }
        node.getAsJsonObject("action")?.let { a ->
            row.isClickable = true
            row.isFocusable = true
            row.setOnClickListener { host.onAction(deepCopy(a)) }
        }
        return row
    }

                                            
                                                                 
    private fun metricTile(node: JsonObject, scope: Map<String, Any?>): View {
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(d(12), d(12), d(12), d(12))
        col.background = shapeBg(theme.color(context, "surfaceContainer"), shapeRadius("large"))
        col.addView(text2(Template.bind(node.get("value")?.asString ?: "", scope), style = "title"))
        col.addView(text2(Template.bind(node.get("label")?.asString ?: "", scope), style = "caption"))
        return col
    }

                                                        
                                                                               
                                                         
    private fun metricCard(node: JsonObject, scope: Map<String, Any?>): View {
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(d(14), d(14), d(14), d(14))
        col.background = shapeBg(theme.color(context, "surfaceContainer"), shapeRadius("large"))
        val tintName = node.get("tint")?.asString ?: "primary"
        val tint = theme.color(context, tintName)
        node.get("icon")?.asString?.removePrefix("msym:")?.let { iconMap[it] }?.let { res ->
            col.addView(
                ImageView(context).apply {
                    setImageResource(res)
                    setColorFilter(tint)
                },
                LinearLayout.LayoutParams(d(22), d(22)),
            )
        }
        col.addView(text2(Template.bind(node.get("value")?.asString ?: "", scope), style = "title"))
        col.addView(text2(Template.bind(node.get("label")?.asString ?: "", scope), style = "caption"))
        return col
    }

    private fun list(node: JsonObject, scope: Map<String, Any?>): View {
        val lp = LinearLayout(context); lp.orientation = LinearLayout.VERTICAL
        val itemsRef = node.get("items")?.asString ?: ""
        val items = Template.bindRaw(itemsRef, scope)
        val arr: JsonArray? = when (items) {
            is JsonArray -> items
            is JsonObject -> null
            else -> null
        }
        val template = node.getAsJsonObject("template")
        if (arr == null || template == null) {
            lp.addView(text2("(空列表)", style = "caption"))
            return lp
        }
        val gap = d(8f)
                                                             
        val cols = num(node, "columns", scope, 1f).toInt().coerceIn(1, 6)
        if (cols > 1) {
            val colGap = d(8f)
            var i = 0
            while (i < arr.size()) {
                val rowLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                if (i > 0) rowLp.topMargin = gap
                val rowView = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                var c = 0
                while (c < cols && i < arr.size()) {
                    val childScope = scopeWithItem(scope, Template.unwrap(arr[i]))
                    val v = renderNode(template, childScope)
                    if (v != null) {
                        val cellLp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        if (c > 0) cellLp.marginStart = colGap
                        rowView.addView(v, cellLp)
                    }
                    c++; i++
                }
                while (c < cols) {               
                    rowView.addView(Space(context), LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = colGap })
                    c++
                }
                lp.addView(rowView, rowLp)
            }
            return lp
        }
        var first = true
        for (item in arr) {
            val childScope = scopeWithItem(scope, Template.unwrap(item))
            val v = renderNode(template, childScope) ?: continue
            val lpp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (!first) lpp.topMargin = gap
            first = false
            lp.addView(v, lpp)
        }
        if (arr.size() == 0) lp.addView(text2("(空列表)", style = "caption"))
        return lp
    }
}
