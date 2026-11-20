package com.o3.vanguard.visionary.domain.voice

import com.o3.vanguard.visionary.domain.model.MediaCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoiceQueryParserTest {

    @Test
    fun parse_findReceiptScreenshotForTehzeebBakers_returnsKeywordsAndHints() {
        val query = VoiceQueryParser.parse("Find the receipt screenshot for Tehzeeb Bakers")
        assertEquals(listOf("tehzeeb", "bakers"), query.keywords)
        assertEquals(setOf(MediaCategory.RECEIPT, MediaCategory.SCREENSHOT), query.categoryHints)
    }

    @Test
    fun parse_showMeScreenShotFromDaraz_returnsScreenshotHintAndKeyword() {
        val query = VoiceQueryParser.parse("show me the screen shot from Daraz")
        assertEquals(listOf("daraz"), query.keywords)
        assertEquals(setOf(MediaCategory.SCREENSHOT), query.categoryHints)
    }

    @Test
    fun parse_tehzeebBakersBill_stripsPossessiveAndExtractsReceiptHint() {
        val query = VoiceQueryParser.parse("Tehzeeb Baker's bill")
        assertEquals(listOf("tehzeeb", "baker"), query.keywords)
        assertEquals(setOf(MediaCategory.RECEIPT), query.categoryHints)
    }

    @Test
    fun parse_invoiceNumber_keepsNumberAndReceiptHint() {
        val query = VoiceQueryParser.parse("invoice 4521")
        assertEquals(listOf("4521"), query.keywords)
        assertEquals(setOf(MediaCategory.RECEIPT), query.categoryHints)
    }

    @Test
    fun parse_findMyPhotos_returnsNoKeywordsNoHints() {
        val query = VoiceQueryParser.parse("find my photos")
        assertTrue(query.keywords.isEmpty())
        assertTrue(query.categoryHints.isEmpty())
    }

    @Test
    fun parse_emptyStringAndWhitespace_returnsNoKeywords() {
        val queryEmpty = VoiceQueryParser.parse("")
        assertTrue(queryEmpty.keywords.isEmpty())
        assertTrue(queryEmpty.categoryHints.isEmpty())

        val querySpaces = VoiceQueryParser.parse("   \t \n ")
        assertTrue(querySpaces.keywords.isEmpty())
        assertTrue(querySpaces.categoryHints.isEmpty())
    }

    @Test
    fun parse_punctuationAndMixedCase_extractsCategoryHintWithoutKeywords() {
        val query = VoiceQueryParser.parse("FIND… the RECEIPT!!")
        assertTrue(query.keywords.isEmpty())
        assertEquals(setOf(MediaCategory.RECEIPT), query.categoryHints)
    }
}
