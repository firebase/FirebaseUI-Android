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

package com.firebase.ui.auth.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The single-provider configurations, where the provider's own step **is** the back stack's only
 * entry.
 *
 * `FirebaseAuthScreen` guards both provider steps' `onCancel` with `!skipsMethodPicker &&`, and
 * that short-circuit is the only thing standing between an email-only or phone-only configuration
 * and a back press that has nothing left to pop. Its two halves fail differently:
 *
 * * Drop the short-circuit and the flow rebuilds a method picker the configuration never offered —
 *   a user with one provider is dumped on a one-button chooser they can never leave.
 * * Drop the "is there anything to pop?" test that `popOrNull` performs before mutating, and the
 *   back stack empties; `NavDisplay` throws `IllegalArgumentException: NavDisplay backstack cannot
 *   be empty` from recomposition, so the crash names neither the callback nor this screen.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthScreenSingleProviderBackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var authUI: FirebaseAuthUI

    @Before
    fun setUp() {
        FirebaseAuthUI.clearInstanceCache()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach { it.delete() }
        val app = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )!!
        val auth = mock(FirebaseAuth::class.java)
        `when`(auth.app).thenReturn(app)
        authUI = FirebaseAuthUI.create(app, auth)
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    @Test
    fun `back at the root of an email-only flow stays on the email step`() {
        start(emailOnlyConfiguration())

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()

        // Repeated, not once: a mutation that only *sometimes* empties the stack would pass a
        // single press. Each press goes through the guarded cancel path.
        repeat(3) {
            composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.BACK_BUTTON).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD)
                .assertIsDisplayed()
            // The short-circuit held: no method picker was built for a configuration that offers
            // exactly one provider.
            composeTestRule.onAllNodesWithTag(FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST)
                .assertCountEquals(0)
        }
    }

    @Test
    fun `back at the root of a phone-only flow stays on the phone step`() {
        start(phoneOnlyConfiguration())

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.PhoneNumber.PHONE_NUMBER_FIELD)
            .assertIsDisplayed()

        repeat(3) {
            composeTestRule.onNodeWithTag(FirebaseAuthTestTags.PhoneNumber.BACK_BUTTON)
                .performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag(FirebaseAuthTestTags.PhoneNumber.PHONE_NUMBER_FIELD)
                .assertIsDisplayed()
            composeTestRule.onAllNodesWithTag(FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST)
                .assertCountEquals(0)
        }
    }

    /**
     * The other side of the same guard: with the method picker as the root, leaving the email flow
     * really does return to it, so the short-circuit is not simply suppressing the whole branch.
     */
    @Test
    fun `leaving the email flow returns to the method picker when one is configured`() {
        start(emailAndPhoneConfiguration())

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(EMAIL_PROVIDER_LABEL).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.BACK_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST)
            .assertIsDisplayed()
    }

    private fun start(configuration: AuthUIConfiguration) {
        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun emailOnlyConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = ApplicationProvider.getApplicationContext()
        providers {
            provider(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                )
            )
        }
        isCredentialManagerEnabled = false
    }

    private fun phoneOnlyConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = ApplicationProvider.getApplicationContext()
        providers {
            provider(
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = null,
                    allowedCountries = null
                )
            )
        }
        isCredentialManagerEnabled = false
    }

    private fun emailAndPhoneConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = ApplicationProvider.getApplicationContext()
        providers {
            provider(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                )
            )
            provider(
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = null,
                    allowedCountries = null
                )
            )
        }
        isCredentialManagerEnabled = false
    }

    private companion object {
        const val EMAIL_PROVIDER_LABEL = "Sign in with email"
    }
}
