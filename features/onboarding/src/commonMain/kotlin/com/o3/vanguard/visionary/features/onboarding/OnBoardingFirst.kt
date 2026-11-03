package com.o3.vanguard.visionary.features.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboarding_bottom
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboarding_content
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboarding_title
import com.o3.vanguard.visionary.core.designsystem.generated.resources.splash_title

@Preview(showBackground = true)
@Composable
private fun OnBoardingFirstPreview() {
    VisionaryTheme {
        OnBoardingFirst(onNext = {}, onSkip = {})
    }
}

@Composable
fun OnBoardingFirst(
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

        Spacer(Modifier.height(24.dp))
        OnboardingPhotoCollage(Modifier.fillMaxWidth().height(232.dp))

        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(DsRes.string.onboarding_title),
                color = colors.black,
                fontSize = 30.sp,
                lineHeight = 37.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.75).sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(DsRes.string.onboarding_content),
                color = colors.regularText,
                fontSize = 16.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(width = 24.dp, height = 8.dp).clip(RoundedCornerShape(4.dp))
                    .background(colors.green)
            )
            Spacer(Modifier.width(8.dp))
            repeat(2) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(colors.mintGreen))
                if (it == 0) Spacer(Modifier.width(8.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.green,
                contentColor = colors.white,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 24.dp,
                vertical = 16.dp
            ),
        ) {
            Text(stringResource(DsRes.string.next), fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("\uD83D\uDD12", color = colors.regularText, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(DsRes.string.onboarding_bottom),
                color = colors.regularText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun OnboardingPhotoCollage(modifier: Modifier = Modifier) {
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
