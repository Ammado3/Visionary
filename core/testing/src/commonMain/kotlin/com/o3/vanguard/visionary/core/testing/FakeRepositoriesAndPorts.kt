@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.o3.vanguard.visionary.core.testing

import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.*
import com.o3.vanguard.visionary.domain.port.*
import com.o3.vanguard.visionary.domain.repository.*
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.koin.dsl.module
import kotlin.time.Clock

class FakeFailure : RuntimeException("fake failure")

class FakeMediaRepository(private val controls: FakeControls) : MediaRepository {
    private val items = MutableStateFlow(FakeData.media)

    private fun <T> scenarioFlow(empty: T, content: Flow<T>): Flow<T> =
        controls.scenario.flatMapLatest { scenario ->
            when (scenario) {
                FakeScenario.Normal -> content.onStart { delay(100) }
                FakeScenario.Empty -> flowOf(empty)
                FakeScenario.Error -> flow { throw FakeFailure() }
                FakeScenario.Loading -> flow { awaitCancellation() }
            }
        }

    override fun observeMedia(kind: CollectionKind): Flow<List<MediaItem>> =
        scenarioFlow(emptyList(), items.map { list -> list.filter { matchesKind(it, kind) } })

    override fun observeMediaByAlbum(albumId: String): Flow<List<MediaItem>> =
        scenarioFlow(emptyList(), items.map { list -> list.filter { it.albumId == albumId } })

    override fun observeFavorites(): Flow<List<MediaItem>> =
        scenarioFlow(emptyList(), items.map { list -> list.filter { it.isFavorite } })

    override fun observeItem(id: MediaId): Flow<MediaItem?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override fun observeOcrLines(id: MediaId): Flow<List<OcrTextLine>> =
        flowOf(listOf(OcrTextLine("Sample line text", NormalizedRect(0f, 0f, 1f, 0.1f), 0.99f)))

    override suspend fun deleteMedia(id: MediaId) {
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun setFavorite(id: MediaId, isFavorite: Boolean) {
        items.update { list -> list.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it } }
    }

    private fun matchesKind(item: MediaItem, kind: CollectionKind): Boolean = when (kind) {
        CollectionKind.ALL -> true
        CollectionKind.SCREENSHOTS -> item.category == MediaCategory.SCREENSHOT
        CollectionKind.DOCUMENTS -> item.category == MediaCategory.DOCUMENT
        CollectionKind.RECEIPTS -> item.category == MediaCategory.RECEIPT
        CollectionKind.SCANS -> item.origin == MediaOrigin.APP_SCAN
    }
}

class FakeSearchRepository : SearchRepository {
    private val recentSearches = MutableStateFlow(listOf("wifi password", "receipt total", "invoice"))

    override fun search(query: String, filter: SearchFilter, mode: SearchMode): Flow<List<SearchHit>> =
        flowOf(
            FakeData.media
                .filter { it.textPreview?.contains(query, ignoreCase = true) == true || query.isBlank() }
                .map { SearchHit(it, it.textPreview ?: "Matched snippet", listOf(query)) }
        )

    override fun observeRecentSearches(): Flow<List<String>> = recentSearches

    override suspend fun saveRecentSearch(query: String) {
        recentSearches.update { (listOf(query) + it).distinct() }
    }
}

class FakeIndexRepository : IndexRepository {
    private val progress = MutableStateFlow(IndexProgress(60, 60, 0, 0, IndexState.IDLE))

    override fun observeProgress(): Flow<IndexProgress> = progress

    override suspend fun retryFailed(): AppResult<Unit> = AppResult.Ok(Unit)

    override suspend fun rebuild(): AppResult<Unit> = AppResult.Ok(Unit)

    override suspend fun pause(): AppResult<Unit> {
        progress.update { it.copy(state = IndexState.PAUSED_BATTERY) }
        return AppResult.Ok(Unit)
    }

    override suspend fun resume(): AppResult<Unit> {
        progress.update { it.copy(state = IndexState.RUNNING) }
        return AppResult.Ok(Unit)
    }
}

class FakeSettingsRepository : SettingsRepository {
    private val settings = MutableStateFlow(AppSettings())

    override fun observe(): Flow<AppSettings> = settings

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settings.update(transform)
    }
}

class FakeVaultRepository : VaultRepository {
    override val isUnlocked = MutableStateFlow(false)
    private val vaultItems = MutableStateFlow(emptyList<VaultItem>())

    override fun observeItems(): Flow<List<VaultItem>> =
        isUnlocked.flatMapLatest { unlocked ->
            if (unlocked) vaultItems else flowOf(emptyList())
        }

    override suspend fun unlock(): AppResult<Unit> {
        delay(300)
        isUnlocked.value = true
        return AppResult.Ok(Unit)
    }

    override fun lock() {
        isUnlocked.value = false
    }

    override suspend fun moveToVault(id: MediaId): AppResult<Unit> {
        val item = VaultItem("vault_${id.value}", 1_760_000_000_000L, 1080, 1920)
        vaultItems.update { it + item }
        return AppResult.Ok(Unit)
    }

    override suspend fun restore(itemId: String): AppResult<Unit> {
        vaultItems.update { list -> list.filterNot { it.id == itemId } }
        return AppResult.Ok(Unit)
    }

    override suspend fun delete(itemId: String): AppResult<Unit> {
        vaultItems.update { list -> list.filterNot { it.id == itemId } }
        return AppResult.Ok(Unit)
    }
}

class FakePermissionGateway : PermissionGateway {
    private val access = MutableStateFlow<MediaAccess>(MediaAccess.Full)

    override fun observeAccess(): Flow<MediaAccess> = access

    override suspend fun requestAccess(): MediaAccess = MediaAccess.Full
}

class FakeBiometricAuthenticator : BiometricAuthenticator {
    override suspend fun authenticate(): AppResult<Unit> = AppResult.Ok(Unit)
}

class FakeTrashRepository : TrashRepository {
    private val trashItems = MutableStateFlow<List<TrashItem>>(emptyList())
    override fun observeTrashItems(): Flow<List<TrashItem>> = trashItems
    override suspend fun moveToTrash(item: MediaItem) {
        trashItems.value = trashItems.value + TrashItem(item.id, item.uri, item.category, item.fileName, Clock.System.now().toEpochMilliseconds())
    }
    override suspend fun deleteFromTrash(id: MediaId) {
        trashItems.value = trashItems.value.filterNot { it.id == id }
    }
    override suspend fun restoreFromTrash(id: MediaId) {
        trashItems.value = trashItems.value.filterNot { it.id == id }
    }
}

val fakeDataModule = module {
    single { FakeControls() }
    single<MediaRepository> { FakeMediaRepository(get()) }
    single<SearchRepository> { FakeSearchRepository() }
    single<IndexRepository> { FakeIndexRepository() }
    single<SettingsRepository> { FakeSettingsRepository() }
    single<VaultRepository> { FakeVaultRepository() }
    single<PermissionGateway> { FakePermissionGateway() }
    single<BiometricAuthenticator> { FakeBiometricAuthenticator() }
    single<TrashRepository> { FakeTrashRepository() }
}
