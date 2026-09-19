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
            



            inner.addView(View(ctx), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 8f)))
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
        b.cornerRadius = Md3.dp(ctx, theme.shapeDp("btn", Md3.BTN_HEIGHT / 2))
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
        



        val t = theme ?: ThemeEngine.last
        val dlg = ThemedDialogBuilder(ctx, t)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok) { _, _ -> onOk() }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        
        if (t != null) dlg.setOnShowListener { tintDialog(dlg, t) }
        dlg.show()
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
        val dlg = ThemedDialogBuilder(ctx, theme)
            .setTitle(title)
            .setView(sv)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dlg.setOnShowListener { tintDialog(dlg, theme) }
        dlg.show()
        tintDialog(dlg, theme)
        
        dlg.window?.let { w ->
            val dm = ctx.resources.displayMetrics
            w.setLayout((dm.widthPixels * 0.92f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    
    @Volatile private var keepOpenFlag = false

    
    fun keepOpen() { keepOpenFlag = true }

    






    






    fun retintTree(root: android.view.View?, theme: ThemeEngine) {
        if (root == null) return
        when (root) {
            is com.google.android.material.textfield.TextInputLayout -> {
                val c = root.context
                val onVar = theme.color(c, "onSurfaceVariant")
                val outline = theme.color(c, "outline")
                val primary = theme.color(c, "primary")
                root.setBoxStrokeColorStateList(
                    ColorStateList(arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()), intArrayOf(primary, outline)),
                )
                root.defaultHintTextColor = ColorStateList.valueOf(onVar)
                root.hintTextColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()), intArrayOf(primary, onVar),
                )
            }
            is com.google.android.material.progressindicator.LinearProgressIndicator -> {
                val c = root.context
                root.trackColor = theme.color(c, "surfaceContainerHighest")
                root.setIndicatorColor(theme.color(c, "primary"))
            }
            is android.widget.ProgressBar -> {
                val c = root.context
                root.indeterminateTintList = ColorStateList.valueOf(theme.color(c, "primary"))
                root.progressTintList = ColorStateList.valueOf(theme.color(c, "primary"))
            }
            is com.google.android.material.materialswitch.MaterialSwitch -> {
                val c = root.context
                root.trackTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(theme.color(c, "primary"), theme.color(c, "surfaceContainerHighest")),
                )
                root.thumbTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(theme.color(c, "onPrimary"), theme.color(c, "outline")),
                )
            }
            


            is android.widget.CheckBox -> {
                
                val c = root.context
                val onVar = theme.color(c, "onSurfaceVariant")
                root.buttonTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(theme.color(c, "primary"), onVar),
                )
                root.setTextColor(theme.color(c, "onSurface"))
            }
            is android.widget.RadioButton -> {
                
                val c = root.context
                val onVar = theme.color(c, "onSurfaceVariant")
                root.buttonTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(theme.color(c, "primary"), onVar),
                )
                root.setTextColor(theme.color(c, "onSurface"))
            }
            is android.widget.Switch -> {
                
                val c = root.context
                root.trackTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(theme.color(c, "primary"), theme.color(c, "surfaceContainerHighest")),
                )
                root.thumbTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(theme.color(c, "onPrimary"), theme.color(c, "outline")),
                )
            }
            is android.widget.EditText -> {
                

                val c = root.context
                root.setTextColor(theme.color(c, "onSurface"))
                root.setHintTextColor(theme.color(c, "onSurfaceVariant"))
                root.highlightColor = Md3.withAlpha(theme.color(c, "primary"), 0.30f)
                tintEditCaret(root, theme)
            }
            is com.google.android.material.progressindicator.CircularProgressIndicator -> {
                root.setIndicatorColor(theme.color(root.context, "primary"))
            }
            is android.widget.ScrollView, is androidx.core.widget.NestedScrollView -> {
                tintScrollThumb(root, theme)
            }
        }
        if (root is android.view.ViewGroup) {
            for (i in 0 until root.childCount) retintTree(root.getChildAt(i), theme)
        }
    }

    

    

    @Volatile var lastCaretStatus: String = "未执行（还没有输入框被着色）"

    
    private val tintHooked = java.util.WeakHashMap<View, Boolean>()

    













    fun autoTint(root: View?, themeProvider: () -> ThemeEngine?) {
        if (root == null) return
        themeProvider()?.let { retintTree(root, it) }
        hookTree(root, themeProvider)
    }

    private fun hookTree(v: View, themeProvider: () -> ThemeEngine?) {
        if (v !is ViewGroup) return
        




        val reserved = v is androidx.coordinatorlayout.widget.CoordinatorLayout ||
            v is com.google.android.material.chip.ChipGroup
        if (!reserved && tintHooked.put(v, true) == null) {
            v.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    val c = child ?: return
                    themeProvider()?.let { retintTree(c, it) }
                    hookTree(c, themeProvider)
                }
                override fun onChildViewRemoved(parent: View?, child: View?) {}
            })
        }
        for (i in 0 until v.childCount) hookTree(v.getChildAt(i), themeProvider)
    }

    





    private fun tintScrollThumb(v: View, theme: ThemeEngine) {
        if (android.os.Build.VERSION.SDK_INT < 29) return
        try {
            val c = v.context
            val w = Md3.dp(c, 4)
            val thumb = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(theme.color(c, "outlineVariant"))
                cornerRadius = w / 2f
                setSize(w, Md3.dp(c, 40))
            }
            View::class.java
                .getMethod("setScrollbarThumbVertical", android.graphics.drawable.Drawable::class.java)
                .invoke(v, thumb)
        } catch (_: Throwable) { }
    }

    








    
    internal fun tintEditCaret(e: android.widget.EditText, theme: ThemeEngine) {
        if (android.os.Build.VERSION.SDK_INT < 29) return
        val c = e.context
        val primary = theme.color(c, "primary")
        var how = "自造光标"
        







        val caretW = maxOf(6, Md3.dp(c, 6))
        try {
            e.textCursorDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(primary)
                setSize(caretW, maxOf(Md3.dp(c, 16), maxOf(0, e.lineHeight)))
            }
        } catch (_: Throwable) {
            

            try {
                val cur = e.textCursorDrawable
                if (cur != null) {
                    val tinted = cur.mutate()
                    tinted.setTint(primary)
                    e.textCursorDrawable = tinted
                    how = "tint系统光标(兜底)"
                }
            } catch (_: Throwable) { }
        }
        

        var handles = 0
        for (side in listOf("Left", "Right", "Center")) {
            try {
                val g = android.widget.EditText::class.java.getMethod("getTextSelectHandle$side")
                val s = android.widget.EditText::class.java.getMethod(
                    "setTextSelectHandle$side", android.graphics.drawable.Drawable::class.java,
                )
                (g.invoke(e) as? android.graphics.drawable.Drawable)?.let { h ->
                    

                    val tinted = h.mutate()
                    tinted.setTint(primary)
                    s.invoke(e, tinted)
                    handles++
                }
            } catch (_: Throwable) { }
        }
        
        lastCaretStatus = how + " · #" + Integer.toHexString(primary) + " · 宽" + caretW + "px · 手柄" + handles
        try {
            android.util.Log.i("GayTint", "光标着色 " + e.javaClass.simpleName + " " + lastCaretStatus)
        } catch (_: Throwable) { }
    }

    








    fun tintDialogWindow(dialog: android.app.Dialog, theme: ThemeEngine, bottomSheet: Boolean = false) {
        val dec = try { dialog.window?.decorView } catch (_: Throwable) { null } ?: return
        if (bottomSheet) {
            try {
                

                dec.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { bs ->
                    bs.backgroundTintList = ColorStateList.valueOf(theme.color(bs.context, "surfaceContainer"))
                }
            } catch (_: Throwable) { }
        }
        retintTree(dec, theme)
        autoTint(dec) { theme }
    }

    
    fun tintDialog(d: androidx.appcompat.app.AlertDialog, theme: ThemeEngine?) {
        val t = theme ?: ThemeEngine.last ?: return
        val bgHit = tintDialogBackground(d, t)
        val p = t.color(d.context, "primary")
        d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(p)
        d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(p)
        d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL)?.setTextColor(p)
        retintTree(d.window?.decorView, t)
        
        try {
            val ctx = d.context
            val titleId = ctx.resources.getIdentifier("alertTitle", "id", "android")
            if (titleId != 0) {
                d.findViewById<android.widget.TextView>(titleId)?.setTextColor(t.color(ctx, "onSurface"))
            }
            d.findViewById<android.widget.TextView>(android.R.id.message)
                ?.setTextColor(t.color(ctx, "onSurfaceVariant"))
        } catch (_: Throwable) { }
        
        autoTint(d.window?.decorView) { t }
        

        try {
            android.util.Log.i(
                "GayTint",
                "弹窗着色 theme=" + (if (theme == null) "全局兜底" else "显式") +
                    " primary=" + Integer.toHexString(p) +
                    " surfaceHigh=" + Integer.toHexString(t.color(d.context, "surfaceContainerHigh")) +
                    " 底色命中=" + bgHit,
            )
        } catch (_: Throwable) { }
    }

    







    fun tintDialogBackground(d: androidx.appcompat.app.AlertDialog, theme: ThemeEngine): String? {
        val win = d.window ?: return null
        val color = theme.color(d.context, "surfaceContainerHigh")
        

        val dec = win.decorView
        if (dec != null && dec.background != null) {
            try {
                dec.backgroundTintList = ColorStateList.valueOf(color)
                return "DecorView(window背景)✓"
            } catch (_: Exception) { }
        }
        
        val content = dec?.findViewById<View>(android.R.id.content) ?: return null
        return paintRootBackground(content, color, 0)
    }

    
    private fun paintRootBackground(v: View, color: Int, depth: Int): String? {
        if (depth > 4) return null
        if (v.background != null && v !is android.widget.EditText && v !is android.widget.TextView) {
            var ok = false
            try { v.backgroundTintList = ColorStateList.valueOf(color); ok = true } catch (_: Exception) { }
            return v.javaClass.simpleName + if (ok) "✓" else "✗"
        }
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                val r = paintRootBackground(v.getChildAt(i), color, depth + 1)
                if (r != null) return r
            }
        }
        return null
    }

    class ThemedDialogBuilder(ctx: Context, theme: ThemeEngine?) :
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx) {
        
        private val th: ThemeEngine? = theme ?: ThemeEngine.last
        override fun show(): androidx.appcompat.app.AlertDialog {
            val d = super.show()
            tintDialog(d, th)
            return d
        }
    }

    fun customDialog(
        ctx: Context, theme: ThemeEngine, title: String, content: View,
        okText: String = "确定", onOk: () -> Unit,
    ) {
        val sv = android.widget.ScrollView(ctx).apply { addView(content) }
        



        val sidePad = dp(ctx, 24f)
        val hasOwnPad = content.paddingLeft > 0 || content.paddingRight > 0
        val cL = if (hasOwnPad) 0 else sidePad
        val cT = if (hasOwnPad) 0 else dp(ctx, 8f)
        val dlg = ThemedDialogBuilder(ctx, theme)
            .setTitle(title)
            .setView(sv, cL, cT, cL, 0)
            .setPositiveButton(okText, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dlg.setOnShowListener {
            
            tintDialog(dlg, theme)
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


