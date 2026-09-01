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
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.mfa.SmsEnrollmentHandler
import com.firebase.ui.auth.mfa.TotpEnrollmentHandler
import com.firebase.ui.auth.mfa.TotpSecret
import com.firebase.ui.auth.ui.screens.mfa.MfaEnrollmentScreen
import com.firebase.ui.auth.ui.screens.mfa.MfaEnrollmentScreenInternal
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorInfo
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
    }
}
