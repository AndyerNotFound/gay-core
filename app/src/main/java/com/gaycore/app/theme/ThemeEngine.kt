package com.gaycore.app.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.view.Window
import com.gaycore.app.data.ThemeInfo



class ThemeEngine(
    private val theme: ThemeInfo?,
    private val darkMode: String = "system",
    private val dynamicColors: Boolean = false,
    private val customSeed: Int? = null,
) {

    init {
        
        last = this
    }

    companion object {
        







        @Volatile var last: ThemeEngine? = null

        
        val DARK = mapOf(
            
            "primary" to "#D0BCFF", "onPrimary" to "#381E72", "primaryContainer" to "#4F378B",
            "onPrimaryContainer" to "#EADDFF",
            
            "secondary" to "#CCC2DC", "onSecondary" to "#332D41", "secondaryContainer" to "#4A4458",
            "onSecondaryContainer" to "#E8DEF8",
            
            "tertiary" to "#EFB8C8", "onTertiary" to "#492532", "tertiaryContainer" to "#633B48",
            "onTertiaryContainer" to "#FFD8E4",
            
            "surface" to "#141218", "surfaceContainerLowest" to "#0F0D13",
            "surfaceContainer" to "#1D1B20", "surfaceContainerHigh" to "#2B2930",
            "surfaceContainerHighest" to "#36343B",
            "background" to "#141218", "onSurface" to "#E6E0E9", "onSurfaceVariant" to "#CAC4D0",
            "inverseSurface" to "#E6E0E9", "inverseOnSurface" to "#322F35",
            
            "outline" to "#938F99", "outlineVariant" to "#49454F",
            
            "error" to "#F2B8B5", "onError" to "#601410",
            "errorContainer" to "#8B1A1A", "onErrorContainer" to "#F9DEDC",
            "success" to "#4ADE80", "successContainer" to "#12301F", "onSuccessContainer" to "#86EFAC",
        )
        val LIGHT = mapOf(
            
            "primary" to "#6750A4", "onPrimary" to "#FFFFFF", "primaryContainer" to "#EADDFF",
            "onPrimaryContainer" to "#21005D",
            
            "secondary" to "#625B71", "onSecondary" to "#FFFFFF", "secondaryContainer" to "#E8DEF8",
            "onSecondaryContainer" to "#1D192B",
            
            "tertiary" to "#7D5260", "onTertiary" to "#FFFFFF", "tertiaryContainer" to "#FFD8E4",
            "onTertiaryContainer" to "#31111D",
            
            "surface" to "#FEF7FF", "surfaceContainerLowest" to "#FFFFFF",
            "surfaceContainer" to "#F3EDF7", "surfaceContainerHigh" to "#ECE6F0",
            "surfaceContainerHighest" to "#E6E0E9",
            "background" to "#FEF7FF", "onSurface" to "#1D1B20", "onSurfaceVariant" to "#49454F",
            "inverseSurface" to "#322F35", "inverseOnSurface" to "#F5EFF7",
            
            "outline" to "#79747E", "outlineVariant" to "#CAC4D0",
            
            "error" to "#B3261E", "onError" to "#FFFFFF",
            "errorContainer" to "#F9DEDC", "onErrorContainer" to "#410E0B",
            "success" to "#16A34A", "successContainer" to "#E8F8EE", "onSuccessContainer" to "#14532D",
        )

        fun isNight(ctx: Context): Boolean =
            (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun shouldUseDark(ctx: Context): Boolean = when (darkMode) {
        "dark" -> true
        "light" -> false
        else -> isNight(ctx)
    }

    
    private var cachedPalette: Map<String, String>? = null
    private var cachedDark: Boolean? = null

    fun palette(ctx: Context): Map<String, String> {
        val dark = shouldUseDark(ctx)
        val cached = cachedPalette
        if (cached != null && cachedDark == dark) return cached
        val base = if (dark) DARK else LIGHT
        val over = (if (dark) theme?.dark else theme?.light) ?: emptyMap()
        var result = base + over
        if (customSeed != null) {
            
            val dyn = dynamicPalette(customSeed!!, dark)
            if (dyn.isNotEmpty()) result = result + dyn
        } else if (dynamicColors && android.os.Build.VERSION.SDK_INT >= 31) {
            val seed = wallpaperSeed(ctx)
            if (seed != null) {
                val dyn = dynamicPalette(seed, dark)
                if (dyn.isNotEmpty()) result = result + dyn
            }
        }
        cachedPalette = result
        cachedDark = dark
        return result
    }

    






    private fun dynamicPalette(seed: Int, dark: Boolean): Map<String, String> {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            android.graphics.Color.red(seed), android.graphics.Color.green(seed), android.graphics.Color.blue(seed), hsv,
        )
        val h = hsv[0]
        
        val s = if (hsv[1] < 0.12f) 0.35f else minOf(hsv[1], 0.75f)

        fun c(hue: Float, sat: Float, value: Float): String =
            hexColor(
                android.graphics.Color.HSVToColor(
                    floatArrayOf(((hue % 360f) + 360f) % 360f, sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f)),
                ),
            )

        val m = HashMap<String, String>()
        if (dark) {
            m["primary"] = c(h, s * 0.55f, 0.82f)
            m["onPrimary"] = c(h, s * 0.85f, 0.18f)
            m["primaryContainer"] = c(h, s * 0.60f, 0.30f)
            m["onPrimaryContainer"] = c(h, s * 0.45f, 0.94f)
            m["secondary"] = c(h, s * 0.28f, 0.80f)
            m["onSecondary"] = c(h, s * 0.40f, 0.18f)
            m["secondaryContainer"] = c(h, s * 0.26f, 0.30f)
            m["onSecondaryContainer"] = c(h, s * 0.24f, 0.93f)
            m["tertiary"] = c(h + 60f, s * 0.40f, 0.80f)
            m["onTertiary"] = c(h + 60f, s * 0.45f, 0.18f)
            m["tertiaryContainer"] = c(h + 60f, s * 0.36f, 0.30f)
            m["onTertiaryContainer"] = c(h + 60f, s * 0.28f, 0.93f)
            m["background"] = c(h, 0.10f, 0.09f)
            m["surface"] = c(h, 0.10f, 0.09f)
            m["surfaceContainerLowest"] = c(h, 0.14f, 0.06f)
            m["surfaceContainer"] = c(h, 0.12f, 0.13f)
            m["surfaceContainerHigh"] = c(h, 0.11f, 0.18f)
            m["surfaceContainerHighest"] = c(h, 0.10f, 0.23f)
            m["onSurface"] = c(h, 0.05f, 0.92f)
            m["onSurfaceVariant"] = c(h, 0.12f, 0.80f)
            m["outline"] = c(h, 0.10f, 0.60f)
            m["outlineVariant"] = c(h, 0.10f, 0.30f)
            m["inverseSurface"] = c(h, 0.05f, 0.92f)
            m["inverseOnSurface"] = c(h, 0.08f, 0.20f)
        } else {
            m["primary"] = c(h, s * 0.80f, 0.42f)
            m["onPrimary"] = c(h, 0.06f, 1.0f)
            m["primaryContainer"] = c(h, s * 0.70f, 0.92f)
            m["onPrimaryContainer"] = c(h, s * 0.70f, 0.14f)
            m["secondary"] = c(h, s * 0.32f, 0.40f)
            m["onSecondary"] = c(h, 0.06f, 1.0f)
            m["secondaryContainer"] = c(h, s * 0.32f, 0.91f)
            m["onSecondaryContainer"] = c(h, s * 0.38f, 0.14f)
            m["tertiary"] = c(h + 60f, s * 0.45f, 0.40f)
            m["onTertiary"] = c(h + 60f, 0.06f, 1.0f)
            m["tertiaryContainer"] = c(h + 60f, s * 0.45f, 0.91f)
            m["onTertiaryContainer"] = c(h + 60f, s * 0.40f, 0.16f)
            m["background"] = c(h, 0.06f, 0.99f)
            m["surface"] = c(h, 0.06f, 0.99f)
            m["surfaceContainerLowest"] = c(h, 0.03f, 1.0f)
            m["surfaceContainer"] = c(h, 0.08f, 0.95f)
            m["surfaceContainerHigh"] = c(h, 0.09f, 0.92f)
            m["surfaceContainerHighest"] = c(h, 0.10f, 0.89f)
            m["onSurface"] = c(h, 0.12f, 0.11f)
            m["onSurfaceVariant"] = c(h, 0.14f, 0.32f)
            m["outline"] = c(h, 0.10f, 0.50f)
            m["outlineVariant"] = c(h, 0.12f, 0.78f)
            m["inverseSurface"] = c(h, 0.10f, 0.20f)
            m["inverseOnSurface"] = c(h, 0.05f, 0.95f)
        }
        return m
    }

    
    private fun wallpaperSeed(ctx: Context): Int? {
        if (android.os.Build.VERSION.SDK_INT < 31) return null
        return try {
            val wm = android.app.WallpaperManager.getInstance(ctx)
            val wc = try {
                wm.getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM)
            } catch (_: Throwable) {
                null
            } ?: try {
                wm.getWallpaperColors(android.app.WallpaperManager.FLAG_LOCK)
            } catch (_: Throwable) {
                null
            }
            wc?.primaryColor?.toArgb()
        } catch (_: Throwable) {
            null
        }
    }

    private fun hexColor(c: Int): String = String.format("#%06X", 0xFFFFFF and c)

    
    fun color(ctx: Context, name: String?): Int {
        if (name == null) return Color.TRANSPARENT
        val key = if (name.startsWith("$")) name.substring(1) else name
        val v = palette(ctx)[key] ?: name
        return try { Color.parseColor(v) } catch (_: Exception) { Color.parseColor(palette(ctx)["primary"]) }
    }

    
    private fun cssVar(name: String): String? = theme?.cssVars?.get(name)
    private fun parsePx(v: String?, default: Int): Int {
        if (v == null) return default
        val n = v.replace("px", "").replace("dp", "").trim().toFloatOrNull()?.toInt() ?: return default
        return n
    }
    
    fun shapeDp(name: String, defaultDp: Int): Int = parsePx(cssVar("gc-radius-$name"), defaultDp)
    
    fun strokeWidthDp(): Float = parsePx(cssVar("gc-stroke-width"), 1).toFloat()
    
    fun cardStyle(): String = cssVar("gc-card-style") ?: "outlined"
    
    fun scheme(): String = cssVar("gc-scheme") ?: "standard"

    fun applyToWindow(activity: Activity) {
        val w: Window = activity.window
        w.statusBarColor = color(activity, "background")
        w.navigationBarColor = color(activity, "background")
        val night = isNight(activity)
        w.decorView.post {
            val c = androidx.core.view.WindowInsetsControllerCompat(w, w.decorView)
            c.isAppearanceLightStatusBars = !night
            c.isAppearanceLightNavigationBars = !night
        }
    }
}
