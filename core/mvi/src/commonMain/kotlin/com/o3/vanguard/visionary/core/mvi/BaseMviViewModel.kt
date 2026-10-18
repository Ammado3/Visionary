package com.o3.vanguard.visionary.core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

interface UiState
interface UiIntent
interface UiEvent

abstract class BaseMviViewModel<S : UiState, I : UiIntent, E : UiEvent>(
    initialState: S,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _events = Channel<E>(Channel.UNLIMITED)
    val events: Flow<E> = _events.receiveAsFlow()

    protected fun updateState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    protected fun sendEvent(event: E) {
        _events.trySend(event)
    }

    abstract fun onIntent(intent: I)
}

@Composable
fun <E : UiEvent> CollectEvents(events: Flow<E>, onEvent: (E) -> Unit) {
    val current by rememberUpdatedState(onEvent)
    LaunchedEffect(events) {
        events.collect { current(it) }
    }
}
