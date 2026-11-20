package com.o3.vanguard.visionary.data

import com.o3.vanguard.visionary.core.common.AppError
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.IndexProgress
import com.o3.vanguard.visionary.domain.model.IndexState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex

/**
 * Runs one indexing pass at a time and makes sure the published state never stays `RUNNING`
 * after the pass ends.
 *
 * - A call while a pass is already running returns `Ok` at once instead of queueing a second
 *   full scan (the foreground service, onboarding and settings can all trigger a run).
 * - When the pass ends in any way (normal return, error, cancellation) while the state is still
 *   `RUNNING`, it is set back to `IDLE`.
 * - Exceptions become `AppResult.Err`; `CancellationException` is rethrown.
 */
internal class IndexRunGuard(private val progress: MutableStateFlow<IndexProgress>) {
    private val mutex = Mutex()

    suspend fun run(pass: suspend () -> AppResult<Unit>): AppResult<Unit> {
        if (!mutex.tryLock()) return AppResult.Ok(Unit)
        return try {
            pass()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Err(AppError.Unknown(e::class.simpleName ?: "IndexingFailed"))
        } finally {
            if (progress.value.state == IndexState.RUNNING) {
                progress.value = progress.value.copy(state = IndexState.IDLE)
            }
            mutex.unlock()
        }
    }
}
