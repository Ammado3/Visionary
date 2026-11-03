package com.o3.vanguard.visionary.features.onboarding.component


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.o3.vanguard.visionary.core.designsystem.AppTheme

@Composable
fun OnboardingProgress(
    activePage: Int,
    modifier: Modifier = Modifier,
    pageCount: Int = 3,
) {
    val colors = AppTheme.colorScheme
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { page ->
            val isActive = page == activePage
            Box(
                Modifier
                    .size(width = if (isActive) 28.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) colors.green else colors.mintGreen),
            )
        }
    }
}