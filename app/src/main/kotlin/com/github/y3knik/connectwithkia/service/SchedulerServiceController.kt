package com.github.y3knik.connectwithkia.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.y3knik.connectwithkia.scheduler.LockScheduler
import com.github.y3knik.connectwithkia.scheduler.LockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SchedulerServiceController(
    private val context: Context,
    private val scheduler: LockScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            scheduler.state.collect { state ->
                when (state) {
                    is LockState.PendingLock -> startService()
                    else -> Unit // service self-stops on Done or non-pending state
                }
            }
        }
    }

    private fun startService() {
        val intent = Intent(context, LockForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
