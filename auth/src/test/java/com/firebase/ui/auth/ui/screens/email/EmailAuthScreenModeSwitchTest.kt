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
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [EmailAuthScreen] used on its own, with nothing outside it driving the mode — the shape
 * `EmailAuthSlotDemoActivity` and the custom reauthentication content both use.
 *
 * Guards that a mode switch clears only the mode-specific secrets and keeps the typed address.
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
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    @Test
    fun `the typed address survives every mode switch`() {
        start()
        type { it.onEmailChange(TYPED_EMAIL) }

        listOf(
            EmailAuthMode.SignUp,
            EmailAuthMode.SignIn,
            EmailAuthMode.ResetPassword,
            EmailAuthMode.EmailLinkSignIn,
            EmailAuthMode.SignIn,
        ).forEach { target ->
            goTo(target)

            assertThat(state().mode).isEqualTo(target)
            assertThat(state().email).isEqualTo(TYPED_EMAIL)
        }
    }

    @Test
    fun `switching modes still clears the password, its confirmation and the display name`() {
        start()
        type {
            it.onEmailChange(TYPED_EMAIL)
            it.onPasswordChange("hunter2")
            it.onConfirmPasswordChange("hunter2")
            it.onDisplayNameChange("Ada")
        }

        goTo(EmailAuthMode.SignUp)

        assertThat(state().password).isEmpty()
        assertThat(state().confirmPassword).isEmpty()
        assertThat(state().displayName).isEmpty()
    }

    /** Without a host driving it, the screen owns the mode itself. */
    @Test
    fun `the unhosted screen drives its own mode from local state`() {
        start()

        assertThat(state().mode).isEqualTo(EmailAuthMode.SignIn)

        goTo(EmailAuthMode.ResetPassword)
        assertThat(state().mode).isEqualTo(EmailAuthMode.ResetPassword)

        goTo(EmailAuthMode.SignIn)
        assertThat(state().mode).isEqualTo(EmailAuthMode.SignIn)
    }

    /**
     * The screen never moves itself on an error: unhosted, the user stays on the form with
     * everything they typed, password included.
     */
    @Test
    fun `an unhosted screen stays put and keeps what was typed when the account is not found`() {
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

        assertThat(state().mode).isEqualTo(EmailAuthMode.SignIn)
        assertThat(state().email).isEqualTo(TYPED_EMAIL)
        assertThat(state().password).isEqualTo("hunter2")
    }

    /** The same for an address already taken. */
    @Test
    fun `an unhosted screen stays put when the address is already in use`() {
        start()
        goTo(EmailAuthMode.SignUp)
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

        assertThat(state().mode).isEqualTo(EmailAuthMode.SignUp)
        assertThat(state().email).isEqualTo(TYPED_EMAIL)
    }

    /**
     * `mode` and `onNavigateToMode` are two halves of one contract; either alone leaves every
     * switch control silently inert, so it is rejected.
     */
    @Test
    fun `a mode with no way to report a switch is rejected`() {
        val thrown = assertThrows(IllegalArgumentException::class.java) {
            startWith(mode = EmailAuthMode.SignUp, onNavigateToMode = null)
        }

        // The message names both halves, so either omission is identifiable.
        assertThat(thrown).hasMessageThat().contains("mode")
        assertThat(thrown).hasMessageThat().contains("onNavigateToMode")
    }

    /** The message is just as usable when it is `mode` that was forgotten. */
    @Test
    fun `a switch callback with no mode to render is rejected`() {
        val thrown = assertThrows(IllegalArgumentException::class.java) {
            startWith(mode = null, onNavigateToMode = { _, _ -> })
        }

        assertThat(thrown).hasMessageThat().contains("mode")
        assertThat(thrown).hasMessageThat().contains("onNavigateToMode")
    }

    private fun startWith(
        mode: EmailAuthMode?,
        onNavigateToMode: ((EmailAuthMode, String) -> Unit)?,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides
                        DefaultAuthUIStringProvider(applicationContext)
            ) {
                EmailAuthScreen(
                    context = applicationContext,
                    configuration = configuration(),
                    authUI = authUI,
                    onSuccess = {},
                    onError = {},
                    onCancel = {},
                    mode = mode,
                    onNavigateToMode = onNavigateToMode,
                    content = { },
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    // Email-link sign-in configured, so all four modes are actually on offer.
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

    private fun start() {
        composeTestRule.setContent { EmailScreenUnderTest(configuration()) }
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
    private fun EmailScreenUnderTest(configuration: AuthUIConfiguration) {
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
                content = { state -> lastState = state },
            )
        }
    }

    private companion object {
        const val TYPED_EMAIL = "user+tag@example.com"
    }
}
