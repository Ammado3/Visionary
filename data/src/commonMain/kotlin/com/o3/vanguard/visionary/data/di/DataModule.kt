package com.o3.vanguard.visionary.data.di

import com.o3.vanguard.visionary.core.common.VectorSearchEngine
import com.o3.vanguard.visionary.data.*
import com.o3.vanguard.visionary.domain.repository.*
import org.koin.dsl.module

val dataModule = module {
    single { VectorSearchEngine() }
    single<MediaRepository> { RoomMediaRepository(get()) }
    single<SearchRepository> { RoomSearchRepository(get(), get()) }
    single<IndexRepository> { RoomIndexRepository(get(), get(), get(), get(), get()) }
    single<SettingsRepository> { RoomSettingsRepository() }
    single<VaultRepository> { RoomVaultRepository(get()) }
    single<TrashRepository> { RoomTrashRepository(get()) }
}
