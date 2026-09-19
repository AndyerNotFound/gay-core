package com.gaycore.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.gaycore.app.theme.ThemeEngine







abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.decorView.post { installInsets(); installTint() }
    }

    



    protected open fun tintTheme(): ThemeEngine? = null

    






    private fun installTint() {
        try {
            if (tintTheme() == null) return
            val root = findViewById<View>(android.R.id.content) ?: return
            
            UiKit.autoTint(root) { tintTheme() }
        } catch (_: Throwable) { }
    }

    private fun installInsets() {
        try {
            

            findViewById<View>(android.R.id.content)?.let { UiKit.applyInsets(it) }
        } catch (_: Throwable) { }
    }
}
