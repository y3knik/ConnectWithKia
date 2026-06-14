package com.github.y3knik.connectwithkia.kia.internal

import okhttp3.logging.HttpLoggingInterceptor

/**
 * Sanitizes sensitive header and JSON-body values out of HttpLoggingInterceptor's output
 * BEFORE they are written to the log sink. The actual outgoing HTTP request is unmodified
 * — secrets reach the server unchanged.
 *
 * Wire as: `HttpLoggingInterceptor(RedactingLogger { line -> ... })`.
 */
internal class RedactingLogger(private val sink: (String) -> Unit) : HttpLoggingInterceptor.Logger {
    private val secretHeaders = setOf("Accesstoken", "pAuth")
    private val secretJsonFields = listOf("password", "pin", "accessToken", "pAuth")

    override fun log(message: String) {
        sink(sanitize(message))
    }

    private fun sanitize(line: String): String {
        val withHeaders =
            secretHeaders.fold(line) { acc, header ->
                acc.replace(
                    Regex("($header:\\s*)\\S+", RegexOption.IGNORE_CASE),
                    "$1REDACTED",
                )
            }
        return secretJsonFields.fold(withHeaders) { acc, field ->
            acc.replace(Regex("\"$field\"\\s*:\\s*\"[^\"]*\""), "\"$field\":\"REDACTED\"")
        }
    }
}
