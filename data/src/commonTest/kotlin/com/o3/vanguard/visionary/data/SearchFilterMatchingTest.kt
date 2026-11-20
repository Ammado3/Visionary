package com.o3.vanguard.visionary.data

import com.o3.vanguard.visionary.domain.model.SearchFilter
import com.o3.vanguard.visionary.domain.model.SmartEntityType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchFilterMatchingTest {

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
