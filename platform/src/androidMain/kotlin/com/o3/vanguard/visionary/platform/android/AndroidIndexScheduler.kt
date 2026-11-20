package com.o3.vanguard.visionary.platform.android

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.o3.vanguard.visionary.domain.port.BackgroundSyncScheduler
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs indexing in [IndexingForegroundService], so Android doesn't kill a long first index.
 *
 * Android 12+ throws `ForegroundServiceStartNotAllowedException` when a foreground service is
 * started from the background. So a request made while the app is in the background is kept,
 * and the service starts the next time the app comes to the foreground.
 *
 * [requestIndexing] may be called from any thread; the work happens on the main thread.
 */
class AndroidIndexScheduler(private val context: Context) : BackgroundSyncScheduler {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingRequest = AtomicBoolean(false)
    private val observerRegistered = AtomicBoolean(false)

    override fun requestIndexing() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            requestOnMainThread()
        } else {
            mainHandler.post { requestOnMainThread() }
        }
    }

    @MainThread
    private fun requestOnMainThread() {
        registerForegroundObserverOnce()
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            startService()
        } else {
            defer()
        }
    }

    @MainThread
    private fun registerForegroundObserverOnce() {
        if (!observerRegistered.compareAndSet(false, true)) return
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    if (pendingRequest.compareAndSet(true, false)) startService()
                }
            },
        )
    }

    @MainThread
    private fun startService() {
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, IndexingForegroundService::class.java),
            )
        } catch (_: IllegalStateException) {
            // Includes ForegroundServiceStartNotAllowedException (Android 12+): try again on the
            // next foreground start.
            defer()
        }
    }

    private fun defer() {
        pendingRequest.set(true)
        Log.i(TAG, "Indexing start deferred until the app is in the foreground")
    }

    private companion object {
        const val TAG = "AndroidIndexScheduler"
    }
}
