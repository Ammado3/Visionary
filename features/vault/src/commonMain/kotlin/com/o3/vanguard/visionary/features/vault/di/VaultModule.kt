package com.o3.vanguard.visionary.features.vault.di

import com.o3.vanguard.visionary.features.vault.valutgridscreen.VaultGridViewModel
import com.o3.vanguard.visionary.features.vault.vault.VaultLockViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val vaultModule = module {
    viewModelOf(::VaultLockViewModel)
    viewModelOf(::VaultGridViewModel)
}
