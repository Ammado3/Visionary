package com.o3.vanguard.visionary.data.di

import com.o3.vanguard.visionary.data.*
import com.o3.vanguard.visionary.domain.repository.*
import org.koin.dsl.module

val dataModule = module {
    single<MediaRepository> { RoomMediaRepository(get()) }
    single<SearchRepository> { RoomSearchRepository(get()) }
    single<IndexRepository> { RoomIndexRepository(get()) }
    single<SettingsRepository> { RoomSettingsRepository() }
    single<VaultRepository> { RoomVaultRepository(get()) }
}
