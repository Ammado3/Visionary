package com.o3.vanguard.visionary.feature.teleport.data

import android.os.Build
import com.o3.vanguard.visionary.feature.teleport.domain.port.DeviceNameProvider

class AndroidDeviceNameProvider : DeviceNameProvider {
    override fun deviceName(): String = Build.MODEL.take(MAX_LEN).ifBlank { "Android" }
    private companion object { const val MAX_LEN = 32 }
}
