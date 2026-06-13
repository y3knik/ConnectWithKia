package com.github.y3knik.connectwithkia.kia

import org.junit.Test
import kotlin.reflect.full.declaredFunctions
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KiaClientContractTest {
    @Test
    fun `KiaClient exposes login, vehicles, lock`() {
        val methods = KiaClient::class.declaredFunctions.map { it.name }.toSet()
        assertTrue("login" in methods, "missing login")
        assertTrue("vehicles" in methods, "missing vehicles")
        assertTrue("lock" in methods, "missing lock")
    }

    @Test
    fun `Vehicle data class has id, nickname, vin`() {
        val v = Vehicle(id = "abc", nickname = "EV9", vin = "VIN")
        assertEquals("abc", v.id)
        assertEquals("EV9", v.nickname)
        assertEquals("VIN", v.vin)
    }

    @Test
    fun `TokenStorage is referenced and instantiable as a stub`() {
        val storage = object : TokenStorage {
            private var token: String? = null
            override fun readAccessToken(): String? = token
            override fun writeAccessToken(token: String, expiresAtEpochMs: Long) {
                this.token = token
            }
            override fun clear() {
                token = null
            }
        }
        storage.writeAccessToken("tk", 1)
        assertNotNull(storage.readAccessToken())
    }
}
