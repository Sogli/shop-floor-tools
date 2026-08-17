package com.livnica

import android.app.Application
import com.programi.crashlog.CrashLogStorage

class LivnicaApplication : Application() {
    lateinit var shiftRepository: ShiftRepository
        private set

    companion object {
        const val CRASH_LOG_FILE = CrashLogStorage.CRASH_LOG_FILE
    }

    override fun onCreate() {
        super.onCreate()
        CrashLogStorage.install(this, getString(R.string.app_name))
    }

    /**
     * Inicijalizuje repozitorijum.
     * Poziva se iz MainActivity POSLE provere crash loga.
     */
    fun initializeServices() {
        if (::shiftRepository.isInitialized) return

        // Inicijalizuj repozitorijum (podaci samo u memoriji)
        shiftRepository = ShiftRepository()

        // Učitaj iz lokalne memorije ODMAH (sinhrono, brzo)
        shiftRepository.loadFromLocal(this)

        // Auto-save lokalno pri svakoj promeni podataka
        shiftRepository.onChange {
            shiftRepository.saveToLocal(this@LivnicaApplication)
        }
    }
}

