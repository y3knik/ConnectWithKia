package com.github.y3knik.connectwithkia.kia

import com.github.y3knik.connectwithkia.kia.internal.KiaApi
import com.github.y3knik.connectwithkia.kia.internal.LoginRequest
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class DefaultKiaClient internal constructor(
    baseUrl: String,
    private val tokenStorage: TokenStorage,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    okHttpClient: OkHttpClient = defaultOkHttp(),
) : KiaClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val api: KiaApi = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(KiaApi::class.java)

    constructor(tokenStorage: TokenStorage) : this(
        baseUrl = "https://kiaconnect.ca/",
        tokenStorage = tokenStorage,
    )

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = api.login(LoginRequest(email, password))
        val body = response.body()
        require(response.isSuccessful && body != null) { "login failed: HTTP ${response.code()}" }
        tokenStorage.writeAccessToken(
            token = body.accessToken,
            expiresAtEpochMs = clockMs() + body.expiresIn * 1000,
        )
    }

    override suspend fun vehicles(): Result<List<Vehicle>> =
        Result.failure(NotImplementedError("vehicles() implemented in Task 8"))

    override suspend fun lock(vehicleId: String, pin: String): Result<Unit> =
        Result.failure(NotImplementedError("lock() implemented in Task 9"))

    companion object {
        private fun defaultOkHttp(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
    }
}
