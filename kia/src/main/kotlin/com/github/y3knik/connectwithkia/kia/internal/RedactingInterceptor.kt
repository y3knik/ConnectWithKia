package com.github.y3knik.connectwithkia.kia.internal

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer

/**
 * Replaces sensitive header and body fields with REDACTED so they don't appear
 * in logs from HttpLoggingInterceptor. Must be installed BEFORE the logger.
 */
internal class RedactingInterceptor : Interceptor {
    private val secretHeaders = setOf("Accesstoken", "pAuth")
    private val secretJsonFields = listOf("password", "pin", "accessToken", "pAuth")

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val redacted = redactRequest(original)
        return chain.proceed(redacted)
    }

    private fun redactRequest(request: Request): Request {
        val builder = request.newBuilder()
        for (name in secretHeaders) {
            if (request.header(name) != null) builder.header(name, "REDACTED")
        }
        val body = request.body
        if (body != null) {
            val buffer = Buffer().also { body.writeTo(it) }
            val text = buffer.readUtf8()
            val sanitized = secretJsonFields.fold(text) { acc, field ->
                acc.replace(Regex("\"$field\"\\s*:\\s*\"[^\"]*\""), "\"$field\":\"REDACTED\"")
            }
            builder.method(request.method, sanitized.toRequestBody(body.contentType() ?: "application/json".toMediaTypeOrNull()))
        }
        return builder.build()
    }
}
