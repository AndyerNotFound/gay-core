package com.gaycore.app.ui

import android.content.Context
import android.content.res.ColorStateList
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.gaycore.app.R
import com.gaycore.app.theme.ThemeEngine
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout














class MdField @JvmOverloads constructor(
    ctx: Context,
    private val theme: ThemeEngine,
    label: String,
    value: String = "",
    numeric: Boolean = false,
    pill: Boolean = false,
) : FrameLayout(ctx) {

    private val til: TextInputLayout =
        LayoutInflater.from(ctx).inflate(R.layout.md_field, this, false) as TextInputLayout

    val edit: TextInputEditText = til.editText as TextInputEditText

    val text: String get() = edit.text?.toString() ?: ""

    var inputType: Int
        get() = edit.inputType
        set(v) { edit.inputType = v }

    




    private val caretWidth by lazy { maxOf(2, Md3.dp(context, 2)) }
    private val caretHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var caretOn = true
    private var caretView: View? = null

    private val caretBlink = object : Runnable {
        override fun run() {
            caretOn = !caretOn
            caretView?.invalidate()
            caretHandler.postDelayed(this, 500)   
        }
    }

    init {
        addView(til, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        edit.setText(value)
        til.hint = label
        

        if (numeric) {
            edit.inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_SIGNED or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val inputR = Md3.dp(ctx, theme.shapeDp("input", if (pill) 28 else 12)).toFloat()
        til.setBoxCornerRadii(inputR, inputR, inputR, inputR)

        
        val onSurface = theme.color(ctx, Md3.Role.onSurface)
        val onSurfaceVariant = theme.color(ctx, Md3.Role.onSurfaceVariant)
        val outline = theme.color(ctx, Md3.Role.outline)
        val primary = theme.color(ctx, Md3.Role.primary)

        til.setBoxStrokeColorStateList(
            ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
                intArrayOf(primary, outline),
            ),
        )
        til.defaultHintTextColor = ColorStateList.valueOf(onSurfaceVariant)
        til.hintTextColor = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
            intArrayOf(primary, onSurfaceVariant),
        )
        edit.setTextColor(onSurface)
        edit.setHintTextColor(onSurfaceVariant)
        


        UiKit.tintEditCaret(edit, theme)
        


        installCustomCaret()
    }

    









    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        
        til.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        val w = if (View.MeasureSpec.getMode(widthMeasureSpec) == View.MeasureSpec.UNSPECIFIED) til.measuredWidth
        else View.MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, til.measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = r - l
        val h = b - t
        til.layout(0, 0, w, h)
        caretView?.layout(0, 0, w, h)
    }

    private fun installCustomCaret() {
        val cv = object : View(context) {
            private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            private val winA = IntArray(2)
            private val winB = IntArray(2)

            



            private fun offsetInSelf(): IntArray {
                edit.getLocationInWindow(winA)
                this.getLocationInWindow(winB)
                return intArrayOf(winA[0] - winB[0], winA[1] - winB[1])
            }

            

            private fun verticalOffset(): Int {
                val lay = edit.layout ?: return 0
                val vgrav = edit.gravity and android.view.Gravity.VERTICAL_GRAVITY_MASK
                if (vgrav == android.view.Gravity.TOP) return 0
                val box = edit.height - edit.totalPaddingTop - edit.totalPaddingBottom - lay.height
                val off = if (vgrav == android.view.Gravity.BOTTOM) box else box / 2
                return off.coerceAtLeast(0)
            }

            override fun onDraw(canvas: android.graphics.Canvas) {
                
                if (edit.isCursorVisible) edit.isCursorVisible = false
                if (!edit.hasFocus() || !caretOn) return
                val lay = edit.layout ?: return
                val n = edit.text?.length ?: 0
                val pos = edit.selectionEnd.coerceIn(0, n)
                val line = lay.getLineForOffset(pos)
                val off = offsetInSelf()
                val x = off[0] + edit.totalPaddingLeft + lay.getPrimaryHorizontal(pos) - edit.scrollX
                val vy = off[1] + edit.totalPaddingTop + verticalOffset()
                val top = vy + lay.getLineTop(line) - edit.scrollY
                val bottom = vy + lay.getLineBottom(line) - edit.scrollY
                paint.color = theme.color(context, "primary")
                paint.strokeWidth = caretWidth.toFloat()
                canvas.drawLine(x + caretWidth / 2f, top.toFloat(), x + caretWidth / 2f, bottom.toFloat(), paint)
            }
        }
        cv.isClickable = false
        cv.isFocusable = false
        addView(cv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        caretView = cv
        
        edit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                caretOn = true
                cv.invalidate()
            }
            override fun afterTextChanged(s: android.text.Editable?) { cv.invalidate() }
        })
        edit.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> cv.invalidate() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        caretHandler.removeCallbacks(caretBlink)
        caretHandler.postDelayed(caretBlink, 500)
    }

    override fun onDetachedFromWindow() {
        caretHandler.removeCallbacks(caretBlink)   
        super.onDetachedFromWindow()
    }

    

    

    fun setMultiline(minHeightPx: Int) {
        edit.inputType = edit.inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        edit.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        edit.minLines = 3
        edit.maxLines = Int.MAX_VALUE
        edit.isVerticalScrollBarEnabled = false
        edit.minHeight = minHeightPx
    }

    
    fun setLeadingIcon(res: Int) {
        til.setStartIconDrawable(res)
        til.setStartIconTintList(ColorStateList.valueOf(theme.color(context, Md3.Role.onSurfaceVariant)))
    }

    
    fun enablePasswordToggle() {
        til.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
    }
}
