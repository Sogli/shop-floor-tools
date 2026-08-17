package com.livnica

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    private lateinit var repo: ShiftRepository
    private var servicesInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        // Proveri da li postoji crash log od prethodnog pokretanja
        val crashFile = File(filesDir, LivnicaApplication.CRASH_LOG_FILE)
        if (crashFile.exists()) {
            val crashLog = crashFile.readText()
            crashFile.delete()
            setContent {
                LivnicaTheme {
                    CrashLogScreen(crashLog)
                }
            }
            return
        }

        // Normalna inicijalizacija wrappovana u try-catch
        try {
            initializeApp()
        } catch (e: Exception) {
            Log.e("MainActivity", "Inicijalizacija pukla", e)
            val sw = StringWriter()
            e.printStackTrace(PrintWriter(sw))
            setContent {
                LivnicaTheme {
                    CrashLogScreen(sw.toString())
                }
            }
        }
    }

    private fun initializeApp() {
        val app = application as LivnicaApplication
        app.initializeServices()
        repo = app.shiftRepository
        servicesInitialized = true

        setContent {
            LivnicaApp(repo)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (servicesInitialized) {
            repo.close()
        }
    }
}

