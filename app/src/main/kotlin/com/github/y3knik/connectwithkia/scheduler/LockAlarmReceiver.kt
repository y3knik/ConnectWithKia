package com.github.y3knik.connectwithkia.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.y3knik.connectwithkia.di.AppContainer

class LockAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppContainer.get(context).scheduler.onEvent(LockEvent.AlarmFired)
    }
}
