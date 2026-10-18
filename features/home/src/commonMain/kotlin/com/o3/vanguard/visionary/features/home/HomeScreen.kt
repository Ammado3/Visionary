package com.o3.vanguard.visionary.features.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import com.o3.vanguard.visionary.domain.model.CollectionKind
import com.o3.vanguard.visionary.domain.model.MediaId
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoute(
    onOpenCollection: (CollectionKind) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenViewer: (MediaId) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            is HomeEvent.OpenCollection -> onOpenCollection(event.kind)
            is HomeEvent.OpenViewer -> onOpenViewer(event.id)
            HomeEvent.OpenSearch -> onOpenSearch()
            HomeEvent.OpenScanner -> onOpenScanner()
            HomeEvent.OpenVault -> onOpenVault()
            HomeEvent.OpenSettings -> onOpenSettings()
        }
    }
    HomeScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        if (state.loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.error) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Failed to load content")
                Button(onClick = { onIntent(HomeIntent.OnRetryClicked) }) { Text("Retry") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Visionary Home", style = MaterialTheme.typography.headlineMedium)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { onIntent(HomeIntent.OnSearchClicked) }) { Text("Search") }
                        Button(onClick = { onIntent(HomeIntent.OnScanClicked) }) { Text("Scan") }
                        Button(onClick = { onIntent(HomeIntent.OnVaultClicked) }) { Text("Vault") }
                        Button(onClick = { onIntent(HomeIntent.OnSettingsClicked) }) { Text("Settings") }
                    }
                }

                item {
                    Text("Collections", style = MaterialTheme.typography.titleLarge)
                }

                items(state.collections) { collection ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIntent(HomeIntent.OnCollectionClicked(collection.kind)) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(collection.kind.name)
                            Text("${collection.count} items")
                        }
                    }
                }
            }
        }
    }
}
