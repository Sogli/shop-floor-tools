package com.precnik

import android.app.Application
import com.programi.crashlog.CrashLogStorage

class CrashLoggingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogStorage.install(this, getString(R.string.app_name))
    }
}
