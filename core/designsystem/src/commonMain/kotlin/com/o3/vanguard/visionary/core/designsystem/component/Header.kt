package com.o3.vanguard.visionary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import com.o3.vanguard.visionary.core.designsystem.AppTheme
import com.o3.vanguard.visionary.core.designsystem.VisionaryTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VisionaryHeader(
    title: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = AppTheme.colorScheme.white,
    contentColor: Color = AppTheme.colorScheme.black,
    navigationIcon: ImageVector? = Icons.Default.Menu,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = {
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = contentColor
            )
        }
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = contentColor
            )
        }
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = contentColor
            )
        }
    }
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .height(56.dp)
            .background(backgroundColor)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional Navigation / Leading Icon
            if (navigationIcon != null) {
                IconButton(onClick = { onNavigationClick?.invoke() }) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Navigation Icon",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Title
            Text(
                text = title,
                color = contentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (navigationIcon != null) 8.dp else 16.dp)
            )

            // Optional Actions / Trailing Icons
            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VisionaryHeaderPreview() {
    VisionaryTheme {
        VisionaryHeader(
            title = "Gallery",
            onNavigationClick = {},
            actions = {

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AppTheme.colorScheme.black
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = AppTheme.colorScheme.black
                    )
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VisionaryHeaderMinimalPreview() {
    VisionaryTheme {
        VisionaryHeader(
            title = "Gallery",
            navigationIcon = null,
            actions = null
        )
    }
}
