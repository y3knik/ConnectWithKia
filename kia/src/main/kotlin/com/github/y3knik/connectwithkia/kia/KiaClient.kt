package com.github.y3knik.connectwithkia.kia

interface KiaClient {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun vehicles(): Result<List<Vehicle>>
    suspend fun lock(vehicleId: String, pin: String): Result<Unit>
}

data class Vehicle(
    val id: String,
    val nickname: String,
    val vin: String,
)
