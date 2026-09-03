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

import com.firebase.ui.auth.abandonedReauth
import com.firebase.ui.auth.retryingReauth
import android.content.Context
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.AuthUITransitions
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.R
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.firebase.ui.auth.ui.screens.reauth.ReauthContentState
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorAssertion
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.TotpMultiFactorGenerator
import com.google.firebase.auth.TotpMultiFactorInfo
import com.google.firebase.auth.UserInfo
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
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

    private fun emailAndPhoneConfiguration(
        authUITransitions: AuthUITransitions? = null,
    ): AuthUIConfiguration = authUIConfiguration {
        context = this@FirebaseAuthScreenReauthContentStateTest.context
        transitions = authUITransitions
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
                AuthState.Reauthentication.Required(user, reason = "Confirm it is you")
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
            signedInAuthUI.updateAuthState(AuthState.Reauthentication.Required(user))
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
            signedInAuthUI.updateAuthState(AuthState.Reauthentication.Required(user))
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
                retryingReauth(user) { retryRan = true }
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
                retryingReauth(user) { retryRan = true }
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
            signedInAuthUI.updateAuthState(AuthState.Reauthentication.Required(user))
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
                retryingReauth(user) { retryRan = true }
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
     * its own [AuthState.Loading] (e.g. a cancelled phone verification retracted on dispose). None
     * of those is evidence of reauthentication, and no one-step lookback at the previous state can
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
                retryingReauth(user) { retryRan = true }
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
                retryingReauth(user) { retryCount++ }
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
     * The error dialog's recovery actions navigate the *outer* back stack to the non-reauth email
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
                retryingReauth(user) {}
            )
        }
        composeTestRule.waitForIdle()

        // The sheet opens on its method picker (two linked providers), so no password field is on
        // screen yet. The outer back stack is still on the method picker behind it.
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

        // Ungated, onRecover would navigate the outer back stack to the non-reauth email screen.
        // With both callbacks withheld the button has nothing to do, so it must not render.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON).assertDoesNotExist()
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

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON).performClick()
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
                retryingReauth(user) { retryRan = true }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertExists()
        assertThat(captured).isNotNull()

        composeTestRule.onAllNodesWithText(stringProvider.passwordHint).assertCountEquals(0)

        // Ungated, selecting email navigates the outer back stack to its non-reauth email screen,
        // surfacing a password field behind the slot.
        composeTestRule.onNodeWithTag("pick_password").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(stringProvider.passwordHint).assertCountEquals(0)
        composeTestRule.onNodeWithTag("reauth_slot").assertExists()
        assertThat(retryRan).isFalse()
    }

    /**
     * Arming a second sensitive operation while the first is still pending must replace it. Value
     * equality on [AuthState.Reauthentication.Required] made the second write equal to the current
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
                retryingReauth(user) { ran.add("first") }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                retryingReauth(user) { ran.add("second") }
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
            authUI.updateAuthState(AuthState.Reauthentication.Required(user))
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
                retryingReauth(armedUser) { retryRan = true }
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
     * resets the back stack to a single entry, which would wipe the stack under the armed slot.
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
                retryingReauth(user) { retryRan = true }
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
     * A TOTP resolver, which is the challenge shape that needs no phone verification round trip.
     */
    private fun totpResolver(resolveSignIn: Task<AuthResult>): MultiFactorResolver {
        val hint = mock(TotpMultiFactorInfo::class.java)
        `when`(hint.factorId).thenReturn(TotpMultiFactorGenerator.FACTOR_ID)
        `when`(hint.uid).thenReturn("enrollment-1")
        val resolver = mock(MultiFactorResolver::class.java)
        `when`(resolver.hints).thenReturn(listOf(hint))
        `when`(resolver.resolveSignIn(any(MultiFactorAssertion::class.java)))
            .thenReturn(resolveSignIn)
        return resolver
    }

    /**
     * Firebase requires the second factor to complete the reauthentication too, so the challenge
     * has to be presented *inside* the reauth surface — on the outer display it renders beneath
     * the modal, unreachable, and the operation stays pending forever.
     */
    @Test
    fun `an MFA challenge inside the reauth slot runs the pending operation exactly once`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        val resolver = totpResolver(Tasks.forResult(mock(AuthResult::class.java)))
        var retryCount = 0
        var challenge: MfaChallengeContentState? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                mfaChallengeContent = { state ->
                    challenge = state
                    Text(text = "MFA", modifier = Modifier.testTag("mfa_challenge"))
                },
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                },
                authenticatedContent = { _, _ -> Text(text = "AUTHENTICATED") },
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                retryingReauth(user) { retryCount++ }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.RequiresMfa(resolver, "authenticator"))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mfa_challenge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reauth_slot").assertDoesNotExist()

        composeTestRule.runOnIdle {
            requireNotNull(challenge).onVerificationCodeChange("123456")
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { requireNotNull(challenge).onVerifyClick() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { retryCount > 0 }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        assertThat(retryCount).isEqualTo(1)
    }

    /**
     * The default sheet needs the same sub-flow: its own NavDisplay, so the challenge replaces the
     * provider screen inside the modal instead of rendering under it.
     */
    @Test
    fun `an MFA challenge inside the default reauth sheet runs the pending operation exactly once`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        val resolver = totpResolver(Tasks.forResult(mock(AuthResult::class.java)))
        var retryCount = 0
        var challenge: MfaChallengeContentState? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                mfaChallengeContent = { state ->
                    challenge = state
                    Text(text = "MFA", modifier = Modifier.testTag("mfa_challenge"))
                },
                authenticatedContent = { _, _ -> Text(text = "AUTHENTICATED") },
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                retryingReauth(user) { retryCount++ }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.RequiresMfa(resolver, "authenticator"))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mfa_challenge").assertIsDisplayed()

        composeTestRule.runOnIdle {
            requireNotNull(challenge).onVerificationCodeChange("123456")
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { requireNotNull(challenge).onVerifyClick() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { retryCount > 0 }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        assertThat(retryCount).isEqualTo(1)
    }

    /**
     * Backing out of the challenge is not abandoning reauthentication: the request stays armed, so
     * the host must not be told the flow was cancelled and the operation must still be runnable.
     */
    @Test
    fun `cancelling the MFA challenge returns to provider selection with the request still armed`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        val resolver = totpResolver(Tasks.forResult(mock(AuthResult::class.java)))
        var retryCount = 0
        var cancelledCount = 0
        var challenge: MfaChallengeContentState? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = { cancelledCount++ },
                mfaChallengeContent = { state ->
                    challenge = state
                    Text(text = "MFA", modifier = Modifier.testTag("mfa_challenge"))
                },
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                },
                authenticatedContent = { _, _ -> Text(text = "AUTHENTICATED") },
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                retryingReauth(user) { retryCount++ }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.RequiresMfa(resolver, "authenticator"))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mfa_challenge").assertIsDisplayed()

        composeTestRule.runOnIdle { requireNotNull(challenge).onCancelClick() }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("mfa_challenge").assertDoesNotExist()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()
        assertThat(cancelledCount).isEqualTo(0)
        assertThat(retryCount).isEqualTo(0)

        // Still armed: a later genuine reauthentication of the same user still runs the operation.
        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                AuthState.Success(result = null, user = user, reauthenticatedUid = user.uid)
            )
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { retryCount > 0 }
        assertThat(retryCount).isEqualTo(1)
        assertThat(cancelledCount).isEqualTo(0)
    }

    /** A failed challenge is an ordinary failed attempt: it latches into the slot's error. */
    @Test
    fun `an MFA challenge failure surfaces as an attempt failure in the reauth slot`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        val resolver = totpResolver(Tasks.forException(RuntimeException("wrong code")))
        var retryCount = 0
        var cancelledCount = 0
        var captured: ReauthContentState? = null
        var challenge: MfaChallengeContentState? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = { cancelledCount++ },
                mfaChallengeContent = { state ->
                    challenge = state
                    Text(text = "MFA", modifier = Modifier.testTag("mfa_challenge"))
                },
                reauthContent = { state ->
                    captured = state
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                },
                authenticatedContent = { _, _ -> Text(text = "AUTHENTICATED") },
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                retryingReauth(user) { retryCount++ }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.RequiresMfa(resolver, "authenticator"))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mfa_challenge").assertIsDisplayed()

        composeTestRule.runOnIdle {
            requireNotNull(challenge).onVerificationCodeChange("123456")
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { requireNotNull(challenge).onVerifyClick() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { captured?.error != null }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()
        val state = requireNotNull(captured)
        assertThat(state.error).isNotNull()
        assertThat(state.exception).isInstanceOf(AuthException::class.java)
        assertThat(retryCount).isEqualTo(0)
        assertThat(cancelledCount).isEqualTo(0)
    }

    /**
     * The stamp is what proves the reauthentication, and it needs a user to name. With no current
     * user there is nothing to stamp, so the attempt must fail rather than publish a bare Success.
     */
    @Test
    fun `an MFA challenge resolved with no current user does not run the pending operation`() {
        val user = passwordOnlyUser("linked@example.com")
        val resolver = totpResolver(Tasks.forResult(mock(AuthResult::class.java)))
        var retryCount = 0
        var captured: ReauthContentState? = null
        var challenge: MfaChallengeContentState? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                mfaChallengeContent = { state ->
                    challenge = state
                    Text(text = "MFA", modifier = Modifier.testTag("mfa_challenge"))
                },
                reauthContent = { state ->
                    captured = state
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                },
                authenticatedContent = { _, _ -> Text(text = "AUTHENTICATED") },
            )
        }

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                retryingReauth(user) { retryCount++ }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.RequiresMfa(resolver, "authenticator"))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mfa_challenge").assertIsDisplayed()

        composeTestRule.runOnIdle {
            requireNotNull(challenge).onVerificationCodeChange("123456")
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { requireNotNull(challenge).onVerifyClick() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { captured?.error != null }
        composeTestRule.waitForIdle()

        assertThat(retryCount).isEqualTo(0)
        assertThat(requireNotNull(captured).exception)
            .isInstanceOf(AuthException.UserNotFoundException::class.java)
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
                retryingReauth(user) { retryRan = true }
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
     * The phase is composition-scoped, so rotating destroys it — but its value is still latched on
     * the flow, and a surfaced failure is the only report the user got, so the restored screen
     * re-arms from it rather than restarting them at provider selection with nothing said.
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
            signedInAuthUI.updateAuthState(AuthState.Reauthentication.Required(user))
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
        assertThat(requireNotNull(captured).exception)
            .isInstanceOf(AuthException.InvalidCredentialsException::class.java)
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
            signedInAuthUI.updateAuthState(AuthState.Reauthentication.Required(user))
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
                retryingReauth(user) { retryCount++ }
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
     * Recreation during an in-flight attempt. Nothing retains the caller, so what survives is the
     * request latched on the flow — enough to re-arm the same request and complete it. The attempt
     * itself does not come back: its network call died with the Activity, so the restored screen
     * restarts at provider selection rather than showing progress for nothing.
     */
    @Test
    fun `an attempt survives Activity recreation and completes the same request`() {
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
                retryingReauth(user) { retryCount++ }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_error_reauth_interrupted))
            .assertDoesNotExist()

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                AuthState.Success(result = null, user = user, reauthenticatedUid = user.uid)
            )
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { retryCount == 1 }

        assertThat(retryCount).isEqualTo(1)
        composeTestRule.onNodeWithTag("reauth_slot").assertDoesNotExist()
    }

    /**
     * Process death, unlike rotation, also takes the process-cached [FirebaseAuthUI] holding the
     * arming: the restored screen's first state comes from the persisted session, so it is an
     * [AuthState.Success] and no [AuthState.Reauthentication.Required] is ever available to
     * re-derive from. The pending operation is gone and must still be reported, not dropped.
     */
    @Test
    fun `an arming lost to process death is reported rather than dropped`() {
        val user = passwordOnlyUser("linked@example.com")
        var retryCount = 0
        // Read on every composition, so the restore below observes the replacement instance.
        var currentAuthUI = signedInAuthUI(user)
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = currentAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                }
            )
        }

        composeTestRule.runOnIdle {
            currentAuthUI.updateAuthState(
                retryingReauth(user) { retryCount++ }
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()

        // The instance cache dies with the process; what comes back knows only the session.
        composeTestRule.runOnIdle {
            FirebaseAuthUI.clearInstanceCache()
            currentAuthUI = signedInAuthUI(user)
        }
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(context.getString(R.string.fui_error_reauth_interrupted))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("reauth_slot").assertDoesNotExist()

        composeTestRule.runOnIdle {
            currentAuthUI.updateAuthState(
                AuthState.Success(result = null, user = user, reauthenticatedUid = user.uid)
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        assertThat(retryCount).isEqualTo(0)
    }

    /**
     * The mirror image, and the regression the broadened guard risks: rotation keeps the cached
     * [FirebaseAuthUI], so the arming re-derives and must not be reported as interrupted.
     */
    @Test
    fun `recreation that can re-derive the arming reports no interruption`() {
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
                retryingReauth(user) { retryCount++ }
            )
        }
        composeTestRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_error_reauth_interrupted))
            .assertDoesNotExist()

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

    /** A real restored Idle is distinguishable from collectAsState's null placeholder. */
    @Test
    fun `process death that restores a signed-out Idle reports interruption`() {
        val user = passwordOnlyUser("linked@example.com")
        var currentAuthUI = signedInAuthUI(user)
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = currentAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                }
            )
        }

        composeTestRule.runOnIdle {
            currentAuthUI.updateAuthState(AuthState.Reauthentication.Required(user))
        }
        composeTestRule.waitForIdle()

        // Signed out, so the replacement process emits a real Idle after the null UI placeholder.
        composeTestRule.runOnIdle {
            FirebaseAuthUI.clearInstanceCache()
            currentAuthUI = signedOutAuthUI()
        }
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(context.getString(R.string.fui_error_reauth_interrupted))
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("reauth_slot").assertDoesNotExist()
    }

    /**
     * The sensitive operation must run at most once. It runs on the caller's own coroutine now, so
     * neither a recreation nor a second resolution can start it again: there is no closure on the
     * state for a restored screen to claim, and a resolved request ignores being resolved.
     */
    @Test
    fun `the operation runs once however often its request is resolved`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        val runs = AtomicInteger(0)
        val restorationTester = StateRestorationTester(composeTestRule)
        val armed = retryingReauth(user) { runs.incrementAndGet() }

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

        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(armed) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                AuthState.Success(result = null, user = user, reauthenticatedUid = user.uid)
            )
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { runs.get() == 1 }
        composeTestRule.waitForIdle()

        // The request is over, so the surface comes down rather than covering the retry.
        composeTestRule.onNodeWithTag("reauth_slot").assertDoesNotExist()

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()
        assertThat(runs.get()).isEqualTo(1)

        armed.request.resolve(true)
        composeTestRule.waitForIdle()
        assertThat(runs.get()).isEqualTo(1)
    }

    /**
     * A recreation that outlived the caller: the request is still armed and still latched, but the
     * coroutine that would run the operation is gone. Presenting the sheet would take credentials
     * and then report a success for an operation that can never run, so it is reported instead.
     */
    @Test
    fun `a request whose caller is gone is reported instead of presented`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)

        composeTestRule.setContent {
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
            signedInAuthUI.updateAuthState(abandonedReauth(user))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reauth_slot").assertDoesNotExist()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithText(context.getString(R.string.fui_error_reauth_interrupted))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The latched failure is never the live [AuthState.Error], so the dialog needs its own dedupe
     * key: leaving and re-entering a sub-flow re-adds the effect that shows it, and the dialog the
     * user already dismissed would reappear over the freshly reopened sub-flow.
     */
    @Test
    fun `a dismissed attempt-failure dialog does not reappear on reopening a sub-flow`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        val thrown = FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "RAW-BACKEND-CODE-17")
        val expectedMessage = requireNotNull(AuthException.from(thrown, stringProvider).message)

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
            signedInAuthUI.updateAuthState(AuthState.Reauthentication.Required(user))
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Error(thrown)) }
        composeTestRule.waitForIdle()

        // Opening the email sub-flow replaces the slot, so the failure is surfaced as a dialog.
        composeTestRule.onNodeWithTag("pick_provider").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(expectedMessage).assertIsDisplayed()

        composeTestRule.onNodeWithText(stringProvider.dismissAction).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(stringProvider.backAction).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pick_provider").performClick()
        composeTestRule.waitForIdle()

        assertThat(
            composeTestRule.onAllNodesWithText(expectedMessage).fetchSemanticsNodes()
        ).isEmpty()
    }

    /**
     * Steps inside the reauthentication surface animate the way the flow underneath does: the
     * host's configured spec runs for them, so a step change is not a snap.
     */
    @Test
    fun `the configured transition spec runs for a step change inside the reauth surface`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        val resolver = totpResolver(Tasks.forResult(mock(AuthResult::class.java)))
        val transitions = mutableListOf<Pair<AuthRoute?, AuthRoute?>>()
        val record: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
            transitions += initialState.authRoute() to targetState.authRoute()
            fadeIn() togetherWith fadeOut()
        }
        val configuration = emailAndPhoneConfiguration(
            AuthUITransitions(transitionSpec = record, popTransitionSpec = record)
        )

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                emailContent = {
                    Text(text = "EMAIL", modifier = Modifier.testTag("reauth_email"))
                },
                mfaChallengeContent = {
                    Text(text = "MFA", modifier = Modifier.testTag("mfa_challenge"))
                },
                authenticatedContent = { _, _ -> Text(text = "AUTHENTICATED") },
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.Reauthentication.Required(user))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_email").assertIsDisplayed()

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.RequiresMfa(resolver, "authenticator"))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("mfa_challenge").assertIsDisplayed()
        assertThat(transitions).contains(AuthRoute.Email.SignIn(null) to AuthRoute.MfaChallenge)
    }

    /**
     * The challenge is a real entry, so the request leaving
     * [AuthState.Reauthentication.RequiresMfa] has to take it off the stack — by any path, not
     * only through the challenge's own callbacks. Nothing else can: a challenge with no resolver
     * renders nothing at all.
     */
    @Test
    fun `leaving the MFA challenge state returns the reauth surface to its start step`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        val resolver = totpResolver(Tasks.forResult(mock(AuthResult::class.java)))
        var retryCount = 0
        var cancelledCount = 0
        val required = retryingReauth(user) { retryCount++ }

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = { cancelledCount++ },
                mfaChallengeContent = {
                    Text(text = "MFA", modifier = Modifier.testTag("mfa_challenge"))
                },
                reauthContent = {
                    Text(text = "REAUTH", modifier = Modifier.testTag("reauth_slot"))
                },
                authenticatedContent = { _, _ -> Text(text = "AUTHENTICATED") },
            )
        }

        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(required) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(AuthState.RequiresMfa(resolver, "authenticator"))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mfa_challenge").assertIsDisplayed()

        // Straight off RequiresMfa, without the challenge's own cancel or error path running:
        // an ordinary Cancelled folds to provider selection, which is the phase leaving RequiresMfa.
        composeTestRule.runOnIdle { signedInAuthUI.updateAuthState(AuthState.Cancelled) }
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("mfa_challenge").assertDoesNotExist()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()
        assertThat(cancelledCount).isEqualTo(0)
        assertThat(retryCount).isEqualTo(0)
    }

    /**
     * The surface no longer covers the retry. The operation runs on the caller's coroutine after
     * the request ends, so the sheet comes down on the proof and the host publishes its own
     * handover progress — which is what stops a `Success` being claimed before the operation runs.
     */
    @Test
    fun `the reauth slot comes down when the credential proof lands`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        val runs = AtomicInteger(0)

        composeTestRule.setContent {
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
            signedInAuthUI.updateAuthState(retryingReauth(user) { runs.incrementAndGet() })
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_slot").assertIsDisplayed()

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                AuthState.Success(result = null, user = user, reauthenticatedUid = user.uid)
            )
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { runs.get() == 1 }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reauth_slot").assertDoesNotExist()
    }

    /** The default sheet is the same surface, and comes down on the same condition. */
    @Test
    fun `the default reauth sheet comes down when the credential proof lands`() {
        val user = passwordOnlyUser("linked@example.com")
        val signedInAuthUI = signedInAuthUI(user)
        val runs = AtomicInteger(0)

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = signedInAuthUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                emailContent = {
                    Text(text = "EMAIL", modifier = Modifier.testTag("reauth_email"))
                },
                authenticatedContent = { _, _ -> Text(text = "AUTHENTICATED") },
            )
        }

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(retryingReauth(user) { runs.incrementAndGet() })
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_email").assertIsDisplayed()

        composeTestRule.runOnIdle {
            signedInAuthUI.updateAuthState(
                AuthState.Success(result = null, user = user, reauthenticatedUid = user.uid)
            )
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { runs.get() == 1 }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("reauth_email").assertDoesNotExist()
    }

    /** A [FirebaseAuthUI] over a mocked, *signed-out* [FirebaseAuth]: `authStateFlow()` is Idle. */
    private fun signedOutAuthUI(): FirebaseAuthUI {
        val auth = mock(FirebaseAuth::class.java)
        `when`(auth.currentUser).thenReturn(null)
        `when`(auth.app).thenReturn(FirebaseApp.getInstance())
        return FirebaseAuthUI.create(FirebaseApp.getInstance(), auth)
    }
}
