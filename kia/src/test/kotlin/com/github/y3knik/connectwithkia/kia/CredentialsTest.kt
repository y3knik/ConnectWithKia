package com.github.y3knik.connectwithkia.kia

import org.junit.Test
import kotlin.test.assertFalse

class CredentialsTest {
    @Test
    fun `toString does not leak password`() {
        val creds = Credentials(email = "a@b.com", password = "s3cret")
        assertFalse(creds.toString().contains("s3cret"))
    }
}
