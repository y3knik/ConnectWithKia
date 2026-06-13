package com.github.y3knik.connectwithkia.ui.credentials

import app.cash.turbine.test
import com.github.y3knik.connectwithkia.data.CredentialsRepository
import com.github.y3knik.connectwithkia.data.InMemoryPreferences
import com.github.y3knik.connectwithkia.kia.KiaClient
import com.github.y3knik.connectwithkia.kia.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CredentialsViewModelTest {
    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeKia(var loginOk: Boolean, var vehicles: List<Vehicle>) : KiaClient {
        override suspend fun login(
            email: String,
            password: String,
        ) = if (loginOk) Result.success(Unit) else Result.failure(RuntimeException("bad creds"))

        override suspend fun vehicles() = Result.success(vehicles)

        override suspend fun lock(
            vehicleId: String,
            pin: String,
        ) = Result.success(Unit)
    }

    @Test
    fun `testConnection succeeds and persists email password pin vehicleId`() =
        runTest {
            val repo = CredentialsRepository(InMemoryPreferences())
            val kia = FakeKia(loginOk = true, vehicles = listOf(Vehicle("VID-1", "EV9", "VIN")))
            val vm = CredentialsViewModel(repo, kia)
            vm.onEmailChange("a@b.com")
            vm.onPasswordChange("pw")
            vm.onPinChange("1234")

            vm.testAndSave()

            vm.state.test {
                val first = awaitItem()
                assertTrue(first is CredentialsUiState.SavedSuccessfully || first is CredentialsUiState.Verifying)
                var state = first
                while (state !is CredentialsUiState.SavedSuccessfully) state = awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            val stored = repo.read()!!
            assertEquals("a@b.com", stored.email)
            assertEquals("pw", stored.password)
            assertEquals("1234", stored.pin)
            assertEquals("VID-1", repo.vehicleId)
        }

    @Test
    fun `testConnection emits Error on bad credentials`() =
        runTest {
            val repo = CredentialsRepository(InMemoryPreferences())
            val kia = FakeKia(loginOk = false, vehicles = emptyList())
            val vm = CredentialsViewModel(repo, kia)
            vm.onEmailChange("a@b.com")
            vm.onPasswordChange("wrong")
            vm.onPinChange("1234")

            vm.testAndSave()

            vm.state.test {
                var state = awaitItem()
                while (state !is CredentialsUiState.Error) state = awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
