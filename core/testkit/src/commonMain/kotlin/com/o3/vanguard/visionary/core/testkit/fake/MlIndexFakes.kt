package com.o3.vanguard.visionary.core.testkit.fake

import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.IndexProgress
import com.o3.vanguard.visionary.domain.model.IndexState
import com.o3.vanguard.visionary.domain.model.OcrResult
import com.o3.vanguard.visionary.domain.model.ScannedDocument
import com.o3.vanguard.visionary.domain.port.ImageLabelEngine
import com.o3.vanguard.visionary.domain.port.OcrEngine
import com.o3.vanguard.visionary.domain.port.ScannerPort
import com.o3.vanguard.visionary.domain.repository.IndexRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeOcrEngine : OcrEngine {
    val recognizeCalls = mutableListOf<String>()
    var nextResult: AppResult<OcrResult> = AppResult.Ok(OcrResult(fullText = "", lines = emptyList()))

    override suspend fun recognizeText(uri: String): AppResult<OcrResult> {
        recognizeCalls.add(uri)
        return nextResult
    }
}

class FakeImageLabelEngine : ImageLabelEngine {
    val labelCalls = mutableListOf<String>()
    var nextResult: AppResult<List<String>> = AppResult.Ok(emptyList())

    override suspend fun labelImage(uri: String): AppResult<List<String>> {
        labelCalls.add(uri)
        return nextResult
    }
}

class FakeIndexRepository(
    initialProgress: IndexProgress = IndexProgress(
        total = 0,
        indexed = 0,
        pending = 0,
        failed = 0,
        state = IndexState.IDLE,
    ),
) : IndexRepository {
    val progress = MutableStateFlow(initialProgress)
    var retryFailedCalls = 0
    var rebuildCalls = 0
    var nextRetryFailedResult: AppResult<Unit> = AppResult.Ok(Unit)
    var nextRebuildResult: AppResult<Unit> = AppResult.Ok(Unit)

    override fun observeProgress(): Flow<IndexProgress> = progress

    override suspend fun retryFailed(): AppResult<Unit> {
        retryFailedCalls++
        return nextRetryFailedResult
    }

    override suspend fun rebuild(): AppResult<Unit> {
        rebuildCalls++
        return nextRebuildResult
    }
}

class FakeScannerPort : ScannerPort {
    var available = true
    var scanCalls = 0
    var nextScanResult: AppResult<ScannedDocument> = AppResult.Ok(ScannedDocument(emptyList(), null))

    override suspend fun isAvailable(): Boolean = available

    override suspend fun scan(): AppResult<ScannedDocument> {
        scanCalls++
        return nextScanResult
    }
}
