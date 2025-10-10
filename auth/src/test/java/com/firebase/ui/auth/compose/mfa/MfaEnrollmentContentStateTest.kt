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

package com.firebase.ui.auth.compose.mfa

import com.firebase.ui.auth.compose.configuration.MfaFactor
import com.firebase.ui.auth.compose.data.CountryData
import com.google.firebase.auth.TotpSecret as FirebaseTotpSecret
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MfaEnrollmentContentStateTest {

    @Test
    fun `isValid returns true for SelectFactor with available factors`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.SelectFactor,
            availableFactors = listOf(MfaFactor.Sms, MfaFactor.Totp)
        )

        // When & Then
        assertTrue(state.isValid)
    }

    @Test
    fun `isValid returns false for SelectFactor with no available factors`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.SelectFactor,
            availableFactors = emptyList()
        )

        // When & Then
        assertFalse(state.isValid)
    }

    @Test
    fun `isValid returns true for ConfigureSms with valid phone number`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.ConfigureSms,
            phoneNumber = "1234567890"
        )

        // When & Then
        assertTrue(state.isValid)
    }

    @Test
    fun `isValid returns false for ConfigureSms with empty phone number`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.ConfigureSms,
            phoneNumber = ""
        )

        // When & Then
        assertFalse(state.isValid)
    }

    @Test
    fun `isValid returns true for ConfigureTotp with secret and QR URL`() {
        // Given
        val mockFirebaseTotpSecret = mock(FirebaseTotpSecret::class.java)
        val mockTotpSecret = TotpSecret.from(mockFirebaseTotpSecret)
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.ConfigureTotp,
            totpSecret = mockTotpSecret,
            totpQrCodeUrl = "otpauth://totp/test"
        )

        // When & Then
        assertTrue(state.isValid)
    }

    @Test
    fun `isValid returns false for ConfigureTotp without secret`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.ConfigureTotp,
            totpSecret = null,
            totpQrCodeUrl = "otpauth://totp/test"
        )

        // When & Then
        assertFalse(state.isValid)
    }

    @Test
    fun `isValid returns true for VerifyFactor with 6-digit code`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.VerifyFactor,
            verificationCode = "123456"
        )

        // When & Then
        assertTrue(state.isValid)
    }

    @Test
    fun `isValid returns false for VerifyFactor with invalid code length`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.VerifyFactor,
            verificationCode = "12345"
        )

        // When & Then
        assertFalse(state.isValid)
    }

    @Test
    fun `isValid returns true for ShowRecoveryCodes with codes`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.ShowRecoveryCodes,
            recoveryCodes = listOf("code1", "code2", "code3")
        )

        // When & Then
        assertTrue(state.isValid)
    }

    @Test
    fun `isValid returns false for ShowRecoveryCodes without codes`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.ShowRecoveryCodes,
            recoveryCodes = null
        )

        // When & Then
        assertFalse(state.isValid)
    }

    @Test
    fun `hasError returns true when error is present`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.SelectFactor,
            error = "Something went wrong"
        )

        // When & Then
        assertTrue(state.hasError)
    }

    @Test
    fun `hasError returns false when error is null`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.SelectFactor,
            error = null
        )

        // When & Then
        assertFalse(state.hasError)
    }

    @Test
    fun `hasError returns false when error is blank`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.SelectFactor,
            error = "   "
        )

        // When & Then
        assertFalse(state.hasError)
    }

    @Test
    fun `canSkip returns true when on SelectFactor step with skip callback`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.SelectFactor,
            onSkipClick = {}
        )

        // When & Then
        assertTrue(state.canSkip)
    }

    @Test
    fun `canSkip returns false when skip callback is null`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.SelectFactor,
            onSkipClick = null
        )

        // When & Then
        assertFalse(state.canSkip)
    }

    @Test
    fun `canSkip returns false when not on SelectFactor step`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.ConfigureTotp,
            onSkipClick = {}
        )

        // When & Then
        assertFalse(state.canSkip)
    }

    @Test
    fun `canGoBack returns false on SelectFactor step`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.SelectFactor
        )

        // When & Then
        assertFalse(state.canGoBack)
    }

    @Test
    fun `canGoBack returns true on other steps`() {
        // Given
        val steps = listOf(
            MfaEnrollmentStep.ConfigureSms,
            MfaEnrollmentStep.ConfigureTotp,
            MfaEnrollmentStep.VerifyFactor,
            MfaEnrollmentStep.ShowRecoveryCodes
        )

        // When & Then
        steps.forEach { step ->
            val state = MfaEnrollmentContentState(step = step)
            assertTrue("Expected canGoBack to be true for step $step", state.canGoBack)
        }
    }

    @Test
    fun `getStepTitle returns correct titles for each step`() {
        // Given & When & Then
        assertEquals(
            "Choose Authentication Method",
            MfaEnrollmentContentState(step = MfaEnrollmentStep.SelectFactor).getStepTitle()
        )
        assertEquals(
            "Set Up SMS Verification",
            MfaEnrollmentContentState(step = MfaEnrollmentStep.ConfigureSms).getStepTitle()
        )
        assertEquals(
            "Set Up Authenticator App",
            MfaEnrollmentContentState(step = MfaEnrollmentStep.ConfigureTotp).getStepTitle()
        )
        assertEquals(
            "Verify Your Code",
            MfaEnrollmentContentState(step = MfaEnrollmentStep.VerifyFactor).getStepTitle()
        )
        assertEquals(
            "Save Your Recovery Codes",
            MfaEnrollmentContentState(step = MfaEnrollmentStep.ShowRecoveryCodes).getStepTitle()
        )
    }

    @Test
    fun `getStepHelperText returns correct text for SelectFactor`() {
        // Given
        val state = MfaEnrollmentContentState(step = MfaEnrollmentStep.SelectFactor)

        // When & Then
        assertEquals(
            "Select a second authentication method to secure your account",
            state.getStepHelperText()
        )
    }

    @Test
    fun `getStepHelperText returns correct text for ConfigureSms`() {
        // Given
        val state = MfaEnrollmentContentState(step = MfaEnrollmentStep.ConfigureSms)

        // When & Then
        assertEquals(
            "Enter your phone number to receive verification codes",
            state.getStepHelperText()
        )
    }

    @Test
    fun `getStepHelperText returns correct text for ConfigureTotp`() {
        // Given
        val state = MfaEnrollmentContentState(step = MfaEnrollmentStep.ConfigureTotp)

        // When & Then
        assertEquals(
            "Scan the QR code with your authenticator app",
            state.getStepHelperText()
        )
    }

    @Test
    fun `getStepHelperText returns correct text for VerifyFactor with SMS`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.VerifyFactor,
            selectedFactor = MfaFactor.Sms
        )

        // When & Then
        assertEquals(
            "Enter the code sent to your phone",
            state.getStepHelperText()
        )
    }

    @Test
    fun `getStepHelperText returns correct text for VerifyFactor with TOTP`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.VerifyFactor,
            selectedFactor = MfaFactor.Totp
        )

        // When & Then
        assertEquals(
            "Enter the code from your authenticator app",
            state.getStepHelperText()
        )
    }

    @Test
    fun `getStepHelperText returns generic text for VerifyFactor with no factor`() {
        // Given
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.VerifyFactor,
            selectedFactor = null
        )

        // When & Then
        assertEquals(
            "Enter your verification code",
            state.getStepHelperText()
        )
    }

    @Test
    fun `getStepHelperText returns correct text for ShowRecoveryCodes`() {
        // Given
        val state = MfaEnrollmentContentState(step = MfaEnrollmentStep.ShowRecoveryCodes)

        // When & Then
        assertTrue(state.getStepHelperText().contains("Store these codes in a safe place"))
    }

    @Test
    fun `data class properties are accessible`() {
        // Given
        val mockFirebaseTotpSecret = mock(FirebaseTotpSecret::class.java)
        val mockTotpSecret = TotpSecret.from(mockFirebaseTotpSecret)
        val mockCountry = CountryData("United States", "+1", "us", "🇺🇸")
        val state = MfaEnrollmentContentState(
            step = MfaEnrollmentStep.ConfigureTotp,
            isLoading = true,
            error = "Error message",
            phoneNumber = "1234567890",
            selectedCountry = mockCountry,
            totpSecret = mockTotpSecret,
            totpQrCodeUrl = "otpauth://totp/test",
            verificationCode = "123456",
            selectedFactor = MfaFactor.Totp,
            recoveryCodes = listOf("code1", "code2"),
            availableFactors = listOf(MfaFactor.Sms, MfaFactor.Totp)
        )

        // When & Then
        assertEquals(MfaEnrollmentStep.ConfigureTotp, state.step)
        assertTrue(state.isLoading)
        assertEquals("Error message", state.error)
        assertEquals("1234567890", state.phoneNumber)
        assertEquals(mockCountry, state.selectedCountry)
        assertEquals(mockTotpSecret, state.totpSecret)
        assertEquals("otpauth://totp/test", state.totpQrCodeUrl)
        assertEquals("123456", state.verificationCode)
        assertEquals(MfaFactor.Totp, state.selectedFactor)
        assertEquals(listOf("code1", "code2"), state.recoveryCodes)
        assertEquals(listOf(MfaFactor.Sms, MfaFactor.Totp), state.availableFactors)
    }
}
