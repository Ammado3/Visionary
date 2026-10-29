package com.o3.vanguard.visionary.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.o3.vanguard.visionary.core.designsystem.AppTheme
import com.o3.vanguard.visionary.core.designsystem.VisionaryTheme

@Composable
fun VisionaryCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = AppTheme.colorScheme.white,
    borderColor: Color = AppTheme.colorScheme.green,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    val border = BorderStroke(1.dp, borderColor)
    val colors = CardDefaults.cardColors(containerColor = backgroundColor)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border
        ) {
            content()
        }
    }
}

@Composable
fun VisionaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    VisionaryCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = AppTheme.colorScheme.black,
                modifier = Modifier.size(24.dp)
            )
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = AppTheme.colorScheme.black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = AppTheme.colorScheme.regularText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VisionaryCardPresetPreview() {
    VisionaryTheme {
        VisionaryCard(
            title = "People",
            value = "248",
            icon = Icons.Default.Group,
            modifier = Modifier.padding(16.dp),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VisionaryCardCustomContentPreview() {
    VisionaryTheme {
        VisionaryCard(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Custom Card Header",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Custom content matching exact specs.",
                    fontSize = 12.sp,
                    color = AppTheme.colorScheme.regularText
                )
            }
        }
    }
}
