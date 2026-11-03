package com.o3.vanguard.visionary.features.onboarding.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.o3.vanguard.visionary.features.onboarding.generated.resources.Res
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Res as DsRes
import com.o3.vanguard.visionary.features.onboarding.generated.resources.onboarding_brand_icon
import com.o3.vanguard.visionary.core.designsystem.generated.resources.onboarding_text

@Preview
@Composable
private fun OnboardingHeaderPreview() {
    OnboardingHeader(onSkip = {})
}


@Composable
fun OnboardingHeader(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "AI Gallery",
    textColor: Color = Color(0xFF0B1B14),
    skipColor: Color = Color(0xFF496156),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(Res.drawable.onboarding_brand_icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = textColor,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
            )
        }
        Text(
            text = stringResource(DsRes.string.onboarding_text),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onSkip)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            color = skipColor,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}
