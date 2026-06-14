package com.github.y3knik.connectwithkia.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CredentialsRepositoryTest {
    @Test
    fun `has returns false when nothing stored`() {
        val repo = CredentialsRepository(InMemoryPreferences())
        assertFalse(repo.hasCompleteCredentials)
        assertNull(repo.read())
    }

    @Test
    fun `write then read round-trips`() {
        val prefs = InMemoryPreferences()
        val repo = CredentialsRepository(prefs)
        repo.write(email = "a@b.com", password = "pw", pin = "1234")
        val out = repo.read()!!
        assertEquals("a@b.com", out.email)
        assertEquals("pw", out.password)
        assertEquals("1234", out.pin)
        assertTrue(repo.hasCompleteCredentials)
    }

    @Test
    fun `vehicleId persists separately`() {
        val repo = CredentialsRepository(InMemoryPreferences())
        repo.vehicleId = "VID-123"
        assertEquals("VID-123", repo.vehicleId)
    }

    @Test
    fun `clear removes everything`() {
        val prefs = InMemoryPreferences()
        val repo = CredentialsRepository(prefs)
        repo.write("a@b.com", "pw", "1234")
        repo.vehicleId = "VID-123"
        repo.clear()
        assertNull(repo.read())
        assertNull(repo.vehicleId)
    }
}
