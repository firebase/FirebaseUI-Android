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

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.data.CountryData
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.SmsEnrollmentSession
import com.firebase.ui.auth.mfa.TotpSecret
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.popOrNull
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactor
import com.google.firebase.auth.TotpSecret as FirebaseTotpSecret
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Proves, rather than predicts, what an Activity recreation does to
 * [MfaEnrollmentFlowState] mid-SMS-verification.
 *
 * A prior read-only review of [MfaEnrollmentDestinations.kt] concluded that all eight fields —
 * [smsSession] and [selectedCountry] included — were lost on recreation, [smsSession] leaving
 * [MfaEnrollmentContentState.onResendCodeClick] a silent no-op. [smsSession] and
 * [selectedCountry] now have hand-written `Saver`s
 * ([com.firebase.ui.auth.mfa.SmsEnrollmentSessionSaver],
 * [com.firebase.ui.auth.data.CountryDataSaver]) and are `rememberSaveable`, so this test asserts
 * they now *survive* recreation — restored values checked for equality against what was set
 * before, not just non-null. [totpSecret] and [totpQrCodeUrl] are unaffected by that fix and stay
 * lost: `com.google.firebase.auth.TotpSecret`'s only concrete implementation is obfuscated SDK
 * internal plumbing this library must not reference, so there is no `Saver` to write for it — see
 * [MfaEnrollmentFlowState] and [MfaEnrollmentTotpRegenerationTest] for how that loss is instead
 * handled by regenerating the secret rather than surviving recreation.
 *
 * This test drives the hosted flow to [AuthRoute.MfaEnrollment.VerifyFactor] for SMS with a live
 * session, recreates the Activity via [StateRestorationTester], and asserts the actual
 * post-recreation [MfaEnrollmentFlowState] — nothing here is asserted from prediction. It stops at
 * asserting the restored field values rather than also invoking `onResendCodeClick`/`onVerifyClick`
 * as the pre-fix version of this test did: with a real [SmsEnrollmentSession] now present after
 * restore, either callback would drive [com.firebase.ui.auth.mfa.SmsEnrollmentHandler] into a real
 * Firebase network call, which is [MfaEnrollmentRouteNavigationTest]'s and this module's SMS
 * enrollment tests' concern, not this file's — this file's concern is only whether
 * [MfaEnrollmentFlowState] itself survives.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class MfaEnrollmentFlowStateRestorationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockAuth: FirebaseAuth

    @Mock
    private lateinit var mockUser: FirebaseUser

    @Mock
    private lateinit var mockMultiFactor: MultiFactor

    private lateinit var authUI: FirebaseAuthUI

    private var backStack: NavBackStack<NavKey>? = null
    private var flowState: MfaEnrollmentFlowState? = null
    private var lastState: MfaEnrollmentContentState? = null

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
        )
        `when`(mockAuth.app).thenReturn(app)
        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("mfa-restoration-user")
        `when`(mockUser.email).thenReturn("user@example.com")
        `when`(mockUser.multiFactor).thenReturn(mockMultiFactor)
        `when`(mockMultiFactor.enrolledFactors).thenReturn(emptyList())
        authUI = FirebaseAuthUI.create(app, mockAuth)
    }

    @After
    fun tearDown() {
        backStack = null
        flowState = null
        lastState = null
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
    fun `recreation mid-SMS-verification now survives smsSession and selectedCountry, still drops the TOTP secret`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent { MfaFlowHost() }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { requireNotNull(lastState).onFactorSelected(MfaFactor.Sms) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            requireNotNull(lastState).onPhoneNumberChange(TYPED_PHONE_NUMBER)
        }
        composeTestRule.waitForIdle()

        // Stands in for onSendSmsCodeClick's own network call, the same way
        // MfaEnrollmentRouteNavigationTest.pushVerifyFactorDirectly avoids it: populates exactly
        // what a real send populates on flowState, then pushes VerifyFactor. totpSecret and
        // totpQrCodeUrl are populated too even though this is an SMS run, so the still-lost
        // plain-`remember` fields are exercised in the same test as the now-surviving ones.
        val fakeSession = SmsEnrollmentSession(
            verificationId = "verification-id",
            phoneNumber = "+1$TYPED_PHONE_NUMBER",
            forceResendingToken = null,
            sentAt = System.currentTimeMillis(),
        )
        val fakeCountry = CountryData(
            name = "United Kingdom",
            dialCode = "+44",
            countryCode = "GB",
            flagEmoji = "🇬🇧",
        )
        val fakeTotpSecret = TotpSecret.from(mock(FirebaseTotpSecret::class.java))
        composeTestRule.runOnIdle {
            val state = requireNotNull(flowState)
            state.smsSession.value = fakeSession
            state.totpSecret.value = fakeTotpSecret
            state.totpQrCodeUrl.value = FAKE_QR_URL
            state.selectedCountry.value = fakeCountry
            requireNotNull(backStack).navigateToMfaStep(AuthRoute.MfaEnrollment.VerifyFactor)
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            requireNotNull(lastState).onVerificationCodeChange(TYPED_VERIFICATION_CODE)
        }
        composeTestRule.waitForIdle()

        // Sanity on the pre-recreation state, so what follows is provably about recreation's
        // effect and not a fixture mistake.
        assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor)
        assertThat(requireNotNull(lastState).selectedFactor).isEqualTo(MfaFactor.Sms)
        assertThat(requireNotNull(lastState).phoneNumber).isEqualTo(TYPED_PHONE_NUMBER)
        assertThat(requireNotNull(lastState).verificationCode).isEqualTo(TYPED_VERIFICATION_CODE)
        assertThat(requireNotNull(flowState).smsSession.value).isEqualTo(fakeSession)
        assertThat(requireNotNull(flowState).totpSecret.value).isEqualTo(fakeTotpSecret)
        assertThat(requireNotNull(flowState).totpQrCodeUrl.value).isEqualTo(FAKE_QR_URL)
        assertThat(requireNotNull(flowState).selectedCountry.value).isEqualTo(fakeCountry)

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        // currentStep / the active destination: survives — rememberNavBackStack serializes the
        // AuthRoute keys, fields included.
        assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor)
        // selectedFactor, phoneNumber, verificationCode, resendTimerSeconds: rememberSaveable —
        // survive.
        assertThat(requireNotNull(lastState).selectedFactor).isEqualTo(MfaFactor.Sms)
        assertThat(requireNotNull(lastState).phoneNumber).isEqualTo(TYPED_PHONE_NUMBER)
        assertThat(requireNotNull(lastState).verificationCode).isEqualTo(TYPED_VERIFICATION_CODE)
        assertThat(requireNotNull(lastState).resendTimer).isEqualTo(0)
        // smsSession, selectedCountry: now rememberSaveable via a hand-written Saver — survive,
        // and survive as the *same* value, not merely a non-null one.
        assertThat(requireNotNull(flowState).smsSession.value).isEqualTo(fakeSession)
        assertThat(requireNotNull(flowState).selectedCountry.value).isEqualTo(fakeCountry)
        // The control this used to leave silently inert: onResendCodeClick is offered (Sms
        // survived as the selected factor) and now has a real session to read instead of null —
        // resending itself is SmsEnrollmentHandler's concern (real Firebase network I/O), not
        // this file's; the point here is only that flowState no longer hands it null.
        assertThat(requireNotNull(lastState).onResendCodeClick).isNotNull()

        // totpSecret, totpQrCodeUrl: still plain remember — still lost. Unaffected by this fix;
        // see MfaEnrollmentTotpRegenerationTest for how that loss is instead handled.
        assertThat(requireNotNull(flowState).totpSecret.value).isNull()
        assertThat(requireNotNull(flowState).totpQrCodeUrl.value).isNull()
    }

    @Composable
    private fun MfaFlowHost() {
        val stack = rememberNavBackStack(AuthRoute.MfaEnrollment.SelectFactor)
        val state = rememberMfaEnrollmentFlowState()
        SideEffect {
            backStack = stack
            flowState = state
        }

        NavDisplay(
            backStack = stack,
            onBack = { stack.popOrNull() },
            // Transitions would keep two MFA destinations composed at once, which has nothing to
            // do with the state-restoration behavior under test.
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            predictivePopTransitionSpec = { _ ->
                EnterTransition.None togetherWith ExitTransition.None
            },
            entryProvider = entryProvider {
                mfaEnrollmentDestinations(
                    backStack = stack,
                    configuration = MfaConfiguration(
                        allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),
                        requireEnrollment = false,
                    ),
                    authConfiguration = null,
                    authUI = authUI,
                    flowState = state,
                    content = { contentState -> lastState = contentState },
                    onComplete = {},
                    onSkip = {},
                    onError = {},
                )
            },
        )
    }

    private fun currentKey(): NavKey? = composeTestRule.runOnIdle {
        backStack?.lastOrNull()
    }

    private companion object {
        const val TYPED_PHONE_NUMBER = "5551234567"
        const val TYPED_VERIFICATION_CODE = "123456"
        const val FAKE_QR_URL = "otpauth://totp/test-issuer:user%40example.com?secret=FAKESECRET"
    }
}
