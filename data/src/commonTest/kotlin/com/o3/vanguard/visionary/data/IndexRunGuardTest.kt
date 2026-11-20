package com.o3.vanguard.visionary.data

import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.IndexProgress
import com.o3.vanguard.visionary.domain.model.IndexState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IndexRunGuardTest {

    private val progress = MutableStateFlow(IndexProgress(0, 0, 0, 0, IndexState.IDLE))
    private val guard = IndexRunGuard(progress)

    private fun markRunning() {
        progress.value = IndexProgress(total = 10, indexed = 3, pending = 7, failed = 0, state = IndexState.RUNNING)
    }

    @Test
    fun run_passReturnsWhileRunning_stateEndsIdle() = runTest {
        val result = guard.run {
            markRunning()
            AppResult.Ok(Unit)
        }

        assertEquals(AppResult.Ok(Unit), result)
        assertEquals(IndexState.IDLE, progress.value.state)
        assertEquals(3, progress.value.indexed)
    }

    @Test
    fun run_passThrows_returnsErrAndStateEndsIdle() = runTest {
        val result = guard.run {
            markRunning()
            throw IllegalStateException("database closed")
        }

        assertIs<AppResult.Err>(result)
        assertEquals(IndexState.IDLE, progress.value.state)
    }

    @Test
    fun run_passCancelled_rethrowsAndStateEndsIdleAndLockIsReleased() = runTest {
        val started = CompletableDeferred<Unit>()
        val job = launch {
            guard.run {
                markRunning()
                started.complete(Unit)
                awaitCancellation()
            }
        }
        started.await()
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(IndexState.IDLE, progress.value.state)

        var secondPassRan = false
        guard.run {
            secondPassRan = true
            AppResult.Ok(Unit)
        }
        assertTrue(secondPassRan)
    }

    @Test
    fun run_callWhileAPassIsRunning_isSkipped() = runTest {
        val release = CompletableDeferred<Unit>()
        var passes = 0
        val first = async {
            guard.run {
                passes++
                markRunning()
                release.await()
                AppResult.Ok(Unit)
            }
        }
        runCurrent()

        val second = guard.run {
            passes++
            AppResult.Ok(Unit)
        }

        assertEquals(AppResult.Ok(Unit), second)
        assertEquals(1, passes)
        assertEquals(IndexState.RUNNING, progress.value.state)

        release.complete(Unit)
        assertEquals(AppResult.Ok(Unit), first.await())
        assertEquals(IndexState.IDLE, progress.value.state)
    }

    @Test
    fun run_passEndsPaused_pausedStateIsKept() = runTest {
        guard.run {
            progress.value = progress.value.copy(state = IndexState.PAUSED_BATTERY)
            AppResult.Ok(Unit)
        }

        assertEquals(IndexState.PAUSED_BATTERY, progress.value.state)
    }

    @Test
    fun run_cancellationExceptionFromPass_isNotTurnedIntoErr() = runTest {
        var caught: Throwable? = null
        try {
            guard.run { throw CancellationException("stop") }
        } catch (e: CancellationException) {
            caught = e
        }
        assertIs<CancellationException>(caught)
    }
}
