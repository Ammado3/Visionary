package com.o3.vanguard.visionary.domain

import com.o3.vanguard.visionary.domain.model.SmartEntityType
import com.o3.vanguard.visionary.domain.usecase.EntityExtractor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Characterization tests for [EntityExtractor].
 */
class EntityExtractorTest {

    private val extractor = EntityExtractor()

    @Test
    fun extractor_textWithPhoneNumber_detectsPhoneEntity() {
        val input = "Call me at 555-123-4567 tomorrow"
        val entities = extractor.extract(input)

        assertEquals(1, entities.size)
        assertEquals(SmartEntityType.PHONE, entities[0].type)
        assertEquals("Phone", entities[0].displayLabel)
        // Characterization check: rawValue currently captures entire input string
        assertEquals(input, entities[0].rawValue)
    }

    @Test
    fun extractor_textWithEmailAddress_detectsEmailEntity() {
        val input = "Contact support@example.com for help"
        val entities = extractor.extract(input)

        assertEquals(1, entities.size)
        assertEquals(SmartEntityType.EMAIL, entities[0].type)
        assertEquals("Email", entities[0].displayLabel)
        assertEquals(input, entities[0].rawValue)
    }

    @Test
    fun extractor_textWithPhoneAndEmail_detectsBothEntities() {
        val input = "Reach out to info@example.com or 123-456-7890"
        val entities = extractor.extract(input)

        assertEquals(2, entities.size)
        assertTrue(entities.any { it.type == SmartEntityType.PHONE })
        assertTrue(entities.any { it.type == SmartEntityType.EMAIL })
    }

    @Test
    fun extractor_textWithoutEntities_returnsEmptyList() {
        val entities = extractor.extract("Just a plain text without contact details")
        assertTrue(entities.isEmpty())
    }
}
