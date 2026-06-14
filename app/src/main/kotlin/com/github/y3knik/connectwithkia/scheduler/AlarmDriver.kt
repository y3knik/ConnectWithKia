package com.github.y3knik.connectwithkia.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log

interface AlarmDriver {
    fun arm(targetEpochMs: Long)

    fun cancel()
}

class AndroidAlarmDriver(private val context: Context) : AlarmDriver {
    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, LockAlarmReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun arm(targetEpochMs: Long) {
        // SCHEDULE_EXACT_ALARM is granted at install time but the user can revoke it from system
        // settings on Android 14+; fall back to an inexact alarm rather than crashing.
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetEpochMs,
                pendingIntent,
            )
        } catch (e: SecurityException) {
            Log.w("AndroidAlarmDriver", "SCHEDULE_EXACT_ALARM unavailable, falling back to inexact alarm", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetEpochMs,
                pendingIntent,
            )
        }
    }

    override fun cancel() {
        alarmManager.cancel(pendingIntent)
    }

    private companion object {
        const val REQUEST_CODE = 0x10C4
    }
}
