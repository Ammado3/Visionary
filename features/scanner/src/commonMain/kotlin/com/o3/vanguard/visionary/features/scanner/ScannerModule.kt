package com.o3.vanguard.visionary.features.scanner

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val scannerModule = module {
    viewModelOf(::ScannerViewModel)
}
