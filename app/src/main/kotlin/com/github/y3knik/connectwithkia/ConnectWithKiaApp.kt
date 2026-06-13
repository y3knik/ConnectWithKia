package com.github.y3knik.connectwithkia

import android.app.Application
import com.github.y3knik.connectwithkia.di.AppContainer

class ConnectWithKiaApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.get(this)
    }
}
