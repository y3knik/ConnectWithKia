package com.github.y3knik.connectwithkia.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

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
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            targetEpochMs,
            pendingIntent,
        )
    }

    override fun cancel() {
        alarmManager.cancel(pendingIntent)
    }

    private companion object {
        const val REQUEST_CODE = 0x10C4
    }
}
