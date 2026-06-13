package com.github.y3knik.connectwithkia.kia.internal

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface KiaApi {
    @POST("tods/api/lgn")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
