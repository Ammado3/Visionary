package com.o3.vanguard.visionary.feature.teleport.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.feature.teleport.domain.model.*
import com.o3.vanguard.visionary.feature.teleport.domain.port.DeviceNameProvider
import com.o3.vanguard.visionary.feature.teleport.domain.port.ReceivedPhotoStore
import com.o3.vanguard.visionary.feature.teleport.domain.session.TeleportReceiverSession
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TeleportTransport
import kotlinx.coroutines.flow.StateFlow

class TeleportReceiverViewModel(
    transport: TeleportTransport,
    store: ReceivedPhotoStore,
    deviceNames: DeviceNameProvider,
) : ViewModel() {
    private val session = TeleportReceiverSession(transport, store, deviceNames, viewModelScope)
    val state: StateFlow<ReceiverState> = session.state

    fun start() = session.start()
    fun onDropCommitted() = session.onDropCommitted()
    fun cancel() = session.cancel()

    override fun onCleared() {
        super.onCleared()
        session.dispose()
    }
}
