package com.o3.vanguard.visionary.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.domain.model.IndexState
import com.o3.vanguard.visionary.core.designsystem.generated.resources.Res
import com.o3.vanguard.visionary.core.designsystem.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    onOpenIndexHealth: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.OpenIndexHealth -> onOpenIndexHealth()
            SettingsEvent.NavigateBack -> onBack()
        }
    }
    SettingsScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = state.indexProgress
    val total = progress?.total?.takeIf { it > 0 } ?: 2846
    val indexed = progress?.indexed ?: 1842
    val percent = ((indexed * 100f / total).toInt()).coerceIn(0, 100)
    val progressStatus = when (progress?.state) {
        IndexState.PAUSED_BATTERY -> "Indexing paused"
        IndexState.RUNNING -> "Indexing..."
        else -> "Indexing paused"
    }

    Surface(modifier = modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp))
                        .clickable { onIntent(SettingsIntent.OnBackClicked) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VisionaryColors.color0B1B14,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = "Settings",
                    color = VisionaryColors.color0B1B14,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(40.dp))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(VisionaryColors.colorCEDED3))

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                SettingsSection(title = "Appearance") {
                    Column {
                        SettingsRow(
                            title = "Theme",
                            subtitle = "System default",
                            trailing = {
                                Icon(
                                    Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = VisionaryColors.color496156
                                )
                            },
                        )
                        Box(Modifier.fillMaxWidth().height(1.dp).background(VisionaryColors.colorCEDED3))
                        ThemeSelector()
                    }
                }

                SettingsSection(title = "AI Search") {
                    Column {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "OCR Indexing",
                                    color = VisionaryColors.color0B1B14,
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "$percent%",
                                    color = VisionaryColors.color007847,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Indexed $indexed of $total photos",
                                color = VisionaryColors.color496156, fontSize = 14.sp, lineHeight = 20.sp,
                            )
                            Spacer(Modifier.height(12.dp))
                            Box(
                                Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                                    .background(VisionaryColors.colorE6F3EA)
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(percent / 100f).fillMaxHeight()
                                        .clip(RoundedCornerShape(50)).background(VisionaryColors.color007847)
                                )
                            }
                        }
                        SettingsDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Visual Analysis",
                                    color = VisionaryColors.color0B1B14,
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    progressStatus,
                                    color = VisionaryColors.color496156,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                            Switch(
                                checked = false,
                                onCheckedChange = null,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = VisionaryColors.color007847,
                                    uncheckedTrackColor = VisionaryColors.colorCEDED3,
                                    uncheckedThumbColor = VisionaryColors.colorF4FDF7
                                ),
                            )
                        }
                        SettingsDivider()
                        SettingsRow(
                            title = "Reindex library",
                            subtitle = "Continue on this device",
                            trailing = { Chevron() },
                            onClick = { onIntent(SettingsIntent.OnOpenIndexHealthClicked) },
                        )
                        SettingsDivider()
                        SettingsRow(
                            title = "Indexing preferences",
                            subtitle = "Wi-Fi only, ${if (state.settings.indexOnlyWhileCharging) "Charging only" else "Any power"}",
                            trailing = { Chevron() },
                            onClick = { onIntent(SettingsIntent.OnOpenIndexHealthClicked) },
                        )
                    }
                }

                SettingsSection(title = "About") {
                    Column {
                        SettingsRow(
                            title = "Help",
                            trailing = { Chevron() })
                        SettingsDivider()
                        SettingsRow(
                            title = "Privacy policy",
                            trailing = { Chevron() })
                        SettingsDivider()
                        SettingsRow(
                            title = "App Version",
                            subtitle = "1.0.0",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 4.dp),
            color = VisionaryColors.color496156,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(VisionaryColors.colorFBFFFD).border(1.dp, VisionaryColors.colorCEDED3, RoundedCornerShape(8.dp)),
            content = content,
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = VisionaryColors.color0B1B14,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) Text(
                subtitle,
                color = VisionaryColors.color496156,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun SettingsDivider() = Box(Modifier.fillMaxWidth().height(1.dp).background(VisionaryColors.colorCEDED3))

@Composable
private fun Chevron() =
    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = VisionaryColors.color496156)

@Composable
private fun ThemeSelector() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(8.dp)).background(VisionaryColors.colorE6F3EA).padding(3.dp),
    ) {
        listOf("Light", "Dark", "System").forEachIndexed { index, label ->
            Box(
                modifier = Modifier.weight(1f).height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (index == 0) VisionaryColors.colorF4FDF7 else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = VisionaryColors.color0B1B14,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun IndexHealthScreen(onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = VisionaryColors.colorF4FDF7) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "Index Health",
                style = MaterialTheme.typography.headlineLarge,
                color = VisionaryColors.color0B1B14
            )
            Spacer(Modifier.height(16.dp))
            Text("OCR Status", color = VisionaryColors.color496156)
            Spacer(Modifier.height(24.dp))
            Text(
                "Back",
                color = VisionaryColors.color007847,
                modifier = Modifier.clickable(onClick = onBack)
            )
        }
    }
}

@Composable
fun IndexHealthRoute(onBack: () -> Unit) = IndexHealthScreen(onBack)

