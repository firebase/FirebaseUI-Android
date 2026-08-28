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
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.R
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.ui.components.ERROR_DIALOG_ACTION_TEST_TAG
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorResolver
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
 * Contract tests for the [ReauthContentState] handed to [FirebaseAuthScreen]'s `reauthContent`
 * slot: the slot only ever chooses a provider, and the library owns every credential path —
 * including temporarily presenting its own email sub-flow (prefilled with the reauthenticating
 * user's address) for [AuthProvider.Email].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthScreenReauthContentStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var stringProvider: DefaultAuthUIStringProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(context).forEach { it.delete() }
        FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
        authUI = FirebaseAuthUI.getInstance()
        stringProvider = DefaultAuthUIStringProvider(context)
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(context).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    /** A user linked to the password provider only — phone must be filtered out of the slot. */
    private fun passwordOnlyUser(email: String?): FirebaseUser = userLinkedTo("password", email)

    /** A user linked only to a provider that is *not* configured, so nothing can be offered. */
    private fun googleOnlyUser(email: String?): FirebaseUser = userLinkedTo("google.com", email)

    private fun userLinkedTo(providerId: String, email: String?): FirebaseUser {
        val providerInfo = mock(UserInfo::class.java)
        `when`(providerInfo.providerId).thenReturn(providerId)
        val user = mock(FirebaseUser::class.java)
        `when`(user.providerData).thenReturn(listOf(providerInfo))
        `when`(user.email).thenReturn(email)
        `when`(user.uid).thenReturn("uid-$providerId")
        return user
    }

    private fun emailAndPhoneConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = this@FirebaseAuthScreenReauthContentStateTest.context
        providers {
            provider(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                )
            )
            provider(
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = null,
                    allowedCountries = null
                )
            )
        }
        isCredentialManagerEnabled = false
    }

    @Test
    fun `reauthContent receives only the providers linked to the user`() {
        val user = passwordOnlyUser("linked@example.com")
        var captured: ReauthContentState? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = { state ->
                    captured = state
                    Text(
                        text = "REAUTH:${state.reason}",
                        modifier = Modifier.testTag("reauth_slot")
                    )
                }
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, reason = "Confirm it is you")
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()
        composeTestRule.onNodeWithText("REAUTH:Confirm it is you").assertIsDisplayed()

        val state = requireNotNull(captured) { "reauthContent was never composed" }
        assertThat(state.providers.map { it.providerId }).containsExactly("password")
        assertThat(state.user).isSameInstanceAs(user)
        assertThat(state.reason).isEqualTo("Confirm it is you")
        assertThat(state.error).isNull()
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `selecting email from the reauth slot presents the library email sub-flow prefilled`() {
        val user = passwordOnlyUser("linked@example.com")
        // The sub-flow starts its own authStateFlow() collector, and a fresh AuthStateListener
        // fires immediately: over a signed-out session that legitimately disarms the reauth.
        val signedInAuthUI = signedInAuthUI(user)

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                emailContent = { state ->
                    Text(
                        text = "EMAIL_SUBFLOW:${state.email}",
                        modifier = Modifier.testTag("email_subflow")
                    )
                },
                reauthContent = { state ->
                    Button(
                        onClick = { state.onProviderSelected(state.providers.first()) },
                        modifier = Modifier.testTag("pick_provider")
                    ) {
                        Text("Continue")
                    }
                }
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.ReauthenticationRequired(user))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pick_provider").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("email_subflow").assertIsDisplayed()
        composeTestRule.onNodeWithText("EMAIL_SUBFLOW:linked@example.com").assertIsDisplayed()
    }

    @Test
    fun `cancelling the email sub-flow returns to the reauth slot`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = { state ->
                    Button(
                        onClick = { state.onProviderSelected(state.providers.first()) },
                        modifier = Modifier.testTag("pick_provider")
                    ) {
                        Text("Continue")
                    }
                }
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.ReauthenticationRequired(user))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pick_provider").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(stringProvider.backAction).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pick_provider").assertIsDisplayed()
    }

    /**
     * A dismissed provider sheet (Credential Manager, an OAuth web flow, …) emits
     * [AuthState.Cancelled]. While reauthentication is armed that only cancels *that attempt*: the
     * slot must stay up, the flow must not report itself cancelled, and the pending sensitive
     * operation must survive so a later successful reauthentication still runs it.
     */
    @Test
    fun `cancelling a provider attempt keeps the reauth slot armed`() {
        val user = passwordOnlyUser("linked@example.com")
        var cancelledCount = 0
        var retryRan = false

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = { cancelledCount++ },
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                }
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { retryRan = true })
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()

        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Cancelled()) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()
        assertThat(cancelledCount).isEqualTo(0)
        assertThat(retryRan).isFalse()

        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.Success(result = null, user = user, reauthenticatedUid = user.uid))
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { retryRan }

        assertThat(retryRan).isTrue()
    }

    /**
     * The same contract on the default bottom-sheet path: a cancelled provider attempt must not
     * report the flow as cancelled nor drop the pending operation.
     */
    @Test
    fun `cancelling a provider attempt in the default reauth sheet keeps it armed`() {
        val phoneInfo = mock(UserInfo::class.java)
        `when`(phoneInfo.providerId).thenReturn("phone")
        val passwordInfo = mock(UserInfo::class.java)
        `when`(passwordInfo.providerId).thenReturn("password")
        val user = mock(FirebaseUser::class.java)
        `when`(user.providerData).thenReturn(listOf(passwordInfo, phoneInfo))
        `when`(user.email).thenReturn("linked@example.com")
        `when`(user.uid).thenReturn("uid-multi")

        var cancelledCount = 0
        var retryRan = false

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = { cancelledCount++ },
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { retryRan = true })
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Cancelled()) }
        composeTestRule.waitForIdle()

        assertThat(cancelledCount).isEqualTo(0)
        assertThat(retryRan).isFalse()

        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.Success(result = null, user = user, reauthenticatedUid = user.uid))
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { retryRan }

        assertThat(retryRan).isTrue()
    }

    /**
     * [ReauthContentState.error] has to outlive the reset-to-Idle that consumes [AuthState.Error],
     * carry the *localized* message rather than the raw throwable message, and be suppressed from
     * the library's own error dialog so the failure surfaces exactly once — in the slot.
     */
    @Test
    fun `a failed attempt latches a localized error and exception into the slot`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        var captured: ReauthContentState? = null
        val rawMessage = "RAW-BACKEND-CODE-17"
        val thrown = FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", rawMessage)
        val expectedMessage = requireNotNull(AuthException.from(thrown, stringProvider).message)

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = { state ->
                    captured = state
                    Button(
                        onClick = { state.onProviderSelected(state.providers.first()) },
                        modifier = Modifier.testTag("pick_provider")
                    ) {
                        Text("SLOT_ERROR=${state.error}")
                    }
                }
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.ReauthenticationRequired(user))
        }
        composeTestRule.waitForIdle()
        assertThat(requireNotNull(captured).error).isNull()

        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Error(thrown)) }
        composeTestRule.waitForIdle()

        assertThat(requireNotNull(captured).error).isEqualTo(expectedMessage)
        assertThat(requireNotNull(captured).error).doesNotContain(rawMessage)
        assertThat(requireNotNull(captured).exception)
            .isInstanceOf(AuthException.InvalidCredentialsException::class.java)
        assertThat(requireNotNull(captured).exception?.cause).isSameInstanceAs(thrown)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SLOT_ERROR=$expectedMessage").assertIsDisplayed()
        assertThat(requireNotNull(captured).error).isEqualTo(expectedMessage)

        assertThat(
            composeTestRule.onAllNodesWithText(expectedMessage).fetchSemanticsNodes()
        ).isEmpty()

        // Opening and backing out of the email sub-flow is not an attempt, so the latched
        // failure survives it — otherwise a mis-tap would silently erase a real error.
        composeTestRule.onNodeWithTag("pick_provider").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(stringProvider.backAction).performClick()
        composeTestRule.waitForIdle()

        assertThat(requireNotNull(captured).error).isEqualTo(expectedMessage)
        assertThat(requireNotNull(captured).exception).isNotNull()
    }

    /**
     * When no configured provider is linked to the user there is no reauth UI to show, so nothing
     * may stay armed — otherwise a later Loading → Success would consume the pending operation and
     * run the sensitive action with no reauthentication at all.
     */
    @Test
    fun `no linked providers leaves nothing armed`() {
        val user = googleOnlyUser("federated@example.com")
        var slotComposed = false
        var retryRan = false

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = {
                    slotComposed = true
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                }
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { retryRan = true })
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reauth_slot").assertDoesNotExist()
        assertThat(slotComposed).isFalse()

        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.Success(result = null, user = user))
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        assertThat(retryRan).isFalse()
    }

    /**
     * A [FirebaseAuthUI] over a mocked, *signed-in* [com.google.firebase.auth.FirebaseAuth] — the
     * only state reauthentication can happen in, and the one the rest of this suite cannot reach
     * (with no current user `authStateFlow()` falls back to [AuthState.Idle] instead).
     */
    private fun signedInAuthUI(user: FirebaseUser): FirebaseAuthUI {
        `when`(user.isEmailVerified).thenReturn(true)
        val auth = mock(FirebaseAuth::class.java)
        `when`(auth.currentUser).thenReturn(user)
        `when`(auth.app).thenReturn(FirebaseApp.getInstance())
        return FirebaseAuthUI.create(FirebaseApp.getInstance(), auth)
    }

    /**
     * The sensitive operation must never run without an actual credential exchange.
     *
     * `authStateFlow()` prefers the internal state and otherwise falls back to the live Firebase
     * session, so for the (necessarily signed-in) user being reauthenticated *every* reset to
     * [AuthState.Idle] re-emits an [AuthState.Success] for the session that already existed —
     * after a cancelled provider attempt, after a latched error, and whenever a provider retracts
     * its own [AuthState.Loading] (`clearLoadingState`, e.g. cancelled phone verification). None of
     * those is evidence of reauthentication, and no one-step lookback at the previous state can
     * tell them apart: this sequence ends on `Loading -> Success`, exactly the shape a genuine
     * reauthentication has.
     */
    @Test
    fun `an ambient Success from the signed-in session does not run the pending operation`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        var retryRan = false

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                },
                authenticatedContent = { _, _ -> Text(text = "AUTHENTICATED") },
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { retryRan = true })
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()

        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Cancelled()) }
        composeTestRule.waitForIdle()
        assertThat(retryRan).isFalse()

        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Idle) }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        assertThat(retryRan).isFalse()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()
    }

    /**
     * The other half of the contract above: an [AuthState.Success] the library published itself —
     * what every provider's credential exchange ends with — does consume the operation, exactly
     * once, even though the ambient session is emitting Successes of its own.
     */
    @Test
    fun `a library-published Success runs the pending operation exactly once`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        var retryCount = 0

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                },
                authenticatedContent = { _, _ -> Text(text = "AUTHENTICATED") },
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { retryCount++ })
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.Success(result = null, user = user, reauthenticatedUid = user.uid))
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { retryCount > 0 }

        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Idle) }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        assertThat(retryCount).isEqualTo(1)
    }

    /**
     * The error dialog's recovery actions navigate the *outer* NavHost to the non-reauth email
     * screen. While a reauthentication is armed both `onRecover` and `onRetry` are withheld, so the
     * dialog has no action to offer and must not render an action button that silently dismisses
     * instead of recovering. This is the default-sheet path — with a custom slot the error latches
     * into the slot and no dialog is shown at all.
     */
    @Test
    fun `a recoverable error offers no action while reauthentication is armed`() {
        val phoneInfo = mock(UserInfo::class.java)
        `when`(phoneInfo.providerId).thenReturn("phone")
        val passwordInfo = mock(UserInfo::class.java)
        `when`(passwordInfo.providerId).thenReturn("password")
        val user = mock(FirebaseUser::class.java)
        `when`(user.providerData).thenReturn(listOf(passwordInfo, phoneInfo))
        `when`(user.email).thenReturn("linked@example.com")
        `when`(user.uid).thenReturn("uid-multi")

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = {})
            )
        }
        composeTestRule.waitForIdle()

        // The sheet opens on its method picker (two linked providers), so no password field is on
        // screen yet. The outer NavHost is still on the method-picker route behind it.
        composeTestRule.onAllNodesWithText(stringProvider.passwordHint).assertCountEquals(0)

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Error(
                    AuthException.EmailAlreadyInUseException(
                        message = "already in use",
                        email = "linked@example.com",
                    )
                )
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertExists()

        // Ungated, onRecover would navigate the outer NavHost to the non-reauth email screen.
        // With both callbacks withheld the button has nothing to do, so it must not render.
        composeTestRule.onNodeWithTag(ERROR_DIALOG_ACTION_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText(stringProvider.dismissAction).assertExists()
        composeTestRule.onAllNodesWithText(stringProvider.passwordHint).assertCountEquals(0)
    }

    /**
     * The control for the test above: outside reauthentication the same error still offers its
     * recovery action, and it still navigates to the email screen.
     */
    @Test
    fun `a recoverable error still offers its recovery action outside reauthentication`() {
        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
            )
        }

        composeTestRule.onAllNodesWithText(stringProvider.passwordHint).assertCountEquals(0)

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Error(
                    AuthException.EmailAlreadyInUseException(
                        message = "already in use",
                        email = "linked@example.com",
                    )
                )
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(ERROR_DIALOG_ACTION_TEST_TAG).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.passwordHint).assertExists()
    }

    /**
     * The method picker stays composed underneath a custom reauth slot, wired to the *non-reauth*
     * configuration. A tap reaching it would start an ordinary sign-in while a sensitive operation
     * is pending, so provider selection has to be inert.
     */
    @Test
    fun `provider selection is inert while reauthentication is armed`() {
        val user = passwordOnlyUser("linked@example.com")
        var retryRan = false
        var captured: ReauthContentState? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                customMethodPickerLayout = { providers, onProviderSelected ->
                    Column {
                        providers.forEach { provider ->
                            Button(
                                onClick = { onProviderSelected(provider) },
                                modifier = Modifier.testTag("pick_${provider.providerId}"),
                            ) { Text(provider.providerId) }
                        }
                    }
                },
                reauthContent = { state ->
                    captured = state
                    Text("reauth_slot", modifier = Modifier.testTag("reauth_slot"))
                },
            )
        }

        composeTestRule.onNodeWithTag("pick_password").assertExists()

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { retryRan = true })
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertExists()
        assertThat(captured).isNotNull()

        composeTestRule.onAllNodesWithText(stringProvider.passwordHint).assertCountEquals(0)

        // Ungated, selecting email navigates the outer NavHost to its non-reauth email screen,
        // surfacing a password field behind the slot.
        composeTestRule.onNodeWithTag("pick_password").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(stringProvider.passwordHint).assertCountEquals(0)
        composeTestRule.onNodeWithTag("reauth_slot").assertExists()
        assertThat(retryRan).isFalse()
    }

    /**
     * Arming a second sensitive operation while the first is still pending must replace it. Value
     * equality on [AuthState.ReauthenticationRequired] made the second write equal to the current
     * one, which [kotlinx.coroutines.flow.MutableStateFlow] silently drops — so the screen kept the
     * *first* lambda and ran the wrong sensitive operation after reauthentication.
     */
    @Test
    fun `arming a second operation for the same user replaces the first`() {
        val user = passwordOnlyUser("linked@example.com")
        val ran = mutableListOf<String>()

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                }
            )
        }

        // Same user, same (absent) reason: the two states differ only in the attached operation.
        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { ran.add("first") })
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { ran.add("second") })
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()

        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Success(result = null, user = user, reauthenticatedUid = user.uid)
            )
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { ran.isNotEmpty() }
        composeTestRule.waitForIdle()

        assertThat(ran).containsExactly("second")
    }

    /**
     * Reauthentication is not a sign-in. With no operation attached the library still has to consume
     * the matched stamp and stop there — falling through published the reauthentication's
     * [com.google.firebase.auth.AuthResult] to `onSignInSuccess`, which federated providers stamp
     * and the email provider does not, so the same public callback behaved differently by provider.
     */
    @Test
    fun `a matched reauthentication with no pending operation does not report a sign-in`() {
        val user = passwordOnlyUser("linked@example.com")
        val authResult = mock(AuthResult::class.java)
        `when`(authResult.user).thenReturn(user)
        var signInSuccessCount = 0

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = { signInSuccessCount++ },
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                },
                authenticatedContent = { _, _ -> Text(text = "AUTHENTICATED") },
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.ReauthenticationRequired(user, retryOperation = null))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()

        // The federated stamp shape: a non-null AuthResult alongside the reauthenticated uid.
        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Success(
                    result = authResult,
                    user = user,
                    reauthenticatedUid = user.uid,
                )
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        assertThat(signInSuccessCount).isEqualTo(0)
        composeTestRule.onNodeWithTag("reauth_slot").assertDoesNotExist()
        composeTestRule.onNodeWithText("AUTHENTICATED").assertExists()
    }

    /**
     * The uid comparison is the whole guarantee: a stamped success for *another* account is not
     * evidence that the armed user re-proved anything, so the operation must not run and the slot
     * must stay up. Without this the comparison could be weakened to a null check unnoticed.
     */
    @Test
    fun `a stamped Success for a different uid does not run the pending operation`() {
        val armedUser = passwordOnlyUser("armed@example.com")
        val otherUser = userLinkedTo("google.com", "other@example.com")
        var retryRan = false
        var captured: ReauthContentState? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = { state ->
                    captured = state
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                }
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.ReauthenticationRequired(armedUser, retryOperation = { retryRan = true })
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()
        assertThat(armedUser.uid).isNotEqualTo(otherUser.uid)

        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Success(
                    result = null,
                    user = otherUser,
                    reauthenticatedUid = otherUser.uid,
                )
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        assertThat(retryRan).isFalse()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()
        assertThat(requireNotNull(captured).error)
            .isEqualTo(context.getString(R.string.fui_error_reauth_incomplete))
    }

    /**
     * A wrong password for an unverified account ends up here: the consumed Error resets to Idle,
     * the combine falls back to the live session, and that yields RequiresEmailVerification. It
     * navigates with `popUpTo(inclusive = true)`, which would wipe the stack under the armed slot.
     */
    @Test
    fun `RequiresEmailVerification does not navigate while reauthentication is armed`() {
        val user = passwordOnlyUser("linked@example.com")
        var retryRan = false

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                },
                authenticatedContent = { _, _ ->
                    Text(text = "AUTHENTICATED", modifier = Modifier.testTag("authenticated"))
                },
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { retryRan = true })
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.RequiresEmailVerification(user = user, email = "linked@example.com")
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("authenticated").assertDoesNotExist()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()
        assertThat(retryRan).isFalse()
    }

    /**
     * An MFA-enrolled account cannot complete reauthentication at all (a known, separate defect).
     * Unguarded, RequiresMfa pushed AuthRoute.MfaChallenge *beneath* the armed slot, which then
     * showed neither loading nor an error — a dead UI with the operation still pending.
     */
    @Test
    fun `RequiresMfa does not navigate while reauthentication is armed and latches an error`() {
        val user = passwordOnlyUser("linked@example.com")
        var retryRan = false
        var captured: ReauthContentState? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = { state ->
                    captured = state
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                },
                authenticatedContent = { _, _ ->
                    Text(text = "AUTHENTICATED", modifier = Modifier.testTag("authenticated"))
                },
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { retryRan = true })
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()

        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.RequiresMfa(mock(MultiFactorResolver::class.java)))
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()
        assertThat(retryRan).isFalse()
        val state = requireNotNull(captured)
        assertThat(state.error)
            .isEqualTo(context.getString(R.string.fui_error_reauth_mfa_unsupported))
        assertThat(state.exception).isInstanceOf(AuthException.UnknownException::class.java)
    }

    /**
     * Dismissing abandons the operation for good, and `withReauth` has already returned normally —
     * so the host has no other way to learn its sensitive operation will never run.
     */
    @Test
    fun `dismissing the reauth slot reports the flow as cancelled exactly once`() {
        val user = passwordOnlyUser("linked@example.com")
        var cancelledCount = 0
        var retryRan = false

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = { cancelledCount++ },
                reauthContent = { state ->
                    Button(
                        onClick = state.onDismiss,
                        modifier = Modifier.testTag("dismiss_reauth")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { retryRan = true })
            )
        }
        composeTestRule.waitForIdle()
        assertThat(cancelledCount).isEqualTo(0)

        composeTestRule.onNodeWithTag("dismiss_reauth").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        assertThat(cancelledCount).isEqualTo(1)
        assertThat(retryRan).isFalse()
        composeTestRule.onNodeWithTag("dismiss_reauth").assertDoesNotExist()
    }

    /**
     * Rotating while the slot shows a latched failure must not silently erase it. The message is
     * saveable and survives; the [AuthException] behind it is not, so `exception` comes back null
     * (documented on [ReauthContentState.exception]) rather than disagreeing with `error`.
     */
    @Test
    fun `a latched slot error survives Activity recreation`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        var captured: ReauthContentState? = null
        val thrown = FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "RAW-BACKEND-CODE-17")
        val expectedMessage = requireNotNull(AuthException.from(thrown, stringProvider).message)
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = { state ->
                    captured = state
                    Text(text = "SLOT_ERROR=${state.error}", modifier = Modifier.testTag("slot"))
                }
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.ReauthenticationRequired(user))
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Error(thrown)) }
        composeTestRule.waitForIdle()
        assertThat(requireNotNull(captured).error).isEqualTo(expectedMessage)

        captured = null
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("SLOT_ERROR=$expectedMessage").assertIsDisplayed()
        assertThat(requireNotNull(captured).error).isEqualTo(expectedMessage)
        assertThat(requireNotNull(captured).exception).isNull()
    }

    /**
     * Rotating part-way through the library's own email sub-flow must not bounce the user back to
     * the provider chooser: the active sub-route is saved alongside the arming.
     */
    @Test
    fun `an active email sub-flow survives Activity recreation`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                emailContent = { state ->
                    Text(
                        text = "EMAIL_SUBFLOW:${state.email}",
                        modifier = Modifier.testTag("email_subflow")
                    )
                },
                reauthContent = { state ->
                    Button(
                        onClick = { state.onProviderSelected(state.providers.first()) },
                        modifier = Modifier.testTag("pick_provider")
                    ) {
                        Text("Continue")
                    }
                }
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.ReauthenticationRequired(user))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pick_provider").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("email_subflow").assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("email_subflow").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pick_provider").assertDoesNotExist()
    }

    /**
     * The likeliest moment to rotate is right after a cancelled or failed attempt. Resetting the
     * flow to [AuthState.Idle] there would drop the arming from the process-cached [FirebaseAuthUI]
     * and lose the pending operation silently; the arming is re-emitted instead, so a recreation
     * re-derives both it and the operation, and a later genuine reauthentication still runs it.
     */
    @Test
    fun `the pending operation survives recreation after a cancelled attempt`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        var retryCount = 0
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                }
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { retryCount++ })
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Cancelled()) }
        composeTestRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()

        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                AuthState.Success(result = null, user = user, reauthenticatedUid = user.uid)
            )
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { retryCount > 0 }

        assertThat(retryCount).isEqualTo(1)
    }

    /**
     * The one arming a recreation genuinely cannot re-derive: the credential exchange in flight
     * died with the composition, so the flow still reads [AuthState.Loading] and the suspend
     * operation is gone. That must be reported, not dropped silently, and must not later be
     * consumed by an unrelated success.
     */
    @Test
    fun `an attempt interrupted by recreation is reported rather than dropped`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        var retryCount = 0
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                }
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                AuthState.ReauthenticationRequired(user, retryOperation = { retryCount++ })
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reauth_slot").assertDoesNotExist()
        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_error_reauth_interrupted))
            .assertExists()

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                AuthState.Success(result = null, user = user, reauthenticatedUid = user.uid)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        assertThat(retryCount).isEqualTo(0)
    }
}
