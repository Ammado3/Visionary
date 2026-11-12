package com.o3.vanguard.visionary.features.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.designsystem.AppTheme
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Res as DsRes
import com.o3.vanguard.visionary.core.designsystem.generated.resources.splash_title
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.features.onboarding.component.OnboardingHeader
import com.o3.vanguard.visionary.features.onboarding.component.OnboardingProgress
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoute(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            OnboardingEvent.NavigateToHome -> onComplete()
        }
    }
    OnboardingScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colorScheme
    val pageIndex = state.currentPage.coerceIn(onboardingPages.indices)
    val page = onboardingPages[pageIndex]
    val isLastPage = pageIndex == onboardingPages.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.white)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        OnboardingHeader(
            onSkip = { onIntent(OnboardingIntent.OnCompleteClicked) },
            title = stringResource(DsRes.string.splash_title),
            textColor = colors.black,
            skipColor = colors.regularText,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(pageIndex) {
                    var horizontalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount -> horizontalDrag += dragAmount },
                        onDragEnd = {
                            when {
                                horizontalDrag < -80f && pageIndex < onboardingPages.lastIndex ->
                                    onIntent(OnboardingIntent.OnNextPageClicked)
                                horizontalDrag > 80f && pageIndex > 0 ->
                                    onIntent(OnboardingIntent.OnPreviousPageClicked)
                            }
                        },
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(24.dp))
            Image(
                painter = painterResource(page.image),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxWidth().height(if (isLastPage) 256.dp else 232.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(page.title),
                    color = colors.black,
                    fontSize = 30.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.75).sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(page.description),
                    color = colors.regularText,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            OnboardingProgress(activePage = pageIndex, pageCount = onboardingPages.size)
            Button(
                onClick = {
                    onIntent(
                        if (isLastPage) OnboardingIntent.OnCompleteClicked
                        else OnboardingIntent.OnNextPageClicked,
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.green,
                    contentColor = colors.white,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(
                    text = stringResource(page.buttonLabel),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(page.footer),
                color = colors.regularText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }
}
