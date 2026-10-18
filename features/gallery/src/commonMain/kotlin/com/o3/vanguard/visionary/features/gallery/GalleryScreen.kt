package com.o3.vanguard.visionary.features.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.domain.model.MediaId
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GalleryRoute(
    kindId: String,
    onOpenViewer: (MediaId) -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = koinViewModel { parametersOf(kindId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is GalleryEvent.OpenViewer -> onOpenViewer(event.id)
            GalleryEvent.NavigateBack -> onBack()
        }
    }
    GalleryScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun GalleryScreen(
    state: GalleryState,
    onIntent: (GalleryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(state.collectionKind.name, style = MaterialTheme.typography.headlineMedium)
                Button(onClick = { onIntent(GalleryIntent.OnBackClicked) }) { Text("Back") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items) { item ->
                    Card(
                        modifier = Modifier
                            .height(120.dp)
                            .clickable { onIntent(GalleryIntent.OnItemClicked(item.id)) }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Media #${item.id.value}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ViewerRoute(
    mediaId: Long,
    highlight: String?,
    onBack: () -> Unit,
    viewModel: ViewerViewModel = koinViewModel { parametersOf(mediaId, highlight) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            ViewerEvent.NavigateBack -> onBack()
        }
    }
    ViewerScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun ViewerScreen(
    state: ViewerState,
    onIntent: (ViewerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Viewer for Media #${state.mediaId?.value}", style = MaterialTheme.typography.headlineLarge)
            if (state.highlightText != null) {
                Text("Highlight: ${state.highlightText}", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onIntent(ViewerIntent.OnMoveToVaultClicked) }) { Text("Move to Vault") }
                Button(onClick = { onIntent(ViewerIntent.OnBackClicked) }) { Text("Back") }
            }
        }
    }
}
