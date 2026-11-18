package com.o3.vanguard.visionary.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Res
import com.o3.vanguard.visionary.core.designsystem.generated.resources.aiSearch
import com.o3.vanguard.visionary.core.designsystem.generated.resources.album
import com.o3.vanguard.visionary.core.designsystem.generated.resources.photo
import com.o3.vanguard.visionary.core.designsystem.generated.resources.vault
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview


enum class AppTab(val label: String, val icon: DrawableResource) {
    PHOTOS("Photos", Res.drawable.photo),
    ALBUMS("Albums",Res.drawable.album),
    SEARCH("AI Search", Res.drawable.aiSearch),
    VAULT("Vault", Res.drawable.vault),
}

@Composable
fun AppBottomNavigation(
    selected: AppTab,
    onSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                val stroke = 1.dp.toPx()
                drawLine(
                    color = VisionaryColors.colorCEDED3,
                    start = Offset(0f, stroke / 2),
                    end = Offset(size.width, stroke / 2),
                    strokeWidth = stroke,
                )
            },
        color = VisionaryColors.colorF4FDF7,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            AppTab.entries.forEach { tab ->
                NavigationItem(
                    tab = tab,
                    selected = tab == selected,
                    selectedColor = VisionaryColors.color007847,
                    mutedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onSelected(tab) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavigationItem(
    tab: AppTab,
    selected: Boolean,
    selectedColor: Color,
    mutedColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .background(
                    color = if (selected) selectedColor else Color.Transparent,
                    shape = RoundedCornerShape(50),
                )
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(tab.icon),
                contentDescription = tab.label,
                modifier = Modifier.size(20.dp),
                tint = if (selected) VisionaryColors.colorFFFFFFFF else mutedColor,
            )
        }
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
            ),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) selectedColor else mutedColor,
        )
    }
}

