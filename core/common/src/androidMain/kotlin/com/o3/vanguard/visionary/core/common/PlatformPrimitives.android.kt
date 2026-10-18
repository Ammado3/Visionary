package com.o3.vanguard.visionary.core.common

import java.util.UUID

actual fun randomUuid(): String = UUID.randomUUID().toString()
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
