package com.gaycore.app

import android.app.Application
import android.content.Context
import com.gaycore.app.data.ServerStore

class App : Application() {
    lateinit var store: ServerStore
        private set

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        store = ServerStore(this)
    }

    companion object {
        fun of(ctx: Context): App = ctx.applicationContext as App
    }
}
