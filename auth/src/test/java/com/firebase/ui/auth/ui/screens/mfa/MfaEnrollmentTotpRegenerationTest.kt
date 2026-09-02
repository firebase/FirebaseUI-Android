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

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.StateRestorationTester
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
import com.google.android.gms.tasks.TaskCompletionSource
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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Proves the other half of the TOTP-loss fix [MfaEnrollmentFlowStateRestorationTest] documents:
 * `totpSecret`/`totpQrCodeUrl` are genuinely lost to Activity recreation (there is no `Saver` to
 * write for `com.google.firebase.auth.TotpSecret` without reaching into obfuscated SDK internals —
 * see [MfaEnrollmentFlowState]), and that loss must not become a dead end — `onVerifyClick`
 * throwing `"No TOTP secret available"` with nowhere to go.
 *
 * Drives the hosted flow to [AuthRoute.MfaEnrollment.VerifyFactor] for TOTP with a live secret,
 * recreates the Activity via [StateRestorationTester], and asserts the actual, executed recovery:
 * [TotpMultiFactorGenerator.generateSecret] is called again, the user lands back on
 * [AuthRoute.MfaEnrollment.ConfigureTotp] with a *new* secret and QR code rather than the stale
 * one, and [MfaEnrollmentContentState.error] explains why — nothing here is asserted from
 * prediction.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class MfaEnrollmentTotpRegenerationTest {

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
    private var flowState: MfaEnrollmentFlowState? = null
    private var lastState: MfaEnrollmentContentState? = null

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
        `when`(mockUser.uid).thenReturn("mfa-totp-regen-user")
        `when`(mockUser.email).thenReturn("user@example.com")
        `when`(mockUser.multiFactor).thenReturn(mockMultiFactor)
        `when`(mockMultiFactor.enrolledFactors).thenReturn(emptyList())
        authUI = FirebaseAuthUI.create(app, mockAuth)
    }

    @After
    fun tearDown() {
        backStack = null
        flowState = null
        lastState = null
        FirebaseAuthUI.clearInstanceCache()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    @Test
    fun `losing the TOTP secret to recreation regenerates it and bounces VerifyFactor back to ConfigureTotp`() {
        val mockSession = mock(MultiFactorSession::class.java)
        val firstSecret = mock(FirebaseTotpSecret::class.java)
        val secondSecret = mock(FirebaseTotpSecret::class.java)
        `when`(mockMultiFactor.session).thenReturn(Tasks.forResult(mockSession))
        `when`(firstSecret.sharedSecretKey).thenReturn(FIRST_SHARED_SECRET)
        `when`(firstSecret.generateQrCodeUrl(any(), any())).thenReturn(FIRST_QR_URL)
        `when`(secondSecret.sharedSecretKey).thenReturn(SECOND_SHARED_SECRET)
        `when`(secondSecret.generateQrCodeUrl(any(), any())).thenReturn(SECOND_QR_URL)

        // The second generateSecret call — the regeneration this test is actually about — is
        // held pending rather than completing immediately, specifically so there is a real,
        // observable moment where recreation has already dropped totpSecret but the fetch that
        // replaces it has not yet resolved. Without that, an already-completed Task (as Firebase
        // Tasks used elsewhere in this suite are) resolves within the same idle pass that runs
        // the recomposition, and "totpSecret.value is null post-recreation" would only ever be
        // inferred, not actually observed.
        val secondSecretSource = TaskCompletionSource<FirebaseTotpSecret>()

        mockStatic(TotpMultiFactorGenerator::class.java).use { totpStatic ->
            totpStatic.`when`<Task<FirebaseTotpSecret>> {
                TotpMultiFactorGenerator.generateSecret(mockSession)
            }.thenReturn(Tasks.forResult(firstSecret), secondSecretSource.task)

            val restorationTester = StateRestorationTester(composeTestRule)
            restorationTester.setContent {
                MfaFlowHost(twoFactorConfiguration(), AuthRoute.MfaEnrollment.SelectFactor)
            }
            composeTestRule.waitForIdle()

            composeTestRule.runOnIdle { requireNotNull(lastState).onFactorSelected(MfaFactor.Totp) }
            composeTestRule.waitForIdle()

            // Sanity: the original secret was actually fetched and is what ConfigureTotp shows,
            // so what follows is provably about recreation's effect and not a fixture mistake.
            assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp)
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isEqualTo(FIRST_QR_URL)
            totpStatic.verify(
                { TotpMultiFactorGenerator.generateSecret(mockSession) },
                times(1),
            )

            composeTestRule.runOnIdle { requireNotNull(lastState).onContinueToVerifyClick() }
            composeTestRule.waitForIdle()
            assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor)

            restorationTester.emulateSavedInstanceStateRestore()
            composeTestRule.waitForIdle()

            // The loss itself, actually observed rather than inferred: unchanged by this fix,
            // totpSecret is a plain `remember` and is genuinely null right after restore, exactly
            // as MfaEnrollmentFlowStateRestorationTest proves for the SMS-side fields that *don't*
            // survive either. VerifyFactor's own LaunchedEffect(currentStep) has already reacted
            // to that null by queuing the expired-secret message and popping back to
            // ConfigureTotp — real navigation, so it lands there before the still-pending
            // regeneration fetch resolves.
            assertThat(requireNotNull(flowState).totpSecret.value).isNull()
            assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp)
            assertThat(requireNotNull(lastState).isLoading).isTrue()

            // (a) generateSecret was called again — exactly once more, not on every recomposition.
            totpStatic.verify(
                { TotpMultiFactorGenerator.generateSecret(mockSession) },
                times(2),
            )

            // Let the regeneration actually resolve.
            secondSecretSource.setResult(secondSecret)
            composeTestRule.waitForIdle()

            // (b) The regenerated secret is a genuinely new one, not the stale pre-recreation QR.
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isEqualTo(SECOND_QR_URL)
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isNotEqualTo(FIRST_QR_URL)
            assertThat(requireNotNull(flowState).totpSecret.value).isNotNull()
            // (c) The user is told why: this was not a silent recovery.
            assertThat(requireNotNull(lastState).error).isEqualTo(TOTP_SECRET_EXPIRED_MESSAGE)
        }
    }

    /**
     * The single-allowed-factor variant of the test above. A TOTP-only [MfaConfiguration]
     * resolves its start step straight to [AuthRoute.MfaEnrollment.ConfigureTotp] —
     * [mfaEnrollmentStartStep] — so this flow never visits [AuthRoute.MfaEnrollment.SelectFactor]
     * at all: `ConfigureTotp` **is** the back stack's only initial entry, with nothing beneath it.
     *
     * That makes the recovery's `onNavigateBack()` step worth checking on its own: with a
     * `SelectFactor` entry in the stack (the two-factor case above), landing back on
     * `ConfigureTotp` is unsurprising — it is simply the next entry down. Here there is nothing
     * below `ConfigureTotp` to reason about, so a real defect (e.g. `popBackStack()` overshooting,
     * or leaving `VerifyFactor` in place) would show up as either a wrong route or a stack that
     * still has something beneath the current entry. [isAtBackStackRoot] asserts there is nothing
     * beneath it, on top of the same recovery sequence
     * [MfaEnrollmentTotpRegenerationTest] already proves for the two-factor case.
     *
     * This is also the harshest back-stack site in the flow. The pop comes from an effect on a
     * precondition failure, on the first frame after restore, on a stack whose
     * only entry is the one being popped from — and `NavDisplay` throws
     * `IllegalArgumentException: NavDisplay backstack cannot be empty` from *recomposition* rather
     * than from the mutation, so an unguarded `removeLastOrNull()` here would surface as a crash
     * naming neither this file nor `MfaEnrollmentScreen`. Hence the explicit
     * [backStackKeys] assertion below: the stack must still hold exactly `ConfigureTotp`, not
     * merely report it as its top.
     */
    @Test
    fun `losing the TOTP secret to recreation regenerates it and bounces back to ConfigureTotp when TOTP is the only allowed factor`() {
        val mockSession = mock(MultiFactorSession::class.java)
        val firstSecret = mock(FirebaseTotpSecret::class.java)
        val secondSecret = mock(FirebaseTotpSecret::class.java)
        `when`(mockMultiFactor.session).thenReturn(Tasks.forResult(mockSession))
        `when`(firstSecret.sharedSecretKey).thenReturn(FIRST_SHARED_SECRET)
        `when`(firstSecret.generateQrCodeUrl(any(), any())).thenReturn(FIRST_QR_URL)
        `when`(secondSecret.sharedSecretKey).thenReturn(SECOND_SHARED_SECRET)
        `when`(secondSecret.generateQrCodeUrl(any(), any())).thenReturn(SECOND_QR_URL)

        // Same rationale as the two-factor test: the regeneration fetch is held pending so
        // "totpSecret.value is null post-recreation" is actually observed, not inferred from an
        // already-resolved Task.
        val secondSecretSource = TaskCompletionSource<FirebaseTotpSecret>()

        mockStatic(TotpMultiFactorGenerator::class.java).use { totpStatic ->
            totpStatic.`when`<Task<FirebaseTotpSecret>> {
                TotpMultiFactorGenerator.generateSecret(mockSession)
            }.thenReturn(Tasks.forResult(firstSecret), secondSecretSource.task)

            val configuration = totpOnlyConfiguration()
            val restorationTester = StateRestorationTester(composeTestRule)
            restorationTester.setContent {
                MfaFlowHost(configuration, mfaEnrollmentStartStep(configuration))
            }
            composeTestRule.waitForIdle()

            // Sanity: the flow landed directly on ConfigureTotp with a live secret — no
            // SelectFactor visit needed, and it is genuinely the back-stack root.
            assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp)
            assertThat(isAtBackStackRoot()).isTrue()
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isEqualTo(FIRST_QR_URL)
            totpStatic.verify(
                { TotpMultiFactorGenerator.generateSecret(mockSession) },
                times(1),
            )

            composeTestRule.runOnIdle { requireNotNull(lastState).onContinueToVerifyClick() }
            composeTestRule.waitForIdle()
            assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor)

            restorationTester.emulateSavedInstanceStateRestore()
            composeTestRule.waitForIdle()

            // The loss, actually observed: totpSecret is null right after restore, exactly as the
            // two-factor case. VerifyFactor's LaunchedEffect(currentStep) has already reacted by
            // queuing the expired-secret message and popping back — and with no SelectFactor entry
            // in this stack, "back to ConfigureTotp" and "back to the stack root" are the same
            // assertion, which isAtBackStackRoot makes explicit rather than assumed.
            assertThat(requireNotNull(flowState).totpSecret.value).isNull()
            assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp)
            assertThat(isAtBackStackRoot()).isTrue()
            // Exactly one entry, and it is ConfigureTotp: the guarded pop declined to mutate a
            // single-entry stack rather than emptying it.
            assertThat(backStackKeys())
                .containsExactly(AuthRoute.MfaEnrollment.ConfigureTotp)
            assertThat(requireNotNull(lastState).isLoading).isTrue()

            // (a) generateSecret was called again — exactly once more, not on every recomposition.
            totpStatic.verify(
                { TotpMultiFactorGenerator.generateSecret(mockSession) },
                times(2),
            )

            // Let the regeneration actually resolve.
            secondSecretSource.setResult(secondSecret)
            composeTestRule.waitForIdle()

            // (b) The regenerated secret is a genuinely new one, not the stale pre-recreation QR.
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isEqualTo(SECOND_QR_URL)
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isNotEqualTo(FIRST_QR_URL)
            assertThat(requireNotNull(flowState).totpSecret.value).isNotNull()
            // (c) The user is told why: this was not a silent recovery.
            assertThat(requireNotNull(lastState).error).isEqualTo(TOTP_SECRET_EXPIRED_MESSAGE)
        }
    }

    /**
     * The same recovery, but with [AuthRoute.MfaEnrollment.VerifyFactor] as the back stack's
     * **only** entry — the shape a host produces by entering that step directly through the public
     * `AuthSuccessUiContext.onNavigate`, and the shape a restored back stack can produce on its
     * own.
     *
     * This is the one place in the flow where the TOTP-loss redirect has nothing to pop to, and it
     * is only reachable through a real Activity recreation: `totpSecret` is deliberately a plain
     * `remember` (see [MfaEnrollmentFlowState]), while `selectedFactor` is `rememberSaveable`, so
     * "TOTP was chosen but the secret is gone" cannot be constructed without one. Popping the last
     * entry makes `NavDisplay` throw `IllegalArgumentException: NavDisplay backstack cannot be
     * empty` — and throw it from *recomposition*, naming neither this file nor
     * `MfaEnrollmentScreen`.
     *
     * With nothing underneath, the redirect stays put: the user is left on `VerifyFactor` with the
     * regenerated secret rather than bounced.
     */
    @Test
    fun `the TOTP-loss redirect does not empty a back stack whose only entry is VerifyFactor`() {
        val mockSession = mock(MultiFactorSession::class.java)
        val firstSecret = mock(FirebaseTotpSecret::class.java)
        val secondSecret = mock(FirebaseTotpSecret::class.java)
        `when`(mockMultiFactor.session).thenReturn(Tasks.forResult(mockSession))
        `when`(firstSecret.sharedSecretKey).thenReturn(FIRST_SHARED_SECRET)
        `when`(firstSecret.generateQrCodeUrl(any(), any())).thenReturn(FIRST_QR_URL)
        `when`(secondSecret.sharedSecretKey).thenReturn(SECOND_SHARED_SECRET)
        `when`(secondSecret.generateQrCodeUrl(any(), any())).thenReturn(SECOND_QR_URL)

        mockStatic(TotpMultiFactorGenerator::class.java).use { totpStatic ->
            totpStatic.`when`<Task<FirebaseTotpSecret>> {
                TotpMultiFactorGenerator.generateSecret(mockSession)
            }.thenReturn(Tasks.forResult(firstSecret), Tasks.forResult(secondSecret))

            val restorationTester = StateRestorationTester(composeTestRule)
            restorationTester.setContent {
                MfaFlowHost(totpOnlyConfiguration(), AuthRoute.MfaEnrollment.VerifyFactor)
            }
            composeTestRule.waitForIdle()

            // The pre-recreation state a real flow would have left behind: TOTP chosen, a live
            // secret in hand, sitting on VerifyFactor with nothing beneath it. selectedFactor is
            // rememberSaveable and survives the restore; totpSecret is not and does not.
            composeTestRule.runOnIdle {
                val state = requireNotNull(flowState)
                state.selectedFactor.value = MfaFactor.Totp
                state.totpSecret.value =
                    com.firebase.ui.auth.mfa.TotpSecret.from(firstSecret)
                state.totpQrCodeUrl.value = FIRST_QR_URL
            }
            composeTestRule.waitForIdle()
            assertThat(backStackKeys())
                .containsExactly(AuthRoute.MfaEnrollment.VerifyFactor)

            restorationTester.emulateSavedInstanceStateRestore()
            composeTestRule.waitForIdle()

            // The redirect fired (selectedFactor survived as Totp, the secret did not) and found
            // nothing to pop to. The stack still holds exactly one entry, and it is still
            // VerifyFactor — not an empty list, and not a crash from NavDisplay.
            assertThat(backStackKeys())
                .containsExactly(AuthRoute.MfaEnrollment.VerifyFactor)
            assertThat(currentKey()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor)
        }
    }

    @Composable
    private fun MfaFlowHost(
        configuration: MfaConfiguration,
        startStep: AuthRoute.MfaEnrollment.Step,
    ) {
        val stack = rememberNavBackStack(startStep)
        val state = rememberMfaEnrollmentFlowState()
        SideEffect {
            backStack = stack
            flowState = state
        }

        NavDisplay(
            backStack = stack,
            onBack = { stack.popOrNull() },
            // Transitions would keep two MFA destinations composed at once, which has nothing to
            // do with the state-restoration behavior under test.
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
                    flowState = state,
                    content = { contentState -> lastState = contentState },
                    onComplete = {},
                    onSkip = {},
                    onError = {},
                )
            },
        )
    }

    private fun currentKey(): NavKey? = composeTestRule.runOnIdle {
        backStack?.lastOrNull()
    }

    /** The keys on the stack, bottom to top. */
    private fun backStackKeys(): List<NavKey> =
        composeTestRule.runOnIdle { backStack?.toList().orEmpty() }

    /**
     * Whether the current back-stack entry has nothing beneath it — true only when it is the
     * back stack's only entry, with no earlier entry to pop to.
     */
    private fun isAtBackStackRoot(): Boolean = composeTestRule.runOnIdle {
        backStack?.size == 1
    }

    private fun twoFactorConfiguration() = MfaConfiguration(
        allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),
        requireEnrollment = false,
    )

    private fun totpOnlyConfiguration() = MfaConfiguration(
        allowedFactors = listOf(MfaFactor.Totp),
        requireEnrollment = false,
    )

    private companion object {
        const val FIRST_SHARED_SECRET = "JBSWY3DPEHPK3PXP"
        const val SECOND_SHARED_SECRET = "KRSXG5CTMVRXEZLU"
        const val FIRST_QR_URL = "otpauth://totp/test-issuer:user%40example.com?secret=$FIRST_SHARED_SECRET"
        const val SECOND_QR_URL = "otpauth://totp/test-issuer:user%40example.com?secret=$SECOND_SHARED_SECRET"
    }
}
