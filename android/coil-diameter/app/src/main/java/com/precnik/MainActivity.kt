package com.precnik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.precnik.ui.screens.CalculatorScreen
import com.precnik.ui.theme.PrecnikTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrecnikTheme {
                CalculatorScreen()
            }
        }
    }
}
