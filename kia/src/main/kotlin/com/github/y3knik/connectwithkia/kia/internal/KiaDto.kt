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
