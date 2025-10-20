package com.firebase.ui.auth.compose.testutil

import java.net.HttpURLConnection
import java.net.URL

internal class HttpClient(
    private val host: String,
    private val port: Int,
    private val timeoutMs: Int = DEFAULT_TIMEOUT_MS
) {

    companion object {
        const val DEFAULT_TIMEOUT_MS = 5_000
    }

    fun delete(path: String, block: (HttpURLConnection) -> Unit) {
        execute(path = path, method = "DELETE", block = block)
    }

    fun <T> get(path: String, block: (HttpURLConnection) -> T): T {
        return execute(path = path, method = "GET", block = block)
    }

    private fun <T> execute(path: String, method: String, block: (HttpURLConnection) -> T): T {
        val connection = buildUrl(path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs

        return try {
            block(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrl(path: String): URL {
        return URL("http://$host:$port$path")
    }
}
