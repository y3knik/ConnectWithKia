package com.github.y3knik.connectwithkia.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SecurePreferencesTest {
    @Test
    fun `getString returns null when absent`() {
        val prefs = InMemoryPreferences()
        assertNull(prefs.getString("missing"))
    }

    @Test
    fun `putString then getString round-trips`() {
        val prefs = InMemoryPreferences()
        prefs.putString("key", "value")
        assertEquals("value", prefs.getString("key"))
    }

    @Test
    fun `remove deletes a key`() {
        val prefs = InMemoryPreferences()
        prefs.putString("key", "value")
        prefs.remove("key")
        assertNull(prefs.getString("key"))
    }

    @Test
    fun `clear deletes all keys`() {
        val prefs = InMemoryPreferences()
        prefs.putString("a", "1")
        prefs.putString("b", "2")
        prefs.clear()
        assertNull(prefs.getString("a"))
        assertNull(prefs.getString("b"))
    }
}
