package com.github.y3knik.connectwithkia.di

import android.content.Context
import com.github.y3knik.connectwithkia.BuildConfig
import com.github.y3knik.connectwithkia.data.AppSettings
import com.github.y3knik.connectwithkia.data.CredentialsRepository
import com.github.y3knik.connectwithkia.data.KeystoreTokenStorage
import com.github.y3knik.connectwithkia.data.SecurePreferences
import com.github.y3knik.connectwithkia.kia.CredentialProvider
import com.github.y3knik.connectwithkia.kia.Credentials
import com.github.y3knik.connectwithkia.kia.DefaultKiaClient
import com.github.y3knik.connectwithkia.kia.KiaClient
import com.github.y3knik.connectwithkia.scheduler.AndroidAlarmDriver
import com.github.y3knik.connectwithkia.scheduler.LockScheduler

class AppContainer private constructor(context: Context) {
    val prefs = SecurePreferences(context.applicationContext)
    val settings = AppSettings(prefs)
    val credentials = CredentialsRepository(prefs)
    val tokenStorage = KeystoreTokenStorage(prefs)

    val kiaClient: KiaClient = DefaultKiaClient(
        tokenStorage = tokenStorage,
        credentialProvider = CredentialProvider {
            credentials.read()?.let { Credentials(it.email, it.password) }
        },
        enableLogging = BuildConfig.NETWORK_LOGS,
    )

    val alarmDriver = AndroidAlarmDriver(context.applicationContext)

    val scheduler = LockScheduler(
        kia = kiaClient,
        alarmDriver = alarmDriver,
        delayMinutes = { settings.lockDelayMinutes },
        credentials = {
            val c = credentials.read() ?: return@LockScheduler null
            val vid = credentials.vehicleId ?: return@LockScheduler null
            Triple(vid, c.pin, settings.enabled)
        },
    )

    companion object {
        @Volatile private var instance: AppContainer? = null
        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context).also { instance = it }
            }
    }
}
