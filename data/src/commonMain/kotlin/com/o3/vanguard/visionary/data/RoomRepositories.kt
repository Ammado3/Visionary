package com.o3.vanguard.visionary.data

import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.core.database.AppDatabase
import com.o3.vanguard.visionary.core.database.entity.MediaEntity
import com.o3.vanguard.visionary.domain.model.*
import com.o3.vanguard.visionary.domain.repository.*
import kotlinx.coroutines.flow.*

class RoomMediaRepository(private val db: AppDatabase) : MediaRepository {
    override fun observeCollections(): Flow<List<CollectionSummary>> =
        db.mediaDao().observeCategoryCounts().map { counts ->
            val countMap = counts.associate { it.category to it.count }
            CollectionKind.entries.map { kind ->
                CollectionSummary(kind, countMap[kind.name] ?: 0, null)
            }
        }

    override fun observeMedia(kind: CollectionKind): Flow<List<MediaItem>> =
        (if (kind == CollectionKind.ALL) db.mediaDao().observeAll()
        else db.mediaDao().observeByCategory(kind.name))
            .map { list -> list.map { it.toDomain() } }

    override fun observeItem(id: MediaId): Flow<MediaItem?> =
        db.mediaDao().observeById(id.value).map { it?.toDomain() }

    override fun observeOcrLines(id: MediaId): Flow<List<OcrTextLine>> =
        flowOf(emptyList())

    override fun observeRecentFinds(limit: Int): Flow<List<MediaItem>> =
        db.mediaDao().observeAll().map { list -> list.take(limit).map { it.toDomain() } }
}

class RoomSearchRepository(private val db: AppDatabase) : SearchRepository {
    override fun search(query: String, filter: SearchFilter): Flow<List<SearchHit>> =
        flowOf(emptyList())

    override fun observeRecentSearches(): Flow<List<String>> =
        db.searchDao().observeRecentSearches().map { list -> list.map { it.query } }

    override suspend fun saveRecentSearch(query: String) {}
}

class RoomIndexRepository(private val db: AppDatabase) : IndexRepository {
    override fun observeProgress(): Flow<IndexProgress> =
        flowOf(IndexProgress(0, 0, 0, 0, IndexState.IDLE))

    override suspend fun retryFailed(): AppResult<Unit> = AppResult.Ok(Unit)

    override suspend fun rebuild(): AppResult<Unit> = AppResult.Ok(Unit)
}

class RoomSettingsRepository : SettingsRepository {
    private val settings = MutableStateFlow(AppSettings())

    override fun observe(): Flow<AppSettings> = settings

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settings.update(transform)
    }
}

class RoomVaultRepository(private val db: AppDatabase) : VaultRepository {
    override val isUnlocked = MutableStateFlow(false)

    override fun observeItems(): Flow<List<VaultItem>> =
        db.vaultDao().observeVaultItems().map { list ->
            list.map { VaultItem(it.id, it.createdAtMs, it.width, it.height) }
        }

    override suspend fun unlock(): AppResult<Unit> {
        isUnlocked.value = true
        return AppResult.Ok(Unit)
    }

    override fun lock() {
        isUnlocked.value = false
    }

    override suspend fun moveToVault(id: MediaId): AppResult<Unit> = AppResult.Ok(Unit)

    override suspend fun restore(itemId: String): AppResult<Unit> = AppResult.Ok(Unit)

    override suspend fun delete(itemId: String): AppResult<Unit> = AppResult.Ok(Unit)
}

internal fun MediaEntity.toDomain(): MediaItem = MediaItem(
    id = MediaId(id),
    uri = ref,
    origin = runCatching { MediaOrigin.valueOf(origin) }.getOrDefault(MediaOrigin.LIBRARY),
    category = runCatching { MediaCategory.valueOf(category) }.getOrDefault(MediaCategory.PHOTO),
    takenAtMs = sortKey,
    width = width,
    height = height,
    textPreview = null,
    entityFlags = entityFlags
)
