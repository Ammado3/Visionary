package com.o3.vanguard.visionary.core.testkit.fake

import com.o3.vanguard.visionary.domain.port.BackgroundSyncScheduler

class FakeBackgroundSyncScheduler : BackgroundSyncScheduler {
    val scheduleCalls = mutableListOf<Boolean>()

    override fun scheduleIncrementalIndexing(chargingOnly: Boolean) {
        scheduleCalls.add(chargingOnly)
    }
}
