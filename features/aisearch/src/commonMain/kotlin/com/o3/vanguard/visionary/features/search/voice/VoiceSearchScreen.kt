@file:OptIn(ExperimentalMaterial3Api::class)

package com.o3.vanguard.visionary.features.search.voice

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.o3.vanguard.visionary.core.designsystem.VisionaryColors
import com.o3.vanguard.visionary.core.designsystem.generated.resources.*
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.domain.model.MediaCategory
import com.o3.vanguard.visionary.domain.model.MediaId
import com.o3.vanguard.visionary.domain.model.SearchHit
import com.o3.vanguard.visionary.domain.port.SpeechAvailability
import com.o3.vanguard.visionary.platform.permission.rememberMicrophonePermission
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VoiceSearchRoute(
    onOpenViewer: (MediaId, String?) -> Unit,
    onTypeInstead: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: VoiceSearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val micPermission = rememberMicrophonePermission { granted ->
        viewModel.onIntent(VoiceSearchIntent.OnPermissionResult(granted))
    }

    CollectEvents(viewModel.events) { event ->
        when (event) {
            VoiceSearchEvent.RequestMicPermission -> {
                if (micPermission.isGranted) {
                    viewModel.onIntent(VoiceSearchIntent.OnPermissionResult(granted = true))
                } else {
                    micPermission.request()
                }
            }

            is VoiceSearchEvent.OpenViewer -> onOpenViewer(event.mediaId, event.highlight)
            is VoiceSearchEvent.OpenTextSearch -> onTypeInstead(event.query)
            VoiceSearchEvent.NavigateBack -> onBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onIntent(VoiceSearchIntent.OnStart)
    }

    VoiceSearchScreen(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@Composable
fun VoiceSearchScreen(
    state: VoiceSearchState,
    onIntent: (VoiceSearchIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { onIntent(VoiceSearchIntent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(Res.string.talk_to_visionary),
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VisionaryColors.colorF4FDF7,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Mic area
            MicHeaderSection(
                phase = state.phase,
                availability = state.availability,
                liveText = state.liveText,
                onMicClick = {
                    if (state.phase == VoiceSearchState.Phase.Listening) {
                        onIntent(VoiceSearchIntent.OnStopListening)
                    } else {
                        onIntent(VoiceSearchIntent.OnStart)
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body depending on phase
            when (state.phase) {
                VoiceSearchState.Phase.Searching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = VisionaryColors.color007847)
                    }
                }

                VoiceSearchState.Phase.Results -> {
                    ResultsSection(
                        state = state,
                        onIntent = onIntent,
                        modifier = Modifier.weight(1f),
                    )
                }

                VoiceSearchState.Phase.NoResults -> {
                    NoResultsView(
                        queryText = state.query?.searchText ?: state.liveText,
                        onRetry = { onIntent(VoiceSearchIntent.OnRetry) },
                        onTypeInstead = { onIntent(VoiceSearchIntent.OnTypeInstead) },
                        modifier = Modifier.weight(1f),
                    )
                }

                VoiceSearchState.Phase.NoKeywords -> {
                    NoKeywordsView(
                        onRetry = { onIntent(VoiceSearchIntent.OnRetry) },
                        onTypeInstead = { onIntent(VoiceSearchIntent.OnTypeInstead) },
                        modifier = Modifier.weight(1f),
                    )
                }

                VoiceSearchState.Phase.NeedsPermission -> {
                    NeedsPermissionView(
                        onGrantPermission = { onIntent(VoiceSearchIntent.OnStart) },
                        modifier = Modifier.weight(1f),
                    )
                }

                VoiceSearchState.Phase.Error -> {
                    ErrorStateView(
                        onRetry = { onIntent(VoiceSearchIntent.OnRetry) },
                        onTypeInstead = { onIntent(VoiceSearchIntent.OnTypeInstead) },
                        modifier = Modifier.weight(1f),
                    )
                }

                VoiceSearchState.Phase.Idle, VoiceSearchState.Phase.Listening -> {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MicHeaderSection(
    phase: VoiceSearchState.Phase,
    availability: SpeechAvailability,
    liveText: String,
    onMicClick: () -> Unit,
) {
    val isListening = phase == VoiceSearchState.Phase.Listening
    val micDescription = if (isListening) {
        stringResource(Res.string.stop_listening)
    } else {
        stringResource(Res.string.start_voice_search)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp),
        ) {
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .background(
                            color = VisionaryColors.color007847.copy(alpha = 0.2f),
                            shape = CircleShape,
                        ),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = if (isListening) VisionaryColors.color007847 else VisionaryColors.color007847.copy(alpha = 0.1f),
                        shape = CircleShape,
                    )
                    .clickable(onClick = onMicClick)
                    .semantics { contentDescription = micDescription },
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isListening) VisionaryColors.colorF4FDF7 else VisionaryColors.color007847,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (liveText.isNotBlank()) {
            Text(
                text = liveText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = VisionaryColors.color0B1B14,
            )
        } else if (phase == VoiceSearchState.Phase.Idle) {
            Text(
                text = stringResource(Res.string.try_voice_search_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (availability == SpeechAvailability.SYSTEM_SERVICE) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.system_speech_notice),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultsSection(
    state: VoiceSearchState,
    onIntent: (VoiceSearchIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditingKeywords by remember { mutableStateOf(false) }
    var editKeywordText by remember { mutableStateOf(state.query?.searchText ?: "") }

    Column(modifier = modifier.fillMaxWidth()) {
        // "Looking for" row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(Res.string.looking_for) + ":",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = VisionaryColors.color0B1B14,
                modifier = Modifier.padding(end = 8.dp),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.query?.categoryHints?.forEach { category ->
                    val hintLabel = when (category) {
                        MediaCategory.RECEIPT -> "Receipt"
                        MediaCategory.SCREENSHOT -> "Screenshot"
                        MediaCategory.DOCUMENT -> "Document"
                        MediaCategory.SCAN -> "Scan"
                        MediaCategory.VIDEO -> "Video"
                        MediaCategory.PHOTO -> "Photo"
                    }
                    SuggestionChip(
                        onClick = {},
                        label = { Text(hintLabel) },
                    )
                }

                if (!isEditingKeywords) {
                    val keywordsText = state.query?.searchText ?: ""
                    if (keywordsText.isNotBlank()) {
                        SuggestionChip(
                            onClick = {
                                editKeywordText = keywordsText
                                isEditingKeywords = true
                            },
                            label = { Text(keywordsText) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit search words",
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }
        }

        if (isEditingKeywords) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                OutlinedTextField(
                    value = editKeywordText,
                    onValueChange = { editKeywordText = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (editKeywordText.isNotBlank()) {
                                isEditingKeywords = false
                                onIntent(VoiceSearchIntent.OnEditedText(editKeywordText))
                            }
                        },
                    ),
                )
                IconButton(
                    onClick = {
                        if (editKeywordText.isNotBlank()) {
                            isEditingKeywords = false
                            onIntent(VoiceSearchIntent.OnEditedText(editKeywordText))
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Submit search edit",
                        tint = VisionaryColors.color007847,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Results Grid with liveRegion semantics
        val resultCount = state.hits.size
        Text(
            text = "$resultCount photos found",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "$resultCount photos found"
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = state.hits,
                key = { it.item.id.value },
            ) { hit ->
                SearchResultGridItem(
                    hit = hit,
                    onClick = { onIntent(VoiceSearchIntent.OnResultClicked(hit)) },
                )
            }
        }
    }
}

@Composable
private fun SearchResultGridItem(
    hit: SearchHit,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(VisionaryColors.color007847.copy(alpha = 0.05f), shape = RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = hit.item.uri,
            contentDescription = hit.item.fileName.ifBlank { "Photo result" },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun NoResultsView(
    queryText: String,
    onRetry: () -> Unit,
    onTypeInstead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.no_photos_with_query, queryText),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(Res.string.try_again))
            }
            Button(
                onClick = onTypeInstead,
                colors = ButtonDefaults.buttonColors(containerColor = VisionaryColors.color007847),
            ) {
                Text(stringResource(Res.string.type_instead))
            }
        }
    }
}

@Composable
private fun NoKeywordsView(
    onRetry: () -> Unit,
    onTypeInstead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.no_keywords_prompt),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(Res.string.try_again))
            }
            Button(
                onClick = onTypeInstead,
                colors = ButtonDefaults.buttonColors(containerColor = VisionaryColors.color007847),
            ) {
                Text(stringResource(Res.string.type_instead))
            }
        }
    }
}

@Composable
private fun NeedsPermissionView(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.mic_permission_required),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGrantPermission,
            colors = ButtonDefaults.buttonColors(containerColor = VisionaryColors.color007847),
        ) {
            Text(stringResource(Res.string.grant_permission))
        }
    }
}

@Composable
private fun ErrorStateView(
    onRetry: () -> Unit,
    onTypeInstead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.voice_offline_error),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(Res.string.try_again))
            }
            Button(
                onClick = onTypeInstead,
                colors = ButtonDefaults.buttonColors(containerColor = VisionaryColors.color007847),
            ) {
                Text(stringResource(Res.string.type_instead))
            }
        }
    }
}
