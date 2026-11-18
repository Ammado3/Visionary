package com.o3.vanguard.visionary.platform

import coil3.fetch.Fetcher
import coil3.decode.Decoder
import coil3.video.VideoFrameDecoder

actual fun getPlatformFetcherFactory(): Fetcher.Factory<Any>? = null

actual fun getPlatformDecoderFactories(): List<Decoder.Factory> =
    listOf(VideoFrameDecoder.Factory())
