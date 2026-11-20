package com.o3.vanguard.visionary.core.testkit.fake

import com.o3.vanguard.visionary.domain.port.BackgroundSyncScheduler

class FakeBackgroundSyncScheduler : BackgroundSyncScheduler {
    var requestIndexingCalls: Int = 0
        private set

    override fun requestIndexing() {
        requestIndexingCalls++
    }
}
