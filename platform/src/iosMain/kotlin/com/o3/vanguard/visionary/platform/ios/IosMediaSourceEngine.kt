package com.o3.vanguard.visionary.platform.ios

import com.o3.vanguard.visionary.core.common.AppError
import com.o3.vanguard.visionary.core.common.AppResult
import com.o3.vanguard.visionary.domain.model.*
import com.o3.vanguard.visionary.domain.port.MediaSourceEngine
import com.o3.vanguard.visionary.domain.port.StorageAlbumSourceEngine
import platform.Foundation.*
import platform.Photos.*

class IosMediaSourceEngine : MediaSourceEngine {

    override suspend fun queryDelta(sinceCheckpoint: Long): AppResult<List<MediaItem>> = try {
        val options = PHFetchOptions().apply {
            sortDescriptors = listOf(NSSortDescriptor.sortDescriptorWithKey("creationDate", ascending = false))
        }

        val result = mutableListOf<MediaItem>()

        val imagesResult = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, options)
        val videosResult = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeVideo, options)

        val totalImages = imagesResult.count.toInt()
        for (i in 0 until totalImages) {
            val asset = imagesResult.objectAtIndex(i.toULong()) as PHAsset
            result.add(mapAssetToMediaItem(asset, isVideo = false))
        }

        val totalVideos = videosResult.count.toInt()
        for (i in 0 until totalVideos) {
            val asset = videosResult.objectAtIndex(i.toULong()) as PHAsset
            result.add(mapAssetToMediaItem(asset, isVideo = true))
        }

        result.sortByDescending { it.takenAtMs }
        AppResult.Ok(result)
    } catch (e: Throwable) {
        AppResult.Err(AppError.Io(e.message ?: "Failed to query iOS Photo Library"))
    }

    override suspend fun queryAllLibraryIds(): AppResult<Set<Long>> {
        return when (val query = queryDelta(0L)) {
            is AppResult.Ok -> AppResult.Ok(query.value.map { it.id.value }.toSet())
            is AppResult.Err -> query
        }
    }

    private fun mapAssetToMediaItem(asset: PHAsset, isVideo: Boolean): MediaItem {
        val localId = asset.localIdentifier
        val longId = localIdToLongId(localId)
        val isScreenshot = (asset.mediaSubtypes and PHAssetMediaSubtypePhotoScreenshot) != 0UL
        val creationDateMs = ((asset.creationDate?.timeIntervalSince1970() ?: 0.0) * 1000.0).toLong()

        return MediaItem(
            id = MediaId(longId),
            uri = "ph://$localId",
            origin = MediaOrigin.LIBRARY,
            category = when {
                isVideo -> MediaCategory.VIDEO
                isScreenshot -> MediaCategory.SCREENSHOT
                else -> MediaCategory.PHOTO
            },
            takenAtMs = if (creationDateMs > 0) creationDateMs else 0L,
            width = asset.pixelWidth.toInt(),
            height = asset.pixelHeight.toInt(),
            textPreview = null,
        )
    }

    private fun localIdToLongId(localIdentifier: String): Long {
        var hash = -3750763025362895579L
        for (char in localIdentifier) {
            hash = hash xor char.code.toLong()
            hash *= 1099511628211L
        }
        return hash and Long.MAX_VALUE
    }
}

class IosStorageAlbumSourceEngine : StorageAlbumSourceEngine {

    override suspend fun loadAlbums(): AppResult<List<StorageAlbum>> = try {
        val albums = mutableListOf<StorageAlbum>()

        val userAlbums = PHAssetCollection.fetchAssetCollectionsWithType(
            PHAssetCollectionTypeAlbum,
            PHAssetCollectionSubtypeAny,
            null
        )
        val smartAlbums = PHAssetCollection.fetchAssetCollectionsWithType(
            PHAssetCollectionTypeSmartAlbum,
            PHAssetCollectionSubtypeAny,
            null
        )

        val processCollections = { collections: PHFetchResult ->
            val count = collections.count.toInt()
            for (i in 0 until count) {
                val collection = collections.objectAtIndex(i.toULong()) as PHAssetCollection
                val name = collection.localizedTitle ?: "Album"
                val assets = PHAsset.fetchAssetsInAssetCollection(collection, null)
                val totalCount = assets.count.toInt()
                if (totalCount > 0) {
                    var videoCount = 0
                    var firstCoverUri: String? = null

                    for (j in 0 until totalCount) {
                        val asset = assets.objectAtIndex(j.toULong()) as PHAsset
                        if (firstCoverUri == null) {
                            firstCoverUri = "ph://${asset.localIdentifier}"
                        }
                        if (asset.mediaType == PHAssetMediaTypeVideo) {
                            videoCount++
                        }
                    }

                    albums.add(
                        StorageAlbum(
                            id = collection.localIdentifier,
                            name = name,
                            itemCount = totalCount,
                            videoCount = videoCount,
                            coverUri = firstCoverUri,
                        )
                    )
                }
            }
        }

        processCollections(userAlbums)
        processCollections(smartAlbums)

        AppResult.Ok(albums)
    } catch (e: Throwable) {
        AppResult.Err(AppError.Io(e.message ?: "Failed to load iOS Albums"))
    }
}
