package com.o3.vanguard.visionary.di

import com.o3.vanguard.visionary.core.common.AppConfig
import com.o3.vanguard.visionary.core.common.coreCommonModule
import com.o3.vanguard.visionary.core.database.databaseModule
import com.o3.vanguard.visionary.core.testing.fakeDataModule
import com.o3.vanguard.visionary.data.di.dataModule
import com.o3.vanguard.visionary.domain.di.domainModule
import com.o3.vanguard.visionary.features.gallery.di.galleryModule
import com.o3.vanguard.visionary.features.home.homeModule
import com.o3.vanguard.visionary.features.onboarding.onboardingModule
import com.o3.vanguard.visionary.features.scanner.scannerModule
import com.o3.vanguard.visionary.features.search.searchModule
import com.o3.vanguard.visionary.features.settings.settingsModule
import com.o3.vanguard.visionary.features.vault.biometricsetup.di.biometric
import com.o3.vanguard.visionary.features.vault.vault.vaultModule
import com.o3.vanguard.visionary.platform.di.platformModule
import org.koin.core.module.Module
import org.koin.dsl.module

val featureModules = listOf(
    onboardingModule,
    homeModule,
    galleryModule,
    searchModule,
    scannerModule,
    vaultModule,
    biometric,
    settingsModule,
)

fun appModules(config: AppConfig): List<Module> = buildList {
    add(module { single { config } })
    add(coreCommonModule)
    add(domainModule)
    addAll(featureModules)
    if (config.useFakeData) {
        add(fakeDataModule)
    } else {
        add(databaseModule)
        add(dataModule)
        add(platformModule)
    }
}
