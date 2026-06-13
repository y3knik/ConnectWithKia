package com.github.y3knik.connectwithkia.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.y3knik.connectwithkia.di.AppContainer

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return
        AppContainer.get(context).scheduler.rearmIfPending()
    }
}
