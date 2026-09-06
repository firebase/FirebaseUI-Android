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

package com.firebase.ui.auth.ui.screens.mfa

import android.content.Context
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.AuthUITransitions
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.firebase.ui.auth.ui.screens.AuthSuccessUiContext
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactor
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
 * The SMS enrolment step rendered by the **default** MFA content, rather than through a
 * `mfaEnrollmentContent` slot.
 *
 * Every other enrolment test supplies that slot, which is why nothing caught the step reading the
 * hosting [AuthUIConfiguration]'s phone provider and crashing on a configuration that declares
 * none. SMS is a second factor, configured independently of phone sign-in: Firebase enables it
 * separately, and phone sign-in cannot carry a second factor at all — so an email-only
 * configuration is the ordinary case, not an edge case.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class MfaEnrollmentSmsStepTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockAuth: FirebaseAuth

    @Mock
    private lateinit var mockUser: FirebaseUser

    @Mock
    private lateinit var mockMultiFactor: MultiFactor

    private lateinit var applicationContext: Context
    private lateinit var authUI: FirebaseAuthUI

    private var uiContext: AuthSuccessUiContext? = null

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        applicationContext = ApplicationProvider.getApplicationContext()
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
        `when`(mockAuth.app).thenReturn(app)
        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("mfa-sms-step-user")
        `when`(mockUser.email).thenReturn("user@example.com")
        `when`(mockUser.isEmailVerified).thenReturn(true)
        `when`(mockUser.multiFactor).thenReturn(mockMultiFactor)
        `when`(mockMultiFactor.enrolledFactors).thenReturn(emptyList())
        authUI = FirebaseAuthUI.create(app, mockAuth)
    }

    @After
    fun tearDown() {
        uiContext = null
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    @Test
    fun `SMS step renders on a configuration that declares no phone provider`() {
        enterSmsEnrollment(MfaConfiguration(allowedFactors = listOf(MfaFactor.Sms)))

        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.PhoneNumber.PHONE_NUMBER_FIELD)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.PhoneNumber.SEND_CODE_BUTTON)
            .assertIsDisplayed()
    }

    /**
     * Renders the real screen with the default MFA content, signs in, then enters enrolment
     * through the callback the "Manage MFA" control uses. A single allowed factor starts the flow
     * on that factor's configuration step, so this lands directly on SMS.
     */
    private fun enterSmsEnrollment(mfaConfiguration: MfaConfiguration) {
        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailOnlyConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                mfaConfiguration = mfaConfiguration,
                authenticatedContent = { _, context ->
                    uiContext = context
                    Text(text = "authenticated", modifier = Modifier.testTag(AUTHENTICATED_TAG))
                },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Success(result = null, user = mockUser, isNewUser = false)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()

        composeTestRule.runOnIdle { requireNotNull(uiContext).onManageMfa() }
        composeTestRule.waitForIdle()
    }

    private fun emailOnlyConfiguration(): AuthUIConfiguration = authUIConfiguration {
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
        // The default fades would keep the destination being left composed alongside its successor.
        transitions = AuthUITransitions(
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            predictivePopTransitionSpec = {
                EnterTransition.None togetherWith ExitTransition.None
            },
        )
    }

    private companion object {
        const val AUTHENTICATED_TAG = "authenticated-destination"
    }
}
