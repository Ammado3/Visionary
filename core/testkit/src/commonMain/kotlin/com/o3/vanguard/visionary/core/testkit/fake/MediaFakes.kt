package com.o3.vanguard.visionary.core.testkit.fake

import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.CollectionKind
import com.o3.vanguard.visionary.domain.model.CollectionSummary
import com.o3.vanguard.visionary.domain.model.DeleteConsentResult
import com.o3.vanguard.visionary.domain.model.MediaAccess
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem
import com.o3.vanguard.visionary.domain.model.OcrTextLine
import com.o3.vanguard.visionary.domain.model.StorageAlbum
import com.o3.vanguard.visionary.domain.model.TrashItem
import com.o3.vanguard.visionary.domain.port.MediaDeleteGateway
import com.o3.vanguard.visionary.domain.port.MediaSourceEngine
import com.o3.vanguard.visionary.domain.port.PermissionGateway
import com.o3.vanguard.visionary.domain.port.StorageAlbumSourceEngine
import com.o3.vanguard.visionary.domain.repository.MediaRepository
import com.o3.vanguard.visionary.domain.repository.TrashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMediaRepository : MediaRepository {
    val collections = MutableStateFlow<List<CollectionSummary>>(emptyList())
    val mediaItemsByKind = MutableStateFlow<Map<CollectionKind, List<MediaItem>>>(emptyMap())
    val itemsById = MutableStateFlow<Map<MediaId, MediaItem>>(emptyMap())
    val ocrLinesById = MutableStateFlow<Map<MediaId, List<OcrTextLine>>>(emptyMap())
    val recentFinds = MutableStateFlow<List<MediaItem>>(emptyList())

    override fun observeCollections(): Flow<List<CollectionSummary>> = collections

    override fun observeMedia(kind: CollectionKind): Flow<List<MediaItem>> =
        mediaItemsByKind.map { it[kind] ?: emptyList() }

    override fun observeItem(id: MediaId): Flow<MediaItem?> =
        itemsById.map { it[id] }

    override fun observeOcrLines(id: MediaId): Flow<List<OcrTextLine>> =
        ocrLinesById.map { it[id] ?: emptyList() }

    override fun observeRecentFinds(limit: Int): Flow<List<MediaItem>> =
        recentFinds.map { it.take(limit) }

    val deleteMediaCalls = mutableListOf<MediaId>()
    override suspend fun deleteMedia(id: MediaId) {
        deleteMediaCalls.add(id)
    }
}

class FakePermissionGateway(initialAccess: MediaAccess = MediaAccess.Full) : PermissionGateway {
    val access = MutableStateFlow(initialAccess)
    var requestAccessCalls = 0

    override fun observeAccess(): Flow<MediaAccess> = access

    override suspend fun requestAccess(): MediaAccess {
        requestAccessCalls++
        return access.value
    }
}

class FakeMediaDeleteGateway : MediaDeleteGateway {
    val deleteRequests = mutableListOf<List<String>>()
    var nextResult: DeleteConsentResult = DeleteConsentResult.Deleted

    override suspend fun requestDelete(uris: List<String>): DeleteConsentResult {
        deleteRequests.add(uris)
        return nextResult
    }
}

class FakeStorageAlbumSourceEngine : StorageAlbumSourceEngine {
    var loadAlbumsCalls = 0
    var nextResult: AppResult<List<StorageAlbum>> = AppResult.Ok(emptyList())

    override suspend fun loadAlbums(): AppResult<List<StorageAlbum>> {
        loadAlbumsCalls++
        return nextResult
    }
}

class FakeMediaSourceEngine : MediaSourceEngine {
    val queryDeltaCalls = mutableListOf<Long>()
    var queryAllLibraryIdsCalls = 0
    var nextQueryDeltaResult: AppResult<List<MediaItem>> = AppResult.Ok(emptyList())
    var nextQueryAllLibraryIdsResult: AppResult<Set<Long>> = AppResult.Ok(emptySet())

    override suspend fun queryDelta(sinceCheckpoint: Long): AppResult<List<MediaItem>> {
        queryDeltaCalls.add(sinceCheckpoint)
        return nextQueryDeltaResult
    }

    override suspend fun queryAllLibraryIds(): AppResult<Set<Long>> {
        queryAllLibraryIdsCalls++
        return nextQueryAllLibraryIdsResult
    }
}

class FakeTrashRepository : TrashRepository {
    val trashItems = MutableStateFlow<List<TrashItem>>(emptyList())
    val moveToTrashCalls = mutableListOf<MediaItem>()
    val deleteFromTrashCalls = mutableListOf<MediaId>()
    val restoreFromTrashCalls = mutableListOf<MediaId>()

    override fun observeTrashItems(): Flow<List<TrashItem>> = trashItems

    override suspend fun moveToTrash(item: MediaItem) {
        moveToTrashCalls.add(item)
    }

    override suspend fun deleteFromTrash(id: MediaId) {
        deleteFromTrashCalls.add(id)
    }

    override suspend fun restoreFromTrash(id: MediaId) {
        restoreFromTrashCalls.add(id)
    }
}
