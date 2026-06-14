package com.github.y3knik.connectwithkia.scheduler

import com.github.y3knik.connectwithkia.kia.KiaClient
import com.github.y3knik.connectwithkia.kia.Vehicle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LockSchedulerTest {
    private class FakeAlarmDriver : AlarmDriver {
        var armedAt: Long? = null
        var cancelled: Boolean = false

        override fun arm(targetEpochMs: Long) {
            armedAt = targetEpochMs
            cancelled = false
        }

        override fun cancel() {
            cancelled = true
            armedAt = null
        }
    }

    private class FakeKiaClient(var lockResult: Result<Unit>) : KiaClient {
        var lockCalls = 0

        override suspend fun login(
            email: String,
            password: String,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun vehicles(): Result<List<Vehicle>> = Result.success(emptyList())

        override suspend fun lock(
            vehicleId: String,
            pin: String,
        ): Result<Unit> {
            lockCalls++
            return lockResult
        }
    }

    @Test
    fun `AaDisconnected arms alarm 5 minutes out`() =
        runTest {
            val driver = FakeAlarmDriver()
            val scheduler =
                LockScheduler(
                    kia = FakeKiaClient(Result.success(Unit)),
                    alarmDriver = driver,
                    delayMinutes = { 5 },
                    credentials = { Triple("vid", "1234", true) },
                    clockMs = { 1_000_000L },
                )
            scheduler.onEvent(LockEvent.AaConnected)
            scheduler.onEvent(LockEvent.AaDisconnected)
            assertEquals(1_000_000L + 5 * 60_000L, driver.armedAt)
        }

    @Test
    fun `AA reconnect cancels armed alarm`() =
        runTest {
            val driver = FakeAlarmDriver()
            val scheduler =
                LockScheduler(
                    kia = FakeKiaClient(Result.success(Unit)),
                    alarmDriver = driver,
                    delayMinutes = { 5 },
                    credentials = { Triple("vid", "1234", true) },
                    clockMs = { 1L },
                )
            scheduler.onEvent(LockEvent.AaConnected)
            scheduler.onEvent(LockEvent.AaDisconnected)
            scheduler.onEvent(LockEvent.AaConnected)
            assertTrue(driver.cancelled)
        }

    @Test
    fun `alarm fired triggers kia lock and emits Done(success)`() =
        runTest {
            val kia = FakeKiaClient(Result.success(Unit))
            val driver = FakeAlarmDriver()
            val scheduler =
                LockScheduler(
                    kia = kia,
                    alarmDriver = driver,
                    delayMinutes = { 1 },
                    credentials = { Triple("vid", "1234", true) },
                    clockMs = { 1L },
                )
            scheduler.onEvent(LockEvent.AaConnected)
            scheduler.onEvent(LockEvent.AaDisconnected)
            scheduler.onEvent(LockEvent.AlarmFired)
            scheduler.awaitLock()
            assertEquals(1, kia.lockCalls)
            assertEquals(LockState.Done(success = true), scheduler.state.first())
        }

    @Test
    fun `alarm fired with lock failure emits Done(failure)`() =
        runTest {
            val kia = FakeKiaClient(Result.failure(RuntimeException("boom")))
            val driver = FakeAlarmDriver()
            val scheduler =
                LockScheduler(
                    kia = kia,
                    alarmDriver = driver,
                    delayMinutes = { 1 },
                    credentials = { Triple("vid", "1234", true) },
                    clockMs = { 1L },
                )
            scheduler.onEvent(LockEvent.AaConnected)
            scheduler.onEvent(LockEvent.AaDisconnected)
            scheduler.onEvent(LockEvent.AlarmFired)
            scheduler.awaitLock()
            assertEquals(LockState.Done(success = false, reason = "boom"), scheduler.state.first())
        }
}
