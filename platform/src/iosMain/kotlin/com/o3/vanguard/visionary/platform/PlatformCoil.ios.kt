package com.o3.vanguard.visionary.platform

import coil3.fetch.Fetcher
import coil3.decode.Decoder
import com.o3.vanguard.visionary.platform.ios.PhUriFetcher

actual fun getPlatformFetcherFactory(): Fetcher.Factory<Any>? = PhUriFetcher.Factory()

actual fun getPlatformDecoderFactories(): List<Decoder.Factory> = emptyList()
