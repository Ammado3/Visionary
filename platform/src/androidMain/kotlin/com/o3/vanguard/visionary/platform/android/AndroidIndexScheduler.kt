package com.o3.vanguard.visionary.platform.android

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.o3.vanguard.visionary.domain.port.BackgroundSyncScheduler

/** Runs indexing in [IndexingForegroundService], so Android doesn't kill a long first index. */
class AndroidIndexScheduler(private val context: Context) : BackgroundSyncScheduler {
    override fun requestIndexing() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, IndexingForegroundService::class.java),
        )
    }
}
