package com.github.y3knik.connectwithkia.scheduler

import com.github.y3knik.connectwithkia.kia.KiaClient
import com.github.y3knik.connectwithkia.kia.Vehicle
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LockSchedulerPersistenceTest {

    private class FakeAlarmDriver : AlarmDriver {
        var armedAt: Long? = null
        var cancelled = false
        override fun arm(targetEpochMs: Long) { armedAt = targetEpochMs; cancelled = false }
        override fun cancel() { cancelled = true; armedAt = null }
    }

    private class NoopKia : KiaClient {
        override suspend fun login(email: String, password: String) = Result.success(Unit)
        override suspend fun vehicles() = Result.success(emptyList<Vehicle>())
        override suspend fun lock(vehicleId: String, pin: String) = Result.success(Unit)
    }

    @Test
    fun `arming a pending lock persists target time`() {
        val driver = FakeAlarmDriver()
        var stored: Long? = null
        val scheduler = LockScheduler(
            kia = NoopKia(),
            alarmDriver = driver,
            delayMinutes = { 5 },
            credentials = { Triple("vid", "1234", true) },
            clockMs = { 1_000_000L },
            persistedTarget = object : LockScheduler.PersistedTarget {
                override fun read(): Long? = stored
                override fun write(value: Long?) { stored = value }
            },
        )
        scheduler.onEvent(LockEvent.AaConnected)
        scheduler.onEvent(LockEvent.AaDisconnected)
        assertEquals(1_000_000L + 5 * 60_000L, stored)
    }

    @Test
    fun `cancelling a pending lock clears persisted target`() {
        val driver = FakeAlarmDriver()
        var stored: Long? = null
        val scheduler = LockScheduler(
            kia = NoopKia(),
            alarmDriver = driver,
            delayMinutes = { 5 },
            credentials = { Triple("vid", "1234", true) },
            clockMs = { 1_000_000L },
            persistedTarget = object : LockScheduler.PersistedTarget {
                override fun read(): Long? = stored
                override fun write(value: Long?) { stored = value }
            },
        )
        scheduler.onEvent(LockEvent.AaConnected)
        scheduler.onEvent(LockEvent.AaDisconnected)
        scheduler.onEvent(LockEvent.UserCancelled)
        assertNull(stored)
    }
}
