package com.o3.vanguard.visionary.domain.voice

import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.SearchFilter
import com.o3.vanguard.visionary.domain.model.SearchHit
import com.o3.vanguard.visionary.domain.model.SearchMode
import com.o3.vanguard.visionary.domain.repository.SearchRepository
import kotlinx.coroutines.flow.first

data class VoiceSearchResult(
    val query: VoiceQuery,
    val hits: List<SearchHit>,
)

class VoiceSearchUseCase(
    private val searchRepository: SearchRepository,
) {
    suspend operator fun invoke(alternatives: List<String>): VoiceSearchResult {
        val nonBlankAlts = alternatives.filter { it.isNotBlank() }.take(3)
        val parsedQueries = nonBlankAlts.map { VoiceQueryParser.parse(it) }
        val queriesWithKeywords = parsedQueries
            .filter { it.keywords.isNotEmpty() }
            .distinctBy { it.searchText }

        if (queriesWithKeywords.isEmpty()) {
            val fallbackAlt = nonBlankAlts.firstOrNull() ?: ""
            return VoiceSearchResult(
                query = VoiceQueryParser.parse(fallbackAlt),
                hits = emptyList(),
            )
        }

        val chosenQuery = queriesWithKeywords.first()

        val mergedHitsById = mutableMapOf<MediaId, SearchHit>()

        for (q in queriesWithKeywords) {
            val hits = searchRepository.search(q.searchText, SearchFilter.ALL, SearchMode.FUZZY).first()
            for (hit in hits) {
                val existing = mergedHitsById[hit.item.id]
                if (existing == null || hit.relevanceScore > existing.relevanceScore) {
                    mergedHitsById[hit.item.id] = hit
                }
            }
        }

        val boostedHits = mergedHitsById.values.map { hit ->
            val boost = if (chosenQuery.categoryHints.contains(hit.item.category)) 0.25f else 0f
            hit.copy(relevanceScore = hit.relevanceScore + boost)
        }.sortedWith(
            compareByDescending<SearchHit> { it.relevanceScore }
                .thenByDescending { it.item.takenAtMs }
        ).take(60)

        return VoiceSearchResult(
            query = chosenQuery,
            hits = boostedHits,
        )
    }
}
