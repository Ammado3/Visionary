package com.o3.vanguard.visionary.domain

import com.o3.vanguard.visionary.core.testkit.factory.testAppSettings
import com.o3.vanguard.visionary.core.testkit.fake.FakeSettingsRepository
import com.o3.vanguard.visionary.domain.usecase.ResolveStartRouteUseCase
import com.o3.vanguard.visionary.domain.usecase.StartDestination
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveStartRouteUseCaseTest {

    @Test
    fun useCase_onboardingNotDone_returnsOnboarding() = runTest {
        val fakeSettingsRepository = FakeSettingsRepository(initial = testAppSettings(onboardingDone = false))
        val useCase = ResolveStartRouteUseCase(fakeSettingsRepository)

        val destination = useCase()

        assertEquals(StartDestination.ONBOARDING, destination)
    }

    @Test
    fun useCase_onboardingDone_returnsHome() = runTest {
        val fakeSettingsRepository = FakeSettingsRepository(initial = testAppSettings(onboardingDone = true))
        val useCase = ResolveStartRouteUseCase(fakeSettingsRepository)

        val destination = useCase()

        assertEquals(StartDestination.HOME, destination)
    }
}
