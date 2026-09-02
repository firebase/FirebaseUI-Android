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
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.get
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.ui.components.LocalTopLevelDialogController
import com.firebase.ui.auth.ui.components.rememberTopLevelDialogController
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.EMAIL_ARG
import com.firebase.ui.auth.ui.screens.resetBackStackTo
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
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the one-destination-per-[EmailAuthMode] email graph.
 *
 * The unit under test is [emailAuthDestinations] — the same graph extension both
 * [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen] and the reauthentication sheet install —
 * hosted here in a bare [NavHost] so the back stack, its depth and the arguments it carries can
 * be read directly. The sign-in step is this graph's start destination, the shape a
 * single-provider email configuration produces and the one every back-stack hazard turns on.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class EmailAuthRouteNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var stringProvider: DefaultAuthUIStringProvider

    private var navController: NavHostController? = null
    private var lastState: EmailAuthContentState? = null
    private var pressBack: (() -> Unit)? = null

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
    }

    @After
    fun tearDown() {
        navController = null
        lastState = null
        pressBack = null
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    // User-initiated switches

    @Test
    fun `going to sign-up pushes the sign-up destination carrying the typed address`() {
        start()
        typeEmail(TYPED_EMAIL)

        goTo(EmailAuthMode.SignUp)

        assertThat(currentRoute()).isEqualTo(AuthRoute.Email.SignUp.routePattern)
        assertThat(currentEmailArgument()).isEqualTo(TYPED_EMAIL)
        assertThat(renderedEmail()).isEqualTo(TYPED_EMAIL)
        assertThat(backStackRoutes()).containsExactly(
            AuthRoute.Email.SignIn.routePattern,
            AuthRoute.Email.SignUp.routePattern,
        ).inOrder()
    }

    @Test
    fun `the typed address survives every mode switch`() {
        start()
        typeEmail(TYPED_EMAIL)

        listOf(
            EmailAuthMode.SignUp,
            EmailAuthMode.ResetPassword,
            EmailAuthMode.EmailLinkSignIn,
            EmailAuthMode.SignIn,
        ).forEach { target ->
            goTo(target)

            assertThat(currentRoute()).isEqualTo(AuthRoute.Email.stepFor(target).routePattern)
            assertThat(currentEmailArgument()).isEqualTo(TYPED_EMAIL)
            assertThat(renderedEmail()).isEqualTo(TYPED_EMAIL)
        }
    }

    @Test
    fun `the exposed mode always matches the active destination`() {
        start()

        EmailAuthMode.entries.forEach { target ->
            goTo(target)

            assertThat(currentRoute()).isEqualTo(AuthRoute.Email.stepFor(target).routePattern)
            assertThat(renderedMode()).isEqualTo(target)
        }
    }

    @Test
    fun `system back walks the modes in reverse order of the pushes`() {
        start()
        typeEmail(TYPED_EMAIL)
        goTo(EmailAuthMode.SignUp)
        goTo(EmailAuthMode.ResetPassword)

        back()
        assertThat(currentRoute()).isEqualTo(AuthRoute.Email.SignUp.routePattern)

        back()
        assertThat(currentRoute()).isEqualTo(AuthRoute.Email.SignIn.routePattern)
    }

    /**
     * `launchSingleTop` alone only de-duplicates a target already on top, so toggling between two
     * steps must not add an entry per tap.
     */
    @Test
    fun `toggling sign-in and sign-up keeps the back stack bounded`() {
        start()
        typeEmail(TYPED_EMAIL)

        repeat(6) {
            goTo(EmailAuthMode.SignUp)
            assertThat(backStackRoutes()).containsExactly(
                AuthRoute.Email.SignIn.routePattern,
                AuthRoute.Email.SignUp.routePattern,
            ).inOrder()

            goTo(EmailAuthMode.SignIn)
            assertThat(backStackRoutes())
                .containsExactly(AuthRoute.Email.SignIn.routePattern)
        }
    }

    /**
     * Switching to a step already beneath the current one pops back to it instead of stacking a
     * second copy, and the address just typed wins over the one that entry was created with.
     */
    @Test
    fun `switching back to a step already on the stack keeps the newest address`() {
        start()
        goTo(EmailAuthMode.SignUp)
        typeEmail(TYPED_EMAIL)

        goTo(EmailAuthMode.SignIn)

        assertThat(backStackRoutes()).containsExactly(AuthRoute.Email.SignIn.routePattern)
        assertThat(currentEmailArgument()).isEqualTo(TYPED_EMAIL)
        assertThat(renderedEmail()).isEqualTo(TYPED_EMAIL)
    }

    /** The other half of re-pushing: a switch clears the password, a pop-back would restore it. */
    @Test
    fun `a switch clears the password even when the target step was already on the stack`() {
        start()
        typeEmail(TYPED_EMAIL)
        composeTestRule.runOnIdle { requireNotNull(lastState).onPasswordChange("hunter2") }
        composeTestRule.waitForIdle()

        goTo(EmailAuthMode.SignUp)
        goTo(EmailAuthMode.SignIn)

        assertThat(composeTestRule.runOnIdle { lastState?.password }).isEmpty()
    }

    // Argument encoding

    /**
     * The address travels in the route's query string, so every character a local part or domain
     * may legally hold has to survive `Uri.encode` on the way out and the decode on the way in.
     */
    @Test
    fun `awkward addresses round-trip through the route argument unchanged`() {
        start()

        listOf(
            "user+tag@example.com",
            "user name@example.com",
            "user#hash@example.com",
            "user?query=1@example.com",
            "user&more@example.com",
            "ada@königsberg.example",
            "用户@例え.jp",
        ).forEach { address ->
            typeEmail(address)
            goTo(EmailAuthMode.SignUp)

            assertThat(currentEmailArgument()).isEqualTo(address)
            assertThat(renderedEmail()).isEqualTo(address)

            goTo(EmailAuthMode.SignIn)
        }
    }

    // Errors belong to the host

    /**
     * `AuthState.Error` is one event on a shared flow. A hosted step neither moves the flow nor
     * raises the dialog, so it cannot race the host's own recovery actions.
     */
    @Test
    fun `a hosted step neither navigates nor raises a dialog on an error`() {
        start()
        typeEmail(TYPED_EMAIL)

        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.Error(AuthException.UserNotFoundException(message = "no such user"))
            )
        }
        composeTestRule.waitForIdle()

        assertThat(backStackRoutes()).containsExactly(AuthRoute.Email.SignIn.routePattern)
        assertThat(renderedEmail()).isEqualTo(TYPED_EMAIL)
        composeTestRule.onAllNodesWithText(stringProvider.errorDialogTitle).assertCountEquals(0)
    }

    // Steps the configuration does not offer

    @Test
    fun `reauthentication cannot switch to sign-up`() {
        start(configuration = reauthConfiguration())
        typeEmail(TYPED_EMAIL)

        goTo(EmailAuthMode.SignUp)

        assertThat(currentRoute()).isEqualTo(AuthRoute.Email.SignIn.routePattern)
    }

    @Test
    fun `reauthentication cannot switch to email-link sign-in`() {
        start(configuration = reauthConfiguration())
        typeEmail(TYPED_EMAIL)

        goTo(EmailAuthMode.EmailLinkSignIn)

        assertThat(currentRoute()).isEqualTo(AuthRoute.Email.SignIn.routePattern)
    }

    @Test
    fun `a provider without email-link sign-in cannot switch to it`() {
        start(configuration = emailConfiguration(isEmailLinkSignInEnabled = false))
        typeEmail(TYPED_EMAIL)

        goTo(EmailAuthMode.EmailLinkSignIn)

        assertThat(currentRoute()).isEqualTo(AuthRoute.Email.SignIn.routePattern)
    }

    /**
     * `AuthSuccessUiContext.onNavigate` takes any [AuthRoute], so a host — or a restored back
     * stack — can reach a step the configuration switched off. The bounce keeps it unreachable,
     * and keeps the address.
     */
    @Test
    fun `the sign-up destination bounces to sign-in when account creation is not offered`() {
        start(configuration = reauthConfiguration())

        navigateDirectlyTo(AuthRoute.Email.SignUp)

        assertThat(currentRoute()).isEqualTo(AuthRoute.Email.SignIn.routePattern)
        assertThat(currentEmailArgument()).isEqualTo(TYPED_EMAIL)
        // Gone, not left underneath, where back would return to it and bounce again.
        assertThat(backStackRoutes()).containsExactly(AuthRoute.Email.SignIn.routePattern)
    }

    /** The same exposure, and now the same guard, for a provider with email-link disabled. */
    @Test
    fun `the email-link destination bounces to sign-in when the provider disables it`() {
        start(configuration = emailConfiguration(isEmailLinkSignInEnabled = false))

        navigateDirectlyTo(AuthRoute.Email.EmailLinkSignIn)

        assertThat(currentRoute()).isEqualTo(AuthRoute.Email.SignIn.routePattern)
        assertThat(currentEmailArgument()).isEqualTo(TYPED_EMAIL)
        assertThat(backStackRoutes()).containsExactly(AuthRoute.Email.SignIn.routePattern)
    }

    /**
     * The redirect is not instant: against the configured 700 ms cross-fade the bounced step is on
     * screen for the whole transition, so it must render something.
     */
    @Test
    fun `a step that is redirecting out of itself still renders something`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides stringProvider
            ) {
                RedirectingStep()
            }
        }
        composeTestRule.waitForIdle()

        // No test tag: main-source tags come from the public FirebaseAuthTestTags registry only.
        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    // Resets

    /**
     * [resetBackStackTo] is what every reset in
     * [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen] goes through — success, cancellation
     * and a genuine idle — and must leave nothing of the flow underneath what it lands on.
     */
    @Test
    fun `a reset clears the flow whatever the back stack holds`() {
        start()
        typeEmail(TYPED_EMAIL)
        goTo(EmailAuthMode.SignUp)
        goTo(EmailAuthMode.ResetPassword)

        reset(AuthRoute.MethodPicker)

        assertThat(backStackRoutes()).containsExactly(AuthRoute.MethodPicker.routePattern)
    }

    /** Repeated resets must not pile up either, whichever destination they land on. */
    @Test
    fun `repeated resets keep the back stack at a single entry`() {
        start()

        repeat(4) {
            reset(AuthRoute.MethodPicker)
            assertThat(backStackRoutes()).containsExactly(AuthRoute.MethodPicker.routePattern)

            reset(AuthRoute.Email)
            assertThat(backStackRoutes()).containsExactly(AuthRoute.Email.SignIn.routePattern)
        }
    }

    /** A reset lands on a single entry, leaving nothing inside the flow for system back. */
    @Test
    fun `system back after a reset has nothing in the flow to return to`() {
        start()
        goTo(EmailAuthMode.SignUp)
        reset(AuthRoute.Email)

        back()

        assertThat(currentRoute()).isEqualTo(AuthRoute.Email.SignIn.routePattern)
        assertThat(backStackRoutes()).containsExactly(AuthRoute.Email.SignIn.routePattern)
    }

    // Harness

    private fun emailConfiguration(
        isEmailLinkSignInEnabled: Boolean = true,
    ): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(
                AuthProvider.Email(
                    isEmailLinkSignInEnabled = isEmailLinkSignInEnabled,
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
    }

    private fun reauthConfiguration(): AuthUIConfiguration = emailConfiguration().copy(
        isNewEmailAccountsAllowed = false,
        isReauthenticationMode = true,
    )

    private fun start(configuration: AuthUIConfiguration = emailConfiguration()) {
        composeTestRule.setContent { EmailFlowHost(configuration) }
        composeTestRule.waitForIdle()
    }

    private fun typeEmail(email: String) {
        composeTestRule.runOnIdle { requireNotNull(lastState).onEmailChange(email) }
        composeTestRule.waitForIdle()
    }

    private fun goTo(mode: EmailAuthMode) {
        composeTestRule.runOnIdle {
            val state = requireNotNull(lastState)
            when (mode) {
                EmailAuthMode.SignIn -> state.onGoToSignIn()
                EmailAuthMode.SignUp -> state.onGoToSignUp()
                EmailAuthMode.ResetPassword -> state.onGoToResetPassword()
                EmailAuthMode.EmailLinkSignIn -> state.onGoToEmailLinkSignIn()
            }
        }
        composeTestRule.waitForIdle()
    }

    /** Enters [step] the way a host's `onNavigate` does — bypassing the screen's own guards. */
    private fun navigateDirectlyTo(step: AuthRoute.Email.Step) {
        composeTestRule.runOnIdle {
            requireNotNull(navController).navigate(step.withEmail(TYPED_EMAIL))
        }
        composeTestRule.waitForIdle()
    }

    private fun reset(route: AuthRoute) {
        composeTestRule.runOnIdle { requireNotNull(navController).resetBackStackTo(route) }
        composeTestRule.waitForIdle()
    }

    private fun back() {
        composeTestRule.runOnUiThread { requireNotNull(pressBack).invoke() }
        composeTestRule.waitForIdle()
    }

    private fun currentRoute(): String? =
        composeTestRule.runOnIdle { navController?.currentBackStackEntry?.destination?.route }

    private fun currentEmailArgument(): String? = composeTestRule.runOnIdle {
        navController?.currentBackStackEntry?.arguments?.getString(EMAIL_ARG)
    }

    /**
     * The composed destinations on the stack, bottom to top, read from the [ComposeNavigator]'s
     * own back stack. `NavController.currentBackStack` is `@RestrictTo(LIBRARY_GROUP)`.
     */
    private fun backStackRoutes(): List<String?> = composeTestRule.runOnIdle {
        navController?.navigatorProvider?.get(ComposeNavigator::class)
            ?.backStack?.value
            ?.map { it.destination.route }
            .orEmpty()
    }

    private fun renderedEmail(): String? = composeTestRule.runOnIdle { lastState?.email }

    private fun renderedMode(): EmailAuthMode? = composeTestRule.runOnIdle { lastState?.mode }

    @Composable
    private fun EmailFlowHost(configuration: AuthUIConfiguration) {
        val controller = rememberNavController()
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        SideEffect {
            navController = controller
            pressBack = dispatcher?.let { { it.onBackPressed() } }
        }

        val authState by remember(authUI) { authUI.authStateFlow() }.collectAsState(AuthState.Idle)
        val dialogController = rememberTopLevelDialogController(stringProvider) { authState }

        CompositionLocalProvider(
            LocalAuthUIStringProvider provides configuration.stringProvider,
            LocalTopLevelDialogController provides dialogController,
        ) {
            NavHost(
                navController = controller,
                startDestination = AuthRoute.Email.routePattern,
                // Transitions would keep two email destinations composed at once.
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                emailAuthDestinations(
                    navController = controller,
                    context = applicationContext,
                    configuration = configuration,
                    authUI = authUI,
                    content = { state -> lastState = state },
                    onCancel = {},
                )
                // Somewhere outside the flow for a reset to land on, standing in for the picker.
                composable(AuthRoute.MethodPicker.routePattern) { }
            }
            dialogController.CurrentDialog()
        }
    }

    private companion object {
        const val TYPED_EMAIL = "user+tag@example.com"
    }
}
