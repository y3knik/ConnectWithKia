package com.github.y3knik.connectwithkia.scheduler

import com.github.y3knik.connectwithkia.kia.KiaClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LockScheduler(
    private val kia: KiaClient,
    private val alarmDriver: AlarmDriver,
    private val delayMinutes: () -> Int,
    /** Returns vehicleId, pin, configured-flag. Null if not configured. */
    private val credentials: () -> Triple<String, String, Boolean>?,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val persistedTarget: PersistedTarget = NoopPersistedTarget,
) {
    interface PersistedTarget {
        fun read(): Long?

        fun write(value: Long?)
    }

    private object NoopPersistedTarget : PersistedTarget {
        override fun read(): Long? = null

        override fun write(value: Long?) {}
    }

    private val _state =
        MutableStateFlow<LockState>(
            if (credentials() != null) LockState.Idle else LockState.Disabled(configured = false),
        )
    val state: StateFlow<LockState> = _state.asStateFlow()
    private val machine = LockStateMachine(initial = _state.value)
    private var lockJob: Job? = null

    fun onEvent(event: LockEvent) {
        synchronized(this) {
            val previous = _state.value
            val next = machine.transition(event)
            _state.value = next
            applySideEffects(previous, next, event)
        }
    }

    /** Test hook: waits for the active lock job (if any) to finish. */
    suspend fun awaitLock() {
        lockJob?.join()
    }

    private fun applySideEffects(
        previous: LockState,
        next: LockState,
        event: LockEvent,
    ) {
        if (next is LockState.PendingLock && previous !is LockState.PendingLock) {
            val targetMs = clockMs() + delayMinutes().coerceAtLeast(1) * 60_000L
            alarmDriver.arm(targetMs)
            persistedTarget.write(targetMs)
        }
        if (previous is LockState.PendingLock && next !is LockState.PendingLock && event != LockEvent.AlarmFired) {
            alarmDriver.cancel()
            persistedTarget.write(null)
        }
        if (next is LockState.Locking && previous !is LockState.Locking) {
            persistedTarget.write(null)
            performLock()
        }
    }

    /** Called by BootReceiver. Returns true if an alarm was re-armed. */
    fun rearmIfPending(): Boolean {
        val target = persistedTarget.read() ?: return false
        val now = clockMs()
        return synchronized(this) {
            when {
                target > now -> {
                    alarmDriver.arm(target)
                    machine.reset(LockState.PendingLock)
                    _state.value = LockState.PendingLock
                    true
                }
                now - target <= STALE_LIMIT_MS -> {
                    machine.reset(LockState.PendingLock)
                    _state.value = LockState.PendingLock
                    // Re-enter the lock flow synchronously; onEvent re-takes the same monitor
                    // (synchronized is reentrant), so this is safe.
                    val next = machine.transition(LockEvent.AlarmFired)
                    _state.value = next
                    applySideEffects(LockState.PendingLock, next, LockEvent.AlarmFired)
                    true
                }
                else -> {
                    persistedTarget.write(null)
                    false
                }
            }
        }
    }

    private fun performLock() {
        val creds =
            credentials() ?: run {
                onEvent(LockEvent.LockFailed("no credentials"))
                return
            }
        val (vehicleId, pin, _) = creds
        lockJob =
            scope.launch {
                val result = kia.lock(vehicleId, pin)
                result.fold(
                    onSuccess = { onEvent(LockEvent.LockSucceeded) },
                    onFailure = { onEvent(LockEvent.LockFailed(it.message ?: "unknown")) },
                )
            }
    }

    private companion object {
        const val STALE_LIMIT_MS = 30L * 60_000L
    }
}
