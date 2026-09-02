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
import androidx.compose.runtime.SideEffect
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
import com.firebase.ui.auth.data.CountryData
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.mfa.SmsEnrollmentSession
import com.firebase.ui.auth.mfa.TotpSecret
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.AuthSuccessUiContext
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.firebase.ui.auth.util.CountryUtils
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
 * [MfaEnrollmentFlowState] is remembered once by [FirebaseAuthScreen] above the `NavDisplay` and
 * handed to every step, and six of its nine fields are `rememberSaveable`. Nothing used to clear
 * it, so a second enrolment opened on a form still holding the first one's phone number,
 * verification code and consumed `smsSession` — across a sign-out, another user's data.
 *
 * These pin [MfaEnrollmentFlowState.reset] and the [enterMfaEnrollment] funnel that calls it:
 *
 * * both of the host's entry call sites — `onManageMfa` and `onNavigate` — independently, since
 *   either one alone reaching the funnel would leave the other still entering dirty;
 * * an entry naming a specific step clearing **and still landing on that step**, including the
 *   single-factor case where the named step is not the one [mfaEnrollmentStartStep] resolves to;
 * * every one of the nine fields going back to what [rememberMfaEnrollmentFlowState] created it
 *   with, asserted against the factory's own values rather than against restated literals.
 *
 * The emulator cannot perform a real TOTP enrolment, so the secret and the assertion are mocked —
 * the same approach [MfaEnrollmentHostDestinationsTest] takes.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class MfaEnrollmentEntryResetTest {

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
    private var flowState: MfaEnrollmentFlowState? = null

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
        `when`(mockUser.uid).thenReturn("mfa-entry-reset-user")
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
        flowState = null
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    // =============================================================================================
    // Re-entry after a completed enrolment, once per host entry call site
    // =============================================================================================

    /**
     * Entry site A: the `onManageMfa` callback the "Manage MFA" control on the success destination
     * invokes.
     */
    @Test
    fun `re-entry through onManageMfa starts on a blank form`() {
        withMockedTotpEnrollment {
            renderSignedIn()
            enterThroughManageMfa()
            dirtyEveryReachableFieldAndEnroll()

            enterThroughManageMfa()

            assertEnteredBlankOn(MfaEnrollmentStep.SelectFactor)
        }
    }

    /**
     * Entry site B: the `onNavigate` callback, which a host with its own `authenticatedContent`
     * drives. A separate call site from `onManageMfa`, so it is pinned separately — one of the two
     * reaching the funnel says nothing about the other.
     */
    @Test
    fun `re-entry through onNavigate starts on a blank form`() {
        withMockedTotpEnrollment {
            renderSignedIn()
            enterThroughNavigate(AuthRoute.MfaEnrollment)
            dirtyEveryReachableFieldAndEnroll()

            enterThroughNavigate(AuthRoute.MfaEnrollment)

            assertEnteredBlankOn(MfaEnrollmentStep.SelectFactor)
        }
    }

    // =============================================================================================
    // Entry naming a step: clear it, and still land where the caller asked
    // =============================================================================================

    /**
     * `onNavigate` takes any [AuthRoute], and every [AuthRoute.MfaEnrollment.Step] is a `NavKey` a
     * host may name directly — which is how the flow used to be entered with no clear at all.
     * Naming a step must clear, and must still land on the step named rather than on the flow's
     * resolved start step.
     */
    @Test
    fun `entry naming a step clears and lands on that step`() {
        renderSignedIn()
        enterThroughManageMfa()
        selectFactor(MfaFactor.Sms)
        typePhoneNumber()
        skipEnrollment()

        enterThroughNavigate(AuthRoute.MfaEnrollment.ConfigureSms)

        assertStep(MfaEnrollmentStep.ConfigureSms)
        assertThat(requireNotNull(mfaState).phoneNumber).isEmpty()
    }

    /**
     * Where "honour the step named" is load-bearing rather than incidental: a single-factor
     * configuration resolves flow entry straight to that factor's own step, so a host naming
     * [AuthRoute.MfaEnrollment.SelectFactor] is deliberately asking for the picker it would
     * otherwise never see. Resolving a named step through [mfaEnrollmentStartStep] would bounce it
     * to `ConfigureSms` and take that choice away.
     */
    @Test
    fun `entry naming SelectFactor under a single-factor configuration still lands on the picker`() {
        val smsOnly = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms),
            requireEnrollment = false,
        )
        renderSignedIn(smsOnly)
        enterThroughManageMfa(startStep = MfaEnrollmentStep.ConfigureSms)
        typePhoneNumber()
        skipEnrollment()

        enterThroughNavigate(AuthRoute.MfaEnrollment.SelectFactor)

        assertThat(mfaEnrollmentStartStep(smsOnly)).isEqualTo(AuthRoute.MfaEnrollment.ConfigureSms)
        assertStep(MfaEnrollmentStep.SelectFactor)
        assertThat(requireNotNull(mfaState).phoneNumber).isEmpty()
    }

    // =============================================================================================
    // reset() itself, field by field
    // =============================================================================================

    /**
     * Every field, against [rememberMfaEnrollmentFlowState]'s own initial value rather than against
     * a literal restated here — the factory is the authority on what "initial" means, and a reset
     * that drifts from it (`selectedCountry` back to `null` instead of
     * [CountryUtils.getDefaultCountry], say) is exactly the kind of drift a restated literal would
     * hide.
     */
    @Test
    fun `reset returns all nine fields to their initial values`() {
        composeTestRule.setContent {
            val state = rememberMfaEnrollmentFlowState()
            SideEffect { flowState = state }
        }
        composeTestRule.waitForIdle()

        val state = requireNotNull(flowState)
        val initialFactor = state.selectedFactor.value
        val initialPhoneNumber = state.phoneNumber.value
        val initialVerificationCode = state.verificationCode.value
        val initialResendTimer = state.resendTimerSeconds.intValue
        val initialSmsSession = state.smsSession.value
        val initialTotpSecret = state.totpSecret.value
        val initialTotpQrCodeUrl = state.totpQrCodeUrl.value
        val initialCountry = state.selectedCountry.value
        val initialExpiredMessage = state.totpSecretExpiredMessage.value

        composeTestRule.runOnIdle {
            state.selectedFactor.value = MfaFactor.Totp
            state.phoneNumber.value = TYPED_PHONE_NUMBER
            state.verificationCode.value = VERIFICATION_CODE
            state.resendTimerSeconds.intValue = DIRTY_RESEND_SECONDS
            state.smsSession.value = fakeSmsSession()
            state.totpSecret.value = TotpSecret.from(mock(FirebaseTotpSecret::class.java))
            state.totpQrCodeUrl.value = FAKE_QR_URL
            state.selectedCountry.value = FAKE_COUNTRY
            state.totpSecretExpiredMessage.value = "expired"
        }
        composeTestRule.waitForIdle()

        // No field may be dirtied to the value it already had, or resetting it would be untested.
        assertThat(state.selectedFactor.value).isNotEqualTo(initialFactor)
        assertThat(state.phoneNumber.value).isNotEqualTo(initialPhoneNumber)
        assertThat(state.verificationCode.value).isNotEqualTo(initialVerificationCode)
        assertThat(state.resendTimerSeconds.intValue).isNotEqualTo(initialResendTimer)
        assertThat(state.smsSession.value).isNotEqualTo(initialSmsSession)
        assertThat(state.totpSecret.value).isNotEqualTo(initialTotpSecret)
        assertThat(state.totpQrCodeUrl.value).isNotEqualTo(initialTotpQrCodeUrl)
        assertThat(state.selectedCountry.value).isNotEqualTo(initialCountry)
        assertThat(state.totpSecretExpiredMessage.value).isNotEqualTo(initialExpiredMessage)

        composeTestRule.runOnIdle { state.reset() }
        composeTestRule.waitForIdle()

        assertThat(state.selectedFactor.value).isEqualTo(initialFactor)
        assertThat(state.phoneNumber.value).isEqualTo(initialPhoneNumber)
        assertThat(state.verificationCode.value).isEqualTo(initialVerificationCode)
        assertThat(state.resendTimerSeconds.intValue).isEqualTo(initialResendTimer)
        assertThat(state.smsSession.value).isEqualTo(initialSmsSession)
        assertThat(state.totpSecret.value).isEqualTo(initialTotpSecret)
        assertThat(state.totpQrCodeUrl.value).isEqualTo(initialTotpQrCodeUrl)
        assertThat(state.selectedCountry.value).isEqualTo(initialCountry)
        assertThat(state.totpSecretExpiredMessage.value).isEqualTo(initialExpiredMessage)
    }

    // =============================================================================================
    // Harness
    // =============================================================================================

    /** Renders the real screen and signs in, so the success destination is the whole back stack. */
    private fun renderSignedIn(
        mfaConfiguration: MfaConfiguration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),
            requireEnrollment = false,
        ),
    ) {
        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = mfaEnabledConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                mfaConfiguration = mfaConfiguration,
                mfaEnrollmentContent = { state ->
                    mfaState = state
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
    }

    /** Host entry site A. */
    private fun enterThroughManageMfa(
        startStep: MfaEnrollmentStep = MfaEnrollmentStep.SelectFactor,
    ) {
        composeTestRule.runOnIdle { requireNotNull(uiContext).onManageMfa() }
        composeTestRule.waitForIdle()
        assertStep(startStep)
    }

    /** Host entry site B. */
    private fun enterThroughNavigate(route: AuthRoute) {
        composeTestRule.runOnIdle { requireNotNull(uiContext).onNavigate(route) }
        composeTestRule.waitForIdle()
    }

    /**
     * Walks the two-factor flow through every field a host test can dirty — the SMS phone
     * number and country, then the TOTP secret, QR URL and verification code — and enrols
     * successfully, so what a re-entry finds is a *completed* attempt's leftovers rather than an
     * abandoned one's.
     */
    private fun dirtyEveryReachableFieldAndEnroll() {
        selectFactor(MfaFactor.Sms)
        typePhoneNumber()
        composeTestRule.runOnIdle { requireNotNull(mfaState).onCountrySelected(FAKE_COUNTRY) }
        composeTestRule.waitForIdle()
        assertThat(requireNotNull(mfaState).selectedCountry).isEqualTo(FAKE_COUNTRY)

        // Back out of the SMS branch without leaving the flow, so the TOTP branch can be walked
        // with the SMS fields left dirty behind it.
        composeTestRule.runOnIdle { requireNotNull(mfaState).onBackClick() }
        composeTestRule.waitForIdle()
        assertStep(MfaEnrollmentStep.SelectFactor)

        selectFactor(MfaFactor.Totp)
        assertStep(MfaEnrollmentStep.ConfigureTotp)
        assertThat(requireNotNull(mfaState).totpSecret).isNotNull()
        assertThat(requireNotNull(mfaState).totpQrCodeUrl).isEqualTo(FAKE_QR_URL)

        composeTestRule.runOnIdle { requireNotNull(mfaState).onContinueToVerifyClick() }
        composeTestRule.waitForIdle()
        assertStep(MfaEnrollmentStep.VerifyFactor)

        composeTestRule.runOnIdle {
            requireNotNull(mfaState).onVerificationCodeChange(VERIFICATION_CODE)
        }
        composeTestRule.waitForIdle()
        assertThat(requireNotNull(mfaState).verificationCode).isEqualTo(VERIFICATION_CODE)

        composeTestRule.runOnIdle { requireNotNull(mfaState).onVerifyClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(MFA_STEP_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
    }

    /**
     * Every field the content slot exposes *and* a host test can dirty, back to its initial value.
     * The other three are covered by `reset returns all nine fields to their initial values`:
     * `smsSession` and `totpSecretExpiredMessage` are not on [MfaEnrollmentContentState], and
     * `resendTimer` is written only by a real SMS send, which no host test can reach —
     * `PhoneAuthOptions` casts the `MultiFactorSession` to a `final` Firebase-internal class whose
     * obfuscated name moves between firebase-auth releases.
     */
    private fun assertEnteredBlankOn(step: MfaEnrollmentStep) {
        val state = requireNotNull(mfaState)
        assertThat(state.step).isEqualTo(step)
        assertThat(state.selectedFactor).isNull()
        assertThat(state.phoneNumber).isEmpty()
        assertThat(state.verificationCode).isEmpty()
        assertThat(state.totpSecret).isNull()
        assertThat(state.totpQrCodeUrl).isNull()
        assertThat(state.selectedCountry).isEqualTo(CountryUtils.getDefaultCountry())
    }

    private fun assertStep(step: MfaEnrollmentStep) {
        assertThat(requireNotNull(mfaState).step).isEqualTo(step)
    }

    private fun selectFactor(factor: MfaFactor) {
        composeTestRule.runOnIdle { requireNotNull(mfaState).onFactorSelected(factor) }
        composeTestRule.waitForIdle()
    }

    private fun typePhoneNumber() {
        composeTestRule.runOnIdle {
            requireNotNull(mfaState).onPhoneNumberChange(TYPED_PHONE_NUMBER)
        }
        composeTestRule.waitForIdle()
        assertThat(requireNotNull(mfaState).phoneNumber).isEqualTo(TYPED_PHONE_NUMBER)
    }

    private fun skipEnrollment() {
        composeTestRule.runOnIdle {
            requireNotNull(requireNotNull(mfaState).onSkipClick).invoke()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
    }

    private fun fakeSmsSession() = SmsEnrollmentSession(
        verificationId = "verification-id",
        phoneNumber = "+1$TYPED_PHONE_NUMBER",
        forceResendingToken = null,
        sentAt = System.currentTimeMillis(),
    )

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
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            predictivePopTransitionSpec = {
                EnterTransition.None togetherWith ExitTransition.None
            },
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
        const val TYPED_PHONE_NUMBER = "5551234567"
        const val VERIFICATION_CODE = "123456"
        const val DIRTY_RESEND_SECONDS = 30
        const val FAKE_SHARED_SECRET = "JBSWY3DPEHPK3PXP"
        const val FAKE_QR_URL =
            "otpauth://totp/test-issuer:user%40example.com?secret=JBSWY3DPEHPK3PXP"

        /** Deliberately not [CountryUtils.getDefaultCountry]'s value under the test locale. */
        val FAKE_COUNTRY = CountryData(
            name = "United Kingdom",
            dialCode = "+44",
            countryCode = "GB",
            flagEmoji = "🇬🇧",
        )
    }
}
