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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MfaChallengeContentStateTest {

    @Test
    fun `state holds all properties correctly for SMS`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            maskedPhoneNumber = "+1••••••890",
            isLoading = false,
            error = null,
            verificationCode = "123456"
        )

        assertEquals(MfaFactor.Sms, state.factorType)
        assertEquals("+1••••••890", state.maskedPhoneNumber)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("123456", state.verificationCode)
    }

    @Test
    fun `state holds all properties correctly for TOTP`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Totp,
            maskedPhoneNumber = null,
            isLoading = true,
            error = "Test error",
            verificationCode = "654321"
        )

        assertEquals(MfaFactor.Totp, state.factorType)
        assertNull(state.maskedPhoneNumber)
        assertTrue(state.isLoading)
        assertEquals("Test error", state.error)
        assertEquals("654321", state.verificationCode)
    }

    @Test
    fun `isValid returns true for valid 6-digit code`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            verificationCode = "123456"
        )

        assertTrue(state.isValid)
    }

    @Test
    fun `isValid returns false for code that is too short`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            verificationCode = "12345"
        )

        assertFalse(state.isValid)
    }

    @Test
    fun `isValid returns false for code that is too long`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            verificationCode = "1234567"
        )

        assertFalse(state.isValid)
    }

    @Test
    fun `isValid returns false for code with non-digits`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            verificationCode = "12345a"
        )

        assertFalse(state.isValid)
    }

    @Test
    fun `isValid returns false for empty code`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            verificationCode = ""
        )

        assertFalse(state.isValid)
    }

    @Test
    fun `hasError returns true when error is present`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            error = "Invalid code"
        )

        assertTrue(state.hasError)
    }

    @Test
    fun `hasError returns false when error is null`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            error = null
        )

        assertFalse(state.hasError)
    }

    @Test
    fun `hasError returns false when error is blank`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            error = "   "
        )

        assertFalse(state.hasError)
    }

    @Test
    fun `canResend returns true for SMS when callback is provided`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            onResendCodeClick = {}
        )

        assertTrue(state.canResend)
    }

    @Test
    fun `canResend returns false for SMS when callback is null`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            onResendCodeClick = null
        )

        assertFalse(state.canResend)
    }

    @Test
    fun `canResend returns false for TOTP even with callback`() {
        val state = MfaChallengeContentState(
            factorType = MfaFactor.Totp,
            onResendCodeClick = {}
        )

        assertFalse(state.canResend)
    }

    @Test
    fun `callbacks are invoked correctly`() {
        var verificationCodeChanged = false
        var verifyClicked = false
        var resendClicked = false
        var cancelClicked = false

        val state = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            onVerificationCodeChange = { verificationCodeChanged = true },
            onVerifyClick = { verifyClicked = true },
            onResendCodeClick = { resendClicked = true },
            onCancelClick = { cancelClicked = true }
        )

        state.onVerificationCodeChange("123456")
        assertTrue(verificationCodeChanged)

        state.onVerifyClick()
        assertTrue(verifyClicked)

        state.onResendCodeClick?.invoke()
        assertTrue(resendClicked)

        state.onCancelClick()
        assertTrue(cancelClicked)
    }

    @Test
    fun `state equality works correctly`() {
        val state1 = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            maskedPhoneNumber = "+1••••••890",
            verificationCode = "123456"
        )

        val state2 = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            maskedPhoneNumber = "+1••••••890",
            verificationCode = "123456"
        )

        val state3 = MfaChallengeContentState(
            factorType = MfaFactor.Totp,
            maskedPhoneNumber = null,
            verificationCode = "123456"
        )

        assertEquals(state1, state2)
        assertFalse(state1 == state3)
    }

    @Test
    fun `state copy works correctly`() {
        val original = MfaChallengeContentState(
            factorType = MfaFactor.Sms,
            verificationCode = "123456",
            isLoading = false
        )

        val copied = original.copy(isLoading = true)

        assertTrue(copied.isLoading)
        assertEquals("123456", copied.verificationCode)
        assertFalse(original.isLoading)
    }
}
