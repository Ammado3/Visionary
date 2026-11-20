package com.o3.vanguard.visionary.data

import com.o3.vanguard.visionary.core.common.AppError
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.IndexProgress
import com.o3.vanguard.visionary.domain.model.IndexState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Runs one indexing pass at a time and makes sure the published state never stays `RUNNING`
 * after a pass ends.
 *
 * - A call while a pass is running returns `Ok` at once and asks for a re-run. When the pass ends
 *   normally, the guard runs one more pass, however many calls came in. A pass that ends with an
 *   error or a cancellation drops the request.
 * - When a pass ends in any way while the state is still `RUNNING`, it is set back to `IDLE`.
 * - Exceptions become `AppResult.Err`; `CancellationException` is rethrown.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class IndexRunGuard(private val progress: MutableStateFlow<IndexProgress>) {
    /** [IDLE], [RUNNING], or [RERUN_REQUESTED] (running, with another pass wanted). */
    private val state = AtomicInt(IDLE)

    suspend fun run(pass: suspend () -> AppResult<Unit>): AppResult<Unit> {
        if (!claimOrRequestRerun()) return AppResult.Ok(Unit)
        try {
            while (true) {
                val result = runOnce(pass)
                if (result !is AppResult.Ok) {
                    state.store(IDLE)
                    return result
                }
                if (!rerunOrRelease()) return result
            }
        } catch (e: CancellationException) {
            state.store(IDLE)
            throw e
        }
    }

    /**
     * After a successful pass: `true` to run again (a re-run was requested), or `false` after
     * releasing the guard. Both are compare-and-set, so a request that arrives at this moment is
     * never lost: it either turns into the re-run or finds the guard idle and runs itself.
     */
    private fun rerunOrRelease(): Boolean {
        while (true) {
            if (state.compareAndSet(RERUN_REQUESTED, RUNNING)) return true
            if (state.compareAndSet(RUNNING, IDLE)) return false
        }
    }

    /** `true` if this caller now owns the run; `false` if a pass is running (a re-run is now requested). */
    private fun claimOrRequestRerun(): Boolean {
        while (true) {
            when (state.load()) {
                IDLE -> if (state.compareAndSet(IDLE, RUNNING)) return true
                RUNNING -> if (state.compareAndSet(RUNNING, RERUN_REQUESTED)) return false
                else -> return false
            }
        }
    }

    private suspend fun runOnce(pass: suspend () -> AppResult<Unit>): AppResult<Unit> =
        try {
            pass()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Err(AppError.Unknown(e::class.simpleName ?: "IndexingFailed"))
        } finally {
            if (progress.value.state == IndexState.RUNNING) {
                progress.value = progress.value.copy(state = IndexState.IDLE)
            }
        }

    private companion object {
        const val IDLE = 0
        const val RUNNING = 1
        const val RERUN_REQUESTED = 2
    }
}
