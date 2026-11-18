package com.star.menugenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.star.menugenerator.ui.StarRoot
import com.star.menugenerator.ui.theme.StarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StarTheme {
                StarRoot()
            }
        }
    }
}
