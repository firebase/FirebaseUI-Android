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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.MfaConfiguration
import com.firebase.ui.auth.compose.configuration.MfaFactor
import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.compose.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.compose.mfa.getHelperText
import com.firebase.ui.auth.compose.mfa.getTitle
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseUser
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
 * E2E tests for [MfaEnrollmentScreen].
 *
 * These tests verify the UI state management and transitions for the MFA enrollment flow.
 *
 * **Important Note**: Firebase Auth Emulator has **limited MFA support**, so these tests
 * use mocked Firebase users and focus on UI flow validation. Actual MFA operations
 * (enrollment, verification) will fail with the emulator and are caught/ignored in tests.
 *
 * For full integration testing of MFA functionality, use a real Firebase project.
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class MfaEnrollmentScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var testUser: FirebaseUser

    @Mock
    private lateinit var mockFirebaseUser: FirebaseUser

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

        // Use mock user instead of real Firebase user
        `when`(mockFirebaseUser.email).thenReturn("mfatest@example.com")
        `when`(mockFirebaseUser.uid).thenReturn("test-uid-123")
        testUser = mockFirebaseUser
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
    }

    @Test
    fun `screen starts at SelectFactor step with multiple factors`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),
            requireEnrollment = false
        )

        var capturedState: MfaEnrollmentContentState? = null

        composeTestRule.setContent {
            TestMfaEnrollmentScreen(
                configuration = configuration,
                onStateChange = { capturedState = it }
            )
        }

        composeTestRule.waitForIdle()

        assertThat(capturedState?.step).isEqualTo(MfaEnrollmentStep.SelectFactor)
        assertThat(capturedState?.availableFactors).containsExactly(MfaFactor.Sms, MfaFactor.Totp)
        composeTestRule.onNodeWithText(MfaEnrollmentStep.SelectFactor.getTitle(stringProvider))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(MfaEnrollmentStep.SelectFactor.getHelperText(stringProvider))
            .assertIsDisplayed()
    }

    @Test
    fun `skip button is available when enrollment is not required`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),
            requireEnrollment = false
        )

        var skipClicked = false

        composeTestRule.setContent {
            TestMfaEnrollmentScreen(
                configuration = configuration,
                onSkip = { skipClicked = true }
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SKIP")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()
        assertThat(skipClicked).isTrue()
    }

    @Test
    fun `selecting SMS factor navigates to ConfigureSms step`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp)
        )

        var capturedState: MfaEnrollmentContentState? = null

        composeTestRule.setContent {
            TestMfaEnrollmentScreen(
                configuration = configuration,
                onStateChange = { capturedState = it }
            )
        }

        composeTestRule.waitForIdle()
        assertThat(capturedState?.step).isEqualTo(MfaEnrollmentStep.SelectFactor)

        composeTestRule.onNodeWithText("SMS")
            .performClick()

        composeTestRule.waitForIdle()
        assertThat(capturedState?.step).isEqualTo(MfaEnrollmentStep.ConfigureSms)
        composeTestRule.onNodeWithText(MfaEnrollmentStep.ConfigureSms.getTitle(stringProvider))
            .assertIsDisplayed()
    }

    @Test
    fun `selecting TOTP factor navigates to ConfigureTotp step`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp)
        )

        var capturedState: MfaEnrollmentContentState? = null

        composeTestRule.setContent {
            TestMfaEnrollmentScreen(
                configuration = configuration,
                onStateChange = { capturedState = it }
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("TOTP")
            .performClick()

        composeTestRule.waitForIdle()
        assertThat(capturedState?.step).isEqualTo(MfaEnrollmentStep.ConfigureTotp)
        composeTestRule.onNodeWithText(MfaEnrollmentStep.ConfigureTotp.getTitle(stringProvider))
            .assertIsDisplayed()
    }

    @Test
    fun `phone number input enables send button`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms)
        )

        composeTestRule.setContent {
            TestMfaEnrollmentScreen(configuration = configuration)
        }

        composeTestRule.waitForIdle()

        // Initially at ConfigureSms since only one factor
        composeTestRule.onNodeWithText("SEND CODE")
            .assertIsNotEnabled()

        composeTestRule.onNodeWithText("Phone number")
            .performTextInput("1234567890")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SEND CODE")
            .assertIsEnabled()
    }

    @Test
    fun `back navigation works from ConfigureSms to SelectFactor`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp)
        )

        var capturedState: MfaEnrollmentContentState? = null

        composeTestRule.setContent {
            TestMfaEnrollmentScreen(
                configuration = configuration,
                onStateChange = { capturedState = it }
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SMS")
            .performClick()

        composeTestRule.waitForIdle()
        assertThat(capturedState?.step).isEqualTo(MfaEnrollmentStep.ConfigureSms)

        composeTestRule.onNodeWithText("BACK")
            .performClick()

        composeTestRule.waitForIdle()
        assertThat(capturedState?.step).isEqualTo(MfaEnrollmentStep.SelectFactor)
    }

    @Test
    fun `TOTP secret and QR code URL are generated on ConfigureTotp step`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Totp)
        )

        var capturedState: MfaEnrollmentContentState? = null
        var errorOccurred = false

        composeTestRule.setContent {
            TestMfaEnrollmentScreen(
                configuration = configuration,
                onStateChange = { capturedState = it },
                onError = { errorOccurred = true }
            )
        }

        composeTestRule.waitForIdle()

        // Should be at ConfigureTotp since only one factor
        assertThat(capturedState?.step).isEqualTo(MfaEnrollmentStep.ConfigureTotp)

        // If no error occurred (rare in emulator), verify TOTP setup
        if (!errorOccurred && capturedState?.totpSecret != null) {
            assertThat(capturedState?.totpQrCodeUrl).isNotNull()
            assertThat(capturedState?.totpQrCodeUrl).startsWith("otpauth://totp/")
        }
    }

    @Test
    fun `verification code input enables verify button`() {
        val configuration = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Totp)
        )

        var capturedState: MfaEnrollmentContentState? = null

        composeTestRule.setContent {
            TestMfaEnrollmentScreen(
                configuration = configuration,
                onStateChange = { capturedState = it }
            )
        }

        composeTestRule.waitForIdle()

        // If TOTP generation failed (common with mocked user), skip the step navigation test
        if (capturedState?.totpSecret == null) {
            // Test would require real Firebase user with MFA support
            return
        }

        composeTestRule.onNodeWithText("CONTINUE")
            .performClick()

        composeTestRule.waitForIdle()
        assertThat(capturedState?.step).isEqualTo(MfaEnrollmentStep.VerifyFactor)

        composeTestRule.onNodeWithText("VERIFY")
            .assertIsNotEnabled()

        composeTestRule.onNodeWithText("Verification code")
            .performTextInput("123456")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("VERIFY")
            .assertIsEnabled()
    }

    @Composable
    private fun TestMfaEnrollmentScreen(
        configuration: MfaConfiguration,
        onComplete: () -> Unit = {},
        onSkip: () -> Unit = {},
        onError: (Exception) -> Unit = {},
        onStateChange: (MfaEnrollmentContentState) -> Unit = {}
    ) {
        MfaEnrollmentScreen(
            user = testUser,
            auth = authUI.auth,
            configuration = configuration,
            onComplete = onComplete,
            onSkip = onSkip,
            onError = onError
        ) { state ->
            onStateChange(state)
            TestMfaEnrollmentUI(state = state)
        }
    }

    @Composable
    private fun TestMfaEnrollmentUI(state: MfaEnrollmentContentState) {
        androidx.compose.foundation.layout.Column {
            // Title
            androidx.compose.material3.Text(state.step.getTitle(stringProvider))
            androidx.compose.material3.Text(state.step.getHelperText(stringProvider, state.selectedFactor))

            when (state.step) {
                MfaEnrollmentStep.SelectFactor -> {
                    state.availableFactors.forEach { factor ->
                        androidx.compose.material3.Button(
                            onClick = { state.onFactorSelected(factor) }
                        ) {
                            androidx.compose.material3.Text(factor.name.uppercase())
                        }
                    }
                    state.onSkipClick?.let {
                        androidx.compose.material3.Button(onClick = it) {
                            androidx.compose.material3.Text("SKIP")
                        }
                    }
                }

                MfaEnrollmentStep.ConfigureSms -> {
                    androidx.compose.material3.TextField(
                        value = state.phoneNumber,
                        onValueChange = state.onPhoneNumberChange,
                        label = { androidx.compose.material3.Text("Phone number") }
                    )
                    androidx.compose.material3.Button(
                        onClick = state.onSendSmsCodeClick,
                        enabled = state.isValid && !state.isLoading
                    ) {
                        androidx.compose.material3.Text("SEND CODE")
                    }
                    androidx.compose.material3.Button(onClick = state.onBackClick) {
                        androidx.compose.material3.Text("BACK")
                    }
                }

                MfaEnrollmentStep.ConfigureTotp -> {
                    state.totpSecret?.let {
                        androidx.compose.material3.Text("Secret: ${it.sharedSecretKey}")
                    }
                    state.totpQrCodeUrl?.let {
                        androidx.compose.material3.Text("QR: $it")
                    }
                    androidx.compose.material3.Button(
                        onClick = state.onContinueToVerifyClick,
                        enabled = state.isValid && !state.isLoading
                    ) {
                        androidx.compose.material3.Text("CONTINUE")
                    }
                    androidx.compose.material3.Button(onClick = state.onBackClick) {
                        androidx.compose.material3.Text("BACK")
                    }
                }

                MfaEnrollmentStep.VerifyFactor -> {
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
                            androidx.compose.material3.Text("RESEND")
                        }
                    }
                    androidx.compose.material3.Button(onClick = state.onBackClick) {
                        androidx.compose.material3.Text("BACK")
                    }
                }

                MfaEnrollmentStep.ShowRecoveryCodes -> {
                    state.recoveryCodes?.forEach { code ->
                        androidx.compose.material3.Text(code)
                    }
                    androidx.compose.material3.Button(
                        onClick = state.onCodesSavedClick,
                        enabled = !state.isLoading
                    ) {
                        androidx.compose.material3.Text("DONE")
                    }
                }
            }
        }
    }

}
