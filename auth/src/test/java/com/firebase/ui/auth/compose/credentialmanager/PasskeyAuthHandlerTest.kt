/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.compose.credentialmanager

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.SecureRandom

/**
 * Unit tests for [PasskeyAuthHandler].
 *
 * Note: These tests focus on JSON generation and validation logic.
 * Testing the actual Credential Manager integration requires instrumented tests
 * with proper device/emulator setup.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PasskeyAuthHandlerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // =============================================================================================
    // JSON Request Generation Tests
    // =============================================================================================

    @Test
    fun `createRegistrationRequestJson generates valid JSON structure`() {
        val challenge = generateRandomChallenge()
        val userId = "user123"
        val userName = "user@example.com"
        val displayName = "John Doe"
        val rpId = "example.com"
        val rpName = "Example App"

        val json = PasskeyAuthHandler.createRegistrationRequestJson(
            challenge = challenge,
            userId = userId,
            userName = userName,
            displayName = displayName,
            rpId = rpId,
            rpName = rpName
        )

        val jsonObject = JSONObject(json)

        // Verify top-level fields
        assertThat(jsonObject.has("challenge")).isTrue()
        assertThat(jsonObject.getString("challenge")).isEqualTo(challenge)
        assertThat(jsonObject.has("rp")).isTrue()
        assertThat(jsonObject.has("user")).isTrue()
        assertThat(jsonObject.has("pubKeyCredParams")).isTrue()
        assertThat(jsonObject.has("timeout")).isTrue()
        assertThat(jsonObject.has("attestation")).isTrue()
        assertThat(jsonObject.has("authenticatorSelection")).isTrue()
    }

    @Test
    fun `createRegistrationRequestJson includes correct RP information`() {
        val challenge = generateRandomChallenge()
        val rpId = "example.com"
        val rpName = "Example App"

        val json = PasskeyAuthHandler.createRegistrationRequestJson(
            challenge = challenge,
            userId = "user123",
            userName = "user@example.com",
            displayName = "John Doe",
            rpId = rpId,
            rpName = rpName
        )

        val jsonObject = JSONObject(json)
        val rp = jsonObject.getJSONObject("rp")

        assertThat(rp.getString("name")).isEqualTo(rpName)
        assertThat(rp.getString("id")).isEqualTo(rpId)
    }

    @Test
    fun `createRegistrationRequestJson includes correct user information`() {
        val challenge = generateRandomChallenge()
        val userId = "user123"
        val userName = "user@example.com"
        val displayName = "John Doe"

        val json = PasskeyAuthHandler.createRegistrationRequestJson(
            challenge = challenge,
            userId = userId,
            userName = userName,
            displayName = displayName,
            rpId = "example.com",
            rpName = "Example App"
        )

        val jsonObject = JSONObject(json)
        val user = jsonObject.getJSONObject("user")

        assertThat(user.getString("id")).isEqualTo(userId)
        assertThat(user.getString("name")).isEqualTo(userName)
        assertThat(user.getString("displayName")).isEqualTo(displayName)
    }

    @Test
    fun `createRegistrationRequestJson includes ES256 and RS256 algorithms`() {
        val challenge = generateRandomChallenge()

        val json = PasskeyAuthHandler.createRegistrationRequestJson(
            challenge = challenge,
            userId = "user123",
            userName = "user@example.com",
            displayName = "John Doe",
            rpId = "example.com",
            rpName = "Example App"
        )

        val jsonObject = JSONObject(json)
        val pubKeyCredParams = jsonObject.getJSONArray("pubKeyCredParams")

        assertThat(pubKeyCredParams.length()).isEqualTo(2)

        val firstAlg = pubKeyCredParams.getJSONObject(0)
        assertThat(firstAlg.getString("type")).isEqualTo("public-key")
        assertThat(firstAlg.getInt("alg")).isEqualTo(-7) // ES256

        val secondAlg = pubKeyCredParams.getJSONObject(1)
        assertThat(secondAlg.getString("type")).isEqualTo("public-key")
        assertThat(secondAlg.getInt("alg")).isEqualTo(-257) // RS256
    }

    @Test
    fun `createRegistrationRequestJson includes authenticator selection`() {
        val challenge = generateRandomChallenge()

        val json = PasskeyAuthHandler.createRegistrationRequestJson(
            challenge = challenge,
            userId = "user123",
            userName = "user@example.com",
            displayName = "John Doe",
            rpId = "example.com",
            rpName = "Example App"
        )

        val jsonObject = JSONObject(json)
        val authSelection = jsonObject.getJSONObject("authenticatorSelection")

        assertThat(authSelection.getString("authenticatorAttachment")).isEqualTo("platform")
        assertThat(authSelection.getBoolean("requireResidentKey")).isTrue()
        assertThat(authSelection.getString("residentKey")).isEqualTo("required")
        assertThat(authSelection.getString("userVerification")).isEqualTo("required")
    }

    @Test
    fun `createRegistrationRequestJson sets timeout to 60 seconds`() {
        val challenge = generateRandomChallenge()

        val json = PasskeyAuthHandler.createRegistrationRequestJson(
            challenge = challenge,
            userId = "user123",
            userName = "user@example.com",
            displayName = "John Doe",
            rpId = "example.com",
            rpName = "Example App"
        )

        val jsonObject = JSONObject(json)
        assertThat(jsonObject.getInt("timeout")).isEqualTo(60000)
    }

    @Test
    fun `createRegistrationRequestJson sets attestation to none`() {
        val challenge = generateRandomChallenge()

        val json = PasskeyAuthHandler.createRegistrationRequestJson(
            challenge = challenge,
            userId = "user123",
            userName = "user@example.com",
            displayName = "John Doe",
            rpId = "example.com",
            rpName = "Example App"
        )

        val jsonObject = JSONObject(json)
        assertThat(jsonObject.getString("attestation")).isEqualTo("none")
    }

    @Test
    fun `createAuthenticationRequestJson generates valid JSON structure`() {
        val challenge = generateRandomChallenge()
        val rpId = "example.com"

        val json = PasskeyAuthHandler.createAuthenticationRequestJson(
            challenge = challenge,
            rpId = rpId
        )

        val jsonObject = JSONObject(json)

        assertThat(jsonObject.has("challenge")).isTrue()
        assertThat(jsonObject.getString("challenge")).isEqualTo(challenge)
        assertThat(jsonObject.has("timeout")).isTrue()
        assertThat(jsonObject.has("rpId")).isTrue()
        assertThat(jsonObject.getString("rpId")).isEqualTo(rpId)
        assertThat(jsonObject.has("userVerification")).isTrue()
        assertThat(jsonObject.getString("userVerification")).isEqualTo("required")
    }

    @Test
    fun `createAuthenticationRequestJson sets timeout to 60 seconds`() {
        val challenge = generateRandomChallenge()

        val json = PasskeyAuthHandler.createAuthenticationRequestJson(
            challenge = challenge,
            rpId = "example.com"
        )

        val jsonObject = JSONObject(json)
        assertThat(jsonObject.getInt("timeout")).isEqualTo(60000)
    }

    @Test
    fun `createAuthenticationRequestJson includes rpId`() {
        val challenge = generateRandomChallenge()
        val rpId = "example.com"

        val json = PasskeyAuthHandler.createAuthenticationRequestJson(
            challenge = challenge,
            rpId = rpId
        )

        val jsonObject = JSONObject(json)
        assertThat(jsonObject.getString("rpId")).isEqualTo(rpId)
    }

    // =============================================================================================
    // Challenge Generation Tests
    // =============================================================================================

    @Test
    fun `generated challenges are different`() {
        val challenge1 = generateRandomChallenge()
        val challenge2 = generateRandomChallenge()

        assertThat(challenge1).isNotEqualTo(challenge2)
    }

    @Test
    fun `generated challenges are base64 encoded`() {
        val challenge = generateRandomChallenge()

        // Base64 strings should only contain alphanumeric, +, /, and = characters
        val base64Regex = Regex("^[A-Za-z0-9+/]+=*$")
        assertThat(base64Regex.matches(challenge)).isTrue()
    }

    @Test
    fun `generated challenges are at least 32 bytes`() {
        val challenge = generateRandomChallenge()

        // Decode base64 to check byte length
        val decoded = Base64.decode(challenge, Base64.NO_WRAP)
        assertThat(decoded.size).isAtLeast(32)
    }

    // =============================================================================================
    // PasskeyAuthHandler Instantiation Tests
    // =============================================================================================

    // Note: Instantiation tests are skipped in unit tests because CredentialManager
    // requires an actual Android environment. These should be tested in instrumented tests.

    // =============================================================================================
    // JSON Validation Tests
    // =============================================================================================

    @Test
    fun `registration request JSON is valid for different user inputs`() {
        val testCases = listOf(
            Triple("user@example.com", "John Doe", "user123"),
            Triple("test+tag@domain.co.uk", "Test User", "test456"),
            Triple("unicode.user@example.com", "José García", "user789"),
            Triple("simple@test.com", "A", "1")
        )

        testCases.forEach { (userName, displayName, userId) ->
            val json = PasskeyAuthHandler.createRegistrationRequestJson(
                challenge = generateRandomChallenge(),
                userId = userId,
                userName = userName,
                displayName = displayName,
                rpId = "example.com",
                rpName = "Example App"
            )

            val jsonObject = JSONObject(json)
            val user = jsonObject.getJSONObject("user")

            assertThat(user.getString("name")).isEqualTo(userName)
            assertThat(user.getString("displayName")).isEqualTo(displayName)
            assertThat(user.getString("id")).isEqualTo(userId)
        }
    }

    @Test
    fun `authentication request JSON is valid for different rpIds`() {
        val testCases = listOf(
            "example.com",
            "subdomain.example.com",
            "app.test.co.uk",
            "localhost"
        )

        testCases.forEach { rpId ->
            val json = PasskeyAuthHandler.createAuthenticationRequestJson(
                challenge = generateRandomChallenge(),
                rpId = rpId
            )

            val jsonObject = JSONObject(json)
            assertThat(jsonObject.getString("rpId")).isEqualTo(rpId)
        }
    }

    // =============================================================================================
    // Helper Methods
    // =============================================================================================

    private fun generateRandomChallenge(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
