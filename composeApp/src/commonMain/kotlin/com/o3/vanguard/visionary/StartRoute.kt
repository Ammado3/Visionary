package com.o3.vanguard.visionary

import com.o3.vanguard.visionary.core.navigation.AppRoute
import com.o3.vanguard.visionary.domain.model.MediaAccess
import com.o3.vanguard.visionary.domain.usecase.StartDestination

internal fun chooseStartRoute(destination: StartDestination, access: MediaAccess): AppRoute =
    when {
        destination == StartDestination.ONBOARDING -> AppRoute.Onboarding()
        access is MediaAccess.Full || access is MediaAccess.Partial -> AppRoute.Home
        else -> AppRoute.Onboarding(skipIntro = true)
    }
