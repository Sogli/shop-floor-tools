package com.example.racunanjekilaze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.racunanjekilaze.ui.screens.CalculatorScreen
import com.example.racunanjekilaze.ui.theme.KilazaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KilazaTheme {
                CalculatorScreen()
            }
        }
    }
}
