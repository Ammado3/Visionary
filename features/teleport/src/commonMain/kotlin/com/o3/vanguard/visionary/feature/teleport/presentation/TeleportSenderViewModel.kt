package com.o3.vanguard.visionary.feature.teleport.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.o3.vanguard.visionary.feature.teleport.domain.model.*
import com.o3.vanguard.visionary.feature.teleport.domain.port.DeviceNameProvider
import com.o3.vanguard.visionary.feature.teleport.domain.port.ThumbnailMaker
import com.o3.vanguard.visionary.feature.teleport.domain.session.TeleportSenderSession
import com.o3.vanguard.visionary.feature.teleport.domain.transport.TeleportTransport
import kotlinx.coroutines.flow.StateFlow

class TeleportSenderViewModel(
    transport: TeleportTransport,
    thumbnails: ThumbnailMaker,
    deviceNames: DeviceNameProvider,
) : ViewModel() {
    private val session = TeleportSenderSession(transport, thumbnails, deviceNames, viewModelScope)
    val state: StateFlow<SenderState> = session.state

    fun start(photo: PhotoRef) = session.start(photo)
    fun connect(peer: TeleportPeer) = session.connect(peer)
    fun onGrabCommitted() = session.onGrabCommitted()
    fun cancel() = session.cancel()

    override fun onCleared() {
        super.onCleared()
        session.dispose()
    }
}
