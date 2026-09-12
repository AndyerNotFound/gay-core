package com.gaycore.app

import android.app.Application
import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

   
                                                       
                        
   
object CrashHandler {
    private const val FILE = "crash.log"

    fun install(app: Application) {
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val txt = buildString {
                    append("时间: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())).append('\n')
                    append("版本: ").append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
                    append("线程: ").append(t.name).append('\n')
                    append('\n').append(sw.toString())
                }
                File(app.filesDir, FILE).writeText(txt)
                                           
                try { app.getExternalFilesDir(null)?.let { File(it, "gaycore-crash.log").writeText(txt) } } catch (_: Throwable) {}
            } catch (_: Throwable) {}
            def?.uncaughtException(t, e) ?: exitProcess(2)
        }
    }

                                            
    fun peek(ctx: Context): String? {
        return try {
            val f = File(ctx.filesDir, FILE)
            if (!f.exists()) return null
            val s = f.readText()
            if (s.isBlank()) null else if (s.length > 3000) s.substring(0, 3000) + "\n…(截断)" else s
        } catch (_: Throwable) { null }
    }

    fun clear(ctx: Context) {
        try { File(ctx.filesDir, FILE).delete() } catch (_: Throwable) {}
        try { ctx.getExternalFilesDir(null)?.let { File(it, "gaycore-crash.log").delete() } } catch (_: Throwable) {}
    }
}
