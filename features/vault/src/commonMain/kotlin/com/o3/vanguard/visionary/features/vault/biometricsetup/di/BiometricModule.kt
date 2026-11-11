package com.o3.vanguard.visionary.features.vault.biometricsetup.di

import com.o3.vanguard.visionary.features.vault.biometricsetup.BiometricViewModel
import com.o3.vanguard.visionary.features.vault.vault.VaultViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val biometric= module {
    viewModelOf(::BiometricViewModel)
}
