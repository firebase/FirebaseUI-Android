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

package com.firebase.ui.auth.ui.screens

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.data.ALL_COUNTRIES
import com.firebase.ui.auth.data.CountryData
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
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

/**
 * Covers how [FirebaseAuthScreen] enters the MFA enrolment flow from its authenticated destination
 * — the wiring that [com.firebase.ui.auth.ui.screens.mfa.enterMfaEnrollment] exists for.
 *
 * Hosts the real screen rather than a bare `NavHost`, because which of the host's two entry points
 * clears the flow's state is a property of `FirebaseAuthScreen` itself, not of the destinations:
 * `onManageMfa` and `onNavigate` each choose their own way in.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthScreenMfaEnrollmentEntryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockAuth: FirebaseAuth

    @Mock
    private lateinit var mockUser: FirebaseUser

    @Mock
    private lateinit var mockMultiFactor: MultiFactor

    private lateinit var authUI: FirebaseAuthUI

    private var enrollmentState: MfaEnrollmentContentState? = null
    private var uiContext: AuthSuccessUiContext? = null

    /**
     * The back-stack entry the authenticated destination is currently composed in, or null while
     * it is not composed — see [leaveFlow] for what its identity is worth.
     */
    private var hostEntry: NavBackStackEntry? = null

    /** [hostEntry] as first composed, which every later one is compared against. */
    private var firstHostEntry: NavBackStackEntry? = null

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        FirebaseAuthUI.clearInstanceCache()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach { it.delete() }
        val app = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
        `when`(mockAuth.app).thenReturn(app)
        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("mfa-entry-user")
        `when`(mockUser.email).thenReturn("user@example.com")
        `when`(mockUser.isEmailVerified).thenReturn(true)
        `when`(mockUser.multiFactor).thenReturn(mockMultiFactor)
        `when`(mockMultiFactor.enrolledFactors).thenReturn(emptyList())
        authUI = FirebaseAuthUI.create(app, mockAuth)
    }

    @After
    fun tearDown() {
        enrollmentState = null
        uiContext = null
        hostEntry = null
        firstHostEntry = null
        FirebaseAuthUI.clearInstanceCache()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    /** The CPRN-384 regression, through the public entry point a host actually calls. */
    @Test
    fun `re-entering from the authenticated destination starts on an empty form`() {
        startAuthenticated()
        manageMfa()
        selectFactor(MfaFactor.Sms)
        typePhoneNumber(TYPED_PHONE_NUMBER)
        val pickedCountry = nonDefaultCountry()
        selectCountry(pickedCountry)
        assertThat(requireNotNull(enrollmentState).phoneNumber).isEqualTo(TYPED_PHONE_NUMBER)
        assertThat(requireNotNull(enrollmentState).selectedCountry).isEqualTo(pickedCountry)
        leaveFlow()

        manageMfa()
        selectFactor(MfaFactor.Sms)

        assertThat(requireNotNull(enrollmentState).step).isEqualTo(MfaEnrollmentStep.ConfigureSms)
        assertThat(requireNotNull(enrollmentState).phoneNumber).isEmpty()
        assertThat(requireNotNull(enrollmentState).selectedCountry)
            .isEqualTo(CountryUtils.getDefaultCountry())
    }

    /**
     * [AuthSuccessUiContext.onNavigate] is the other way into the flow, and it names
     * [AuthRoute.MfaEnrollment] rather than a step — so it has to clear the same state
     * [AuthSuccessUiContext.onManageMfa] does.
     */
    @Test
    fun `entering through onNavigate also starts on an empty form`() {
        startAuthenticated()
        manageMfa()
        selectFactor(MfaFactor.Sms)
        typePhoneNumber(TYPED_PHONE_NUMBER)
        leaveFlow()

        navigateTo(AuthRoute.MfaEnrollment)
        selectFactor(MfaFactor.Sms)

        assertThat(requireNotNull(enrollmentState).phoneNumber).isEmpty()
    }

    /**
     * The step-level entry, which used to slip past the clear entirely: naming
     * [AuthRoute.MfaEnrollment.SelectFactor] is indistinguishable to a host from naming
     * [AuthRoute.MfaEnrollment], because the two report the same [AuthRoute.route] — so it has to
     * clear the same state. Across a sign-out the state it used to keep is another user's.
     */
    @Test
    fun `entering through onNavigate at a named step also starts on an empty form`() {
        startAuthenticated()
        manageMfa()
        selectFactor(MfaFactor.Sms)
        typePhoneNumber(TYPED_PHONE_NUMBER)
        leaveFlow()

        navigateTo(AuthRoute.MfaEnrollment.SelectFactor)

        assertThat(requireNotNull(enrollmentState).step).isEqualTo(MfaEnrollmentStep.SelectFactor)
        assertThat(requireNotNull(enrollmentState).phoneNumber).isEmpty()
        assertThat(requireNotNull(enrollmentState).selectedFactor).isNull()
    }

    /**
     * The clear must not cost the host the destination it asked for. Under an SMS-only
     * configuration [com.firebase.ui.auth.ui.screens.mfa.mfaEnrollmentStartStep] resolves to
     * [MfaEnrollmentStep.ConfigureSms], so routing a step-level entry through it would take a
     * host that deliberately named [AuthRoute.MfaEnrollment.SelectFactor] — to show the factor
     * picker, or to let the user unenroll an existing factor — somewhere else.
     */
    @Test
    fun `a named step is honoured over the configuration's resolved start step`() {
        startAuthenticated(MfaConfiguration(allowedFactors = listOf(MfaFactor.Sms)))
        manageMfa()
        assertThat(requireNotNull(enrollmentState).step).isEqualTo(MfaEnrollmentStep.ConfigureSms)
        typePhoneNumber(TYPED_PHONE_NUMBER)
        leaveFlow()

        navigateTo(AuthRoute.MfaEnrollment.SelectFactor)

        assertThat(requireNotNull(enrollmentState).step).isEqualTo(MfaEnrollmentStep.SelectFactor)
        assertThat(requireNotNull(enrollmentState).phoneNumber).isEmpty()
    }

    // Harness

    private fun startAuthenticated(
        mfaConfiguration: MfaConfiguration = MfaConfiguration(),
    ) {
        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                mfaConfiguration = mfaConfiguration,
                mfaEnrollmentContent = { state -> enrollmentState = state },
                authenticatedContent = { _, context ->
                    uiContext = context
                    // navigation-compose composes a destination inside its own entry's
                    // LocalOwnersProvider, so this is that destination's NavBackStackEntry.
                    val entry = LocalViewModelStoreOwner.current as NavBackStackEntry
                    hostEntry = entry
                    if (firstHostEntry == null) firstHostEntry = entry
                    Text(
                        text = "authenticated",
                        modifier = Modifier.testTag(AUTHENTICATED_TAG),
                    )
                },
            )
        }
        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Success(result = null, user = mockUser, isNewUser = false)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
    }

    private fun manageMfa() {
        composeTestRule.runOnIdle { requireNotNull(uiContext).onManageMfa() }
        composeTestRule.waitForIdle()
    }

    private fun selectFactor(factor: MfaFactor) {
        composeTestRule.runOnIdle { requireNotNull(enrollmentState).onFactorSelected(factor) }
        composeTestRule.waitForIdle()
    }

    private fun typePhoneNumber(value: String) {
        composeTestRule.runOnIdle { requireNotNull(enrollmentState).onPhoneNumberChange(value) }
        composeTestRule.waitForIdle()
    }

    private fun selectCountry(country: CountryData) {
        composeTestRule.runOnIdle { requireNotNull(enrollmentState).onCountrySelected(country) }
        composeTestRule.waitForIdle()
    }

    /**
     * A country the device locale cannot already have selected — [CountryUtils.getDefaultCountry]
     * reads `Locale.getDefault().country`, so hardcoding one would let a differently-localed run
     * turn the "country is back to the default" assertion vacuous.
     */
    private fun nonDefaultCountry(): CountryData {
        val default = CountryUtils.getDefaultCountry()
        return ALL_COUNTRIES.first { it.countryCode != default.countryCode }
    }

    /** Enters [route] the way a host does, through [AuthSuccessUiContext.onNavigate]. */
    private fun navigateTo(route: AuthRoute) {
        composeTestRule.runOnIdle { requireNotNull(uiContext).onNavigate(route) }
        composeTestRule.waitForIdle()
    }

    /**
     * Skips out of the flow, from however deep in it the caller got, and asserts the
     * authenticated destination is showing again — so the entry these tests then make really is a
     * re-entry from outside the flow.
     *
     * The identity check is what makes the tag assertion mean something: a host rebuilt from
     * scratch would display the tag just as happily, and rebuilding it destroys whatever
     * `rememberSaveable` an `authenticatedContent` holds. Only reference equality on the
     * `NavBackStackEntry` separates "popped back to the host" from "built a new host". [hostEntry]
     * is cleared first so a destination that never recomposes fails rather than reporting the
     * instance it last saw. (Assertion borrowed from the PR #2467 review.)
     */
    private fun leaveFlow() {
        hostEntry = null
        composeTestRule.runOnIdle {
            requireNotNull(requireNotNull(enrollmentState).onSkipClick).invoke()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
        assertThat(hostEntry).isSameInstanceAs(requireNotNull(firstHostEntry))
    }

    private fun emailConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = ApplicationProvider.getApplicationContext()
        providers {
            provider(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList(),
                )
            )
        }
    }

    private companion object {
        const val AUTHENTICATED_TAG = "authenticated-destination"
        const val TYPED_PHONE_NUMBER = "5551234567"
    }
}
