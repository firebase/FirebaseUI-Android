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
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavBackStackEntry
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.AuthUITransitions
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
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
 * [FirebaseAuthScreen] is the single owner of the navigation an error recovery performs; the email
 * steps observe the same [AuthState.Error] and leave it alone.
 *
 * Every test here runs on the email-only configuration, where the flow's sign-in step is the
 * graph's start destination. That is the shape the back-stack hazards live in: a recovery that
 * destroyed the start entry makes every later reset silently do nothing, because
 * androidx.navigation ignores a `popUpTo` whose target is not on the stack.
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
    private lateinit var mockUser: FirebaseUser

    private var pressBack: (() -> Unit)? = null
    private var lastUiContext: AuthSuccessUiContext? = null

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
        val auth = mock(FirebaseAuth::class.java)
        `when`(auth.app).thenReturn(app)
        authUI = FirebaseAuthUI.create(app, auth)
        mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("recovery-user-uid")
    }

    @After
    fun tearDown() {
        pressBack = null
        lastUiContext = null
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    // Recovery moves the flow, and only from here

    @Test
    fun `no account for the address recovers to sign-up and keeps what was typed`() {
        start()
        typeSignInEmail()

        recoverFrom(AuthException.UserNotFoundException(message = "no such user"))

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()
    }

    /**
     * A recovery pushes when the step it starts from stays useful: "no account for this address"
     * leaves the sign-in form underneath, address intact, as where a mistyped one gets fixed.
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
     * A recovery must not pop the graph's start destination: the later reset's
     * `popUpTo(startDestination)` would then match nothing and leave the form under the success
     * screen for back to return to.
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
     * A recovery replaces when the step it starts from is a dead end: an address that already has
     * an account makes the sign-up form useless, so the move pops back rather than burying it.
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
        // The dead-end form is gone rather than buried.
        back()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertDoesNotExist()
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

    // The email-link recovery step, both branches

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
     * back to password sign-in rather than rendering a form the provider cannot complete.
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

    // The address the recovery carries

    /**
     * A recovery can start from a provider attempt, which never went through an email step: the
     * failure names the address, and the recovery must use it rather than open an empty form.
     */
    @Test
    fun `a recovery with nothing typed uses the address the failure names`() {
        start(configuration = emailAndPhoneConfiguration())

        // Still on the method picker, so the host has no typed address to fall back on.
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
     * When both are available the failure still wins: the typed address can be the one that
     * provoked the failure rather than the one it is about.
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

    // Activity recreation

    /**
     * The address the host tracks has to be saveable: a recovery decided after a recreation has
     * only the route argument to fall back on, and the start destination carries the empty default.
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

        // The field itself survives on its own.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()

        // Nothing is typed after the recreation, so the recovery has only the host's own record.
        recoverFrom(AuthException.UserNotFoundException(message = "no such user"))

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()
    }

    /**
     * Surviving a recreation must not mean surviving a sign-out: the next session's user must not
     * be handed the previous one's address, on the form or through a recovery.
     */
    @Test
    fun `signing out drops the address for both the form and the next recovery`() {
        start()
        typeSignInEmail()
        signIn()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()

        // What signOut publishes: Loading (not a notification, so the Idle is a real reset), Idle.
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

    // Reauthentication

    /**
     * The invariant the recovery veto rests on: while a request is armed,
     * `FirebaseAuthUI.contextualizeReauthenticationState` folds every `AuthState.Error` into
     * `AuthState.Reauthentication.AttemptFailed`, so the branch offering recovery is unreachable
     * while a reauthentication surface is up. Were that to stop, a recovery could navigate the
     * outer graph out from under the sheet with the request still armed.
     */
    @Test
    fun `an error raised while a reauth request is armed never surfaces as an error state`() {
        val passwordInfo = mock(UserInfo::class.java)
        `when`(passwordInfo.providerId).thenReturn(EmailAuthProvider.PROVIDER_ID)
        val user = mock(FirebaseUser::class.java)
        `when`(user.providerData).thenReturn(listOf(passwordInfo))
        `when`(user.email).thenReturn(TYPED_EMAIL)
        `when`(user.uid).thenReturn("reauth-user-uid")

        // A second collector, so the folded state can be read directly.
        val seen = mutableListOf<AuthState>()
        composeTestRule.setContent {
            LaunchedEffect(authUI) { authUI.authStateFlow().collect { seen += it } }
            FirebaseAuthScreen(
                configuration = emailConfiguration(),
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Reauthentication.Required(user, retryOperation = {})
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Error(AuthException.UserNotFoundException(message = "no such user"))
            )
        }
        composeTestRule.waitForIdle()

        assertThat(composeTestRule.runOnIdle { seen.lastOrNull() })
            .isInstanceOf(AuthState.Reauthentication.AttemptFailed::class.java)
        // No action button on the dialog, and the outer graph did not move behind the sheet.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.ErrorRecovery.RETRY_BUTTON)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD).assertDoesNotExist()
    }

    /**
     * Every recovery either moves the flow to an email step or starts a fresh provider sign-in,
     * neither of which may happen while reauthenticating, so the dialog offers nothing.
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

    // Every route the public API exposes is a registered destination

    /**
     * `AuthSuccessUiContext.onNavigate` takes any [AuthRoute] and hands it straight to the
     * controller, which throws for a destination nothing registered, so every value must resolve.
     */
    @Test
    fun `every AuthRoute resolves to a registered destination`() {
        // Both provider flows configured: a phone step composes the phone screen.
        start(configuration = emailAndPhoneConfiguration())
        signIn()

        val uiContext = requireNotNull(composeTestRule.runOnIdle { lastUiContext })
        val unresolved = mutableListOf<String>()
        AuthRoute.all.forEach { route ->
            composeTestRule.runOnIdle {
                try {
                    uiContext.onNavigate(route)
                } catch (e: IllegalArgumentException) {
                    unresolved += "${route.route}: ${e.message}"
                }
            }
            composeTestRule.waitForIdle()
        }

        assertThat(unresolved).isEmpty()
    }

    // Configured transitions

    /**
     * A step-to-step move is real navigation, animated by whatever the caller configured: the
     * graph asks the transition lambdas, naming the two step routes it moves between.
     */
    @Test
    fun `the configured transitions drive a step-to-step move`() {
        val observed = mutableListOf<String>()
        start(configuration = emailConfiguration(transitions = recordingTransitions(observed)))

        goToSignUp()

        val signIn = AuthRoute.Email.SignIn.routePattern
        val signUp = AuthRoute.Email.SignUp.routePattern
        assertThat(observed).contains("enter:$signIn->$signUp")
        assertThat(observed).contains("exit:$signIn->$signUp")
    }

    // Harness

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

    private fun emailAndPhoneConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(
                AuthProvider.Email(
                    isDisplayNameRequired = false,
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

    private fun recordingTransitions(into: MutableList<String>) = AuthUITransitions(
        enterTransition = { into += "enter:${label()}"; EnterTransition.None },
        exitTransition = { into += "exit:${label()}"; ExitTransition.None },
        popEnterTransition = { into += "popEnter:${label()}"; EnterTransition.None },
        popExitTransition = { into += "popExit:${label()}"; ExitTransition.None },
    )

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.label(): String =
        "${initialState.destination.route}->${targetState.destination.route}"

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
            )
        }
        composeTestRule.waitForIdle()
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
    }
}
