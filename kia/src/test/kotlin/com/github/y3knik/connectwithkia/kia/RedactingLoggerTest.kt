package com.github.y3knik.connectwithkia.kia

import com.github.y3knik.connectwithkia.kia.internal.RedactingLogger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RedactingLoggerTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
    }

    @After fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `password and tokens redacted from log output, original sent on the wire`() {
        val log = StringBuilder()
        val logger =
            HttpLoggingInterceptor(RedactingLogger { log.appendLine(it) })
                .setLevel(HttpLoggingInterceptor.Level.BODY)
        val client =
            OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        val body =
            """{"email":"a@b.com","password":"hunter2"}"""
                .toRequestBody("application/json".toMediaType())
        val request =
            Request.Builder()
                .url(server.url("/lgn"))
                .header("Accesstoken", "SECRET_TOKEN")
                .header("pAuth", "PAUTH_TOKEN")
                .post(body)
                .build()

        client.newCall(request).execute().close()

        val sent = server.takeRequest()
        assertTrue("hunter2" in sent.body.readUtf8(), "real password must reach server")
        assertEquals("SECRET_TOKEN", sent.getHeader("Accesstoken"))
        assertEquals("PAUTH_TOKEN", sent.getHeader("pAuth"))

        val text = log.toString()
        assertFalse("hunter2" in text, "password leaked in log: $text")
        assertFalse("SECRET_TOKEN" in text, "access token leaked in log")
        assertFalse("PAUTH_TOKEN" in text, "pAuth leaked in log")
        assertTrue("REDACTED" in text, "no REDACTED marker in log: $text")
    }
}
