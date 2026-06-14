package com.github.y3knik.connectwithkia.data

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsTest {
    @Test
    fun `defaults are sensible`() {
        val s = AppSettings(InMemoryPreferences())
        assertTrue(s.enabled)
        assertTrue(s.highProminenceCountdown)
        assertTrue(s.successNotification)
        assertEquals(5, s.lockDelayMinutes)
    }

    @Test
    fun `setting values round-trips`() {
        val prefs = InMemoryPreferences()
        val s = AppSettings(prefs)
        s.enabled = false
        s.highProminenceCountdown = false
        s.successNotification = false
        s.lockDelayMinutes = 3
        val s2 = AppSettings(prefs)
        assertFalse(s2.enabled)
        assertFalse(s2.highProminenceCountdown)
        assertFalse(s2.successNotification)
        assertEquals(3, s2.lockDelayMinutes)
    }

    @Test
    fun `lockDelayMinutes clamps to 1 to 15`() {
        val s = AppSettings(InMemoryPreferences())
        s.lockDelayMinutes = 0
        assertEquals(1, s.lockDelayMinutes)
        s.lockDelayMinutes = 99
        assertEquals(15, s.lockDelayMinutes)
    }
}
