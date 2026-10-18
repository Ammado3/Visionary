package com.o3.vanguard.visionary.core.database

import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.o3.vanguard.visionary.core.common.AppDispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect val databasePlatformModule: Module

fun buildAppDatabase(
    builder: RoomDatabase.Builder<AppDatabase>,
    dispatchers: AppDispatchers,
): AppDatabase = builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(dispatchers.io)
    .addCallback(FtsCallback)
    .build()

val databaseModule = module {
    includes(databasePlatformModule)
    single<AppDatabase> { buildAppDatabase(get(), get()) }
    single { get<AppDatabase>().mediaDao() }
    single { get<AppDatabase>().searchDao() }
    single { get<AppDatabase>().indexJobDao() }
    single { get<AppDatabase>().vaultDao() }
}
