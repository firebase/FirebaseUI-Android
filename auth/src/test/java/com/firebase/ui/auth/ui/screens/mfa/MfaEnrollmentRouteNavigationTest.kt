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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.popOrNull
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
 * The unit under test is [mfaEnrollmentDestinations], the entry-provider extension
 * [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen] installs, hosted here in a bare
 * `NavDisplay` so the back stack can be read directly — the same shape
 * `com.firebase.ui.auth.ui.screens.email.EmailAuthRouteNavigationTest` uses for the email flow.
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

    private var backStack: NavBackStack<NavKey>? = null
    private var lastState: MfaEnrollmentContentState? = null
    private var pressBack: (() -> Unit)? = null

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
        backStack = null
        lastState = null
        pressBack = null
        FirebaseAuthUI.clearInstanceCache()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    // =============================================================================================
    // The headline bug: a step switch must not dispose what a previous step held
    // =============================================================================================

    /**
     * The ticket's headline bug. `MfaEnrollmentScreen.onBackClick` used to blank the phone number
     * on the way back to [AuthRoute.MfaEnrollment.SelectFactor] — see the `git show HEAD` version
     * of `onBackClick`. Hosted, back is real navigation and the flow's data lives in
     * [MfaEnrollmentFlowState], which the step returned to still holds.
     */
    @Test
    fun `the typed phone number survives a detour through TOTP and back`() {
        start()
        selectFactor(MfaFactor.Sms)
        typePhoneNumber(TYPED_PHONE_NUMBER)

        back()
        assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.SelectFactor)

        // A detour through the other factor and back — not just an immediate re-selection —
        // is what proves the data lives in the shared flowState rather than surviving by luck in
        // whatever local state a single recomposition happened to keep around.
        selectFactor(MfaFactor.Totp)
        back()

        selectFactor(MfaFactor.Sms)
        assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureSms)
        assertThat(requireNotNull(lastState).phoneNumber).isEqualTo(TYPED_PHONE_NUMBER)
    }

    // =============================================================================================
    // System back walks VerifyFactor back to whichever factor was actually chosen
    // =============================================================================================

    @Test
    fun `back from VerifyFactor returns to ConfigureSms when SMS was chosen`() {
        start()
        selectFactor(MfaFactor.Sms)
        // Stands in for onSendSmsCodeClick's own navigation, without the real SMS network call
        // onSendSmsCodeClick would make.
        pushVerifyFactorDirectly()
        assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor)

        back()

        assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureSms)
    }

    @Test
    fun `back from VerifyFactor returns to ConfigureTotp when TOTP was chosen`() {
        withMockedTotpSecret {
            start()
            selectFactor(MfaFactor.Totp)
            continueToVerify()
            assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor)

            back()

            assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp)
        }
    }

    // =============================================================================================
    // The TOTP secret is fetched once, not on every visit to ConfigureTotp
    // =============================================================================================

    @Test
    fun `the TOTP secret is fetched once and survives a back-and-forward through SMS`() {
        withMockedTotpSecret { totpStatic, mockSession ->
            start()

            selectFactor(MfaFactor.Totp)
            assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp)
            assertThat(requireNotNull(lastState).totpSecret).isNotNull()
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isEqualTo(FAKE_QR_URL)

            back()
            selectFactor(MfaFactor.Sms)
            back()
            selectFactor(MfaFactor.Totp)

            assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp)
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isEqualTo(FAKE_QR_URL)
            totpStatic.verify(
                { TotpMultiFactorGenerator.generateSecret(mockSession) },
                times(1),
            )
        }
    }

    // =============================================================================================
    // A single allowed factor resolves its start step at flow entry
    // =============================================================================================

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

        assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureSms)
        assertThat(backStackKeys())
            .containsExactly(AuthRoute.MfaEnrollment.ConfigureSms)
    }

    /**
     * The pre-fetch that used to happen in `MfaEnrollmentScreen`'s own `LaunchedEffect(Unit)`
     * bounce off `SelectFactor` still has to happen when the host resolves straight to
     * `ConfigureTotp` instead of routing through `SelectFactor` first.
     */
    @Test
    fun `a TOTP-only flow fetches the secret without ever visiting SelectFactor`() {
        withMockedTotpSecret { totpStatic, mockSession ->
            val configuration = totpOnlyConfiguration()
            start(configuration, startStep = mfaEnrollmentStartStep(configuration))

            assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp)
            assertThat(backStackKeys())
                .containsExactly(AuthRoute.MfaEnrollment.ConfigureTotp)
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isEqualTo(FAKE_QR_URL)
            totpStatic.verify(
                { TotpMultiFactorGenerator.generateSecret(mockSession) },
                times(1),
            )
        }
    }

    // =============================================================================================
    // Every public AuthRoute.MfaEnrollment value is a registered destination
    // =============================================================================================

    /**
     * Wrapped in [withMockedTotpSecret]: by the time this loop reaches
     * [AuthRoute.MfaEnrollment.VerifyFactor], the [AuthRoute.MfaEnrollment.ConfigureTotp] step
     * visited just before it (steps are declared and walked in that order) has already set
     * `selectedFactor` to TOTP. Without a mocked secret, that fetch fails and leaves `totpSecret`
     * null — which `MfaEnrollmentScreen`'s TOTP-loss recovery (added alongside
     * [com.firebase.ui.auth.ui.screens.mfa.MfaEnrollmentTotpRegenerationTest]) now correctly reads
     * as "landed on VerifyFactor with no live secret" and bounces back to `ConfigureTotp` for,
     * exactly as it should for a genuine loss. That is a real product behavior this test must
     * account for, not something to route around: mocking the secret is what makes "every step is
     * directly reachable" true again, the same way [withMockedTotpSecret] already lets the other
     * TOTP-path tests in this class reach `VerifyFactor` at all.
     */
    @Test
    fun `every declared MFA enrolment step is reachable directly`() {
        withMockedTotpSecret {
            start()

            AuthRoute.MfaEnrollment.steps.forEach { step ->
                navigateDirectlyTo(step)
                assertThat(currentKey()).isEqualTo(step)
            }
        }
    }

    // =============================================================================================
    // Leaving the flow drops every entry it pushed, from any depth
    // =============================================================================================

    /**
     * The headline defect: every move between steps is a push, so a single pop from three deep
     * strands the user on the step before rather than leaving.
     */
    @Test
    fun `leaving from the deepest step drops every entry the flow pushed`() {
        val stack = stackOf(
            AuthRoute.MethodPicker,
            AuthRoute.Success,
            AuthRoute.MfaEnrollment.SelectFactor,
            AuthRoute.MfaEnrollment.ConfigureSms,
            AuthRoute.MfaEnrollment.VerifyFactor,
        )

        stack.exitMfaEnrollment()

        assertThat(stack.toList())
            .containsExactly(AuthRoute.MethodPicker, AuthRoute.Success)
            .inOrder()
    }

    /**
     * Entered, left, entered again: the flow's entries need not be one unbroken run at the top, and
     * a pop loop that stops at the first non-step leaves the earlier ones stranded underneath.
     * Truncating to the lowest step is what makes the exit independent of how the stack got there.
     */
    @Test
    fun `leaving drops the flow's entries wherever they sit on the stack`() {
        val stack = stackOf(
            AuthRoute.Success,
            AuthRoute.MfaEnrollment.SelectFactor,
            AuthRoute.MfaChallenge,
            AuthRoute.MfaEnrollment.ConfigureSms,
        )

        stack.exitMfaEnrollment()

        assertThat(stack.toList()).containsExactly(AuthRoute.Success)
    }

    /**
     * A single-factor configuration resolves its start step to a `Configure…` step, so the flow's
     * lowest entry is not [AuthRoute.MfaEnrollment.SelectFactor] — see [mfaEnrollmentStartStep].
     */
    @Test
    fun `leaving works when the flow never started on SelectFactor`() {
        val stack = stackOf(
            AuthRoute.Success,
            AuthRoute.MfaEnrollment.ConfigureSms,
            AuthRoute.MfaEnrollment.VerifyFactor,
        )

        stack.exitMfaEnrollment()

        assertThat(stack.toList()).containsExactly(AuthRoute.Success)
    }

    /**
     * `onComplete` and `onSkip` are both reachable more than once — a second tap in the same frame,
     * or a completion racing a skip — and the second call must not eat the destination the first
     * one returned to.
     */
    @Test
    fun `leaving a flow already left changes nothing`() {
        val stack = stackOf(AuthRoute.MethodPicker, AuthRoute.Success)

        stack.exitMfaEnrollment()
        stack.exitMfaEnrollment()

        assertThat(stack.toList())
            .containsExactly(AuthRoute.MethodPicker, AuthRoute.Success)
            .inOrder()
    }

    /**
     * Nothing under the flow to return to: truncating would empty the stack, and `NavDisplay`
     * throws `IllegalArgumentException: NavDisplay backstack cannot be empty` from recomposition
     * rather than from the call that emptied it. Same convention as `resetBackStackTo`, which this
     * delegates to, so the size is checked at every snapshot write rather than only at the end.
     */
    @Test
    fun `leaving never empties the stack, even momentarily`() {
        val stack = stackOf(
            AuthRoute.MfaEnrollment.SelectFactor,
            AuthRoute.MfaEnrollment.ConfigureSms,
        )
        val sizes = mutableListOf<Int>()

        Snapshot.observe(writeObserver = { sizes += stack.size }) { stack.exitMfaEnrollment() }

        assertThat(sizes).isNotEmpty()
        assertThat(sizes.min()).isAtLeast(1)
        assertThat(stack.toList()).containsExactly(AuthRoute.Success)
    }

    /**
     * The second, worse instance of the same defect. A step whose user has gone signs itself out of
     * the flow from composition; popping one entry there hands the step below the same null user,
     * which pops again, so the flow eats the stack one recomposition at a time until nothing is
     * left to pop and the user is stranded on a step that renders nothing. Leaving in one write
     * cannot cascade.
     */
    @Test
    fun `a step with no signed-in user leaves the flow rather than draining the stack`() {
        `when`(mockAuth.currentUser).thenReturn(null)

        composeTestRule.setContent {
            SignedOutFlowHost(
                AuthRoute.MfaEnrollment.SelectFactor,
                AuthRoute.MfaEnrollment.ConfigureSms,
            )
        }
        composeTestRule.waitForIdle()

        assertThat(backStackKeys()).containsExactly(AuthRoute.Success)
    }

    // =============================================================================================
    // Harness
    // =============================================================================================

    private fun stackOf(vararg keys: NavKey): NavBackStack<NavKey> =
        NavBackStack<NavKey>().apply { addAll(keys) }

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
     * synchronously with a fake secret, for the duration of [block]. A `MockedStatic` is scoped to
     * `use { }`, so every action and assertion that depends on the stub — starting the flow,
     * driving it, reading [lastState] — has to run inside [block].
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

    /** Enters [AuthRoute.MfaEnrollment.VerifyFactor] the way `onSendSmsCodeClick` does, minus the
     * real SMS network call. */
    private fun pushVerifyFactorDirectly() {
        composeTestRule.runOnIdle {
            requireNotNull(backStack).navigateToMfaStep(AuthRoute.MfaEnrollment.VerifyFactor)
        }
        composeTestRule.waitForIdle()
    }

    /** Enters [step] the way a host's `onNavigate` does — bypassing the screen's own guards. */
    private fun navigateDirectlyTo(step: AuthRoute.MfaEnrollment.Step) {
        composeTestRule.runOnIdle { requireNotNull(backStack).add(step) }
        composeTestRule.waitForIdle()
    }

    private fun back() {
        composeTestRule.runOnUiThread { requireNotNull(pressBack).invoke() }
        composeTestRule.waitForIdle()
    }

    private fun currentKey(): NavKey? =
        composeTestRule.runOnIdle { backStack?.lastOrNull() }

    /** The keys on the stack, bottom to top. */
    private fun backStackKeys(): List<NavKey> =
        composeTestRule.runOnIdle { backStack?.toList().orEmpty() }

    @Composable
    private fun MfaFlowHost(configuration: MfaConfiguration, startStep: AuthRoute.MfaEnrollment.Step) {
        val stack = rememberNavBackStack(startStep)
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        val flowState = rememberMfaEnrollmentFlowState()
        SideEffect {
            backStack = stack
            pressBack = dispatcher?.let { { it.onBackPressed() } }
        }

        NavDisplay(
            backStack = stack,
            onBack = { stack.popOrNull() },
            // Transitions would keep two MFA destinations composed at once, which has nothing to
            // do with the routing under test.
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            predictivePopTransitionSpec = { _ ->
                EnterTransition.None togetherWith ExitTransition.None
            },
            entryProvider = entryProvider {
                mfaEnrollmentDestinations(
                    backStack = stack,
                    configuration = configuration,
                    authConfiguration = null,
                    authUI = authUI,
                    flowState = flowState,
                    content = { state -> lastState = state },
                    onComplete = {},
                    onSkip = {},
                    onError = {},
                )
            },
        )
    }

    /**
     * The same graph as [MfaFlowHost], started on [initialSteps] and with an
     * [AuthRoute.Success] entry registered so the exit's stack-would-be-empty fallback resolves.
     */
    @Composable
    private fun SignedOutFlowHost(vararg initialSteps: AuthRoute.MfaEnrollment.Step) {
        val stack = rememberNavBackStack(*initialSteps)
        SideEffect { backStack = stack }

        NavDisplay(
            backStack = stack,
            onBack = { stack.popOrNull() },
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            predictivePopTransitionSpec = { _ ->
                EnterTransition.None togetherWith ExitTransition.None
            },
            entryProvider = entryProvider {
                entry<AuthRoute.Success> { Text(text = "left-the-flow") }
                mfaEnrollmentDestinations(
                    backStack = stack,
                    configuration = twoFactorConfiguration(),
                    authConfiguration = null,
                    authUI = authUI,
                    flowState = rememberMfaEnrollmentFlowState(),
                    content = { state -> lastState = state },
                    onComplete = {},
                    onSkip = {},
                    onError = {},
                )
            },
        )
    }

    private companion object {
        const val TYPED_PHONE_NUMBER = "5551234567"
        const val FAKE_SHARED_SECRET = "JBSWY3DPEHPK3PXP"
        const val FAKE_QR_URL = "otpauth://totp/test-issuer:user%40example.com?secret=JBSWY3DPEHPK3PXP"
    }
}
