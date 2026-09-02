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

package com.firebase.ui.auth.ui.screens.mfa

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.get
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactor
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.TotpMultiFactorGenerator
import com.google.firebase.auth.TotpSecret as FirebaseTotpSecret
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers moving the MFA enrolment flow's steps — [AuthRoute.MfaEnrollment.SelectFactor],
 * [AuthRoute.MfaEnrollment.ConfigureSms], [AuthRoute.MfaEnrollment.ConfigureTotp] and
 * [AuthRoute.MfaEnrollment.VerifyFactor] — onto real navigation destinations.
 *
 * The unit under test is [mfaEnrollmentDestinations], hosted here in a bare `NavHost` so the back
 * stack can be read directly.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class MfaEnrollmentRouteNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockAuth: FirebaseAuth

    @Mock
    private lateinit var mockUser: FirebaseUser

    @Mock
    private lateinit var mockMultiFactor: MultiFactor

    private lateinit var authUI: FirebaseAuthUI

    private var navController: NavHostController? = null
    private var lastState: MfaEnrollmentContentState? = null
    private var pressBack: (() -> Unit)? = null
    private var reportComplete: (() -> Unit)? = null

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        FirebaseAuthUI.clearInstanceCache()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach { it.delete() }
        val app = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
        `when`(mockAuth.app).thenReturn(app)
        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.uid).thenReturn("mfa-route-user")
        `when`(mockUser.email).thenReturn("user@example.com")
        `when`(mockUser.multiFactor).thenReturn(mockMultiFactor)
        `when`(mockMultiFactor.enrolledFactors).thenReturn(emptyList())
        authUI = FirebaseAuthUI.create(app, mockAuth)
    }

    @After
    fun tearDown() {
        navController = null
        lastState = null
        pressBack = null
        reportComplete = null
        FirebaseAuthUI.clearInstanceCache()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    // A step switch must not dispose what a previous step held

    /**
     * Guards the regression where backing out to [AuthRoute.MfaEnrollment.SelectFactor] blanked
     * the typed phone number.
     */
    @Test
    fun `the typed phone number survives a detour through TOTP and back`() {
        start()
        selectFactor(MfaFactor.Sms)
        typePhoneNumber(TYPED_PHONE_NUMBER)

        back()
        assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.SelectFactor.routePattern)

        // A detour, not an immediate re-selection: local state could survive the latter by luck.
        selectFactor(MfaFactor.Totp)
        back()

        selectFactor(MfaFactor.Sms)
        assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureSms.routePattern)
        assertThat(requireNotNull(lastState).phoneNumber).isEqualTo(TYPED_PHONE_NUMBER)
    }

    // System back walks VerifyFactor back to whichever factor was chosen

    @Test
    fun `back from VerifyFactor returns to ConfigureSms when SMS was chosen`() {
        start()
        selectFactor(MfaFactor.Sms)
        pushVerifyFactorDirectly()
        assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor.routePattern)

        back()

        assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureSms.routePattern)
    }

    @Test
    fun `back from VerifyFactor returns to ConfigureTotp when TOTP was chosen`() {
        withMockedTotpSecret {
            start()
            selectFactor(MfaFactor.Totp)
            continueToVerify()
            assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor.routePattern)

            back()

            assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp.routePattern)
        }
    }

    // The TOTP secret is fetched once, not on every visit to ConfigureTotp

    @Test
    fun `the TOTP secret is fetched once and survives a back-and-forward through SMS`() {
        withMockedTotpSecret { totpStatic, mockSession ->
            start()

            selectFactor(MfaFactor.Totp)
            assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp.routePattern)
            assertThat(requireNotNull(lastState).totpSecret).isNotNull()
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isEqualTo(FAKE_QR_URL)

            back()
            selectFactor(MfaFactor.Sms)
            back()
            selectFactor(MfaFactor.Totp)

            assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp.routePattern)
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isEqualTo(FAKE_QR_URL)
            totpStatic.verify(
                { TotpMultiFactorGenerator.generateSecret(mockSession) },
                times(1),
            )
        }
    }

    // A single allowed factor resolves its start step at flow entry

    @Test
    fun `an SMS-only configuration resolves to ConfigureSms`() {
        assertThat(mfaEnrollmentStartStep(smsOnlyConfiguration()))
            .isEqualTo(AuthRoute.MfaEnrollment.ConfigureSms)
    }

    @Test
    fun `a TOTP-only configuration resolves to ConfigureTotp`() {
        assertThat(mfaEnrollmentStartStep(totpOnlyConfiguration()))
            .isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp)
    }

    @Test
    fun `a configuration with more than one factor resolves to SelectFactor`() {
        assertThat(mfaEnrollmentStartStep(twoFactorConfiguration()))
            .isEqualTo(AuthRoute.MfaEnrollment.SelectFactor)
    }

    @Test
    fun `an SMS-only flow never visits SelectFactor`() {
        val configuration = smsOnlyConfiguration()
        start(configuration, startStep = mfaEnrollmentStartStep(configuration))

        assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureSms.routePattern)
        assertThat(backStackRoutes())
            .containsExactly(AuthRoute.MfaEnrollment.ConfigureSms.routePattern)
    }

    /** The secret must still be pre-fetched when `ConfigureTotp` is the flow's first destination. */
    @Test
    fun `a TOTP-only flow fetches the secret without ever visiting SelectFactor`() {
        withMockedTotpSecret { totpStatic, mockSession ->
            val configuration = totpOnlyConfiguration()
            start(configuration, startStep = mfaEnrollmentStartStep(configuration))

            assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp.routePattern)
            assertThat(backStackRoutes())
                .containsExactly(AuthRoute.MfaEnrollment.ConfigureTotp.routePattern)
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isEqualTo(FAKE_QR_URL)
            totpStatic.verify(
                { TotpMultiFactorGenerator.generateSecret(mockSession) },
                times(1),
            )
        }
    }

    // Completing or skipping leaves the flow, from whichever step it happened on

    @Test
    fun `a successful enrolment three steps deep leaves the flow`() {
        startOutsideFlow()
        val hostEntry = hostEntry()
        selectFactor(MfaFactor.Sms)
        pushVerifyFactorDirectly()
        assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor.routePattern)

        completeEnrollment()

        assertThat(currentRoute()).isEqualTo(HOST_ROUTE)
        assertThat(backStackRoutes()).containsExactly(HOST_ROUTE)
        assertThat(hostEntry()).isSameInstanceAs(hostEntry)
    }

    @Test
    fun `a skip from a pushed step leaves the flow`() {
        startOutsideFlow()
        val hostEntry = hostEntry()
        selectFactor(MfaFactor.Sms)
        assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureSms.routePattern)

        skipEnrollment()

        assertThat(currentRoute()).isEqualTo(HOST_ROUTE)
        assertThat(backStackRoutes()).containsExactly(HOST_ROUTE)
        assertThat(hostEntry()).isSameInstanceAs(hostEntry)
    }

    /** The start step is `ConfigureSms`, so an exit pinned to `SelectFactor` would pop nothing. */
    @Test
    fun `a successful enrolment leaves an SMS-only flow that never visited SelectFactor`() {
        startOutsideFlow(smsOnlyConfiguration())
        val hostEntry = hostEntry()
        assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureSms.routePattern)
        pushVerifyFactorDirectly()

        completeEnrollment()

        assertThat(currentRoute()).isEqualTo(HOST_ROUTE)
        assertThat(hostEntry()).isSameInstanceAs(hostEntry)
    }

    /**
     * A host can enter at any step through `AuthSuccessUiContext.onNavigate`, which never pushes
     * the resolved start step — so an exit that pops up to that start step finds nothing.
     */
    @Test
    fun `a successful enrolment leaves a flow entered at a step that is not its start step`() {
        startOutsideFlow(enterAtStartStep = false)
        val hostEntry = hostEntry()
        navigateDirectlyTo(AuthRoute.MfaEnrollment.ConfigureSms)
        pushVerifyFactorDirectly()
        assertThat(backStackRoutes())
            .doesNotContain(AuthRoute.MfaEnrollment.SelectFactor.routePattern)

        completeEnrollment()

        assertThat(currentRoute()).isEqualTo(HOST_ROUTE)
        assertThat(backStackRoutes()).containsExactly(HOST_ROUTE)
        assertThat(hostEntry()).isSameInstanceAs(hostEntry)
    }

    /**
     * The "Manage MFA" control is an undebounced `Button` and entry is a bare `navigate`, so two
     * taps in one frame stack the start step twice. An exit popping only the topmost occurrence
     * would land on the duplicate.
     */
    @Test
    fun `a successful enrolment leaves a flow whose start step was entered twice`() {
        startOutsideFlow()
        val hostEntry = hostEntry()
        enterFlow(twoFactorConfiguration())
        selectFactor(MfaFactor.Sms)
        assertThat(backStackRoutes()).containsExactly(
            HOST_ROUTE,
            AuthRoute.MfaEnrollment.SelectFactor.routePattern,
            AuthRoute.MfaEnrollment.SelectFactor.routePattern,
            AuthRoute.MfaEnrollment.ConfigureSms.routePattern,
        ).inOrder()

        completeEnrollment()

        assertThat(currentRoute()).isEqualTo(HOST_ROUTE)
        assertThat(backStackRoutes()).containsExactly(HOST_ROUTE)
        assertThat(hostEntry()).isSameInstanceAs(hostEntry)
    }

    /**
     * `onNavigate` accepts any step, so an SMS-only configuration — whose start step is
     * `ConfigureSms` — can still be entered at `SelectFactor`. An exit inclusive of the resolved
     * start step would strand the user on the picker it stacked underneath.
     */
    @Test
    fun `a successful enrolment leaves an SMS-only flow entered at SelectFactor`() {
        startOutsideFlow(smsOnlyConfiguration(), enterAtStartStep = false)
        val hostEntry = hostEntry()
        navigateDirectlyTo(AuthRoute.MfaEnrollment.SelectFactor)
        selectFactor(MfaFactor.Sms)
        pushVerifyFactorDirectly()

        completeEnrollment()

        assertThat(currentRoute()).isEqualTo(HOST_ROUTE)
        assertThat(backStackRoutes()).containsExactly(HOST_ROUTE)
        assertThat(hostEntry()).isSameInstanceAs(hostEntry)
    }

    /**
     * `onVerifyClick` reports completion from an unguarded coroutine, so a second exit is
     * reachable. It must not rebuild the host entry the caller's own state is scoped to.
     */
    @Test
    fun `a second exit after the flow has been left changes nothing`() {
        startOutsideFlow()
        val hostEntry = hostEntry()
        selectFactor(MfaFactor.Sms)
        completeEnrollment()

        completeEnrollment()

        assertThat(currentRoute()).isEqualTo(HOST_ROUTE)
        assertThat(backStackRoutes()).containsExactly(HOST_ROUTE)
        assertThat(hostEntry()).isSameInstanceAs(hostEntry)
    }

    /**
     * A step reached with no signed-in user cannot render, and neither can the step underneath
     * it. Popping one at a time would walk the flow out a step per frame; leaving does it in one.
     */
    @Test
    fun `a step reached with no signed-in user leaves the whole flow`() {
        startOutsideFlow()
        val hostEntry = hostEntry()
        selectFactor(MfaFactor.Sms)
        pushVerifyFactorDirectly()
        `when`(mockAuth.currentUser).thenReturn(null)

        navigateDirectlyTo(AuthRoute.MfaEnrollment.ConfigureTotp)

        assertThat(currentRoute()).isEqualTo(HOST_ROUTE)
        assertThat(backStackRoutes()).containsExactly(HOST_ROUTE)
        assertThat(hostEntry()).isSameInstanceAs(hostEntry)
    }

    /** Nothing to pop back to: the fallback has to put something on the emptied stack. */
    @Test
    fun `an exit from a flow that is the whole back stack resets to Success`() {
        start()
        selectFactor(MfaFactor.Sms)
        assertThat(backStackRoutes()).doesNotContain(HOST_ROUTE)

        completeEnrollment()

        assertThat(currentRoute()).isEqualTo(AuthRoute.Success.routePattern)
        assertThat(backStackRoutes()).containsExactly(AuthRoute.Success.routePattern)
    }

    // Every public AuthRoute.MfaEnrollment value is a registered destination

    /**
     * Wrapped in [withMockedTotpSecret]: the loop walks `ConfigureTotp` before `VerifyFactor`, so
     * without a live secret the TOTP-loss recovery would bounce `VerifyFactor` back — correct
     * behavior, but it would hide whether that step is reachable at all.
     */
    @Test
    fun `every declared MFA enrolment step is reachable directly`() {
        withMockedTotpSecret {
            start()

            AuthRoute.MfaEnrollment.steps.forEach { step ->
                navigateDirectlyTo(step)
                assertThat(currentRoute()).isEqualTo(step.routePattern)
            }
        }
    }

    // Harness

    private fun twoFactorConfiguration() = MfaConfiguration(
        allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),
        requireEnrollment = false,
    )

    private fun smsOnlyConfiguration() = MfaConfiguration(
        allowedFactors = listOf(MfaFactor.Sms),
        requireEnrollment = false,
    )

    private fun totpOnlyConfiguration() = MfaConfiguration(
        allowedFactors = listOf(MfaFactor.Totp),
        requireEnrollment = false,
    )

    /**
     * Stubs [mockMultiFactor]'s session and [TotpMultiFactorGenerator.generateSecret] to complete
     * synchronously with a fake secret, for the duration of [block]. Every action and assertion
     * depending on the stub must run inside [block].
     */
    private fun withMockedTotpSecret(
        block: (
            totpStatic: MockedStatic<TotpMultiFactorGenerator>,
            mockSession: MultiFactorSession,
        ) -> Unit
    ) {
        val mockSession = mock(MultiFactorSession::class.java)
        val mockFirebaseSecret = mock(FirebaseTotpSecret::class.java)
        `when`(mockMultiFactor.session).thenReturn(Tasks.forResult(mockSession))
        `when`(mockFirebaseSecret.sharedSecretKey).thenReturn(FAKE_SHARED_SECRET)
        `when`(mockFirebaseSecret.generateQrCodeUrl(any(), any())).thenReturn(FAKE_QR_URL)

        mockStatic(TotpMultiFactorGenerator::class.java).use { totpStatic ->
            totpStatic.`when`<Task<FirebaseTotpSecret>> {
                TotpMultiFactorGenerator.generateSecret(mockSession)
            }.thenReturn(Tasks.forResult(mockFirebaseSecret))

            block(totpStatic, mockSession)
        }
    }

    private fun withMockedTotpSecret(block: () -> Unit) = withMockedTotpSecret { _, _ -> block() }

    private fun start(
        configuration: MfaConfiguration = twoFactorConfiguration(),
        startStep: AuthRoute.MfaEnrollment.Step = AuthRoute.MfaEnrollment.SelectFactor,
    ) {
        composeTestRule.setContent { MfaFlowHost(configuration, startStep) }
        composeTestRule.waitForIdle()
    }

    /**
     * Hosts the flow the way a real host does: [HOST_ROUTE] underneath it. Needed to tell leaving
     * the flow apart both from landing on one of its steps and from the [AuthRoute.Success]
     * fallback, which is a separate destination here.
     *
     * @param enterAtStartStep false to stay on [HOST_ROUTE], for a caller entering the flow at a
     * step of its own choosing.
     */
    private fun startOutsideFlow(
        configuration: MfaConfiguration = twoFactorConfiguration(),
        enterAtStartStep: Boolean = true,
    ) {
        composeTestRule.setContent {
            MfaFlowHost(
                configuration = configuration,
                startStep = AuthRoute.MfaEnrollment.SelectFactor,
                startOutsideFlow = true,
            )
        }
        composeTestRule.waitForIdle()
        if (enterAtStartStep) enterFlow(configuration)
    }

    /** Enters the flow the way both of `FirebaseAuthScreen`'s entry points do. */
    private fun enterFlow(configuration: MfaConfiguration) {
        composeTestRule.runOnIdle {
            requireNotNull(navController).navigate(mfaEnrollmentStartStep(configuration).route)
        }
        composeTestRule.waitForIdle()
    }

    /**
     * The live [HOST_ROUTE] entry, or null once it is gone. Compared by reference: a pop leaves
     * the same instance, a reset builds a new one and destroys whatever was scoped to the old.
     */
    private fun hostEntry(): NavBackStackEntry? = composeTestRule.runOnIdle {
        navController?.navigatorProvider?.get(ComposeNavigator::class)
            ?.backStack?.value
            ?.firstOrNull { it.destination.route == HOST_ROUTE }
    }

    /** Invokes the host's `onComplete`, as the screen does on a successful enrolment. */
    private fun completeEnrollment() {
        composeTestRule.runOnIdle { requireNotNull(reportComplete).invoke() }
        composeTestRule.waitForIdle()
    }

    private fun skipEnrollment() {
        composeTestRule.runOnIdle {
            requireNotNull(requireNotNull(lastState).onSkipClick).invoke()
        }
        composeTestRule.waitForIdle()
    }

    private fun selectFactor(factor: MfaFactor) {
        composeTestRule.runOnIdle { requireNotNull(lastState).onFactorSelected(factor) }
        composeTestRule.waitForIdle()
    }

    private fun typePhoneNumber(value: String) {
        composeTestRule.runOnIdle { requireNotNull(lastState).onPhoneNumberChange(value) }
        composeTestRule.waitForIdle()
    }

    private fun continueToVerify() {
        composeTestRule.runOnIdle { requireNotNull(lastState).onContinueToVerifyClick() }
        composeTestRule.waitForIdle()
    }

    /** Enters [AuthRoute.MfaEnrollment.VerifyFactor] as `onSendSmsCodeClick` does, minus the
     * real SMS network call. */
    private fun pushVerifyFactorDirectly() {
        composeTestRule.runOnIdle {
            requireNotNull(navController).navigateToMfaStep(AuthRoute.MfaEnrollment.VerifyFactor)
        }
        composeTestRule.waitForIdle()
    }

    /** Enters [step] the way a host's `onNavigate` does — bypassing the screen's own guards. */
    private fun navigateDirectlyTo(step: AuthRoute.MfaEnrollment.Step) {
        composeTestRule.runOnIdle { requireNotNull(navController).navigate(step.route) }
        composeTestRule.waitForIdle()
    }

    private fun back() {
        composeTestRule.runOnUiThread { requireNotNull(pressBack).invoke() }
        composeTestRule.waitForIdle()
    }

    private fun currentRoute(): String? =
        composeTestRule.runOnIdle { navController?.currentBackStackEntry?.destination?.route }

    /**
     * The composed destinations on the stack, bottom to top. Reads the `ComposeNavigator`'s own
     * back stack; `NavController.currentBackStack` is `@RestrictTo`.
     */
    private fun backStackRoutes(): List<String?> = composeTestRule.runOnIdle {
        navController?.navigatorProvider?.get(ComposeNavigator::class)
            ?.backStack?.value
            ?.map { it.destination.route }
            .orEmpty()
    }

    @Composable
    private fun MfaFlowHost(
        configuration: MfaConfiguration,
        startStep: AuthRoute.MfaEnrollment.Step,
        startOutsideFlow: Boolean = false,
    ) {
        val controller = rememberNavController()
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        val flowState = rememberMfaEnrollmentFlowState()
        val exit: () -> Unit = { controller.exitMfaEnrollment() }
        SideEffect {
            navController = controller
            pressBack = dispatcher?.let { { it.onBackPressed() } }
            reportComplete = exit
        }

        NavHost(
            navController = controller,
            startDestination =
                if (startOutsideFlow) HOST_ROUTE else startStep.routePattern,
            // Transitions would keep two MFA destinations composed at once.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(HOST_ROUTE) {}
            composable(AuthRoute.Success.routePattern) {}
            mfaEnrollmentDestinations(
                navController = controller,
                configuration = configuration,
                authConfiguration = null,
                authUI = authUI,
                flowState = flowState,
                content = { state -> lastState = state },
                onComplete = exit,
                onSkip = exit,
                onError = {},
            )
        }
    }

    private companion object {
        /**
         * Stands in for whatever the host had on the stack before the flow was entered.
         * Deliberately not [AuthRoute.Success]: that is the exit's fallback target, and the two
         * outcomes have to be distinguishable.
         */
        const val HOST_ROUTE = "host_outside_flow"

        const val TYPED_PHONE_NUMBER = "5551234567"
        const val FAKE_SHARED_SECRET = "JBSWY3DPEHPK3PXP"
        const val FAKE_QR_URL = "otpauth://totp/test-issuer:user%40example.com?secret=JBSWY3DPEHPK3PXP"
    }
}
