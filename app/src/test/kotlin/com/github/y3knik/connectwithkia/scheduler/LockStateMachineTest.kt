package com.github.y3knik.connectwithkia.scheduler

import org.junit.Test
import kotlin.test.assertEquals

class LockStateMachineTest {

    @Test
    fun `disabled stays disabled on any event when not configured`() {
        val sm = LockStateMachine(initial = LockState.Disabled(configured = false))
        assertEquals(LockState.Disabled(configured = false), sm.transition(LockEvent.AaConnected))
        assertEquals(LockState.Disabled(configured = false), sm.transition(LockEvent.AaDisconnected))
    }

    @Test
    fun `enabled with credentials and AA connected goes to Connected`() {
        val sm = LockStateMachine(initial = LockState.Idle)
        assertEquals(LockState.Connected, sm.transition(LockEvent.AaConnected))
    }

    @Test
    fun `connected then AA disconnected arms pending lock`() {
        val sm = LockStateMachine(initial = LockState.Connected)
        val out = sm.transition(LockEvent.AaDisconnected)
        assertEquals(LockState.PendingLock, out)
    }

    @Test
    fun `pending lock then AA reconnected goes back to Connected`() {
        val sm = LockStateMachine(initial = LockState.PendingLock)
        assertEquals(LockState.Connected, sm.transition(LockEvent.AaConnected))
    }

    @Test
    fun `pending lock then user cancel goes to Idle`() {
        val sm = LockStateMachine(initial = LockState.PendingLock)
        assertEquals(LockState.Idle, sm.transition(LockEvent.UserCancelled))
    }

    @Test
    fun `pending lock then alarm fires goes to Locking`() {
        val sm = LockStateMachine(initial = LockState.PendingLock)
        assertEquals(LockState.Locking, sm.transition(LockEvent.AlarmFired))
    }

    @Test
    fun `locking then success goes to Done(success)`() {
        val sm = LockStateMachine(initial = LockState.Locking)
        assertEquals(LockState.Done(success = true), sm.transition(LockEvent.LockSucceeded))
    }

    @Test
    fun `locking then failure goes to Done(failure)`() {
        val sm = LockStateMachine(initial = LockState.Locking)
        assertEquals(LockState.Done(success = false, reason = "boom"), sm.transition(LockEvent.LockFailed("boom")))
    }

    @Test
    fun `Done collapses on next AA connected`() {
        val sm = LockStateMachine(initial = LockState.Done(success = true))
        assertEquals(LockState.Connected, sm.transition(LockEvent.AaConnected))
    }

    @Test
    fun `master disable returns to Disabled`() {
        val sm = LockStateMachine(initial = LockState.PendingLock)
        assertEquals(LockState.Disabled(configured = true), sm.transition(LockEvent.MasterDisabled))
    }
}
