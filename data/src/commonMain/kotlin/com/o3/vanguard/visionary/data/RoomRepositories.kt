package com.o3.vanguard.visionary.data

import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.core.common.AppError
import com.o3.vanguard.visionary.core.common.currentTimeMillis
import com.o3.vanguard.visionary.core.database.entity.VaultItemEntity
import com.o3.vanguard.visionary.domain.port.MediaDeleteGateway
import com.o3.vanguard.visionary.domain.port.VaultCryptoEngine
import com.o3.vanguard.visionary.core.common.VectorSearchEngine
import com.o3.vanguard.visionary.core.common.toByteArray
import com.o3.vanguard.visionary.core.common.toFloatArray
import com.o3.vanguard.visionary.core.database.AppDatabase
import com.o3.vanguard.visionary.core.database.entity.IndexExclusionEntity
import com.o3.vanguard.visionary.core.database.entity.MediaEntity
import com.o3.vanguard.visionary.core.database.entity.MediaTextEntity
import com.o3.vanguard.visionary.core.database.entity.TrashItemEntity
import com.o3.vanguard.visionary.core.database.entity.AppSettingsEntity
import com.o3.vanguard.visionary.domain.model.*
import com.o3.vanguard.visionary.domain.port.ImageLabelEngine
import com.o3.vanguard.visionary.domain.port.MediaSourceEngine
import com.o3.vanguard.visionary.domain.port.OcrEngine
import com.o3.vanguard.visionary.domain.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlin.math.min
import kotlin.time.Clock

class RoomMediaRepository(private val db: AppDatabase) : MediaRepository {
    override fun observeMedia(kind: CollectionKind): Flow<List<MediaItem>> =
        (if (kind == CollectionKind.ALL) db.mediaDao().observeAll()
        else db.mediaDao().observeByCategories(kind.categories().map { it.name }))
            .map { list ->
                val textByMediaId = if (list.isEmpty()) {
                    emptyMap()
                } else {
                    db.mediaDao()
                        .getTexts(list.map { it.id })
                        .associateBy { it.mediaId }
                }
                list.map { entity ->
                    entity.toDomain(textByMediaId[entity.id])
                }
            }

    override fun observeMediaByAlbum(albumId: String): Flow<List<MediaItem>> =
        db.mediaDao().observeByAlbum(albumId).map { list ->
            val textByMediaId = if (list.isEmpty()) {
                emptyMap()
            } else {
                db.mediaDao().getTexts(list.map { it.id }).associateBy { it.mediaId }
            }
            list.map { entity -> entity.toDomain(textByMediaId[entity.id]) }
        }

    override fun observeFavorites(): Flow<List<MediaItem>> =
        db.mediaDao().observeFavorites().map { list ->
            val textByMediaId = db.mediaDao().getTexts(list.map { it.id }).associateBy { it.mediaId }
            list.map { entity -> entity.toDomain(textByMediaId[entity.id]) }
        }

    override fun observeItem(id: MediaId): Flow<MediaItem?> =
        db.mediaDao().observeById(id.value).map { entity ->
            entity?.let {
                val textEntity = db.mediaDao().getText(it.id)
                it.toDomain(textEntity)
            }
        }

    override fun observeOcrLines(id: MediaId): Flow<List<OcrTextLine>> =
        flowOf(emptyList())

    override suspend fun deleteMedia(id: MediaId) {
        db.mediaDao().delete(listOf(id.value))
    }

    override suspend fun setFavorite(id: MediaId, isFavorite: Boolean) {
        db.mediaDao().updateFavorite(id.value, isFavorite)
    }
}

internal fun matchesSearchFilter(category: String, entityFlags: Int, filter: SearchFilter): Boolean {
    val allowedCategories = filter.categories()?.map { it.name }?.toSet()
    val requiredFlags = filter.requiredEntityFlags()
    return (allowedCategories == null || category in allowedCategories) &&
        (requiredFlags == 0 || (entityFlags and requiredFlags) != 0)
}

class RoomSearchRepository(
    private val db: AppDatabase,
    private val vectorSearchEngine: VectorSearchEngine,
) : SearchRepository {

    override fun search(query: String, filter: SearchFilter, mode: SearchMode): Flow<List<SearchHit>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val trimmedQuery = query.trim()
        val queryWords = trimmedQuery.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }

        val filteredEntities = db.mediaDao().getAll().filter { entity ->
            matchesSearchFilter(entity.category, entity.entityFlags, filter)
        }

        if (filteredEntities.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val hits = if (mode == SearchMode.FUZZY) {
            val queryEmbedding = vectorSearchEngine.generateQueryEmbedding(trimmedQuery)

            filteredEntities.mapNotNull { entity ->
                val textEntity = db.mediaDao().getText(entity.id)
                val domainItem = entity.toDomain(textEntity)

                val textScore = calculateTextScore(domainItem, queryWords, mode)
                val semanticScore = textEntity?.embedding?.let { bytes ->
                    vectorSearchEngine.cosineSimilarity(queryEmbedding, bytes.toFloatArray())
                } ?: 0f

                val combinedScore = (textScore * 0.6f) + (semanticScore * 0.4f)

                if (combinedScore > 0.1f) {
                    SearchHit(
                        item = domainItem,
                        snippet = buildSnippet(domainItem.textPreview.orEmpty(), trimmedQuery),
                        matchedTerms = queryWords.filter { word ->
                            domainItem.textPreview.orEmpty().contains(word, ignoreCase = true) ||
                                    domainItem.labels.contains(word, ignoreCase = true) ||
                                    domainItem.uri.contains(word, ignoreCase = true)
                        },
                        relevanceScore = combinedScore
                    )
                } else null
            }.sortedByDescending { it.relevanceScore }
        } else {
            filteredEntities.mapNotNull { entity ->
                val textEntity = db.mediaDao().getText(entity.id)
                val domainItem = entity.toDomain(textEntity)

                val textScore = calculateTextScore(domainItem, queryWords, mode)
                if (textScore > 0f) {
                    SearchHit(
                        item = domainItem,
                        snippet = buildSnippet(domainItem.textPreview.orEmpty(), trimmedQuery),
                        matchedTerms = queryWords,
                        relevanceScore = textScore
                    )
                } else null
            }.sortedByDescending { it.relevanceScore }
        }

        emit(hits.take(100))
    }

    private fun calculateTextScore(
        item: MediaItem,
        queryWords: List<String>,
        mode: SearchMode
    ): Float {
        if (queryWords.isEmpty()) return 0f

        val previewText = item.textPreview
        val searchTokens = buildList {
            if (item.labels.isNotBlank()) {
                addAll(item.labels.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() })
            }
            if (!previewText.isNullOrBlank()) {
                addAll(previewText.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() })
            }
            if (item.fileName.isNotBlank()) {
                addAll(item.fileName.lowercase().split(Regex("\\s+|[._-]")).filter { it.isNotBlank() })
            } else {
                val name = item.uri.substringAfterLast("/")
                addAll(name.lowercase().split(Regex("\\s+|[._-]")).filter { it.isNotBlank() })
            }
        }.distinct()

        if (searchTokens.isEmpty()) return 0f

        var totalScore = 0f
        var matchCount = 0

        for (queryWord in queryWords) {
            var bestMatchScore = 0f

            for (token in searchTokens) {
                val score = when (mode) {
                    SearchMode.EXACT -> if (token == queryWord) 1.0f else 0f
                    SearchMode.PREFIX -> {
                        when {
                            token == queryWord -> 1.0f
                            token.startsWith(queryWord) && queryWord.length >= 2 -> {
                                val matchRatio = queryWord.length.toFloat() / token.length.toFloat()
                                0.7f + (matchRatio * 0.3f)
                            }
                            queryWord.startsWith(token) && token.length >= 2 -> 0.8f
                            else -> 0f
                        }
                    }
                    SearchMode.CONTAINS -> {
                        when {
                            token == queryWord -> 1.0f
                            token.contains(queryWord) -> 0.7f
                            queryWord.contains(token) && token.length >= 2 -> 0.5f
                            else -> 0f
                        }
                    }
                    SearchMode.FUZZY -> {
                        when {
                            token == queryWord -> 1.0f
                            token.startsWith(queryWord) && queryWord.length >= 2 -> {
                                val matchRatio = queryWord.length.toFloat() / token.length.toFloat()
                                0.7f + (matchRatio * 0.2f)
                            }
                            queryWord.startsWith(token) && token.length >= 2 -> 0.6f
                            token.contains(queryWord) -> 0.8f
                            calculateStringSimilarity(token, queryWord) > 0.7f -> 0.4f
                            else -> 0f
                        }
                    }
                }

                if (score > bestMatchScore) {
                    bestMatchScore = score
                }
            }

            if (bestMatchScore > 0f) {
                totalScore += bestMatchScore
                matchCount++
            }
        }

        return if (matchCount > 0) totalScore / queryWords.size else 0f
    }

    private fun calculateStringSimilarity(s1: String, s2: String): Float {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        if (longer.isEmpty()) return 1.0f
        val editDistance = calculateEditDistance(longer, shorter)
        return (longer.length - editDistance) / longer.length.toFloat()
    }

    private fun calculateEditDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + if (s1[i - 1] == s2[j - 1]) 0 else 1
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    override fun observeRecentSearches(): Flow<List<String>> =
        db.searchDao().observeRecentSearches().map { list -> list.map { it.query } }

    override suspend fun saveRecentSearch(query: String) {
        db.searchDao().upsertRecentSearch(
            com.o3.vanguard.visionary.core.database.entity.RecentSearchEntity(query, kotlin.time.Clock.System.now().toEpochMilliseconds())
        )
    }
}

class RoomIndexRepository(
    private val db: AppDatabase,
    private val mediaSource: MediaSourceEngine,
    private val ocrEngine: OcrEngine,
    private val labelEngine: ImageLabelEngine,
    private val vectorSearchEngine: VectorSearchEngine,
    private val settingsRepository: SettingsRepository,
) : IndexRepository {
    private val progress = MutableStateFlow(IndexProgress(0, 0, 0, 0, IndexState.IDLE))

    /** One pass at a time; a call during a pass is skipped, and the state never stays RUNNING. */
    private val runGuard = IndexRunGuard(progress)
    private var isPaused = false

    override fun observeProgress(): Flow<IndexProgress> = flow {
        if (progress.value.total == 0) {
            val allEntities = db.mediaDao().getAll()
            val textEntities = db.mediaDao().getAllTextEntities()
            val totalCount = allEntities.size
            val indexedCount = textEntities.size
            if (totalCount > 0 && progress.value.total == 0) {
                val initialState = if (isPaused) IndexState.PAUSED_BATTERY else IndexState.IDLE
                progress.value = progress.value.withCounts(
                    total = totalCount,
                    indexed = indexedCount,
                    pending = (totalCount - indexedCount).coerceAtLeast(0),
                    failed = 0,
                    state = initialState,
                )
            }
        }
        emitAll(progress)
    }

    override suspend fun pause(): AppResult<Unit> {
        isPaused = true
        progress.value = progress.value.copy(state = IndexState.PAUSED_BATTERY)
        return AppResult.Ok(Unit)
    }

    override suspend fun resume(): AppResult<Unit> {
        isPaused = false
        if (progress.value.state == IndexState.PAUSED_BATTERY) {
            progress.value = progress.value.copy(state = IndexState.IDLE)
        }
        return AppResult.Ok(Unit)
    }

    // TODO: route through BackgroundSyncScheduler if a UI caller is added
    override suspend fun retryFailed(): AppResult<Unit> {
        isPaused = false
        return rebuild()
    }

    override suspend fun rebuild(): AppResult<Unit> = runGuard.run { indexPass() }

    private suspend fun indexPass(): AppResult<Unit> {
        val settings = settingsRepository.observe().first()
        if (!settings.visualAnalysisEnabled || isPaused) {
            isPaused = true
            progress.value = progress.value.copy(state = IndexState.PAUSED_BATTERY)
            return AppResult.Ok(Unit)
        }
        val source = mediaSource.queryDelta(0L)
        if (source !is AppResult.Ok) return source as AppResult.Err
        val excludedRefs = db.exclusionDao().getAllExclusions().toSet()
        val media = source.value.filter { it.uri !in excludedRefs }

        val currentRefs = media.map { it.uri }.toSet()
        if (currentRefs.isNotEmpty()) {
            db.mediaDao().deleteNonExistingImages(currentRefs.toList())
        }

        // Upsert all media items with conflict fallback so no item is skipped
        media.forEach { item ->
            val existingFavorite = db.mediaDao().getById(item.id.value)?.isFavorite ?: item.isFavorite
            val entity = item.copy(isFavorite = existingFavorite).toEntity()
            try {
                db.mediaDao().upsert(listOf(entity))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                db.mediaDao().deleteByRef(entity.ref)
                try {
                    db.mediaDao().upsert(listOf(entity))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Skip this item; the next pass retries it.
                }
            }
        }

        val initialIndexed = media.count { item ->
            item.category == MediaCategory.VIDEO || db.mediaDao().getText(item.id.value) != null
        }
        progress.value = progress.value.withCounts(media.size, initialIndexed, media.size - initialIndexed, 0, IndexState.RUNNING)

        var indexed = 0
        var failed = 0
        for (item in media) {
            if (isPaused || !settingsRepository.observe().first().visualAnalysisEnabled) {
                isPaused = true
                progress.value = progress.value.withCounts(media.size, indexed, media.size - indexed - failed, failed, IndexState.PAUSED_BATTERY)
                return AppResult.Ok(Unit)
            }
            try {
                // Resume where it left off: skip if already indexed in database
                if (item.category != MediaCategory.VIDEO && db.mediaDao().getText(item.id.value) != null) {
                    indexed++
                    progress.value = progress.value.withCounts(media.size, indexed, media.size - indexed - failed, failed, IndexState.RUNNING)
                    continue
                }

                if (item.category == MediaCategory.VIDEO) {
                    indexed++
                    progress.value = progress.value.withCounts(media.size, indexed, media.size - indexed - failed, failed, IndexState.RUNNING)
                    continue
                }
                val ocr = (ocrEngine.recognizeText(item.uri) as? AppResult.Ok)?.value
                val labelsList = (labelEngine.labelImage(item.uri) as? AppResult.Ok)?.value.orEmpty()

                val formattedLabels = labelsList.map { label ->
                    label.trim().replace(Regex("\\s+"), " ")
                        .lowercase()
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }.distinct()

                val rawRecognizedText = ocr?.fullText.orEmpty().trim().replace("\r\n", "\n")

                val ocrWords = rawRecognizedText.lowercase()
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() && it.length >= 2 }
                    .distinct()

                val combinedTerms = (formattedLabels.map { it.lowercase() } + ocrWords).distinct()
                val embeddingArray = vectorSearchEngine.generateEmbedding(combinedTerms)
                val embeddingBytes = embeddingArray.toByteArray()

                db.mediaDao().upsertText(
                    listOf(
                        MediaTextEntity(
                            mediaId = item.id.value,
                            fullText = rawRecognizedText,
                            searchText = "$rawRecognizedText ${formattedLabels.joinToString(" ")}",
                            labelsText = formattedLabels.joinToString(","),
                            blocksJson = "[]",
                            wordCount = ocrWords.size,
                            textCoverage = if (rawRecognizedText.isBlank()) 0f else 1f,
                            embedding = embeddingBytes,
                            confidence = 0.8f,
                            lastIndexed = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                        )
                    )
                )
                indexed++
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed++
            }
            progress.value = progress.value.withCounts(media.size, indexed, media.size - indexed - failed, failed, IndexState.RUNNING)
        }
        progress.value = progress.value.withCounts(media.size, indexed, 0, failed, IndexState.IDLE)
        return AppResult.Ok(Unit)
    }
}

class RoomSettingsRepository(private val db: AppDatabase) : SettingsRepository {
    override fun observe(): Flow<AppSettings> = db.settingsDao().observe()
        .map { entity -> entity?.toDomain() ?: AppSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        val current = db.settingsDao().get()?.toDomain() ?: AppSettings()
        db.settingsDao().upsert(transform(current).toEntity())
    }
}

private fun AppSettingsEntity.toDomain() = AppSettings(
    onboardingDone = onboardingDone,
    indexOnlyWhileCharging = indexOnlyWhileCharging,
    vaultLockTimeoutSeconds = vaultLockTimeoutSeconds,
    visualAnalysisEnabled = visualAnalysisEnabled,
    indexingCardShown = indexingCardShown,
)

private fun AppSettings.toEntity() = AppSettingsEntity(
    onboardingDone = onboardingDone,
    indexOnlyWhileCharging = indexOnlyWhileCharging,
    vaultLockTimeoutSeconds = vaultLockTimeoutSeconds,
    visualAnalysisEnabled = visualAnalysisEnabled,
    indexingCardShown = indexingCardShown,
)

class RoomVaultRepository(
    private val db: AppDatabase,
    private val mediaDeleteGateway: MediaDeleteGateway,
    private val vaultCryptoEngine: VaultCryptoEngine
) : VaultRepository {
    override val isUnlocked = MutableStateFlow(false)

    override fun observeItems(): Flow<List<VaultItem>> =
        db.vaultDao().observeVaultItems().map { list ->
            list.map { 
                VaultItem(
                    id = it.id,
                    filePath = vaultCryptoEngine.getBlobPath(it.id),
                    createdAtMs = it.createdAtMs,
                    width = it.width,
                    height = it.height,
                    mime = it.mime
                )
            }
        }

    override suspend fun unlock(): AppResult<Unit> {
        isUnlocked.value = true
        return AppResult.Ok(Unit)
    }

    override fun lock() {
        isUnlocked.value = false
    }

    override suspend fun moveToVault(id: MediaId): AppResult<Unit> {
        val media = db.mediaDao().getById(id.value)
            ?: return AppResult.Err(AppError.Io("Media item not found"))

        val enc = vaultCryptoEngine.encryptToVault(media.ref, media.id.toString())
        if (enc is AppResult.Err) return enc

        val filePath = media.ref
        val vaultEntity = VaultItemEntity(
            id = media.id.toString(),
            filePath = filePath,
            createdAtMs = currentTimeMillis(),
            mime = if (media.category.equals("VIDEO", ignoreCase = true)) "video/mp4" else "image/jpeg",
            sizeBytes = media.sizeBytes,
            width = media.width,
            height = media.height,
            originalTakenAtMs = media.sortKey
        )
        db.vaultDao().upsertVaultItem(vaultEntity)

        val del = mediaDeleteGateway.requestDelete(listOf(media.ref))
        if (del !is DeleteConsentResult.Deleted) {
            // rollback
            db.vaultDao().deleteVaultItem(vaultEntity.id)
            vaultCryptoEngine.deleteVaultBlobs(media.id.toString())
            val reason = (del as? DeleteConsentResult.NotPossible)?.reason ?: "Deletion declined"
            return AppResult.Err(AppError.Io(reason))
        }

        db.mediaDao().delete(listOf(media.id))
        db.mediaDao().deleteText(media.id)
        return AppResult.Ok(Unit)
    }

    override suspend fun restore(itemId: String): AppResult<Unit> {
        val vaultEntity = db.vaultDao().getVaultItem(itemId)
            ?: return AppResult.Err(AppError.Io("Media item not found"))

        val restored = vaultCryptoEngine.restoreToStorage(
            itemId = itemId,
            mime = vaultEntity.mime,
            takenAtMs = vaultEntity.originalTakenAtMs,
        )
        val newUri = when (restored) {
            is AppResult.Ok -> restored.value
            is AppResult.Err -> return restored
        }

        // MediaStore ki nayi id (Images/Video/Files teeno table mein same _ID hoti hai)
        val newId = newUri.substringAfterLast("/").toLongOrNull()
            ?: return AppResult.Err(AppError.Io("Invalid restored URI: $newUri"))
        // queryDelta wali same format, taake dobara vault mein add karne par resolve ho
        val newRef = "content://media/external/file/$newId"

        val mediaEntity = MediaEntity(
            id = newId,
            ref = newRef,
            origin = "LIBRARY",
            category = if (vaultEntity.mime.contains("video", true)) "VIDEO" else "PHOTO",
            sortKey = vaultEntity.originalTakenAtMs,
            width = vaultEntity.width,
            height = vaultEntity.height,
            sizeBytes = vaultEntity.sizeBytes,
            isScreenshot = false,
            entityFlags = 0,
            fingerprint = newRef
        )
        db.mediaDao().upsert(listOf(mediaEntity))
        db.vaultDao().deleteVaultItem(itemId)
        return AppResult.Ok(Unit)
    }

    override suspend fun delete(itemId: String): AppResult<Unit> {
        val result = vaultCryptoEngine.deleteVaultBlobs(itemId)
        if (result is AppResult.Err) return result
        db.vaultDao().deleteVaultItem(itemId)
        return AppResult.Ok(Unit)
    }
}

internal fun MediaEntity.toDomain(textEntity: MediaTextEntity? = null): MediaItem {
    val fileName = ref.substringAfterLast("/")
    return MediaItem(
        id = MediaId(id),
        uri = ref,
        origin = runCatching { MediaOrigin.valueOf(origin) }.getOrDefault(MediaOrigin.LIBRARY),
        category = runCatching { MediaCategory.valueOf(category) }.getOrDefault(MediaCategory.PHOTO),
        takenAtMs = sortKey,
        width = width,
        height = height,
        textPreview = textEntity?.fullText?.takeIf { it.isNotBlank() },
        entityFlags = entityFlags,
        fileName = fileName,
        labels = textEntity?.labelsText.orEmpty(),
        embedding = textEntity?.embedding,
        confidence = textEntity?.confidence ?: 0f,
        albumId = albumId,
        isFavorite = isFavorite,
    )
}

private fun MediaItem.toEntity() = MediaEntity(
    id = id.value,
    ref = uri,
    origin = origin.name,
    category = category.name,
    sortKey = takenAtMs,
    width = width,
    height = height,
    sizeBytes = 0L,
    isScreenshot = category == MediaCategory.SCREENSHOT,
    entityFlags = entityFlags,
    fingerprint = uri,
    albumId = albumId,
    isFavorite = isFavorite,
)

private fun buildSnippet(text: String, query: String): String {
    if (text.isBlank()) return "No text recognized"
    val index = text.indexOf(query, ignoreCase = true).coerceAtLeast(0)
    return text.substring(index.coerceAtMost(text.length), min(text.length, index + 180))
}

class RoomTrashRepository(
    private val db: AppDatabase,
    private val vaultCryptoEngine: VaultCryptoEngine,
) : TrashRepository {
    override fun observeTrashItems(): Flow<List<TrashItem>> =
        db.trashDao().observeTrashItems().map { list ->
            list.map { entity ->
                TrashItem(
                    id = MediaId(entity.id),
                    uri = entity.uri,
                    category = MediaCategory.valueOf(entity.category),
                    fileName = entity.fileName,
                    deletedAtMs = entity.deletedAtMs,
                )
            }
        }

    override suspend fun moveToTrash(item: MediaItem) {
        val trashUri = when (val res = vaultCryptoEngine.copyToTrash(item.uri, item.id.value.toString())) {
            is AppResult.Ok -> res.value
            else -> item.uri
        }
        db.trashDao().insertTrashItem(
            TrashItemEntity(
                id = item.id.value,
                uri = trashUri,
                category = item.category.name,
                fileName = item.fileName,
                deletedAtMs = Clock.System.now().toEpochMilliseconds(),
            )
        )
        db.exclusionDao().upsertExclusion(
            IndexExclusionEntity(
                ref = item.uri,
                excludedAtMs = Clock.System.now().toEpochMilliseconds(),
            )
        )
    }

    override suspend fun deleteFromTrash(id: MediaId) {
        db.trashDao().deleteTrashItem(id.value)
    }

    override suspend fun restoreFromTrash(id: MediaId) {
        val entity = db.trashDao().getTrashItem(id.value) ?: return
        db.mediaDao().upsert(
            listOf(
                MediaEntity(
                    id = entity.id,
                    ref = entity.uri,
                    origin = "GALLERY",
                    category = entity.category,
                    sortKey = entity.deletedAtMs,
                    mime = "image/*",
                    width = 0,
                    height = 0,
                    sizeBytes = 0L,
                    isScreenshot = false,
                    entityFlags = 0,
                    fingerprint = ""
                )
            )
        )
        db.exclusionDao().deleteExclusion(entity.uri)
        db.trashDao().deleteTrashItem(entity.id)
    }
}
