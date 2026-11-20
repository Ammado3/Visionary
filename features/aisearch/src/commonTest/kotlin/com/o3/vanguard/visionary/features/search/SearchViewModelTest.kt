package com.o3.vanguard.visionary.features.search

import app.cash.turbine.test
import com.o3.vanguard.visionary.core.testkit.dispatcher.runViewModelTest
import com.o3.vanguard.visionary.core.testkit.factory.testMediaItem
import com.o3.vanguard.visionary.core.testkit.fake.FakeBackgroundSyncScheduler
import com.o3.vanguard.visionary.core.testkit.fake.FakeIndexRepository
import com.o3.vanguard.visionary.core.testkit.fake.FakeMediaDeleteGateway
import com.o3.vanguard.visionary.core.testkit.fake.FakeMediaRepository
import com.o3.vanguard.visionary.core.testkit.fake.FakeSearchRepository
import com.o3.vanguard.visionary.core.testkit.fake.FakeTrashRepository
import com.o3.vanguard.visionary.core.testkit.mvi.collectEvents
import com.o3.vanguard.visionary.domain.model.CollectionKind
import com.o3.vanguard.visionary.domain.model.DeleteConsentResult
import com.o3.vanguard.visionary.domain.model.IndexProgress
import com.o3.vanguard.visionary.domain.model.IndexState
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.SearchHit
import com.o3.vanguard.visionary.domain.usecase.DeleteMediaUseCase
import com.o3.vanguard.visionary.domain.usecase.ObserveIndexProgressUseCase
import com.o3.vanguard.visionary.domain.usecase.ObserveMediaUseCase
import com.o3.vanguard.visionary.domain.usecase.SearchMediaUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchViewModelTest {

    private fun createViewModel(
        searchRepository: FakeSearchRepository,
        mediaRepository: FakeMediaRepository,
        indexRepository: FakeIndexRepository,
        trashRepository: FakeTrashRepository,
        deleteGateway: FakeMediaDeleteGateway,
        initialQuery: String = "cat",
        indexScheduler: FakeBackgroundSyncScheduler = FakeBackgroundSyncScheduler(),
    ): SearchViewModel {
        val searchMediaUseCase = SearchMediaUseCase(searchRepository)
        val observeMediaUseCase = ObserveMediaUseCase(mediaRepository)
        val observeIndexProgressUseCase = ObserveIndexProgressUseCase(indexRepository)
        val deleteMediaUseCase = DeleteMediaUseCase(deleteGateway, mediaRepository, trashRepository)

        return SearchViewModel(
            initialQuery = initialQuery,
            searchMediaUseCase = searchMediaUseCase,
            observeMediaUseCase = observeMediaUseCase,
            observeIndexProgressUseCase = observeIndexProgressUseCase,
            indexScheduler = indexScheduler,
            searchRepository = searchRepository,
            deleteMediaUseCase = deleteMediaUseCase,
        )
    }

    @Test
    fun onDeleteImage_withDeletedResult_removesItemAndEmitsToastAndRequestsDelete() = runViewModelTest {
        val searchRepository = FakeSearchRepository()
        val mediaRepository = FakeMediaRepository()
        val indexRepository = FakeIndexRepository()
        val trashRepository = FakeTrashRepository()
        val deleteGateway = FakeMediaDeleteGateway()
        deleteGateway.nextResult = DeleteConsentResult.Deleted

        val item = testMediaItem(id = MediaId(1L), uri = "content://media/1")
        val hit = SearchHit(item = item, snippet = "cat picture", matchedTerms = listOf("cat"))
        searchRepository.searchHits = listOf(hit)
        mediaRepository.mediaItemsByKind.value = mapOf(CollectionKind.ALL to listOf(item))

        val viewModel = createViewModel(
            searchRepository = searchRepository,
            mediaRepository = mediaRepository,
            indexRepository = indexRepository,
            trashRepository = trashRepository,
            deleteGateway = deleteGateway,
            initialQuery = "cat",
        )
        testScheduler.advanceUntilIdle()

        val events = collectEvents(viewModel)

        viewModel.onIntent(SearchIntent.OnDeleteImage(item))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.results.isEmpty())
            assertTrue(state.allImages.isEmpty())
        }

        assertEquals(1, events.size)
        assertEquals(SearchEvent.ShowToast("Image moved to Bin"), events[0])
        assertEquals(1, deleteGateway.deleteRequests.size)
        assertEquals(listOf("content://media/1"), deleteGateway.deleteRequests[0])
    }

    @Test
    fun onDeleteImage_withDeclinedByUserResult_keepsItemAndEmitsNoToast() = runViewModelTest {
        val searchRepository = FakeSearchRepository()
        val mediaRepository = FakeMediaRepository()
        val indexRepository = FakeIndexRepository()
        val trashRepository = FakeTrashRepository()
        val deleteGateway = FakeMediaDeleteGateway()
        deleteGateway.nextResult = DeleteConsentResult.DeclinedByUser

        val item = testMediaItem(id = MediaId(1L), uri = "content://media/1")
        val hit = SearchHit(item = item, snippet = "cat picture", matchedTerms = listOf("cat"))
        searchRepository.searchHits = listOf(hit)
        mediaRepository.mediaItemsByKind.value = mapOf(CollectionKind.ALL to listOf(item))

        val viewModel = createViewModel(
            searchRepository = searchRepository,
            mediaRepository = mediaRepository,
            indexRepository = indexRepository,
            trashRepository = trashRepository,
            deleteGateway = deleteGateway,
            initialQuery = "cat",
        )
        testScheduler.advanceUntilIdle()

        val events = collectEvents(viewModel)

        viewModel.onIntent(SearchIntent.OnDeleteImage(item))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.results.size)
            assertEquals(item.id, state.results[0].item.id)
        }

        assertTrue(events.isEmpty())
    }

    @Test
    fun onDeleteImage_withNotPossibleResult_keepsItemAndEmitsErrorToast() = runViewModelTest {
        val searchRepository = FakeSearchRepository()
        val mediaRepository = FakeMediaRepository()
        val indexRepository = FakeIndexRepository()
        val trashRepository = FakeTrashRepository()
        val deleteGateway = FakeMediaDeleteGateway()
        deleteGateway.nextResult = DeleteConsentResult.NotPossible("Permission denied")

        val item = testMediaItem(id = MediaId(1L), uri = "content://media/1")
        val hit = SearchHit(item = item, snippet = "cat picture", matchedTerms = listOf("cat"))
        searchRepository.searchHits = listOf(hit)
        mediaRepository.mediaItemsByKind.value = mapOf(CollectionKind.ALL to listOf(item))

        val viewModel = createViewModel(
            searchRepository = searchRepository,
            mediaRepository = mediaRepository,
            indexRepository = indexRepository,
            trashRepository = trashRepository,
            deleteGateway = deleteGateway,
            initialQuery = "cat",
        )
        testScheduler.advanceUntilIdle()

        val events = collectEvents(viewModel)

        viewModel.onIntent(SearchIntent.OnDeleteImage(item))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.results.size)
            assertEquals(item.id, state.results[0].item.id)
        }

        assertEquals(1, events.size)
        assertEquals(SearchEvent.ShowToast("Couldn't delete image"), events[0])
    }

    @Test
    fun onReindexClicked_requestsIndexingThroughTheSchedulerAndShowsIndexing() = runViewModelTest {
        val indexRepository = FakeIndexRepository()
        val indexScheduler = FakeBackgroundSyncScheduler()
        val viewModel = createViewModel(
            searchRepository = FakeSearchRepository(),
            mediaRepository = FakeMediaRepository(),
            indexRepository = indexRepository,
            trashRepository = FakeTrashRepository(),
            deleteGateway = FakeMediaDeleteGateway(),
            initialQuery = "",
            indexScheduler = indexScheduler,
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(SearchIntent.OnReindexClicked)

        assertEquals(1, indexScheduler.requestIndexingCalls)
        assertEquals(0, indexRepository.rebuildCalls)
        assertTrue(viewModel.state.value.isIndexing)
        assertEquals(null, viewModel.state.value.errorMessage)
    }

    @Test
    fun onReindexClicked_passEndsWithoutRunning_isIndexingBecomesFalse() = runViewModelTest {
        val indexRepository = FakeIndexRepository()
        val viewModel = createViewModel(
            searchRepository = FakeSearchRepository(),
            mediaRepository = FakeMediaRepository(),
            indexRepository = indexRepository,
            trashRepository = FakeTrashRepository(),
            deleteGateway = FakeMediaDeleteGateway(),
            initialQuery = "",
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(SearchIntent.OnReindexClicked)
        assertTrue(viewModel.state.value.isIndexing)

        indexRepository.progress.value = IndexProgress(
            total = 0, indexed = 0, pending = 0, failed = 0,
            state = IndexState.IDLE, completedPasses = 1, lastPassFailed = false
        )
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isIndexing)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun onReindexClicked_passEndsWithFailure_showsErrorMessageAndIsIndexingBecomesFalse() = runViewModelTest {
        val indexRepository = FakeIndexRepository()
        val viewModel = createViewModel(
            searchRepository = FakeSearchRepository(),
            mediaRepository = FakeMediaRepository(),
            indexRepository = indexRepository,
            trashRepository = FakeTrashRepository(),
            deleteGateway = FakeMediaDeleteGateway(),
            initialQuery = "",
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(SearchIntent.OnReindexClicked)
        assertTrue(viewModel.state.value.isIndexing)

        indexRepository.progress.value = IndexProgress(
            total = 0, indexed = 0, pending = 0, failed = 1,
            state = IndexState.IDLE, completedPasses = 1, lastPassFailed = true
        )
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isIndexing)
        assertEquals("Indexing failed", viewModel.state.value.errorMessage)
    }

    @Test
    fun onReindexClicked_passGoesRunningThenIdle_isIndexingIsTrueWhileRunningAndFalseAfter() = runViewModelTest {
        val indexRepository = FakeIndexRepository()
        val viewModel = createViewModel(
            searchRepository = FakeSearchRepository(),
            mediaRepository = FakeMediaRepository(),
            indexRepository = indexRepository,
            trashRepository = FakeTrashRepository(),
            deleteGateway = FakeMediaDeleteGateway(),
            initialQuery = "",
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(SearchIntent.OnReindexClicked)
        assertTrue(viewModel.state.value.isIndexing)

        indexRepository.progress.value = IndexProgress(
            total = 10, indexed = 2, pending = 8, failed = 0,
            state = IndexState.RUNNING, completedPasses = 0, lastPassFailed = false
        )
        testScheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isIndexing)

        indexRepository.progress.value = IndexProgress(
            total = 10, indexed = 10, pending = 0, failed = 0,
            state = IndexState.IDLE, completedPasses = 1, lastPassFailed = false
        )
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isIndexing)
        assertNull(viewModel.state.value.errorMessage)
    }
}
