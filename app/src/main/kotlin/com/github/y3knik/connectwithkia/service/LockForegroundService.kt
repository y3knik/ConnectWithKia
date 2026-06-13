package com.github.y3knik.connectwithkia.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.github.y3knik.connectwithkia.di.AppContainer
import com.github.y3knik.connectwithkia.scheduler.LockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LockForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var tickerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val container = AppContainer.get(this)
        val notifications = NotificationHelper(this)
        notifications.ensureChannels(container.settings.highProminenceCountdown)

        startForeground(NotificationHelper.NOTIF_COUNTDOWN, notifications.countdown("..."))

        scope.launch {
            container.scheduler.state.collectLatest { state ->
                when (state) {
                    is LockState.PendingLock -> startTicker(notifications, container.settings.pendingLockTargetMs ?: 0L)
                    is LockState.Locking -> {
                        tickerJob?.cancel()
                    }
                    is LockState.Done -> {
                        if (state.success && container.settings.successNotification) notifications.success()
                        if (!state.success) notifications.failure(state.reason ?: "unknown error")
                        stopSelf()
                    }
                    else -> stopSelf()
                }
            }
        }
    }

    private fun startTicker(notifications: NotificationHelper, targetEpochMs: Long) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                val remaining = targetEpochMs - System.currentTimeMillis()
                if (remaining <= 0) break
                val mins = TimeUnit.MILLISECONDS.toMinutes(remaining)
                val secs = TimeUnit.MILLISECONDS.toSeconds(remaining) - mins * 60
                val text = "%d:%02d".format(mins, secs)
                val n = notifications.countdown(text)
                getSystemService(android.app.NotificationManager::class.java)
                    .notify(NotificationHelper.NOTIF_COUNTDOWN, n)
                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
