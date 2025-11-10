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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.configuration.MfaFactor
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MfaEnrollmentStepTest {

    private lateinit var stringProvider: DefaultAuthUIStringProvider

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        stringProvider = DefaultAuthUIStringProvider(context)
    }

    @Test
    fun `enum has all expected values`() {
        val values = MfaEnrollmentStep.entries.toTypedArray()

        assertEquals(5, values.size)
        assertEquals(MfaEnrollmentStep.SelectFactor, values[0])
        assertEquals(MfaEnrollmentStep.ConfigureSms, values[1])
        assertEquals(MfaEnrollmentStep.ConfigureTotp, values[2])
        assertEquals(MfaEnrollmentStep.VerifyFactor, values[3])
        assertEquals(MfaEnrollmentStep.ShowRecoveryCodes, values[4])
    }

    @Test
    fun `valueOf works correctly`() {
        assertEquals(MfaEnrollmentStep.SelectFactor, MfaEnrollmentStep.valueOf("SelectFactor"))
        assertEquals(MfaEnrollmentStep.ConfigureSms, MfaEnrollmentStep.valueOf("ConfigureSms"))
        assertEquals(MfaEnrollmentStep.ConfigureTotp, MfaEnrollmentStep.valueOf("ConfigureTotp"))
        assertEquals(MfaEnrollmentStep.VerifyFactor, MfaEnrollmentStep.valueOf("VerifyFactor"))
        assertEquals(MfaEnrollmentStep.ShowRecoveryCodes, MfaEnrollmentStep.valueOf("ShowRecoveryCodes"))
    }

    @Test
    fun `enum ordinals are in expected order`() {
        assertEquals(0, MfaEnrollmentStep.SelectFactor.ordinal)
        assertEquals(1, MfaEnrollmentStep.ConfigureSms.ordinal)
        assertEquals(2, MfaEnrollmentStep.ConfigureTotp.ordinal)
        assertEquals(3, MfaEnrollmentStep.VerifyFactor.ordinal)
        assertEquals(4, MfaEnrollmentStep.ShowRecoveryCodes.ordinal)
    }

    @Test
    fun `getTitle returns correct values for each step`() {
        assertEquals("Choose Authentication Method", MfaEnrollmentStep.SelectFactor.getTitle(stringProvider))
        assertEquals("Set Up SMS Verification", MfaEnrollmentStep.ConfigureSms.getTitle(stringProvider))
        assertEquals("Set Up Authenticator App", MfaEnrollmentStep.ConfigureTotp.getTitle(stringProvider))
        assertEquals("Verify Your Code", MfaEnrollmentStep.VerifyFactor.getTitle(stringProvider))
        assertEquals("Save Your Recovery Codes", MfaEnrollmentStep.ShowRecoveryCodes.getTitle(stringProvider))
    }

    @Test
    fun `getHelperText returns correct text for SelectFactor`() {
        assertEquals(
            "Select a second authentication method to secure your account",
            MfaEnrollmentStep.SelectFactor.getHelperText(stringProvider)
        )
    }

    @Test
    fun `getHelperText returns correct text for ConfigureSms`() {
        assertEquals(
            "Enter your phone number to receive verification codes",
            MfaEnrollmentStep.ConfigureSms.getHelperText(stringProvider)
        )
    }

    @Test
    fun `getHelperText returns correct text for ConfigureTotp`() {
        assertEquals(
            "Scan the QR code with your authenticator app",
            MfaEnrollmentStep.ConfigureTotp.getHelperText(stringProvider)
        )
    }

    @Test
    fun `getHelperText returns correct text for VerifyFactor with SMS`() {
        assertEquals(
            "Enter the code sent to your phone",
            MfaEnrollmentStep.VerifyFactor.getHelperText(stringProvider, MfaFactor.Sms)
        )
    }

    @Test
    fun `getHelperText returns correct text for VerifyFactor with TOTP`() {
        assertEquals(
            "Enter the code from your authenticator app",
            MfaEnrollmentStep.VerifyFactor.getHelperText(stringProvider, MfaFactor.Totp)
        )
    }

    @Test
    fun `getHelperText returns generic text for VerifyFactor with no factor`() {
        assertEquals(
            "Enter your verification code",
            MfaEnrollmentStep.VerifyFactor.getHelperText(stringProvider, null)
        )
    }

    @Test
    fun `getHelperText returns generic text for VerifyFactor without parameter`() {
        assertEquals(
            "Enter your verification code",
            MfaEnrollmentStep.VerifyFactor.getHelperText(stringProvider)
        )
    }

    @Test
    fun `getHelperText returns correct text for ShowRecoveryCodes`() {
        val helperText = MfaEnrollmentStep.ShowRecoveryCodes.getHelperText(stringProvider)
        assertTrue(helperText.contains("Store these codes in a safe place"))
    }

    @Test
    fun `getHelperText ignores factor parameter for non-VerifyFactor steps`() {
        // These should return the same result regardless of the factor parameter
        assertEquals(
            MfaEnrollmentStep.SelectFactor.getHelperText(stringProvider),
            MfaEnrollmentStep.SelectFactor.getHelperText(stringProvider, MfaFactor.Sms)
        )
        assertEquals(
            MfaEnrollmentStep.ConfigureSms.getHelperText(stringProvider),
            MfaEnrollmentStep.ConfigureSms.getHelperText(stringProvider, MfaFactor.Totp)
        )
        assertEquals(
            MfaEnrollmentStep.ConfigureTotp.getHelperText(stringProvider),
            MfaEnrollmentStep.ConfigureTotp.getHelperText(stringProvider, MfaFactor.Sms)
        )
    }
}
