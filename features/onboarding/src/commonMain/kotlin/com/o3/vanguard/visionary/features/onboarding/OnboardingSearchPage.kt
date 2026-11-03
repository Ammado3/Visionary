package com.o3.vanguard.visionary.features.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.o3.vanguard.visionary.core.designsystem.AppTheme
import com.o3.vanguard.visionary.core.designsystem.VisionaryTheme
import com.o3.vanguard.visionary.features.onboarding.component.OnboardingHeader
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.o3.vanguard.visionary.features.onboarding.generated.resources.Res
import com.o3.vanguard.visionary.features.onboarding.generated.resources.Search_illustration
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Res as DsRes
import com.o3.vanguard.visionary.core.designsystem.generated.resources.next
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboardingScreen_content
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboardingScreen_title
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboardingScren_bottom
import com.o3.vanguard.visionary.core.designsystem.generated.resources.splash_title

@Preview(showBackground = true)
@Composable
private fun OnboardingSearchPagePreview() {
    VisionaryTheme {
        OnboardingSearchPage(onNext = {}, onSkip = {})
    }
}

@Composable
fun OnboardingSearchPage(
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colorScheme
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
            onSkip = onSkip,
            title = stringResource(DsRes.string.splash_title),
            textColor = colors.black,
            skipColor = colors.regularText,
        )

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(24.dp))
            SearchIllustration(Modifier.fillMaxWidth().height(232.dp))
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(DsRes.string.onboardingScreen_title),
                    color = colors.black,
                    fontSize = 30.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.75).sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(DsRes.string.onboardingScreen_content),
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
            OnboardingProgress(activePage = 1)
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.green,
                    contentColor = colors.white,
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(stringResource(DsRes.string.next), fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                stringResource(DsRes.string.onboardingScren_bottom),
                color = colors.regularText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OnboardingProgress(activePage: Int) {
    val colors = AppTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { page ->
            Box(
                Modifier
                    .size(width = if (page == activePage) 28.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (page == activePage) colors.green else colors.paleGreen),
            )
        }
    }
}

@Composable
private fun SearchIllustration(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
    ) {
        if (maxWidth > 0.dp && maxHeight > 0.dp) {
            Image(
                painter = painterResource(Res.drawable.Search_illustration),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
