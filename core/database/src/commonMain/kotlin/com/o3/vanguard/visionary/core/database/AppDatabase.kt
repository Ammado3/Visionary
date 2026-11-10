package com.o3.vanguard.visionary.core.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.o3.vanguard.visionary.core.database.dao.*
import com.o3.vanguard.visionary.core.database.entity.*

@Database(
    entities = [
        MediaEntity::class,
        MediaTextEntity::class,
        IndexJobEntity::class,
        IndexExclusionEntity::class,
        VaultItemEntity::class,
        RecentSearchEntity::class,
        SyncCheckpointEntity::class,
    ],
    version = 2,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun searchDao(): SearchDao
    abstract fun indexJobDao(): IndexJobDao
    abstract fun vaultDao(): VaultDao
}
