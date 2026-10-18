package com.o3.vanguard.visionary.features.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.domain.model.MediaId
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SearchRoute(
    initialQuery: String,
    onOpenViewer: (MediaId, String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = koinViewModel { parametersOf(initialQuery) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is SearchEvent.OpenViewer -> onOpenViewer(event.id, event.highlight)
            SearchEvent.NavigateBack -> onBack()
        }
    }
    SearchScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun SearchScreen(
    state: SearchState,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onIntent(SearchIntent.OnQueryChanged(it)) },
                label = { Text("Search images & OCR text") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.results) { hit ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIntent(SearchIntent.OnHitClicked(hit.item.id, hit.snippet)) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Media #${hit.item.id.value}", style = MaterialTheme.typography.titleMedium)
                            Text(hit.snippet, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
