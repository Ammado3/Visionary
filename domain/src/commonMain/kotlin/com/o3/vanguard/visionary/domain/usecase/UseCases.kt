package com.o3.vanguard.visionary.domain.usecase

import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.*
import com.o3.vanguard.visionary.domain.port.MediaDeleteGateway
import com.o3.vanguard.visionary.domain.port.ScannerPort
import com.o3.vanguard.visionary.domain.port.StorageAlbumSourceEngine
import com.o3.vanguard.visionary.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class ObserveCollectionsUseCase(private val mediaRepository: MediaRepository) {
    operator fun invoke(): Flow<List<CollectionSummary>> = mediaRepository.observeCollections()
}

class ObserveRecentFindsUseCase(private val mediaRepository: MediaRepository) {
    operator fun invoke(limit: Int): Flow<List<MediaItem>> = mediaRepository.observeRecentFinds(limit)
}

class ObserveIndexProgressUseCase(private val indexRepository: IndexRepository) {
    operator fun invoke(): Flow<IndexProgress> = indexRepository.observeProgress()
}

class RebuildIndexUseCase(private val indexRepository: IndexRepository) {
    suspend operator fun invoke(): AppResult<Unit> = indexRepository.rebuild()
}

class ObserveMediaUseCase(private val mediaRepository: MediaRepository) {
    operator fun invoke(kind: CollectionKind): Flow<List<MediaItem>> = mediaRepository.observeMedia(kind)
}

class ObserveItemUseCase(private val mediaRepository: MediaRepository) {
    operator fun invoke(id: MediaId): Flow<MediaItem?> = mediaRepository.observeItem(id)
}

class ObserveStorageAlbumsUseCase(private val source: StorageAlbumSourceEngine) {
    suspend operator fun invoke(): AppResult<List<StorageAlbum>> = source.loadAlbums()
}

class SearchMediaUseCase(private val searchRepository: SearchRepository) {
    operator fun invoke(query: String, filter: SearchFilter, mode: SearchMode = SearchMode.FUZZY): Flow<List<SearchHit>> =
        if (query.isBlank()) flowOf(emptyList()) else searchRepository.search(query.trim(), filter, mode)
}

enum class StartDestination { ONBOARDING, HOME }

class ResolveStartRouteUseCase(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(): StartDestination {
        val settings = settingsRepository.observe().first()
        return if (settings.onboardingDone) StartDestination.HOME else StartDestination.ONBOARDING
    }
}

class MoveToVaultUseCase(private val vaultRepository: VaultRepository) {
    suspend operator fun invoke(id: MediaId): AppResult<Unit> = vaultRepository.moveToVault(id)
}

class DeleteMediaUseCase(
    private val mediaDeleteGateway: MediaDeleteGateway,
    private val mediaRepository: MediaRepository,
    private val trashRepository: TrashRepository,
) {
    suspend operator fun invoke(item: MediaItem): DeleteConsentResult {
        val result = mediaDeleteGateway.requestDelete(listOf(item.uri))
        try {
            trashRepository.moveToTrash(item)
            mediaRepository.deleteMedia(item.id)
        } catch (e: Exception) {
            // ignore
        }
        return result
    }
}

class StartScanUseCase(private val scannerPort: ScannerPort) {
    suspend operator fun invoke(): AppResult<ScannedDocument> = scannerPort.scan()
}

class SmartMediaClassifier {
    fun classify(text: String, isScreenshot: Boolean): MediaCategory {
        val lower = text.lowercase()
        return when {
            lower.contains("total") || lower.contains("receipt") || lower.contains("amount due") -> MediaCategory.RECEIPT
            lower.contains("invoice") || lower.contains("document") || lower.contains("statement") -> MediaCategory.DOCUMENT
            isScreenshot -> MediaCategory.SCREENSHOT
            else -> MediaCategory.PHOTO
        }
    }
}

class EntityExtractor {
    fun extract(text: String): List<SmartEntity> {
        val entities = mutableListOf<SmartEntity>()
        if (text.contains(Regex("""\b\d{3}[-.\s]?\d{3}[-.\s]?\d{4}\b"""))) {
            entities.add(SmartEntity(SmartEntityType.PHONE, text, "Phone"))
        }
        if (text.contains(Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b"""))) {
            entities.add(SmartEntity(SmartEntityType.EMAIL, text, "Email"))
        }
        return entities
    }
}
