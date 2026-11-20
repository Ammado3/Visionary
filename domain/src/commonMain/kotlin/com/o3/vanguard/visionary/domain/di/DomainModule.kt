package com.o3.vanguard.visionary.domain.di

import com.o3.vanguard.visionary.domain.usecase.*
import com.o3.vanguard.visionary.domain.voice.VoiceSearchUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    factoryOf(::ObserveIndexProgressUseCase)
    factoryOf(::ObserveMediaUseCase)
    factoryOf(::ObserveAlbumMediaUseCase)
    factoryOf(::ObserveFavoriteMediaUseCase)
    factoryOf(::SetFavoriteUseCase)
    factoryOf(::ObserveItemUseCase)
    singleOf(::ObserveStorageAlbumsUseCase)
    factoryOf(::SearchMediaUseCase)
    factoryOf(::ResolveStartRouteUseCase)
    factoryOf(::MoveToVaultUseCase)
    factoryOf(::DeleteMediaUseCase)
    factoryOf(::SmartMediaClassifier)
    factoryOf(::EntityExtractor)
    factoryOf(::VoiceSearchUseCase)
}
