package com.metraza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.metraza.ui.screens.CalculatorScreen
import com.metraza.ui.theme.MetrazaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MetrazaTheme {
                CalculatorScreen()
            }
        }
    }
}
