package com.o3.vanguard.visionary.data

import com.o3.vanguard.visionary.core.common.AppError
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
    fun run_callDuringAPass_returnsAtOnceAndCausesExactlyOneExtraPass() = runTest {
        val release = CompletableDeferred<Unit>()
        var passes = 0
        val first = async {
            guard.run {
                passes++
                if (passes == 1) {
                    markRunning()
                    release.await()
                }
                AppResult.Ok(Unit)
            }
        }
        runCurrent()

        val second = guard.run {
            passes++
            AppResult.Ok(Unit)
        }

        // The second call doesn't run its own pass and doesn't wait.
        assertEquals(AppResult.Ok(Unit), second)
        assertEquals(1, passes)
        assertEquals(IndexState.RUNNING, progress.value.state)

        release.complete(Unit)
        assertEquals(AppResult.Ok(Unit), first.await())
        assertEquals(2, passes)
        assertEquals(IndexState.IDLE, progress.value.state)
    }

    @Test
    fun run_threeCallsDuringAPass_stillCauseOneExtraPass() = runTest {
        val release = CompletableDeferred<Unit>()
        var passes = 0
        val pass: suspend () -> AppResult<Unit> = {
            passes++
            if (passes == 1) release.await()
            AppResult.Ok(Unit)
        }
        val first = async { guard.run(pass) }
        runCurrent()

        repeat(3) { guard.run(pass) }
        release.complete(Unit)
        first.await()

        assertEquals(2, passes)
    }

    @Test
    fun run_passCancelledAfterACallCameIn_noExtraPass() = runTest {
        val started = CompletableDeferred<Unit>()
        var passes = 0
        val job = launch {
            guard.run {
                passes++
                started.complete(Unit)
                awaitCancellation()
            }
        }
        started.await()
        guard.run {
            passes++
            AppResult.Ok(Unit)
        }

        job.cancel()
        job.join()
        runCurrent()

        assertEquals(1, passes)
    }

    @Test
    fun run_passFailsAfterACallCameIn_noExtraPass() = runTest {
        val release = CompletableDeferred<Unit>()
        var passes = 0
        val first = async {
            guard.run {
                passes++
                release.await()
                AppResult.Err(AppError.Io("scan failed"))
            }
        }
        runCurrent()
        guard.run {
            passes++
            AppResult.Ok(Unit)
        }

        release.complete(Unit)

        assertIs<AppResult.Err>(first.await())
        assertEquals(1, passes)
    }

    @Test
    fun run_afterTheExtraPass_guardIsFreeAgain() = runTest {
        val release = CompletableDeferred<Unit>()
        var passes = 0
        val first = async {
            guard.run {
                passes++
                if (passes == 1) release.await()
                AppResult.Ok(Unit)
            }
        }
        runCurrent()
        guard.run { AppResult.Ok(Unit) }
        release.complete(Unit)
        first.await()

        var ranAgain = false
        guard.run {
            ranAgain = true
            AppResult.Ok(Unit)
        }
        assertTrue(ranAgain)
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

    @Test
    fun run_successfulPass_raisesCompletedPassesAndLastPassFailedIsFalse() = runTest {
        assertEquals(0, progress.value.completedPasses)

        val result = guard.run {
            markRunning()
            AppResult.Ok(Unit)
        }

        assertEquals(AppResult.Ok(Unit), result)
        assertEquals(1, progress.value.completedPasses)
        assertEquals(false, progress.value.lastPassFailed)
    }

    @Test
    fun run_failedPass_raisesCompletedPassesAndLastPassFailedIsTrue() = runTest {
        assertEquals(0, progress.value.completedPasses)

        val result = guard.run {
            markRunning()
            AppResult.Err(AppError.Unknown("Failed"))
        }

        assertIs<AppResult.Err>(result)
        assertEquals(1, progress.value.completedPasses)
        assertEquals(true, progress.value.lastPassFailed)
    }

    @Test
    fun run_cancelledPass_doesNotChangeCompletedPasses() = runTest {
        assertEquals(0, progress.value.completedPasses)

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

        assertEquals(0, progress.value.completedPasses)
    }

    @Test
    fun withCounts_keepsPassHistoryAndUpdatesCountsAndState() {
        val initial = IndexProgress(
            total = 10,
            indexed = 5,
            pending = 5,
            failed = 0,
            state = IndexState.RUNNING,
            completedPasses = 4,
            lastPassFailed = true,
        )

        val updated = initial.withCounts(
            total = 20,
            indexed = 15,
            pending = 3,
            failed = 2,
            state = IndexState.IDLE,
        )

        assertEquals(20, updated.total)
        assertEquals(15, updated.indexed)
        assertEquals(3, updated.pending)
        assertEquals(2, updated.failed)
        assertEquals(IndexState.IDLE, updated.state)
        assertEquals(4, updated.completedPasses)
        assertEquals(true, updated.lastPassFailed)
    }

    @Test
    fun run_threePassesInARowWithWithCounts_incrementsCompletedPassesTo1Then2Then3() = runTest {
        assertEquals(0, progress.value.completedPasses)

        repeat(3) { passIndex ->
            val result = guard.run {
                progress.value = progress.value.withCounts(
                    total = 10,
                    indexed = 5,
                    pending = 5,
                    failed = 0,
                    state = IndexState.RUNNING,
                )
                progress.value = progress.value.withCounts(
                    total = 10,
                    indexed = 10,
                    pending = 0,
                    failed = 0,
                    state = IndexState.IDLE,
                )
                AppResult.Ok(Unit)
            }
            assertEquals(AppResult.Ok(Unit), result)
            assertEquals(passIndex + 1, progress.value.completedPasses)
            assertEquals(false, progress.value.lastPassFailed)
        }
    }
}
