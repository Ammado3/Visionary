package com.o3.vanguard.visionary

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.o3.vanguard.visionary.core.common.AppDispatchers
import com.o3.vanguard.visionary.domain.repository.IndexRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Android's only indexing trigger (started by [MainActivity]). Runs one pass, then stops.
 *
 * - `START_NOT_STICKY`: if the process dies, the next app launch indexes again instead of the
 *   system restarting a full scan in the background.
 * - Android 15+ limits `dataSync` services to 6 hours a day and calls [onTimeout] when the limit
 *   is reached; the service must stop within seconds or the app crashes.
 */
class IndexingForegroundService : Service() {
    private val indexRepository: IndexRepository by inject()
    private val dispatchers: AppDispatchers by inject()
    private val serviceScope by lazy { CoroutineScope(SupervisorJob() + dispatchers.default) }
    private var indexJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (indexJob?.isActive != true) {
            indexJob = serviceScope.launch {
                try {
                    indexRepository.rebuild()
                } finally {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        indexJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // minSdk is 26, so notification channels always exist.
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Photo indexing",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_indexing)
            .setContentTitle("Processing your photos")
            .setContentText("Extracting text and organizing your gallery")
            .setOngoing(true)
            .build()

    private companion object {
        const val CHANNEL_ID = "photo_indexing"
        const val NOTIFICATION_ID = 4101
    }
}
