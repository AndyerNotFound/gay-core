package com.gaycore.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import java.net.URL
import java.util.concurrent.Executors

   
                              
  
                                              
                            
  
                                                           
                   
   
object ImageLoader {

    private val cache = LruCache<String, Bitmap>(24)
    private val pool = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

                                                       
    fun load(url: String, into: ImageView, circular: Boolean = true, onFail: (() -> Unit)? = null) {
        if (url.isBlank() || !(url.startsWith("http://") || url.startsWith("https://"))) {
            onFail?.invoke()
            return
        }
        into.tag = url
        cache.get(url)?.let { bmp -> apply(into, url, bmp, circular); return }
        pool.execute {
            val bmp = try {
                URL(url).openStream().use { BitmapFactory.decodeStream(it) }
            } catch (_: Throwable) {
                null
            }
            if (bmp == null) {
                main.post { if (into.tag == url) onFail?.invoke() }
                return@execute
            }
            cache.put(url, bmp)
            main.post { if (into.tag == url) apply(into, url, bmp, circular) }
        }
    }

    private fun apply(iv: ImageView, url: String, bmp: Bitmap, circular: Boolean) {
        if (iv.tag != url) return
        if (circular) {
            try {
                val d = RoundedBitmapDrawableFactory.create(iv.resources, bmp)
                d.isCircular = true
                iv.setImageDrawable(d)
                return
            } catch (_: Throwable) {             }
        }
        iv.setImageBitmap(bmp)
    }
}
