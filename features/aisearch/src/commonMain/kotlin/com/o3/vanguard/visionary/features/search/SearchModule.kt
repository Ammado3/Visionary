package com.o3.vanguard.visionary.features.search

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val searchModule = module {
    viewModel { (initialQuery: String) ->
        SearchViewModel(
            initialQuery = initialQuery,
            searchMediaUseCase = get(),
            observeMediaUseCase = get(),
            observeIndexProgressUseCase = get(),
            rebuildIndexUseCase = get(),
            searchRepository = get(),
        )
    }
}
