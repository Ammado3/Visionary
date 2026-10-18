package com.o3.vanguard.visionary

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform