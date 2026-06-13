package com.github.y3knik.connectwithkia.ui.status

import androidx.lifecycle.ViewModel
import com.github.y3knik.connectwithkia.scheduler.LockState
import kotlinx.coroutines.flow.StateFlow

class StatusViewModel(
    val state: StateFlow<LockState>,
    val lockNow: () -> Unit,
    val toggleEnabled: () -> Unit,
    val isEnabled: () -> Boolean,
) : ViewModel() {

    fun displayText(state: LockState): String = when (state) {
        is LockState.Disabled -> if (state.configured) "Disabled (master toggle off)" else "Not configured — add credentials"
        LockState.Idle -> "Idle — waiting for next drive"
        LockState.Connected -> "Watching — Android Auto connected"
        LockState.PendingLock -> "Watching — Android Auto disconnected"
        LockState.Locking -> "Locking now…"
        is LockState.Done -> if (state.success) "Last attempt: locked" else "Last attempt failed: ${state.reason ?: "unknown"}"
    }
}
