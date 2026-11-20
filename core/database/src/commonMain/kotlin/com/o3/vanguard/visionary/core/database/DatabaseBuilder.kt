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
    // Never add a blanket destructive fallback. Every version bump needs an explicit
    // Migration and a committed schema JSON in core/database/schemas/.
    // TEMPORARY: versions 3 and 4 are reset (as Develop did before this merge) so old dev
    // installs don't crash on launch. Fix-plan Step 04 replaces 3 and 4 with real migrations.
    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4)
    .build()

val databaseModule = module {
    includes(databasePlatformModule)
    single<AppDatabase> { buildAppDatabase(get(), get()) }
    single { get<AppDatabase>().mediaDao() }
    single { get<AppDatabase>().searchDao() }
    single { get<AppDatabase>().indexJobDao() }
    single { get<AppDatabase>().vaultDao() }
}
