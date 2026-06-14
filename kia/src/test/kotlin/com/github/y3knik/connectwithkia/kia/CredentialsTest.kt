package com.github.y3knik.connectwithkia.kia

import org.junit.Test
import kotlin.test.assertFalse

class CredentialsTest {
    @Test
    fun `toString does not leak password or email`() {
        val creds = Credentials(email = "a@b.com", password = "s3cret")
        val rendered = creds.toString()
        assertFalse(rendered.contains("s3cret"))
        assertFalse(rendered.contains("a@b.com"))
    }
}
