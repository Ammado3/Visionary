package com.o3.vanguard.visionary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.o3.vanguard.visionary.core.designsystem.AppTheme
import com.o3.vanguard.visionary.core.designsystem.VisionaryTheme
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

@Composable
fun VisionaryAlbumHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    backgroundColor: Color = AppTheme.colorScheme.white,
    contentColor: Color = AppTheme.colorScheme.black,
    navigationIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowBack,
    onNavigationClick: (() -> Unit)? = {},
    showSearch: Boolean = true,
    onSearchClick: (() -> Unit)? = {},
    onMoreClick: (() -> Unit)? = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                IconButton(onClick = { onNavigationClick?.invoke() }) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Back",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (navigationIcon != null) 8.dp else 16.dp)
            ) {
                Text(
                    text = title,
                    color = contentColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            if (showSearch) {
                IconButton(onClick = { onSearchClick?.invoke() }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            IconButton(onClick = { onMoreClick?.invoke() }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VisionaryAlbumHeaderPreview() {
    VisionaryTheme {
        VisionaryAlbumHeader(
            title = "Camera",
            subtitle = "1,284 photos",
            onNavigationClick = {},
            showSearch = true,
            onSearchClick = {},
            onMoreClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VisionaryAlbumHeaderNoSearchPreview() {
    VisionaryTheme {
        VisionaryAlbumHeader(
            title = "Camera",
            subtitle = null,
            onNavigationClick = {},
            showSearch = false,
            onSearchClick = {},
            onMoreClick = {}
        )
    }
}
