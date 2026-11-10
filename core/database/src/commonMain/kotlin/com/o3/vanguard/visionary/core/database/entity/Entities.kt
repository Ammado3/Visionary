package com.o3.vanguard.visionary.core.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "media_item",
    indices = [Index("sortKey"), Index("category"), Index("ref", unique = true)]
)
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ref: String,
    val origin: String,
    val category: String,
    val sortKey: Long,
    val mime: String? = null,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val isScreenshot: Boolean,
    val entityFlags: Int,
    val fingerprint: String,
)

@Entity(tableName = "media_text")
data class MediaTextEntity(
    @PrimaryKey val mediaId: Long,
    val fullText: String,
    val searchText: String,
    val labelsText: String,
    val blocksJson: String,
    val wordCount: Int,
    val textCoverage: Float,
    val embedding: ByteArray? = null,
    val confidence: Float = 0f,
    val lastIndexed: Long = 0L,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as MediaTextEntity
        if (mediaId != other.mediaId) return false
        if (fullText != other.fullText) return false
        if (searchText != other.searchText) return false
        if (labelsText != other.labelsText) return false
        if (blocksJson != other.blocksJson) return false
        if (wordCount != other.wordCount) return false
        if (textCoverage != other.textCoverage) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (confidence != other.confidence) return false
        if (lastIndexed != other.lastIndexed) return false
        return true
    }

    override fun hashCode(): Int {
        var result = mediaId.hashCode()
        result = 31 * result + fullText.hashCode()
        result = 31 * result + searchText.hashCode()
        result = 31 * result + labelsText.hashCode()
        result = 31 * result + blocksJson.hashCode()
        result = 31 * result + wordCount
        result = 31 * result + textCoverage.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + confidence.hashCode()
        result = 31 * result + lastIndexed.hashCode()
        return result
    }
}

@Entity(tableName = "index_job", indices = [Index("state")])
data class IndexJobEntity(
    @PrimaryKey val mediaId: Long,
    val state: String,
    val attempts: Int,
    val nextAttemptAtMs: Long,
    val lastErrorKind: String?,
)

@Entity(tableName = "index_exclusion")
data class IndexExclusionEntity(
    @PrimaryKey val ref: String,
    val excludedAtMs: Long,
)

@Entity(tableName = "vault_item")
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val createdAtMs: Long,
    val mime: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val originalTakenAtMs: Long,
)

@Entity(tableName = "recent_search")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAtMs: Long,
)

@Entity(tableName = "sync_checkpoint")
data class SyncCheckpointEntity(
    @PrimaryKey val key: String = "DEFAULT",
    val lastTimestampMs: Long,
    val lastGeneration: Long,
)
