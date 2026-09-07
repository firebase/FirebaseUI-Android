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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers a regression surfaced by code review on the stale-AuthState-reset fix: with multiple
 * providers configured, an Error occurring on a provider screen (e.g. Email) must not bounce the
 * user back to the method picker once the error dialog is consumed.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthScreenErrorNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    private lateinit var authUI: FirebaseAuthUI
    private lateinit var stringProvider: DefaultAuthUIStringProvider

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        FirebaseAuthUI.clearInstanceCache()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach { app -> app.delete() }

        val defaultApp = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )!!

        `when`(mockFirebaseAuth.app).thenReturn(defaultApp)

        authUI = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
        stringProvider = DefaultAuthUIStringProvider(context)
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach { app -> app.delete() }
    }

    @Test
    fun `error on email screen with multiple providers does not navigate back to method picker`() {
        val configuration = authUIConfiguration {
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
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
            )
        }

        // Navigate from the method picker into the Email screen.
        composeTestRule.onNodeWithText(stringProvider.signInWithEmail)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()

        // Trigger a plain error while on the Email screen.
        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.Error(Exception("boom")))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle)
            .assertIsDisplayed()

        // Dismiss the dialog.
        composeTestRule.onNodeWithText(stringProvider.dismissAction)
            .performClick()
        composeTestRule.waitForIdle()

        // We must still be on the Email screen, not bounced back to the method picker.
        composeTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringProvider.signInWithEmail)
            .assertIsNotDisplayed()
    }
}
