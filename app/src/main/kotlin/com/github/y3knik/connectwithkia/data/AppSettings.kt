package com.github.y3knik.connectwithkia.data

class AppSettings(private val prefs: PreferencesSource) {

    var enabled: Boolean
        get() = prefs.getString(KEY_ENABLED)?.toBoolean() ?: true
        set(value) { prefs.putString(KEY_ENABLED, value.toString()) }

    var highProminenceCountdown: Boolean
        get() = prefs.getString(KEY_HIGH_PROMINENCE)?.toBoolean() ?: true
        set(value) { prefs.putString(KEY_HIGH_PROMINENCE, value.toString()) }

    var successNotification: Boolean
        get() = prefs.getString(KEY_SUCCESS_NOTIF)?.toBoolean() ?: true
        set(value) { prefs.putString(KEY_SUCCESS_NOTIF, value.toString()) }

    var lockDelayMinutes: Int
        get() = prefs.getString(KEY_DELAY_MIN)?.toIntOrNull() ?: DEFAULT_DELAY_MIN
        set(value) { prefs.putString(KEY_DELAY_MIN, value.coerceIn(MIN_DELAY_MIN, MAX_DELAY_MIN).toString()) }

    private companion object {
        const val KEY_ENABLED = "settings_enabled"
        const val KEY_HIGH_PROMINENCE = "settings_high_prominence"
        const val KEY_SUCCESS_NOTIF = "settings_success_notif"
        const val KEY_DELAY_MIN = "settings_delay_min"
        const val DEFAULT_DELAY_MIN = 5
        const val MIN_DELAY_MIN = 1
        const val MAX_DELAY_MIN = 15
    }
}
