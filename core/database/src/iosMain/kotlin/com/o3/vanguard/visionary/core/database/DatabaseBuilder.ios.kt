package com.o3.vanguard.visionary.core.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun getIosDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val docDir = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val dbPath = requireNotNull(docDir?.path) + "/visionary.db"
    return Room.databaseBuilder<AppDatabase>(name = dbPath)
}

actual val databasePlatformModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> { getIosDatabaseBuilder() }
}
