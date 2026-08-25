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

package com.firebase.ui.auth.ui.screens.email

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import androidx.compose.runtime.CompositionLocalProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The reauthentication email lock has to survive an [EmailAuthMode] round-trip.
 *
 * [DefaultEmailAuthContent] dispatches modes with a `when`, so leaving [EmailAuthMode.SignIn]
 * *disposes* the [SignInUI] composition group and coming back creates a fresh one. Any lock
 * [SignInUI] inferred from its own (mutable) field value was therefore re-decided on every return —
 * either dropping the lock, or locking an address the library never prefilled.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class EmailAuthScreenReauthEmailLockTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider
    private lateinit var authUI: FirebaseAuthUI

    private val prefillEmail = "linked@example.com"

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)
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
        val providerInfo = mock(UserInfo::class.java)
        `when`(providerInfo.providerId).thenReturn("password")
        val user = mock(FirebaseUser::class.java)
        `when`(user.providerData).thenReturn(listOf(providerInfo))
        `when`(user.email).thenReturn(prefillEmail)
        `when`(user.uid).thenReturn("uid-password")
        val auth = mock(FirebaseAuth::class.java)
        `when`(auth.currentUser).thenReturn(user)
        authUI = FirebaseAuthUI.create(app, auth)
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    /** The configuration `FirebaseAuthUI.createReauthFlow` actually produces. */
    private fun reauthConfiguration(): AuthUIConfiguration {
        val configuration = authUIConfiguration {
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
        }
        return authUI.createReauthFlow(configuration).configuration
    }

    /** The same reauth configuration, but with email-link sign-in available. */
    private fun reauthConfigurationWithEmailLink(): AuthUIConfiguration {
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        isEmailLinkSignInEnabled = true,
                        emailLinkActionCodeSettings = ActionCodeSettings.newBuilder()
                            .setUrl("https://example.com")
                            .setHandleCodeInApp(true)
                            .setAndroidPackageName("com.test", true, null)
                            .build(),
                        passwordValidationRules = emptyList()
                    )
                )
            }
            isCredentialManagerEnabled = false
        }
        return authUI.createReauthFlow(configuration).configuration
    }

    @Composable
    private fun EmailAuthScreenUnderTest(
        configuration: AuthUIConfiguration,
        prefill: String?,
    ) {
        CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
            EmailAuthScreen(
                context = applicationContext,
                configuration = configuration,
                authUI = authUI,
                prefillEmail = prefill,
                onSuccess = {},
                onError = {},
                onCancel = {},
            )
        }
    }

    /**
     * Resetting the text fields (the mode switches all do it) must put the locked address back,
     * not leave the user on an empty read-only field.
     */
    @Test
    fun `the locked email is restored when the text fields are reset`() {
        var email: String? = null
        var isEmailLocked: Boolean? = null
        var goToSignIn: (() -> Unit)? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                EmailAuthScreen(
                    context = applicationContext,
                    configuration = reauthConfiguration(),
                    authUI = authUI,
                    prefillEmail = prefillEmail,
                    onSuccess = {},
                    onError = {},
                    onCancel = {},
                    content = { state ->
                        email = state.email
                        isEmailLocked = state.isEmailLocked
                        goToSignIn = state.onGoToSignIn
                    },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { requireNotNull(goToSignIn).invoke() }
        composeTestRule.waitForIdle()

        assertThat(email).isEqualTo(prefillEmail)
        assertThat(isEmailLocked).isTrue()
    }

    /**
     * The lock is wired into every mode that shows the address, not only SignIn — reauthentication
     * cannot reach those modes any more, but a custom `emailContent` slot and a future route can.
     */
    @Test
    fun `ResetPasswordUI renders a locked email read-only`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                ResetPasswordUI(
                    configuration = reauthConfiguration(),
                    isLoading = false,
                    email = prefillEmail,
                    resetLinkSent = false,
                    onEmailChange = {},
                    onSendResetLink = {},
                    onGoToSignIn = {},
                    isEmailLocked = true,
                )
            }
        }

        composeTestRule.onNodeWithText(stringProvider.recoverPasswordPageTitle).assertExists()
        composeTestRule.onNodeWithText(prefillEmail)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
    }

    /** The same for the email-link route, the other mode that shows the address. */
    @Test
    fun `SignInEmailLinkUI renders a locked email read-only`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                SignInEmailLinkUI(
                    configuration = reauthConfigurationWithEmailLink(),
                    isLoading = false,
                    emailSignInLinkSent = false,
                    email = prefillEmail,
                    onEmailChange = {},
                    onSignInWithEmailLink = {},
                    onGoToSignIn = {},
                    onGoToResetPassword = {},
                    isEmailLocked = true,
                )
            }
        }

        composeTestRule.onNodeWithText(stringProvider.passwordHint).assertDoesNotExist()
        composeTestRule.onNodeWithText(prefillEmail)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
    }

    /**
     * Both routes hand off to an out-of-band email step the reauthentication sheet cannot observe,
     * and an email link reopens the app with no pending operation left to resume.
     */
    @Test
    fun `neither password recovery nor email-link sign-in is offered while reauthenticating`() {
        composeTestRule.setContent {
            EmailAuthScreenUnderTest(reauthConfigurationWithEmailLink(), prefill = prefillEmail)
        }

        // The password field proves this is the reauth SignIn screen, still usable as intended.
        composeTestRule.onNodeWithText(stringProvider.passwordHint).assertExists()
        composeTestRule.onNodeWithText(stringProvider.troubleSigningIn).assertDoesNotExist()
        composeTestRule.onNodeWithText(stringProvider.signInWithEmailLink, ignoreCase = true)
            .assertDoesNotExist()
    }

    /** Defence in depth: a custom `emailContent` slot cannot reach those modes either. */
    @Test
    fun `the reauth mode switches to ResetPassword and EmailLink are inert`() {
        val observed = mutableListOf<EmailAuthMode>()
        var goToResetPassword: (() -> Unit)? = null
        var goToEmailLinkSignIn: (() -> Unit)? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                EmailAuthScreen(
                    context = applicationContext,
                    configuration = reauthConfigurationWithEmailLink(),
                    authUI = authUI,
                    prefillEmail = prefillEmail,
                    onSuccess = {},
                    onError = {},
                    onCancel = {},
                    content = { state ->
                        observed.add(state.mode)
                        goToResetPassword = state.onGoToResetPassword
                        goToEmailLinkSignIn = state.onGoToEmailLinkSignIn
                    },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { requireNotNull(goToResetPassword).invoke() }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { requireNotNull(goToEmailLinkSignIn).invoke() }
        composeTestRule.waitForIdle()

        assertThat(observed.toSet()).containsExactly(EmailAuthMode.SignIn)
    }

    /**
     * The mirror case: outside reauthentication nothing is locked, so a round-trip must leave the
     * field editable (and the "sign in" mode switch keeps clearing it as it always did).
     */
    @Test
    fun `the email field stays editable across a round-trip outside reauthentication`() {
        val configuration = authUIConfiguration {
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
        }

        composeTestRule.setContent {
            EmailAuthScreenUnderTest(configuration, prefill = prefillEmail)
        }

        composeTestRule.onNodeWithText(stringProvider.troubleSigningIn).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.signInDefault, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
    }

    /**
     * With nothing prefilled there is nothing to lock, so the standalone `createReauthFlow` entry
     * point must not strand the user on a blank read-only field.
     */
    @Test
    fun `nothing is locked in reauthentication mode when nothing was prefilled`() {
        composeTestRule.setContent {
            EmailAuthScreenUnderTest(reauthConfiguration(), prefill = null)
        }

        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
    }

    /**
     * `EmailAuthContentState.isEmailLocked` is the signal a custom `emailContent` slot needs in
     * order to render the field read-only itself, and it must not flip as the user moves modes.
     */
    @Test
    fun `isEmailLocked is reported to a custom content slot and is stable across modes`() {
        val observed = mutableListOf<Pair<EmailAuthMode, Boolean>>()
        var goToSignIn: (() -> Unit)? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                EmailAuthScreen(
                    context = applicationContext,
                    configuration = reauthConfiguration(),
                    authUI = authUI,
                    prefillEmail = prefillEmail,
                    onSuccess = {},
                    onError = {},
                    onCancel = {},
                    content = { state ->
                        observed.add(state.mode to state.isEmailLocked)
                        goToSignIn = state.onGoToSignIn
                    },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { requireNotNull(goToSignIn).invoke() }
        composeTestRule.waitForIdle()

        assertThat(observed.map { it.first }.last()).isEqualTo(EmailAuthMode.SignIn)
        assertThat(observed.map { it.second }.toSet()).containsExactly(true)
    }

    /** A locked address is inert: nothing may substitute another account for the one being re-proved. */
    @Test
    fun `onEmailChange cannot replace a locked address`() {
        var email: String? = null
        var onEmailChange: ((String) -> Unit)? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                EmailAuthScreen(
                    context = applicationContext,
                    configuration = reauthConfiguration(),
                    authUI = authUI,
                    prefillEmail = prefillEmail,
                    onSuccess = {},
                    onError = {},
                    onCancel = {},
                    content = { state ->
                        email = state.email
                        onEmailChange = state.onEmailChange
                    },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { requireNotNull(onEmailChange).invoke("attacker@example.com") }
        composeTestRule.waitForIdle()

        assertThat(email).isEqualTo(prefillEmail)
    }
}
