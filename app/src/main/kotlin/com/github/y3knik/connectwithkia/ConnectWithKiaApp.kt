package com.github.y3knik.connectwithkia

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.y3knik.connectwithkia.detect.CarConnectionObserver
import com.github.y3knik.connectwithkia.detect.CarConnectionState
import com.github.y3knik.connectwithkia.di.AppContainer
import com.github.y3knik.connectwithkia.scheduler.LockEvent

class ConnectWithKiaApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.get(this)

        val observer = CarConnectionObserver(this)
        var lastState: CarConnectionState? = null
        observer.observe(ProcessLifecycleOwner.get()) { state ->
            if (state == lastState) return@observe
            lastState = state
            val event =
                when (state) {
                    CarConnectionState.NOT_CONNECTED -> LockEvent.AaDisconnected
                    CarConnectionState.PROJECTION, CarConnectionState.NATIVE -> LockEvent.AaConnected
                }
            container.scheduler.onEvent(event)
        }
    }
}
