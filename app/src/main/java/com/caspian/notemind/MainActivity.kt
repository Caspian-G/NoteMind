package com.caspian.notemind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.caspian.notemind.data.Store
import com.caspian.notemind.ui.NoteMindApp
import com.caspian.notemind.ui.theme.NoteMindTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        enableEdgeToEdge()
        setContent {
            NoteMindTheme {
                NoteMindApp()
            }
        }
    }
}
