package com.github.y3knik.connectwithkia.data

import com.github.y3knik.connectwithkia.kia.TokenStorage

class KeystoreTokenStorage(private val prefs: PreferencesSource) : TokenStorage {
    private val keyToken = "access_token"
    private val keyExpires = "access_token_expires_at"

    override fun readAccessToken(): String? = prefs.getString(keyToken)

    override fun writeAccessToken(
        token: String,
        expiresAtEpochMs: Long,
    ) {
        prefs.putString(keyToken, token)
        prefs.putString(keyExpires, expiresAtEpochMs.toString())
    }

    override fun clear() {
        prefs.remove(keyToken)
        prefs.remove(keyExpires)
    }
}
