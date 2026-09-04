package com.firebase.ui.auth.testutil

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection

class EmulatorAuthApi(
    private val projectId: String,
    emulatorHost: String,
    emulatorPort: Int,
) {

    private val httpClient = HttpClient(host = emulatorHost, port = emulatorPort)

    /**
     * Clears all data from the Firebase Auth Emulator.
     *
     * This function calls the emulator's clear data endpoint to remove all accounts,
     * OOB codes, and other authentication data. This ensures test isolation by providing
     * a clean slate for each test.
     *
     * Retries on transient failures (e.g. a loaded CI runner momentarily failing to respond)
     * and throws if the emulator still can't be cleared, so a broken reset fails the test
     * loudly instead of silently leaking stale accounts into the next test.
     */
    fun clearEmulatorData() {
        val maxRetries = 3
        var lastError: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                clearAccounts()
                return
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw e
            } catch (e: Exception) {
                lastError = e
                println("WARNING: Failed to clear emulator data (attempt $attempt/$maxRetries): ${e.message}")
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(500L * attempt)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw ie
                    }
                }
            }
        }
        throw IllegalStateException(
            "Failed to clear Firebase Auth Emulator data after $maxRetries attempts. " +
                    "Aborting test to avoid running against stale emulator state.",
            lastError
        )
    }

    fun clearAccounts() {
        httpClient.delete("/emulator/v1/projects/$projectId/accounts") { connection ->
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Failed to clear emulator data: HTTP $responseCode")
            }
            println("TEST: Cleared emulator data")
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

    fun fetchEmailSignInLink(email: String): String {
        val oobCodes = fetchOobCodes()
        return (0 until oobCodes.length())
            .asSequence()
            .mapNotNull { index -> oobCodes.optJSONObject(index) }
            .lastOrNull { json ->
                json.optString("email") == email &&
                        json.optString("requestType") == "EMAIL_SIGNIN"
            }
            ?.optString("oobLink")
            ?.takeIf { it.isNotBlank() }
            ?: throw Exception("No EMAIL_SIGNIN OOB link found for user email: $email")
    }

    fun fetchVerifyPhoneCode(phone: String): String {
        val payload =
            httpClient.get("/emulator/v1/projects/$projectId/verificationCodes") { connection ->
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Failed to get verification codes: HTTP $responseCode")
                }

                connection.inputStream.bufferedReader().use { it.readText() }
            }

        val verificationCodes = JSONObject(payload).optJSONArray("verificationCodes") ?: JSONArray()

        return (0 until verificationCodes.length())
            .asSequence()
            .mapNotNull { index -> verificationCodes.optJSONObject(index) }
            .lastOrNull { json ->
                val jsonPhone = json.optString("phoneNumber")
                // Try matching with and without country code prefix
                // The emulator may store the phone with a country code like +1, +49, etc.
                jsonPhone.endsWith(phone) ||
                        phone.endsWith(jsonPhone.removePrefix("+")) ||
                        jsonPhone == phone ||
                        jsonPhone == "+$phone"
            }
            ?.optString("code")
            ?.takeIf { it.isNotBlank() }
            ?: throw Exception("No phone verification code found for phone: $phone")
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
