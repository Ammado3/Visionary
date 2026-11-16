package com.o3.vanguard.visionary.platform.ios

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.Buffer
import okio.FileSystem

class PhUriFetcher(
    private val url: String,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        if (!url.startsWith("ph://")) return null

        val bytes = PhAssetResolver.fetchImageData(url) ?: return null
        if (bytes.isEmpty()) return null

        val buffer = Buffer().apply { write(bytes) }

        return SourceFetchResult(
            source = ImageSource(
                source = buffer,
                fileSystem = FileSystem.SYSTEM,
            ),
            mimeType = "image/jpeg",
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Any> {
        override fun create(data: Any, options: Options, imageLoader: ImageLoader): Fetcher? {
            val urlString = when (data) {
                is String -> data
                is Uri -> data.toString()
                else -> data.toString()
            }

            if (urlString.startsWith("ph://")) {
                return PhUriFetcher(urlString)
            }
            return null
        }
    }
}
