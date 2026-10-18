package com.o3.vanguard.visionary.core.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

fun getAndroidDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("visionary.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}

actual val databasePlatformModule: Module = module {
    single<RoomDatabase.Builder<AppDatabase>> { getAndroidDatabaseBuilder(get()) }
}
