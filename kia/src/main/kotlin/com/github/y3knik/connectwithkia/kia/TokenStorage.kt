package com.github.y3knik.connectwithkia.kia

/**
 * Persistence SPI for the access token. The :kia module owns the protocol;
 * the :app module supplies an Android Keystore-backed implementation.
 */
interface TokenStorage {
    fun readAccessToken(): String?

    fun writeAccessToken(
        token: String,
        expiresAtEpochMs: Long,
    )

    fun clear()
}
