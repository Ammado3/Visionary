package com.o3.vanguard.visionary.data

import com.o3.vanguard.visionary.core.database.dao.CategoryCount
import com.o3.vanguard.visionary.domain.model.CollectionKind
import com.o3.vanguard.visionary.domain.model.SearchFilter
import com.o3.vanguard.visionary.domain.model.SmartEntityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollectionSummaryTest {

    @Test
    fun summarizeCollections_emptyInput_returnsFiveSummariesWithZeroCountAndNullCover() {
        val summaries = summarizeCollections(emptyList())

        assertEquals(5, summaries.size)
        assertEquals(CollectionKind.entries, summaries.map { it.kind })
        summaries.forEach { summary ->
            assertEquals(0, summary.count)
            assertNull(summary.coverUri)
        }
    }

    @Test
    fun summarizeCollections_withInput_computesCountsAndCoversCorrectly() {
        val counts = listOf(
            CategoryCount(category = "PHOTO", count = 3, coverRef = "photo_cover_uri", newestSortKey = 100L),
            CategoryCount(category = "SCREENSHOT", count = 2, coverRef = "screenshot_cover_uri", newestSortKey = 300L),
            CategoryCount(category = "RECEIPT", count = 1, coverRef = "receipt_cover_uri", newestSortKey = 200L)
        )

        val summaries = summarizeCollections(counts)

        assertEquals(CollectionKind.entries, summaries.map { it.kind })

        val allSummary = summaries.first { it.kind == CollectionKind.ALL }
        assertEquals(6, allSummary.count)
        assertEquals("screenshot_cover_uri", allSummary.coverUri)

        val screenshotSummary = summaries.first { it.kind == CollectionKind.SCREENSHOTS }
        assertEquals(2, screenshotSummary.count)
        assertEquals("screenshot_cover_uri", screenshotSummary.coverUri)

        val receiptSummary = summaries.first { it.kind == CollectionKind.RECEIPTS }
        assertEquals(1, receiptSummary.count)
        assertEquals("receipt_cover_uri", receiptSummary.coverUri)

        val documentSummary = summaries.first { it.kind == CollectionKind.DOCUMENTS }
        assertEquals(0, documentSummary.count)
        assertNull(documentSummary.coverUri)

        val scanSummary = summaries.first { it.kind == CollectionKind.SCANS }
        assertEquals(0, scanSummary.count)
        assertNull(scanSummary.coverUri)
    }

    @Test
    fun matchesSearchFilter_categoryMatchAndMiss() {
        assertTrue(matchesSearchFilter("SCREENSHOT", 0, SearchFilter.SCREENSHOTS))
        assertFalse(matchesSearchFilter("PHOTO", 0, SearchFilter.SCREENSHOTS))
        assertTrue(matchesSearchFilter("PHOTO", 0, SearchFilter.ALL))
    }

    @Test
    fun matchesSearchFilter_hasPhoneOrUrl_entityFlags() {
        val phoneFlags = SmartEntityType.PHONE.flagBit
        val urlFlags = SmartEntityType.URL.flagBit
        val emailFlags = SmartEntityType.EMAIL.flagBit

        assertTrue(matchesSearchFilter("PHOTO", phoneFlags, SearchFilter.HAS_PHONE_OR_URL))
        assertTrue(matchesSearchFilter("PHOTO", urlFlags, SearchFilter.HAS_PHONE_OR_URL))
        assertFalse(matchesSearchFilter("PHOTO", emailFlags, SearchFilter.HAS_PHONE_OR_URL))
        assertFalse(matchesSearchFilter("PHOTO", 0, SearchFilter.HAS_PHONE_OR_URL))
    }
}
