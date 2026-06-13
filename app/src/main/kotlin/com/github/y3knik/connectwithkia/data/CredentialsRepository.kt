package com.github.y3knik.connectwithkia.data

data class StoredCredentials(val email: String, val password: String, val pin: String) {
    override fun toString(): String = "StoredCredentials(email=$email, password=***, pin=***)"
}

class CredentialsRepository(private val prefs: PreferencesSource) {

    val hasCompleteCredentials: Boolean
        get() = read() != null

    fun read(): StoredCredentials? {
        val email = prefs.getString(KEY_EMAIL) ?: return null
        val password = prefs.getString(KEY_PASSWORD) ?: return null
        val pin = prefs.getString(KEY_PIN) ?: return null
        return StoredCredentials(email, password, pin)
    }

    fun write(email: String, password: String, pin: String) {
        prefs.putString(KEY_EMAIL, email)
        prefs.putString(KEY_PASSWORD, password)
        prefs.putString(KEY_PIN, pin)
    }

    var vehicleId: String?
        get() = prefs.getString(KEY_VEHICLE_ID)
        set(value) {
            if (value == null) prefs.remove(KEY_VEHICLE_ID)
            else prefs.putString(KEY_VEHICLE_ID, value)
        }

    fun clear() {
        prefs.remove(KEY_EMAIL)
        prefs.remove(KEY_PASSWORD)
        prefs.remove(KEY_PIN)
        prefs.remove(KEY_VEHICLE_ID)
    }

    private companion object {
        const val KEY_EMAIL = "kia_email"
        const val KEY_PASSWORD = "kia_password"
        const val KEY_PIN = "kia_pin"
        const val KEY_VEHICLE_ID = "kia_vehicle_id"
    }
}
