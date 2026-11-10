package com.o3.vanguard.visionary.features.vault.vault

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val vaultModule = module {
    viewModelOf(::VaultViewModel)
}
