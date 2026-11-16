package com.o3.vanguard.visionary.features.onboarding

import androidx.compose.runtime.Immutable
import com.o3.vanguard.visionary.core.designsystem.generated.resources.bottom_text
import com.o3.vanguard.visionary.core.designsystem.generated.resources.get_started
import com.o3.vanguard.visionary.core.designsystem.generated.resources.next
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboardingScreen_content
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboardingScreen_title
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboardingScren_bottom
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboarding_content
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboarding_heading
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboarding_title
import com.o3.vanguard.visionary.core.designsystem.generated.resources.search_your_file
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Res as DsRes
import com.o3.vanguard.visionary.features.onboarding.generated.resources.Background
import com.o3.vanguard.visionary.features.onboarding.generated.resources.Collage
import com.o3.vanguard.visionary.features.onboarding.generated.resources.Res
import com.o3.vanguard.visionary.features.onboarding.generated.resources.Search_illustration
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

@Immutable
data class OnboardingPage(
    val image: DrawableResource,
    val title: StringResource,
    val description: StringResource,
    val footer: StringResource,
    val buttonLabel: StringResource,
)

internal val onboardingPages = listOf(
    OnboardingPage(
        image = Res.drawable.Collage,
        title = DsRes.string.onboarding_title,
        description = DsRes.string.onboarding_content,
        footer = DsRes.string.bottom_text,
        buttonLabel = DsRes.string.next,
    ),
    OnboardingPage(
        image = Res.drawable.Search_illustration,
        title = DsRes.string.onboardingScreen_title,
        description = DsRes.string.onboardingScreen_content,
        footer = DsRes.string.onboardingScren_bottom,
        buttonLabel = DsRes.string.next,
    ),
    OnboardingPage(
        image = Res.drawable.Background,
        title = DsRes.string.onboarding_heading,
        description = DsRes.string.search_your_file,
        footer = DsRes.string.bottom_text,
        buttonLabel = DsRes.string.get_started,
    ),
)
