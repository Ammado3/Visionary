package com.o3.vanguard.visionary

import android.app.Application
import com.o3.vanguard.visionary.core.common.AppConfig
import com.o3.vanguard.visionary.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VisionaryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val config = AppConfig(
            versionName = "1.0",
            isDebug = true,
            useFakeData = true
        )
        startKoin {
            androidContext(this@VisionaryApp)
            modules(appModules(config))
        }
    }
}
