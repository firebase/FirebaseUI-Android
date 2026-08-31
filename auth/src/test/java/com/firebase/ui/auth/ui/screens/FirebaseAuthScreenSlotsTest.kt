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

import android.content.Context
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.method_picker.MethodPickerTermsConfiguration
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
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
 * Tests that [FirebaseAuthScreen] correctly forwards each customization slot to the
 * appropriate sub-screen.
 *
 * These tests cover the fix for the API gap where slots such as [customMethodPickerLayout],
 * [emailContent], and [phoneContent] were accepted by sub-screens but never reachable through
 * the high-level [FirebaseAuthScreen] composable.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthScreenSlotsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var authUI: FirebaseAuthUI

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(context).forEach { it.delete() }
        FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
        authUI = FirebaseAuthUI.getInstance()
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(context).forEach {
            try { it.delete() } catch (_: Exception) {}
        }
    }

    // =============================================================================================
    // customMethodPickerLayout slot tests
    // =============================================================================================

    @Test
    fun `customMethodPickerLayout is rendered when provided`() {
        val configuration = authUIConfiguration {
            context = this@FirebaseAuthScreenSlotsTest.context
            providers {
                provider(AuthProvider.Email(emailLinkActionCodeSettings = null, passwordValidationRules = emptyList()))
                provider(AuthProvider.Phone(defaultNumber = null, defaultCountryCode = null, allowedCountries = null))
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                customMethodPickerLayout = { _, _ ->
                    Text(
                        text = "Custom Picker",
                        modifier = Modifier.testTag("custom_method_picker")
                    )
                }
            )
        }

        composeTestRule.onNodeWithTag("custom_method_picker").assertIsDisplayed()
    }

    @Test
    fun `default method picker renders when customMethodPickerLayout is null`() {
        val configuration = authUIConfiguration {
            context = this@FirebaseAuthScreenSlotsTest.context
            providers {
                provider(AuthProvider.Email(emailLinkActionCodeSettings = null, passwordValidationRules = emptyList()))
                provider(AuthProvider.Phone(defaultNumber = null, defaultCountryCode = null, allowedCountries = null))
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {}
            )
        }

        composeTestRule.onNodeWithTag("AuthMethodPicker LazyColumn").assertIsDisplayed()
    }

    @Test
    fun `customMethodPickerLayout provided hides the default method picker chrome`() {
        val configuration = authUIConfiguration {
            context = this@FirebaseAuthScreenSlotsTest.context
            providers {
                provider(AuthProvider.Email(emailLinkActionCodeSettings = null, passwordValidationRules = emptyList()))
                provider(AuthProvider.Phone(defaultNumber = null, defaultCountryCode = null, allowedCountries = null))
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                customMethodPickerLayout = { _, _ ->
                    Text(
                        text = "Custom Picker",
                        modifier = Modifier.testTag("custom_method_picker")
                    )
                }
            )
        }

        composeTestRule.onNodeWithTag("custom_method_picker").assertIsDisplayed()
        // AuthMethodPicker (and with it, the logo/ToS footer it renders) must not exist at all —
        // customMethodPickerLayout now takes over the entire screen, it doesn't sit alongside them.
        composeTestRule.onNodeWithTag("AuthMethodPicker LazyColumn").assertDoesNotExist()
    }

    @Test
    fun `customMethodPickerTermsConfiguration is ignored when customMethodPickerLayout is provided`() {
        val configuration = authUIConfiguration {
            context = this@FirebaseAuthScreenSlotsTest.context
            providers {
                provider(AuthProvider.Email(emailLinkActionCodeSettings = null, passwordValidationRules = emptyList()))
                provider(AuthProvider.Phone(defaultNumber = null, defaultCountryCode = null, allowedCountries = null))
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                customMethodPickerLayout = { _, _ ->
                    Text(
                        text = "Custom Picker",
                        modifier = Modifier.testTag("custom_method_picker")
                    )
                },
                customMethodPickerTermsConfiguration = MethodPickerTermsConfiguration(
                    content = {
                        Checkbox(
                            checked = false,
                            onCheckedChange = {},
                            modifier = Modifier.testTag("terms_checkbox")
                        )
                    }
                )
            )
        }

        composeTestRule.onNodeWithTag("custom_method_picker").assertIsDisplayed()
        composeTestRule.onNodeWithTag("terms_checkbox").assertDoesNotExist()
    }

    @Test
    fun `customMethodPickerLayout renders full-screen in the reauthentication sheet too`() {
        val mockProviderInfo = mock(UserInfo::class.java)
        `when`(mockProviderInfo.providerId).thenReturn("password")
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("uid-custom-picker")
        `when`(mockUser.providerData).thenReturn(listOf(mockProviderInfo))

        val configuration = authUIConfiguration {
            context = this@FirebaseAuthScreenSlotsTest.context
            providers {
                provider(AuthProvider.Email(emailLinkActionCodeSettings = null, passwordValidationRules = emptyList()))
                provider(AuthProvider.Phone(defaultNumber = null, defaultCountryCode = null, allowedCountries = null))
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                customMethodPickerLayout = { _, _ ->
                    Text(
                        text = "Custom Reauth Picker",
                        modifier = Modifier.testTag("custom_reauth_picker")
                    )
                }
            )
        }

        authUI.updateAuthState(AuthState.Reauthentication.Required(mockUser))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("custom_reauth_picker").assertIsDisplayed()
        composeTestRule.onNodeWithTag("AuthMethodPicker LazyColumn").assertDoesNotExist()
    }

    // =============================================================================================
    // emailContent slot tests
    // =============================================================================================

    @Test
    fun `emailContent slot is rendered when provided`() {
        val configuration = authUIConfiguration {
            context = this@FirebaseAuthScreenSlotsTest.context
            providers {
                provider(AuthProvider.Email(emailLinkActionCodeSettings = null, passwordValidationRules = emptyList()))
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                emailContent = { _ ->
                    Text(
                        text = "Custom Email UI",
                        modifier = Modifier.testTag("custom_email_slot")
                    )
                }
            )
        }

        composeTestRule.onNodeWithTag("custom_email_slot").assertIsDisplayed()
    }

    // =============================================================================================
    // phoneContent slot tests
    // =============================================================================================

    @Test
    fun `phoneContent slot is rendered when provided`() {
        val configuration = authUIConfiguration {
            context = this@FirebaseAuthScreenSlotsTest.context
            providers {
                provider(AuthProvider.Phone(defaultNumber = null, defaultCountryCode = null, allowedCountries = null))
            }
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                phoneContent = { _ ->
                    Text(
                        text = "Custom Phone UI",
                        modifier = Modifier.testTag("custom_phone_slot")
                    )
                }
            )
        }

        composeTestRule.onNodeWithTag("custom_phone_slot").assertIsDisplayed()
    }
}
