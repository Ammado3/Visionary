package com.o3.vanguard.visionary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.o3.vanguard.visionary.core.designsystem.AppTheme

/**
 * A reusable page shell for screens with standard title, description, and custom content.
 */
@Composable
fun BaseTextScreen(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    backgroundColor: Color = AppTheme.colorScheme.white,
    titleColor: Color = AppTheme.colorScheme.black,
    descriptionColor: Color = AppTheme.colorScheme.regularText,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
    contentSpacing: Dp = 16.dp,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .systemBarsPadding()
            .padding(contentPadding)
            .then(scrollModifier),
    ) {
        Text(
            text = title,
            color = titleColor,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        )

        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                modifier = Modifier.padding(top = 8.dp),
                color = descriptionColor,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            )
        }

        Spacer(modifier = Modifier.height(contentSpacing))

        content()
    }
}