package com.github.y3knik.connectwithkia.detect

import android.content.Context
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class CarConnectionState { NOT_CONNECTED, PROJECTION, NATIVE }

interface CarConnectionStream {
    fun observe(owner: LifecycleOwner, onChange: (CarConnectionState) -> Unit)
    fun asFlow(): Flow<CarConnectionState>
}

class CarConnectionObserver(context: Context) : CarConnectionStream {
    private val carConnection = CarConnection(context.applicationContext)

    override fun observe(owner: LifecycleOwner, onChange: (CarConnectionState) -> Unit) {
        carConnection.type.observe(owner, Observer { type -> onChange(mapType(type)) })
    }

    override fun asFlow(): Flow<CarConnectionState> = callbackFlow {
        val observer = Observer<Int> { type -> trySend(mapType(type)) }
        carConnection.type.observeForever(observer)
        awaitClose { carConnection.type.removeObserver(observer) }
    }

    private fun mapType(type: Int): CarConnectionState = when (type) {
        CarConnection.CONNECTION_TYPE_PROJECTION -> CarConnectionState.PROJECTION
        CarConnection.CONNECTION_TYPE_NATIVE -> CarConnectionState.NATIVE
        else -> CarConnectionState.NOT_CONNECTED
    }
}
