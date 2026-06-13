package com.github.y3knik.connectwithkia.kia

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultKiaClientLoginTest {
    private lateinit var server: MockWebServer
    private lateinit var storage: InMemoryTokenStorage
    private lateinit var client: KiaClient

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        storage = InMemoryTokenStorage()
        client = DefaultKiaClient(baseUrl = server.url("/").toString(), tokenStorage = storage, clockMs = { 1_000L })
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login persists token on success`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(fixture("fixtures/login_success.json")),
        )

        val result = client.login("user@example.com", "pw")

        assertTrue(result.isSuccess, "login should succeed")
        assertEquals("TEST_ACCESS_TOKEN", storage.readAccessToken())
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertTrue(recorded.path?.endsWith("lgn") == true, "wrong path: ${recorded.path}")
        val body = recorded.body.readUtf8()
        assertTrue("user@example.com" in body)
        assertTrue("pw" in body)
    }

    @Test
    fun `login returns failure on 401`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"bad creds"}"""))

        val result = client.login("user@example.com", "wrong")

        assertTrue(result.isFailure)
    }
}

internal class InMemoryTokenStorage : TokenStorage {
    private var token: String? = null
    private var expires: Long = 0
    override fun readAccessToken(): String? = token
    override fun writeAccessToken(token: String, expiresAtEpochMs: Long) {
        this.token = token
        this.expires = expiresAtEpochMs
    }
    override fun clear() { token = null; expires = 0 }
    fun expiresAt(): Long = expires
}

internal fun fixture(path: String): String =
    DefaultKiaClientLoginTest::class.java.classLoader!!.getResourceAsStream(path)!!
        .bufferedReader().use { it.readText() }
