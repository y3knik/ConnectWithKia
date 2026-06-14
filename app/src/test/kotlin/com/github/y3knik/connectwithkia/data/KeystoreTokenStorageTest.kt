package com.github.y3knik.connectwithkia.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeystoreTokenStorageTest {
    @Test
    fun `read returns null before any write`() {
        val storage = KeystoreTokenStorage(InMemoryPreferences())
        assertNull(storage.readAccessToken())
    }

    @Test
    fun `write then read returns token`() {
        val prefs = InMemoryPreferences()
        val storage = KeystoreTokenStorage(prefs)
        storage.writeAccessToken("TOKEN", expiresAtEpochMs = 12345L)
        assertEquals("TOKEN", storage.readAccessToken())
    }

    @Test
    fun `clear removes token`() {
        val prefs = InMemoryPreferences()
        val storage = KeystoreTokenStorage(prefs)
        storage.writeAccessToken("TOKEN", 1L)
        storage.clear()
        assertNull(storage.readAccessToken())
    }
}
