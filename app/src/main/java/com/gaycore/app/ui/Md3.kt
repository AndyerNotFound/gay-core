package com.gaycore.app.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.widget.TextView
import com.gaycore.app.theme.ThemeEngine







object Md3 {

    

    
    data class Type(val size: Float, val lineHeight: Float, val weight: Int, val tracking: Float = 0f)

    val displayLarge = Type(57f, 64f, Typeface.NORMAL, -0.25f)
    val displayMedium = Type(45f, 52f, Typeface.NORMAL)
    val displaySmall = Type(36f, 44f, Typeface.NORMAL)

    val headlineLarge = Type(32f, 40f, Typeface.NORMAL)
    val headlineMedium = Type(28f, 36f, Typeface.NORMAL)
    val headlineSmall = Type(24f, 32f, Typeface.NORMAL)

    val titleLarge = Type(22f, 28f, Typeface.NORMAL)
    val titleMedium = Type(16f, 24f, Typeface.BOLD, 0.15f)
    val titleSmall = Type(14f, 20f, Typeface.BOLD, 0.1f)

    val bodyLarge = Type(16f, 24f, Typeface.NORMAL, 0.5f)
    val bodyMedium = Type(14f, 20f, Typeface.NORMAL, 0.25f)
    val bodySmall = Type(12f, 16f, Typeface.NORMAL, 0.4f)

    val labelLarge = Type(14f, 20f, Typeface.BOLD, 0.1f)
    val labelMedium = Type(12f, 16f, Typeface.BOLD, 0.5f)
    val labelSmall = Type(11f, 16f, Typeface.BOLD, 0.5f)

    

    const val SHAPE_NONE = 0
    const val SHAPE_XS = 4          
    const val SHAPE_S = 8
    const val SHAPE_M = 12          
    const val SHAPE_L = 16
    const val SHAPE_XL = 28         
    const val SHAPE_FULL = 999      

    

    const val SP_1 = 4
    const val SP_2 = 8
    const val SP_3 = 12
    const val SP_4 = 16
    const val SP_5 = 20
    const val SP_6 = 24
    const val SP_8 = 32

    
    const val SCREEN_MARGIN = 16

    
    const val TOUCH_TARGET = 48

    

    const val BTN_HEIGHT = 40
    const val BTN_PAD_H = 24          
    const val BTN_PAD_H_ICON = 16     
    const val BTN_ICON = 18
    const val CHIP_HEIGHT = 32
    const val ICON_BTN_SIZE = 48      
    const val ICON_BTN_ICON = 24
    const val FIELD_HEIGHT = 56
    const val CARD_PAD = 16
    const val CARD_RADIUS = SHAPE_M

    

    fun dp(ctx: Context, v: Number): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), ctx.resources.displayMetrics).toInt()

    
    fun apply(tv: TextView, t: Type, color: Int) {
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, t.size)
        tv.setLineSpacing(dp(tv.context, t.lineHeight - t.size).toFloat(), 1f)
        tv.setTypeface(Typeface.create(tv.typeface ?: Typeface.DEFAULT, t.weight))
        if (t.tracking != 0f) tv.letterSpacing = t.tracking / 100f
        tv.setTextColor(color)
    }

    
    fun ripple(color: Int): RippleDrawable =
        RippleDrawable(ColorStateList.valueOf(withAlpha(color, 0.12f)), null, null)

    fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((255 * alpha).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    
    fun disabledContainer(onSurface: Int) = withAlpha(onSurface, 0.12f)
    fun disabledLabel(onSurface: Int) = withAlpha(onSurface, 0.38f)

    
    fun shape(ctx: Context, fill: Int, radiusDp: Int, strokeDp: Float = 0f, stroke: Int = Color.TRANSPARENT) =
        MaterialShapeDrawable(
            ShapeAppearanceModel.builder()
                .setAllCornerSizes(
                    if (radiusDp >= SHAPE_FULL) dp(ctx, 1000) / 2f * 2f
                    else dp(ctx, radiusDp).toFloat(),
                )
                .build(),
        ).apply {
            fillColor = ColorStateList.valueOf(fill)
            if (strokeDp > 0f) {
                strokeWidth = dp(ctx, strokeDp).toFloat()
                strokeColor = ColorStateList.valueOf(stroke)
            }
        }

    
    object Role {
        const val primary = "primary"
        const val onPrimary = "onPrimary"
        const val primaryContainer = "primaryContainer"
        const val onPrimaryContainer = "onPrimaryContainer"
        const val secondaryContainer = "secondaryContainer"
        const val onSecondaryContainer = "onSecondaryContainer"
        const val tertiary = "tertiary"
        const val background = "background"
        const val surface = "surface"
        const val onSurface = "onSurface"
        const val surfaceContainer = "surfaceContainer"
        const val surfaceContainerHigh = "surfaceContainerHigh"
        const val surfaceContainerHighest = "surfaceContainerHighest"
        const val surfaceContainerLow = "surfaceContainerLow"
        const val onSurfaceVariant = "onSurfaceVariant"
        const val outline = "outline"
        const val outlineVariant = "outlineVariant"
        const val error = "error"
        const val onError = "onError"
        const val errorContainer = "errorContainer"
        const val onErrorContainer = "onErrorContainer"
    }

    fun c(ctx: Context, theme: ThemeEngine, role: String): Int = theme.color(ctx, role)
}
