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

package com.firebase.ui.auth.ui.screens.mfa

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.util.CountryUtils
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactor
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
import java.util.Locale

/**
 * A host driving [MfaEnrollmentScreen] itself, the way the README's custom-navigation example
 * does, with a `flowState` it built **without** passing the restriction.
 *
 * [MfaEnrollmentFlowState] seeds its country from the list handed to
 * [rememberMfaEnrollmentFlowState], which [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen]
 * supplies. That left the restriction depending on a defaulted argument: a host that omitted it
 * got a filtered selector and an unpermitted dial code on send. The screen reconciles the country
 * on entry so omitting it cannot bring that back — the same reason a
 * `rememberSaveable` restore of a country an earlier configuration allowed cannot either.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class MfaEnrollmentCountryReconciliationTest {

    // The screen requires LocalActivity for SMS verification, so this needs a real Activity.
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Mock
    private lateinit var mockAuth: FirebaseAuth

    @Mock
    private lateinit var mockUser: FirebaseUser

    @Mock
    private lateinit var mockMultiFactor: MultiFactor

    private lateinit var applicationContext: Context
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        applicationContext = ApplicationProvider.getApplicationContext()
        originalLocale = Locale.getDefault()
        // The device country the restriction below does not permit.
        Locale.setDefault(Locale.Builder().setLanguage("en").setRegion(DEVICE_COUNTRY).build())

        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach { it.delete() }
        val app = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
        `when`(mockAuth.app).thenReturn(app)
        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("mfa-reconcile-user")
        `when`(mockUser.email).thenReturn("user@example.com")
        `when`(mockUser.isEmailVerified).thenReturn(true)
        `when`(mockUser.multiFactor).thenReturn(mockMultiFactor)
        `when`(mockMultiFactor.enrolledFactors).thenReturn(emptyList())
        authUI = FirebaseAuthUI.create(app, mockAuth)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * The dial code is the assertion, not the selector's list: a filtered picker beside an
     * unpermitted prefix is the exact mismatch this closes.
     */
    @Test
    fun `entering the SMS step snaps an unpermitted country into the allowed set`() {
        val flowState = hostScreenWithUnpassedRestriction()

        assertThat(flowState.selectedCountry.value.countryCode).isEqualTo(ALLOWED_COUNTRY)
        assertThat(flowState.selectedCountry.value.dialCode)
            .isEqualTo(CountryUtils.findByCountryCode(ALLOWED_COUNTRY)!!.dialCode)
        assertThat(flowState.selectedCountry.value.dialCode)
            .isNotEqualTo(CountryUtils.findByCountryCode(DEVICE_COUNTRY)!!.dialCode)
    }

    /** An unrestricted configuration must not move the device country. */
    @Test
    fun `entering the SMS step leaves the country alone when unrestricted`() {
        val flowState = hostScreenWithUnpassedRestriction(allowedCountries = null)

        assertThat(flowState.selectedCountry.value.countryCode).isEqualTo(DEVICE_COUNTRY)
    }

    /**
     * Hosts the step with a `flowState` built the way a host that never passed the restriction
     * would build it, so the seeding in [rememberMfaEnrollmentFlowState] cannot be what passes
     * this.
     */
    private fun hostScreenWithUnpassedRestriction(
        allowedCountries: List<String>? = listOf(ALLOWED_COUNTRY),
    ): MfaEnrollmentFlowState {
        lateinit var captured: MfaEnrollmentFlowState
        val step = mutableStateOf(MfaEnrollmentStep.ConfigureSms)

        composeTestRule.setContent {
            val flowState = rememberMfaEnrollmentFlowState()
            captured = flowState
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(applicationContext)
            ) {
                MfaEnrollmentScreen(
                    user = mockUser,
                    auth = authUI.auth,
                    configuration = MfaConfiguration(
                        allowedFactors = listOf(MfaFactor.Sms),
                        allowedCountries = allowedCountries,
                    ),
                    onComplete = {},
                    step = step.value,
                    onNavigateToStep = { step.value = it },
                    onNavigateBack = {},
                    flowState = flowState,
                )
            }
        }
        composeTestRule.waitForIdle()
        return captured
    }

    private companion object {
        const val DEVICE_COUNTRY = "US"
        const val ALLOWED_COUNTRY = "GB"
    }
}
