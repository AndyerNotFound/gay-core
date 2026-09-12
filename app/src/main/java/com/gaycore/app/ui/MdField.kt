package com.gaycore.app.ui

import android.content.Context
import android.content.res.ColorStateList
import android.text.InputType
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.gaycore.app.R
import com.gaycore.app.theme.ThemeEngine
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

   
                                                              
  
           
                                                     
                                                           
                                        
  
                                                    
                                
  
                                              
   
class MdField @JvmOverloads constructor(
    ctx: Context,
    theme: ThemeEngine,
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

    init {
        addView(til, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        edit.setText(value)
        til.hint = label
        if (numeric) edit.inputType = InputType.TYPE_CLASS_NUMBER
        if (pill) {
                                      
            val r = Md3.dp(ctx, 28).toFloat()
            til.setBoxCornerRadii(r, r, r, r)
        }

                              
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
    }
}
