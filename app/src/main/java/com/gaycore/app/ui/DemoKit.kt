package com.gaycore.app.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.gaycore.app.R
import com.gaycore.app.theme.ThemeEngine
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlin.math.roundToInt














object DemoKit {

    

    enum class Accent(val label: String, val primary: String, val container: String) {
        VIOLET("紫藤", "#7C5CFC", "#E7DEFF"),
        BLUE("晴空", "#2878D8", "#D8E8FF"),
        GREEN("薄荷", "#137B65", "#CDEFE1"),
        ORANGE("琥珀", "#A85B00", "#FFE1B8"),
        RED("莓果", "#B32645", "#FFD9E1"),
    }

    

    fun dp(ctx: Context, v: Number): Int =
        (v.toFloat() * ctx.resources.displayMetrics.density + 0.5f).toInt()

    fun tc(ctx: Context, theme: ThemeEngine, name: String): Int = theme.color(ctx, name)

    fun isDark(ctx: Context, theme: ThemeEngine): Boolean = luminance(theme.color(ctx, "background")) < 0.5f

    fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha.coerceIn(0f, 1f) * 255).roundToInt(), Color.red(color), Color.green(color), Color.blue(color))

    private fun luminance(color: Int): Float {
        fun ch(v: Int): Float {
            val c = v / 255f
            return if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
        }

        return 0.2126f * ch(Color.red(color)) + 0.7152f * ch(Color.green(color)) + 0.0722f * ch(Color.blue(color))
    }

    

    fun themeSwitch(ctx: Context, theme: ThemeEngine, checked: Boolean): com.google.android.material.materialswitch.MaterialSwitch =
        com.google.android.material.materialswitch.MaterialSwitch(ctx).apply {
            isChecked = checked
            trackTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(theme.color(ctx, "primary"), theme.color(ctx, "surfaceContainerHighest")),
            )
            thumbTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(theme.color(ctx, "onPrimary"), theme.color(ctx, "outline")),
            )
        }

    

    fun seedPalette(
        act: android.app.Activity,
        theme: ThemeEngine,
        currentSeed: Int?,
        onPick: (Int?) -> Unit,
    ): View {
        val seeds = listOf(
            0xFF4C8BF5.toInt() to "蓝", 0xFF34A853.toInt() to "绿", 0xFFEA4335.toInt() to "红",
            0xFFF9A825.toInt() to "橙", 0xFF9C6ADE.toInt() to "紫", 0xFF26C6DA.toInt() to "青",
            0xFFEC407A.toInt() to "粉", 0xFF607D8B.toInt() to "灰蓝",
        )
        val cells = ArrayList<View>()
        cells.add(seedCell(act, theme, null, "跟随主题", currentSeed == null) { onPick(null) })
        for ((c, nm) in seeds) cells.add(seedCell(act, theme, c, nm, c == currentSeed) { onPick(c) })
        return chipWrap(cells, 10)
    }

    
    private fun seedCell(
        act: android.app.Activity,
        theme: ThemeEngine,
        color: Int?,
        name: String,
        selected: Boolean,
        onClick: () -> Unit,
    ): View {
        val size = dp(act, 52)
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        val dot = View(act).apply {
            background = if (color == null) {
                Md3.shape(
                    act, theme.color(act, "surfaceContainerHighest"), 15,
                    if (selected) 3f else 1.5f,
                    theme.color(act, if (selected) "primary" else "outline"),
                )
            } else {
                Md3.shape(
                    act, color, 15,
                    if (selected) 3f else 0f,
                    if (selected) theme.color(act, "onSurface") else android.graphics.Color.TRANSPARENT,
                )
            }
        }
        col.addView(dot, LinearLayout.LayoutParams(size, size))
        col.addView(
            txt(act, theme, name, 10.5f, selected, if (selected) "primary" else "onSurfaceVariant"),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(act, 4) },
        )
        return col
    }

    
    fun txt(
        ctx: Context, theme: ThemeEngine, t: String,
        sizeSp: Float = 15f, bold: Boolean = false, colorName: String = "onSurface",
    ): TextView = TextView(ctx).apply {
        text = t
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        if (bold) setTypeface(Typeface.DEFAULT_BOLD)
        setTextColor(theme.color(ctx, colorName))
    }

    fun toneColor(ctx: Context, theme: ThemeEngine, tone: String): Int = when (tone) {
        "success" -> theme.color(ctx, "success")
        "warning" -> theme.color(ctx, "tertiary")
        "error" -> theme.color(ctx, "error")
        "neutral" -> theme.color(ctx, "onSurfaceVariant")
        else -> theme.color(ctx, "primary")
    }

    

    fun rounded(ctx: Context, color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(ctx, radiusDp).toFloat()
    }

    fun rounded(ctx: Context, colorHex: String, radiusDp: Int): GradientDrawable =
        rounded(ctx, Color.parseColor(colorHex), radiusDp)

    fun roundedOutline(
        ctx: Context, fill: Int, stroke: Int, radiusDp: Int = 20, strokeWidthDp: Int = 1,
    ): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(ctx, radiusDp).toFloat()
        if (strokeWidthDp > 0) setStroke(dp(ctx, strokeWidthDp), stroke)
    }

    
    private fun rippleShape(ctx: Context, theme: ThemeEngine, shape: GradientDrawable, rippleHex: String? = null): RippleDrawable {
        val c = rippleHex?.let { Color.parseColor(it) } ?: theme.color(ctx, "onSurface")
        return RippleDrawable(ColorStateList.valueOf(withAlpha(c, 0.12f)), shape, null)
    }

    

    
    fun panel(ctx: Context, theme: ThemeEngine, radiusDp: Int = 20, ripple: Boolean = false): LinearLayout {
        val style = theme.cardStyle()
        val r = theme.shapeDp("card", radiusDp)
        val sw = theme.strokeWidthDp().toInt()
        val fill: Int
        val stroke: Int
        val strokeWidth: Int
        val shape: android.graphics.drawable.Drawable
        when (style) {
            "filled" -> {
                fill = theme.color(ctx, "surfaceContainer"); stroke = Color.TRANSPARENT; strokeWidth = 0
                shape = roundedOutline(ctx, fill, stroke, r, 0)
            }
            "elevated" -> {
                fill = theme.color(ctx, "surface"); stroke = Color.TRANSPARENT; strokeWidth = 0
                shape = roundedOutline(ctx, fill, stroke, r, 0)
            }
            "tonal" -> {
                fill = blend(theme.color(ctx, "surface"), theme.color(ctx, "primary"), 0.10f)
                stroke = Color.TRANSPARENT; strokeWidth = 0
                shape = roundedOutline(ctx, fill, stroke, r, 0)
            }
            "gradient" -> {
                fill = theme.color(ctx, "surface")
                val c2 = theme.color(ctx, "surfaceContainer")
                shape = android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(fill, c2)
                ).apply { cornerRadius = dp(ctx, r).toFloat() }
                stroke = Color.TRANSPARENT; strokeWidth = 0
            }
            else -> { 
                fill = theme.color(ctx, "surfaceContainerLowest"); stroke = theme.color(ctx, "outlineVariant"); strokeWidth = sw
                shape = roundedOutline(ctx, fill, stroke, r, strokeWidth)
            }
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = if (ripple) rippleShape(ctx, theme, shape as GradientDrawable, null) else shape
            setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 14))
            clipToPadding = false
            if (style == "elevated") {
                elevation = dp(ctx, 2).toFloat()
            }
            if (ripple) {
                isClickable = true
                isFocusable = true
            }
        }
    }

    
    fun panelBg(ctx: Context, theme: ThemeEngine, radiusDp: Int = 12): android.graphics.drawable.Drawable =
        rippleShape(ctx, theme, roundedOutline(ctx, theme.color(ctx, "surfaceContainerLowest"), theme.color(ctx, "outlineVariant"), radiusDp))

    
    fun selectedBg(ctx: Context, theme: ThemeEngine, radiusDp: Int = 12): android.graphics.drawable.Drawable =
        roundedOutline(ctx, theme.color(ctx, "primaryContainer"), theme.color(ctx, "primary"), radiusDp)

    
    fun gradientPanel(ctx: Context, theme: ThemeEngine, radiusDp: Int = 16): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(theme.color(ctx, "primaryContainer"), theme.color(ctx, "surfaceContainer")),
        ).apply { cornerRadius = dp(ctx, radiusDp).toFloat() }
        setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 14))
        clipToPadding = false
    }

    










    class FixedHeightView(ctx: Context, private val hPx: Int) : View(ctx) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec),
                if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
                    MeasureSpec.getSize(heightMeasureSpec)
                } else hPx,
            )
        }
    }

    fun divider(ctx: Context, theme: ThemeEngine): View =
        FixedHeightView(ctx, dp(ctx, 1)).apply {
            setBackgroundColor(theme.color(ctx, "outlineVariant"))
        }

    
    fun pageColumn(ctx: Context, theme: ThemeEngine, bottomSpace: Int = 96): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, bottomSpace))
        setBackgroundColor(theme.color(ctx, "background"))
    }

    
    
    fun box(ctx: Context): LinearLayout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

    fun put(
        parent: LinearLayout, child: View, gapDp: Int = 0,
        width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        weight: Float = 0f,
    ) {
        val lp = if (weight > 0f) LinearLayout.LayoutParams(0, height, weight) else LinearLayout.LayoutParams(width, height)
        


        if (parent.orientation == LinearLayout.HORIZONTAL && parent.childCount > 0 &&
            lp.width == ViewGroup.LayoutParams.MATCH_PARENT
        ) {
            parent.orientation = LinearLayout.VERTICAL
        }
        if (gapDp > 0 && parent.childCount > 0) lp.topMargin = dp(parent.context, gapDp)
        parent.addView(child, lp)
    }

    
    fun splitRow(ctx: Context, left: View, right: View): LinearLayout {
        val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(right, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(ctx, 8) })
        return row
    }

    
    fun hscrollRow(ctx: Context, children: List<View>, gapDp: Int = 8): View {
        val inner = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        children.forEachIndexed { i, v ->
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (i > 0) lp.marginStart = dp(ctx, gapDp)
            inner.addView(v, lp)
        }
        return android.widget.HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(inner)
        }
    }

    

    fun sectionTitle(ctx: Context, theme: ThemeEngine, title: String, subtitle: String = ""): View {
        val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        val texts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        put(texts, txt(ctx, theme, title, 18f, true))
        if (subtitle.isNotEmpty()) put(texts, txt(ctx, theme, subtitle, 11.5f, false, "onSurfaceVariant"), 2)
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }

    

    fun badge(ctx: Context, theme: ThemeEngine, text: String, tone: String = "neutral"): TextView {
        val bg: String
        val fg: String
        when (tone) {
            "success" -> { bg = "successContainer"; fg = "success" }
            "error" -> { bg = "errorContainer"; fg = "error" }
            "warning" -> { bg = "tertiaryContainer"; fg = "onTertiaryContainer" }
            "primary" -> { bg = "primaryContainer"; fg = "primary" }
            "tertiary" -> { bg = "tertiaryContainer"; fg = "tertiary" }
            else -> { bg = "surfaceContainerHighest"; fg = "onSurfaceVariant" }
        }
        return TextView(ctx).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(dp(ctx, 9), dp(ctx, 4), dp(ctx, 9), dp(ctx, 4))
            setTextColor(theme.color(ctx, fg))
            background = rounded(ctx, theme.color(ctx, bg), theme.shapeDp("chip", 100))
            maxLines = 1
        }
    }

    fun chip(
        ctx: Context, theme: ThemeEngine, label: String, iconRes: Int? = null,
        selected: Boolean = false, onClick: (() -> Unit)? = null,
    ): TextView {
        val chipR = theme.shapeDp("chip", 100)
        val shape = if (selected) roundedOutline(ctx, theme.color(ctx, "primaryContainer"), theme.color(ctx, "primary"), chipR)
        else roundedOutline(ctx, Color.TRANSPARENT, theme.color(ctx, "outline"), chipR)
        val tv = TextView(ctx).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), dp(ctx, 8))
            setTextColor(if (selected) theme.color(ctx, "primary") else theme.color(ctx, "onSurfaceVariant"))
            if (selected) setTypeface(Typeface.DEFAULT_BOLD)
            background = rippleShape(ctx, theme, shape)
            isSingleLine = true
            if (onClick != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        }
        if (iconRes != null) {
            val d = androidx.core.content.ContextCompat.getDrawable(ctx, iconRes)?.mutate()
            if (d != null) {
                val sz = dp(ctx, 16)
                d.setBounds(0, 0, sz, sz)
                d.setTint(tv.currentTextColor)
                tv.setCompoundDrawables(d, null, null, null)
                tv.compoundDrawablePadding = dp(ctx, 5)
            }
        }
        return tv
    }

    fun pill(ctx: Context, theme: ThemeEngine, text: String, colorHex: String, strong: Boolean): TextView {
        val color = Color.parseColor(colorHex)
        return TextView(ctx).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
            if (strong) setTypeface(Typeface.DEFAULT_BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(ctx, 10), dp(ctx, 4), dp(ctx, 10), dp(ctx, 4))
            setTextColor(if (strong) Color.WHITE else theme.color(ctx, "onSurfaceVariant"))
            background = rounded(ctx, if (strong) color else withAlpha(color, 0.18f), 100)
            maxLines = 1
        }
    }

    

    




    fun segmented(
        ctx: Context, theme: ThemeEngine, labels: List<String>, selected: Int, onSelect: (Int) -> Unit,
    ): SegmentedButtons = SegmentedButtons(ctx, theme, labels, selected, onSelect)

    class SegmentedButtons(
        private val ctx: Context,
        private val theme: ThemeEngine,
        private val labels: List<String>,
        selected: Int,
        private val onSelect: (Int) -> Unit,
    ) : FrameLayout(ctx) {

        private val pad = dp(ctx, 4)
        private val pillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.color(ctx, "surfaceContainerLowest")
        }
        private val pillRect = android.graphics.RectF()
        private val texts = ArrayList<TextView>()
        private var index = selected.coerceIn(0, maxOf(0, labels.size - 1))

        
        private var offset = index.toFloat()
        private var animator: android.animation.ValueAnimator? = null

        init {
            


            background = rounded(ctx, theme.color(ctx, "surfaceContainerHighest"), 100)
            setPadding(pad, pad, pad, pad)

            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            labels.forEachIndexed { i, label ->
                val tv = TextView(ctx).apply {
                    text = label
                    gravity = Gravity.CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    setPadding(dp(ctx, 8), dp(ctx, 9), dp(ctx, 8), dp(ctx, 9))
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { select(i) }
                }
                texts.add(tv)
                row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            applyTextStyle()
        }

        private fun segWidth(): Float =
            ((width - pad * 2).coerceAtLeast(0)).toFloat() / maxOf(1, labels.size)

        override fun dispatchDraw(canvas: android.graphics.Canvas) {
            val w = segWidth()
            if (w > 0f && height > pad * 2) {
                val left = pad + offset * w
                val r = (height - pad * 2) / 2f      
                pillRect.set(left, pad.toFloat(), left + w, (height - pad).toFloat())
                canvas.drawRoundRect(pillRect, r, r, pillPaint)
            }
            super.dispatchDraw(canvas)
        }

        
        fun select(i: Int) {
            index = i
            animateTo(i)
            applyTextStyle()
            onSelect(i)
        }

        
        fun setSelected(i: Int, animate: Boolean = true) {
            index = i.coerceIn(0, maxOf(0, labels.size - 1))
            if (animate) animateTo(index) else {
                animator?.cancel()
                offset = index.toFloat()
                invalidate()
            }
            applyTextStyle()
        }

        fun selectedIndex(): Int = index

        private fun animateTo(target: Int) {
            animator?.cancel()
            animator = android.animation.ValueAnimator.ofFloat(offset, target.toFloat()).apply {
                duration = 200
                interpolator = android.view.animation.DecelerateInterpolator()
                addUpdateListener {
                    offset = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        private fun applyTextStyle() {
            texts.forEachIndexed { i, tv ->
                val sel = i == index
                tv.setTextColor(theme.color(ctx, if (sel) "onSurface" else "onSurfaceVariant"))
                tv.typeface = if (sel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        }
    }

    

    fun button(ctx: Context, theme: ThemeEngine, text: String, style: String = "filled", onClick: (() -> Unit)? = null): MaterialButton =
        UiKit.button(ctx, theme, text, style).apply { if (onClick != null) setOnClickListener { onClick() } }

    fun iconButton(ctx: Context, theme: ThemeEngine, iconRes: Int, description: String, onClick: () -> Unit): ImageView =
        ImageView(ctx).apply {
            setImageResource(iconRes)
            setImageTintList(ColorStateList.valueOf(theme.color(ctx, "onSurfaceVariant")))
            contentDescription = description
            val pad = dp(ctx, 8)
            setPadding(pad, pad, pad, pad)
            background = RippleDrawable(
                ColorStateList.valueOf(withAlpha(theme.color(ctx, "onSurface"), 0.12f)), null, null,
            )
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

    

    
    fun iconBadge(ctx: Context, theme: ThemeEngine, iconRes: Int, colorHex: String? = null, sizeDp: Int = 36): View {
        val dark = isDark(ctx, theme)
        val base = colorHex?.let { Color.parseColor(it) } ?: theme.color(ctx, "primary")
        val fg = if (colorHex == null) theme.color(ctx, "primary") else (if (dark) lighten(base, 1.45f) else base)
        val bg = if (colorHex == null) theme.color(ctx, "primaryContainer") else withAlpha(base, if (dark) 0.32f else 0.16f)
        val frame = FrameLayout(ctx).apply { background = rounded(ctx, bg, 14) }
        val iv = ImageView(ctx).apply { setImageResource(iconRes); setImageTintList(ColorStateList.valueOf(fg)) }
        val iconSize = dp(ctx, sizeDp * 0.55f)
        frame.addView(iv, FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER))
        return frame
    }

    



    fun avatarAuto(ctx: Context, theme: ThemeEngine, url: String, text: String, sizeDp: Int): View {
        val size = dp(ctx, sizeDp)
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            return avatarSoft(ctx, theme, text, sizeDp)
        }
        val holder = FrameLayout(ctx)
        holder.layoutParams = LinearLayout.LayoutParams(size, size)
        val iv = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        holder.addView(iv, FrameLayout.LayoutParams(size, size))
        val fallback = avatarSoft(ctx, theme, text, sizeDp)
        com.gaycore.app.data.ImageLoader.load(
            url, iv,
            com.gaycore.app.data.ImageLoader.Shape.ROUNDED,
            theme.color(ctx, "surfaceContainerHigh"),
        ) {
            holder.removeAllViews()
            holder.addView(fallback, FrameLayout.LayoutParams(size, size))
        }
        return holder
    }

    fun avatar(ctx: Context, text: String, color: Int, sizeDp: Int): TextView =
        avatar(ctx, text, String.format("#%06X", 0xFFFFFF and color), sizeDp)

    fun avatar(ctx: Context, text: String, colorHex: String, sizeDp: Int): TextView =
        avatar(ctx, text, colorHex, sizeDp, Color.WHITE)

    
    fun avatarSoft(ctx: Context, theme: ThemeEngine, text: String, sizeDp: Int): TextView {
        val bg = theme.color(ctx, "primaryContainer")
        return avatar(ctx, text, String.format("#%06X", 0xFFFFFF and bg), sizeDp, theme.color(ctx, "onPrimaryContainer"))
    }

    fun avatar(ctx: Context, text: String, colorHex: String, sizeDp: Int, textColor: Int): TextView {
        val size = dp(ctx, sizeDp)
        return TextView(ctx).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (sizeDp > 50) 22f else if (sizeDp > 32) 16f else 13f)
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(textColor)
            
            background = rounded(ctx, colorHex, (sizeDp * 0.24f).toInt().coerceAtLeast(4))
            layoutParams = LinearLayout.LayoutParams(size, size)
            maxLines = 1
        }
    }

    

    fun metricTile(
        ctx: Context, theme: ThemeEngine, value: String, label: String, glyph: String,
        onClick: (() -> Unit)? = null,
    ): View {
        val shape = roundedOutline(ctx, theme.color(ctx, "surfaceContainerLowest"), theme.color(ctx, "outlineVariant"), 16)
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 14))
            gravity = Gravity.CENTER
            background = if (onClick != null) rippleShape(ctx, theme, shape) else shape
        }
        put(card, txt(ctx, theme, glyph, 16f, true, "primary"))
        put(card, txt(ctx, theme, value, 19f, true), 8)
        put(card, txt(ctx, theme, label, 11f, false, "onSurfaceVariant"), 3)
        if (onClick != null) {
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener { onClick() }
        }
        return card
    }

    fun metricRow(ctx: Context, vararg tiles: View): LinearLayout {
        val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER }
        tiles.forEachIndexed { i, t ->
            val lp = LinearLayout.LayoutParams(0, dp(ctx, 104), 1f)
            if (i > 0) lp.marginStart = dp(ctx, 8)
            row.addView(t, lp)
        }
        return row
    }

    
    fun actionTile(ctx: Context, theme: ThemeEngine, label: String, iconRes: Int, colorHex: String? = null, onClick: () -> Unit): View {
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 14))
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            background = rippleShape(
                ctx, theme,
                roundedOutline(ctx, theme.color(ctx, "surfaceContainerLowest"), theme.color(ctx, "outlineVariant"), 14),
            )
            setOnClickListener { onClick() }
        }
        put(card, iconBadge(ctx, theme, iconRes, colorHex, 30), 0, dp(ctx, 30), dp(ctx, 30))
        put(card, txt(ctx, theme, label, 11.5f, true), 6)
        return card
    }

    

    class SearchBar(val view: View, val input: EditText)

    fun searchBar(
        ctx: Context, theme: ThemeEngine, hint: String,
        onQuery: ((String) -> Unit)? = null,
        onFilter: (() -> Unit)? = null,
    ): SearchBar {
        val row = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(ctx, theme.color(ctx, "surfaceContainer"), 100)
            setPadding(dp(ctx, 14), 0, dp(ctx, 6), 0)
        }
        val ic = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_search)
            setImageTintList(ColorStateList.valueOf(theme.color(ctx, "onSurfaceVariant")))
        }
        row.addView(ic, LinearLayout.LayoutParams(dp(ctx, 18), dp(ctx, 18)))
        val input = EditText(ctx).apply {
            this.hint = hint
            setSingleLine(true)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(theme.color(ctx, "onSurface"))
            setHintTextColor(theme.color(ctx, "onSurfaceVariant"))
            background = null
        }
        row.addView(input, LinearLayout.LayoutParams(0, dp(ctx, 48), 1f).apply { marginStart = dp(ctx, 8) })
        if (onFilter != null) {
            row.addView(iconButton(ctx, theme, R.drawable.ic_filter, "筛选", onFilter), LinearLayout.LayoutParams(dp(ctx, 42), dp(ctx, 48)))
        }
        if (onQuery != null) {
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) { onQuery(s?.toString() ?: "") }
            })
        }
        return SearchBar(row, input)
    }

    

    
    fun menuCard(
        ctx: Context, theme: ThemeEngine, title: String, desc: String, iconRes: Int,
        colorHex: String? = null, danger: Boolean = false, onClick: () -> Unit,
    ): View {
        val card = panel(ctx, theme, 16).apply {
            isClickable = true
            isFocusable = true
            background = rippleShape(
                ctx, theme,
                roundedOutline(ctx, theme.color(ctx, "surfaceContainerLowest"), theme.color(ctx, "outlineVariant"), 16),
            )
            setOnClickListener { onClick() }
        }
        val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(
            if (danger) dangerBadge(ctx, theme, iconRes) else iconBadge(ctx, theme, iconRes, colorHex),
            LinearLayout.LayoutParams(dp(ctx, 42), dp(ctx, 42)),
        )
        val texts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        put(texts, txt(ctx, theme, title, 14f, true, if (danger) "error" else "onSurface"))
        if (desc.isNotEmpty()) put(texts, txt(ctx, theme, desc, 11.5f, false, "onSurfaceVariant"), 3)
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(ctx, 10) })
        val chev = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_chevron_right)
            setImageTintList(ColorStateList.valueOf(theme.color(ctx, if (danger) "error" else "outline")))
        }
        row.addView(chev, LinearLayout.LayoutParams(dp(ctx, 20), dp(ctx, 20)))
        put(card, row)
        return card
    }

    private fun dangerBadge(ctx: Context, theme: ThemeEngine, iconRes: Int): View {
        val box = FrameLayout(ctx).apply { background = rounded(ctx, theme.color(ctx, "errorContainer"), 14) }
        val iv = ImageView(ctx).apply {
            setImageResource(iconRes)
            setImageTintList(ColorStateList.valueOf(theme.color(ctx, "error")))
        }
        box.addView(iv, FrameLayout.LayoutParams(dp(ctx, 19), dp(ctx, 19), Gravity.CENTER))
        return box
    }

    
    fun dangerCard(ctx: Context, theme: ThemeEngine, title: String, desc: String, iconRes: Int, onClick: () -> Unit): View =
        menuCard(ctx, theme, title, desc, iconRes, danger = true, onClick = onClick)

    
    fun settingsGroup(ctx: Context, theme: ThemeEngine, rows: List<View>): View {
        val card = panel(ctx, theme, 20)
        card.setPadding(0, dp(ctx, 4), 0, dp(ctx, 4))
        rows.forEachIndexed { i, row ->
            if (i > 0) {
                val holder = LinearLayout(ctx).apply { setPadding(dp(ctx, 18), 0, dp(ctx, 18), 0) }
                holder.addView(divider(ctx, theme), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 1)))
                card.addView(holder, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 1)))
            }
            card.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        return card
    }

    
    fun settingsRow(
        ctx: Context, theme: ThemeEngine, iconRes: Int, title: String, desc: String,
        colorHex: String? = null, danger: Boolean = false, onClick: () -> Unit,
    ): View {
        val row = LinearLayout(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 14))
            isClickable = true
            isFocusable = true
            background = RippleDrawable(
                ColorStateList.valueOf(withAlpha(theme.color(ctx, "onSurface"), 0.10f)), null, null,
            )
            setOnClickListener { onClick() }
        }
        row.addView(
            if (danger) dangerBadge(ctx, theme, iconRes) else iconBadge(ctx, theme, iconRes, colorHex),
            LinearLayout.LayoutParams(dp(ctx, 40), dp(ctx, 40)),
        )
        val texts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        put(texts, txt(ctx, theme, title, 14f, true, if (danger) "error" else "onSurface"))
        if (desc.isNotEmpty()) put(texts, txt(ctx, theme, desc, 11.5f, false, "onSurfaceVariant"), 3)
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(ctx, 12) })
        val chev = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_chevron_right)
            setImageTintList(ColorStateList.valueOf(theme.color(ctx, if (danger) "error" else "outline")))
        }
        row.addView(chev, LinearLayout.LayoutParams(dp(ctx, 20), dp(ctx, 20)))
        return row
    }

    
    fun valueRow(ctx: Context, theme: ThemeEngine, label: String, value: String): View {
        val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(txt(ctx, theme, label, 12.5f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(dp(ctx, 64), ViewGroup.LayoutParams.WRAP_CONTENT))
        row.addView(txt(ctx, theme, value, 13.5f, false, "onSurface"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }

    

    fun progressRow(ctx: Context, theme: ThemeEngine, label: String, value: Long, max: Long, detail: String): View {
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(txt(ctx, theme, label, 12f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(txt(ctx, theme, detail, 12f, true))
        put(col, row)
        val m = if (max <= 0) 100L else max
        val bar = LinearProgressIndicator(ctx).apply {
            this.max = m.toInt()
            progress = value.coerceIn(0, m).toInt()
            trackColor = theme.color(ctx, "surfaceContainerHighest")
            setIndicatorColor(theme.color(ctx, "primary"))
            trackCornerRadius = dp(ctx, 4)
        }
        put(col, bar, 8)
        return col
    }

    
    fun rankRow(
        ctx: Context, theme: ThemeEngine, name: String, count: String, ratio: String,
        colorHex: String, value: Float,
    ): View {
        val card = panel(ctx, theme, 16)
        val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(avatar(ctx, name.take(1).uppercase(), colorHex, 34), LinearLayout.LayoutParams(dp(ctx, 34), dp(ctx, 34)))
        row.addView(txt(ctx, theme, name, 14f, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(ctx, 10) })
        row.addView(txt(ctx, theme, count, 13f, true))
        row.addView(txt(ctx, theme, "  " + ratio, 11f, false, "onSurfaceVariant"))
        put(card, row)
        val bar = LinearProgressIndicator(ctx).apply {
            max = 100
            progress = (value * 100).roundToInt().coerceIn(0, 100)
            trackColor = theme.color(ctx, "surfaceContainerHighest")
            setIndicatorColor(Color.parseColor(colorHex))
            trackCornerRadius = dp(ctx, 4)
        }
        put(card, bar, 10)
        return card
    }

    
    fun barChart(ctx: Context, theme: ThemeEngine, data: List<Int>, colorHex: String, labels: List<String> = emptyList()): View {
        val row = LinearLayout(ctx).apply { gravity = Gravity.BOTTOM }
        val maxV = (data.maxOrNull() ?: 1).coerceAtLeast(1)
        data.forEachIndexed { index, value ->
            val item = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            val h = ((value.toFloat() / maxV) * 110f).roundToInt().coerceAtLeast(4)
            val bar = View(ctx).apply { background = rounded(ctx, colorHex, 8) }
            item.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, h)).apply {
                marginStart = dp(ctx, 4); marginEnd = dp(ctx, 4)
            })
            val lb = labels.getOrNull(index) ?: "" + (index + 1)
            item.addView(txt(ctx, theme, lb, 10f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 20)))
            row.addView(item, LinearLayout.LayoutParams(0, dp(ctx, 150), 1f))
        }
        return row
    }

    

    fun chatBubble(ctx: Context, theme: ThemeEngine, text: String, mine: Boolean, maxWidthPx: Int): View {
        val tv = txt(ctx, theme, text, 13.5f, false, if (mine) "onPrimaryContainer" else "onSurface").apply {
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            background = rounded(ctx, theme.color(ctx, if (mine) "primaryContainer" else "surfaceContainerHighest"), 16)
        }
        val wrap = LinearLayout(ctx).apply { gravity = if (mine) Gravity.END else Gravity.START }
        wrap.addView(tv, LinearLayout.LayoutParams(maxWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT))
        return wrap
    }

    
    fun chipWrap(chips: List<View>, gapDp: Int = 8): View {
        if (chips.isEmpty()) return View(chips.firstOrNull()?.context ?: return View(null))
        val ctx = chips.first().context
        val flow = FlowRow(ctx, gapDp)
        chips.forEach { flow.addView(it) }
        return flow
    }

    
    class FlowRow(ctx: Context, private val gapDp: Int) : ViewGroup(ctx) {
        private val gap = dp(ctx, gapDp)
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val avail = MeasureSpec.getSize(widthMeasureSpec)
            var x = 0
            var y = 0
            var lineH = 0
            for (i in 0 until childCount) {
                val c = getChildAt(i)
                measureChild(c, MeasureSpec.makeMeasureSpec(avail, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
                val w = c.measuredWidth
                val h = c.measuredHeight
                if (x > 0 && x + gap + w > avail) {
                    x = 0
                    y += lineH + gap
                    lineH = 0
                }
                x += (if (x > 0) gap else 0) + w
                if (h > lineH) lineH = h
            }
            setMeasuredDimension(avail, y + lineH)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            val avail = width
            var x = 0
            var y = 0
            var lineH = 0
            for (i in 0 until childCount) {
                val c = getChildAt(i)
                val w = c.measuredWidth
                val h = c.measuredHeight
                if (x > 0 && x + gap + w > avail) {
                    x = 0
                    y += lineH + gap
                    lineH = 0
                }
                if (x > 0) x += gap
                c.layout(x, y, x + w, y + h)
                x += w
                if (h > lineH) lineH = h
            }
        }
    }

    

    
    fun chatInput(ctx: Context, theme: ThemeEngine, hint: String, minLines: Int = 1): EditText =
        EditText(ctx).apply {
            this.hint = hint
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(theme.color(ctx, "onSurface"))
            setHintTextColor(theme.color(ctx, "onSurfaceVariant"))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            this.minLines = minLines
            maxLines = 4
            background = roundedOutline(ctx, theme.color(ctx, "surfaceContainerHighest"), theme.color(ctx, "outlineVariant"), 22)
            setPadding(dp(ctx, 14), dp(ctx, 10), dp(ctx, 14), dp(ctx, 10))
        }

    
    fun iconRes(name: String?): Int = when (name?.removePrefix("msym:")) {
        "home" -> R.drawable.ic_home
        "person" -> R.drawable.ic_person
        "settings" -> R.drawable.ic_settings
        "check" -> R.drawable.ic_check
        "add" -> R.drawable.ic_add
        "close" -> R.drawable.ic_close
        "refresh" -> R.drawable.ic_refresh
        "key" -> R.drawable.ic_key
        "info" -> R.drawable.ic_info
        "palette" -> R.drawable.ic_palette
        "edit" -> R.drawable.ic_edit
        "star" -> R.drawable.ic_star
        "group" -> R.drawable.ic_group
        "copy" -> R.drawable.ic_copy
        "wallet" -> R.drawable.ic_wallet
        "bug" -> R.drawable.ic_bug
        "search" -> R.drawable.ic_search
        "chevron" -> R.drawable.ic_chevron_right
        "delete" -> R.drawable.ic_delete
        "block" -> R.drawable.ic_block
        "sort" -> R.drawable.ic_sort
        "filter" -> R.drawable.ic_filter
        "grid" -> R.drawable.ic_grid
        "chat" -> R.drawable.ic_chat
        "schedule" -> R.drawable.ic_schedule
        "more" -> R.drawable.ic_more_vert
        "menu" -> R.drawable.ic_menu
        "send" -> R.drawable.ic_send
        "bolt" -> R.drawable.ic_bolt
        "tune" -> R.drawable.ic_tune
        "arrow_back" -> R.drawable.ic_arrow_back
        else -> R.drawable.ic_apps
    }

    
    fun copy(ctx: Context, label: String, text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
    }

    

    
    fun animateIn(v: View, fromDp: Int = 10) {
        v.alpha = 0f
        v.translationY = dp(v.context, fromDp).toFloat()
        v.animate().alpha(1f).translationY(0f)
            .setDuration(220)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    
    fun animateInStaggered(views: List<View>, stepMs: Long = 24) {
        views.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = dp(v.context, 14).toFloat()
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(i * stepMs).setDuration(240)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    

    fun lighten(color: Int, factor: Float): Int = adjust(color, factor)

    fun darken(color: Int, factor: Float): Int = adjust(color, factor)

    private fun adjust(color: Int, factor: Float): Int {
        fun channel(v: Int) = (v * factor).roundToInt().coerceIn(0, 255)
        return Color.rgb(channel(Color.red(color)), channel(Color.green(color)), channel(Color.blue(color)))
    }

    
    fun blend(a: Int, b: Int, ratio: Float): Int {
        val r = ratio.coerceIn(0f, 1f)
        fun ch(x: Int, y: Int) = (x + (y - x) * r).roundToInt().coerceIn(0, 255)
        return Color.rgb(ch(Color.red(a), Color.red(b)), ch(Color.green(a), Color.green(b)), ch(Color.blue(a), Color.blue(b)))
    }

    

    fun switchRow(
        ctx: Context, theme: ThemeEngine, title: String, desc: String, checked: Boolean,
        onChecked: (Boolean) -> Unit,
    ): View {
        val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        val texts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        put(texts, txt(ctx, theme, title, 14f, true))
        if (desc.isNotEmpty()) put(texts, txt(ctx, theme, desc, 11.5f, false, "onSurfaceVariant"), 3)
        row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val sw = themeSwitch(ctx, theme, checked).apply {
            setOnCheckedChangeListener { _, c -> onChecked(c) }
        }
        row.addView(sw, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return row
    }

    

    
    fun miniChart(ctx: Context, theme: ThemeEngine, data: List<Pair<Int, Int>>): View {
        val h = dp(ctx, 100)
        return object : View(ctx) {
            

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
                if (data.isEmpty()) return
                val w = width.toFloat()
                val h2 = height.toFloat()
                val max = maxOf(1, data.maxOf { it.first })
                val bw = w / data.size
                val barH = h2 - 22f
                val pPri = android.graphics.Paint().apply { color = theme.color(ctx, "primary"); isAntiAlias = true }
                val pErr = android.graphics.Paint().apply { color = theme.color(ctx, "error"); isAntiAlias = true }
                val pOut = android.graphics.Paint().apply { color = theme.color(ctx, "outlineVariant"); isAntiAlias = true }
                val pTxt = android.graphics.Paint().apply { color = theme.color(ctx, "onSurfaceVariant"); textSize = dp(ctx, 9).toFloat(); isAntiAlias = true }
                data.forEachIndexed { i, (count, errs) ->
                    val bh = (count.toFloat() / max) * barH
                    val x = i * bw
                    if (bh > 1f) {
                        canvas.drawRect(x + 1.5f, barH - bh + 8f, x + bw - 1.5f, barH + 8f, if (errs > 0) pErr else pPri)
                    } else {
                        canvas.drawRect(x + 1.5f, barH + 7f, x + bw - 1.5f, barH + 8f, pOut)
                    }
                }
                canvas.drawText("-24h", 2f, h2 - 4f, pTxt)
                canvas.drawText("now", w - 28f, h2 - 4f, pTxt)
                val label = "\u5cf0\u503c " + max
                canvas.drawText(label, w / 2f - dp(ctx, 18), 12f, pTxt)
            }
        }.apply { layoutParams = LinearLayout.LayoutParams(-1, h) }
    }

    
    fun hbarChart(ctx: Context, theme: ThemeEngine, items: List<Pair<String, Int>>): View {
        val col = box(ctx)
        if (items.isEmpty()) { put(col, txt(ctx, theme, "\u6682\u65e0\u6570\u636e", 13f, false, "onSurfaceVariant")); return col }
        val max = maxOf(1, items.maxOf { it.second })
        for ((name, count) in items.take(8)) {
            val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(ctx, 4), 0, dp(ctx, 4)) }
            row.addView(TextView(ctx).apply {
                text = name; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(theme.color(ctx, "onSurfaceVariant")); textSize = 12f
            }, LinearLayout.LayoutParams(dp(ctx, 70), -2))
            val track = FrameLayout(ctx).apply {
                setBackgroundColor(theme.color(ctx, "surfaceContainer"))
                addView(View(ctx).apply {
                    setBackgroundColor(theme.color(ctx, "primary"))
                    layoutParams = FrameLayout.LayoutParams(
                        maxOf(2, (count.toFloat() / max * 1000).toInt()),
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                })
            }
            row.addView(track, LinearLayout.LayoutParams(0, dp(ctx, 20), 1f).apply { marginStart = dp(ctx, 6) })
            row.addView(TextView(ctx).apply {
                text = count.toString(); setTextColor(theme.color(ctx, "onSurfaceVariant")); textSize = 11f
                gravity = Gravity.END
            }, LinearLayout.LayoutParams(dp(ctx, 40), -2))
            put(col, row, 2)
        }
        return col
    }

    
    fun activityFeed(ctx: Context, theme: ThemeEngine, items: List<com.google.gson.JsonObject>): View {
        val col = box(ctx)
        if (items.isEmpty()) { put(col, txt(ctx, theme, "\u6682\u65e0\u8bf7\u6c42\u8bb0\u5f55", 13f, false, "onSurfaceVariant")); return col }
        for (r in items.take(20)) {
            val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(ctx, 5), 0, dp(ctx, 5)) }
            val status = r.get("status")?.asInt ?: 200
            val dot = View(ctx).apply {
                setBackgroundColor(if (status < 400) 0xFF4CAF50.toInt() else theme.color(ctx, "error"))
                layoutParams = LinearLayout.LayoutParams(dp(ctx, 7), dp(ctx, 7))
            }
            row.addView(dot)
            row.addView(TextView(ctx).apply {
                val tRaw = r.get("time")?.asString ?: ""
                text = if (tRaw.length >= 19) tRaw.substring(11) else tRaw
                setTextColor(theme.color(ctx, "onSurfaceVariant")); textSize = 11f
            }, LinearLayout.LayoutParams(dp(ctx, 48), -2).apply { marginStart = dp(ctx, 6) })
            row.addView(TextView(ctx).apply {
                text = r.get("model")?.asString ?: ""
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(theme.color(ctx, "onSurface")); textSize = 13f
            }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(ctx).apply {
                text = r.get("channel")?.asString ?: ""
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(theme.color(ctx, "onSurfaceVariant")); textSize = 11f
            }, LinearLayout.LayoutParams(dp(ctx, 60), -2))
            row.addView(TextView(ctx).apply {
                text = (r.get("duration")?.asLong ?: 0L).toString() + "ms"
                setTextColor(theme.color(ctx, "onSurfaceVariant")); textSize = 11f
            }, LinearLayout.LayoutParams(dp(ctx, 44), -2))
            put(col, row, 0)
        }
        return col
    }
}
