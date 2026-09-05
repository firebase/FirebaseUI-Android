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

package com.firebase.ui.auth.ui.screens.phone

import android.content.Context
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.popOrNull
import com.firebase.ui.auth.util.CountryUtils
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers moving the phone flow's steps — [AuthRoute.Phone.EnterPhoneNumber] and
 * [AuthRoute.Phone.EnterVerificationCode] — onto real navigation destinations.
 *
 * The unit under test is [phoneAuthDestinations], the entry-provider extension
 * [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen] installs, hosted here in a bare `NavDisplay`
 * so the back stack can be read directly — the same shape `MfaEnrollmentRouteNavigationTest` uses
 * for the enrolment flow. [PhoneAuthHostDestinationsTest] pins the host's own use of it.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class PhoneAuthRouteNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var authUI: FirebaseAuthUI

    private var backStack: NavBackStack<NavKey>? = null
    private var lastState: PhoneAuthContentState? = null
    private var pressBack: (() -> Unit)? = null
    private val reportedErrors = mutableListOf<AuthException>()

    @Before
    fun setUp() {
        FirebaseAuthUI.clearInstanceCache()
        applicationContext = ApplicationProvider.getApplicationContext()
        FirebaseApp.getApps(applicationContext).forEach { it.delete() }
        val app = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )!!
        mockAuth = mock(FirebaseAuth::class.java)
        `when`(mockAuth.app).thenReturn(app)
        authUI = FirebaseAuthUI.create(app, mockAuth)
    }

    @After
    fun tearDown() {
        backStack = null
        lastState = null
        pressBack = null
        reportedErrors.clear()
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    // =============================================================================================
    // Each step is a destination of its own
    // =============================================================================================

    @Test
    fun `a sent code pushes code entry, leaving number entry underneath`() {
        start()

        sendCode("verification-id-1")

        assertThat(backStackKeys()).containsExactly(
            AuthRoute.Phone.EnterPhoneNumber,
            AuthRoute.Phone.EnterVerificationCode,
        ).inOrder()
        assertThat(requireNotNull(lastState).step).isEqualTo(PhoneAuthStep.EnterVerificationCode)
    }

    @Test
    fun `back from code entry returns to number entry`() {
        start()
        sendCode("verification-id-1")

        back()

        assertThat(backStackKeys()).containsExactly(AuthRoute.Phone.EnterPhoneNumber)
        assertThat(requireNotNull(lastState).step).isEqualTo(PhoneAuthStep.EnterPhoneNumber)
    }

    /**
     * The step returned to re-runs the auth-state effect on the emission it left with, so the move
     * that effect already made must not repeat and bounce the user straight back.
     */
    @Test
    fun `back from code entry does not bounce forward on the state it left with`() {
        start()
        sendCode("verification-id-1")

        back()
        // Two frames of settling: a bounce arrives from a LaunchedEffect, not from the pop.
        composeTestRule.waitForIdle()
        composeTestRule.waitForIdle()

        assertThat(backStackKeys()).containsExactly(AuthRoute.Phone.EnterPhoneNumber)
    }

    @Test
    fun `a resend of the same code does not stack a second code-entry entry`() {
        start()
        sendCode("verification-id-1")
        sendCode("verification-id-2")

        assertThat(backStackKeys()).containsExactly(
            AuthRoute.Phone.EnterPhoneNumber,
            AuthRoute.Phone.EnterVerificationCode,
        ).inOrder()
    }

    @Test
    fun `every declared phone step is reachable directly`() {
        start()

        AuthRoute.Phone.steps.forEach { step ->
            navigateDirectlyTo(step)
            assertThat(currentKey()).isEqualTo(step)
            assertThat(requireNotNull(lastState).step).isEqualTo(step.expectedContentStep())
        }
    }

    // =============================================================================================
    // What must outlive the step being left
    // =============================================================================================

    /**
     * The step being left is disposed along with everything it held in composition, so the number
     * typed into it has to live in [PhoneAuthFlowState] to still be there on the way back.
     */
    @Test
    fun `the typed number and verification id survive a round trip through code entry`() {
        start()
        typePhoneNumber(TYPED_PHONE_NUMBER)
        sendCode("verification-id-1")
        assertThat(requireNotNull(lastState).fullPhoneNumber).contains(TYPED_PHONE_NUMBER)

        back()
        assertThat(requireNotNull(lastState).phoneNumber).isEqualTo(TYPED_PHONE_NUMBER)

        sendCode("verification-id-2")
        assertThat(requireNotNull(lastState).step).isEqualTo(PhoneAuthStep.EnterVerificationCode)
        assertThat(requireNotNull(lastState).fullPhoneNumber).contains(TYPED_PHONE_NUMBER)
    }

    /**
     * The country picked on number entry is half of the number code entry confirms back to the
     * user, and it is the one field the un-hosted screen never saved either.
     */
    @Test
    fun `the country picked on number entry is the one code entry formats with`() {
        start()
        typePhoneNumber(TYPED_PHONE_NUMBER)
        composeTestRule.runOnIdle {
            requireNotNull(lastState).onCountrySelected(
                requireNotNull(CountryUtils.findByCountryCode(NON_DEFAULT_COUNTRY_CODE))
            )
        }
        composeTestRule.waitForIdle()

        sendCode("verification-id-1")

        assertThat(requireNotNull(lastState).step).isEqualTo(PhoneAuthStep.EnterVerificationCode)
        assertThat(requireNotNull(lastState).selectedCountry.countryCode)
            .isEqualTo(NON_DEFAULT_COUNTRY_CODE)
        assertThat(requireNotNull(lastState).fullPhoneNumber).startsWith(NON_DEFAULT_DIAL_CODE)
    }

    /** Set on number entry by the emission that moves the flow, and read on code entry. */
    @Test
    fun `the resend countdown set on number entry is the one code entry shows`() {
        start(timeout = 60L)

        sendCode("verification-id-1")

        assertThat(requireNotNull(lastState).step).isEqualTo(PhoneAuthStep.EnterVerificationCode)
        assertThat(requireNotNull(lastState).resendTimer).isEqualTo(60)
    }

    /**
     * The typed code is transient to code entry in every other sense, but "change number" clears
     * it on its way out — from the step being disposed, into state the step returned to still
     * holds.
     */
    @Test
    fun `changing the number clears the code typed into the step being left`() {
        start()
        sendCode("verification-id-1")
        composeTestRule.runOnIdle { requireNotNull(lastState).onVerificationCodeChange("123456") }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { requireNotNull(lastState).onChangeNumberClick() }
        composeTestRule.waitForIdle()

        assertThat(currentKey()).isEqualTo(AuthRoute.Phone.EnterPhoneNumber)
        sendCode("verification-id-2")
        assertThat(requireNotNull(lastState).verificationCode).isEmpty()
    }

    /**
     * The attempt is a long-lived collection that stays open past the sent code, so a scope tied to
     * the step that started it would cancel auto-retrieval on the way to code entry.
     */
    @Test
    fun `the verification attempt started on number entry is still live on code entry`() {
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(TaskCompletionSource<AuthResult>().task)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            val credential = mock(PhoneAuthCredential::class.java)
            statics.`when`<PhoneAuthCredential> {
                PhoneAuthProvider.getCredential(any(), any())
            }.thenReturn(credential)
            start()
            typePhoneNumber(TYPED_PHONE_NUMBER)
            sendCodeForReal()
            val callbacks = latestCallbacks(statics)
            codeSent(callbacks, "verification-id-1")
            assertThat(currentKey()).isEqualTo(AuthRoute.Phone.EnterVerificationCode)

            composeTestRule.runOnUiThread { callbacks.onVerificationCompleted(credential) }
            composeTestRule.waitForIdle()

            verify(mockAuth, times(1)).signInWithCredential(any())
        }
    }

    /**
     * The cooldown that rejects a duplicate verification of the same number is recorded on number
     * entry and has to still be there when the user backs out of code entry and taps send again.
     */
    @Test
    fun `the cooldown record survives backing out of code entry`() {
        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            start(timeout = 60L)
            typePhoneNumber(TYPED_PHONE_NUMBER)
            sendCodeForReal()
            codeSent(latestCallbacks(statics), "verification-id-1")
            assertThat(currentKey()).isEqualTo(AuthRoute.Phone.EnterVerificationCode)

            back()
            sendCodeForReal()

            assertThat(reportedErrors.map { it::class.java })
                .contains(AuthException.PhoneVerificationCooldownException::class.java)
        }
    }

    /**
     * The attempt outlives the step that started it, so the step that abandons it is the one that
     * has to be able to cancel it — a late auto-verification must not sign in behind a user who
     * asked to change their number.
     */
    @Test
    fun `changing the number cancels the attempt started on number entry`() {
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(TaskCompletionSource<AuthResult>().task)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            val credential = mock(PhoneAuthCredential::class.java)
            statics.`when`<PhoneAuthCredential> {
                PhoneAuthProvider.getCredential(any(), any())
            }.thenReturn(credential)
            start()
            typePhoneNumber(TYPED_PHONE_NUMBER)
            sendCodeForReal()
            val callbacks = latestCallbacks(statics)
            codeSent(callbacks, "verification-id-1")

            composeTestRule.runOnIdle { requireNotNull(lastState).onChangeNumberClick() }
            composeTestRule.waitForIdle()
            composeTestRule.runOnUiThread { callbacks.onVerificationCompleted(credential) }
            repeat(3) { composeTestRule.waitForIdle() }

            assertThat(currentKey()).isEqualTo(AuthRoute.Phone.EnterPhoneNumber)
            verify(mockAuth, never()).signInWithCredential(any())
        }
    }

    /**
     * A step transition has both steps composed at once, each observing the same auth state off the
     * process-scoped [FirebaseAuthUI], and one auto-verified credential can only be signed in with
     * once. Two screens sharing one [PhoneAuthFlowState] is that overlap, without an animation
     * clock to hold still.
     */
    @Test
    fun `both steps composed at once auto-verify one credential once`() {
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(TaskCompletionSource<AuthResult>().task)
        val credential = mock(PhoneAuthCredential::class.java)
        val configuration = phoneConfiguration(timeout = 0L)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides configuration.stringProvider
            ) {
                val shared = rememberPhoneAuthFlowState(configuration)
                Row {
                    PhoneAuthStep.entries.forEach { step ->
                        PhoneAuthStepUnderTest(configuration, shared, step)
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.SMSAutoVerified(credential)) }
        composeTestRule.waitForIdle()

        verify(mockAuth, times(1)).signInWithCredential(any())
    }

    // =============================================================================================
    // Leaving the flow drops every entry it pushed
    // =============================================================================================

    /**
     * The defect requirement 5 names: every move between steps is a push, so a single pop from
     * code entry strands the user on number entry rather than leaving.
     */
    @Test
    fun `leaving from code entry drops every entry the flow pushed`() {
        val below = AuthRoute.Email.SignIn(KEPT_EMAIL)
        val stack = stackOf(
            AuthRoute.MethodPicker,
            below,
            AuthRoute.Phone.EnterPhoneNumber,
            AuthRoute.Phone.EnterVerificationCode,
        )

        assertThat(stack.exitPhoneAuth()).isTrue()

        assertThat(stack.toList()).containsExactly(AuthRoute.MethodPicker, below).inOrder()
        // Reference equality, not `==`: a reset would put an equal key back rather than leave the
        // entry — and with it the composition state it carries — where it was.
        assertThat(stack[1]).isSameInstanceAs(below)
    }

    /**
     * Entered, left, entered again: the flow's entries need not be one unbroken run at the top, and
     * a pop loop that stops at the first non-step leaves the earlier ones stranded underneath.
     */
    @Test
    fun `leaving drops the flow's entries wherever they sit on the stack`() {
        val stack = stackOf(
            AuthRoute.Success,
            AuthRoute.Phone.EnterPhoneNumber,
            AuthRoute.MfaChallenge,
            AuthRoute.Phone.EnterVerificationCode,
        )

        assertThat(stack.exitPhoneAuth()).isTrue()

        assertThat(stack.toList()).containsExactly(AuthRoute.Success)
    }

    /**
     * `onCancel` is reachable more than once — a second tap in the same frame, or an
     * [AuthState.Cancelled] racing one — and the second call must not eat the destination the first
     * one returned to.
     */
    @Test
    fun `leaving a flow already left changes nothing`() {
        val stack = stackOf(AuthRoute.MethodPicker, AuthRoute.Success)

        assertThat(stack.exitPhoneAuth()).isFalse()

        assertThat(stack.toList())
            .containsExactly(AuthRoute.MethodPicker, AuthRoute.Success)
            .inOrder()
    }

    /**
     * Nothing under the flow to return to, as in a phone-only configuration: truncating would empty
     * the stack, and `NavDisplay` throws `IllegalArgumentException: NavDisplay backstack cannot be
     * empty` from recomposition rather than from the call that emptied it. Reported instead, so the
     * caller decides what replaces the flow.
     */
    @Test
    fun `leaving reports nothing done when the flow is the whole stack`() {
        val stack = stackOf(
            AuthRoute.Phone.EnterPhoneNumber,
            AuthRoute.Phone.EnterVerificationCode,
        )

        assertThat(stack.exitPhoneAuth()).isFalse()

        assertThat(stack.toList()).containsExactly(
            AuthRoute.Phone.EnterPhoneNumber,
            AuthRoute.Phone.EnterVerificationCode,
        ).inOrder()
    }

    /** One write per entry dropped, and never one that leaves nothing on the stack. */
    @Test
    fun `leaving never empties the stack, even momentarily`() {
        val stack = stackOf(
            AuthRoute.Success,
            AuthRoute.Phone.EnterPhoneNumber,
            AuthRoute.Phone.EnterVerificationCode,
        )
        val sizes = mutableListOf<Int>()

        Snapshot.observe(writeObserver = { sizes += stack.size }) { stack.exitPhoneAuth() }

        assertThat(sizes).isNotEmpty()
        assertThat(sizes.min()).isAtLeast(1)
        assertThat(stack.toList()).containsExactly(AuthRoute.Success)
    }

    @Test
    fun `moving to the step already on top writes nothing`() {
        val stack = stackOf(
            AuthRoute.Phone.EnterPhoneNumber,
            AuthRoute.Phone.EnterVerificationCode,
        )
        val writes = mutableListOf<Any>()

        Snapshot.observe(writeObserver = { writes += it }) {
            stack.navigateToPhoneStep(AuthRoute.Phone.EnterVerificationCode)
        }

        assertThat(writes).isEmpty()
        assertThat(stack.toList()).containsExactly(
            AuthRoute.Phone.EnterPhoneNumber,
            AuthRoute.Phone.EnterVerificationCode,
        ).inOrder()
    }

    // =============================================================================================
    // Harness
    // =============================================================================================

    private fun stackOf(vararg keys: NavKey): NavBackStack<NavKey> =
        NavBackStack<NavKey>().apply { addAll(keys) }

    /**
     * The callbacks Firebase was handed by the most recent verification attempt. [PhoneAuthOptions]
     * exposes no accessor, only an obfuscated zero-arg method returning them, so locate it
     * reflectively and assert exactly one such method exists — as
     * [PhoneAuthScreenVerificationLifecycleTest] does.
     */
    private fun latestCallbacks(
        statics: MockedStatic<PhoneAuthProvider>
    ): OnVerificationStateChangedCallbacks {
        val captor = ArgumentCaptor.forClass(PhoneAuthOptions::class.java)
        statics.verify({ PhoneAuthProvider.verifyPhoneNumber(captor.capture()) }, atLeastOnce())
        val candidates = PhoneAuthOptions::class.java.declaredMethods.filter {
            it.parameterCount == 0 &&
                it.returnType == OnVerificationStateChangedCallbacks::class.java
        }
        check(candidates.size == 1) {
            "Expected exactly one zero-arg accessor returning " +
                "OnVerificationStateChangedCallbacks on PhoneAuthOptions, found " +
                "${candidates.size}: $candidates"
        }
        return candidates.single().also { it.isAccessible = true }
            .invoke(captor.allValues.last()) as OnVerificationStateChangedCallbacks
    }

    private fun phoneConfiguration(timeout: Long): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = "US",
                    allowedCountries = null,
                    timeout = timeout,
                )
            )
        }
        isCredentialManagerEnabled = false
    }

    // timeout = 0 keeps the resend countdown at zero, so no 1-second ticking effect is left
    // pending between assertions.
    private fun start(timeout: Long = 0L) {
        val configuration = phoneConfiguration(timeout)
        composeTestRule.setContent { PhoneFlowHost(configuration) }
        composeTestRule.waitForIdle()
    }

    /**
     * The emission Firebase's `onCodeSent` callback ends up publishing, which is the only thing
     * that moves the flow on to code entry.
     */
    private fun sendCode(verificationId: String) {
        composeTestRule.runOnIdle {
            authUI.updateAuthState(
                AuthState.PhoneNumberVerificationRequired(
                    verificationId = verificationId,
                    forceResendingToken = mock(PhoneAuthProvider.ForceResendingToken::class.java),
                )
            )
        }
        composeTestRule.waitForIdle()
    }

    /** The send the user performs, which is what records the cooldown and starts the attempt. */
    private fun sendCodeForReal() {
        composeTestRule.runOnIdle { requireNotNull(lastState).onSendCodeClick() }
        composeTestRule.waitForIdle()
    }

    /** Firebase's `onCodeSent`, which is what publishes the emission that moves the flow. */
    private fun codeSent(
        callbacks: OnVerificationStateChangedCallbacks,
        verificationId: String,
    ) {
        composeTestRule.runOnUiThread {
            callbacks.onCodeSent(
                verificationId,
                mock(PhoneAuthProvider.ForceResendingToken::class.java),
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun typePhoneNumber(value: String) {
        composeTestRule.runOnIdle { requireNotNull(lastState).onPhoneNumberChange(value) }
        composeTestRule.waitForIdle()
    }

    /** Enters [step] the way a host's `onNavigate` does — bypassing the screen's own guards. */
    private fun navigateDirectlyTo(step: AuthRoute.Phone.Step) {
        composeTestRule.runOnIdle { requireNotNull(backStack).add(step) }
        composeTestRule.waitForIdle()
    }

    private fun back() {
        composeTestRule.runOnUiThread { requireNotNull(pressBack).invoke() }
        composeTestRule.waitForIdle()
    }

    private fun currentKey(): NavKey? = composeTestRule.runOnIdle { backStack?.lastOrNull() }

    /** The keys on the stack, bottom to top. */
    private fun backStackKeys(): List<NavKey> =
        composeTestRule.runOnIdle { backStack?.toList().orEmpty() }

    private fun AuthRoute.Phone.Step.expectedContentStep(): PhoneAuthStep = when (this) {
        AuthRoute.Phone.EnterPhoneNumber -> PhoneAuthStep.EnterPhoneNumber
        AuthRoute.Phone.EnterVerificationCode -> PhoneAuthStep.EnterVerificationCode
    }

    @Composable
    private fun PhoneAuthStepUnderTest(
        configuration: AuthUIConfiguration,
        flowState: PhoneAuthFlowState,
        step: PhoneAuthStep,
    ) {
        PhoneAuthScreen(
            context = applicationContext,
            configuration = configuration,
            authUI = authUI,
            onSuccess = {},
            onError = {},
            onCancel = {},
            step = step,
            onNavigateToStep = {},
            onNavigateBack = {},
            flowState = flowState,
            content = { state -> lastState = state },
        )
    }

    @Composable
    private fun PhoneFlowHost(configuration: AuthUIConfiguration) {
        val stack = rememberNavBackStack(AuthRoute.Phone.EnterPhoneNumber)
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        val flowState = rememberPhoneAuthFlowState(configuration)
        SideEffect {
            backStack = stack
            pressBack = dispatcher?.let { { it.onBackPressed() } }
        }

        // FirebaseAuthScreen provides this itself; a bare NavDisplay has to.
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides configuration.stringProvider
        ) {
            NavDisplay(
                backStack = stack,
                onBack = { stack.popOrNull() },
                // Transitions would keep two phone destinations composed at once, which has
                // nothing to do with the routing under test.
                transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                predictivePopTransitionSpec = { _ ->
                    EnterTransition.None togetherWith ExitTransition.None
                },
                entryProvider = entryProvider {
                    phoneAuthDestinations(
                        backStack = stack,
                        context = applicationContext,
                        configuration = configuration,
                        authUI = authUI,
                        flowState = flowState,
                        content = { state -> lastState = state },
                        onCancel = {},
                        onError = { reportedErrors += it },
                    )
                },
            )
        }
    }

    private companion object {
        const val TYPED_PHONE_NUMBER = "5555550123"
        const val KEPT_EMAIL = "keep@example.com"
        const val NON_DEFAULT_COUNTRY_CODE = "GB"
        const val NON_DEFAULT_DIAL_CODE = "+44"
    }
}
