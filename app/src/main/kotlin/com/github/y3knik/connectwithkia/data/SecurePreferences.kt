package com.github.y3knik.connectwithkia.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

interface PreferencesSource {
    fun getString(key: String): String?

    fun putString(
        key: String,
        value: String,
    )

    fun remove(key: String)

    fun clear()
}

class SecurePreferences(context: Context) : PreferencesSource {
    private val prefs: SharedPreferences by lazy {
        val masterKey =
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        EncryptedSharedPreferences.create(
            context,
            "connectwithkia_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(
        key: String,
        value: String,
    ) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}

class InMemoryPreferences : PreferencesSource {
    private val store = mutableMapOf<String, String>()

    override fun getString(key: String): String? = store[key]

    override fun putString(
        key: String,
        value: String,
    ) {
        store[key] = value
    }

    override fun remove(key: String) {
        store.remove(key)
    }

    override fun clear() {
        store.clear()
    }
}
