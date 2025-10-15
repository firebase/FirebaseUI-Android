package com.firebase.ui.auth.compose.testutil

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection

internal class EmulatorAuthApi(
    private val projectId: String,
    emulatorHost: String,
    emulatorPort: Int
) {

    private val httpClient = HttpClient(host = emulatorHost, port = emulatorPort)

    fun clearAccounts() {
        httpClient.delete("/emulator/v1/projects/$projectId/accounts") { connection ->
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                println("WARNING: Failed to clear emulator data: HTTP $responseCode")
            } else {
                println("TEST: Cleared emulator data")
            }
        }
    }

    fun fetchVerifyEmailCode(email: String): String {
        val oobCodes = fetchOobCodes()
        return (0 until oobCodes.length())
            .asSequence()
            .mapNotNull { index -> oobCodes.optJSONObject(index) }
            .firstOrNull { json ->
                json.optString("email") == email &&
                        json.optString("requestType") == "VERIFY_EMAIL"
            }
            ?.optString("oobCode")
            ?.takeIf { it.isNotBlank() }
            ?: throw Exception("No VERIFY_EMAIL OOB code found for user email: $email")
    }

    private fun fetchOobCodes(): JSONArray {
        val payload = httpClient.get("/emulator/v1/projects/$projectId/oobCodes") { connection ->
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Failed to get OOB codes: HTTP $responseCode")
            }

            connection.inputStream.bufferedReader().use { it.readText() }
        }

        return JSONObject(payload).optJSONArray("oobCodes") ?: JSONArray()
    }
}
