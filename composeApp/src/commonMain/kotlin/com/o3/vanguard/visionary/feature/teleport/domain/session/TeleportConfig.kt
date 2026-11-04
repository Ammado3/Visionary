package com.o3.vanguard.visionary.feature.teleport.domain.session

data class TeleportConfig(
    val autoConnectDelayMs: Long = 1_200,
    val searchTimeoutMs: Long = 45_000,
    val transferTimeoutMs: Long = 120_000,
    val receiverIdleTimeoutMs: Long = 180_000,
    val shutdownGraceMs: Long = 600,
)
