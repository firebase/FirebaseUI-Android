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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.ui.screens.AuthRoute
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
 * Covers the recovery from `totpSecret`/`totpQrCodeUrl` being lost to Activity recreation — the
 * loss [MfaEnrollmentFlowStateRestorationTest] establishes.
 *
 * Drives the hosted flow to [AuthRoute.MfaEnrollment.VerifyFactor] for TOTP with a live secret,
 * recreates the Activity via [StateRestorationTester], and asserts the executed recovery:
 * [TotpMultiFactorGenerator.generateSecret] is called again, the user lands back on
 * [AuthRoute.MfaEnrollment.ConfigureTotp] with a *new* secret and QR code rather than the stale
 * one, and [MfaEnrollmentContentState.error] explains why.
 *
 * Guards the regression where that loss dead-ended in `onVerifyClick` throwing
 * `"No TOTP secret available"` with nowhere to go.
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

    private var navController: NavHostController? = null
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
        navController = null
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

        // Held pending: a completed Task would resolve in the same idle pass as the recomposition.
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

            // Sanity on the pre-recreation state, so a fixture mistake cannot pass as a loss.
            assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp.routePattern)
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isEqualTo(FIRST_QR_URL)
            totpStatic.verify(
                { TotpMultiFactorGenerator.generateSecret(mockSession) },
                times(1),
            )

            composeTestRule.runOnIdle { requireNotNull(lastState).onContinueToVerifyClick() }
            composeTestRule.waitForIdle()
            assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor.routePattern)

            restorationTester.emulateSavedInstanceStateRestore()
            composeTestRule.waitForIdle()

            // Observed before the pending fetch resolves: VerifyFactor has already popped back.
            assertThat(requireNotNull(flowState).totpSecret.value).isNull()
            assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp.routePattern)
            assertThat(requireNotNull(lastState).isLoading).isTrue()

            // (a) generateSecret was called again — exactly once more, not on every recomposition.
            totpStatic.verify(
                { TotpMultiFactorGenerator.generateSecret(mockSession) },
                times(2),
            )

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
     * The single-allowed-factor variant of the test above: [AuthRoute.MfaEnrollment.ConfigureTotp]
     * **is** the `NavHost`'s `startDestination`, with no entry beneath it.
     *
     * With nothing below it, a `popBackStack()` that overshoots or that leaves `VerifyFactor` in
     * place shows up as a wrong route or a non-root stack, which [isAtBackStackRoot] catches.
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

        // Held pending, as in the two-factor test, so the null totpSecret is observed.
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

            // Sanity: landed directly on ConfigureTotp, at the back-stack root, with a live secret.
            assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp.routePattern)
            assertThat(isAtBackStackRoot()).isTrue()
            assertThat(requireNotNull(lastState).totpQrCodeUrl).isEqualTo(FIRST_QR_URL)
            totpStatic.verify(
                { TotpMultiFactorGenerator.generateSecret(mockSession) },
                times(1),
            )

            composeTestRule.runOnIdle { requireNotNull(lastState).onContinueToVerifyClick() }
            composeTestRule.waitForIdle()
            assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.VerifyFactor.routePattern)

            restorationTester.emulateSavedInstanceStateRestore()
            composeTestRule.waitForIdle()

            // With no SelectFactor entry, back-to-ConfigureTotp and back-to-root are one claim.
            assertThat(requireNotNull(flowState).totpSecret.value).isNull()
            assertThat(currentRoute()).isEqualTo(AuthRoute.MfaEnrollment.ConfigureTotp.routePattern)
            assertThat(isAtBackStackRoot()).isTrue()
            assertThat(requireNotNull(lastState).isLoading).isTrue()

            // (a) generateSecret was called again — exactly once more, not on every recomposition.
            totpStatic.verify(
                { TotpMultiFactorGenerator.generateSecret(mockSession) },
                times(2),
            )

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

    @Composable
    private fun MfaFlowHost(
        configuration: MfaConfiguration,
        startStep: AuthRoute.MfaEnrollment.Step,
    ) {
        val controller = rememberNavController()
        val state = rememberMfaEnrollmentFlowState()
        SideEffect {
            navController = controller
            flowState = state
        }

        NavHost(
            navController = controller,
            startDestination = startStep.routePattern,
            // Transitions would keep two MFA destinations composed at once.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            mfaEnrollmentDestinations(
                navController = controller,
                configuration = configuration,
                authConfiguration = null,
                authUI = authUI,
                flowState = state,
                content = { contentState -> lastState = contentState },
                onComplete = {},
                onSkip = {},
                onError = {},
            )
        }
    }

    private fun currentRoute(): String? = composeTestRule.runOnIdle {
        navController?.currentBackStackEntry?.destination?.route
    }

    /**
     * Whether the current back-stack entry has nothing beneath it — true only when it is the
     * `NavHost`'s `startDestination` itself, with no earlier entry to pop to.
     */
    private fun isAtBackStackRoot(): Boolean = composeTestRule.runOnIdle {
        navController?.previousBackStackEntry == null
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
