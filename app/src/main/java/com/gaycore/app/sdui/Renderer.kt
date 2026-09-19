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
import com.gaycore.app.ui.MdField
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

    
    val selected = linkedSetOf<String>()

    


    private val selViews = ArrayList<Pair<TextView, String>>()

    

    private val selChecks = ArrayList<Pair<com.google.android.material.checkbox.MaterialCheckBox, String>>()

    
    private var refreshingSel = false

    
    fun refreshSelectedDependent() {
        refreshingSel = true
        try {
            if (selViews.isNotEmpty()) {
                val sc = baseScope()
                for ((tv, raw) in selViews) {
                    val t = Template.bind(raw, sc)
                    if (tv.text != t) tv.text = t
                }
            }
            for ((cb, v) in selChecks) {
                val want = v in selected
                if (cb.isChecked != want) cb.isChecked = want
            }
        } finally {
            refreshingSel = false
        }
    }
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
        


        val sv = nearestScroller(container)
        val sy = scrollYOf(sv)
        selViews.clear()
        selChecks.clear()
        container.removeAllViews()
        state = merged.getAsJsonObject("state")
        val root = merged.getAsJsonObject("root") ?: return
        renderNode(root, baseScope())?.let { container.addView(it) }
        inputs.putAll(saved)
        if (sv != null && sy > 0) sv.post { restoreScroll(sv, sy) }
    }

    





    fun nearestScroller(v: View?): View? {
        var p: android.view.ViewParent? = v?.parent
        while (p != null) {
            if (p is android.widget.ScrollView || p is androidx.core.widget.NestedScrollView) return p as View
            p = p.parent
        }
        return null
    }

    fun scrollYOf(sv: View?): Int = when (sv) {
        is android.widget.ScrollView -> sv.scrollY
        is androidx.core.widget.NestedScrollView -> sv.scrollY
        else -> 0
    }

    
    fun restoreScroll(sv: View?, y: Int) {
        if (sv == null) return
        val child = (sv as? ViewGroup)?.getChildAt(0) ?: return
        val max = (child.height - sv.height).coerceAtLeast(0)
        val ty = y.coerceIn(0, max)
        when (sv) {
            is android.widget.ScrollView -> sv.scrollTo(0, ty)
            is androidx.core.widget.NestedScrollView -> sv.scrollTo(0, ty)
        }
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
    
    private fun blendColor(a: Int, b: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        fun ch(x: Int, y: Int) = (x + (y - x) * r).toInt().coerceIn(0, 255)
        return android.graphics.Color.rgb(ch(android.graphics.Color.red(a), android.graphics.Color.red(b)),
            ch(android.graphics.Color.green(a), android.graphics.Color.green(b)),
            ch(android.graphics.Color.blue(a), android.graphics.Color.blue(b)))
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

    


    private val iconMap = SduiIcons.map

    
    fun render(page: JsonObject, container: ViewGroup) {
        lastPage = page
        container.removeAllViews()
        inputs.clear()
        selViews.clear()
        selChecks.clear()
        
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
        mapOf(
            "state" to state,
            "user" to host.userScope(),
            
            "selected" to selected.toList(),
            "selectedCount" to selected.size,
        )

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
                

                "spacer" -> com.gaycore.app.ui.DemoKit.FixedHeightView(
                    context, d(node.get("height")?.asFloat ?: 8f),
                )
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
                
                "pagination" -> pagination(node, scope)
                "checkbox" -> checkbox(node, scope)
                "form" -> form(node, scope)
                "chart" -> chart(node, scope)
                else -> text2("未知组件: $type", isError = true)
            }
        } catch (e: Exception) {
            text2("组件渲染失败: $type (${e.message})", isError = true) 
        }
    }

    
    


    private fun hasSpacerChild(node: JsonObject): Boolean {
        val arr = node.get("children")?.takeIf { it.isJsonArray }?.asJsonArray ?: return false
        for (c in arr) {
            val t = c.takeIf { it.isJsonObject }?.asJsonObject
                ?.get("type")?.takeIf { it.isJsonPrimitive }?.asString
            if (t == "spacer") return true
        }
        return false
    }

    private fun applyChildren(lp: LinearLayout, node: JsonObject, scope: Map<String, Any?>) {
        



        val defGap = if (lp.orientation == LinearLayout.VERTICAL && !hasSpacerChild(node)) 8f else 0f
        val gap = d(node.get("gap")?.asFloat ?: defGap)
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
        val nodeVariant = node.get("variant")?.asString
        val style = if (nodeVariant != null && nodeVariant != "outlined") nodeVariant else theme.cardStyle()
        

        val shape = node.get("shape")?.asString
        val r = if (shape != null) shapeRadius(shape) else d(theme.shapeDp("card", 16)).toFloat()
        val sw = theme.strokeWidthDp()
        when (style) {
            "filled" -> inner.background = shapeBg(theme.color(context, "surfaceContainer"), r)
            "elevated" -> {
                inner.background = shapeBg(theme.color(context, "surface"), r)
                inner.elevation = d(2).toFloat()
            }
            "tonal" -> inner.background = shapeBg(blendColor(theme.color(context, "surface"), theme.color(context, "primary"), 0.10f), r)
            "gradient" -> inner.background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(theme.color(context, "surface"), theme.color(context, "surfaceContainer")),
            ).apply { cornerRadius = r }
            else -> inner.background = shapeBg(
                theme.color(context, "surfaceContainerLowest"), r, sw, theme.color(context, "outlineVariant"),
            )
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
            inner.setOnClickListener { host.onAction(bindAction(action, scope)) }
        }
        val longAction = node.getAsJsonObject("longAction")
        if (longAction != null) {
            inner.isClickable = true; inner.isFocusable = true
            inner.setOnLongClickListener { host.onAction(bindAction(longAction, scope)); true }
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
        val tv = text2(
            Template.bind(raw, scope),
            style = node.get("style")?.asString ?: "body",
            colorName = node.get("color")?.asString,
        )
        
        if (raw.contains("{{selected")) selViews.add(tv to raw)
        

        val ml = node.get("maxLines")?.takeIf { it.isJsonPrimitive }?.asInt ?: 0
        if (ml > 0) {
            tv.maxLines = ml
            tv.ellipsize = android.text.TextUtils.TruncateAt.END
            if (ml == 1) tv.isSingleLine = true
        }
        return tv
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
        

        val v = com.gaycore.app.ui.DemoKit.FixedHeightView(context, 1)
        v.setBackgroundColor(theme.color(context, "outline"))
        v.alpha = 0.3f
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
        
        val shapeName = node.get("shape")?.asString
        val radius = if (shapeName != null) shapeRadius(shapeName) else d(theme.shapeDp("btn", 12)).toFloat()
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
            b.setOnClickListener { host.onAction(bindAction(action, scope)) }
        }
        return b
    }

    private fun deepCopy(o: JsonObject): JsonObject = JsonObject().apply {
        for ((k, v) in o.entrySet()) add(k, v)
    }

    



    private fun bindAction(action: JsonObject, scope: Map<String, Any?>): JsonObject {
        




        val b = Template.bindJsonKeep(action, scope - "selected" - "selectedCount")
        return if (b.isJsonObject) b.asJsonObject else deepCopy(action)
    }

    private fun input(node: JsonObject, scope: Map<String, Any?>): View {
        val key = node.get("key")?.asString ?: return text2("input 缺 key", isError = true)
        val label = Template.bind(node.get("label")?.asString ?: "", scope)
        val itype = node.get("inputType")?.asString ?: "text"
        

        val f = MdField(
            context, theme, label,
            
            inputs[key] ?: Template.bind(node.get("value")?.asString ?: "", scope),
            numeric = itype == "number",
            pill = node.get("shape")?.asString == "pill",
        )
        val et = f.edit
        when (itype) {
            "password" -> {
                et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                f.enablePasswordToggle()   
            }
            "number" -> {  }
            else -> et.inputType = InputType.TYPE_CLASS_TEXT
        }
        
        if (Template.truthy(Template.bindRaw(node.get("multiline")?.let { if (it.isJsonPrimitive) it.asString else null }, scope))) {
            f.setMultiline(d(num(node, "height", scope, 104f)))
        }
        
        node.get("leadingIcon")?.asString?.removePrefix("msym:")?.takeIf { it.isNotEmpty() }?.let { nm ->
            iconMap[nm]?.let { res -> f.setLeadingIcon(res) }
        }
        inputs[key] = f.text
        
        val submitAction = node.getAsJsonObject("submit")
        if (submitAction != null) {
            et.imeOptions = et.imeOptions or android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            et.setOnEditorActionListener { _, actionId, _ ->
                val ok = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO
                if (ok) {
                    inputs[key] = f.text
                    host.onAction(bindAction(submitAction, scope))
                }
                ok
            }
        }
        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { inputs[key] = s?.toString() ?: "" }
        })
        return f
    }
    private fun switch(node: JsonObject, scope: Map<String, Any?>): View {
        val sw = MaterialSwitch(context)   
        sw.text = Template.bind(node.get("label")?.asString ?: "", scope)
        sw.setTextColor(theme.color(context, "onSurface"))
        val checked = Template.bindRaw(node.get("checked")?.asString, scope)
        sw.isChecked = Template.truthy(checked)
        val action = node.getAsJsonObject("action")
        val fieldKey = node.get("key")?.asString?.takeIf { it.isNotEmpty() }
        
        if (fieldKey != null) inputs[fieldKey] = sw.isChecked.toString()
        



        if (action != null || fieldKey != null) {
            sw.setOnCheckedChangeListener { _, isChecked ->
                if (action != null) {
                    val a = bindAction(action, scope)
                    a.addProperty("_checked", isChecked) 
                    inputs["_checked"] = isChecked.toString()
                    host.onAction(a)
                }
                if (fieldKey != null) inputs[fieldKey] = isChecked.toString()
            }
        }
        return sw
    }

    private fun progress(node: JsonObject, scope: Map<String, Any?>): View {
        val p = LinearProgressIndicator(context).apply {
            
            trackColor = theme.color(context, "surfaceContainerHighest")
            setIndicatorColor(theme.color(context, "primary"))
            trackCornerRadius = d(4)
        }
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
        if (action != null) rb.setOnClickListener { host.onAction(bindAction(action, scope)) }
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
        
        val chipR = run {
            val sn = node.get("shape")?.asString
            if (sn != null) shapeRadius(sn) else theme.shapeDp("chip", -1).let { if (it < 0) 1000f else d(it).toFloat() }
        }

        fun paint(sel: Boolean) {
            if (sel) {
                tv.background = shapeBg(
                    theme.color(context, "primaryContainer"), chipR,
                    d(1).toFloat(), theme.color(context, "primary"),
                )
                tv.setTextColor(theme.color(context, "primary"))
            } else {
                tv.background = shapeBg(
                    android.graphics.Color.TRANSPARENT, chipR,
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
                val out = bindAction(action, scope)
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
                
                node.get("key")?.asString?.takeIf { it.isNotEmpty() }?.let { k -> inputs[k] = value }
                if (action != null) {
                    val childScope = scopeWithItem(scope, mapOf("value" to value, "text" to tv.text.toString()))
                    val out = bindAction(action, childScope)
                    out.addProperty("_value", value)
                    inputs["_value"] = value
                    host.onAction(out)
                }
            }
            paintSegment(tv, value == selectedVal)
            views.add(tv to value)
            wrap.addView(tv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        node.get("key")?.asString?.takeIf { it.isNotEmpty() }?.let { k -> inputs[k] = selectedVal }
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
            iv.scaleType = ImageView.ScaleType.FIT_CENTER
            


            iv.clipToOutline = true
            iv.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    val r = minOf(view.width, view.height) * 0.24f
                    outline.setRoundRect(0, 0, view.width, view.height, r)
                }
            }
            val fallback = Template.bind(node.get("text")?.asString ?: "", scope)
            com.gaycore.app.data.ImageLoader.load(
                url, iv,
                com.gaycore.app.data.ImageLoader.Shape.LOGO,
                theme.color(context, "surfaceContainerHigh"),
            ) {
                
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
        
        val iconName = Template.bind(node.get("icon")?.asString ?: "", scope).removePrefix("msym:")
        iconMap[iconName]?.let { b.setIconResource(it) }
        
        val tone = Template.bindRaw(node.get("tone")?.asString, scope)?.toString() ?: "default"
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
        node.getAsJsonObject("action")?.let { a -> b.setOnClickListener { host.onAction(bindAction(a, scope)) } }
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
            row.setOnClickListener { host.onAction(bindAction(a, scope)) }
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

    



    



    private fun pagination(node: JsonObject, scope: Map<String, Any?>): View {
        val page = num(node, "page", scope, 1f).toInt().coerceAtLeast(1)
        val size = num(node, "pageSize", scope, 20f).toInt().coerceAtLeast(1)
        val total = num(node, "total", scope, 0f).toInt()
        val pages = maxOf(1, (total + size - 1) / size)
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val action = node.getAsJsonObject("action")
        fun mkBtn(text: String, target: Int, enabled: Boolean): View {
            val tv = TextView(context)
            tv.text = text
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            tv.gravity = Gravity.CENTER
            tv.setPadding(d(12), d(8), d(12), d(8))
            val on = enabled && action != null
            tv.background = shapeBg(
                if (on) theme.color(context, "surfaceContainerHighest") else android.graphics.Color.TRANSPARENT,
                shapeRadius("pill"),
            )
            tv.setTextColor(theme.color(context, if (on) "primary" else "onSurfaceVariant"))
            tv.alpha = if (on) 1f else 0.45f
            if (on) {
                tv.isClickable = true
                tv.isFocusable = true
                tv.setOnClickListener {
                    val out = bindAction(action!!, scope)
                    out.addProperty("_page", target)
                    inputs["_page"] = target.toString()
                    host.onAction(out)
                }
            }
            return tv
        }
        row.addView(mkBtn("上一页", page - 1, page > 1))
        row.addView(
            TextView(context).apply {
                text = "第 $page / $pages 页 · 共 $total 条"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTextColor(theme.color(context, "onSurfaceVariant"))
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        row.addView(mkBtn("下一页", page + 1, page < pages))
        return row
    }

    



    private fun checkbox(node: JsonObject, scope: Map<String, Any?>): View {
        val cb = com.google.android.material.checkbox.MaterialCheckBox(context)
        val value = Template.bind(node.get("value")?.asString ?: "", scope)
        val lab = Template.bind(node.get("label")?.asString ?: "", scope)
        if (lab.isNotEmpty()) cb.text = lab
        cb.setTextColor(theme.color(context, "onSurface"))
        cb.isChecked = Template.truthy(Template.bindRaw(node.get("checked")?.asString, scope)) || value in selected
        if (value.isNotEmpty()) selChecks.add(cb to value)
        cb.setOnCheckedChangeListener { _, isChecked ->
            if (refreshingSel) return@setOnCheckedChangeListener
            if (isChecked) selected.add(value) else selected.remove(value)
            val action = node.getAsJsonObject("action")
            if (action != null) {
                val out = bindAction(action, scope)
                out.addProperty("_checked", isChecked)
                out.addProperty("_value", value)
                host.onAction(out)
            } else {
                

                refreshSelectedDependent()
            }
        }
        return cb
    }

    






    private fun form(node: JsonObject, scope: Map<String, Any?>): View {
        val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val fields = node.getAsJsonArray("fields")
        val keys = ArrayList<String>()
        if (fields != null) {
            var first = true
            for (f in fields) {
                if (!f.isJsonObject) continue
                val fo = f.asJsonObject
                val v = renderNode(fo, scope) ?: continue
                if (!first) col.addView(Space(context).apply { layoutParams = ViewGroup.LayoutParams(1, d(10)) })
                col.addView(v)
                first = false
                fo.get("key")?.asString?.takeIf { it.isNotEmpty() }?.let { keys.add(it) }
            }
        }
        val submit = node.getAsJsonObject("submit")
        if (submit != null) {
            val b = android.widget.Button(context)
            b.text = Template.bind(node.get("submitText")?.asString ?: "提交", scope)
            b.isAllCaps = false
            b.textSize = 14f
            b.minimumHeight = 0
            b.minimumWidth = 0
            b.stateListAnimator = null
            b.setPadding(d(20), d(10), d(20), d(10))
            b.background = shapeBg(
                theme.color(context, "primary"),
                shapeRadius(node.get("submitShape")?.asString ?: "large"),
            )
            b.setTextColor(theme.color(context, "onPrimary"))
            b.setOnClickListener {
                
                val f = JsonObject()
                for (k in keys) inputs[k]?.let { v -> f.addProperty(k, v) }
                host.onAction(bindAction(submit, scope + ("form" to f)))
            }
            col.addView(Space(context).apply { layoutParams = ViewGroup.LayoutParams(1, d(12)) })
            col.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        return col
    }

    

    













    private fun chart(node: JsonObject, scope: Map<String, Any?>): View {
        val arr = Template.bindRaw(node.get("items")?.asString, scope) as? JsonArray
        val h = d(num(node, "height", scope, 140f))
        val kind = node.get("kind")?.asString ?: "bar"
        val yKey = node.get("y")?.asString ?: "y"
        val xKey = node.get("x")?.asString ?: "x"
        val yTicks = ((node.get("yTicks")?.takeIf { it.isJsonPrimitive }?.asInt) ?: 4).coerceIn(0, 8)
        val yMaxSpec = node.get("yMax")?.takeIf { it.isJsonPrimitive }?.asString ?: "auto"
        val yMinSpec = node.get("yMin")?.takeIf { it.isJsonPrimitive }?.asString ?: "0"
        val wantX = if (node.has("xLabels")) Template.truthy(Template.bindRaw(node.get("xLabels")?.asString, scope)) else true
        val smooth = if (node.has("smooth")) Template.truthy(Template.bindRaw(node.get("smooth")?.asString, scope)) else true
        val primary = theme.color(context, "primary")
        val tertiary = theme.color(context, "tertiary")
        val gridColor = theme.color(context, "outlineVariant")
        val textColor = theme.color(context, "onSurfaceVariant")
        val vals = ArrayList<Float>()
        val labels = ArrayList<String>()
        if (arr != null) {
            for (e in arr) {
                val o = e.takeIf { it.isJsonObject }?.asJsonObject
                vals.add(o?.get(yKey)?.takeIf { it.isJsonPrimitive }?.asFloat ?: 0f)
                labels.add(o?.get(xKey)?.takeIf { it.isJsonPrimitive }?.asString ?: "")
            }
        }
        val v = object : View(context) {
            

            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                setMeasuredDimension(
                    MeasureSpec.getSize(widthMeasureSpec),
                    if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
                        MeasureSpec.getSize(heightMeasureSpec)
                    } else h,
                )
            }

            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
                if (vals.isEmpty()) return
                val peak = vals.maxOrNull() ?: 0f
                val autoMax = niceCeil(peak)
                
                val maxV = (yMaxSpec.toFloatOrNull() ?: autoMax).coerceAtLeast(1e-6f)
                val minV = (yMinSpec.toFloatOrNull() ?: 0f).coerceAtMost(maxV)
                val range = (maxV - minV).coerceAtLeast(1e-6f)

                val tp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = d(10).toFloat(); color = textColor
                }
                val hasX = wantX && labels.any { it.isNotEmpty() }
                val padB = if (hasX) d(15).toFloat() else 0f
                val chartH = (height - padB).coerceAtLeast(d(10).toFloat())
                
                var padL = 0f
                if (yTicks > 0) {
                    for (i in 0..yTicks) padL = maxOf(padL, tp.measureText(fmtTick(minV + range * i / yTicks)))
                    padL += d(8).toFloat()
                }
                val gridP = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.STROKE; strokeWidth = d(1).toFloat(); color = gridColor
                }
                if (yTicks > 0) {
                    for (i in 0..yTicks) {
                        val frac = i.toFloat() / yTicks
                        val y = d(3) + (chartH - d(6)) * (1f - frac)
                        canvas.drawLine(padL, y, width.toFloat(), y, gridP)
                        val lb = fmtTick(minV + range * frac)
                        canvas.drawText(lb, padL - d(6).toFloat() - tp.measureText(lb), y + d(3).toFloat(), tp)
                    }
                }

                val availW = (width - padL).coerceAtLeast(1f)
                val availH = chartH - d(6)
                val n = vals.size
                val slot = availW / n
                fun px(i: Int) = padL + slot * i + slot / 2
                fun py(val0: Float) = d(3) + availH * (1f - ((val0 - minV) / range))
                if (kind == "line") {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = d(2).toFloat()
                    paint.color = primary
                    val path = android.graphics.Path()
                    if (smooth && n >= 3) {
                        

                        val topY = py(maxV)
                        val botY = py(minV)
                        path.moveTo(px(0), py(vals[0]))
                        for (i in 0 until n - 1) {
                            val i0 = maxOf(0, i - 1)
                            val i3 = minOf(n - 1, i + 2)
                            val c1x = px(i) + (px(i + 1) - px(i0)) / 6f
                            val c1y = (py(vals[i]) + (py(vals[i + 1]) - py(vals[i0])) / 6f).coerceIn(topY, botY)
                            val c2x = px(i + 1) - (px(i3) - px(i)) / 6f
                            val c2y = (py(vals[i + 1]) - (py(vals[i3]) - py(vals[i])) / 6f).coerceIn(topY, botY)
                            path.cubicTo(c1x, c1y, c2x, c2y, px(i + 1), py(vals[i + 1]))
                        }
                    } else {
                        for (i in 0 until n) {
                            if (i == 0) path.moveTo(px(i), py(vals[i])) else path.lineTo(px(i), py(vals[i]))
                        }
                    }
                    canvas.drawPath(path, paint)
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = tertiary
                    
                    if (n <= 40) for (i in 0 until n) canvas.drawCircle(px(i), py(vals[i]), d(2).toFloat(), paint)
                } else {
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = primary }
                    val bw = (slot * 0.6f).coerceAtLeast(d(2).toFloat())
                    for (i in 0 until n) {
                        val top = py(vals[i])
                        val left = px(i) - bw / 2
                        canvas.drawRoundRect(
                            left, top, left + bw, d(3) + availH,
                            d(2).toFloat(), d(2).toFloat(), paint,
                        )
                    }
                }

                
                if (hasX) {
                    val maxLabels = maxOf(2, (availW / d(36).toFloat()).toInt())
                    val stride = maxOf(1, Math.ceil(n.toDouble() / maxLabels).toInt())
                    val baseY = chartH + d(11).toFloat()
                    var lastRight = -1e9f
                    for (i in 0 until n) {
                        if (i % stride != 0 && i != n - 1) continue
                        val lb = labels.getOrNull(i) ?: continue
                        if (lb.isEmpty()) continue
                        val w2 = tp.measureText(lb)
                        val x = (px(i) - w2 / 2).coerceIn(0f, (width - w2).coerceAtLeast(0f))
                        if (x < lastRight + d(4).toFloat()) continue
                        canvas.drawText(lb, x, baseY, tp)
                        lastRight = x + w2
                    }
                }
            }
        }
        v.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
        return v
    }

    
    private fun niceCeil(v: Float): Float {
        if (!(v > 0f)) return 1f
        val exp = Math.floor(Math.log10(v.toDouble())).toInt()
        val base = Math.pow(10.0, exp.toDouble()).toFloat()
        val m = v / base
        val nm = when {
            m <= 1f -> 1f
            m <= 2f -> 2f
            m <= 5f -> 5f
            else -> 10f
        }
        return nm * base
    }

    
    private fun fmtTick(v: Float): String {
        val a = Math.abs(v)
        return when {
            a >= 10000f -> "%.0fk".format(v / 1000f)
            a >= 10f -> "%.0f".format(v)
            a >= 1f -> if (v == Math.round(v).toFloat()) "%.0f".format(v) else "%.1f".format(v)
            a == 0f -> "0"
            else -> "%.1f".format(v)
        }
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
