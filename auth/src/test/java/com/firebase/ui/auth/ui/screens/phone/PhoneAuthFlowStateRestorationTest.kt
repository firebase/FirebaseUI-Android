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

package com.firebase.ui.auth.ui.screens.phone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.util.CountryUtils
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What an Activity recreation does to the country a user picked.
 *
 * The number is `rememberSaveable` and comes back; the country used to be a plain `remember` and
 * did not, so it was re-resolved from the configuration. That pairing is the bug: the dial code is
 * what prefixes the restored number, so a user who picked one country and rotated had their number
 * submitted under a dial code they never chose. The country is now saved by
 * [com.firebase.ui.auth.data.CountryDataSaver], the same saver the MFA flow state uses — see
 * [com.firebase.ui.auth.ui.screens.mfa.MfaEnrollmentFlowStateRestorationTest] for the equivalent
 * there.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class PhoneAuthFlowStateRestorationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: android.content.Context
    private lateinit var authUI: FirebaseAuthUI

    private var flowState: PhoneAuthFlowState? = null

    @Before
    fun setUp() {
        FirebaseAuthUI.clearInstanceCache()
        applicationContext = ApplicationProvider.getApplicationContext()
        FirebaseApp.getApps(applicationContext).forEach { it.delete() }
        val app = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
        authUI = FirebaseAuthUI.create(app, mock(FirebaseAuth::class.java))
    }

    @After
    fun tearDown() {
        flowState = null
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * The country is picked away from the configured default on purpose: re-resolving from the
     * configuration would restore the default and still look like a pass if the test had started
     * there.
     */
    @Test
    fun `the picked country survives recreation alongside the number it prefixes`() {
        val picked = requireNotNull(CountryUtils.findByCountryCode("DE")) { "DE missing" }

        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent { Host(configuration(defaultCountryCode = "US")) }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            val state = requireNotNull(flowState)
            state.selectedCountry.value = picked
            state.phoneNumber.value = TYPED_NUMBER
        }
        composeTestRule.waitForIdle()

        // Sanity before recreation, so what follows is about recreation and not the fixture.
        assertThat(requireNotNull(flowState).selectedCountry.value).isEqualTo(picked)

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        assertThat(requireNotNull(flowState).phoneNumber.value).isEqualTo(TYPED_NUMBER)
        // The assertion the plain `remember` failed: not merely non-null, but the same country,
        // so the dial code still matches the number above.
        assertThat(requireNotNull(flowState).selectedCountry.value).isEqualTo(picked)
        assertThat(requireNotNull(flowState).selectedCountry.value.dialCode)
            .isEqualTo(picked.dialCode)
    }

    /** A country never picked still falls back to the configured default on a fresh flow. */
    @Test
    fun `an untouched country still comes from the configuration`() {
        val configured = requireNotNull(CountryUtils.findByCountryCode("GB")) { "GB missing" }

        composeTestRule.setContent { Host(configuration(defaultCountryCode = "GB")) }
        composeTestRule.waitForIdle()

        assertThat(requireNotNull(flowState).selectedCountry.value).isEqualTo(configured)
    }

    @Composable
    private fun Host(configuration: AuthUIConfiguration) {
        val state = rememberPhoneAuthFlowState(configuration)
        SideEffect { flowState = state }
    }

    private fun configuration(defaultCountryCode: String): AuthUIConfiguration =
        authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = defaultCountryCode,
                        allowedCountries = null,
                    )
                )
            }
        }

    private companion object {
        const val TYPED_NUMBER = "15123456789"
    }
}
