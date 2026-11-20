package com.o3.vanguard.visionary.domain.voice

import com.o3.vanguard.visionary.core.testkit.factory.testMediaItem
import com.o3.vanguard.visionary.core.testkit.fake.FakeSearchRepository
import com.o3.vanguard.visionary.domain.model.MediaCategory
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.SearchFilter
import com.o3.vanguard.visionary.domain.model.SearchHit
import com.o3.vanguard.visionary.domain.model.SearchMode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoiceSearchUseCaseTest {

    @Test
    fun invoke_usesSearchFilterAllAndSearchModeFuzzy() = runTest {
        val fakeRepo = FakeSearchRepository()
        val useCase = VoiceSearchUseCase(fakeRepo)

        useCase(listOf("find receipt for Tehzeeb Bakers"))

        assertEquals(1, fakeRepo.searchCalls.size)
        val call = fakeRepo.searchCalls.first()
        assertEquals("tehzeeb bakers", call.first)
        assertEquals(SearchFilter.ALL, call.second)
        assertEquals(SearchMode.FUZZY, call.third)
    }

    @Test
    fun invoke_maxThreeAlternativesAndDeduplicatesSearches() = runTest {
        val fakeRepo = FakeSearchRepository()
        val useCase = VoiceSearchUseCase(fakeRepo)

        // 4 alternatives, but 3 & 4 produce duplicate keywords "the zeb bakers"
        val alternatives = listOf(
            "find receipt for Tehzeeb Bakers",
            "find receipt for the zeb bakers",
            "show receipt for the zeb bakers",
            "tehzib bakers",
        )

        useCase(alternatives)

        // Only first 3 alternatives are evaluated; "the zeb bakers" is deduplicated
        assertEquals(2, fakeRepo.searchCalls.size)
        assertEquals("tehzeeb bakers", fakeRepo.searchCalls[0].first)
        assertEquals("zeb bakers", fakeRepo.searchCalls[1].first)
    }

    @Test
    fun invoke_hintBoostRanksMatchingCategoryHigher() = runTest {
        val fakeRepo = FakeSearchRepository()
        val useCase = VoiceSearchUseCase(fakeRepo)

        val receiptItem = testMediaItem(id = MediaId(1L), category = MediaCategory.RECEIPT, takenAtMs = 1000L)
        val photoItem = testMediaItem(id = MediaId(2L), category = MediaCategory.PHOTO, takenAtMs = 1000L)

        // photoItem has higher raw score (0.7f), but receiptItem (0.6f) matches RECEIPT hint (+0.25f => 0.85f)
        fakeRepo.searchHits = listOf(
            SearchHit(item = photoItem, snippet = "", matchedTerms = listOf("bakers"), relevanceScore = 0.7f),
            SearchHit(item = receiptItem, snippet = "", matchedTerms = listOf("bakers"), relevanceScore = 0.6f),
        )

        val result = useCase(listOf("receipt for Bakers"))

        assertEquals(2, result.hits.size)
        assertEquals(MediaId(1L), result.hits[0].item.id)
        assertEquals(0.85f, result.hits[0].relevanceScore, 0.001f)
        assertEquals(MediaId(2L), result.hits[1].item.id)
        assertEquals(0.70f, result.hits[1].relevanceScore, 0.001f)
    }

    @Test
    fun invoke_mergesSameItemFromMultipleAlternativesWithHigherScore() = runTest {
        val fakeRepo = FakeSearchRepository()
        val useCase = VoiceSearchUseCase(fakeRepo)

        val item1 = testMediaItem(id = MediaId(1L), category = MediaCategory.PHOTO)

        fakeRepo.hitsByQuery = mapOf(
            "tehzeeb bakers" to listOf(
                SearchHit(item = item1, snippet = "", matchedTerms = listOf("tehzeeb"), relevanceScore = 0.5f)
            ),
            "zeb bakers" to listOf(
                SearchHit(item = item1, snippet = "", matchedTerms = listOf("bakers"), relevanceScore = 0.8f)
            )
        )

        val result = useCase(listOf("Tehzeeb Bakers", "Zeb Bakers"))

        assertEquals(1, result.hits.size)
        assertEquals(MediaId(1L), result.hits[0].item.id)
        assertEquals(0.8f, result.hits[0].relevanceScore, 0.001f)
    }

    @Test
    fun invoke_noKeywords_returnsEmptyHitsAndNoSearchCalls() = runTest {
        val fakeRepo = FakeSearchRepository()
        val useCase = VoiceSearchUseCase(fakeRepo)

        val result = useCase(listOf("find my photos"))

        assertTrue(fakeRepo.searchCalls.isEmpty())
        assertTrue(result.hits.isEmpty())
        assertEquals("find my photos", result.query.transcript)
        assertTrue(result.query.keywords.isEmpty())
    }
}
