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
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
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
import com.firebase.ui.auth.ui.screens.mode
import com.firebase.ui.auth.ui.screens.popOrNull
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
 * Covers the email flow's one destination per [EmailAuthMode].
 *
 * The unit under test is [emailAuthDestinations] — the same graph extension both
 * [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen] and the reauthentication sheet install —
 * hosted here in a bare `NavDisplay` so the back stack and its depth can be read directly. The
 * flow's sign-in step is this graph's start destination, which is the shape a single-provider
 * email configuration produces and the one every back-stack hazard here turns on.
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

    private var backStack: NavBackStack<NavKey>? = null
    private var lastState: EmailAuthContentState? = null
    private var pressBack: (() -> Unit)? = null
    private var backDispatcher: androidx.activity.OnBackPressedDispatcher? = null

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
        backStack = null
        lastState = null
        pressBack = null
        backDispatcher = null
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    // =============================================================================================
    // User-initiated switches
    // =============================================================================================

    @Test
    fun `going to sign-up pushes the sign-up destination carrying the typed address`() {
        start()
        typeEmail(TYPED_EMAIL)

        goTo(EmailAuthMode.SignUp)

        assertThat(currentStep()).isEqualTo(AuthRoute.Email.SignUp(TYPED_EMAIL))
        assertThat(currentStepEmail()).isEqualTo(TYPED_EMAIL)
        assertThat(renderedEmail()).isEqualTo(TYPED_EMAIL)
        assertThat(backStackTypes()).containsExactly(
            AuthRoute.Email.SignIn::class,
            AuthRoute.Email.SignUp::class,
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

            assertThat(currentStep()).isEqualTo(AuthRoute.Email.stepFor(target, TYPED_EMAIL))
            assertThat(currentStepEmail()).isEqualTo(TYPED_EMAIL)
            assertThat(renderedEmail()).isEqualTo(TYPED_EMAIL)
        }
    }

    @Test
    fun `the exposed mode always matches the active destination`() {
        start()

        EmailAuthMode.entries.forEach { target ->
            goTo(target)

            assertThat(currentStep()?.mode).isEqualTo(target)
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
        assertThat(currentStep()).isEqualTo(AuthRoute.Email.SignUp(TYPED_EMAIL))

        back()
        assertThat(currentStep()).isInstanceOf(AuthRoute.Email.SignIn::class.java)
    }

    /**
     * Toggling between two steps must not add an entry per tap: the stack holds those two steps
     * however long the user keeps tapping.
     */
    @Test
    fun `toggling sign-in and sign-up keeps the back stack bounded`() {
        start()
        typeEmail(TYPED_EMAIL)

        repeat(6) {
            goTo(EmailAuthMode.SignUp)
            assertThat(backStackTypes()).containsExactly(
                AuthRoute.Email.SignIn::class,
                AuthRoute.Email.SignUp::class,
            ).inOrder()

            goTo(EmailAuthMode.SignIn)
            assertThat(backStackTypes())
                .containsExactly(AuthRoute.Email.SignIn::class)
        }
    }

    /**
     * Switching to a step already beneath the current one pops back to it instead of stacking a
     * second copy — and the address the user has *just* typed wins over the one that entry was
     * created with, which is what re-pushing it buys.
     */
    @Test
    fun `switching back to a step already on the stack keeps the newest address`() {
        start()
        goTo(EmailAuthMode.SignUp)
        typeEmail(TYPED_EMAIL)

        goTo(EmailAuthMode.SignIn)

        assertThat(backStackTypes()).containsExactly(AuthRoute.Email.SignIn::class)
        assertThat(currentStepEmail()).isEqualTo(TYPED_EMAIL)
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

    // =============================================================================================
    // The address the key carries
    // =============================================================================================

    /**
     * Every character a local part or domain may legally hold has to survive being carried on the
     * key through a live mode switch.
     */
    @Test
    fun `awkward addresses survive a mode switch unchanged`() {
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

            assertThat(currentStepEmail()).isEqualTo(address)
            assertThat(renderedEmail()).isEqualTo(address)

            goTo(EmailAuthMode.SignIn)
        }
    }

    /**
     * A key carries the address as a field, so `SignIn("")` and `SignIn(null)` are different keys
     * and an empty address is a state the flow can genuinely be entered in — by
     * `AuthSuccessUiContext.onNavigate`, by a restored back stack, or by a mode switch made before
     * anything was typed.
     *
     * The destination collapses the two back together, with `step.email?.ifEmpty { null } ?:
     * prefillEmail()`, so an empty address falls back to the prefill exactly as a null one does
     * rather than blanking a field the host had populated. Asserted here, against the destination,
     * because the same claim written as an expression (`SignIn("").email?.ifEmpty { null }`) is a
     * test of the Kotlin stdlib and survives the product losing `?.ifEmpty { null }` entirely.
     */
    @Test
    fun `a step entered with an empty address falls back to the prefill`() {
        start(startKey = AuthRoute.Email.SignIn(""), prefillEmail = PREFILL_EMAIL)

        // The key really does carry the empty address — otherwise this would prove nothing.
        assertThat(currentStepEmail()).isEmpty()
        assertThat(renderedEmail()).isEqualTo(PREFILL_EMAIL)
    }

    /** The same fallback for a null address, which is the case the empty one has to match. */
    @Test
    fun `a step entered with no address falls back to the prefill`() {
        start(startKey = AuthRoute.Email.SignIn(null), prefillEmail = PREFILL_EMAIL)

        assertThat(currentStepEmail()).isNull()
        assertThat(renderedEmail()).isEqualTo(PREFILL_EMAIL)
    }

    /** The direction that must *not* collapse: a real address on the key beats the prefill. */
    @Test
    fun `an address on the key overrides the prefill`() {
        start(startKey = AuthRoute.Email.SignIn(TYPED_EMAIL), prefillEmail = PREFILL_EMAIL)

        assertThat(renderedEmail()).isEqualTo(TYPED_EMAIL)
    }

    /**
     * The address is a field on a `@Serializable` key, restored by `rememberNavBackStack`
     * serializing the keys — the one place an awkward address could be lost across recreation.
     *
     * Driven through a real recreation rather than a direct `Json` round-trip (which
     * `FirebaseAuthScreenRouteTest` covers separately), so what is asserted is the back stack the
     * user actually comes back to.
     */
    @Test
    fun `the address on a key survives Activity recreation`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent { EmailFlowHost(emailConfiguration()) }
        composeTestRule.waitForIdle()

        typeEmail(AWKWARD_EMAIL)
        goTo(EmailAuthMode.SignUp)
        assertThat(currentStep()).isEqualTo(AuthRoute.Email.SignUp(AWKWARD_EMAIL))

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        assertThat(backStackKeys()).containsExactly(
            AuthRoute.Email.SignIn(),
            AuthRoute.Email.SignUp(AWKWARD_EMAIL),
        ).inOrder()
        assertThat(currentStepEmail()).isEqualTo(AWKWARD_EMAIL)
        assertThat(renderedEmail()).isEqualTo(AWKWARD_EMAIL)
    }

    // =============================================================================================
    // Errors belong to the host
    // =============================================================================================

    /**
     * `AuthState.Error` is one event on a shared flow, and the top-level dialog controller
     * de-duplicates first-wins on it. A hosted step reacting to it as well would race the host —
     * whose recovery actions are the ones that know what lies outside the email flow — and
     * composition order would decide which set the user got. A hosted step therefore does
     * neither: it does not move the flow and it does not raise the dialog.
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

        assertThat(backStackTypes()).containsExactly(AuthRoute.Email.SignIn::class)
        assertThat(renderedEmail()).isEqualTo(TYPED_EMAIL)
        composeTestRule.onAllNodesWithText(stringProvider.errorDialogTitle).assertCountEquals(0)
    }

    // =============================================================================================
    // Steps the configuration does not offer
    // =============================================================================================

    @Test
    fun `reauthentication cannot switch to sign-up`() {
        start(configuration = reauthConfiguration())
        typeEmail(TYPED_EMAIL)

        goTo(EmailAuthMode.SignUp)

        assertThat(currentStep()).isInstanceOf(AuthRoute.Email.SignIn::class.java)
    }

    @Test
    fun `reauthentication cannot switch to email-link sign-in`() {
        start(configuration = reauthConfiguration())
        typeEmail(TYPED_EMAIL)

        goTo(EmailAuthMode.EmailLinkSignIn)

        assertThat(currentStep()).isInstanceOf(AuthRoute.Email.SignIn::class.java)
    }

    @Test
    fun `a provider without email-link sign-in cannot switch to it`() {
        start(configuration = emailConfiguration(isEmailLinkSignInEnabled = false))
        typeEmail(TYPED_EMAIL)

        goTo(EmailAuthMode.EmailLinkSignIn)

        assertThat(currentStep()).isInstanceOf(AuthRoute.Email.SignIn::class.java)
    }

    /**
     * `AuthSuccessUiContext.onNavigate` takes any [AuthRoute], so a host can hand the graph a step
     * the configuration switched off; a restored back stack can do the same. The bounce is what
     * keeps such a step unreachable, and it keeps the address.
     */
    @Test
    fun `the sign-up destination bounces to sign-in when account creation is not offered`() {
        start(configuration = reauthConfiguration())

        navigateDirectlyTo(AuthRoute.Email.SignUp())

        assertThat(currentStep()).isInstanceOf(AuthRoute.Email.SignIn::class.java)
        assertThat(currentStepEmail()).isEqualTo(TYPED_EMAIL)
        // The bounced step is gone rather than left underneath, where back would return to it and
        // bounce again, leaving the user unable to go back at all.
        assertThat(backStackTypes()).containsExactly(AuthRoute.Email.SignIn::class)
    }

    /** The same exposure, and now the same guard, for a provider with email-link disabled. */
    @Test
    fun `the email-link destination bounces to sign-in when the provider disables it`() {
        start(configuration = emailConfiguration(isEmailLinkSignInEnabled = false))

        navigateDirectlyTo(AuthRoute.Email.EmailLinkSignIn())

        assertThat(currentStep()).isInstanceOf(AuthRoute.Email.SignIn::class.java)
        assertThat(currentStepEmail()).isEqualTo(TYPED_EMAIL)
        assertThat(backStackTypes()).containsExactly(AuthRoute.Email.SignIn::class)
    }

    /**
     * The redirect is not instant to the eye: against the configured 700 ms cross-fade the step
     * being left behind is on screen for the whole transition, so rendering nothing there would
     * show a hole rather than a frame.
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

        // No test tag: main-source tags have to come from the public FirebaseAuthTestTags
        // registry, and a placeholder nobody addresses does not earn a place in it. The
        // indeterminate progress semantics are what a user actually sees here.
        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    // =============================================================================================
    // Resets
    // =============================================================================================

    /**
     * [resetBackStackTo] is what every reset in
     * [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen] goes through — success, cancellation
     * and a genuine idle. A reset that quietly did nothing would leave the whole flow sitting
     * underneath what it navigated to.
     */
    @Test
    fun `a reset clears the flow whatever the back stack holds`() {
        start()
        typeEmail(TYPED_EMAIL)
        goTo(EmailAuthMode.SignUp)
        goTo(EmailAuthMode.ResetPassword)

        reset(AuthRoute.MethodPicker)

        assertThat(backStackKeys()).containsExactly(AuthRoute.MethodPicker)
    }

    /** Repeated resets must not pile up either, whichever destination they land on. */
    @Test
    fun `repeated resets keep the back stack at a single entry`() {
        start()

        repeat(4) {
            reset(AuthRoute.MethodPicker)
            assertThat(backStackKeys()).containsExactly(AuthRoute.MethodPicker)

            reset(AuthRoute.Email)
            assertThat(backStackKeys()).containsExactly(AuthRoute.Email.SignIn())
        }
    }

    /**
     * A reset lands on a single entry, so there is nothing left inside the flow for system back
     * to return to.
     */
    @Test
    fun `system back after a reset has nothing in the flow to return to`() {
        start()
        goTo(EmailAuthMode.SignUp)
        reset(AuthRoute.Email)

        back()

        assertThat(currentStep()).isInstanceOf(AuthRoute.Email.SignIn::class.java)
        assertThat(backStackKeys()).containsExactly(AuthRoute.Email.SignIn())
    }

    /**
     * S19/S20: a root back press has to fall through to whatever hosts the graph — the Activity on
     * the main display, the `ModalBottomSheet` in the reauthentication sheet, where falling through
     * is what dismisses it.
     *
     * `NavDisplay` enables its back handler only while `scene.previousEntries.isNotEmpty()`, so it
     * swallows nothing at the root — which is why `onBack` only has to be safe for the non-root
     * case and why the sheet does not need to dismiss from `onBack`. Pinned here because
     * it is an unwritten property of the navigation library, not of this code: if a future version
     * enabled the handler at the root, root back would start being swallowed and the sheet would
     * stop dismissing, silently.
     */
    @Test
    fun `the display leaves root back to its host`() {
        start()
        goTo(EmailAuthMode.SignUp)

        assertThat(hasEnabledBackCallbacks()).isTrue()

        back()

        assertThat(backStackKeys()).hasSize(1)
        assertThat(hasEnabledBackCallbacks()).isFalse()
    }

    // =============================================================================================
    // Harness
    // =============================================================================================

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

    private fun start(
        configuration: AuthUIConfiguration = emailConfiguration(),
        startKey: NavKey = AuthRoute.Email.startKey(),
        prefillEmail: String? = null,
    ) {
        composeTestRule.setContent {
            EmailFlowHost(configuration, startKey = startKey, prefillEmail = prefillEmail)
        }
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
            requireNotNull(backStack).add(step.withEmail(TYPED_EMAIL))
        }
        composeTestRule.waitForIdle()
    }

    private fun reset(route: AuthRoute) {
        composeTestRule.runOnIdle { requireNotNull(backStack).resetBackStackTo(route) }
        composeTestRule.waitForIdle()
    }

    private fun back() {
        composeTestRule.runOnUiThread { requireNotNull(pressBack).invoke() }
        composeTestRule.waitForIdle()
    }

    private fun hasEnabledBackCallbacks(): Boolean = composeTestRule.runOnIdle {
        requireNotNull(backDispatcher).hasEnabledCallbacks()
    }

    private fun currentStep(): AuthRoute.Email.Step? =
        composeTestRule.runOnIdle { backStack?.lastOrNull() as? AuthRoute.Email.Step }

    /** The address the top step carries, read straight off the key. */
    private fun currentStepEmail(): String? = composeTestRule.runOnIdle {
        (backStack?.lastOrNull() as? AuthRoute.Email.Step)?.email
    }

    /** The keys on the stack, bottom to top — the back stack is itself a public list. */
    private fun backStackKeys(): List<NavKey> =
        composeTestRule.runOnIdle { backStack?.toList().orEmpty() }

    /** Types only, for the assertions that must ignore which address a key carries. */
    private fun backStackTypes(): List<Any> =
        composeTestRule.runOnIdle { backStack?.map { it::class }.orEmpty() }

    private fun renderedEmail(): String? = composeTestRule.runOnIdle { lastState?.email }

    private fun renderedMode(): EmailAuthMode? = composeTestRule.runOnIdle { lastState?.mode }

    @Composable
    private fun EmailFlowHost(
        configuration: AuthUIConfiguration,
        startKey: NavKey = AuthRoute.Email.startKey(),
        prefillEmail: String? = null,
    ) {
        val stack = rememberNavBackStack(startKey)
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        SideEffect {
            backStack = stack
            backDispatcher = dispatcher
            pressBack = dispatcher?.let { { it.onBackPressed() } }
        }

        val authState by remember(authUI) { authUI.authStateFlow() }.collectAsState(AuthState.Idle)
        val dialogController = rememberTopLevelDialogController(stringProvider) { authState }

        CompositionLocalProvider(
            LocalAuthUIStringProvider provides configuration.stringProvider,
            LocalTopLevelDialogController provides dialogController,
        ) {
            NavDisplay(
                backStack = stack,
                onBack = { stack.popOrNull() },
                // Transitions would keep two email destinations composed at once, and the routing
                // under test has nothing to do with how it animates. That the *configured*
                // transitions reach a step-to-step move is pinned on the real host instead.
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                predictivePopTransitionSpec = { _ ->
                    EnterTransition.None togetherWith ExitTransition.None
                },
                entryProvider = entryProvider {
                    emailAuthDestinations(
                        backStack = stack,
                        context = applicationContext,
                        configuration = configuration,
                        authUI = authUI,
                        content = { state -> lastState = state },
                        onCancel = {},
                        prefillEmail = { prefillEmail },
                    )
                    // Somewhere outside the flow for a reset to land on, standing in for the method
                    // picker the real host resets to when more than one provider is configured.
                    entry<AuthRoute.MethodPicker> { }
                },
            )
            dialogController.CurrentDialog()
        }
    }

    private companion object {
        const val TYPED_EMAIL = "user+tag@example.com"

        /** An address the *host* fixed, which a step with no address of its own falls back to. */
        const val PREFILL_EMAIL = "prefilled@example.com"

        /** Every awkward character class a local part or domain may hold, in one address. */
        const val AWKWARD_EMAIL = "user name#hash?q=1&more@königsberg.example"
    }
}
