package com.o3.vanguard.visionary.features.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.o3.vanguard.visionary.core.mvi.CollectEvents
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ScannerRoute(
    onOpenScans: () -> Unit,
    onBack: () -> Unit,
    viewModel: ScannerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEvents(viewModel.events) { event ->
        when (event) {
            ScannerEvent.NavigateToScansCollection -> onOpenScans()
            ScannerEvent.NavigateBack -> onBack()
        }
    }
    ScannerScreen(state = state, onIntent = viewModel::onIntent)
}

@Composable
fun ScannerScreen(
    state: ScannerState,
    onIntent: (ScannerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Document Scanner", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(24.dp))
            if (state.scanning) {
                CircularProgressIndicator()
            } else {
                Button(onClick = { onIntent(ScannerIntent.OnStartScanClicked) }) {
                    Text("Start Scan")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onIntent(ScannerIntent.OnBackClicked) }) {
                Text("Back")
            }
        }
    }
}
