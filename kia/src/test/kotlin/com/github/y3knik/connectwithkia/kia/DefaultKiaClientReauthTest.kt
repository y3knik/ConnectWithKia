package com.github.y3knik.connectwithkia.kia

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultKiaClientReauthTest {
    private lateinit var server: MockWebServer
    private lateinit var storage: InMemoryTokenStorage
    private lateinit var client: KiaClient

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        storage = InMemoryTokenStorage().also {
            it.writeAccessToken("EXPIRED", 9_999_999_999L)
        }
        client = DefaultKiaClient(
            baseUrl = server.url("/").toString(),
            tokenStorage = storage,
            credentialProvider = { Credentials("user@example.com", "pw") },
        )
    }

    @After fun tearDown() { server.shutdown() }

    @Test
    fun `lock re-logs in on 401 then retries`() = runTest {
        // 1. verifyPin → 401
        server.enqueue(MockResponse().setResponseCode(401))
        // 2. login → 200 with fresh token
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("fixtures/login_success.json")))
        // 3. verifyPin retry → 200
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("fixtures/vrfypin_success.json")))
        // 4. drlck → 200
        server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("fixtures/drlck_success.json")))

        val result = client.lock(vehicleId = "VID-EV9-001", pin = "1234")

        assertTrue(result.isSuccess)
        assertEquals("TEST_ACCESS_TOKEN", storage.readAccessToken())
        assertEquals(4, server.requestCount)
    }
}
