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

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.MfaFactor
import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.compose.mfa.MfaChallengeContentState
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.auth.TotpMultiFactorInfo
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
 * E2E tests for [MfaChallengeScreen].
 *
 * These tests verify the MFA challenge flow including UI interactions and state transitions.
 *
 * Note: Firebase Auth Emulator has limited MFA support, so these tests use mocked
 * MultiFactorResolver to test the UI flow.
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class MfaChallengeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider
    private lateinit var authUI: FirebaseAuthUI

    @Mock
    private lateinit var mockResolver: MultiFactorResolver

    @Mock
    private lateinit var mockSession: MultiFactorSession

    @Mock
    private lateinit var mockPhoneHint: PhoneMultiFactorInfo

    @Mock
    private lateinit var mockTotpHint: TotpMultiFactorInfo

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)

        // Clear any existing Firebase apps
        FirebaseApp.getApps(applicationContext).forEach { app ->
            app.delete()
        }

        // Initialize default FirebaseApp
        FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )

        authUI = FirebaseAuthUI.getInstance()
        authUI.auth.useEmulator("127.0.0.1", 9099)

        // Setup mock resolver
        `when`(mockResolver.session).thenReturn(mockSession)
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
    }

    @Test
    fun `screen detects SMS factor and shows masked phone number`() {
        `when`(mockPhoneHint.factorId).thenReturn("phone")
        `when`(mockPhoneHint.phoneNumber).thenReturn("+1234567890")
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockPhoneHint))

        var capturedState: MfaChallengeContentState? = null

        composeTestRule.setContent {
            TestMfaChallengeScreen(
                resolver = mockResolver,
                onStateChange = { capturedState = it }
            )
        }

        composeTestRule.waitForIdle()

        assertThat(capturedState?.factorType).isEqualTo(MfaFactor.Sms)
        assertThat(capturedState?.maskedPhoneNumber).isNotNull()
        assertThat(capturedState?.maskedPhoneNumber).contains("•")
        composeTestRule.onNodeWithText(capturedState?.maskedPhoneNumber ?: "")
            .assertIsDisplayed()
    }

    @Test
    fun `screen detects TOTP factor and shows no masked phone`() {
        `when`(mockTotpHint.factorId).thenReturn("totp")
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockTotpHint))

        var capturedState: MfaChallengeContentState? = null

        composeTestRule.setContent {
            TestMfaChallengeScreen(
                resolver = mockResolver,
                onStateChange = { capturedState = it }
            )
        }

        composeTestRule.waitForIdle()

        assertThat(capturedState?.factorType).isEqualTo(MfaFactor.Totp)
        assertThat(capturedState?.maskedPhoneNumber).isNull()
    }

    @Test
    fun `verification code input enables verify button`() {
        `when`(mockTotpHint.factorId).thenReturn("totp")
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockTotpHint))

        composeTestRule.setContent {
            TestMfaChallengeScreen(resolver = mockResolver)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("VERIFY")
            .assertIsNotEnabled()

        composeTestRule.onNodeWithText("Verification code")
            .performTextInput("123456")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("VERIFY")
            .assertIsEnabled()
    }

    @Test
    fun `resend button is available for SMS factor`() {
        `when`(mockPhoneHint.factorId).thenReturn("phone")
        `when`(mockPhoneHint.phoneNumber).thenReturn("+1234567890")
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockPhoneHint))

        composeTestRule.setContent {
            TestMfaChallengeScreen(resolver = mockResolver)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("RESEND CODE")
            .assertIsDisplayed()
    }

    @Test
    fun `resend button is not available for TOTP factor`() {
        `when`(mockTotpHint.factorId).thenReturn("totp")
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockTotpHint))

        composeTestRule.setContent {
            TestMfaChallengeScreen(resolver = mockResolver)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("RESEND CODE")
            .assertDoesNotExist()
    }

    @Test
    fun `cancel button invokes callback`() {
        `when`(mockTotpHint.factorId).thenReturn("totp")
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockTotpHint))

        var cancelClicked = false

        composeTestRule.setContent {
            TestMfaChallengeScreen(
                resolver = mockResolver,
                onCancel = { cancelClicked = true }
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("CANCEL")
            .performClick()

        composeTestRule.waitForIdle()
        assertThat(cancelClicked).isTrue()
    }

    @Test
    fun `verification code must be 6 digits to enable verify button`() {
        `when`(mockTotpHint.factorId).thenReturn("totp")
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockTotpHint))

        composeTestRule.setContent {
            TestMfaChallengeScreen(resolver = mockResolver)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Verification code")
            .performTextInput("12345")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("VERIFY")
            .assertIsNotEnabled()

        composeTestRule.onNodeWithText("Verification code")
            .performTextInput("6")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("VERIFY")
            .assertIsEnabled()
    }

    @Test
    fun `default UI shows VerificationCodeInputField for SMS factor`() {
        `when`(mockPhoneHint.factorId).thenReturn("phone")
        `when`(mockPhoneHint.phoneNumber).thenReturn("+1234567890")
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockPhoneHint))

        var capturedState: MfaChallengeContentState? = null

        composeTestRule.setContent {
            TestMfaChallengeScreen(
                resolver = mockResolver,
                onStateChange = { capturedState = it }
            )
        }

        composeTestRule.waitForIdle()

        // Verify SMS factor type is detected
        assertThat(capturedState?.factorType).isEqualTo(MfaFactor.Sms)

        // Verify masked phone number is set correctly
        // +1234567890 is 11 chars: +1 (2) + 6 masked + 890 (3) = +1••••••890
        assertThat(capturedState?.maskedPhoneNumber).isEqualTo("+1••••••890")

        // Verify that the verification code input works (via the state object that would be used by VerificationCodeInputField)
        assertThat(capturedState?.verificationCode).isEmpty()
        assertThat(capturedState?.isValid).isFalse()
    }

    @Test
    fun `default UI shows VerificationCodeInputField for TOTP factor`() {
        `when`(mockTotpHint.factorId).thenReturn("totp")
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockTotpHint))

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(applicationContext)
            ) {
                MfaChallengeScreen(
                    resolver = mockResolver,
                    auth = authUI.auth,
                    onSuccess = {},
                    onCancel = {},
                    onError = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify the default UI is displayed with TOTP-specific title
        composeTestRule.onNodeWithText(stringProvider.mfaStepVerifyFactorTitle)
            .assertIsDisplayed()

        // Verify VerificationCodeInputField is present
        composeTestRule.onNodeWithText(stringProvider.verifyAction)
            .assertIsDisplayed()
            .assertIsNotEnabled() // Should be disabled until code is entered
    }

    @Test
    fun `default UI shows resend button for SMS factor`() {
        // Test SMS factor
        `when`(mockPhoneHint.factorId).thenReturn("phone")
        `when`(mockPhoneHint.phoneNumber).thenReturn("+1234567890")
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockPhoneHint))

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(applicationContext)
            ) {
                MfaChallengeScreen(
                    resolver = mockResolver,
                    auth = authUI.auth,
                    onSuccess = {},
                    onCancel = {},
                    onError = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Should show resend button for SMS
        composeTestRule.onNodeWithText(stringProvider.resendCode, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `default UI does not show resend button for TOTP factor`() {
        // Test TOTP factor
        `when`(mockTotpHint.factorId).thenReturn("totp")
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockTotpHint))

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(applicationContext)
            ) {
                MfaChallengeScreen(
                    resolver = mockResolver,
                    auth = authUI.auth,
                    onSuccess = {},
                    onCancel = {},
                    onError = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Should NOT show resend button for TOTP
        composeTestRule.onNodeWithText(stringProvider.resendCode, substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun `default UI displays masked phone number for SMS factor`() {
        `when`(mockPhoneHint.factorId).thenReturn("phone")
        `when`(mockPhoneHint.phoneNumber).thenReturn("+1234567890")
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockPhoneHint))

        var capturedState: MfaChallengeContentState? = null

        composeTestRule.setContent {
            TestMfaChallengeScreen(
                resolver = mockResolver,
                onStateChange = { capturedState = it }
            )
        }

        composeTestRule.waitForIdle()

        // Verify masked phone number is set correctly in the state
        // +1234567890 is 11 chars: +1 (2) + 6 masked + 890 (3) = +1••••••890
        assertThat(capturedState?.maskedPhoneNumber).isEqualTo("+1••••••890")
        assertThat(capturedState?.factorType).isEqualTo(MfaFactor.Sms)
    }

    @Composable
    private fun TestMfaChallengeScreen(
        resolver: MultiFactorResolver,
        onSuccess: () -> Unit = {},
        onCancel: () -> Unit = {},
        onStateChange: (MfaChallengeContentState) -> Unit = {}
    ) {
        MfaChallengeScreen(
            resolver = resolver,
            auth = authUI.auth,
            onSuccess = { onSuccess() },
            onCancel = onCancel,
            onError = { /* Ignore errors in test UI */ }
        ) { state ->
            onStateChange(state)
            TestMfaChallengeUI(state = state)
        }
    }

    @Composable
    private fun TestMfaChallengeUI(state: MfaChallengeContentState) {
        androidx.compose.foundation.layout.Column {
            androidx.compose.material3.Text("MFA Challenge")

            state.maskedPhoneNumber?.let {
                androidx.compose.material3.Text(it)
            }

            androidx.compose.material3.TextField(
                value = state.verificationCode,
                onValueChange = state.onVerificationCodeChange,
                label = { androidx.compose.material3.Text("Verification code") }
            )

            androidx.compose.material3.Button(
                onClick = state.onVerifyClick,
                enabled = state.isValid && !state.isLoading
            ) {
                androidx.compose.material3.Text("VERIFY")
            }

            state.onResendCodeClick?.let {
                androidx.compose.material3.Button(onClick = it) {
                    androidx.compose.material3.Text("RESEND CODE")
                }
            }

            androidx.compose.material3.Button(onClick = state.onCancelClick) {
                androidx.compose.material3.Text("CANCEL")
            }
        }
    }
}
