package com.github.y3knik.connectwithkia.kia.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
internal data class LoginResponse(
    val accessToken: String,
    val expiresIn: Long,
)

@Serializable
internal data class VehiclesResponse(
    val vehicles: List<VehicleDto>,
)

@Serializable
internal data class VehicleDto(
    @SerialName("vehicleId") val vehicleId: String,
    @SerialName("nickName") val nickName: String,
    val vin: String,
)
