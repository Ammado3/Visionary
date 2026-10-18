package com.o3.vanguard.visionary.core.testing

import com.o3.vanguard.visionary.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow

enum class FakeScenario { Normal, Empty, Error, Loading }

class FakeControls {
    val scenario = MutableStateFlow(FakeScenario.Normal)
}

object FakeData {
    val media: List<MediaItem> = List(60) { i ->
        val category = when (i % 5) {
            0 -> MediaCategory.SCREENSHOT
            1 -> MediaCategory.DOCUMENT
            2 -> MediaCategory.RECEIPT
            3 -> MediaCategory.SCAN
            else -> MediaCategory.PHOTO
        }
        MediaItem(
            id = MediaId(i.toLong() + 1),
            uri = "fake://media/${i + 1}",
            origin = if (category == MediaCategory.SCAN) MediaOrigin.APP_SCAN else MediaOrigin.LIBRARY,
            category = category,
            takenAtMs = 1_760_000_000_000L - i * 3_600_000L,
            width = 1080,
            height = if (category == MediaCategory.SCREENSHOT) 2400 else 1440,
            textPreview = if (i % 2 == 0) "Sample text line ${i + 1}" else null,
            entityFlags = if (i % 3 == 0) SmartEntityType.PHONE.flagBit else 0
        )
    }
}
