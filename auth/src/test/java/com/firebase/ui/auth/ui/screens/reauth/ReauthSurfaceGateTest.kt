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

package com.firebase.ui.auth.ui.screens.reauth

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.DefaultAuthContentTransform
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.phone.rememberPhoneAuthFlowState
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The reauthentication surface exists on exactly one condition: a resolved [ReauthSurface].
 *
 * A saved back stack can carry an [AuthRoute.Reauth] entry into a process with no armed request —
 * the state machine publishes `Interrupted` and pops it, but the entry composes first. This pins
 * what it composes: nothing. Driving [ReauthSceneStrategy] and [reauthDestinations] directly is
 * what makes the unarmed entry reachable at all; `FirebaseAuthScreen` never leaves one standing
 * long enough for a test to observe it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ReauthSurfaceGateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    private lateinit var authUI: FirebaseAuthUI

    /** Readable labels for the request ids the test mints, so a failure names the two requests. */
    private val labels = mutableMapOf<String, String>()

    /** `<request the entry key names>/<request whose data it was handed>`, one per composition. */
    private val handedOut = mutableListOf<String>()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        FirebaseAuthUI.clearInstanceCache()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach { app -> app.delete() }
        val defaultApp = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
        `when`(mockFirebaseAuth.app).thenReturn(defaultApp)
        authUI = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach { app -> app.delete() }
    }

    /** So the absence asserted below is a real absence, not a matcher that never matches. */
    @Test
    fun `an armed request composes the sheet`() {
        setContent(armed = true)

        composeTestRule.onAllNodes(SHEET, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `a reauth entry with no armed request composes no sheet and no scrim`() {
        setContent(armed = false)

        // The sheet owns the scrim and both live in the sheet's own window, so no sheet node means
        // neither is on screen; the flow underneath is what the user is left looking at.
        composeTestRule.onAllNodes(SHEET, useUnmergedTree = true).assertCountEquals(0)
        composeTestRule.onNodeWithTag(FLOW_TAG).assertIsDisplayed()
    }

    /**
     * The entry renders the armed request, so a key naming a different one must render nothing:
     * every write it would offer goes to the id the key names, which is no longer the armed one.
     */
    @Test
    fun `an entry keyed to a request the surface no longer holds is handed nothing`() {
        val stale = request("stale")
        val armed = request("armed")

        composeTestRule.setContent {
            Harness(reauthState = armed, entryRequestId = stale.requestId, useSlot = true)
        }
        composeTestRule.waitForIdle()

        assertThat(handedOut).isEmpty()
    }

    /**
     * `FirebaseAuthScreen` derives the surface in composition but re-arms the stack in a
     * `LaunchedEffect`, so a new request leaves the old entry keyed to the old id for a
     * composition. This reproduces that ordering: no composition may pair mismatched ids.
     */
    @Test
    fun `the stack lagging the state by a composition hands the entry nothing`() {
        val first = request("first")
        val second = request("second")
        val state = mutableStateOf<AuthState.Reauthentication?>(first)

        composeTestRule.setContent {
            Harness(reauthState = state.value, armedRequest = state, useSlot = true)
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { state.value = second }
        composeTestRule.waitForIdle()

        assertThat(handedOut.filter { it.substringBefore('/') != it.substringAfter('/') }).isEmpty()
    }

    private fun setContent(armed: Boolean) {
        val state = if (armed) AuthState.Reauthentication.Required(passwordUser()) else null
        composeTestRule.setContent { Harness(state) }
        composeTestRule.waitForIdle()
    }

    /** A fresh request whose reason is [label], so what the entry is handed is identifiable. */
    private fun request(label: String): AuthState.Reauthentication.Required =
        AuthState.Reauthentication.Required(passwordUser(), reason = label)
            .also { labels[it.requestId] = label }

    /**
     * `FirebaseAuthScreen`'s reauthentication wiring, with the armed state under test control.
     *
     * @param entryRequestId The id the reauthentication entry is keyed to. Defaults to the armed
     * request's, which is what the host's steady state looks like.
     * @param armedRequest When given, the stack is re-armed from it in a `LaunchedEffect`, the way
     * the host does — which is what puts a composition between a new request and its entry.
     * @param useSlot Installs a `reauthContent` slot that records what the entry hands it. The
     * slot is the only path to the entry's `updateReauthentication` writes, so nothing recorded
     * means no write was offered.
     */
    @Composable
    private fun Harness(
        reauthState: AuthState.Reauthentication?,
        entryRequestId: String = reauthState?.requestId ?: "unarmed-request",
        armedRequest: MutableState<AuthState.Reauthentication?>? = null,
        useSlot: Boolean = false,
    ) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val configuration = remember {
            authUIConfiguration {
                this.context = context
                providers {
                    provider(
                        AuthProvider.Email(
                            emailLinkActionCodeSettings = null,
                            passwordValidationRules = emptyList(),
                        )
                    )
                }
            }
        }
        val surface = rememberUpdatedState(reauthState.toReauthSurface(configuration))
        val backStack = rememberNavBackStack(
            AuthRoute.MethodPicker,
            AuthRoute.Reauth(
                requestId = entryRequestId,
                userUid = "uid-password",
                step = AuthRoute.MethodPicker,
            ),
        )
        if (armedRequest != null) {
            // FirebaseAuthScreen's `Reauthentication.Required` branch, reduced to the re-arming.
            val armedId = armedRequest.value?.requestId
            LaunchedEffect(armedId) {
                if (armedId != null && backStack.armedReauth()?.requestId != armedId) {
                    backStack.clearReauth()
                    backStack.add(
                        AuthRoute.Reauth(
                            requestId = armedId,
                            userUid = "uid-password",
                            step = AuthRoute.MethodPicker,
                        )
                    )
                }
            }
        }
        val slot: (@Composable (ReauthContentState) -> Unit)? = if (useSlot) {
            { state ->
                val keyed = labels[backStack.armedReauth()?.requestId] ?: "unlabelled"
                val handed = state.reason
                SideEffect { handedOut += "$keyed/$handed" }
            }
        } else {
            null
        }
        val strategy = remember {
            ReauthSceneStrategy(
                surface = surface,
                onDismissRequest = {},
                transitionSpec = DefaultAuthContentTransform,
                popTransitionSpec = DefaultAuthContentTransform,
            )
        }
        val phoneFlowState = rememberPhoneAuthFlowState(configuration)
        val reauthFlowState = rememberReauthFlowState()
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides configuration.stringProvider,
        ) {
            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(strategy),
                onBack = {},
                entryProvider = entryProvider {
                    entry<AuthRoute.MethodPicker> {
                        Text(text = "flow underneath", modifier = Modifier.testTag(FLOW_TAG))
                    }
                    reauthDestinations(
                        backStack = backStack,
                        authUI = authUI,
                        activity = null,
                        context = context,
                        configuration = configuration,
                        stringProvider = DefaultAuthUIStringProvider(context),
                        surface = surface,
                        reauthFlowState = reauthFlowState,
                        phoneFlowState = phoneFlowState,
                        emailContent = null,
                        phoneContent = null,
                        mfaChallengeContent = null,
                        reauthContent = slot,
                        customMethodPickerLayout = null,
                        onDismiss = {},
                        onLeaveStep = {},
                    )
                },
            )
        }
    }

    private fun passwordUser(): FirebaseUser {
        val providerInfo = mock(UserInfo::class.java)
        `when`(providerInfo.providerId).thenReturn("password")
        val user = mock(FirebaseUser::class.java)
        `when`(user.uid).thenReturn("uid-password")
        `when`(user.providerData).thenReturn(listOf(providerInfo))
        return user
    }

    private companion object {
        const val FLOW_TAG = "flow_underneath"

        /** The `ModalBottomSheet`'s own node: Material 3 gives the sheet this pane title. */
        val SHEET = SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Bottom Sheet")
    }
}
