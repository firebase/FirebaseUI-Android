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

package com.firebase.ui.auth.compose.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.firebase.ui.auth.compose.configuration.MfaFactor
import com.firebase.ui.auth.compose.mfa.MfaChallengeContentState
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.auth.TotpMultiFactorInfo
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for [MfaChallengeScreen].
 *
 * These tests focus on the state management logic and callbacks provided
 * through the content slot for MFA challenge flow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class MfaChallengeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockAuth: FirebaseAuth

    @Mock
    private lateinit var mockResolver: MultiFactorResolver

    @Mock
    private lateinit var mockPhoneMultiFactorInfo: PhoneMultiFactorInfo

    @Mock
    private lateinit var mockTotpMultiFactorInfo: TotpMultiFactorInfo

    @Mock
    private lateinit var mockFirebaseApp: FirebaseApp

    private lateinit var capturedState: MfaChallengeContentState

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        FirebaseApp.initializeApp(RuntimeEnvironment.getApplication())
        `when`(mockAuth.app).thenReturn(mockFirebaseApp)
    }

    @Test
    fun `screen detects SMS factor type from phone hint`() {
        `when`(mockResolver.hints).thenReturn(listOf(mockPhoneMultiFactorInfo))
        `when`(mockPhoneMultiFactorInfo.factorId).thenReturn("phone")
        `when`(mockPhoneMultiFactorInfo.phoneNumber).thenReturn("+1234567890")

        composeTestRule.setContent {
            MfaChallengeScreen(
                resolver = mockResolver,
                auth = mockAuth,
                onSuccess = {},
                onCancel = {}
            ) { state ->
                capturedState = state
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(MfaFactor.Sms, capturedState.factorType)
    }

    @Test
    fun `screen detects TOTP factor type from totp hint`() {
        `when`(mockResolver.hints).thenReturn(listOf(mockTotpMultiFactorInfo))
        `when`(mockTotpMultiFactorInfo.factorId).thenReturn("totp")

        composeTestRule.setContent {
            MfaChallengeScreen(
                resolver = mockResolver,
                auth = mockAuth,
                onSuccess = {},
                onCancel = {}
            ) { state ->
                capturedState = state
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(MfaFactor.Totp, capturedState.factorType)
    }

    @Test
    fun `screen shows masked phone number for SMS factor`() {
        `when`(mockResolver.hints).thenReturn(listOf(mockPhoneMultiFactorInfo))
        `when`(mockPhoneMultiFactorInfo.factorId).thenReturn("phone")
        `when`(mockPhoneMultiFactorInfo.phoneNumber).thenReturn("+1234567890")

        composeTestRule.setContent {
            MfaChallengeScreen(
                resolver = mockResolver,
                auth = mockAuth,
                onSuccess = {},
                onCancel = {}
            ) { state ->
                capturedState = state
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(capturedState.maskedPhoneNumber)
        assertTrue(capturedState.maskedPhoneNumber!!.contains("•"))
    }

    @Test
    fun `screen shows null masked phone for TOTP factor`() {
        `when`(mockResolver.hints).thenReturn(listOf(mockTotpMultiFactorInfo))
        `when`(mockTotpMultiFactorInfo.factorId).thenReturn("totp")

        composeTestRule.setContent {
            MfaChallengeScreen(
                resolver = mockResolver,
                auth = mockAuth,
                onSuccess = {},
                onCancel = {}
            ) { state ->
                capturedState = state
            }
        }

        composeTestRule.waitForIdle()
        assertNull(capturedState.maskedPhoneNumber)
    }

    @Test
    fun `verification code change updates state`() {
        `when`(mockResolver.hints).thenReturn(listOf(mockTotpMultiFactorInfo))
        `when`(mockTotpMultiFactorInfo.factorId).thenReturn("totp")

        var currentState by mutableStateOf<MfaChallengeContentState?>(null)

        composeTestRule.setContent {
            MfaChallengeScreen(
                resolver = mockResolver,
                auth = mockAuth,
                onSuccess = {},
                onCancel = {}
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()
        assertEquals("", currentState?.verificationCode)

        composeTestRule.runOnUiThread {
            currentState?.onVerificationCodeChange?.invoke("123456")
        }

        composeTestRule.waitForIdle()
        assertEquals("123456", currentState?.verificationCode)
    }

    @Test
    fun `resend callback is available for SMS factor`() {
        `when`(mockResolver.hints).thenReturn(listOf(mockPhoneMultiFactorInfo))
        `when`(mockPhoneMultiFactorInfo.factorId).thenReturn("phone")
        `when`(mockPhoneMultiFactorInfo.phoneNumber).thenReturn("+1234567890")

        composeTestRule.setContent {
            MfaChallengeScreen(
                resolver = mockResolver,
                auth = mockAuth,
                onSuccess = {},
                onCancel = {}
            ) { state ->
                capturedState = state
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(capturedState.onResendCodeClick)
        assertTrue(capturedState.canResend)
    }

    @Test
    fun `resend callback is null for TOTP factor`() {
        `when`(mockResolver.hints).thenReturn(listOf(mockTotpMultiFactorInfo))
        `when`(mockTotpMultiFactorInfo.factorId).thenReturn("totp")

        composeTestRule.setContent {
            MfaChallengeScreen(
                resolver = mockResolver,
                auth = mockAuth,
                onSuccess = {},
                onCancel = {}
            ) { state ->
                capturedState = state
            }
        }

        composeTestRule.waitForIdle()
        assertNull(capturedState.onResendCodeClick)
        assertFalse(capturedState.canResend)
    }

    @Test
    fun `state validation works correctly`() {
        `when`(mockResolver.hints).thenReturn(listOf(mockTotpMultiFactorInfo))
        `when`(mockTotpMultiFactorInfo.factorId).thenReturn("totp")

        var currentState by mutableStateOf<MfaChallengeContentState?>(null)

        composeTestRule.setContent {
            MfaChallengeScreen(
                resolver = mockResolver,
                auth = mockAuth,
                onSuccess = {},
                onCancel = {}
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()

        // Invalid when code is empty
        assertFalse(currentState?.isValid ?: true)

        composeTestRule.runOnUiThread {
            currentState?.onVerificationCodeChange?.invoke("12345")
        }

        composeTestRule.waitForIdle()

        // Invalid when code is too short
        assertFalse(currentState?.isValid ?: true)

        composeTestRule.runOnUiThread {
            currentState?.onVerificationCodeChange?.invoke("123456")
        }

        composeTestRule.waitForIdle()

        // Valid when code is 6 digits
        assertTrue(currentState?.isValid ?: false)
    }

    @Test
    fun `cancel callback is invoked correctly`() {
        `when`(mockResolver.hints).thenReturn(listOf(mockTotpMultiFactorInfo))
        `when`(mockTotpMultiFactorInfo.factorId).thenReturn("totp")

        var cancelCalled = false

        composeTestRule.setContent {
            MfaChallengeScreen(
                resolver = mockResolver,
                auth = mockAuth,
                onSuccess = {},
                onCancel = { cancelCalled = true }
            ) { state ->
                capturedState = state
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            capturedState.onCancelClick()
        }

        composeTestRule.waitForIdle()
        assertTrue(cancelCalled)
    }

    @Test
    fun `error clears when verification code changes`() {
        `when`(mockResolver.hints).thenReturn(listOf(mockTotpMultiFactorInfo))
        `when`(mockTotpMultiFactorInfo.factorId).thenReturn("totp")

        var currentState by mutableStateOf<MfaChallengeContentState?>(null)

        composeTestRule.setContent {
            MfaChallengeScreen(
                resolver = mockResolver,
                auth = mockAuth,
                onSuccess = {},
                onCancel = {}
            ) { state ->
                currentState = state
            }
        }

        composeTestRule.waitForIdle()

        // Initially no error
        assertNull(currentState?.error)
        assertFalse(currentState?.hasError ?: true)

        // Change verification code
        composeTestRule.runOnUiThread {
            currentState?.onVerificationCodeChange?.invoke("123456")
        }

        composeTestRule.waitForIdle()

        // Error should still be null
        assertNull(currentState?.error)
        assertFalse(currentState?.hasError ?: true)
    }
}
