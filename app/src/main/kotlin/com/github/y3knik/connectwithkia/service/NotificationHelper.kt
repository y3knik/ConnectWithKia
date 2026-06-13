package com.github.y3knik.connectwithkia.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.github.y3knik.connectwithkia.MainActivity
import com.github.y3knik.connectwithkia.R

class NotificationHelper(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun ensureChannels(highProminence: Boolean) {
        val countdown =
            NotificationChannel(
                CHANNEL_COUNTDOWN,
                context.getString(R.string.channel_countdown_name),
                if (highProminence) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.channel_countdown_desc)
                setShowBadge(false)
            }
        val outcomes =
            NotificationChannel(
                CHANNEL_OUTCOMES,
                context.getString(R.string.channel_outcomes_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_outcomes_desc)
            }
        manager.createNotificationChannel(countdown)
        manager.createNotificationChannel(outcomes)
    }

    fun countdown(remainingText: String): Notification {
        val openApp =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val cancelIntent = Intent(context, com.github.y3knik.connectwithkia.scheduler.CancelLockReceiver::class.java)
        val cancelPi =
            PendingIntent.getBroadcast(
                context,
                1,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
            .setContentTitle(context.getString(R.string.notif_countdown_title))
            .setContentText(context.getString(R.string.notif_countdown_text, remainingText))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, context.getString(R.string.notif_cancel_action), cancelPi)
            .build()
    }

    fun success() {
        val n =
            NotificationCompat.Builder(context, CHANNEL_OUTCOMES)
                .setContentTitle(context.getString(R.string.notif_success_title))
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setAutoCancel(true)
                .build()
        manager.notify(NOTIF_OUTCOME, n)
    }

    fun failure(reason: String) {
        val n =
            NotificationCompat.Builder(context, CHANNEL_OUTCOMES)
                .setContentTitle(context.getString(R.string.notif_failure_title))
                .setContentText(reason)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setAutoCancel(true)
                .build()
        manager.notify(NOTIF_OUTCOME, n)
    }

    companion object {
        const val CHANNEL_COUNTDOWN = "countdown"
        const val CHANNEL_OUTCOMES = "outcomes"
        const val NOTIF_COUNTDOWN = 1001
        const val NOTIF_OUTCOME = 1002
    }
}
