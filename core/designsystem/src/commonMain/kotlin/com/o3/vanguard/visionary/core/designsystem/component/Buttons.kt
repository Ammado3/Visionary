package com.o3.vanguard.visionary.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import com.o3.vanguard.visionary.core.designsystem.AppTheme
import com.o3.vanguard.visionary.core.designsystem.VisionaryTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Res
import com.o3.vanguard.visionary.core.designsystem.generated.resources.next

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colorScheme.green,
            contentColor = AppTheme.colorScheme.white,
            disabledContainerColor = AppTheme.colorScheme.green.copy(
                alpha = 0.5f
            ),
            disabledContentColor = AppTheme.colorScheme.white.copy(
                alpha = 0.7f
            )
        ),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = 16.dp,
            start = 24.dp,
            end = 24.dp
        )
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
fun PrimaryButtonPreview() {
    VisionaryTheme {
        PrimaryButton(
            text = stringResource(Res.string.next),
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}