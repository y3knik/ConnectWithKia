package com.github.y3knik.connectwithkia.scheduler

sealed class LockState {
    data class Disabled(val configured: Boolean) : LockState()
    data object Idle : LockState()
    data object Connected : LockState()
    data object PendingLock : LockState()
    data object Locking : LockState()
    data class Done(val success: Boolean, val reason: String? = null) : LockState()
}

sealed class LockEvent {
    data object AaConnected : LockEvent()
    data object AaDisconnected : LockEvent()
    data object UserCancelled : LockEvent()
    data object AlarmFired : LockEvent()
    data object LockSucceeded : LockEvent()
    data class LockFailed(val reason: String) : LockEvent()
    data object MasterEnabled : LockEvent()
    data object MasterDisabled : LockEvent()
}

class LockStateMachine(initial: LockState) {
    var state: LockState = initial
        private set

    fun transition(event: LockEvent): LockState {
        state = next(state, event)
        return state
    }

    private fun next(state: LockState, event: LockEvent): LockState {
        if (state is LockState.Disabled && !state.configured) return state
        if (event is LockEvent.MasterDisabled) return LockState.Disabled(configured = true)
        if (event is LockEvent.MasterEnabled && state is LockState.Disabled) return LockState.Idle

        return when (state) {
            is LockState.Disabled -> state
            LockState.Idle -> when (event) {
                LockEvent.AaConnected -> LockState.Connected
                else -> state
            }
            LockState.Connected -> when (event) {
                LockEvent.AaDisconnected -> LockState.PendingLock
                else -> state
            }
            LockState.PendingLock -> when (event) {
                LockEvent.AaConnected -> LockState.Connected
                LockEvent.UserCancelled -> LockState.Idle
                LockEvent.AlarmFired -> LockState.Locking
                else -> state
            }
            LockState.Locking -> when (event) {
                LockEvent.LockSucceeded -> LockState.Done(success = true)
                is LockEvent.LockFailed -> LockState.Done(success = false, reason = event.reason)
                else -> state
            }
            is LockState.Done -> when (event) {
                LockEvent.AaConnected -> LockState.Connected
                LockEvent.AaDisconnected -> LockState.PendingLock
                else -> state
            }
        }
    }
}
