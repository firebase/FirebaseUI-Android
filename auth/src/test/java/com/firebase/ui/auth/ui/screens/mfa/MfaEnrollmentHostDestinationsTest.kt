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
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.ui.screens.AuthSuccessUiContext
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactor
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.TotpMultiFactorAssertion
import com.google.firebase.auth.TotpMultiFactorGenerator
import com.google.firebase.auth.TotpSecret as FirebaseTotpSecret
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [FirebaseAuthScreen] installs the MFA enrolment flow through [mfaEnrollmentDestinations] and
 * supplies its own `onComplete` and `onSkip`. [MfaEnrollmentRouteNavigationTest] wires those two
 * itself, so it pins [exitMfaEnrollment] but not the host's choice to call it.
 *
 * These drive the real screen from the "Manage MFA" callback all the way out, so what they pin is
 * the wiring: a completed or skipped enrolment leaves the flow rather than stranding the user on
 * the step underneath.
 *
 * The emulator cannot perform a real TOTP enrolment, so the secret and the assertion are mocked -
 * the same approach [MfaEnrollmentRouteNavigationTest] takes.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class MfaEnrollmentHostDestinationsTest {

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

    private var mfaState: MfaEnrollmentContentState? = null
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
        `when`(mockUser.uid).thenReturn("mfa-host-user")
        `when`(mockUser.email).thenReturn("user@example.com")
        `when`(mockUser.isEmailVerified).thenReturn(true)
        `when`(mockUser.multiFactor).thenReturn(mockMultiFactor)
        `when`(mockMultiFactor.enrolledFactors).thenReturn(emptyList())
        authUI = FirebaseAuthUI.create(app, mockAuth)
    }

    @After
    fun tearDown() {
        mfaState = null
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
    fun `a successful enrolment through the main screen leaves the flow`() {
        withMockedTotpEnrollment {
            signInAndEnterEnrollment()
            selectFactor(MfaFactor.Totp)
            assertStep(MfaEnrollmentStep.ConfigureTotp)
            continueToVerify()
            assertStep(MfaEnrollmentStep.VerifyFactor)

            typeVerificationCode()
            verifyFactor()

            assertLeftTheFlow()
        }
    }

    @Test
    fun `a skip through the main screen leaves the flow`() {
        signInAndEnterEnrollment()
        selectFactor(MfaFactor.Sms)
        assertStep(MfaEnrollmentStep.ConfigureSms)

        skipEnrollment()

        assertLeftTheFlow()
    }

    // Harness

    /**
     * Renders the real screen, signs in so the success destination is the whole back stack, then
     * enters the flow through the callback the "Manage MFA" control uses.
     */
    private fun signInAndEnterEnrollment() {
        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = mfaEnabledConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                mfaConfiguration = MfaConfiguration(
                    allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),
                    requireEnrollment = false,
                ),
                mfaEnrollmentContent = { state ->
                    mfaState = state
                    // Tagged with the step name, so a strand reports which step it stranded on.
                    Text(text = state.step.toString(), modifier = Modifier.testTag(MFA_STEP_TAG))
                },
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
        assertStep(MfaEnrollmentStep.SelectFactor)
    }

    /** No MFA step composed, and the destination the flow was entered from is back. */
    private fun assertLeftTheFlow() {
        composeTestRule.onNodeWithTag(MFA_STEP_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
    }

    private fun assertStep(step: MfaEnrollmentStep) {
        assertThat(requireNotNull(mfaState).step).isEqualTo(step)
    }

    private fun selectFactor(factor: MfaFactor) {
        composeTestRule.runOnIdle { requireNotNull(mfaState).onFactorSelected(factor) }
        composeTestRule.waitForIdle()
    }

    private fun continueToVerify() {
        composeTestRule.runOnIdle { requireNotNull(mfaState).onContinueToVerifyClick() }
        composeTestRule.waitForIdle()
    }

    private fun typeVerificationCode() {
        composeTestRule.runOnIdle {
            requireNotNull(mfaState).onVerificationCodeChange(VERIFICATION_CODE)
        }
        composeTestRule.waitForIdle()
    }

    private fun verifyFactor() {
        composeTestRule.runOnIdle { requireNotNull(mfaState).onVerifyClick() }
        composeTestRule.waitForIdle()
    }

    private fun skipEnrollment() {
        composeTestRule.runOnIdle {
            requireNotNull(requireNotNull(mfaState).onSkipClick).invoke()
        }
        composeTestRule.waitForIdle()
    }

    private fun mfaEnabledConfiguration(): AuthUIConfiguration = authUIConfiguration {
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
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        )
    }

    /**
     * Stubs the TOTP secret, the enrolment assertion and [MultiFactor.enroll] so a TOTP enrolment
     * completes synchronously, for the duration of [block]. Every action and assertion depending
     * on the stubs must run inside [block].
     */
    private fun withMockedTotpEnrollment(block: () -> Unit) {
        val mockSession = mock(MultiFactorSession::class.java)
        val mockSecret = mock(FirebaseTotpSecret::class.java)
        val mockAssertion = mock(TotpMultiFactorAssertion::class.java)
        `when`(mockMultiFactor.session).thenReturn(Tasks.forResult(mockSession))
        `when`(mockMultiFactor.enroll(any(), any())).thenReturn(Tasks.forResult<Void>(null))
        `when`(mockSecret.sharedSecretKey).thenReturn(FAKE_SHARED_SECRET)
        `when`(mockSecret.generateQrCodeUrl(any(), any())).thenReturn(FAKE_QR_URL)

        mockStatic(TotpMultiFactorGenerator::class.java).use { totpStatic ->
            totpStatic.`when`<Task<FirebaseTotpSecret>> {
                TotpMultiFactorGenerator.generateSecret(mockSession)
            }.thenReturn(Tasks.forResult(mockSecret))
            totpStatic.`when`<TotpMultiFactorAssertion> {
                TotpMultiFactorGenerator.getAssertionForEnrollment(mockSecret, VERIFICATION_CODE)
            }.thenReturn(mockAssertion)

            block()
        }
    }

    private companion object {
        const val MFA_STEP_TAG = "mfa-enrollment-step"
        const val AUTHENTICATED_TAG = "authenticated-destination"
        const val VERIFICATION_CODE = "123456"
        const val FAKE_SHARED_SECRET = "JBSWY3DPEHPK3PXP"
        const val FAKE_QR_URL =
            "otpauth://totp/test-issuer:user%40example.com?secret=JBSWY3DPEHPK3PXP"
    }
}
