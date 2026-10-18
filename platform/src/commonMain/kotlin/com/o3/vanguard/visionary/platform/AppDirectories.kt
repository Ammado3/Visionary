package com.o3.vanguard.visionary.platform

expect class AppDirectories {
    fun vaultBlobsDirPath(): String
    fun scansDirPath(): String
    fun cacheDirPath(): String
}
