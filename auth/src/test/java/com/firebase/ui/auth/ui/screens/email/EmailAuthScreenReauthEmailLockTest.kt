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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import androidx.compose.runtime.CompositionLocalProvider
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.firebase.ui.auth.ui.components.LocalTopLevelDialogController
import com.firebase.ui.auth.ui.components.rememberTopLevelDialogController
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
     * Unhosted, this screen shows the error itself but never acts on it: error recovery belongs to
     * a host, which alone knows what lies outside the email flow. With nothing to do, an action
     * button on the dialog could only dismiss it, so it is not rendered — and the dialog keeps
     * explaining the error, which is the part a standalone caller still needs.
     */
    @Test
    fun `the reauth sub-flow error dialog offers no action button`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                val controller = rememberTopLevelDialogController(
                    stringProvider = stringProvider,
                    authState = { AuthState.Idle },
                )
                CompositionLocalProvider(LocalTopLevelDialogController provides controller) {
                    EmailAuthScreenUnderTest(reauthConfiguration(), prefillEmail)
                    controller.CurrentDialog()
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Error(AuthException.UserNotFoundException(message = "nope"))
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.dismissAction).assertExists()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON).assertDoesNotExist()
    }

    /**
     * A mode switch must never leave the user on an empty read-only field. It used to clear the
     * address and put the locked one back; it now keeps the address outright, which reaches the
     * same place by not breaking it in the first place.
     */
    @Test
    fun `the locked email survives a mode switch`() {
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
     * reaches ResetPassword itself, and a custom `emailContent` slot can reach the rest.
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
     * The two out-of-band email routes are not equivalent during reauthentication. A password reset
     * email leaves the sheet up and the request outstanding, so it stays available — blocking it stranded
     * a user who had forgotten their password with no route but dismissal. An email *link* reopens
     * the app with no request outstanding, so completing it reports an interruption instead of finishing the
     * pending operation, and it stays hidden.
     */
    @Test
    fun `password recovery is offered while reauthenticating but email-link sign-in is not`() {
        composeTestRule.setContent {
            EmailAuthScreenUnderTest(reauthConfigurationWithEmailLink(), prefill = prefillEmail)
        }

        // The password field proves this is the reauth SignIn screen, still usable as intended.
        composeTestRule.onNodeWithText(stringProvider.passwordHint).assertExists()
        composeTestRule.onNodeWithText(stringProvider.troubleSigningIn).assertExists()
        composeTestRule.onNodeWithText(stringProvider.signInWithEmailLink, ignoreCase = true)
            .assertDoesNotExist()
    }

    /**
     * The callback side of the same asymmetry, which a custom `emailContent` slot reaches directly:
     * the ResetPassword switch has to work, the EmailLink switch has to stay inert.
     */
    @Test
    fun `the reauth ResetPassword mode switch works while the EmailLink one is inert`() {
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

        assertThat(observed.last()).isEqualTo(EmailAuthMode.ResetPassword)

        composeTestRule.runOnIdle { requireNotNull(goToEmailLinkSignIn).invoke() }
        composeTestRule.waitForIdle()

        assertThat(observed.last()).isEqualTo(EmailAuthMode.ResetPassword)
        assertThat(observed.toSet())
            .containsExactly(EmailAuthMode.SignIn, EmailAuthMode.ResetPassword)
    }

    /**
     * The mirror case: outside reauthentication nothing is locked, so a round-trip must leave the
     * field editable.
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
