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
    private val credentialProvider: CredentialProvider = CredentialProvider { null },
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    enableLogging: Boolean = false,
    okHttpClient: OkHttpClient = buildDefaultOkHttpClient(enableLogging),
) : KiaClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val api: KiaApi =
        Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(KiaApi::class.java)

    constructor(tokenStorage: TokenStorage, credentialProvider: CredentialProvider) : this(
        baseUrl = "https://kiaconnect.ca/",
        tokenStorage = tokenStorage,
        credentialProvider = credentialProvider,
    )

    constructor(tokenStorage: TokenStorage, credentialProvider: CredentialProvider, enableLogging: Boolean) : this(
        baseUrl = "https://kiaconnect.ca/",
        tokenStorage = tokenStorage,
        credentialProvider = credentialProvider,
        enableLogging = enableLogging,
    )

    override suspend fun login(
        email: String,
        password: String,
    ): Result<Unit> =
        runCatching {
            val response = api.login(LoginRequest(email, password))
            val body = response.body()
            require(response.isSuccessful && body != null) { "login failed: HTTP ${response.code()}" }
            tokenStorage.writeAccessToken(
                token = body.accessToken,
                expiresAtEpochMs = clockMs() + body.expiresIn * 1000,
            )
        }

    override suspend fun vehicles(): Result<List<Vehicle>> =
        runCatching {
            val token = requireNotNull(tokenStorage.readAccessToken()) { "not logged in" }
            val response = api.vehicles(token)
            val body = response.body()
            require(response.isSuccessful && body != null) { "vehicles failed: HTTP ${response.code()}" }
            body.vehicles.map { Vehicle(id = it.vehicleId, nickname = it.nickName, vin = it.vin) }
        }

    override suspend fun lock(
        vehicleId: String,
        pin: String,
    ): Result<Unit> =
        runCatching {
            suspend fun attempt(): retrofit2.Response<com.github.y3knik.connectwithkia.kia.internal.PinResponse> {
                val token = requireNotNull(tokenStorage.readAccessToken()) { "not logged in" }
                return api.verifyPin(token, com.github.y3knik.connectwithkia.kia.internal.PinRequest(pin))
            }

            var pinResponse = attempt()
            if (pinResponse.code() == 401) {
                val creds =
                    credentialProvider.current()
                        ?: error("401 from preauth and no credentials available to refresh")
                login(creds.email, creds.password).getOrThrow()
                pinResponse = attempt()
            }
            val pAuth = pinResponse.body()?.pAuth
            require(pinResponse.isSuccessful && pAuth != null) { "pin verify failed: HTTP ${pinResponse.code()}" }

            val token = requireNotNull(tokenStorage.readAccessToken()) { "not logged in" }
            val lockResponse = api.lock(token, pAuth, vehicleId)
            require(lockResponse.isSuccessful) { "lock failed: HTTP ${lockResponse.code()}" }
        }

    companion object {
        private fun buildDefaultOkHttpClient(enableLogging: Boolean): OkHttpClient {
            val builder =
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
            if (enableLogging) {
                val logger = com.github.y3knik.connectwithkia.kia.internal.RedactingLogger { println(it) }
                builder.addInterceptor(
                    okhttp3.logging.HttpLoggingInterceptor(logger)
                        .setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BODY),
                )
            }
            return builder.build()
        }
    }
}
