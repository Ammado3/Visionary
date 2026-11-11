package com.o3.vanguard.visionary.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTypography {
    private val sansSerif = FontFamily.SansSerif

    val displayLarge = TextStyle(
        fontFamily = sansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 37.sp,
        letterSpacing = (-0.75).sp,
    )

    val displayMedium = TextStyle(
        fontFamily = sansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.6).sp,
    )

    val screenTitle = TextStyle(
        fontFamily = sansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.45).sp,
    )

    val titleMedium = TextStyle(
        fontFamily = sansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    val titleSmall = TextStyle(
        fontFamily = sansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    val bodyLarge = TextStyle(
        fontFamily = sansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 28.sp,
    )

    val bodyMedium = TextStyle(
        fontFamily = sansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 24.sp,
    )

    val bodySmall = TextStyle(
        fontFamily = sansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    val labelLarge = TextStyle(
        fontFamily = sansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    val labelMedium = TextStyle(
        fontFamily = sansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    val labelSmall = TextStyle(
        fontFamily = sansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    )

    val button = TextStyle(
        fontFamily = sansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    val navigationLabel = labelSmall

    val material = Typography(
        displayLarge = displayLarge,
        displayMedium = displayMedium,
        displaySmall = displayMedium,
        headlineLarge = displayMedium,
        headlineMedium = screenTitle,
        headlineSmall = screenTitle,
        titleLarge = screenTitle,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
    )
}
