package com.o3.vanguard.visionary.data.di

import com.o3.vanguard.visionary.core.common.VectorSearchEngine
import com.o3.vanguard.visionary.data.RoomIndexRepository
import com.o3.vanguard.visionary.data.RoomMediaRepository
import com.o3.vanguard.visionary.data.RoomSearchRepository
import com.o3.vanguard.visionary.data.RoomSettingsRepository
import com.o3.vanguard.visionary.data.RoomTrashRepository
import com.o3.vanguard.visionary.data.RoomVaultRepository
import com.o3.vanguard.visionary.domain.repository.IndexRepository
import com.o3.vanguard.visionary.domain.repository.MediaRepository
import com.o3.vanguard.visionary.domain.repository.SearchRepository
import com.o3.vanguard.visionary.domain.repository.SettingsRepository
import com.o3.vanguard.visionary.domain.repository.TrashRepository
import com.o3.vanguard.visionary.domain.repository.VaultRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    singleOf(::VectorSearchEngine)
    singleOf(::RoomMediaRepository) bind MediaRepository::class
    singleOf(::RoomSearchRepository) bind SearchRepository::class
    singleOf(::RoomIndexRepository) bind IndexRepository::class
    singleOf(::RoomSettingsRepository) bind SettingsRepository::class
    singleOf(::RoomVaultRepository) bind VaultRepository::class
    singleOf(::RoomTrashRepository) bind TrashRepository::class
}
