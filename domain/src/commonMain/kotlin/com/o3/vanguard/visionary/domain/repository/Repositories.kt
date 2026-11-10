package com.o3.vanguard.visionary.domain.repository

import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MediaRepository {
    fun observeCollections(): Flow<List<CollectionSummary>>
    fun observeMedia(kind: CollectionKind): Flow<List<MediaItem>>
    fun observeItem(id: MediaId): Flow<MediaItem?>
    fun observeOcrLines(id: MediaId): Flow<List<OcrTextLine>>
    fun observeRecentFinds(limit: Int): Flow<List<MediaItem>>
    suspend fun deleteMedia(id: MediaId)
}

interface SearchRepository {
    fun search(query: String, filter: SearchFilter, mode: SearchMode): Flow<List<SearchHit>>
    fun observeRecentSearches(): Flow<List<String>>
    suspend fun saveRecentSearch(query: String)
}

interface IndexRepository {
    fun observeProgress(): Flow<IndexProgress>
    suspend fun retryFailed(): AppResult<Unit>
    suspend fun rebuild(): AppResult<Unit>
}

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

interface VaultRepository {
    val isUnlocked: StateFlow<Boolean>
    fun observeItems(): Flow<List<VaultItem>>
    suspend fun unlock(): AppResult<Unit>
    fun lock()
    suspend fun moveToVault(id: MediaId): AppResult<Unit>
    suspend fun restore(itemId: String): AppResult<Unit>
    suspend fun delete(itemId: String): AppResult<Unit>
}

interface TrashRepository {
    fun observeTrashItems(): Flow<List<TrashItem>>
    suspend fun moveToTrash(item: MediaItem)
    suspend fun deleteFromTrash(id: MediaId)
}
