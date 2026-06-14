package com.github.y3knik.connectwithkia.kia

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultKiaClientVehiclesTest {
    private lateinit var server: MockWebServer
    private lateinit var storage: InMemoryTokenStorage
    private lateinit var client: KiaClient

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        storage =
            InMemoryTokenStorage().also {
                it.writeAccessToken("TEST_ACCESS_TOKEN", expiresAtEpochMs = 9_999_999_999L)
            }
        client = DefaultKiaClient(baseUrl = server.url("/").toString(), tokenStorage = storage)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `vehicles returns parsed list and sends access token`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(fixture("fixtures/vehicles_one_ev9.json")),
            )

            val result = client.vehicles()

            assertTrue(result.isSuccess, "vehicles should succeed")
            val list = result.getOrThrow()
            assertEquals(1, list.size)
            assertEquals("VID-EV9-001", list[0].id)
            assertEquals("EV9", list[0].nickname)
            assertEquals("KNDPC3DG7P0000001", list[0].vin)
            val recorded = server.takeRequest()
            assertEquals("POST", recorded.method)
            assertTrue(recorded.path?.endsWith("vhcllst") == true)
            assertEquals("TEST_ACCESS_TOKEN", recorded.getHeader("Accesstoken"))
        }
}
