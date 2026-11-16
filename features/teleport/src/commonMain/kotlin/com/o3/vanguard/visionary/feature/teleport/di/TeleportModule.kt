package com.o3.vanguard.visionary.feature.teleport.di

import com.o3.vanguard.visionary.feature.teleport.presentation.TeleportReceiverViewModel
import com.o3.vanguard.visionary.feature.teleport.presentation.TeleportSenderViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect val teleportPlatformModule: Module

val teleportModule = module {
    includes(teleportPlatformModule)
    viewModelOf(::TeleportSenderViewModel)
    viewModelOf(::TeleportReceiverViewModel)
}
