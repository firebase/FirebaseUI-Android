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

package com.firebase.ui.auth.compose.configuration

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [MfaConfiguration] covering default values, custom configurations,
 * and validation rules.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MfaConfigurationTest {

    // =============================================================================================
    // Default Configuration Tests
    // =============================================================================================

    @Test
    fun `MfaConfiguration with defaults uses correct values`() {
        val config = MfaConfiguration()

        assertThat(config.allowedFactors).containsExactly(MfaFactor.Sms, MfaFactor.Totp)
        assertThat(config.requireEnrollment).isFalse()
        assertThat(config.enableRecoveryCodes).isTrue()
    }

    @Test
    fun `MfaConfiguration default allowedFactors includes both Sms and Totp`() {
        val config = MfaConfiguration()

        assertThat(config.allowedFactors).hasSize(2)
        assertThat(config.allowedFactors).contains(MfaFactor.Sms)
        assertThat(config.allowedFactors).contains(MfaFactor.Totp)
    }

    // =============================================================================================
    // Custom Configuration Tests
    // =============================================================================================

    @Test
    fun `MfaConfiguration with custom allowedFactors only Sms`() {
        val config = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms)
        )

        assertThat(config.allowedFactors).containsExactly(MfaFactor.Sms)
        assertThat(config.allowedFactors).hasSize(1)
    }

    @Test
    fun `MfaConfiguration with custom allowedFactors only Totp`() {
        val config = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Totp)
        )

        assertThat(config.allowedFactors).containsExactly(MfaFactor.Totp)
        assertThat(config.allowedFactors).hasSize(1)
    }

    @Test
    fun `MfaConfiguration with requireEnrollment enabled`() {
        val config = MfaConfiguration(
            requireEnrollment = true
        )

        assertThat(config.requireEnrollment).isTrue()
    }

    @Test
    fun `MfaConfiguration with enableRecoveryCodes disabled`() {
        val config = MfaConfiguration(
            enableRecoveryCodes = false
        )

        assertThat(config.enableRecoveryCodes).isFalse()
    }

    @Test
    fun `MfaConfiguration with all custom values`() {
        val config = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms),
            requireEnrollment = true,
            enableRecoveryCodes = false
        )

        assertThat(config.allowedFactors).containsExactly(MfaFactor.Sms)
        assertThat(config.requireEnrollment).isTrue()
        assertThat(config.enableRecoveryCodes).isFalse()
    }

    // =============================================================================================
    // Validation Tests
    // =============================================================================================

    @Test
    fun `MfaConfiguration throws when allowedFactors is empty`() {
        try {
            MfaConfiguration(
                allowedFactors = emptyList()
            )
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(e.message).isEqualTo("At least one MFA factor must be allowed")
        }
    }

    @Test
    fun `MfaConfiguration allows both factors in any order`() {
        val config1 = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp)
        )
        val config2 = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Totp, MfaFactor.Sms)
        )

        assertThat(config1.allowedFactors).hasSize(2)
        assertThat(config2.allowedFactors).hasSize(2)
        assertThat(config1.allowedFactors).containsExactly(MfaFactor.Sms, MfaFactor.Totp)
        assertThat(config2.allowedFactors).containsExactly(MfaFactor.Totp, MfaFactor.Sms)
    }

    // =============================================================================================
    // MfaFactor Enum Tests
    // =============================================================================================

    @Test
    fun `MfaFactor enum has exactly two values`() {
        val factors = MfaFactor.entries

        assertThat(factors).hasSize(2)
        assertThat(factors).containsExactly(MfaFactor.Sms, MfaFactor.Totp)
    }

    @Test
    fun `MfaFactor Sms has correct name`() {
        assertThat(MfaFactor.Sms.name).isEqualTo("Sms")
    }

    @Test
    fun `MfaFactor Totp has correct name`() {
        assertThat(MfaFactor.Totp.name).isEqualTo("Totp")
    }
}
