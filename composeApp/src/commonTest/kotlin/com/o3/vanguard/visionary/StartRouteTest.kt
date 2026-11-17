package com.o3.vanguard.visionary

import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.domain.model.MediaAccess
import com.o3.vanguard.visionary.domain.usecase.StartDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class StartRouteTest {

    @Test
    fun onboardingNotDone_withFullAccess_returnsOnboardingWithoutSkipIntro() {
        val route = chooseStartRoute(StartDestination.ONBOARDING, MediaAccess.Full)
        assertEquals(AppRoute.Onboarding(skipIntro = false), route)
    }

    @Test
    fun onboardingDone_withFullAccess_returnsHome() {
        val route = chooseStartRoute(StartDestination.HOME, MediaAccess.Full)
        assertEquals(AppRoute.Home, route)
    }

    @Test
    fun onboardingDone_withPartialAccess_returnsHome() {
        val route = chooseStartRoute(StartDestination.HOME, MediaAccess.Partial(10))
        assertEquals(AppRoute.Home, route)
    }

    @Test
    fun onboardingDone_withDeniedAccess_returnsOnboardingWithSkipIntro() {
        val route = chooseStartRoute(StartDestination.HOME, MediaAccess.Denied)
        assertEquals(AppRoute.Onboarding(skipIntro = true), route)
    }
}
