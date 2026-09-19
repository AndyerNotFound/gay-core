package com.gaycore.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import java.net.URL
import java.util.concurrent.Executors













object ImageLoader {

    



    enum class Shape { CIRCLE, ROUNDED, LOGO }

    private val cache = LruCache<String, Bitmap>(24)
    private val pool = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

    private const val SVG_RENDER_PX = 96

    
    @JvmOverloads
    fun load(
        url: String,
        into: ImageView,
        shape: Shape = Shape.ROUNDED,
        bgColor: Int = 0,
        onFail: (() -> Unit)? = null,
    ) {
        if (url.isBlank() || !(url.startsWith("http://") || url.startsWith("https://"))) {
            onFail?.invoke()
            return
        }
        into.tag = url
        if (bgColor != 0) into.setBackgroundColor(bgColor)
        cache.get(url)?.let { bmp -> apply(into, url, bmp, shape); return }
        pool.execute {
            val bmp = try {
                val bytes = URL(url).openStream().use { it.readBytes() }
                decode(bytes)
            } catch (_: Throwable) {
                null
            }
            if (bmp == null) {
                main.post { if (into.tag == url) onFail?.invoke() }
                return@execute
            }
            cache.put(url, bmp)
            main.post { if (into.tag == url) apply(into, url, bmp, shape) }
        }
    }

    

    @JvmOverloads
    fun loadFile(
        path: String,
        into: ImageView,
        shape: Shape = Shape.ROUNDED,
        bgColor: Int = 0,
        onFail: (() -> Unit)? = null,
    ) {
        if (path.isBlank() || !java.io.File(path).exists()) {
            onFail?.invoke()
            return
        }
        into.tag = path
        if (bgColor != 0) into.setBackgroundColor(bgColor)
        cache.get(path)?.let { bmp -> apply(into, path, bmp, shape); return }
        pool.execute {
            val bmp = try {
                BitmapFactory.decodeFile(path)
            } catch (_: Throwable) {
                null
            }
            if (bmp == null) {
                main.post { if (into.tag == path) onFail?.invoke() }
                return@execute
            }
            cache.put(path, bmp)
            main.post { if (into.tag == path) apply(into, path, bmp, shape) }
        }
    }

    
    private fun decode(bytes: ByteArray): Bitmap? {
        if (isSvg(bytes)) {
            return try {
                val svg = com.caverock.androidsvg.SVG.getFromString(String(bytes, Charsets.UTF_8))
                svg.setDocumentWidth(SVG_RENDER_PX.toFloat())
                svg.setDocumentHeight(SVG_RENDER_PX.toFloat())
                val bmp = Bitmap.createBitmap(SVG_RENDER_PX, SVG_RENDER_PX, Bitmap.Config.ARGB_8888)
                svg.renderToCanvas(Canvas(bmp))
                bmp
            } catch (_: Throwable) {
                null
            }
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun isSvg(bytes: ByteArray): Boolean {
        if (bytes.size < 16) return false
        val head = String(bytes, 0, minOf(bytes.size, 96), Charsets.US_ASCII).lowercase()
        return head.contains("<svg") || head.contains("<?xml")
    }

    private fun apply(iv: ImageView, url: String, bmp: Bitmap, shape: Shape) {
        if (iv.tag != url) return
        try {
            val d = RoundedBitmapDrawableFactory.create(iv.resources, bmp)
            when (shape) {
                Shape.CIRCLE -> {
                    d.isCircular = true
                    iv.scaleType = ImageView.ScaleType.CENTER_CROP
                }
                Shape.ROUNDED -> {
                    d.cornerRadius = minOf(bmp.width, bmp.height) * 0.24f
                    iv.scaleType = ImageView.ScaleType.CENTER_CROP
                }
                Shape.LOGO -> {
                    
                    d.cornerRadius = minOf(bmp.width, bmp.height) * 0.24f
                    iv.scaleType = ImageView.ScaleType.FIT_CENTER
                }
            }
            iv.setImageDrawable(d)
            return
        } catch (_: Throwable) {  }
        iv.setImageBitmap(bmp)
    }
}
