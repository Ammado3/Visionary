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
import com.o3.vanguard.visionary.domain.model.*
import com.o3.vanguard.visionary.domain.port.ImageLabelEngine
import com.o3.vanguard.visionary.domain.port.MediaSourceEngine
import com.o3.vanguard.visionary.domain.port.OcrEngine
import com.o3.vanguard.visionary.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlin.math.min
import kotlin.time.Clock

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
            .map { list ->
                list.map { entity ->
                    val textEntity = db.mediaDao().getText(entity.id)
                    entity.toDomain(textEntity)
                }
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

    override fun observeRecentFinds(limit: Int): Flow<List<MediaItem>> =
        db.mediaDao().observeAll().map { list ->
            list.take(limit).map { entity ->
                val textEntity = db.mediaDao().getText(entity.id)
                entity.toDomain(textEntity)
            }
        }

    override suspend fun deleteMedia(id: MediaId) {
        db.mediaDao().delete(listOf(id.value))
    }
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
        val categoryFilter = when (filter) {
            SearchFilter.ALL, SearchFilter.HAS_PHONE_OR_URL -> "ALL"
            SearchFilter.SCREENSHOTS -> "SCREENSHOTS"
            SearchFilter.RECEIPTS -> "RECEIPTS"
            SearchFilter.DOCUMENTS -> "DOCUMENTS"
            SearchFilter.SCANS -> "SCANS"
        }

        val allEntities = db.mediaDao().getAll()
        val filteredEntities = if (categoryFilter == "ALL") {
            allEntities
        } else {
            allEntities.filter { it.category == categoryFilter }
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
) : IndexRepository {
    private val progress = MutableStateFlow(IndexProgress(0, 0, 0, 0, IndexState.IDLE))

    override fun observeProgress(): Flow<IndexProgress> = progress

    override suspend fun retryFailed(): AppResult<Unit> = rebuild()

    override suspend fun rebuild(): AppResult<Unit> {
        val source = mediaSource.queryDelta(0L)
        if (source !is AppResult.Ok) return source as AppResult.Err
        val excludedRefs = db.exclusionDao().getAllExclusions().toSet()
        val media = source.value.filter { it.uri !in excludedRefs }
        progress.value = IndexProgress(media.size, 0, media.size, 0, IndexState.RUNNING)

        val currentRefs = media.map { it.uri }.toSet()
        db.mediaDao().deleteNonExistingImages(currentRefs.toList())

        var indexed = 0
        var failed = 0
        media.forEach { item ->
            try {
                // Persist every media item, including videos. OCR and image labeling
                // are only available for still images.
                db.mediaDao().upsert(listOf(item.toEntity()))
                if (item.category == MediaCategory.VIDEO) {
                    indexed++
                    progress.value = IndexProgress(media.size, indexed, media.size - indexed - failed, failed, IndexState.RUNNING)
                    return@forEach
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
            } catch (_: Throwable) {
                failed++
            }
            progress.value = IndexProgress(media.size, indexed, media.size - indexed - failed, failed, IndexState.RUNNING)
        }
        progress.value = IndexProgress(media.size, indexed, 0, failed, IndexState.IDLE)
        return AppResult.Ok(Unit)
    }
}

class RoomSettingsRepository : SettingsRepository {
    private val settings = MutableStateFlow(AppSettings())

    override fun observe(): Flow<AppSettings> = settings

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settings.update(transform)
    }
}

class RoomVaultRepository(
    private val db: AppDatabase,
    private val mediaDeleteGateway: MediaDeleteGateway,
    private val vaultCryptoEngine: VaultCryptoEngine
) : VaultRepository {
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

    override suspend fun moveToVault(id: MediaId): AppResult<Unit> {
        val media = db.mediaDao().getById(id.value)
            ?: return AppResult.Err(AppError.Io("Media item not found"))

        val enc = vaultCryptoEngine.encryptToVault(media.ref, media.id.toString())
        if (enc is AppResult.Err) return enc

        val vaultEntity = VaultItemEntity(
            id = media.id.toString(),
            createdAtMs = currentTimeMillis(),
            mime = media.mime ?: "image/*",
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

    override suspend fun restore(itemId: String): AppResult<Unit> = AppResult.Ok(Unit)

    override suspend fun delete(itemId: String): AppResult<Unit> {
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
