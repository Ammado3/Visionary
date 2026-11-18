package com.o3.vanguard.visionary

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.o3.vanguard.visionary.domain.repository.IndexRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class IndexingForegroundService : Service() {
    private val indexRepository: IndexRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            try {
                indexRepository.rebuild()
            } finally {
                stopForeground(true)
                stopSelfResult(startId)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Photo indexing",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Processing your photos")
            .setContentText("Extracting text and organizing your gallery")
            .setOngoing(true)
            .build()
    } else {
        Notification.Builder(this)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Processing your photos")
            .setContentText("Extracting text and organizing your gallery")
            .setOngoing(true)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "photo_indexing"
        const val NOTIFICATION_ID = 4101
    }
}
