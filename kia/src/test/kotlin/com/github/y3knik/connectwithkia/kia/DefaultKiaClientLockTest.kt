package com.github.y3knik.connectwithkia.kia

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultKiaClientLockTest {
    private lateinit var server: MockWebServer
    private lateinit var storage: InMemoryTokenStorage
    private lateinit var client: KiaClient

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        storage =
            InMemoryTokenStorage().also {
                it.writeAccessToken("TEST_ACCESS_TOKEN", 9_999_999_999L)
            }
        client = DefaultKiaClient(baseUrl = server.url("/").toString(), tokenStorage = storage)
    }

    @After fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `lock performs preauth then drlck`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("fixtures/vrfypin_success.json")))
            server.enqueue(MockResponse().setResponseCode(200).setBody(fixture("fixtures/drlck_success.json")))

            val result = client.lock(vehicleId = "VID-EV9-001", pin = "1234")

            assertTrue(result.isSuccess)

            val pinReq = server.takeRequest()
            assertEquals("POST", pinReq.method)
            assertTrue(pinReq.path?.endsWith("vrfypin") == true)
            assertTrue("1234" in pinReq.body.readUtf8())
            assertEquals("TEST_ACCESS_TOKEN", pinReq.getHeader("Accesstoken"))

            val lockReq = server.takeRequest()
            assertEquals("POST", lockReq.method)
            assertTrue(lockReq.path?.endsWith("drlck") == true)
            assertEquals("TEST_ACCESS_TOKEN", lockReq.getHeader("Accesstoken"))
            assertEquals("TEST_PAUTH_TOKEN", lockReq.getHeader("pAuth"))
            assertEquals("VID-EV9-001", lockReq.getHeader("vehicleId"))
        }

    @Test
    fun `lock fails when preauth returns non-200`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"bad pin"}"""))

            val result = client.lock(vehicleId = "VID-EV9-001", pin = "0000")

            assertTrue(result.isFailure)
        }
}
