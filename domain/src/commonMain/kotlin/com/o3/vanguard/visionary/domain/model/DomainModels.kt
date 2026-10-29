package com.o3.vanguard.visionary.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class MediaId(val value: Long)

enum class MediaOrigin { LIBRARY, APP_SCAN }

enum class MediaCategory { PHOTO, SCREENSHOT, DOCUMENT, RECEIPT, SCAN }

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class OcrTextLine(
    val text: String,
    val boundingBox: NormalizedRect,
    val confidence: Float,
)

data class OcrResult(
    val fullText: String,
    val lines: List<OcrTextLine>,
)

enum class SmartEntityType(val flagBit: Int) {
    PHONE(1 shl 0),
    EMAIL(1 shl 1),
    URL(1 shl 2),
    AMOUNT(1 shl 3),
    WIFI_PASSWORD(1 shl 4),
    TRACKING_CODE(1 shl 5),
    OTP(1 shl 6),
}

data class SmartEntity(
    val type: SmartEntityType,
    val rawValue: String,
    val displayLabel: String,
)

data class MediaItem(
    val id: MediaId,
    val uri: String,
    val origin: MediaOrigin,
    val category: MediaCategory,
    val takenAtMs: Long,
    val width: Int,
    val height: Int,
    val textPreview: String?,
    val entityFlags: Int = 0,
    val fileName: String = "",
    val labels: String = "",
    val embedding: ByteArray? = null,
    val confidence: Float = 0f,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as MediaItem
        if (id != other.id) return false
        if (uri != other.uri) return false
        if (origin != other.origin) return false
        if (category != other.category) return false
        if (takenAtMs != other.takenAtMs) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (textPreview != other.textPreview) return false
        if (entityFlags != other.entityFlags) return false
        if (fileName != other.fileName) return false
        if (labels != other.labels) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (confidence != other.confidence) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + origin.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + takenAtMs.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + (textPreview?.hashCode() ?: 0)
        result = 31 * result + entityFlags
        result = 31 * result + fileName.hashCode()
        result = 31 * result + labels.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + confidence.hashCode()
        return result
    }
}

enum class CollectionKind(val id: String) {
    ALL("all"),
    SCREENSHOTS("screenshots"),
    DOCUMENTS("documents"),
    RECEIPTS("receipts"),
    SCANS("scans");

    companion object {
        fun fromId(id: String): CollectionKind = entries.firstOrNull { it.id == id } ?: ALL
    }
}

data class CollectionSummary(
    val kind: CollectionKind,
    val count: Int,
    val coverUri: String?,
)

enum class SearchFilter { ALL, SCREENSHOTS, RECEIPTS, DOCUMENTS, SCANS, HAS_PHONE_OR_URL }

enum class SearchMode { EXACT, PREFIX, CONTAINS, FUZZY }

data class SearchHit(
    val item: MediaItem,
    val snippet: String,
    val matchedTerms: List<String>,
    val entities: List<SmartEntity> = emptyList(),
    val relevanceScore: Float = 0f,
)

enum class IndexState { IDLE, RUNNING, PAUSED_BATTERY, NEEDS_ACCESS }

data class IndexProgress(
    val total: Int,
    val indexed: Int,
    val pending: Int,
    val failed: Int,
    val state: IndexState,
)

sealed interface MediaAccess {
    data object Full : MediaAccess
    data class Partial(val count: Int) : MediaAccess
    data object Denied : MediaAccess
}

sealed interface DeleteConsentResult {
    data object Deleted : DeleteConsentResult
    data object DeclinedByUser : DeleteConsentResult
    data class NotPossible(val reason: String) : DeleteConsentResult
}

data class VaultItem(
    val id: String,
    val createdAtMs: Long,
    val width: Int,
    val height: Int,
)

data class ScannedDocument(
    val pageUris: List<String>,
    val pdfUri: String?,
)

data class AppSettings(
    val onboardingDone: Boolean = false,
    val indexOnlyWhileCharging: Boolean = false,
    val vaultLockTimeoutSeconds: Int = 30,
)
