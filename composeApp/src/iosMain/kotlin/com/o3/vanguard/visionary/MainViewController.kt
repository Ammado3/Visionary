package com.o3.vanguard.visionary

import androidx.compose.ui.window.ComposeUIViewController
import com.o3.vanguard.visionary.core.common.AppConfig
import com.o3.vanguard.visionary.di.appModules
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

private var koinInitialized = false

fun initKoin() {
    if (koinInitialized) return
    koinInitialized = true
    val config = AppConfig(
        versionName = "1.0",
        isDebug = true,
        useFakeData = false
    )
    startKoin {
        modules(appModules(config))
    }
}

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
