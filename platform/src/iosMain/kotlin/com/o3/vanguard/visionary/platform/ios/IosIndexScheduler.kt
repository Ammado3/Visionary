package com.o3.vanguard.visionary.platform.ios

import com.o3.vanguard.visionary.domain.port.BackgroundSyncScheduler
import com.o3.vanguard.visionary.domain.repository.IndexRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * iOS has no background indexing service yet, so a pass runs in the app-wide scope.
 * `IndexRepository` already makes overlapping requests safe.
 */
class IosIndexScheduler(
    private val indexRepository: IndexRepository,
    private val appScope: CoroutineScope,
) : BackgroundSyncScheduler {
    override fun requestIndexing() {
        appScope.launch { indexRepository.rebuild() }
    }
}
