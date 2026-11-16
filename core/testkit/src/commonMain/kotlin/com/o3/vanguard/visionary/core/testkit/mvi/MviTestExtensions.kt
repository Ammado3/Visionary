package com.o3.vanguard.visionary.core.testkit.mvi

import com.o3.vanguard.visionary.core.mvi.BaseMviViewModel
import com.o3.vanguard.visionary.core.mvi.UiEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope

fun <E : UiEvent> TestScope.collectEvents(vm: BaseMviViewModel<*, *, E>): List<E> {
    val events = mutableListOf<E>()
    backgroundScope.launch {
        vm.events.collect { events.add(it) }
    }
    return events
}
