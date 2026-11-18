package com.o3.vanguard.visionary.platform

import coil3.fetch.Fetcher
import com.o3.vanguard.visionary.platform.ios.PhUriFetcher

actual fun getPlatformFetcherFactory(): Fetcher.Factory<Any>? = PhUriFetcher.Factory()
