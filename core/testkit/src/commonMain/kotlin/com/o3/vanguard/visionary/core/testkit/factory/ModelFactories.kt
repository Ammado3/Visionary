package com.o3.vanguard.visionary.core.testkit.factory

import com.o3.vanguard.visionary.domain.model.AppSettings
import com.o3.vanguard.visionary.domain.model.CollectionKind
import com.o3.vanguard.visionary.domain.model.CollectionSummary
import com.o3.vanguard.visionary.domain.model.MediaCategory
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.MediaItem
import com.o3.vanguard.visionary.domain.model.MediaOrigin
import com.o3.vanguard.visionary.domain.model.VaultItem

fun testMediaItem(
    id: MediaId = MediaId(1L),
    uri: String = "content://media/external/images/1",
    origin: MediaOrigin = MediaOrigin.LIBRARY,
    category: MediaCategory = MediaCategory.PHOTO,
    takenAtMs: Long = 1000L,
    width: Int = 1080,
    height: Int = 1920,
    textPreview: String? = null,
    entityFlags: Int = 0,
    fileName: String = "test.jpg",
    labels: String = "",
    embedding: ByteArray? = null,
    confidence: Float = 1.0f,
): MediaItem = MediaItem(
    id = id,
    uri = uri,
    origin = origin,
    category = category,
    takenAtMs = takenAtMs,
    width = width,
    height = height,
    textPreview = textPreview,
    entityFlags = entityFlags,
    fileName = fileName,
    labels = labels,
    embedding = embedding,
    confidence = confidence,
)

fun testCollectionSummary(
    kind: CollectionKind = CollectionKind.ALL,
    count: Int = 10,
    coverUri: String? = "content://media/external/images/1",
): CollectionSummary = CollectionSummary(
    kind = kind,
    count = count,
    coverUri = coverUri,
)

fun testVaultItem(
    id: String = "vault_item_1",
    filePath: String = "/vault/item_1",
    createdAtMs: Long = 1000L,
    width: Int = 1080,
    height: Int = 1920,
    mime: String = "image/jpeg",
): VaultItem = VaultItem(
    id = id,
    filePath = filePath,
    createdAtMs = createdAtMs,
    width = width,
    height = height,
    mime = mime,
)

fun testAppSettings(
    onboardingDone: Boolean = false,
    indexOnlyWhileCharging: Boolean = false,
    vaultLockTimeoutSeconds: Int = 30,
    visualAnalysisEnabled: Boolean = true,
): AppSettings = AppSettings(
    onboardingDone = onboardingDone,
    indexOnlyWhileCharging = indexOnlyWhileCharging,
    vaultLockTimeoutSeconds = vaultLockTimeoutSeconds,
    visualAnalysisEnabled = visualAnalysisEnabled,
)

