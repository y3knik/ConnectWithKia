package com.github.y3knik.connectwithkia.kia.internal

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface KiaApi {
    @POST("tods/api/lgn")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("tods/api/vhcllst")
    suspend fun vehicles(@retrofit2.http.Header("Accesstoken") accessToken: String): Response<VehiclesResponse>

    @POST("tods/api/vrfypin")
    suspend fun verifyPin(
        @retrofit2.http.Header("Accesstoken") accessToken: String,
        @Body request: PinRequest,
    ): Response<PinResponse>

    @POST("tods/api/drlck")
    suspend fun lock(
        @retrofit2.http.Header("Accesstoken") accessToken: String,
        @retrofit2.http.Header("pAuth") pAuth: String,
        @retrofit2.http.Header("vehicleId") vehicleId: String,
    ): Response<LockResponse>
}
