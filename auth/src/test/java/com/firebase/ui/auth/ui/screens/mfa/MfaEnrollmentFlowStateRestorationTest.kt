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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.data.CountryData
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.SmsEnrollmentSession
import com.firebase.ui.auth.mfa.TotpSecret
import com.firebase.ui.auth.ui.screens.AuthRoute
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
 * Asserts which [MfaEnrollmentFlowState] fields survive an Activity recreation
 * mid-SMS-verification.
 *
 * Drives the hosted flow to [AuthRoute.MfaEnrollment.VerifyFactor] for SMS with a live session,
 * recreates the Activity via [StateRestorationTester], then checks the restored values for
 * equality with what was set before — not merely for non-null. `smsSession` and `selectedCountry`
 * survive via their hand-written `Saver`s; `totpSecret` and `totpQrCodeUrl` do not, and
 * [MfaEnrollmentTotpRegenerationTest] covers how that loss is recovered.
 *
 * Guards the regression where a lost `smsSession` left
 * [MfaEnrollmentContentState.onResendCodeClick] a silent no-op.
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

    private var navController: NavHostController? = null
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
        navController = null
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

        // Stands in for onSendSmsCodeClick, which would make a real SMS network call.
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
            requireNotNull(navController).navigateToMfaStep(AuthRoute.MfaEnrollment.VerifyFactor)
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            requireNotNull(lastState).onVerificationCodeChange(TYPED_VERIFICATION_CODE)
        }
        composeTestRule.waitForIdle()

        // Sanity on the pre-recreation state, so a fixture mistake cannot pass as a loss.
        assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor.routePattern)
        assertThat(requireNotNull(lastState).selectedFactor).isEqualTo(MfaFactor.Sms)
        assertThat(requireNotNull(lastState).phoneNumber).isEqualTo(TYPED_PHONE_NUMBER)
        assertThat(requireNotNull(lastState).verificationCode).isEqualTo(TYPED_VERIFICATION_CODE)
        assertThat(requireNotNull(flowState).smsSession.value).isEqualTo(fakeSession)
        assertThat(requireNotNull(flowState).totpSecret.value).isEqualTo(fakeTotpSecret)
        assertThat(requireNotNull(flowState).totpQrCodeUrl.value).isEqualTo(FAKE_QR_URL)
        assertThat(requireNotNull(flowState).selectedCountry.value).isEqualTo(fakeCountry)

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        // The active route: NavController's own Saver restores the back stack.
        assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor.routePattern)
        // rememberSaveable fields survive.
        assertThat(requireNotNull(lastState).selectedFactor).isEqualTo(MfaFactor.Sms)
        assertThat(requireNotNull(lastState).phoneNumber).isEqualTo(TYPED_PHONE_NUMBER)
        assertThat(requireNotNull(lastState).verificationCode).isEqualTo(TYPED_VERIFICATION_CODE)
        assertThat(requireNotNull(lastState).resendTimer).isEqualTo(0)
        // smsSession, selectedCountry: restored as the *same* value, not merely a non-null one.
        assertThat(requireNotNull(flowState).smsSession.value).isEqualTo(fakeSession)
        assertThat(requireNotNull(flowState).selectedCountry.value).isEqualTo(fakeCountry)
        // The control the loss used to leave silently inert.
        assertThat(requireNotNull(lastState).onResendCodeClick).isNotNull()

        // totpSecret, totpQrCodeUrl: plain remember, so still lost.
        assertThat(requireNotNull(flowState).totpSecret.value).isNull()
        assertThat(requireNotNull(flowState).totpQrCodeUrl.value).isNull()
    }

    @Composable
    private fun MfaFlowHost() {
        val controller = rememberNavController()
        val state = rememberMfaEnrollmentFlowState()
        SideEffect {
            navController = controller
            flowState = state
        }

        NavHost(
            navController = controller,
            startDestination = AuthRoute.MfaEnrollment.SelectFactor.routePattern,
            // Transitions would keep two MFA destinations composed at once.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            mfaEnrollmentDestinations(
                navController = controller,
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
        }
    }

    private fun currentRoute(): String? = composeTestRule.runOnIdle {
        navController?.currentBackStackEntry?.destination?.route
    }

    private companion object {
        const val TYPED_PHONE_NUMBER = "5551234567"
        const val TYPED_VERIFICATION_CODE = "123456"
        const val FAKE_QR_URL = "otpauth://totp/test-issuer:user%40example.com?secret=FAKESECRET"
    }
}
