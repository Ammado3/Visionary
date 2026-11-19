package com.o3.vanguard.visionary.platform

import coil3.fetch.Fetcher
import coil3.decode.Decoder

expect fun getPlatformFetcherFactory(): Fetcher.Factory<Any>?
expect fun getPlatformDecoderFactories(): List<Decoder.Factory>
