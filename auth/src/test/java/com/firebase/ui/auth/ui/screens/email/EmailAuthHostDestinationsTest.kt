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

package com.firebase.ui.auth.ui.screens.email

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.firebase.ui.auth.ui.screens.reauth.ReauthSheetContent
import com.google.common.truth.Truth.assertThat
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
 * Both hosts of the email flow install the same destinations, through the same
 * [emailAuthDestinations] extension: [FirebaseAuthScreen]'s graph and the reauthentication
 * sheet's own one. Registering them in only one place is what lets the two drift.
 *
 * These drive the default email UI, so they also pin what the back arrow does: it steps back
 * through the flow rather than leaving it, even when email is the flow's start destination.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class EmailAuthHostDestinationsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: DefaultAuthUIStringProvider
    private lateinit var authUI: FirebaseAuthUI

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach { it.delete() }
        val app = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
        val auth = mock(FirebaseAuth::class.java)
        `when`(auth.app).thenReturn(app)
        authUI = FirebaseAuthUI.create(app, auth)
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    @Test
    fun `the main screen walks sign-in to sign-up and back, keeping the address`() {
        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD)
            .performTextInput(TYPED_EMAIL)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.SIGN_UP_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()

        // Email is this flow's start destination, so before the split there was nothing under
        // sign-up for the back arrow to reach and it did nothing at all.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.BACK_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
    }

    /**
     * A genuine reset to [com.firebase.ui.auth.AuthState.Idle] sends the flow back to its start
     * route. That start route is now the sign-in *step* rather than the whole email screen, so a
     * reset from a sub-step no longer silently keeps the sub-step — which is what a multi-provider
     * flow always did by returning to the method picker.
     */
    @Test
    fun `a genuine idle reset returns to the flow's start step`() {
        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.SIGN_UP_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertIsDisplayed()

        // Loading is not a notification, so the Idle that follows it is a real reset rather than
        // a consumed one-shot state.
        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Idle) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertDoesNotExist()
    }

    @Test
    fun `the reauthentication sheet registers the same email destinations`() {
        var dismissed = 0
        composeTestRule.setContent {
            ReauthSheetUnderTest(onDismiss = { dismissed++ })
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD)
            .performTextInput(TYPED_EMAIL)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.FORGOT_PASSWORD_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()

        // Reaching this destination at all proves the sheet registered it; the address proves it
        // was reached through the same argument-carrying navigation.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ResetPassword.EMAIL_FIELD)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ResetPassword.BACK_BUTTON).performClick()
        composeTestRule.waitForIdle()

        // Back inside the flow: the sheet stays up and the reauthentication is not abandoned.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        assertThat(dismissed).isEqualTo(0)
    }

    /**
     * The sheet registers four email destinations rather than one, so "which flow is open" and
     * "which step the user is on" are different facts. Exactly one thing records the step: the
     * sheet's own `rememberNavBackStack`, which serializes its keys — the address each carries
     * included — across recreation.
     *
     * `ReauthPresentationState` records the *flow*, and the built-in sheet never reads it — that
     * marker belongs to the custom reauth slot, whose email sub-flow is an unhosted
     * `EmailAuthScreen` keeping its own mode in `rememberSaveable`. So the two cannot disagree
     * about a step. This pins the half nothing covered: that the step, and the address it was
     * entered with, come back after a recreation rather than resetting to the flow's start step.
     */
    @Test
    fun `the reauthentication sheet keeps its email step across a recreation`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent { ReauthSheetUnderTest(onDismiss = {}) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD)
            .performTextInput(TYPED_EMAIL)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.FORGOT_PASSWORD_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ResetPassword.EMAIL_FIELD)
            .assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        // Still the step the user was on, not the flow's start step.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ResetPassword.EMAIL_FIELD)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertDoesNotExist()
        // And the address it was entered with, which travels as the route argument.
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()

        // Back still walks the flow rather than leaving it, so what was restored is the real stack
        // and not a single freshly created entry.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ResetPassword.BACK_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
    }

    @Composable
    private fun ReauthSheetUnderTest(onDismiss: () -> Unit) {
        CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
            ReauthSheetContent(
                authUI = authUI,
                reauthConfig = reauthConfiguration(),
                requestId = "request-id",
                activity = null,
                context = applicationContext,
                // Nothing prefilled, so the field stays editable and this test can type into it.
                prefillEmail = null,
                emailContent = null,
                phoneContent = null,
                mfaChallengeContent = null,
                mfaResolver = null,
                customMethodPickerLayout = null,
                onDismiss = onDismiss,
            )
        }
    }

    private fun emailConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
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

    private fun reauthConfiguration(): AuthUIConfiguration = emailConfiguration().copy(
        isAnonymousUpgradeEnabled = false,
        isCredentialLinkingEnabled = false,
        isNewEmailAccountsAllowed = false,
        isReauthenticationMode = true,
    )

    private companion object {
        const val TYPED_EMAIL = "user+tag@example.com"
    }
}
