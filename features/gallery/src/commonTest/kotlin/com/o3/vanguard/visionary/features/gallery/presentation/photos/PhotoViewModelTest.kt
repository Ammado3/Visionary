package com.o3.vanguard.visionary.features.gallery.presentation.photos

import app.cash.turbine.test
import com.o3.vanguard.visionary.core.testkit.dispatcher.runViewModelTest
import com.o3.vanguard.visionary.core.testkit.fake.FakeIndexRepository
import com.o3.vanguard.visionary.core.testkit.fake.FakeMediaDeleteGateway
import com.o3.vanguard.visionary.core.testkit.fake.FakeMediaRepository
import com.o3.vanguard.visionary.core.testkit.fake.FakeSettingsRepository
import com.o3.vanguard.visionary.core.testkit.fake.FakeSpeechRecognizer
import com.o3.vanguard.visionary.core.testkit.fake.FakeTrashRepository
import com.o3.vanguard.visionary.domain.port.ShareGateway
import com.o3.vanguard.visionary.domain.port.SpeechAvailability
import com.o3.vanguard.visionary.domain.usecase.DeleteMediaUseCase
import com.o3.vanguard.visionary.domain.usecase.ObserveIndexProgressUseCase
import com.o3.vanguard.visionary.domain.usecase.ObserveMediaUseCase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhotoViewModelTest {

    private val fakeShareGateway = object : ShareGateway {
        override fun share(uris: List<String>) {}
    }

    @Test
    fun init_voiceUnavailable_voiceAvailableIsFalse() = runViewModelTest {
        val fakeRepo = FakeMediaRepository()
        val fakeIndexRepo = FakeIndexRepository()
        val fakeDeleteGateway = FakeMediaDeleteGateway()
        val fakeTrashRepo = FakeTrashRepository()
        val observeMedia = ObserveMediaUseCase(fakeRepo)
        val observeIndex = ObserveIndexProgressUseCase(fakeIndexRepo)
        val deleteMedia = DeleteMediaUseCase(fakeDeleteGateway, fakeRepo, fakeTrashRepo)
        val settingsRepo = FakeSettingsRepository()
        val fakeSpeech = FakeSpeechRecognizer(availability = SpeechAvailability.UNAVAILABLE)

        val viewModel = PhotoViewModel(
            observeMedia = observeMedia,
            observeIndex = observeIndex,
            deleteMedia = deleteMedia,
            shareGateway = fakeShareGateway,
            settingsRepository = settingsRepo,
            speechRecognizerPort = fakeSpeech,
        )

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.voiceAvailable)
        }
    }

    @Test
    fun init_voiceAvailable_voiceAvailableIsTrue() = runViewModelTest {
        val fakeRepo = FakeMediaRepository()
        val fakeIndexRepo = FakeIndexRepository()
        val fakeDeleteGateway = FakeMediaDeleteGateway()
        val fakeTrashRepo = FakeTrashRepository()
        val observeMedia = ObserveMediaUseCase(fakeRepo)
        val observeIndex = ObserveIndexProgressUseCase(fakeIndexRepo)
        val deleteMedia = DeleteMediaUseCase(fakeDeleteGateway, fakeRepo, fakeTrashRepo)
        val settingsRepo = FakeSettingsRepository()
        val fakeSpeech = FakeSpeechRecognizer(availability = SpeechAvailability.ON_DEVICE)

        val viewModel = PhotoViewModel(
            observeMedia = observeMedia,
            observeIndex = observeIndex,
            deleteMedia = deleteMedia,
            shareGateway = fakeShareGateway,
            settingsRepository = settingsRepo,
            speechRecognizerPort = fakeSpeech,
        )

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.voiceAvailable)
        }
    }
}
