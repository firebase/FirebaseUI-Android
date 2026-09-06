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

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.testutil.AUTH_STATE_WAIT_TIMEOUT_MS
import com.firebase.ui.auth.testutil.EmulatorAuthApi
import com.firebase.ui.auth.testutil.enrollSmsFactorInEmulator
import com.firebase.ui.auth.testutil.ensureFreshUser
import com.firebase.ui.auth.testutil.ensureTestFirebaseApp
import com.firebase.ui.auth.testutil.verifyEmailInEmulator
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.firebase.ui.auth.util.CountryUtils
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.PhoneMultiFactorInfo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * E2E happy paths for SMS multi-factor authentication, driven through [FirebaseAuthScreen] against
 * the Firebase Auth Emulator.
 *
 * The emulator implements SMS MFA in full — enrollment start/finalize and sign-in start/finalize —
 * and publishes both flows' codes on the same `verificationCodes` endpoint the primary phone auth
 * flow reads, so these tests assert on real enrolled factors and a real [AuthState.Success] rather
 * than on the screens' own state. TOTP is a different story: see [MfaEnrollmentScreenTest].
 *
 * The two paths the emulator enforces, and that every test here has to satisfy:
 * - Enrollment requires a **verified** email (`UNVERIFIED_EMAIL` otherwise).
 * - Anonymous, phone and custom-token sign-ins cannot carry a second factor
 *   (`UNSUPPORTED_FIRST_FACTOR`), so the first factor here is always email/password.
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class MfaSmsFlowTest {
    @get:Rule
    val composeAndroidTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var emulatorApi: EmulatorAuthApi

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)

        val firebaseApp = ensureTestFirebaseApp(applicationContext)
        authUI = FirebaseAuthUI.getInstance()

        emulatorApi = EmulatorAuthApi(
            projectId = firebaseApp.options.projectId
                ?: throw IllegalStateException("Project ID is required for emulator interactions"),
            emulatorHost = "127.0.0.1",
            emulatorPort = 9099
        )

        emulatorApi.clearEmulatorData()
    }

    @After
    fun tearDown() {
        // The FirebaseApp is shared across test classes (see ensureTestFirebaseApp), so the
        // client-side session has to be reset here rather than by app re-creation.
        authUI.auth.signOut()
        FirebaseAuthUI.clearInstanceCache()

        emulatorApi.clearEmulatorData()
    }

    @Test
    fun `SMS enrollment enrolls a real second factor`() {
        val email = "mfa-enroll-${System.currentTimeMillis()}@example.com"
        val password = "test123"
        val nationalNumber = uniqueNationalNumber()
        val phoneNumber = "${CountryUtils.getDefaultCountry().dialCode}$nationalNumber"

        val user = requireNotNull(ensureFreshUser(authUI, email, password)) {
            "Failed to create user"
        }
        verifyEmailInEmulator(authUI, emulatorApi, user)
        signOut()

        var currentAuthState: AuthState = AuthState.Idle
        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(configuration = emailProviderConfiguration())
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        signIn(email, password)
        awaitAuthState<AuthState.Success> { currentAuthState }

        assertThat(authUI.auth.currentUser!!.multiFactor.enrolledFactors).isEmpty()

        awaitNodeWithText(stringProvider.manageMfaAction)
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        // Both factors are offered, so enrollment opens on the picker.
        awaitNode(FirebaseAuthTestTags.MfaEnrollment.ENROLL_SMS_BUTTON).performClick()

        awaitNode(FirebaseAuthTestTags.PhoneNumber.PHONE_NUMBER_FIELD)
            .performTextInput(nationalNumber)
        composeAndroidTestRule.onNodeWithTag(FirebaseAuthTestTags.PhoneNumber.SEND_CODE_BUTTON)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        val code = awaitPhoneVerificationCode(phoneNumber)
        awaitNode(FirebaseAuthTestTags.VerificationCode.CODE_FIELD).performTextInput(code)
        composeAndroidTestRule.onNodeWithTag(FirebaseAuthTestTags.VerificationCode.VERIFY_BUTTON)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        // The factor landing on the account is the assertion; the screen leaving enrollment is not.
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            authUI.auth.currentUser?.multiFactor?.enrolledFactors?.isNotEmpty() == true
        }

        val enrolled = authUI.auth.currentUser!!.multiFactor.enrolledFactors
        assertThat(enrolled).hasSize(1)
        val factor = enrolled.single()
        assertThat(factor).isInstanceOf(PhoneMultiFactorInfo::class.java)
        assertThat((factor as PhoneMultiFactorInfo).phoneNumber).endsWith(phoneNumber.takeLast(4))

        // Completing enrollment leaves the flow, back to the signed-in screen it was entered from.
        awaitNodeWithText(stringProvider.manageMfaAction).assertIsDisplayed()
    }

    @Test
    fun `SMS challenge completes sign-in`() {
        val email = "mfa-challenge-${System.currentTimeMillis()}@example.com"
        val password = "test123"
        val phoneNumber = "${CountryUtils.getDefaultCountry().dialCode}${uniqueNationalNumber()}"

        val user = requireNotNull(ensureFreshUser(authUI, email, password)) {
            "Failed to create user"
        }
        verifyEmailInEmulator(authUI, emulatorApi, user)
        enrollSmsFactorInEmulator(
            activity = composeAndroidTestRule.activity,
            authUI = authUI,
            emulatorApi = emulatorApi,
            user = requireNotNull(authUI.auth.currentUser) { "User signed out during setup" },
            phoneNumber = phoneNumber,
        )
        signOut()

        var currentAuthState: AuthState = AuthState.Idle
        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(configuration = emailProviderConfiguration())
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        signIn(email, password)

        // The password alone does not sign the user in any more.
        awaitAuthState<AuthState.RequiresMfa> { currentAuthState }
        assertThat(authUI.auth.currentUser).isNull()

        val hint = (currentAuthState as AuthState.RequiresMfa).resolver.hints.single()
        assertThat(hint).isInstanceOf(PhoneMultiFactorInfo::class.java)

        // The challenge screen requests the code itself on entry, so this is the first code the
        // emulator holds for this number since enrollment redeemed the last one.
        val code = awaitPhoneVerificationCode(phoneNumber)
        awaitNode(FirebaseAuthTestTags.MfaChallenge.CODE_FIELD).performTextInput(code)
        composeAndroidTestRule.onNodeWithTag(FirebaseAuthTestTags.MfaChallenge.VERIFY_BUTTON)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        awaitAuthState<AuthState.Success> { currentAuthState }

        val signedIn = requireNotNull(authUI.auth.currentUser) { "No user after MFA sign-in" }
        assertThat(signedIn.email).isEqualTo(email)
        assertThat(signedIn.uid).isEqualTo(user.uid)
        assertThat(signedIn.multiFactor.enrolledFactors).hasSize(1)
    }

    private fun emailProviderConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                )
            )
        }
        isCredentialManagerEnabled = false
        isMfaEnabled = true
    }

    private fun signOut() {
        authUI.auth.signOut()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun signIn(email: String, password: String) {
        composeAndroidTestRule.waitForIdle()
        composeAndroidTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .performTextInput(email)
        composeAndroidTestRule.onNodeWithText(stringProvider.passwordHint)
            .performScrollTo()
            .performTextInput(password)
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private inline fun <reified T : AuthState> awaitAuthState(crossinline state: () -> AuthState) {
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            state() is T
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(state()).isInstanceOf(T::class.java)
    }

    /**
     * Waits for the emulator to publish an SMS code for [phoneNumber], then returns it.
     *
     * Both flows under test send their code from a coroutine the paused main looper has to run, so
     * the wait pumps that looper rather than sleeping the thread that owes the work. Waiting for
     * the code to appear is also what orders the read after the send, so there is no window in
     * which an earlier code could be read instead.
     */
    private fun awaitPhoneVerificationCode(phoneNumber: String): String {
        var code: String? = null
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            code = runCatching { emulatorApi.fetchVerifyPhoneCode(phoneNumber) }.getOrNull()
            code != null
        }
        return requireNotNull(code) { "No verification code for $phoneNumber" }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun awaitNode(testTag: String) = composeAndroidTestRule
        .apply { waitUntilAtLeastOneExists(hasTestTag(testTag), AUTH_STATE_WAIT_TIMEOUT_MS) }
        .onNodeWithTag(testTag)

    @OptIn(ExperimentalTestApi::class)
    private fun awaitNodeWithText(text: String) = composeAndroidTestRule
        .apply { waitUntilAtLeastOneExists(hasText(text), AUTH_STATE_WAIT_TIMEOUT_MS) }
        .onNodeWithText(text)

    /**
     * A US number in libphonenumber's valid range — the enrollment step's send button stays
     * disabled otherwise — varying per run, so the emulator's `verificationCodes` list, which
     * survives the account wipe between tests, is unlikely to hold another test's code for it.
     */
    private fun uniqueNationalNumber(): String =
        "202555${(System.currentTimeMillis() % 10_000).toString().padStart(4, '0')}"

    /**
     * Deliberately passes no `authenticatedContent`: enrollment is reached from the default
     * signed-in screen's own "manage MFA" action, which a custom slot would replace.
     */
    @Composable
    private fun TestFirebaseAuthScreen(configuration: AuthUIConfiguration) {
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(applicationContext)
        ) {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = { },
                onSignInFailure = { },
                onSignInCancelled = { },
            )
        }
    }
}
