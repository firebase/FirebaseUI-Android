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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.ActionCodeSettings
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
 * How [EmailAuthScreen] reports a mode switch to whoever hosts it.
 *
 * The screen renders the mode it is given and never changes it, so what is testable here is the
 * report: which mode a switch control names, and that the address the user typed goes with it, so
 * a host can carry it to the destination it navigates to.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class EmailAuthScreenModeSwitchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var authUI: FirebaseAuthUI

    private var lastState: EmailAuthContentState? = null
    private val reported = mutableListOf<Pair<EmailAuthMode, String>>()

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
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
        authUI = FirebaseAuthUI.create(app, mock(FirebaseAuth::class.java))
    }

    @After
    fun tearDown() {
        lastState = null
        reported.clear()
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * The address is the one field a switch must not lose: a user who typed it on the sign-in form
     * and tapped through to sign-up should not have to type it again. It travels as the second
     * argument, because the destination is a different composition that holds nothing of this one.
     */
    @Test
    fun `a switch carries the address typed so far`() {
        start()
        type { it.onEmailChange(TYPED_EMAIL) }

        goTo(EmailAuthMode.SignUp)

        assertThat(reported).containsExactly(EmailAuthMode.SignUp to TYPED_EMAIL)
    }

    /** Every switch control names its own mode, and reports rather than renders it. */
    @Test
    fun `every switch control reports the mode it names`() {
        start()

        goTo(EmailAuthMode.SignUp)
        goTo(EmailAuthMode.ResetPassword)
        goTo(EmailAuthMode.EmailLinkSignIn)
        goTo(EmailAuthMode.SignIn)

        assertThat(reported.map { it.first }).containsExactly(
            EmailAuthMode.SignUp,
            EmailAuthMode.ResetPassword,
            EmailAuthMode.EmailLinkSignIn,
            EmailAuthMode.SignIn,
        ).inOrder()
        // Reporting a switch is not rendering one: the mode on screen is still the one passed in.
        assertThat(state().mode).isEqualTo(EmailAuthMode.SignIn)
    }

    /**
     * Signing in with an address that has no account used to hop this screen to sign-up on its
     * own. That decision belongs to a host — only a host knows what lies outside the email flow,
     * and two observers of one shared error event raced each other for the recovery dialog. The
     * user stays on the form with everything they typed, including the password, which nothing
     * asked to clear.
     */
    @Test
    fun `no switch is reported when the account is not found`() {
        start()
        type {
            it.onEmailChange(TYPED_EMAIL)
            it.onPasswordChange("hunter2")
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Error(AuthException.UserNotFoundException(message = "no such user"))
            )
        }
        composeTestRule.waitForIdle()

        assertThat(reported).isEmpty()
        assertThat(state().mode).isEqualTo(EmailAuthMode.SignIn)
        assertThat(state().email).isEqualTo(TYPED_EMAIL)
        assertThat(state().password).isEqualTo("hunter2")
    }

    /** The same for an address already taken, which used to hop the other way. */
    @Test
    fun `no switch is reported when the address is already in use`() {
        start(EmailAuthMode.SignUp)
        type { it.onEmailChange(TYPED_EMAIL) }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Error(
                    AuthException.EmailAlreadyInUseException(
                        message = "already in use",
                        email = TYPED_EMAIL,
                    )
                )
            )
        }
        composeTestRule.waitForIdle()

        assertThat(reported).isEmpty()
        assertThat(state().mode).isEqualTo(EmailAuthMode.SignUp)
        assertThat(state().email).isEqualTo(TYPED_EMAIL)
    }

    /**
     * The shape this screen cannot defend itself against, pinned so the hazard is visible: a host
     * that satisfies [EmailAuthScreen]'s `mode` and `onNavigateToMode` out of plain state, without
     * giving the target mode a composition of its own.
     *
     * Compose sees one call site and keeps the composition, so every `rememberSaveable` the screen
     * holds survives the switch — including the password typed on the sign-in form, which then
     * greets the user pre-filled on sign-up.
     */
    @Test
    fun `flipping the mode parameter within one composition carries the password over`() {
        startFlippingAParameter(fresh = false)

        type { it.onPasswordChange("hunter2") }
        goTo(EmailAuthMode.SignUp)

        assertThat(state().mode).isEqualTo(EmailAuthMode.SignUp)
        assertThat(state().password).isEqualTo("hunter2")
    }

    /**
     * The same host, one line different: the target mode gets its own composition. That is all real
     * navigation does here, and it is the whole of the difference — nothing in the screen changes.
     */
    @Test
    fun `giving the target mode its own composition leaves the password behind`() {
        startFlippingAParameter(fresh = true)

        type { it.onPasswordChange("hunter2") }
        goTo(EmailAuthMode.SignUp)

        assertThat(state().mode).isEqualTo(EmailAuthMode.SignUp)
        assertThat(state().password).isEmpty()
    }

    /**
     * A host driving the mode from plain state. [fresh] is the only variable under test: whether
     * the target mode is composed anew, the way a navigation destination is.
     */
    private fun startFlippingAParameter(fresh: Boolean) {
        composeTestRule.setContent {
            var mode by remember { mutableStateOf(EmailAuthMode.SignIn) }
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(applicationContext)
            ) {
                key(if (fresh) mode else Unit) {
                    EmailAuthScreen(
                        context = applicationContext,
                        configuration = configuration(),
                        authUI = authUI,
                        onSuccess = {},
                        onError = {},
                        onCancel = {},
                        mode = mode,
                        onNavigateToMode = { target, _ -> mode = target },
                        content = { state -> lastState = state },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    // Email-link sign-in configured, so all four modes are actually on offer: a switch to a mode
    // the provider disables is inert, which is the point of `a provider without email-link
    // sign-in cannot switch to it` rather than of anything here.
    private fun configuration(): AuthUIConfiguration = authUIConfiguration {
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

    private fun start(mode: EmailAuthMode = EmailAuthMode.SignIn) {
        composeTestRule.setContent { EmailScreenUnderTest(configuration(), mode) }
        composeTestRule.waitForIdle()
    }

    private fun state(): EmailAuthContentState =
        composeTestRule.runOnIdle { requireNotNull(lastState) }

    private fun type(block: (EmailAuthContentState) -> Unit) {
        composeTestRule.runOnIdle { block(requireNotNull(lastState)) }
        composeTestRule.waitForIdle()
    }

    private fun goTo(mode: EmailAuthMode) {
        composeTestRule.runOnIdle {
            val current = requireNotNull(lastState)
            when (mode) {
                EmailAuthMode.SignIn -> current.onGoToSignIn()
                EmailAuthMode.SignUp -> current.onGoToSignUp()
                EmailAuthMode.ResetPassword -> current.onGoToResetPassword()
                EmailAuthMode.EmailLinkSignIn -> current.onGoToEmailLinkSignIn()
            }
        }
        composeTestRule.waitForIdle()
    }

    @Composable
    private fun EmailScreenUnderTest(
        configuration: AuthUIConfiguration,
        mode: EmailAuthMode,
    ) {
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(applicationContext)
        ) {
            EmailAuthScreen(
                context = applicationContext,
                configuration = configuration,
                authUI = authUI,
                onSuccess = {},
                onError = {},
                onCancel = {},
                mode = mode,
                // A host navigates here; recording is enough to assert what it would be told.
                onNavigateToMode = { target, email -> reported += target to email },
                content = { state -> lastState = state },
            )
        }
    }

    private companion object {
        const val TYPED_EMAIL = "user+tag@example.com"
    }
}
