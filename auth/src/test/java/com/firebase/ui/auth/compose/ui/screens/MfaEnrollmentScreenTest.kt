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
import com.firebase.ui.auth.compose.configuration.MfaConfiguration
import com.firebase.ui.auth.compose.configuration.MfaFactor
import com.firebase.ui.auth.compose.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.compose.mfa.MfaEnrollmentStep
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

    private lateinit var capturedState: MfaEnrollmentContentState

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        FirebaseApp.initializeApp(RuntimeEnvironment.getApplication())
        `when`(mockAuth.app).thenReturn(mockFirebaseApp)
        `when`(mockFirebaseApp.name).thenReturn("TestApp")
        `when`(mockUser.email).thenReturn("test@example.com")
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
}
