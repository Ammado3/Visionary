package com.o3.vanguard.visionary.domain

import com.o3.vanguard.visionary.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryMappingTest {

    @Test
    fun collectionKind_all_returnsAllMediaCategories() {
        assertEquals(MediaCategory.entries.toSet(), CollectionKind.ALL.categories())
    }

    @Test
    fun collectionKind_singularMapping() {
        assertEquals(setOf(MediaCategory.SCREENSHOT), CollectionKind.SCREENSHOTS.categories())
        assertEquals(setOf(MediaCategory.DOCUMENT), CollectionKind.DOCUMENTS.categories())
        assertEquals(setOf(MediaCategory.RECEIPT), CollectionKind.RECEIPTS.categories())
        assertEquals(setOf(MediaCategory.SCAN), CollectionKind.SCANS.categories())
    }

    @Test
    fun collectionKind_regressionGuard_noPluralCategoryNames() {
        val validCategoryNames = MediaCategory.entries.map { it.name }.toSet()
        CollectionKind.entries.filter { it != CollectionKind.ALL }.forEach { kind ->
            val categories = kind.categories()
            categories.forEach { category ->
                assertTrue(category.name in validCategoryNames, "Category name ${category.name} must be a valid MediaCategory name")
                assertFalse(category.name == kind.name, "Category name ${category.name} must not equal plural CollectionKind name ${kind.name}")
            }
        }
    }

    @Test
    fun searchFilter_categories() {
        assertNull(SearchFilter.ALL.categories())
        assertNull(SearchFilter.HAS_PHONE_OR_URL.categories())
        assertEquals(setOf(MediaCategory.SCREENSHOT), SearchFilter.SCREENSHOTS.categories())
        assertEquals(setOf(MediaCategory.RECEIPT), SearchFilter.RECEIPTS.categories())
        assertEquals(setOf(MediaCategory.DOCUMENT), SearchFilter.DOCUMENTS.categories())
        assertEquals(setOf(MediaCategory.SCAN), SearchFilter.SCANS.categories())
    }

    @Test
    fun searchFilter_requiredEntityFlags() {
        val phoneOrUrlFlags = SearchFilter.HAS_PHONE_OR_URL.requiredEntityFlags()
        val phoneBit = SmartEntityType.PHONE.flagBit
        val urlBit = SmartEntityType.URL.flagBit
        val emailBit = SmartEntityType.EMAIL.flagBit

        assertTrue((phoneOrUrlFlags and phoneBit) != 0, "HAS_PHONE_OR_URL must have PHONE bit set")
        assertTrue((phoneOrUrlFlags and urlBit) != 0, "HAS_PHONE_OR_URL must have URL bit set")
        assertFalse((phoneOrUrlFlags and emailBit) != 0, "HAS_PHONE_OR_URL must NOT have EMAIL bit set")

        val otherFilters = listOf(
            SearchFilter.ALL,
            SearchFilter.SCREENSHOTS,
            SearchFilter.RECEIPTS,
            SearchFilter.DOCUMENTS,
            SearchFilter.SCANS
        )
        otherFilters.forEach { filter ->
            assertEquals(0, filter.requiredEntityFlags(), "Filter $filter should have 0 required entity flags")
        }
    }
}
