package com.github.y3knik.connectwithkia.ui.status

import com.github.y3knik.connectwithkia.scheduler.LockState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import kotlin.test.assertEquals

class StatusViewModelTest {
    @Test
    fun `display text reflects state`() {
        val state = MutableStateFlow<LockState>(LockState.Idle)
        val vm = StatusViewModel(state, lockNow = {}, toggleEnabled = {}, isEnabled = { true })
        assertEquals("Idle — waiting for next drive", vm.displayText(LockState.Idle))
        assertEquals("Watching — Android Auto connected", vm.displayText(LockState.Connected))
        assertEquals("Watching — Android Auto disconnected", vm.displayText(LockState.PendingLock))
        assertEquals("Locking now…", vm.displayText(LockState.Locking))
        assertEquals("Last attempt: locked", vm.displayText(LockState.Done(success = true)))
        assertEquals("Last attempt failed: nope", vm.displayText(LockState.Done(success = false, reason = "nope")))
        assertEquals("Not configured — add credentials", vm.displayText(LockState.Disabled(configured = false)))
        assertEquals("Disabled (master toggle off)", vm.displayText(LockState.Disabled(configured = true)))
    }
}
