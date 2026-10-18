package com.o3.vanguard.visionary.features.settings

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
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Index only while charging")
                Switch(
                    checked = state.settings.indexOnlyWhileCharging,
                    onCheckedChange = { onIntent(SettingsIntent.OnChargingOnlyToggled(it)) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onIntent(SettingsIntent.OnOpenIndexHealthClicked) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Index Health & Status")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onIntent(SettingsIntent.OnBackClicked) }) {
                Text("Back")
            }
        }
    }
}

@Composable
fun IndexHealthRoute(onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Index Health", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("OCR indexing engine status: Healthy")
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack) { Text("Back") }
        }
    }
}
