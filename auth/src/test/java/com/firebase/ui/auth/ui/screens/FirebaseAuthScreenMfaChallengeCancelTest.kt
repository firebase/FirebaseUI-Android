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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.MultiFactorSession
import com.google.firebase.auth.TotpMultiFactorInfo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins what cancelling the MFA challenge does to the flow underneath it.
 *
 * **What these two tests do not discriminate on.** `onCancel` publishes [AuthState.Cancelled] and
 * *then* pops the challenge. Both tests here reach the challenge from a stack of `[SignIn(null)]`,
 * and on that shape the pop is invisible: dropping it would leave
 * the [AuthState.Cancelled] branch observing [AuthRoute.MfaChallenge], which is never a start step,
 * so its reset fires — but `resetBackStackTo` adds before it trims, and the `SignIn(null)` it adds
 * is `==` to the entry already there, so that entry never leaves composition and its saved state
 * survives (see `resetBackStackTo`'s clause 5). Both tests pass with the pop removed.
 *
 * That is a property of *this* stack shape, not of the pop. On a stack whose start entry carries an
 * argument — `[SignIn("bob@x.com")]`, which the email recovery really does build — the reset pushes
 * a differently-keyed `SignIn(null)`, the old entry leaves the stack, and the address vanishes. The
 * pop is what stops that, and `FirebaseAuthScreenEmailRecoveryTest`'s `cancelling an MFA challenge
 * keeps the address the recovery carried` is the test that fails without it.
 *
 * Ordering of the two writes before the pop is unobservable, and not merely because Robolectric
 * cannot see it: `updateAuthState` is `_authStateFlow.value = …` and the resolver clear is a
 * snapshot-state write, both synchronous inside one click handler, and the only reader of either is
 * `LaunchedEffect(observedAuthState)` behind a `collectAsState`, which cannot run until the handler
 * returns. The pop rides on that same gap — it lands before the collector reads the stack.
 *
 * So this file asserts the outcomes that *are* observable on this shape, and asserts them against
 * the **live** composition rather than against captured state left over from an earlier frame —
 * which is the one thing the first version of it got wrong.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthScreenMfaChallengeCancelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockResolver: MultiFactorResolver

    @Mock
    private lateinit var mockSession: MultiFactorSession

    @Mock
    private lateinit var mockTotpHint: TotpMultiFactorInfo

    private lateinit var authUI: FirebaseAuthUI

    private var challengeState: MfaChallengeContentState? = null
    private var cancelledCount = 0

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
        )!!
        `when`(mockFirebaseAuth.app).thenReturn(app)
        authUI = FirebaseAuthUI.create(app, mockFirebaseAuth)

        `when`(mockResolver.session).thenReturn(mockSession)
        `when`(mockResolver.hints).thenReturn(listOf<MultiFactorInfo>(mockTotpHint))
        `when`(mockTotpHint.factorId).thenReturn("totp")
        `when`(mockTotpHint.uid).thenReturn("totp-factor-uid")
    }

    @After
    fun tearDown() {
        challengeState = null
        cancelledCount = 0
        FirebaseAuthUI.clearInstanceCache()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Cancelling leaves the challenge and puts the flow back on its start step, exactly once.
     *
     * The email step is asserted through the **live semantics tree**, not through a captured
     * `EmailAuthContentState`. A capture is set by whichever frame composed the slot and is never
     * unset, so it reports that the step composed *at some point* — which stays true both with the
     * challenge still on screen and with a later reset having moved the flow somewhere else
     * entirely. That is what made the first version of this test unable to fail. `challengeState`
     * is still a capture, but it is cleared immediately before the action, so it answers "did the
     * challenge compose again after the cancel?" rather than "ever?".
     */
    @Test
    fun `cancelling the challenge returns to the start step`() {
        start()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD)
            .performTextInput(TYPED_EMAIL)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()

        composeTestRule.runOnIdle { authUI.updateAuthState(AuthState.RequiresMfa(mockResolver)) }
        composeTestRule.waitForIdle()
        val challenge = requireNotNull(composeTestRule.runOnIdle { challengeState })
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertDoesNotExist()

        composeTestRule.runOnIdle {
            challengeState = null
            challenge.onCancelClick()
        }
        composeTestRule.waitForIdle()

        // The destination on screen when everything has settled is the email start step, and the
        // challenge is not composed again.
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD).assertIsDisplayed()
        assertThat(composeTestRule.runOnIdle { challengeState }).isNull()
        // The host was told, once.
        assertThat(cancelledCount).isEqualTo(1)
        // And the step it returned to is the one the user had been using: the flow does not hand
        // back a start step that lost what was typed into it. (See the class KDoc for why this
        // survives on this stack shape even without the pop — the shape that discriminates is
        // FirebaseAuthScreenEmailRecoveryTest's.)
        composeTestRule.onNodeWithText(TYPED_EMAIL).assertIsDisplayed()
    }

    /**
     * Cancelling twice does not tell the host twice, and does not leave a second challenge behind.
     * The second `RequiresMfa` has to re-add the challenge — the guard at the `RequiresMfa` branch
     * is a top-of-stack test, and after the first cancellation the challenge is not on the stack
     * at all.
     */
    @Test
    fun `the challenge can be raised and cancelled again`() {
        start()

        repeat(2) { round ->
            composeTestRule.runOnIdle {
                challengeState = null
                authUI.updateAuthState(AuthState.RequiresMfa(mockResolver))
            }
            composeTestRule.waitForIdle()
            val challenge = requireNotNull(composeTestRule.runOnIdle { challengeState }) {
                "round $round did not compose the challenge"
            }

            composeTestRule.runOnIdle {
                challengeState = null
                challenge.onCancelClick()
            }
            composeTestRule.waitForIdle()

            assertThat(composeTestRule.runOnIdle { challengeState }).isNull()
            composeTestRule.onNodeWithTag(FirebaseAuthTestTags.SignIn.EMAIL_FIELD)
                .assertIsDisplayed()
            assertThat(cancelledCount).isEqualTo(round + 1)
        }
    }

    private fun start() {
        val configuration = authUIConfiguration {
            context = ApplicationProvider.getApplicationContext()
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
            }
            isCredentialManagerEnabled = false
        }

        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = { cancelledCount++ },
                mfaChallengeContent = { state -> challengeState = state },
            )
        }
        composeTestRule.waitForIdle()
    }

    private companion object {
        const val TYPED_EMAIL = "user+tag@example.com"
    }
}
