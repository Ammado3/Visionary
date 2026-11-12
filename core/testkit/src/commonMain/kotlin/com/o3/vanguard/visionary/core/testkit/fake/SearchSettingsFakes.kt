package com.o3.vanguard.visionary.core.testkit.fake

import com.o3.vanguard.visionary.domain.model.AppSettings
import com.o3.vanguard.visionary.domain.model.SearchFilter
import com.o3.vanguard.visionary.domain.model.SearchHit
import com.o3.vanguard.visionary.domain.model.SearchMode
import com.o3.vanguard.visionary.domain.repository.SearchRepository
import com.o3.vanguard.visionary.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSearchRepository : SearchRepository {
    val searchCalls = mutableListOf<Triple<String, SearchFilter, SearchMode>>()
    val recentSearches = MutableStateFlow<List<String>>(emptyList())
    val saveRecentSearchCalls = mutableListOf<String>()
    var searchHits: List<SearchHit> = emptyList()

    override fun search(query: String, filter: SearchFilter, mode: SearchMode): Flow<List<SearchHit>> {
        searchCalls.add(Triple(query, filter, mode))
        return MutableStateFlow(searchHits)
    }

    override fun observeRecentSearches(): Flow<List<String>> = recentSearches

    override suspend fun saveRecentSearch(query: String) {
        saveRecentSearchCalls.add(query)
        recentSearches.update { (listOf(query) + it).distinct() }
    }
}

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    val settings = MutableStateFlow(initial)
    val updates = mutableListOf<AppSettings>()

    override fun observe(): Flow<AppSettings> = settings

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settings.update(transform)
        updates.add(settings.value)
    }
}
