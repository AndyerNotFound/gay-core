package com.gaycore.app.ui

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import android.widget.Toast
import com.gaycore.app.theme.ThemeEngine

                                     
object UiKit {
    fun dp(ctx: Context, v: Float) = (v * ctx.resources.displayMetrics.density + 0.5f).toInt()
    fun dp(ctx: Context, v: Int) = dp(ctx, v.toFloat())

    fun column(ctx: Context, gap: Int = 0, padding: Int = 0): LinearLayout {
        val l = LinearLayout(ctx)
        l.orientation = LinearLayout.VERTICAL
        if (padding > 0) l.setPadding(dp(ctx, padding), dp(ctx, padding), dp(ctx, padding), dp(ctx, padding))
        if (gap > 0) l.tag = gap                    
        return l
    }

                                     
    fun LinearLayout.add(v: View, gapDp: Int = 0) {
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        if (childCount > 0 && gapDp > 0) lp.topMargin = dp(context, gapDp)
        addView(v, lp)
    }

    fun scroll(ctx: Context, content: View): ScrollView {
        val s = ScrollView(ctx)
        s.addView(content)
        s.isFillViewport = true
                                                                                        
        s.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        return s
    }

    fun text(ctx: Context, t: String, sizeSp: Float = 15f, bold: Boolean = false, color: Int? = null): TextView {
        val tv = TextView(ctx)
        tv.text = t
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD)
        if (color != null) tv.setTextColor(color)
        return tv
    }

    fun card(ctx: Context, theme: ThemeEngine, title: String? = null, outlined: Boolean = false): Pair<View, LinearLayout> {
                                                                                           
        val inner = column(ctx)
        inner.background = if (outlined) {
            Md3.shape(ctx, theme.color(ctx, "surfaceContainerLowest"), 16, 1f, theme.color(ctx, "outlineVariant"))
        } else {
            Md3.shape(ctx, theme.color(ctx, "surfaceContainer"), 20)
        }
        inner.setPadding(dp(ctx, 16f), dp(ctx, 14f), dp(ctx, 16f), dp(ctx, 14f))
        if (!title.isNullOrEmpty()) {
            inner.add(text(ctx, title, 16f, true, theme.color(ctx, "onSurface")))
            inner.add(View(ctx).apply { layoutParams = ViewGroup.LayoutParams(1, dp(ctx, 8f)) })
        }
        return inner to inner
    }

                                             
                                                                   
    fun button(ctx: Context, theme: ThemeEngine, text: String, style: String = "filled", iconRes: Int? = null): MaterialButton {
        val attr = when (style) {
            "tonal" -> com.google.android.material.R.attr.materialButtonStyle                           
            "outlined" -> com.google.android.material.R.attr.materialButtonOutlinedStyle
            "text" -> android.R.attr.borderlessButtonStyle
            else -> com.google.android.material.R.attr.materialButtonStyle
        }
        val b = MaterialButton(ctx, null, attr)
        b.text = text
        b.isAllCaps = false
        b.gravity = Gravity.CENTER
        b.letterSpacing = Md3.labelLarge.tracking / 100f
        b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, Md3.labelLarge.size)
        b.minimumHeight = 0
        b.minimumWidth = 0
        b.insetTop = 0
        b.insetBottom = 0
        b.height = Md3.dp(ctx, Md3.BTN_HEIGHT)
        b.cornerRadius = Md3.dp(ctx, Md3.BTN_HEIGHT / 2)                   
        b.iconGravity = MaterialButton.ICON_GRAVITY_START
        b.iconPadding = Md3.dp(ctx, Md3.SP_2)
        val padH = if (iconRes != null) Md3.BTN_PAD_H_ICON else Md3.BTN_PAD_H
        b.setPadding(Md3.dp(ctx, padH), 0, Md3.dp(ctx, Md3.BTN_PAD_H), 0)

        val onSurface = Md3.c(ctx, theme, Md3.Role.onSurface)
        val fill: Int; val label: Int
        when (style) {
            "tonal" -> {
                fill = Md3.c(ctx, theme, Md3.Role.secondaryContainer)
                label = Md3.c(ctx, theme, Md3.Role.onSecondaryContainer)
            }
            "outlined", "text" -> {
                fill = android.graphics.Color.TRANSPARENT
                label = Md3.c(ctx, theme, Md3.Role.primary)
            }
            else -> {
                fill = Md3.c(ctx, theme, Md3.Role.primary)
                label = Md3.c(ctx, theme, Md3.Role.onPrimary)
            }
        }
                                                                 
        b.backgroundTintList = ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(Md3.disabledContainer(onSurface), fill),
        )
        b.setTextColor(ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(Md3.disabledLabel(onSurface), label),
        ))
        if (style == "outlined") {
            b.strokeWidth = Md3.dp(ctx, 1)
            b.strokeColor = ColorStateList.valueOf(Md3.c(ctx, theme, Md3.Role.outline))
        } else {
            b.strokeWidth = 0
        }
        if (iconRes != null) {
            b.setIconResource(iconRes)
            b.iconTint = ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                intArrayOf(Md3.disabledLabel(onSurface), label),
            )
            b.iconSize = Md3.dp(ctx, Md3.BTN_ICON)
        }
        return b
    }

    fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

                                                                      
    fun confirm(ctx: Context, title: String, msg: String, theme: ThemeEngine? = null, onOk: () -> Unit) {
        MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok) { _, _ -> onOk() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

                               
    fun infoDialog(ctx: Context, theme: ThemeEngine, title: String, msg: String) {
        val pad = dp(ctx, 20f)
        val tv = text(ctx, msg, 12.5f, false, theme.color(ctx, "onSurfaceVariant")).apply {
            setTextIsSelectable(true)
                                       
            typeface = android.graphics.Typeface.MONOSPACE
            setLineSpacing(dp(ctx, 3f).toFloat(), 1f)
        }
        val sv = android.widget.ScrollView(ctx).apply {
            clipToPadding = false
            setPadding(pad, dp(ctx, 4f), pad, 0)
            addView(
                tv,
                android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        val dlg = MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setView(sv)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dlg.show()
                             
        dlg.window?.let { w ->
            val dm = ctx.resources.displayMetrics
            w.setLayout((dm.widthPixels * 0.92f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

                                               
    @Volatile private var keepOpenFlag = false

                                                         
    fun keepOpen() { keepOpenFlag = true }

    fun customDialog(
        ctx: Context, theme: ThemeEngine, title: String, content: View,
        okText: String = "确定", onOk: () -> Unit,
    ) {
        val sv = android.widget.ScrollView(ctx).apply { addView(content) }
        val dlg = MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setView(sv, 0, 0, 0, 0)
            .setPositiveButton(okText, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dlg.setOnShowListener {
            dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                                                                
                                                           
                keepOpenFlag = false
                try {
                    onOk()
                } finally {
                    val keep = keepOpenFlag
                    keepOpenFlag = false
                    if (!keep && dlg.isShowing) dlg.dismiss()
                }
            }
        }
        dlg.show()
    }

                        
    fun header(ctx: Context, theme: ThemeEngine, title: String, subtitle: String? = null): LinearLayout {
        val l = column(ctx)
        l.setPadding(dp(ctx, 16f), dp(ctx, 14f), dp(ctx, 16f), dp(ctx, 10f))
        l.add(text(ctx, title, 22f, true, theme.color(ctx, "onSurface")))
        if (subtitle != null) l.add(text(ctx, subtitle, 13f, false, theme.color(ctx, "onSurfaceVariant")), 2)
        return l
    }

    fun centerText(ctx: Context, t: String, color: Int? = null): TextView {
        val tv = text(ctx, t, 14f, false, color)
        tv.gravity = Gravity.CENTER
        tv.setPadding(dp(ctx, 24f), dp(ctx, 40f), dp(ctx, 24f), dp(ctx, 40f))
        return tv
    }

                                                                     
                                                       
    private val insetDone = java.util.WeakHashMap<View, Boolean>()

    fun applyInsets(root: View, top: Boolean = true, bottom: Boolean = true) {
        if (insetDone.containsKey(root)) return
        insetDone[root] = true
        val pl = root.paddingLeft; val pt = root.paddingTop
        val pr = root.paddingRight; val pb = root.paddingBottom
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
              
                                                                      
                                                     
                                                   
               
            val imeBottom = if (android.os.Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom
            } else 0
            val bottomPx = if (bottom) pb + maxOf(bars.bottom, imeBottom) else pb
            v.setPadding(pl, if (top) pt + bars.top else pt, pr, bottomPx)
            insets
        }
        root.requestApplyInsets()
    }
}


