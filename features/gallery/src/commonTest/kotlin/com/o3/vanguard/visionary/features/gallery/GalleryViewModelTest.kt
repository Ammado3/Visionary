package com.o3.vanguard.visionary.features.gallery

import app.cash.turbine.test
import com.o3.vanguard.visionary.core.testkit.dispatcher.runViewModelTest
import com.o3.vanguard.visionary.core.testkit.factory.testMediaItem
import com.o3.vanguard.visionary.core.testkit.fake.FakeMediaRepository
import com.o3.vanguard.visionary.core.testkit.mvi.collectEvents
import com.o3.vanguard.visionary.domain.model.CollectionKind
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.usecase.ObserveMediaUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GalleryViewModelTest {

    @Test
    fun viewModel_observeMedia_itemsLoadAndLoadingBecomesFalse() = runViewModelTest {
        val fakeRepository = FakeMediaRepository()
        val item = testMediaItem(id = MediaId(10L))
        fakeRepository.mediaItemsByKind.value = mapOf(CollectionKind.ALL to listOf(item))

        val observeMediaUseCase = ObserveMediaUseCase(fakeRepository)
        val viewModel = GalleryViewModel(kindId = "all", observeMediaUseCase = observeMediaUseCase)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.loading)
            assertEquals(1, state.items.size)
            assertEquals(MediaId(10L), state.items[0].id)
        }
    }

    @Test
    fun viewModel_onItemClicked_emitsOpenViewerEvent() = runViewModelTest {
        val fakeRepository = FakeMediaRepository()
        val observeMediaUseCase = ObserveMediaUseCase(fakeRepository)
        val viewModel = GalleryViewModel(kindId = "all", observeMediaUseCase = observeMediaUseCase)

        val events = collectEvents(viewModel)

        viewModel.onIntent(GalleryIntent.OnItemClicked(MediaId(42L)))

        assertEquals(1, events.size)
        assertEquals(GalleryEvent.OpenViewer(MediaId(42L)), events[0])
    }

    @Test
    fun viewModel_onBackClicked_emitsNavigateBackEvent() = runViewModelTest {
        val fakeRepository = FakeMediaRepository()
        val observeMediaUseCase = ObserveMediaUseCase(fakeRepository)
        val viewModel = GalleryViewModel(kindId = "all", observeMediaUseCase = observeMediaUseCase)

        val events = collectEvents(viewModel)

        viewModel.onIntent(GalleryIntent.OnBackClicked)

        assertEquals(1, events.size)
        assertEquals(GalleryEvent.NavigateBack, events[0])
    }
}
