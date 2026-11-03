package com.o3.vanguard.visionary.domain.di

import com.o3.vanguard.visionary.domain.usecase.*
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    factoryOf(::ObserveCollectionsUseCase)
    factoryOf(::ObserveRecentFindsUseCase)
    factoryOf(::ObserveIndexProgressUseCase)
    factoryOf(::RebuildIndexUseCase)
    factoryOf(::ObserveMediaUseCase)
    factoryOf(::ObserveItemUseCase)
    singleOf(::ObserveStorageAlbumsUseCase)
    factoryOf(::SearchMediaUseCase)
    factoryOf(::ResolveStartRouteUseCase)
    factoryOf(::MoveToVaultUseCase)
    factoryOf(::DeleteMediaUseCase)
    factoryOf(::StartScanUseCase)
    factoryOf(::SmartMediaClassifier)
    factoryOf(::EntityExtractor)
}
