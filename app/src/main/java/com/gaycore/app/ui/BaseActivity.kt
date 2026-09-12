package com.gaycore.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

   
                                                
  
                                                          
                                                
   
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
                                                                      
        window.decorView.post { installInsets() }
    }

    private fun installInsets() {
        try {
                                            
                                                                
            findViewById<View>(android.R.id.content)?.let { UiKit.applyInsets(it) }
        } catch (_: Throwable) { }
    }
}
