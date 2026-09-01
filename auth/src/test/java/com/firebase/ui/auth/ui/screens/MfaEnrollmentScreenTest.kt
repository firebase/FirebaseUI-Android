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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.data.CountryData
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.mfa.SmsEnrollmentHandler
import com.firebase.ui.auth.mfa.SmsEnrollmentSession
import com.firebase.ui.auth.mfa.TotpEnrollmentHandler
import com.firebase.ui.auth.mfa.TotpSecret
import com.firebase.ui.auth.ui.screens.mfa.MfaEnrollmentScreen
import com.firebase.ui.auth.ui.screens.mfa.MfaEnrollmentScreenInternal
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.PhoneAuthProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import com.google.firebase.auth.TotpSecret as FirebaseTotpSecret

/**
 * Unit tests for [MfaEnrollmentScreen].
 *
 * These tests focus on the state management logic and callbacks provided
 * through the content slot. UI rendering is not tested here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class MfaEnrollmentScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockAuth: FirebaseAuth

    @Mock
    private lateinit var mockUser: FirebaseUser

    @Mock
    private lateinit var mockFirebaseApp: FirebaseApp

    @Mock
    private lateinit var mockMultiFactor: com.google.firebase.auth.MultiFactor

    @Mock
    private lateinit var mockSmsHandler: SmsEnrollmentHandler

    @Mock
    private lateinit var mockTotpHandler: TotpEnrollmentHandler

    @Mock
    private lateinit var mockFirebaseTotpSecret: FirebaseTotpSecret

    private lateinit var totpSecret: TotpSecret

    private lateinit var smsSession: SmsEnrollmentSession

    private lateinit var capturedState: MfaEnrollmentContentState

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        FirebaseApp.initializeApp(RuntimeEnvironment.getApplication())
        `when`(mockAuth.app).thenReturn(mockFirebaseApp)
        `when`(mockFirebaseApp.name).thenReturn("TestApp")
        `when`(mockUser.email).thenReturn("test@example.com")
        `when`(mockUser.multiFactor).thenReturn(mockMultiFactor)
        `when`(mockMultiFactor.enrolledFactors).thenReturn(emptyList())
        `when`(mockFirebaseTotpSecret.sharedSecretKey).thenReturn("SECRET")
        `when`(mockFirebaseTotpSecret.generateQrCodeUrl("test@example.com", "TestApp"))
            .thenReturn("otpauth://totp/test@example.com?secret=SECRET&issuer=TestApp")
        totpSecret = TotpSecret.from(mockFirebaseTotpSecret)
        whenever { mockTotpHandler.generateSecret() }.thenReturn(totpSecret)
        smsSession = SmsEnrollmentSession(
            verificationId = "verification-id",
            phoneNumber = EXPECTED_FULL_PHONE_NUMBER,
            forceResendingToken = mock<PhoneAuthProvider.ForceResendingToken>(),
            sentAt = 0L
        )
    }

    @Test
    fun `screen starts at SelectFactor step with multiple factors`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),
            requireEnrollment = false
        )

        composeTestRule.setContent {
            MfaEnrollmentScreen(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                onComplete = {},
                onSkip = {}
            ) { state ->
                capturedState = state
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(MfaEnrollmentStep.SelectFactor, capturedState.step)
        assertEquals(2, capturedState.availableFactors.size)
        assertNotNull(capturedState.onSkipClick)
    }

    @Test
    fun `screen skips SelectFactor with single SMS factor`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms),
            requireEnrollment = false
        )

        composeTestRule.setContent {
            MfaEnrollmentScreen(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                onComplete = {},
                onSkip = {}
            ) { state ->
                capturedState = state
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(MfaEnrollmentStep.ConfigureSms, capturedState.step)
    }

    @Test
    fun `skip button is null when enrollment is required`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),
            requireEnrollment = true
        )

        composeTestRule.setContent {
            MfaEnrollmentScreen(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                onComplete = {},
                onSkip = {}
            ) { state ->
                capturedState = state
            }
        }

        composeTestRule.waitForIdle()
        assertNull(capturedState.onSkipClick)
        assertFalse(capturedState.canSkip)
    }

    @Test
    fun `selecting SMS factor navigates to ConfigureSms step`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp)
        )

        var currentState by mutableStateOf<MfaEnrollmentContentState?>(null)

        composeTestRule.setContent {
            MfaEnrollmentScreen(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                onComplete = {}
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(MfaEnrollmentStep.SelectFactor, currentState?.step)

        composeTestRule.runOnUiThread {
            currentState?.onFactorSelected?.invoke(MfaFactor.Sms)
        }

        composeTestRule.waitForIdle()
        assertEquals(MfaEnrollmentStep.ConfigureSms, currentState?.step)
    }

    @Test
    fun `phone number change updates state`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms)
        )

        var currentState by mutableStateOf<MfaEnrollmentContentState?>(null)

        composeTestRule.setContent {
            MfaEnrollmentScreen(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                onComplete = {}
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()
        assertEquals("", currentState?.phoneNumber)

        composeTestRule.runOnUiThread {
            currentState?.onPhoneNumberChange?.invoke("1234567890")
        }

        composeTestRule.waitForIdle()
        assertEquals("1234567890", currentState?.phoneNumber)
    }

    @Test
    fun `verification code change updates state`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms)
        )

        var currentState by mutableStateOf<MfaEnrollmentContentState?>(null)

        composeTestRule.setContent {
            MfaEnrollmentScreen(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                onComplete = {}
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()

        // Navigate to verify step manually by updating state
        composeTestRule.runOnUiThread {
            currentState?.onPhoneNumberChange?.invoke("1234567890")
        }

        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            currentState?.onVerificationCodeChange?.invoke("123456")
        }

        composeTestRule.waitForIdle()
        assertEquals("123456", currentState?.verificationCode)
    }

    @Test
    fun `back navigation works from ConfigureSms to SelectFactor`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp)
        )

        var currentState by mutableStateOf<MfaEnrollmentContentState?>(null)

        composeTestRule.setContent {
            MfaEnrollmentScreen(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                onComplete = {}
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            currentState?.onFactorSelected?.invoke(MfaFactor.Sms)
        }

        composeTestRule.waitForIdle()
        assertEquals(MfaEnrollmentStep.ConfigureSms, currentState?.step)

        composeTestRule.runOnUiThread {
            currentState?.onBackClick?.invoke()
        }

        composeTestRule.waitForIdle()
        assertEquals(MfaEnrollmentStep.SelectFactor, currentState?.step)
    }

    @Test
    fun `state validation works correctly`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms)
        )

        var currentState by mutableStateOf<MfaEnrollmentContentState?>(null)

        composeTestRule.setContent {
            MfaEnrollmentScreen(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                onComplete = {}
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()

        // ConfigureSms step - invalid when phone is blank
        assertFalse(currentState?.isValid ?: true)

        composeTestRule.runOnUiThread {
            currentState?.onPhoneNumberChange?.invoke("1234567890")
        }

        composeTestRule.waitForIdle()

        // ConfigureSms step - valid when phone is not blank
        assertTrue(currentState?.isValid ?: false)
    }

    @Test
    fun `canGoBack returns false for SelectFactor step`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp)
        )

        composeTestRule.setContent {
            MfaEnrollmentScreen(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                onComplete = {}
            ) { state ->
                capturedState = state
            }
        }

        composeTestRule.waitForIdle()
        assertFalse(capturedState.canGoBack)
    }

    @Test
    fun `canGoBack returns true for ConfigureSms step`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms)
        )

        composeTestRule.setContent {
            MfaEnrollmentScreen(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                onComplete = {}
            ) { state ->
                capturedState = state
            }
        }

        composeTestRule.waitForIdle()
        assertTrue(capturedState.canGoBack)
    }

    @Test
    fun `successful TOTP enrollment enrolls the entered code and fires onComplete once`() {
        val configuration = MfaConfiguration(allowedFactors = listOf(MfaFactor.Totp))
        var completeCount = 0
        val errors = mutableListOf<Exception>()

        val state = driveTotpFlowToVerifyStep(
            configuration = configuration,
            onComplete = { completeCount++ },
            onError = { errors.add(it) }
        )

        composeTestRule.runOnUiThread {
            state()?.onVerifyClick?.invoke()
        }
        composeTestRule.waitForIdle()

        // Exact arguments: the enrolled secret and code must be the ones the screen collected.
        verifyBlocking(mockTotpHandler) {
            enrollWithVerificationCode(
                totpSecret = totpSecret,
                verificationCode = VERIFICATION_CODE,
                displayName = TOTP_DISPLAY_NAME
            )
        }
        assertEquals(1, completeCount)
        assertEquals(emptyList<Exception>(), errors)
        // The screen does not navigate away on success; the host decides via onComplete.
        assertEquals(MfaEnrollmentStep.VerifyFactor, state()?.step)
        assertEquals(false, state()?.isLoading)
        assertNull(state()?.error)
        assertNull(state()?.exception)
        // The resend affordance is SMS-only: the TOTP route must not expose one.
        assertNotNull(state())
        assertNull(state()?.onResendCodeClick)
    }

    @Test
    fun `successful TOTP enrollment refreshes the enrolled factors from the user`() {
        val enrolledFactor = mock<MultiFactorInfo>()
        // First read is the initial state; the second is the post-enrollment refresh.
        `when`(mockMultiFactor.enrolledFactors)
            .thenReturn(emptyList(), listOf(enrolledFactor))

        val state = driveTotpFlowToVerifyStep(
            configuration = MfaConfiguration(allowedFactors = listOf(MfaFactor.Totp)),
            onComplete = {}
        )

        assertEquals(emptyList<MultiFactorInfo>(), state()?.enrolledFactors)

        composeTestRule.runOnUiThread {
            state()?.onVerifyClick?.invoke()
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(enrolledFactor), state()?.enrolledFactors)
    }

    @Test
    fun `failed TOTP enrollment reports the exception and does not fire onComplete`() {
        val configuration = MfaConfiguration(allowedFactors = listOf(MfaFactor.Totp))
        val failure = IllegalStateException("invalid verification code")
        whenever {
            mockTotpHandler.enrollWithVerificationCode(
                totpSecret = totpSecret,
                verificationCode = VERIFICATION_CODE,
                displayName = TOTP_DISPLAY_NAME
            )
        }.doThrow(failure)

        var completeCount = 0
        val errors = mutableListOf<Exception>()

        val state = driveTotpFlowToVerifyStep(
            configuration = configuration,
            onComplete = { completeCount++ },
            onError = { errors.add(it) }
        )

        composeTestRule.runOnUiThread {
            state()?.onVerifyClick?.invoke()
        }
        composeTestRule.waitForIdle()

        assertEquals(0, completeCount)
        assertEquals(listOf<Exception>(failure), errors)
        assertEquals(failure, state()?.exception)
        assertEquals("invalid verification code", state()?.error)
        assertEquals(MfaEnrollmentStep.VerifyFactor, state()?.step)
        assertEquals(false, state()?.isLoading)
    }

    @Test
    fun `successful SMS enrollment enrolls the entered code and fires onComplete once`() {
        var completeCount = 0
        val errors = mutableListOf<Exception>()

        val state = driveSmsFlowToVerifyStep(
            onComplete = { completeCount++ },
            onError = { errors.add(it) }
        )

        composeTestRule.runOnUiThread {
            state()?.onVerifyClick?.invoke()
        }
        composeTestRule.waitForIdle()

        // Exact arguments: the session must be the one sendVerificationCode handed back, the code
        // the one the screen collected, and the display name the SMS factor label.
        verifyBlocking(mockSmsHandler) {
            enrollWithVerificationCode(
                session = smsSession,
                verificationCode = VERIFICATION_CODE,
                displayName = SMS_DISPLAY_NAME
            )
        }
        assertEquals(1, completeCount)
        assertEquals(emptyList<Exception>(), errors)
        // The screen does not navigate away on success; the host decides via onComplete.
        // onComplete lives after the `when`, so this is the call site shared with the TOTP route.
        assertEquals(MfaEnrollmentStep.VerifyFactor, state()?.step)
        assertEquals(false, state()?.isLoading)
        assertNull(state()?.error)
        assertNull(state()?.exception)
    }

    @Test
    fun `sending the SMS code prefixes the selected dial code and stores the session`() {
        val configuration = MfaConfiguration(allowedFactors = listOf(MfaFactor.Sms))
        val errors = mutableListOf<Exception>()

        // Exact-argument stub: the screen must ask for the dial-code-prefixed number, and the
        // session it hands back is the one the screen has to remember.
        whenever { mockSmsHandler.sendVerificationCode(EXPECTED_FULL_PHONE_NUMBER) }
            .thenReturn(smsSession)

        var currentState by mutableStateOf<MfaEnrollmentContentState?>(null)

        composeTestRule.setContent {
            MfaEnrollmentScreenInternal(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                smsHandler = mockSmsHandler,
                totpHandler = mockTotpHandler,
                onComplete = {},
                onError = { errors.add(it) }
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(MfaEnrollmentStep.ConfigureSms, currentState?.step)

        composeTestRule.runOnUiThread {
            currentState?.onCountrySelected?.invoke(TEST_COUNTRY)
            currentState?.onPhoneNumberChange?.invoke(LOCAL_PHONE_NUMBER)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            currentState?.onSendSmsCodeClick?.invoke()
        }
        composeTestRule.waitForIdle()

        // The dial code of the selected country is prefixed to the entered local number.
        verifyBlocking(mockSmsHandler) {
            sendVerificationCode(EXPECTED_FULL_PHONE_NUMBER)
        }
        // A successful send advances to the verify step and clears any prior error.
        assertEquals(MfaEnrollmentStep.VerifyFactor, currentState?.step)
        assertEquals(emptyList<Exception>(), errors)
        assertNull(currentState?.error)
        assertNull(currentState?.exception)
        assertEquals(false, currentState?.isLoading)
        // A resend affordance only exists on the SMS route.
        assertNotNull(currentState?.onResendCodeClick)

        // The stored session is the one the send returned: enrolling reaches the handler with it
        // rather than failing on a missing session.
        composeTestRule.runOnUiThread {
            currentState?.onVerificationCodeChange?.invoke(VERIFICATION_CODE)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            currentState?.onVerifyClick?.invoke()
        }
        composeTestRule.waitForIdle()

        verifyBlocking(mockSmsHandler) {
            enrollWithVerificationCode(
                session = smsSession,
                verificationCode = VERIFICATION_CODE,
                displayName = SMS_DISPLAY_NAME
            )
        }
        assertEquals(emptyList<Exception>(), errors)
        assertNull(currentState?.error)
    }

    @Test
    fun `failed SMS send reports the exception and stays on the configure step`() {
        val configuration = MfaConfiguration(allowedFactors = listOf(MfaFactor.Sms))
        val failure = IllegalArgumentException("Phone number must be in E.164 format")
        whenever { mockSmsHandler.sendVerificationCode(EXPECTED_FULL_PHONE_NUMBER) }
            .doThrow(failure)

        var completeCount = 0
        val errors = mutableListOf<Exception>()

        var currentState by mutableStateOf<MfaEnrollmentContentState?>(null)

        composeTestRule.setContent {
            MfaEnrollmentScreenInternal(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                smsHandler = mockSmsHandler,
                totpHandler = mockTotpHandler,
                onComplete = { completeCount++ },
                onError = { errors.add(it) }
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(MfaEnrollmentStep.ConfigureSms, currentState?.step)

        composeTestRule.runOnUiThread {
            currentState?.onCountrySelected?.invoke(TEST_COUNTRY)
            currentState?.onPhoneNumberChange?.invoke(LOCAL_PHONE_NUMBER)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            currentState?.onSendSmsCodeClick?.invoke()
        }
        composeTestRule.waitForIdle()

        // onSendSmsCodeClick owns a catch block of its own, distinct from onVerifyClick's: a send
        // failure has to surface through it instead of being swallowed.
        assertEquals(listOf<Exception>(failure), errors)
        assertEquals(failure, currentState?.exception)
        assertEquals("Phone number must be in E.164 format", currentState?.error)
        // A failed send must not park the user on the verify step with no session.
        assertEquals(MfaEnrollmentStep.ConfigureSms, currentState?.step)
        assertEquals(false, currentState?.isLoading)
        assertEquals(0, completeCount)
    }

    @Test
    fun `SMS verification without a session reports the missing session and skips onComplete`() {
        val configuration = MfaConfiguration(allowedFactors = listOf(MfaFactor.Sms))
        var completeCount = 0
        val errors = mutableListOf<Exception>()

        var currentState by mutableStateOf<MfaEnrollmentContentState?>(null)

        composeTestRule.setContent {
            MfaEnrollmentScreenInternal(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                smsHandler = mockSmsHandler,
                totpHandler = mockTotpHandler,
                onComplete = { completeCount++ },
                onError = { errors.add(it) }
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(MfaEnrollmentStep.ConfigureSms, currentState?.step)

        // Reach the verify step without ever sending a code, so no SMS session exists. In
        // production this state is reached across process death: `currentStep` is
        // `rememberSaveable` and is restored, while `smsSession` is a plain `remember` and is not.
        // `onContinueToVerifyClick` is only the cheapest way to reproduce that step/session
        // mismatch here — the guard under test is `onVerifyClick`'s null-session branch, not this
        // callback, so tightening `onContinueToVerifyClick` to the TOTP route should not be read
        // as invalidating this test.
        composeTestRule.runOnUiThread {
            currentState?.onContinueToVerifyClick?.invoke()
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            currentState?.onVerificationCodeChange?.invoke(VERIFICATION_CODE)
        }
        composeTestRule.waitForIdle()
        assertEquals(MfaEnrollmentStep.VerifyFactor, currentState?.step)

        composeTestRule.runOnUiThread {
            currentState?.onVerifyClick?.invoke()
        }
        composeTestRule.waitForIdle()

        assertEquals(0, completeCount)
        assertEquals(1, errors.size)
        assertTrue(errors.single() is IllegalStateException)
        assertEquals(NO_SMS_SESSION_MESSAGE, errors.single().message)
        assertEquals(errors.single(), currentState?.exception)
        assertEquals(NO_SMS_SESSION_MESSAGE, currentState?.error)
        assertEquals(MfaEnrollmentStep.VerifyFactor, currentState?.step)
        assertEquals(false, currentState?.isLoading)
    }

    @Test
    fun `failed SMS enrollment reports the exception and does not fire onComplete`() {
        val failure = IllegalStateException("invalid verification code")
        whenever {
            mockSmsHandler.enrollWithVerificationCode(
                session = smsSession,
                verificationCode = VERIFICATION_CODE,
                displayName = SMS_DISPLAY_NAME
            )
        }.doThrow(failure)

        var completeCount = 0
        val errors = mutableListOf<Exception>()

        val state = driveSmsFlowToVerifyStep(
            onComplete = { completeCount++ },
            onError = { errors.add(it) }
        )

        composeTestRule.runOnUiThread {
            state()?.onVerifyClick?.invoke()
        }
        composeTestRule.waitForIdle()

        assertEquals(0, completeCount)
        assertEquals(listOf<Exception>(failure), errors)
        assertEquals(failure, state()?.exception)
        assertEquals("invalid verification code", state()?.error)
        assertEquals(MfaEnrollmentStep.VerifyFactor, state()?.step)
        assertEquals(false, state()?.isLoading)
    }

    @Test
    fun `resending the SMS code replaces the session used for enrollment`() {
        val resentSession = smsSession.copy(verificationId = "resent-verification-id")
        whenever { mockSmsHandler.resendVerificationCode(smsSession) }.thenReturn(resentSession)

        val errors = mutableListOf<Exception>()
        val state = driveSmsFlowToVerifyStep(onComplete = {}, onError = { errors.add(it) })

        // Resend is gated on the timer the send started: clicking while it still runs is a no-op,
        // which is the rate limit RESEND_DELAY_SECONDS exists to enforce.
        assertEquals(SmsEnrollmentHandler.RESEND_DELAY_SECONDS, state()?.resendTimer)
        composeTestRule.runOnUiThread {
            state()?.onResendCodeClick?.invoke()
        }
        composeTestRule.waitForIdle()
        verifyBlocking(mockSmsHandler, never()) { resendVerificationCode(smsSession) }

        // Run the timer down on the test clock so the gate opens.
        composeTestRule.mainClock.advanceTimeBy(
            SmsEnrollmentHandler.RESEND_DELAY_SECONDS * 1000L + 1000L
        )
        composeTestRule.waitForIdle()
        assertEquals(0, state()?.resendTimer)

        composeTestRule.runOnUiThread {
            state()?.onResendCodeClick?.invoke()
        }
        composeTestRule.waitForIdle()

        verifyBlocking(mockSmsHandler) { resendVerificationCode(smsSession) }
        assertEquals(emptyList<Exception>(), errors)
        // A successful resend restarts the timer, so the next click is gated again.
        assertEquals(SmsEnrollmentHandler.RESEND_DELAY_SECONDS, state()?.resendTimer)

        composeTestRule.runOnUiThread {
            state()?.onVerifyClick?.invoke()
        }
        composeTestRule.waitForIdle()

        // Enrollment must use the session returned by the resend, not the stale original.
        verifyBlocking(mockSmsHandler) {
            enrollWithVerificationCode(
                session = resentSession,
                verificationCode = VERIFICATION_CODE,
                displayName = SMS_DISPLAY_NAME
            )
        }
    }

    /**
     * Renders [MfaEnrollmentScreenInternal] with the stubbed enrollment handlers and drives the
     * SMS route up to (but not including) the verify click: select country, enter the local
     * number, send the code (which yields [smsSession]) and enter the verification code.
     *
     * @return an accessor for the latest [MfaEnrollmentContentState] emitted by the screen
     */
    private fun driveSmsFlowToVerifyStep(
        onComplete: () -> Unit,
        onError: (Exception) -> Unit = {}
    ): () -> MfaEnrollmentContentState? {
        // Exact-argument stub: the screen must ask for the dial-code-prefixed number.
        whenever { mockSmsHandler.sendVerificationCode(EXPECTED_FULL_PHONE_NUMBER) }
            .thenReturn(smsSession)

        var currentState by mutableStateOf<MfaEnrollmentContentState?>(null)

        composeTestRule.setContent {
            MfaEnrollmentScreenInternal(
                user = mockUser,
                auth = mockAuth,
                configuration = MfaConfiguration(allowedFactors = listOf(MfaFactor.Sms)),
                smsHandler = mockSmsHandler,
                totpHandler = mockTotpHandler,
                onComplete = onComplete,
                onError = onError
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()
        // A single allowed factor auto-selects SMS and lands on the configure step.
        assertEquals(MfaEnrollmentStep.ConfigureSms, currentState?.step)

        composeTestRule.runOnUiThread {
            currentState?.onCountrySelected?.invoke(TEST_COUNTRY)
            currentState?.onPhoneNumberChange?.invoke(LOCAL_PHONE_NUMBER)
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            currentState?.onSendSmsCodeClick?.invoke()
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            currentState?.onVerificationCodeChange?.invoke(VERIFICATION_CODE)
        }
        composeTestRule.waitForIdle()

        assertEquals(MfaEnrollmentStep.VerifyFactor, currentState?.step)
        assertEquals(VERIFICATION_CODE, currentState?.verificationCode)
        assertNull(currentState?.error)

        return { currentState }
    }

    /**
     * Renders [MfaEnrollmentScreenInternal] with the stubbed enrollment handlers and drives the
     * TOTP route up to (but not including) the verify click.
     *
     * @return an accessor for the latest [MfaEnrollmentContentState] emitted by the screen
     */
    private fun driveTotpFlowToVerifyStep(
        configuration: MfaConfiguration,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit = {}
    ): () -> MfaEnrollmentContentState? {
        var currentState by mutableStateOf<MfaEnrollmentContentState?>(null)

        composeTestRule.setContent {
            MfaEnrollmentScreenInternal(
                user = mockUser,
                auth = mockAuth,
                configuration = configuration,
                smsHandler = mockSmsHandler,
                totpHandler = mockTotpHandler,
                onComplete = onComplete,
                onError = onError
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()
        // A single allowed factor auto-selects TOTP and generates the secret.
        assertEquals(MfaEnrollmentStep.ConfigureTotp, currentState?.step)
        assertEquals(totpSecret, currentState?.totpSecret)

        composeTestRule.runOnUiThread {
            currentState?.onContinueToVerifyClick?.invoke()
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            currentState?.onVerificationCodeChange?.invoke(VERIFICATION_CODE)
        }
        composeTestRule.waitForIdle()

        assertEquals(MfaEnrollmentStep.VerifyFactor, currentState?.step)
        assertEquals(VERIFICATION_CODE, currentState?.verificationCode)

        return { currentState }
    }

    private companion object {
        const val VERIFICATION_CODE = "123456"
        const val TOTP_DISPLAY_NAME = "Authenticator App"
        const val SMS_DISPLAY_NAME = "SMS"
        const val NO_SMS_SESSION_MESSAGE = "No SMS session available"
        const val LOCAL_PHONE_NUMBER = "5551234567"
        const val EXPECTED_FULL_PHONE_NUMBER = "+445551234567"

        /** Deliberately not the Robolectric default locale country, so the dial code is pinned. */
        val TEST_COUNTRY = CountryData(
            name = "United Kingdom",
            dialCode = "+44",
            countryCode = "GB",
            flagEmoji = "🇬🇧"
        )
    }
}
