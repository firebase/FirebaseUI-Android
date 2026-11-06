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

import com.firebase.ui.auth.ui.method_picker.AuthMethodPicker
import com.firebase.ui.auth.ReauthScopeProbe
import com.firebase.ui.auth.retryingReauth
import android.content.Context
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.AuthUITransitions
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.firebase.ui.auth.ui.screens.email.EmailAuthMode
import com.firebase.ui.auth.ui.screens.email.EmailAuthScreen
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.TotpMultiFactorInfo
import com.google.firebase.auth.UserInfo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.SignInMethodQueryResult
import com.google.firebase.auth.FacebookAuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.firebase.ui.auth.util.EmailLinkPersistenceManager
import org.mockito.kotlin.eq
import kotlinx.coroutines.runBlocking
import androidx.credentials.CredentialManager
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [FirebaseAuthScreen] is the single owner of the navigation an error recovery performs: the email
 * steps observe the same [AuthState.Error] but deliberately leave it alone, so what the user is
 * offered cannot depend on which of the two composed first.
 *
 * Every test here runs on the **email-only** configuration, where the flow's sign-in step *is* the
 * graph's start destination. That is the shape the back-stack hazards live in: if a recovery that
 * destroyed the start entry could make a later reset — success, cancellation, a genuine idle — do
 * nothing, system back would return a signed-in user to the form they had just left.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthScreenEmailRecoveryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: DefaultAuthUIStringProvider
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser

    private var pressBack: (() -> Unit)? = null
    private var lastUiContext: AuthSuccessUiContext? = null
    private var challengeState: MfaChallengeContentState? = null

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
        mockAuth = mock(FirebaseAuth::class.java)
        `when`(mockAuth.app).thenReturn(app)
        authUI = FirebaseAuthUI.create(app, mockAuth)
        mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("recovery-user-uid")
    }

    @After
    fun tearDown() {
        pressBack = null
        lastUiContext = null
        challengeState = null
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    // =============================================================================================
    // Recovery moves the flow, and only from here
    // =============================================================================================

    @Test
    fun `no account for the address recovers to sign-up and keeps what was typed`() {
        start()
        typeSignInEmail()

        recoverFrom(AuthException.UserNotFoundException(message = "no such user"))

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()
    }

    /**
     * One half of the rule the two recoveries deliberately split on: a recovery **pushes** when the
     * step it starts from stays useful. "There is no account for this address" leaves the sign-in
     * form as exactly where a mistyped address gets fixed, so it stays underneath and back returns
     * to it — with the address intact, which is what carrying it in the route buys.
     *
     * The other half is `an address already in use recovers to the sign-in step`. Both fall out of
     * the single rule in `navigateToEmailStep`; neither is a special case there.
     */
    @Test
    fun `back from the recovery target returns to the sign-in step`() {
        start()
        typeSignInEmail()
        recoverFrom(AuthException.UserNotFoundException(message = "no such user"))

        back()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertDoesNotExist()
    }

    /**
     * The regression this whole file exists for: a recovery pops the graph's start destination, so
     * a reset that keyed off that entry would leave the success screen sitting *on top of* the
     * sign-up form instead of replacing it, and back would hand a signed-in user the form again.
     */
    @Test
    fun `back after a success cannot return to the form recovery moved to`() {
        start()
        typeSignInEmail()
        recoverFrom(AuthException.UserNotFoundException(message = "no such user"))
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertIsDisplayed()

        signIn()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()

        back()

        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertDoesNotExist()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertDoesNotExist()
    }

    /**
     * The same reset, reached the other way round: a cancellation returns the flow to its start
     * step, and repeating it must not stack anything up for back to walk into.
     */
    @Test
    fun `repeated cancellations leave a single step for back to find`() {
        start()
        goToSignUp()

        repeat(3) {
            composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Cancelled) }
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()

        back()

        // Nothing inside the flow was left underneath, so back could not move at all.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertDoesNotExist()
    }

    /**
     * The other half of the rule: a recovery **replaces** when the step it starts from is a dead
     * end. An address that already has an account makes the sign-up form useless, so returning to
     * it would be returning to a form that cannot succeed — sign-in is already beneath it, and the
     * move pops back to it rather than burying it.
     */
    @Test
    fun `an address already in use recovers to the sign-in step`() {
        start()
        typeSignInEmail()
        goToSignUp()

        recoverFrom(
            AuthException.EmailAlreadyInUseException(
                message = "already in use",
                email = TYPED_EMAIL,
            )
        )

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        // The dead-end form is gone rather than buried, so back has nothing inside the flow left
        // to return to.
        back()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertDoesNotExist()
    }

    /**
     * The reset guards ask "is the flow already at its start step?", and that question has to
     * ignore the address the key carries. A key comparison — `currentKey != startRoute.toKey()` —
     * would be strictly finer: `SignIn("user+tag@example.com") != SignIn(null)`. The guard would
     * *fire*, the reset would push a differently-keyed entry, the `SaveableStateHolder` would have
     * no state for it, and the address the user is looking at would vanish.
     *
     * That is the exact stack this test builds. `an address already in use recovers to the sign-in
     * step` leaves the stack at `[SignIn(TYPED_EMAIL)]` — one entry, carrying an address, and the
     * start step. A cancellation arriving there must change nothing.
     */
    @Test
    fun `a cancellation on the start step keeps the address the recovery carried`() {
        start()
        typeSignInEmail()
        goToSignUp()
        recoverFrom(
            AuthException.EmailAlreadyInUseException(
                message = "already in use",
                email = TYPED_EMAIL,
            )
        )
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()

        // Cancelled, and then the Idle the Cancelled branch itself publishes — both guards.
        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Cancelled) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()
    }

    /**
     * The same stack, reached the way a user actually reaches it: not by publishing
     * [AuthState.Cancelled] directly, but by the MFA challenge being raised on top of the recovered
     * sign-in step and then cancelled.
     *
     * This is what makes the `popOrNull()` in the challenge's `onCancel` load-bearing. The pop is
     * synchronous inside the click handler and the `LaunchedEffect(observedAuthState)` collector
     * runs a frame later, so by the time the `Cancelled` branch reads the top of the stack it sees
     * `SignIn(TYPED_EMAIL)` — the start step — and skips its reset. Without the pop it sees
     * [AuthRoute.MfaChallenge], which is never a start step, so the reset always fires and pushes a
     * differently-keyed `SignIn(null)`; the old entry leaves the stack, the `SaveableStateHolder`
     * drops its state, and the address the recovery carried disappears.
     */
    @Test
    fun `cancelling an MFA challenge keeps the address the recovery carried`() {
        start()
        typeSignInEmail()
        goToSignUp()
        recoverFrom(
            AuthException.EmailAlreadyInUseException(
                message = "already in use",
                email = TYPED_EMAIL,
            )
        )
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()

        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.RequiresMfa(mfaResolver())) }
        composeTestRule.waitForIdle()
        val challenge = requireNotNull(composeTestRule.runOnIdle { challengeState })
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertDoesNotExist()

        composeTestRule.runOnIdle {
            challengeState = null
            challenge.onCancelClick()
        }
        composeTestRule.waitForIdle()

        assertThat(composeTestRule.runOnIdle { challengeState }).isNull()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()
    }

    /**
     * The same guard on the `Idle` branch, reached without a `Cancelled` in front of it: a
     * sign-out publishes `Loading` and then `Idle`, and `Idle` resets through the identical
     * comparison one branch down.
     */
    @Test
    fun `an idle on the start step keeps the address the recovery carried`() {
        start()
        typeSignInEmail()
        goToSignUp()
        recoverFrom(
            AuthException.EmailAlreadyInUseException(
                message = "already in use",
                email = TYPED_EMAIL,
            )
        )
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()

        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Idle) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()
    }

    @Test
    fun `no recovery is offered for a missing account when sign-up is not offered`() {
        start(configuration = emailConfiguration(isNewAccountsAllowed = false))
        typeSignInEmail()

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Error(AuthException.UserNotFoundException(message = "no such user"))
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertExists()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON)
            .assertDoesNotExist()
    }

    // =============================================================================================
    // The email-link recovery step, both branches
    // =============================================================================================

    @Test
    fun `a suggested email-link method recovers to the email-link step when it is enabled`() {
        start(configuration = emailConfiguration(isEmailLinkSignInEnabled = true))
        typeSignInEmail()

        recoverFrom(differentSignInMethodRequired())

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.EmailLink.EMAIL_FIELD)
            .assertIsDisplayed()
    }

    /**
     * Without email-link sign-in configured there is no such step to offer, so the recovery falls
     * back to password sign-in rather than rendering a form the provider cannot complete. The
     * screen's own handler used to switch to the email-link mode with no such guard at all; that
     * handler is gone, which makes this the single deterministic outcome.
     */
    @Test
    fun `a suggested email-link method recovers to the sign-in step when it is disabled`() {
        start(configuration = emailConfiguration(isEmailLinkSignInEnabled = false))
        typeSignInEmail()
        goToSignUp()

        recoverFrom(differentSignInMethodRequired())

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.EmailLink.EMAIL_FIELD)
            .assertDoesNotExist()
    }

    // =============================================================================================
    // The address the recovery carries
    // =============================================================================================

    /**
     * A recovery can start from a *provider* attempt, which never went through an email step at
     * all: nothing was typed, and tapping a provider in the picker clears whatever a previous
     * "Continue as" had seeded. The failure itself names the address in that case, so the recovery
     * has to prefer it over the host's own record rather than landing the user on an empty form.
     */
    @Test
    fun `a recovery with nothing typed uses the address the failure names`() {
        start(configuration = emailAndPhoneConfiguration())

        // Still on the method picker: no email step has ever been composed, so the host has no
        // typed address of its own to fall back on.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertDoesNotExist()

        recoverFrom(
            AuthException.EmailAlreadyInUseException(
                message = "already in use",
                email = TYPED_EMAIL,
            )
        )

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()
    }

    /**
     * And when both are available the failure still wins. The address the user typed can be the
     * one that *provoked* the failure rather than the one it is about — an anonymous upgrade or a
     * credential link reports the address already holding the account, which is the one the
     * recovery form has to open on.
     */
    @Test
    fun `the address the failure names beats the one the host recorded`() {
        start(configuration = emailConfiguration(isEmailLinkSignInEnabled = true))
        typeSignInEmail()

        recoverFrom(
            AuthException.DifferentSignInMethodRequiredException(
                message = "use the email link",
                email = OTHER_EMAIL,
                signInMethods = listOf(EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD),
                suggestedSignInMethod = EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD,
            )
        )

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.EmailLink.EMAIL_FIELD)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(OTHER_EMAIL).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertDoesNotExist()
    }

    // =============================================================================================
    // Activity recreation
    // =============================================================================================

    /**
     * The step's own field restores itself, but the host's record of the address does not come for
     * free: a recovery decided *after* a recreation has only the route argument left to fall back
     * on, and the graph's start destination carries the empty default. The address the host tracks
     * therefore has to be saveable, or the very first recovery after a rotation lands the user on
     * an empty form.
     */
    @Test
    fun `a recovery after an Activity recreation still carries the typed address`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            CaptureBackDispatcher()
            FirebaseAuthScreen(
                configuration = emailConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
            )
        }
        composeTestRule.waitForIdle()
        typeSignInEmail()

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        // The field itself survives on its own; the point of the test is what follows.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()

        // Nothing is typed after the recreation, so the recovery has only the host's own record.
        recoverFrom(AuthException.UserNotFoundException(message = "no such user"))

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()
    }

    /**
     * The other direction of the same state. Surviving a recreation must not mean surviving a
     * sign-out: signing out publishes Loading and then a genuine Idle, which resets the flow to
     * its start step. The next session's user must not be handed the previous one's address, on
     * the form or through a recovery.
     */
    @Test
    fun `signing out drops the address for both the form and the next recovery`() {
        start()
        typeSignInEmail()
        signIn()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()

        // What FirebaseAuthUI.signOut publishes: a Loading (not a notification, so the Idle that
        // follows is a real reset) and then Idle.
        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Loading()) }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Idle) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertDoesNotExist()

        // And the host's own record went with it, so a recovery cannot carry it either.
        recoverFrom(AuthException.UserNotFoundException(message = "no such user"))

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertDoesNotExist()
    }

    // =============================================================================================
    // Reauthentication
    // =============================================================================================

    /**
     * The invariant the recovery veto above rests on: an exchange's failure never reaches the
     * public flow, so the branch offering recovery is unreachable while a surface is up. Were it
     * to, a recovery could navigate the outer graph out from under the sheet.
     */
    @Test
    fun `an error raised while a reauth request is outstanding never surfaces as an error state`() {
        val probe = ReauthScopeProbe()
        val passwordInfo = mock(UserInfo::class.java)
        `when`(passwordInfo.providerId).thenReturn(EmailAuthProvider.PROVIDER_ID)
        val user = mock(FirebaseUser::class.java)
        `when`(user.providerData).thenReturn(listOf(passwordInfo))
        `when`(user.email).thenReturn(TYPED_EMAIL)
        `when`(user.uid).thenReturn("reauth-user-uid")

        // A second collector, to prove directly that nothing from the exchange lands on it.
        val seen = mutableListOf<AuthState>()
        composeTestRule.setContent {
            LaunchedEffect(authUI) { authUI.authStateFlow().collect { seen += it } }
            FirebaseAuthScreen(
                configuration = emailConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                // One provider, so the sheet opens at the email step and the picker never composes.
                emailContent = { probe.Capture() },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            authUI.pendingReauth.value = retryingReauth(user) {}
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            probe.emit(
                AuthState.Error(AuthException.UserNotFoundException(message = "no such user"))
            )
        }
        composeTestRule.waitForIdle()

        // The exchange's failure never reaches the public flow at all.
        assertThat(composeTestRule.runOnIdle { seen.filterIsInstance<AuthState.Error>() }).isEmpty()
        // Which is what keeps the recovery out of reach: no action button on the dialog, and the
        // outer graph was not moved to a sign-up form behind the sheet.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertDoesNotExist()
    }

    /**
     * Every recovery either moves the flow to an email step, vetoed while reauthenticating, or
     * starts a fresh provider sign-in, which would sign someone in while a sensitive operation
     * is still waiting to be proved. So the dialog explains the error and offers nothing else.
     */
    @Test
    fun `reauthentication offers no recovery action`() {
        start(configuration = emailConfiguration().copy(isReauthenticationMode = true))

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Error(AuthException.UserNotFoundException(message = "no such user"))
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertExists()
        composeTestRule.onNodeWithText(stringProvider.dismissAction).assertExists()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertDoesNotExist()
    }

    // =============================================================================================
    // Every route the public API exposes is a registered destination
    // =============================================================================================

    /**
     * `AuthSuccessUiContext.onNavigate` takes any [AuthRoute] and hands it straight to the
     * controller, which throws for a destination nothing registered. Every value the sealed
     * hierarchy exposes therefore has to resolve — including the step objects of the flows whose
     * screens have not been converted yet.
     */
    @Test
    fun `every AuthRoute resolves to a registered destination`() {
        // Both provider flows configured: a phone step composes the phone screen, which needs its
        // own provider present, and the point here is the destination resolving rather than a
        // single-provider graph.
        start(configuration = emailAndPhoneConfiguration())
        signIn()

        val uiContext = requireNotNull(composeTestRule.runOnIdle { lastUiContext })
        val unresolved = mutableListOf<String>()
        allAuthRoutes.forEach { route ->
            composeTestRule.runOnIdle {
                try {
                    uiContext.onNavigate(route)
                } catch (e: IllegalArgumentException) {
                    unresolved += "$route: ${e.message}"
                }
            }
            composeTestRule.waitForIdle()
        }

        assertThat(unresolved).isEmpty()
    }

    /**
     * The precondition that makes `AuthSuccessUiContext.onNavigate` safe.
     *
     * That callback hands a **consumer-supplied** [AuthRoute] straight to `pushUnique`, which is
     * the one call site whose argument the library does not choose. `pushUnique` behaves sharply
     * when the key it is given is already *buried* on the stack: it moves that entry to the top and
     * drops everything that was above it. Here that case cannot arise, because [AuthRoute.Success]
     * is only ever reached through `resetBackStackTo`, whose postcondition is a one-entry stack —
     * nothing can be buried under a stack that holds one thing.
     *
     * Asserted rather than left to reading, because the invariant lives in a different part of the
     * screen from the function that depends on it. The flow is deliberately taken *deeper* than one
     * entry first, so a reset that stopped clearing would leave something for back to find.
     */
    @Test
    fun `the success destination has nothing beneath it, so a route pushed from it lands directly on top`() {
        start()
        typeSignInEmail()
        goToSignUp()

        signIn()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()

        // Nothing underneath: root back belongs to the host, not the display, so the signed-in
        // user stays put rather than reappearing on the form they just left.
        back()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertDoesNotExist()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertDoesNotExist()

        // So a route pushed through the public callback sits directly on Success, with exactly one
        // entry beneath it for back to return to.
        val uiContext = requireNotNull(composeTestRule.runOnIdle { lastUiContext })
        composeTestRule.runOnIdle { uiContext.onNavigate(AuthRoute.Email) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()

        back()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
    }

    // =============================================================================================
    // Configured transitions
    // =============================================================================================

    /**
     * Splitting the modes into destinations is only worth anything if a step-to-step move is real
     * navigation, animated by whatever the caller configured. The transition lambdas are the
     * observable proof: the display asks them, naming the two steps it is moving between.
     *
     * A forward move now asks **one** lambda (`transitionSpec`) for a single `ContentTransform`,
     * so the two separate enter/exit observations collapse into one. The scene names the
     * destination through `Scene<NavKey>.authRoute()`, which is also the only supported way a
     * consumer can do this — see [com.firebase.ui.auth.ui.screens.AuthRouteMetadataKey].
     */
    @Test
    fun `the configured transitions drive a step-to-step move`() {
        val observed = mutableListOf<String>()
        start(configuration = emailConfiguration(transitions = recordingTransitions(observed)))

        goToSignUp()

        val signIn = AuthRoute.Email.SignIn::class.simpleName
        val signUp = AuthRoute.Email.SignUp::class.simpleName
        assertThat(observed).contains("transition:$signIn->$signUp")
    }

    /**
     * The reverse move, which is a different lambda: `NavDisplay` takes one `ContentTransform` per
     * direction, so `popTransitionSpec` needs its own observation — a wiring that fed
     * `transitionSpec` to both directions would satisfy the test above and still be wrong.
     *
     * `predictivePopTransitionSpec` is deliberately **not** asserted here. It is only consulted
     * while a predictive-back gesture is in progress, which needs a real
     * `OnBackPressedDispatcher.dispatchOnBackStarted`/`OnBackProgressed` sequence from a platform
     * back gesture; Robolectric's dispatcher delivers only the completed `onBackPressed` that
     * [back] triggers, which is the plain pop path. It is new public API with no unit coverage,
     * and this comment is the honest record of that rather than a test that appears to cover it.
     */
    @Test
    fun `the configured pop transition drives a move back`() {
        val observed = mutableListOf<String>()
        start(configuration = emailConfiguration(transitions = recordingTransitions(observed)))

        goToSignUp()
        observed.clear()
        back()

        val signIn = AuthRoute.Email.SignIn::class.simpleName
        val signUp = AuthRoute.Email.SignUp::class.simpleName
        assertThat(observed).contains("popTransition:$signUp->$signIn")
        // The forward spec is not what a pop asks.
        assertThat(observed.none { it.startsWith("transition:") }).isTrue()
    }

    /**
     * The public accessor the reshaped [AuthUITransitions] depends on. Navigation 3 exposes no
     * supported way to ask a `Scene` which destination it shows — `NavEntry` has no public key
     * accessor, `contentKey` is opaque and re-wrapped mid-life, and `Scene.key` is stale on the
     * target side mid-transition — so the library stamps the key into entry metadata itself. If
     * that stamping were dropped, a consumer's per-destination animation would silently degrade to
     * "null on both sides" rather than fail to compile.
     */
    @Test
    fun `a scene reports the AuthRoute it is showing on both sides of a move`() {
        val observed = mutableListOf<String>()
        start(configuration = emailConfiguration(transitions = recordingTransitions(observed)))

        goToSignUp()

        // `none {}` is vacuously true on an empty list, so the lambdas having fired at all is half
        // the assertion: drop the metadata stamping *and* the transitions and this would still pass.
        assertThat(observed).isNotEmpty()
        assertThat(observed.none { it.contains("null") }).isTrue()
    }

    // =============================================================================================
    // The recoveries that write something through for the step they move to
    // =============================================================================================

    /**
     * Three recoveries do more than navigate: they hand the step they move to a value the failure
     * carried — a credential to link, or the link to complete. The write and the move sit in the
     * same lambda, so the step has to be *reached* for the write to be observable at all, and the
     * order of the two is what makes it so.
     *
     * This one starts from the method picker rather than the sign-in step, which is where the
     * failure actually comes from: an anonymous upgrade or a provider attempt discovers an account
     * already holding the address, and nothing has been typed into an email form yet. The address
     * therefore has to come off the failure.
     */
    @Test
    fun `an account that needs linking recovers to the sign-in step with the address the failure names`() {
        start(configuration = emailAndPhoneConfiguration())

        recoverFrom(accountLinkingRequired(mock(AuthCredential::class.java)))

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(OTHER_EMAIL).assertIsDisplayed()
    }

    /**
     * The recovery is gated on an Email provider being configured, not on sign-up being offered:
     * linking signs in to an account that already exists, so an app that turns off new accounts
     * still needs the step.
     */
    @Test
    fun `an account that needs linking still recovers to sign-in when sign-up is not offered`() {
        start(configuration = emailAndPhoneConfiguration(isNewAccountsAllowed = false))

        recoverFrom(accountLinkingRequired(mock(AuthCredential::class.java)))

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(OTHER_EMAIL).assertIsDisplayed()
    }

    /**
     * firebase/FirebaseUI-Android#2550: a provider sign-in hit an existing account, and the
     * dialog's recovery opened an email step the app never configured, which crashed. Without an
     * Email provider there is no step to offer, so the dialog offers none.
     */
    @Test
    fun `no recovery is offered for an account that needs linking when no Email provider is configured`() {
        start(configuration = noEmailConfiguration())

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Error(accountLinkingRequired(mock(AuthCredential::class.java)))
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertExists()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertDoesNotExist()
    }

    /**
     * The same crash reached another way: a Google sign-in collision arrives as
     * `EmailAlreadyInUseException` through `AuthException.from`, and its recovery is the same
     * email step the configuration cannot show.
     */
    @Test
    fun `no recovery is offered for an address already in use when no Email provider is configured`() {
        start(configuration = noEmailConfiguration())

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

        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertExists()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertDoesNotExist()
    }

    /**
     * `EmailAuthScreen` is public, so a host can route to it with a configuration that has no
     * Email provider. Rather than throwing, or leaving the user on an empty step, it reports the
     * misconfiguration and asks the host to leave.
     */
    @Test
    fun `the email screen reports a misconfiguration and leaves without an Email provider`() {
        val errors = mutableListOf<AuthException>()
        var cancelled = 0
        composeTestRule.setContent {
            CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
                EmailAuthScreen(
                    context = applicationContext,
                    configuration = noEmailConfiguration(),
                    authUI = authUI,
                    onSuccess = {},
                    onError = { errors += it },
                    onCancel = { cancelled++ },
                    mode = EmailAuthMode.SignIn,
                    onNavigateToMode = { _, _ -> },
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertDoesNotExist()
        assertThat(errors).hasSize(1)
        assertThat(errors.single()).isInstanceOf(AuthException.MisconfigurationException::class.java)
        assertThat(cancelled).isEqualTo(1)
    }

    /**
     * firebase/FirebaseUI-Android#2550, the half the crash guard left: a Facebook sign-in hit an
     * account Google created. With Google the only other provider that can hold the account, the
     * recovery opens it, and the Facebook credential is linked to the account the user signs in
     * to before the flow reports success. The copy of the Facebook credential the collision saved
     * for a later email-link sign-in is then forgotten, so that sign-in cannot try to link it again.
     */
    @Test
    fun `an account that needs linking opens the lone other provider and links the credential`() {
        runBlocking {
            EmailLinkPersistenceManager.default.saveEmail(applicationContext, OTHER_EMAIL, "session", null)
            EmailLinkPersistenceManager.default.saveCredentialForLinking(
                applicationContext,
                providerType = "facebook.com",
                idToken = null,
                accessToken = "facebook-access-token",
            )
        }
        val facebookCredential = facebookCredential()
        val googleCredential = mock(AuthCredential::class.java)
        val credentialManager = mock(AuthProvider.Google.CredentialManagerProvider::class.java)
        runBlocking {
            `when`(
                credentialManager.getGoogleCredential(
                    context = any(),
                    credentialManager = any<CredentialManager>(),
                    serverClientId = eq(GOOGLE_CLIENT_ID),
                    filterByAuthorizedAccounts = any(),
                    autoSelectEnabled = any(),
                )
            ).thenReturn(
                AuthProvider.Google.GoogleSignInResult(
                    credential = googleCredential,
                    idToken = "google-id-token",
                    displayName = null,
                    photoUrl = null,
                )
            )
        }
        authUI.testCredentialManagerProvider = credentialManager
        val owner = accountOwner()
        `when`(mockAuth.signInWithCredential(googleCredential))
            .thenReturn(Tasks.forResult(owner.signIn))
        start(configuration = googleAndAnonymousConfiguration())

        recoverFrom(accountLinkingRequired(facebookCredential))

        verify(owner.user).linkWithCredential(facebookCredential)
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
        val saved = runBlocking {
            EmailLinkPersistenceManager.default.retrieveSessionRecord(applicationContext)
        }
        assertThat(saved?.email).isEqualTo(OTHER_EMAIL)
        assertThat(saved?.credentialForLinking).isNull()
    }

    /**
     * Without the opt-in lookup nothing says which provider owns the address, so with several
     * candidates the recovery offers the method picker. Whichever the user then signs in with
     * carries the credential: here the email step, reached from the picker rather than from the
     * recovery itself.
     */
    @Test
    fun `with several other providers the recovery offers the picker and the chosen sign-in links`() {
        val facebookCredential = facebookCredential()
        val owner = accountOwner()
        `when`(mockAuth.signInWithEmailAndPassword(OTHER_EMAIL, PASSWORD))
            .thenReturn(Tasks.forResult(owner.signIn))
        start(configuration = emailAndGoogleConfiguration())

        recoverFrom(accountLinkingRequired(facebookCredential))

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertDoesNotExist()

        composeTestRule.onNodeWithText(stringProvider.signInWithEmail).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD)
            .performTextInput(OTHER_EMAIL)
        signInWithPassword()

        verify(owner.user).linkWithCredential(facebookCredential)
    }

    /**
     * Upgrading an anonymous user is a collision with the user already signed in, which only the
     * email step's own handling resolves; the provider routing is for a signed-out flow.
     */
    @Test
    fun `an anonymous upgrade that needs linking still recovers to the email step`() {
        start(configuration = emailAndGoogleConfiguration())
        val anonymous = mock(FirebaseUser::class.java)
        `when`(anonymous.isAnonymous).thenReturn(true)
        `when`(mockAuth.currentUser).thenReturn(anonymous)

        recoverFrom(accountLinkingRequired(facebookCredential()))

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(OTHER_EMAIL).assertIsDisplayed()
    }

    /**
     * "Sign up" on the recovery's own email step, with the address already taken, raises the
     * linking error again but with no credential of its own. That must not drop the one pending:
     * signing in afterwards still links it.
     */
    @Test
    fun `a linking error with no credential keeps the link already pending`() {
        val facebookCredential = facebookCredential()
        val owner = accountOwner()
        `when`(mockAuth.signInWithEmailAndPassword(OTHER_EMAIL, PASSWORD))
            .thenReturn(Tasks.forResult(owner.signIn))
        start(configuration = emailAndPhoneConfiguration())
        recoverFrom(accountLinkingRequired(facebookCredential))

        recoverFrom(
            AuthException.AccountLinkingRequiredException(
                message = "an account already exists with this address",
                email = OTHER_EMAIL,
                credential = null,
            )
        )
        signInWithPassword()

        verify(owner.user).linkWithCredential(facebookCredential)
    }

    /** Without the address there is nothing to hold the owner's account to. */
    @Test
    fun `no provider recovery is offered when the collision names no address`() {
        start(configuration = emailAndGoogleConfiguration())

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Error(
                    AuthException.AccountLinkingRequiredException(
                        message = "an account already exists",
                        email = null,
                        credential = facebookCredential(),
                    )
                )
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertExists()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON)
            .assertDoesNotExist()
    }

    /**
     * The owner's sign-in opens UI outside the app, and the Activity can be recreated meanwhile.
     * The pending link has to come back with it, or the owner signs in and nothing is linked.
     */
    @Test
    fun `the pending link survives an Activity recreation`() {
        // A real credential: saving the state parcels it, which a mock cannot do.
        val facebookCredential = FacebookAuthProvider.getCredential("facebook-access-token")
        val owner = accountOwner()
        `when`(mockAuth.signInWithEmailAndPassword(OTHER_EMAIL, PASSWORD))
            .thenReturn(Tasks.forResult(owner.signIn))
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            FirebaseAuthScreen(
                configuration = emailAndPhoneConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
            )
        }
        composeTestRule.waitForIdle()
        recoverFrom(accountLinkingRequired(facebookCredential))

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()
        signInWithPassword()

        // Parcelled and restored, so an equal credential rather than the same instance.
        verify(owner.user).linkWithCredential(
            org.mockito.kotlin.argThat { this is FacebookAuthCredential && provider == "facebook.com" }
        )
    }

    /**
     * The opt-in lookup is a network call. Until it answers, the picker underneath must not take
     * another tap, or a second sign-in would race the one the lookup is about to start.
     */
    @Test
    fun `the owner lookup blocks the screen until it answers`() {
        val lookup = TaskCompletionSource<SignInMethodQueryResult>()
        `when`(mockAuth.fetchSignInMethodsForEmail(OTHER_EMAIL)).thenReturn(lookup.task)
        start(configuration = emailAndGoogleConfiguration(legacyLookup = true))

        recoverFrom(accountLinkingRequired(facebookCredential()))

        composeTestRule.onNodeWithText(stringProvider.progressDialogLoading).assertIsDisplayed()

        val methods = mock(SignInMethodQueryResult::class.java)
        `when`(methods.signInMethods).thenReturn(listOf(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD))
        composeTestRule.runOnIdle { lookup.setResult(methods) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.progressDialogLoading).assertDoesNotExist()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
    }

    /** Nothing else can hold the account, so there is no sign-in to send the user to. */
    @Test
    fun `no recovery is offered when the colliding provider is the only one that can hold the account`() {
        start(configuration = githubAndPhoneConfiguration())
        val githubCredential = mock(AuthCredential::class.java)
        `when`(githubCredential.provider).thenReturn("github.com")

        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.Error(accountLinkingRequired(githubCredential)))
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.errorDialogTitle).assertExists()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON)
            .assertDoesNotExist()
    }

    /**
     * The point of the recovery, and the half a landing assertion cannot see: the credential the
     * failure carried has to survive the move and be linked once the password sign-in it asked for
     * succeeds. `EmailAuthScreen` reads it once, on the composition of the step it is given, so a
     * recovery that navigated before writing — or that reused the step it was already on — would
     * land the user on the right form and quietly drop the account they were linking.
     */
    @Test
    fun `signing in after an account-linking recovery links the credential the failure carried`() {
        val credential = mock(AuthCredential::class.java)
        val signedInUser = mock(FirebaseUser::class.java)
        // The account the recovery named: the link is held to its address.
        `when`(signedInUser.email).thenReturn(OTHER_EMAIL)
        val signInResult = mock(AuthResult::class.java)
        `when`(signInResult.user).thenReturn(signedInUser)
        `when`(mockAuth.signInWithEmailAndPassword(OTHER_EMAIL, PASSWORD))
            .thenReturn(Tasks.forResult(signInResult))
        `when`(signedInUser.linkWithCredential(credential))
            .thenReturn(Tasks.forResult(mock(AuthResult::class.java)))
        start(configuration = emailAndPhoneConfiguration())

        recoverFrom(accountLinkingRequired(credential))
        signInWithPassword()

        verify(signedInUser).linkWithCredential(credential)
    }

    /**
     * The email link opened on a device that never asked for it: the session data naming the
     * address is on the *other* device, so the only way forward is a form to type it into. The
     * link itself has nowhere to live but the host, which hands it to the step it moves to.
     */
    @Test
    fun `a prompt for the email address recovers to the email-link step`() {
        start(configuration = emailConfiguration(isEmailLinkSignInEnabled = true))
        typeSignInEmail()

        recoverFrom(AuthException.EmailLinkPromptForEmailException(emailLink = EMAIL_LINK))

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.EmailLink.EMAIL_FIELD)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()
    }

    /**
     * The same fallback the suggested-method recovery has: with email-link sign-in unconfigured
     * there is no such step to offer, so the recovery lands on password sign-in rather than a form
     * the provider cannot complete. Sign-up first, so the move is a real one rather than a
     * recovery that was already where it wanted to be.
     */
    @Test
    fun `a prompt for the email address recovers to the sign-in step when email-link sign-in is disabled`() {
        start(configuration = emailConfiguration(isEmailLinkSignInEnabled = false))
        typeSignInEmail()
        goToSignUp()

        recoverFrom(AuthException.EmailLinkPromptForEmailException(emailLink = EMAIL_LINK))

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.EmailLink.EMAIL_FIELD)
            .assertDoesNotExist()
    }

    /**
     * The write-through, asserted the only way it is observable from outside: with a link in hand
     * the step *completes* a sign-in rather than sending a fresh link, and only the completing call
     * validates the link. So `isSignInWithEmailLink` having been asked about this exact link is
     * both halves of the assertion — the link arrived, and it arrived where it changes what the
     * button does.
     */
    @Test
    fun `the step a prompt recovery moved to signs in with the link the failure carried`() {
        start(configuration = emailConfiguration(isEmailLinkSignInEnabled = true))
        typeSignInEmail()
        recoverFrom(AuthException.EmailLinkPromptForEmailException(emailLink = EMAIL_LINK))

        sendEmailLink()

        verify(mockAuth).isSignInWithEmailLink(EMAIL_LINK)
    }

    /**
     * The third of the write-through recoveries, and a separate branch from the prompt above
     * despite doing the same two things: this failure carries a `providerName` alongside its link,
     * so the branch has something else to pick up by mistake.
     */
    @Test
    fun `a cross-device linking failure recovers to the email-link step`() {
        start(configuration = emailConfiguration(isEmailLinkSignInEnabled = true))
        typeSignInEmail()

        recoverFrom(
            AuthException.EmailLinkCrossDeviceLinkingException(
                providerName = "google.com",
                emailLink = EMAIL_LINK,
            )
        )

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.EmailLink.EMAIL_FIELD)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()
    }

    @Test
    fun `the step a cross-device recovery moved to signs in with the link the failure carried`() {
        start(configuration = emailConfiguration(isEmailLinkSignInEnabled = true))
        typeSignInEmail()
        recoverFrom(
            AuthException.EmailLinkCrossDeviceLinkingException(
                providerName = "google.com",
                emailLink = EMAIL_LINK,
            )
        )

        sendEmailLink()

        verify(mockAuth).isSignInWithEmailLink(EMAIL_LINK)
    }

    // =============================================================================================
    // Harness
    // =============================================================================================

    private fun emailConfiguration(
        isEmailLinkSignInEnabled: Boolean = false,
        isNewAccountsAllowed: Boolean = true,
        transitions: AuthUITransitions? = null,
    ): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(
                AuthProvider.Email(
                    isDisplayNameRequired = false,
                    isEmailLinkSignInEnabled = isEmailLinkSignInEnabled,
                    isNewAccountsAllowed = isNewAccountsAllowed,
                    emailLinkActionCodeSettings = if (isEmailLinkSignInEnabled) {
                        ActionCodeSettings.newBuilder()
                            .setUrl("https://example.com")
                            .setHandleCodeInApp(true)
                            .setAndroidPackageName("com.test", true, null)
                            .build()
                    } else {
                        null
                    },
                    passwordValidationRules = emptyList()
                )
            )
        }
        isCredentialManagerEnabled = false
        this.transitions = transitions
    }

    private fun emailAndPhoneConfiguration(
        isNewAccountsAllowed: Boolean = true,
    ): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(
                AuthProvider.Email(
                    isDisplayNameRequired = false,
                    isNewAccountsAllowed = isNewAccountsAllowed,
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

    /**
     * Two providers and neither is Email, so the flow opens on the method picker: where a
     * provider sign-in such as Facebook raises its account-linking failure.
     */
    private fun noEmailConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(AuthProvider.Anonymous)
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

    private fun recordingTransitions(into: MutableList<String>) = AuthUITransitions(
        transitionSpec = {
            into += "transition:${label()}"
            EnterTransition.None togetherWith ExitTransition.None
        },
        popTransitionSpec = {
            into += "popTransition:${label()}"
            EnterTransition.None togetherWith ExitTransition.None
        },
        predictivePopTransitionSpec = { _ ->
            into += "predictivePop:${label()}"
            EnterTransition.None togetherWith ExitTransition.None
        },
    )

    /**
     * The transition scope is over [Scene], which carries no route of its own, so the label comes
     * from `Scene.authRoute()`.
     */
    private fun AnimatedContentTransitionScope<Scene<NavKey>>.label(): String =
        "${initialState.authRoute()?.let { it::class.simpleName }}->" +
                "${targetState.authRoute()?.let { it::class.simpleName }}"

    /** A credential from a Facebook sign-in, which is what Firebase does not trust an address from. */
    private fun facebookCredential(): AuthCredential = mock(AuthCredential::class.java).also {
        `when`(it.provider).thenReturn("facebook.com")
    }

    private class AccountOwner(val user: FirebaseUser, val signIn: AuthResult)

    /** The existing account at [OTHER_EMAIL], which accepts the pending credential. */
    private fun accountOwner(): AccountOwner {
        val user = mock(FirebaseUser::class.java)
        `when`(user.uid).thenReturn("owner-uid")
        `when`(user.email).thenReturn(OTHER_EMAIL)
        `when`(user.isEmailVerified).thenReturn(true)
        val signIn = mock(AuthResult::class.java)
        `when`(signIn.user).thenReturn(user)
        val linked = mock(AuthResult::class.java)
        `when`(linked.user).thenReturn(user)
        `when`(user.linkWithCredential(any())).thenReturn(Tasks.forResult(linked))
        return AccountOwner(user, signIn)
    }

    private fun googleProvider() = AuthProvider.Google(
        scopes = emptyList(),
        serverClientId = GOOGLE_CLIENT_ID,
    )

    /** Google is the only provider besides one that cannot hold an address. */
    private fun googleAndAnonymousConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(googleProvider())
            provider(AuthProvider.Anonymous)
        }
        isCredentialManagerEnabled = false
    }

    private fun emailAndGoogleConfiguration(
        legacyLookup: Boolean = false,
    ): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(
                AuthProvider.Email(
                    isDisplayNameRequired = false,
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                )
            )
            provider(googleProvider())
        }
        isCredentialManagerEnabled = false
        legacyFetchSignInWithEmail = legacyLookup
    }

    private fun githubAndPhoneConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(AuthProvider.Github(customParameters = emptyMap()))
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

    private fun accountLinkingRequired(credential: AuthCredential) =
        AuthException.AccountLinkingRequiredException(
            message = "an account already exists with this address",
            email = OTHER_EMAIL,
            credential = credential,
        )

    private fun differentSignInMethodRequired() =
        AuthException.DifferentSignInMethodRequiredException(
            message = "use the email link",
            email = TYPED_EMAIL,
            signInMethods = listOf(EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD),
            suggestedSignInMethod = EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD,
        )

    private fun start(configuration: AuthUIConfiguration = emailConfiguration()) {
        composeTestRule.setContent {
            CaptureBackDispatcher()
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                authenticatedContent = { _, uiContext ->
                    SideEffect { lastUiContext = uiContext }
                    Text("authenticated", modifier = Modifier.testTag(AUTHENTICATED_TAG))
                },
                mfaChallengeContent = { state -> challengeState = state },
            )
        }
        composeTestRule.waitForIdle()
    }

    /** A resolver with a single TOTP hint — enough for the challenge step to compose. */
    private fun mfaResolver(): MultiFactorResolver {
        val session = mock(MultiFactorSession::class.java)
        val hint = mock(TotpMultiFactorInfo::class.java)
        `when`(hint.factorId).thenReturn("totp")
        `when`(hint.uid).thenReturn("totp-factor-uid")
        val resolver = mock(MultiFactorResolver::class.java)
        `when`(resolver.session).thenReturn(session)
        `when`(resolver.hints).thenReturn(listOf<MultiFactorInfo>(hint))
        return resolver
    }

    @Composable
    private fun CaptureBackDispatcher() {
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        SideEffect { pressBack = dispatcher?.let { { it.onBackPressed() } } }
    }

    private fun typeSignInEmail() {
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD)
            .performTextInput(TYPED_EMAIL)
        composeTestRule.waitForIdle()
    }

    /** Fills in the password the sign-in step is missing and submits it. */
    private fun signInWithPassword() {
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.PASSWORD_FIELD)
            .performTextInput(PASSWORD)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.SIGN_IN_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    /** Submits the email-link step the address it was opened on. */
    private fun sendEmailLink() {
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.EmailLink.SEND_LINK_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun goToSignUp() {
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.SIGN_UP_BUTTON).performClick()
        composeTestRule.waitForIdle()
    }

    /** Raises [exception] and takes the recovery action its error dialog offers. */
    private fun recoverFrom(exception: AuthException) {
        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.Error(exception)) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()
    }

    private fun signIn() {
        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.Success(result = null, user = mockUser))
        }
        composeTestRule.waitForIdle()
    }

    private fun back() {
        composeTestRule.runOnUiThread { requireNotNull(pressBack).invoke() }
        composeTestRule.waitForIdle()
    }

    private companion object {
        const val TYPED_EMAIL = "user+tag@example.com"
        const val OTHER_EMAIL = "someone.else@example.com"
        const val AUTHENTICATED_TAG = "recovery_test_authenticated"
        const val PASSWORD = "correct-horse-battery"
        const val GOOGLE_CLIENT_ID = "test-client-id"
        const val EMAIL_LINK = "https://example.com/finish?oobCode=abc&mode=signIn"
    }
}
