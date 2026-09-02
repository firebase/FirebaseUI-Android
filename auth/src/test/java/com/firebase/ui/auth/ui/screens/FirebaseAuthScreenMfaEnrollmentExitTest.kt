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

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
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
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins the two exits [FirebaseAuthScreen] itself wires the MFA enrolment flow to — the `onComplete`
 * and `onSkip` arguments it hands
 * [com.firebase.ui.auth.ui.screens.mfa.mfaEnrollmentDestinations].
 *
 * `MfaEnrollmentRouteNavigationTest` covers what
 * [com.firebase.ui.auth.ui.screens.mfa.exitMfaEnrollment] does, but through that file's own host,
 * which supplies its own `onComplete` and `onSkip` — so it would stay green with the production
 * lines reverted to a single pop. These two tests drive the real screen, whose back stack is not
 * reachable from a test, and read the exit off what is on screen afterwards instead.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthScreenMfaEnrollmentExitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockAuth: FirebaseAuth

    @Mock
    private lateinit var mockUser: FirebaseUser

    @Mock
    private lateinit var mockMultiFactor: MultiFactor

    private lateinit var authUI: FirebaseAuthUI

    private var mfaState: MfaEnrollmentContentState? = null
    private var manageMfa: (() -> Unit)? = null

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
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
        `when`(mockAuth.app).thenReturn(app)
        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("mfa-exit-user")
        `when`(mockUser.email).thenReturn("user@example.com")
        `when`(mockUser.isEmailVerified).thenReturn(true)
        `when`(mockUser.multiFactor).thenReturn(mockMultiFactor)
        `when`(mockMultiFactor.enrolledFactors).thenReturn(emptyList())
        authUI = FirebaseAuthUI.create(app, mockAuth)
    }

    @After
    fun tearDown() {
        mfaState = null
        manageMfa = null
        FirebaseAuthUI.clearInstanceCache()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Skipping from the second step of the flow. One pop would leave
     * [MfaEnrollmentStep.SelectFactor] on screen, which is what the user was trying to get away
     * from.
     */
    @Test
    fun `skipping enrolment from a step below the first leaves the flow entirely`() {
        startSignedIn()
        enterEnrollment()
        selectFactor(MfaFactor.Sms)
        assertStepShown(MfaEnrollmentStep.ConfigureSms)

        runOnIdle { requireNotNull(requireNotNull(mfaState).onSkipClick).invoke() }

        assertNoStepShown()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
    }

    /**
     * Completing enrolment from [MfaEnrollmentStep.VerifyFactor], three pushes deep. Enrolment
     * itself is mocked out — the TOTP secret, the assertion and the `enroll` call — since none of
     * it can happen against a fake project.
     */
    @Test
    fun `completing enrolment from the last step leaves the flow entirely`() {
        withMockedTotpEnrollment {
            startSignedIn()
            enterEnrollment()
            selectFactor(MfaFactor.Totp)
            assertStepShown(MfaEnrollmentStep.ConfigureTotp)
            runOnIdle { requireNotNull(mfaState).onContinueToVerifyClick() }
            assertStepShown(MfaEnrollmentStep.VerifyFactor)

            runOnIdle { requireNotNull(mfaState).onVerificationCodeChange(VERIFICATION_CODE) }
            runOnIdle { requireNotNull(mfaState).onVerifyClick() }

            assertNoStepShown()
            composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
        }
    }

    // =============================================================================================
    // Harness
    // =============================================================================================

    /** Hosts the real screen and puts it on its authenticated destination. */
    private fun startSignedIn() {
        val configuration = authUIConfiguration {
            context = ApplicationProvider.getApplicationContext()
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
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
                mfaEnrollmentContent = { state ->
                    mfaState = state
                    Text(text = "mfa", modifier = Modifier.testTag(stepTag(state.step)))
                },
                authenticatedContent = { _, uiContext ->
                    manageMfa = uiContext.onManageMfa
                    Text(text = "authenticated", modifier = Modifier.testTag(AUTHENTICATED_TAG))
                }
            )
        }

        runOnIdle {
            authUI.updateAuthState(
                AuthState.Success(result = null, user = mockUser, isNewUser = false)
            )
        }
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
    }

    /** Enters the flow the way the authenticated destination's "manage MFA" action does. */
    private fun enterEnrollment() {
        runOnIdle { requireNotNull(manageMfa).invoke() }
        assertStepShown(MfaEnrollmentStep.SelectFactor)
    }

    private fun selectFactor(factor: MfaFactor) {
        runOnIdle { requireNotNull(mfaState).onFactorSelected(factor) }
    }

    private fun assertStepShown(step: MfaEnrollmentStep) {
        composeTestRule.onNodeWithTag(stepTag(step)).assertIsDisplayed()
    }

    /** No step of the flow is on screen — not the one left, and not one underneath it either. */
    private fun assertNoStepShown() {
        MfaEnrollmentStep.entries.forEach { step ->
            composeTestRule.onNodeWithTag(stepTag(step)).assertDoesNotExist()
        }
    }

    private fun runOnIdle(block: () -> Unit) {
        composeTestRule.runOnIdle(block)
        composeTestRule.waitForIdle()
    }

    /**
     * Stubs the whole TOTP enrolment round trip for the duration of [block]: the multi-factor
     * session, [TotpMultiFactorGenerator.generateSecret], the enrolment assertion, and
     * [MultiFactor.enroll].
     */
    private fun withMockedTotpEnrollment(block: () -> Unit) {
        val session = mock(MultiFactorSession::class.java)
        val firebaseSecret = mock(FirebaseTotpSecret::class.java)
        val assertion = mock(TotpMultiFactorAssertion::class.java)
        `when`(mockMultiFactor.session).thenReturn(Tasks.forResult(session))
        `when`(firebaseSecret.sharedSecretKey).thenReturn(FAKE_SHARED_SECRET)
        `when`(firebaseSecret.generateQrCodeUrl(any(), any())).thenReturn(FAKE_QR_URL)
        `when`(mockMultiFactor.enroll(eq(assertion), any())).thenReturn(Tasks.forResult(null))

        mockStatic(TotpMultiFactorGenerator::class.java).use { totpStatic ->
            totpStatic.`when`<Task<FirebaseTotpSecret>> {
                TotpMultiFactorGenerator.generateSecret(session)
            }.thenReturn(Tasks.forResult(firebaseSecret))
            totpStatic.`when`<TotpMultiFactorAssertion> {
                TotpMultiFactorGenerator.getAssertionForEnrollment(
                    firebaseSecret,
                    VERIFICATION_CODE,
                )
            }.thenReturn(assertion)

            block()
        }
    }

    private companion object {
        const val AUTHENTICATED_TAG = "authenticated-destination"
        const val VERIFICATION_CODE = "123456"
        const val FAKE_SHARED_SECRET = "JBSWY3DPEHPK3PXP"
        const val FAKE_QR_URL =
            "otpauth://totp/test-issuer:user%40example.com?secret=JBSWY3DPEHPK3PXP"

        fun stepTag(step: MfaEnrollmentStep): String = "mfa-step-$step"
    }
}
