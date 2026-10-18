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
)

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
