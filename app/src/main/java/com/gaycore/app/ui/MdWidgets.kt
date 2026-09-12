package com.gaycore.app.ui

import android.content.Context
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.button.MaterialButton
import android.text.TextUtils
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.google.android.material.chip.Chip
import com.gaycore.app.R
import com.gaycore.app.theme.ThemeEngine

   
                                         
  
                                                   
                     
  
                                                  
              
   
object MdWidgets {

                       
    internal fun tcPub(ctx: Context, theme: ThemeEngine, name: String): Int = tc(ctx, theme, name)
    internal fun dpPub(ctx: Context, v: Float): Int = dp(ctx, v)


    fun dp(ctx: Context, v: Float): Int = (v * ctx.resources.displayMetrics.density + 0.5f).toInt()
    fun dp(ctx: Context, v: Int): Int = dp(ctx, v.toFloat())

    private fun tc(ctx: Context, theme: ThemeEngine, name: String): Int = theme.color(ctx, name)

                                              

                     
                                                                                                       
    fun panel(
        ctx: Context,
        theme: ThemeEngine,
        radiusDp: Int = Md3.CARD_RADIUS,
        fill: String = "surface",
        outlined: Boolean = true,
    ): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            val p = Md3.dp(ctx, Md3.CARD_PAD)
            setPadding(p, p, p, p)
            background = Md3.shape(
                ctx,
                tc(ctx, theme, if (outlined) fill else "surfaceContainerHighest"),
                radiusDp,
                if (outlined) 1f else 0f,
                tc(ctx, theme, "outlineVariant"),
            )
        }

                                              

    fun text(ctx: Context, theme: ThemeEngine, t: String, sizeSp: Float = 15f, bold: Boolean = false, colorName: String = "onSurface"): TextView =
        UiKit.text(ctx, t, sizeSp, bold, tc(ctx, theme, colorName))

                                                

    fun sectionTitle(ctx: Context, theme: ThemeEngine, title: String, subtitle: String = ""): View {
        val row = LinearLayout(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        val texts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(text(ctx, theme, title, Md3.titleMedium.size, true))
        if (subtitle.isNotEmpty()) {
            texts.addView(text(ctx, theme, subtitle, Md3.labelMedium.size, false, "onSurfaceVariant"), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 2) })
        }
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        return row
    }

                                                

    fun badge(ctx: Context, theme: ThemeEngine, label: String, tone: String = "neutral"): TextView {
        val (bg, fg) = when (tone) {
            "success" -> "successContainer" to "success"
            "error" -> "errorContainer" to "error"
            "warning" -> "tertiaryContainer" to "onTertiaryContainer"
            "primary" -> "primaryContainer" to "primary"
            "tertiary" -> "tertiaryContainer" to "tertiary"
            else -> "surfaceContainerHighest" to "onSurfaceVariant"
        }
        return TextView(ctx).apply {
            text = label
            setPadding(dp(ctx, 8f), dp(ctx, 3f), dp(ctx, 8f), dp(ctx, 3f))
            setTextColor(tc(ctx, theme, fg))
            Md3.apply(this, Md3.labelSmall, tc(ctx, theme, fg))
            background = Md3.shape(ctx, tc(ctx, theme, bg), (100).toInt())
        }
    }

                                                

                                                        
    fun chip(
        ctx: Context,
        theme: ThemeEngine,
        label: String,
        iconRes: Int? = null,
        selected: Boolean = false,
        onClick: (() -> Unit)? = null,
    ): TextView {
        val selBg = tc(ctx, theme, "secondaryContainer")
        val selFg = tc(ctx, theme, "onSecondaryContainer")
        val unFg = tc(ctx, theme, "onSurfaceVariant")
        val ch = Chip(ctx).apply {
            text = label
            isCheckable = true
            isChecked = selected
            isClickable = onClick != null
            chipStrokeWidth = Md3.dp(ctx, if (selected) 0 else 1).toFloat()
            chipBackgroundColor = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(selBg, android.graphics.Color.TRANSPARENT),
            )
            chipStrokeColor = ColorStateList.valueOf(tc(ctx, theme, "outline"))
            setTextColor(ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(selFg, unFg),
            ))
            Md3.apply(this, Md3.labelLarge, if (selected) selFg else unFg)
            setTextColor(ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(selFg, unFg),
            ))
              
                                                                     
               
            minimumHeight = Md3.dp(ctx, Md3.CHIP_HEIGHT)
                                                       
            shapeAppearanceModel = com.google.android.material.shape.ShapeAppearanceModel.builder()
                .setAllCornerSizes(com.google.android.material.shape.ShapeAppearanceModel.PILL)
                .build()
            if (onClick != null) setOnClickListener { onClick() }
        }
        if (iconRes != null) {
            ch.setChipIconResource(iconRes)
            ch.chipIconTint = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(selFg, unFg),
            )
            ch.chipIconSize = Md3.dp(ctx, 18).toFloat()
            ch.isChipIconVisible = true
        }
        return ch
    }

                                                

                                                     
    fun iconButton(
        ctx: Context,
        theme: ThemeEngine,
        iconRes: Int,
        description: String,
        colorName: String = "onSurfaceVariant",
        onClick: () -> Unit,
    ): MaterialButton = MaterialButton(ctx, null, android.R.attr.borderlessButtonStyle).apply {
                                                           
        text = ""
        setIconResource(iconRes)
        iconPadding = 0
        iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        iconTint = ColorStateList.valueOf(tc(ctx, theme, colorName))
        contentDescription = description
        insetTop = 0
        insetBottom = 0
        minWidth = 0
        minimumWidth = 0
        minimumHeight = Md3.dp(ctx, Md3.ICON_BTN_SIZE)
        isAllCaps = false
        setOnClickListener { onClick() }
    }

                                                   
    fun circleIconButton(
        ctx: Context,
        theme: ThemeEngine,
        iconRes: Int,
        description: String,
        sizeDp: Int = 36,
        fill: String = "surfaceContainerHighest",
        colorName: String = "onSurfaceVariant",
        onClick: () -> Unit,
    ): MaterialButton =
        MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = ""
            setIconResource(iconRes)
            iconPadding = 0
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            iconTint = ColorStateList.valueOf(tc(ctx, theme, colorName))
            backgroundTintList = ColorStateList.valueOf(tc(ctx, theme, fill))
            strokeWidth = 0
            insetTop = 0
            insetBottom = 0
            minWidth = 0
            minimumWidth = 0
            minimumHeight = Md3.dp(ctx, sizeDp)
            shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setAllCornerSizes(ShapeAppearanceModel.PILL).build()
            contentDescription = description
            setOnClickListener { onClick() }
        }

                                                

                                                   
    fun segmented(
        ctx: Context,
        theme: ThemeEngine,
        labels: List<String>,
        selected: Int,
        onSelect: (Int) -> Unit,
    ): View {
        val selBg = tc(ctx, theme, "secondaryContainer")
        val selFg = tc(ctx, theme, "onSecondaryContainer")
        val unFg = tc(ctx, theme, "onSurfaceVariant")
        val pill = ShapeAppearanceModel.builder().setAllCornerSizes(ShapeAppearanceModel.PILL).build()
        val group = MaterialButtonToggleGroup(ctx).apply {
            isSingleSelection = true
            isSelectionRequired = true
            background = Md3.shape(ctx, tc(ctx, theme, "surfaceContainerHighest"), 100)
            setPadding(dp(ctx, 4f), dp(ctx, 4f), dp(ctx, 4f), dp(ctx, 4f))
        }
        labels.forEachIndexed { index, label ->
            val b = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                id = index
                text = label
                isCheckable = true
                isAllCaps = false
                textSize = 13.5f
                shapeAppearanceModel = pill
                strokeWidth = 0
                insetTop = 0
                insetBottom = 0
                minWidth = 0
                minimumWidth = 0
                backgroundTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(selBg, Color.TRANSPARENT),
                )
                setTextColor(
                    ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(selFg, unFg),
                    ),
                )
            }
            group.addView(b, LinearLayout.LayoutParams(0, -2, 1f))
        }
                                                                    
        group.check(selected)
        group.addOnButtonCheckedListener { _, id, isChecked ->
            if (isChecked && id != selected) onSelect(id)
        }
        return group
    }

                                               

                    
    fun metricCard(ctx: Context, theme: ThemeEngine, value: String, label: String, iconRes: Int, iconColorHex: String): View {
        val card = panel(ctx, theme, 16)
        card.addView(iconBadge(ctx, theme, iconRes, iconColorHex, 28), LinearLayout.LayoutParams(dp(ctx, 28), dp(ctx, 28)))
        card.addView(text(ctx, theme, value, 20f, true), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 7) })
        card.addView(text(ctx, theme, label, 11f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 2) })
        return card
    }

                                  
    fun metricTile(ctx: Context, theme: ThemeEngine, value: String, label: String, glyph: String): View {
        val card = panel(ctx, theme, 16)
        card.gravity = Gravity.CENTER
        card.addView(text(ctx, theme, glyph, 16f, true, "primary"), LinearLayout.LayoutParams(-2, -2))
        card.addView(text(ctx, theme, value, 19f, true), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(ctx, 8) })
        card.addView(text(ctx, theme, label, 11f, false, "onSurfaceVariant"), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(ctx, 3) })
        return card
    }

                                                  

    fun iconBadge(ctx: Context, theme: ThemeEngine, iconRes: Int, colorHex: String, sizeDp: Int = 36, dark: Boolean = true): View {
        val base = Color.parseColor(colorHex)
        val bgColor = if (dark) darken(base) else lighten(base)
        val box = FrameLayout(ctx).apply {
            background = Md3.shape(ctx, bgColor, (14f).toInt())
        }
        val iv = ImageView(ctx).apply {
            setImageResource(iconRes)
            setImageTintList(ColorStateList.valueOf(if (dark) Color.WHITE else base))
        }
        val sz = dp(ctx, sizeDp * 0.55f)
        box.addView(iv, FrameLayout.LayoutParams(sz, sz, Gravity.CENTER))
        return box
    }

    private fun darken(c: Int): Int = Color.rgb(
        (Color.red(c) * 0.42f).toInt().coerceIn(0, 255),
        (Color.green(c) * 0.42f).toInt().coerceIn(0, 255),
        (Color.blue(c) * 0.42f).toInt().coerceIn(0, 255),
    )

    private fun lighten(c: Int): Int = Color.rgb(
        (Color.red(c) * 1.22f).toInt().coerceIn(0, 255),
        (Color.green(c) * 1.22f).toInt().coerceIn(0, 255),
        (Color.blue(c) * 1.22f).toInt().coerceIn(0, 255),
    )

                                              

                                            
    fun input(
        ctx: Context,
        theme: ThemeEngine,
        label: String,
        value: String = "",
        numeric: Boolean = false,
        pill: Boolean = false,
    ): MdField = MdField(ctx, theme, label, value, numeric, pill)

                                 
    fun chipRow(ctx: Context, chips: List<View>): View {
        val hsv = android.widget.HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
        }
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        chips.forEachIndexed { i, v ->
            row.addView(v, LinearLayout.LayoutParams(-2, -2).apply { if (i > 0) marginStart = dp(ctx, 8f) })
        }
        hsv.addView(row, android.view.ViewGroup.LayoutParams(-2, -2))
        return hsv
    }

                                               

                                
    fun switchRow(
        ctx: Context,
        theme: ThemeEngine,
        title: String,
        desc: String,
        checked: Boolean,
        onChange: (Boolean) -> Unit,
    ): View {
        val r = row(ctx)
        val texts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(text(ctx, theme, title, 14f, true))
        if (desc.isNotEmpty()) {
            texts.addView(
                text(ctx, theme, desc, 11.5f, false, "onSurfaceVariant"),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 3f) },
            )
        }
        r.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        val sw = com.google.android.material.materialswitch.MaterialSwitch(ctx).apply { isChecked = checked }
        sw.setOnCheckedChangeListener { _, v -> onChange(v) }
        r.addView(sw, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(ctx, 8f) })
        return r
    }

                                         
    fun listRow(
        ctx: Context,
        theme: ThemeEngine,
        iconRes: Int,
        title: String,
        desc: String,
        colorHex: String = "#7C5CFC",
        onClick: () -> Unit,
    ): View {
        val card = panel(ctx, theme, 16)
        val r = row(ctx)
        r.addView(iconBadge(ctx, theme, iconRes, colorHex, 38), LinearLayout.LayoutParams(dp(ctx, 38f), dp(ctx, 38f)))
        val texts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(text(ctx, theme, title, 14f, true))
        if (desc.isNotEmpty()) {
            texts.addView(
                text(ctx, theme, desc, 11.5f, false, "onSurfaceVariant"),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 3f) },
            )
        }
        r.addView(texts, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(ctx, 12f) })
        val chev = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_chevron_right)
            setImageTintList(ColorStateList.valueOf(tc(ctx, theme, "outline")))
        }
        r.addView(chev, LinearLayout.LayoutParams(dp(ctx, 20f), dp(ctx, 20f)))
        card.addView(r)
        card.isClickable = true
        card.isFocusable = true
        card.setOnClickListener { onClick() }
        return card
    }

                     
    fun dangerRow(
        ctx: Context,
        theme: ThemeEngine,
        iconRes: Int,
        title: String,
        desc: String,
        onClick: () -> Unit,
    ): View {
        val card = panel(ctx, theme, 16)
        val r = row(ctx)
        val box = FrameLayout(ctx).apply {
            background = Md3.shape(ctx, tc(ctx, theme, "errorContainer"), (14f).toInt())
        }
        val iv = ImageView(ctx).apply {
            setImageResource(iconRes)
            setImageTintList(ColorStateList.valueOf(tc(ctx, theme, "error")))
        }
        val sz = dp(ctx, 20f)
        box.addView(iv, FrameLayout.LayoutParams(sz, sz, Gravity.CENTER))
        r.addView(box, LinearLayout.LayoutParams(dp(ctx, 38f), dp(ctx, 38f)))
        val texts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(text(ctx, theme, title, 14f, true, "error"))
        if (desc.isNotEmpty()) {
            texts.addView(
                text(ctx, theme, desc, 11.5f, false, "onSurfaceVariant"),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 3f) },
            )
        }
        r.addView(texts, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(ctx, 12f) })
        val chev = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_chevron_right)
            setImageTintList(ColorStateList.valueOf(tc(ctx, theme, "error")))
        }
        r.addView(chev, LinearLayout.LayoutParams(dp(ctx, 20f), dp(ctx, 20f)))
        card.addView(r)
        card.isClickable = true
        card.isFocusable = true
        card.setOnClickListener { onClick() }
        return card
    }

                                               

                                   

                                                                 
                                                  
                                            
    fun settingsCard(
        ctx: Context,
        theme: ThemeEngine,
        iconRes: Int,
        title: String,
        desc: String,
        tone: String = "primary",
        onClick: () -> Unit,
    ): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val px = Md3.dp(ctx, 18)
            val py = Md3.dp(ctx, 15)
            setPadding(px, py, px, py)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        row.addView(
            ImageView(ctx).apply {
                setImageResource(iconRes)
                setColorFilter(tc(ctx, theme, tone))
            },
            LinearLayout.LayoutParams(Md3.dp(ctx, 24), Md3.dp(ctx, 24)).apply { marginEnd = Md3.dp(ctx, 16) },
        )
        val texts = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(text(ctx, theme, title, 15f, true, "onSurface"))
        if (desc.isNotEmpty()) {
            texts.addView(
                text(ctx, theme, desc, 12f, false, "onSurfaceVariant"),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = Md3.dp(ctx, 2) },
            )
        }
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(
            ImageView(ctx).apply {
                setImageResource(R.drawable.ic_chevron_right)
                setColorFilter(tc(ctx, theme, "outline"))
            },
            LinearLayout.LayoutParams(Md3.dp(ctx, 20), Md3.dp(ctx, 20)).apply { marginStart = Md3.dp(ctx, 8) },
        )
        return row
    }

                                                
    fun settingsGroup(ctx: Context, theme: ThemeEngine, rows: List<View>): View {
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = Md3.shape(ctx, tc(ctx, theme, "surfaceContainer"), 16, 1f, tc(ctx, theme, "outlineVariant"))
            clipChildren = true
        }
        rows.forEachIndexed { idx, r ->
            if (idx > 0) {
                col.addView(
                    View(ctx).apply { setBackgroundColor(tc(ctx, theme, "outlineVariant")) },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Md3.dp(ctx, 1))
                        .apply { marginStart = Md3.dp(ctx, 18); marginEnd = Md3.dp(ctx, 18) },
                )
            }
            col.addView(r, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        return col
    }

                                                           
    fun pageTitle(ctx: Context, theme: ThemeEngine, t: String, sub: String = ""): View {
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        col.addView(text(ctx, theme, t, 20f, true, "onSurface"))
        if (sub.isNotEmpty()) {
            col.addView(
                text(ctx, theme, sub, 12.5f, false, "onSurfaceVariant"),
                LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 3) },
            )
        }
        return col
    }

    fun spacer(ctx: Context): View = View(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
    }

    fun row(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

               
    fun gap(ctx: Context, dpValue: Int): View = View(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(ctx, dpValue))
    }
}
