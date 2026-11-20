package com.o3.vanguard.visionary.features.search.voice

import app.cash.turbine.test
import com.o3.vanguard.visionary.core.testkit.dispatcher.TestAppDispatchers
import com.o3.vanguard.visionary.core.testkit.dispatcher.runViewModelTest
import com.o3.vanguard.visionary.core.testkit.factory.testMediaItem
import com.o3.vanguard.visionary.core.testkit.fake.FakeSearchRepository
import com.o3.vanguard.visionary.core.testkit.fake.FakeSpeechRecognizer
import com.o3.vanguard.visionary.core.testkit.mvi.collectEvents
import com.o3.vanguard.visionary.domain.model.MediaCategory
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.SearchHit
import com.o3.vanguard.visionary.domain.port.SpeechAvailability
import com.o3.vanguard.visionary.domain.port.SpeechEvent
import com.o3.vanguard.visionary.domain.port.SpeechFailure
import com.o3.vanguard.visionary.domain.voice.VoiceSearchUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VoiceSearchViewModelTest {

    @Test
    fun permissionGranted_listeningEventsUpdateStateToResults() = runViewModelTest {
        val dispatchers = TestAppDispatchers()
        val fakeSpeech = FakeSpeechRecognizer(availability = SpeechAvailability.ON_DEVICE)
        fakeSpeech.script.addAll(
            listOf(
                SpeechEvent.Ready,
                SpeechEvent.Partial("tehzeeb"),
                SpeechEvent.Final(listOf("receipt for Tehzeeb Bakers")),
            )
        )
        val fakeRepo = FakeSearchRepository()
        val item1 = testMediaItem(id = MediaId(1L), category = MediaCategory.RECEIPT)
        fakeRepo.searchHits = listOf(SearchHit(item = item1, snippet = "", matchedTerms = listOf("tehzeeb")))

        val useCase = VoiceSearchUseCase(fakeRepo)
        val viewModel = VoiceSearchViewModel(fakeSpeech, useCase, dispatchers)

        viewModel.onIntent(VoiceSearchIntent.OnStart)
        viewModel.onIntent(VoiceSearchIntent.OnPermissionResult(true))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(VoiceSearchState.Phase.Results, state.phase)
            assertEquals(1, state.hits.size)
            assertEquals(MediaId(1L), state.hits[0].item.id)
            assertEquals("tehzeeb", state.query?.keywords?.firstOrNull())
        }
    }

    @Test
    fun permissionDenied_stateBecomesNeedsPermissionAndNoListeningStarts() = runViewModelTest {
        val dispatchers = TestAppDispatchers()
        val fakeSpeech = FakeSpeechRecognizer(availability = SpeechAvailability.ON_DEVICE)
        val fakeRepo = FakeSearchRepository()
        val useCase = VoiceSearchUseCase(fakeRepo)

        val viewModel = VoiceSearchViewModel(fakeSpeech, useCase, dispatchers)

        viewModel.onIntent(VoiceSearchIntent.OnStart)
        viewModel.onIntent(VoiceSearchIntent.OnPermissionResult(false))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(VoiceSearchState.Phase.NeedsPermission, state.phase)
            assertEquals(0, fakeSpeech.listenCount)
        }
    }

    @Test
    fun speechFailed_noMatch_stateBecomesIdle() = runViewModelTest {
        val dispatchers = TestAppDispatchers()
        val fakeSpeech = FakeSpeechRecognizer(availability = SpeechAvailability.ON_DEVICE)
        fakeSpeech.script.add(SpeechEvent.Failed(SpeechFailure.NO_MATCH))
        val fakeRepo = FakeSearchRepository()
        val useCase = VoiceSearchUseCase(fakeRepo)

        val viewModel = VoiceSearchViewModel(fakeSpeech, useCase, dispatchers)

        viewModel.onIntent(VoiceSearchIntent.OnStart)
        viewModel.onIntent(VoiceSearchIntent.OnPermissionResult(true))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(VoiceSearchState.Phase.Idle, state.phase)
        }
    }

    @Test
    fun speechFailed_notAvailableOffline_stateBecomesError() = runViewModelTest {
        val dispatchers = TestAppDispatchers()
        val fakeSpeech = FakeSpeechRecognizer(availability = SpeechAvailability.SYSTEM_SERVICE)
        fakeSpeech.script.add(SpeechEvent.Failed(SpeechFailure.NOT_AVAILABLE_OFFLINE))
        val fakeRepo = FakeSearchRepository()
        val useCase = VoiceSearchUseCase(fakeRepo)

        val viewModel = VoiceSearchViewModel(fakeSpeech, useCase, dispatchers)

        viewModel.onIntent(VoiceSearchIntent.OnStart)
        viewModel.onIntent(VoiceSearchIntent.OnPermissionResult(true))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(VoiceSearchState.Phase.Error, state.phase)
        }
    }

    @Test
    fun transcriptWithNoKeywords_stateBecomesNoKeywordsAndNoSearchCalls() = runViewModelTest {
        val dispatchers = TestAppDispatchers()
        val fakeSpeech = FakeSpeechRecognizer(availability = SpeechAvailability.ON_DEVICE)
        fakeSpeech.script.add(SpeechEvent.Final(listOf("find my photos")))
        val fakeRepo = FakeSearchRepository()
        val useCase = VoiceSearchUseCase(fakeRepo)

        val viewModel = VoiceSearchViewModel(fakeSpeech, useCase, dispatchers)

        viewModel.onIntent(VoiceSearchIntent.OnStart)
        viewModel.onIntent(VoiceSearchIntent.OnPermissionResult(true))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(VoiceSearchState.Phase.NoKeywords, state.phase)
            assertTrue(fakeRepo.searchCalls.isEmpty())
        }
    }

    @Test
    fun onEditedText_searchesWithoutListening() = runViewModelTest {
        val dispatchers = TestAppDispatchers()
        val fakeSpeech = FakeSpeechRecognizer(availability = SpeechAvailability.ON_DEVICE)
        val fakeRepo = FakeSearchRepository()
        val item = testMediaItem(id = MediaId(5L))
        fakeRepo.searchHits = listOf(SearchHit(item = item, snippet = "", matchedTerms = listOf("tehzeeb")))
        val useCase = VoiceSearchUseCase(fakeRepo)

        val viewModel = VoiceSearchViewModel(fakeSpeech, useCase, dispatchers)

        viewModel.onIntent(VoiceSearchIntent.OnEditedText("tehzeeb"))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(VoiceSearchState.Phase.Results, state.phase)
            assertEquals(1, fakeRepo.searchCalls.size)
            assertEquals("tehzeeb", fakeRepo.searchCalls[0].first)
            assertEquals(0, fakeSpeech.listenCount)
        }
    }

    @Test
    fun startingAgainWhileListening_cancelsPreviousListenJob() = runViewModelTest {
        val dispatchers = TestAppDispatchers()
        val fakeSpeech = FakeSpeechRecognizer(availability = SpeechAvailability.ON_DEVICE)
        fakeSpeech.suspendForeverAfterScript = true
        fakeSpeech.script.add(SpeechEvent.Ready)

        val fakeRepo = FakeSearchRepository()
        val useCase = VoiceSearchUseCase(fakeRepo)

        val viewModel = VoiceSearchViewModel(fakeSpeech, useCase, dispatchers)

        viewModel.onIntent(VoiceSearchIntent.OnStart)
        viewModel.onIntent(VoiceSearchIntent.OnPermissionResult(true))

        // Start listening second time
        viewModel.onIntent(VoiceSearchIntent.OnStart)
        viewModel.onIntent(VoiceSearchIntent.OnPermissionResult(true))

        assertEquals(2, fakeSpeech.listenCount)
    }

    @Test
    fun onResultClicked_emitsOpenViewerEventWithFirstKeywordHighlight() = runViewModelTest {
        val dispatchers = TestAppDispatchers()
        val fakeSpeech = FakeSpeechRecognizer(availability = SpeechAvailability.ON_DEVICE)
        fakeSpeech.script.add(SpeechEvent.Final(listOf("receipt for Tehzeeb Bakers")))

        val fakeRepo = FakeSearchRepository()
        val item = testMediaItem(id = MediaId(123L), category = MediaCategory.RECEIPT)
        fakeRepo.searchHits = listOf(SearchHit(item = item, snippet = "", matchedTerms = listOf("tehzeeb")))

        val useCase = VoiceSearchUseCase(fakeRepo)
        val viewModel = VoiceSearchViewModel(fakeSpeech, useCase, dispatchers)

        val events = collectEvents(viewModel)

        viewModel.onIntent(VoiceSearchIntent.OnStart)
        viewModel.onIntent(VoiceSearchIntent.OnPermissionResult(true))

        val hit = SearchHit(item = item, snippet = "", matchedTerms = listOf("tehzeeb"))
        viewModel.onIntent(VoiceSearchIntent.OnResultClicked(hit))

        val openViewerEvents = events.filterIsInstance<VoiceSearchEvent.OpenViewer>()
        assertEquals(1, openViewerEvents.size)
        assertEquals(VoiceSearchEvent.OpenViewer(MediaId(123L), "tehzeeb"), openViewerEvents[0])
    }
}
