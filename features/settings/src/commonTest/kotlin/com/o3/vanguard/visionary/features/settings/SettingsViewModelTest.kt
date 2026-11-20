package com.o3.vanguard.visionary.features.settings

import com.o3.vanguard.visionary.core.testkit.dispatcher.runViewModelTest
import com.o3.vanguard.visionary.core.testkit.factory.testAppSettings
import com.o3.vanguard.visionary.core.testkit.fake.FakeBackgroundSyncScheduler
import com.o3.vanguard.visionary.core.testkit.fake.FakeIndexRepository
import com.o3.vanguard.visionary.core.testkit.fake.FakeSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {

    private val settingsRepository = FakeSettingsRepository()
    private val indexRepository = FakeIndexRepository()
    private val indexScheduler = FakeBackgroundSyncScheduler()

    private fun createViewModel() = SettingsViewModel(
        settingsRepository = settingsRepository,
        indexRepository = indexRepository,
        indexScheduler = indexScheduler,
    )

    @Test
    fun visualAnalysisOn_resumesAndRequestsIndexingThroughTheScheduler() = runViewModelTest {
        settingsRepository.settings.value = testAppSettings(visualAnalysisEnabled = false)
        val viewModel = createViewModel()

        viewModel.onIntent(SettingsIntent.OnVisualAnalysisToggled(enabled = true))

        assertTrue(settingsRepository.settings.value.visualAnalysisEnabled)
        assertEquals(1, indexRepository.resumeCalls)
        assertEquals(1, indexScheduler.requestIndexingCalls)
        assertEquals(0, indexRepository.rebuildCalls)
        assertEquals(0, indexRepository.pauseCalls)
    }

    @Test
    fun visualAnalysisOff_pausesAndDoesNotRequestIndexing() = runViewModelTest {
        val viewModel = createViewModel()

        viewModel.onIntent(SettingsIntent.OnVisualAnalysisToggled(enabled = false))

        assertFalse(settingsRepository.settings.value.visualAnalysisEnabled)
        assertEquals(1, indexRepository.pauseCalls)
        assertEquals(0, indexRepository.resumeCalls)
        assertEquals(0, indexScheduler.requestIndexingCalls)
    }
}
