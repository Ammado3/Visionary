package com.o3.vanguard.visionary.core.common

import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

actual fun randomUuid(): String = NSUUID().UUIDString()
actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
