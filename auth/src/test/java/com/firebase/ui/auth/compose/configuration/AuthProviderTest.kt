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

package com.firebase.ui.auth.compose.configuration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.auth.actionCodeSettings
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [AuthProvider] covering provider validation rules, configuration constraints,
 * and error handling for all supported authentication providers.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AuthProviderTest {

    private lateinit var applicationContext: Context

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
    }

    // =============================================================================================
    // Email Provider Tests
    // =============================================================================================

    @Test
    fun `email provider with valid configuration should succeed`() {
        val provider = AuthProvider.Email(
            actionCodeSettings = null,
            passwordValidationRules = listOf()
        )

        provider.validate()
    }

    @Test
    fun `email provider with email link enabled and valid action code settings should succeed`() {
        val actionCodeSettings = actionCodeSettings {
            url = "https://example.com/verify"
            handleCodeInApp = true
        }

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            actionCodeSettings = actionCodeSettings,
            passwordValidationRules = listOf()
        )

        provider.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `email provider with email link enabled but null action code settings should throw`() {
        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            actionCodeSettings = null,
            passwordValidationRules = listOf()
        )

        provider.validate()
    }

    @Test(expected = IllegalStateException::class)
    fun `email provider with email link enabled but canHandleCodeInApp false should throw`() {
        val actionCodeSettings = actionCodeSettings {
            url = "https://example.com/verify"
            handleCodeInApp = false
        }

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            actionCodeSettings = actionCodeSettings,
            passwordValidationRules = listOf()
        )

        provider.validate()
    }

    // =============================================================================================
    // Phone Provider Tests
    // =============================================================================================

    @Test
    fun `phone provider with valid configuration should succeed`() {
        val provider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = null
        )

        provider.validate()
    }

    @Test
    fun `phone provider with valid default number should succeed`() {
        val provider = AuthProvider.Phone(
            defaultNumber = "+1234567890",
            defaultCountryCode = null,
            allowedCountries = null
        )

        provider.validate()
    }

    @Test(expected = IllegalStateException::class)
    fun `phone provider with invalid default number should throw`() {
        val provider = AuthProvider.Phone(
            defaultNumber = "invalid_number",
            defaultCountryCode = null,
            allowedCountries = null
        )

        provider.validate()
    }

    @Test
    fun `phone provider with valid default country code should succeed`() {
        val provider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = "US",
            allowedCountries = null
        )

        provider.validate()
    }

    @Test(expected = IllegalStateException::class)
    fun `phone provider with invalid default country code should throw`() {
        val provider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = "invalid",
            allowedCountries = null
        )

        provider.validate()
    }

    @Test
    fun `phone provider with valid allowed countries should succeed`() {
        val provider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = listOf("US", "CA", "+1")
        )

        provider.validate()
    }

    @Test(expected = IllegalStateException::class)
    fun `phone provider with invalid country in allowed list should throw`() {
        val provider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = listOf("US", "invalid_country")
        )

        provider.validate()
    }

    @Test
    fun `phone provider with valid default number, country code and compatible allowed countries should succeed`() {
        val provider = AuthProvider.Phone(
            defaultNumber = "+1234567890",
            defaultCountryCode = "US",
            allowedCountries = listOf("US", "CA")
        )

        provider.validate()
    }

    // =============================================================================================
    // Google Provider Tests
    // =============================================================================================

    @Test
    fun `google provider with valid configuration should succeed`() {
        val provider = AuthProvider.Google(
            scopes = listOf("email"),
            serverClientId = "test_client_id"
        )

        provider.validate(applicationContext)
    }

    // =============================================================================================
    // Facebook Provider Tests
    // =============================================================================================

    @Test
    fun `facebook provider with valid configuration should succeed`() {
        val provider = AuthProvider.Facebook(applicationId = "application_id")

        provider.validate(applicationContext)
    }

    // =============================================================================================
    // Anonymous Provider Tests
    // =============================================================================================

    @Test(expected = IllegalStateException::class)
    fun `anonymous provider as only provider should throw`() {
        val providers = listOf(AuthProvider.Anonymous)

        AuthProvider.Anonymous.validate(providers)
    }

    @Test
    fun `anonymous provider with other providers should succeed`() {
        val providers = listOf(
            AuthProvider.Anonymous,
            AuthProvider.Email(
                actionCodeSettings = null,
                passwordValidationRules = listOf()
            )
        )

        AuthProvider.Anonymous.validate(providers)
    }

    // =============================================================================================
    // GenericOAuth Provider Tests
    // =============================================================================================

    @Test
    fun `generic oauth provider with valid configuration should succeed`() {
        val provider = AuthProvider.GenericOAuth(
            providerId = "custom.provider",
            scopes = listOf("read"),
            customParameters = mapOf(),
            buttonLabel = "Sign in with Custom",
            buttonIcon = null,
            buttonColor = null
        )

        provider.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `generic oauth provider with blank provider id should throw`() {
        val provider = AuthProvider.GenericOAuth(
            providerId = "",
            scopes = listOf("read"),
            customParameters = mapOf(),
            buttonLabel = "Sign in with Custom",
            buttonIcon = null,
            buttonColor = null
        )

        provider.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `generic oauth provider with blank button label should throw`() {
        val provider = AuthProvider.GenericOAuth(
            providerId = "custom.provider",
            scopes = listOf("read"),
            customParameters = mapOf(),
            buttonLabel = "",
            buttonIcon = null,
            buttonColor = null
        )

        provider.validate()
    }
}