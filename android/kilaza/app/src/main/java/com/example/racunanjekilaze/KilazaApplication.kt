package com.example.racunanjekilaze

import android.app.Application
import com.programi.crashlog.CrashLogStorage

/**
 * Aplikaciona klasa sa globalnim hvatačem grešaka.
 */
class KilazaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLogStorage.install(this, getString(R.string.app_name))
    }
}
