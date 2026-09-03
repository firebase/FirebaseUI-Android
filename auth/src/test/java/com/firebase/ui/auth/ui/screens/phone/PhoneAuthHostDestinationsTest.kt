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

import com.firebase.ui.auth.ui.screens.reauth.ReauthFlowState
import com.firebase.ui.auth.ui.screens.reauth.rememberReauthFlowState
import android.content.Context
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.AuthUITransitions
import com.firebase.ui.auth.configuration.DefaultAuthContentTransform
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.AuthSuccessUiContext
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.firebase.ui.auth.ui.screens.authRoute
import com.firebase.ui.auth.ui.screens.popOrNull
import com.firebase.ui.auth.ui.screens.reauth.ReauthSceneStrategy
import com.firebase.ui.auth.ui.screens.reauth.reauthDestinations
import com.firebase.ui.auth.ui.screens.reauth.toReauthSurface
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthProvider
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
 * The phone flow as [FirebaseAuthScreen] itself installs it: the `flowState` it remembers, the
 * `onCancel` it supplies, and the two entries [phoneAuthDestinations] registers on its display.
 *
 * [PhoneAuthRouteNavigationTest] drives the same extension through its own bare `NavDisplay`, so it
 * pins the helpers but not the host's use of them — reverting the production call sites would leave
 * it green. These render the real screen, whose back stack is not reachable from a test, and read
 * the flow's position off what is on screen instead.
 *
 * The reauthentication surface is the phone flow's *other* host, installing the same steps through
 * [com.firebase.ui.auth.ui.screens.reauth.reauthDestinations]. The `ReauthPhoneSheet` harness below
 * drives that one, as `EmailAuthHostDestinationsTest` does for email.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class PhoneAuthHostDestinationsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var authUI: FirebaseAuthUI

    private var pressBack: (() -> Unit)? = null
    private var uiContext: AuthSuccessUiContext? = null
    private val enteredRoutes = mutableListOf<AuthRoute?>()

    /** The reauthentication harness's own stack, for the assertions that are about keys. */
    private var reauthBackStack: NavBackStack<NavKey>? = null

    /**
     * The sheet's phase holder. A reauthentication phase is the request's own state now, not
     * something published to the public flow, so a test that wants to stand at a particular step
     * puts it here — which is where the sink's fold would have put it.
     */
    private var reauthHolder: ReauthFlowState? = null

    /** The request the reauthentication harness armed, which its own emissions have to carry. */
    private var reauthRequest: AuthState.Reauthentication.Request? = null

    private var reauthDismissals = 0

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
        pressBack = null
        uiContext = null
        enteredRoutes.clear()
        reauthBackStack = null
        reauthRequest = null
        reauthDismissals = 0
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach {
            try {
                it.delete()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * The headline defect. Code entry shared number entry's back-stack entry, so the only thing
     * left for the system back gesture to pop was the phone flow itself.
     */
    @Test
    fun `system back from code entry returns to number entry`() {
        start()
        enterPhoneFlow()
        sendCode()
        assertAtCodeEntry()

        back()

        assertAtNumberEntry()
        assertStillInTheFlow()
    }

    /**
     * The other half of "one destination each": the configured screen transition has to run for the
     * move between the two steps, which it cannot when neither step is entered.
     */
    @Test
    fun `moving to code entry animates as its own destination`() {
        start()
        enterPhoneFlow()

        sendCode()

        // Entering the flow at all proves the recorder works, so the missing code-entry route
        // below is a real absence rather than a spec that never ran.
        assertThat(enteredRoutes).contains(AuthRoute.Phone.EnterPhoneNumber)
        assertThat(enteredRoutes).contains(AuthRoute.Phone.EnterVerificationCode)
    }

    /**
     * `AuthSuccessUiContext.onNavigate` takes any [AuthRoute], code entry included. Registered but
     * never navigated to, that entry used to resolve to a screen whose own state still said number
     * entry, so the step the host asked for was silently swapped for the other one.
     */
    @Test
    fun `a host navigating to code entry lands on code entry`() {
        start()
        signIn()

        composeTestRule.runOnIdle {
            requireNotNull(uiContext).onNavigate(AuthRoute.Phone.EnterVerificationCode)
        }
        composeTestRule.waitForIdle()

        assertAtCodeEntry()
    }

    /**
     * Code entry's own back arrow leaves the flow rather than stepping back through it — that is
     * what the "change number" control is for. Leaving from there has two entries to drop, and a
     * single pop drops one.
     */
    @Test
    fun `leaving from code entry drops number entry with it`() {
        start()
        enterPhoneFlow()
        sendCode()
        assertAtCodeEntry()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.VerificationCode.BACK_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(FirebaseAuthTestTags.PhoneNumber.PHONE_NUMBER_FIELD)
            .assertCountEquals(0)
    }

    /**
     * "Change number" retracts the attempt it is abandoning, and that retraction runs through the
     * host's own abandonment reset on its way back — which used to send a multi-provider
     * configuration all the way out to the method picker.
     */
    @Test
    fun `changing the number returns to number entry rather than the method picker`() {
        start()
        enterPhoneFlow()
        sendCode()
        assertAtCodeEntry()

        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.VerificationCode.CHANGE_PHONE_NUMBER_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()

        assertAtNumberEntry()
        assertStillInTheFlow()
    }

    /** The number typed before the code was sent is what code entry confirms back to the user. */
    @Test
    fun `the typed number survives the move to code entry`() {
        start()
        enterPhoneFlow()
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.PhoneNumber.PHONE_NUMBER_FIELD)
            .performTextInput(PHONE_NUMBER)

        sendCode()

        assertAtCodeEntry()
        composeTestRule.onNodeWithText(FULL_PHONE_NUMBER, substring = true).assertIsDisplayed()
    }

    /**
     * The reauthentication surface keys every entry to one step, and it renders whatever the key
     * names. A key naming code entry that renders number entry means the key is being ignored.
     */
    @Test
    fun `a reauthentication entry keyed to code entry renders code entry`() {
        startReauthSheet(startStep = AuthRoute.Phone.EnterVerificationCode)

        assertAtCodeEntry()
    }

    /** Sending the code inside reauthentication reaches code entry as an entry of its own. */
    @Test
    fun `sending the code inside reauthentication pushes a second reauthentication entry`() {
        startReauthSheet()
        assertAtNumberEntry()

        sendReauthCode()

        assertThat(reauthBackStack?.toList()).containsExactly(
            AuthRoute.Success,
            AuthRoute.Reauth(REQUEST_ID, REAUTH_UID, AuthRoute.Phone.EnterPhoneNumber),
            AuthRoute.Reauth(REQUEST_ID, REAUTH_UID, AuthRoute.Phone.EnterVerificationCode),
        ).inOrder()
    }

    /**
     * Back from a reauthentication step steps back through the surface while another of its entries
     * is underneath, and dismisses it only when none is. With both phone steps sharing one entry
     * there was never one underneath, so back abandoned the reauthentication.
     */
    @Test
    fun `back from reauthentication code entry returns to number entry`() {
        startReauthSheet()
        sendReauthCode()
        assertAtCodeEntry()

        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.VerificationCode.BACK_BUTTON)
            .performClick()
        composeTestRule.waitForIdle()

        assertAtNumberEntry()
        assertThat(reauthDismissals).isEqualTo(0)
    }

    // =============================================================================================
    // Harness
    // =============================================================================================

    /**
     * Renders the real screen on a configuration offering email *and* phone, so the method picker
     * is the entry underneath the phone flow and "left the flow" is distinguishable from "stepped
     * back inside it".
     */
    private fun start() {
        composeTestRule.setContent { Host() }
        composeTestRule.waitForIdle()
    }

    @Composable
    private fun Host() {
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        SideEffect { pressBack = dispatcher?.let { { it.onBackPressed() } } }

        FirebaseAuthScreen(
            configuration = emailAndPhoneConfiguration(),
            authUI = authUI,
            onSignInSuccess = {},
            onSignInFailure = {},
            onSignInCancelled = {},
            authenticatedContent = { _, context ->
                uiContext = context
                Text(text = "authenticated", modifier = Modifier.testTag(AUTHENTICATED_TAG))
            },
        )
    }

    /** Enters the phone flow the way the method picker does. */
    private fun enterPhoneFlow() {
        composeTestRule.onNodeWithText(PHONE_PROVIDER_LABEL).performClick()
        composeTestRule.waitForIdle()
        assertAtNumberEntry()
    }

    /**
     * The emission Firebase's `onCodeSent` callback ends up publishing, which is the only thing
     * that moves the flow on to code entry.
     */
    private fun sendCode(verificationId: String = "verification-id-1") {
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

    /** Puts the screen on its authenticated destination, where `onNavigate` is reachable. */
    private fun signIn() {
        val user = mock(FirebaseUser::class.java)
        `when`(user.uid).thenReturn("phone-host-user")
        `when`(user.email).thenReturn(null)
        composeTestRule.runOnIdle {
            authUI.updateAuthState(AuthState.Success(result = null, user = user, isNewUser = false))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AUTHENTICATED_TAG).assertIsDisplayed()
    }

    private fun back() {
        composeTestRule.runOnUiThread { requireNotNull(pressBack).invoke() }
        composeTestRule.waitForIdle()
    }

    private fun assertAtNumberEntry() {
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.PhoneNumber.PHONE_NUMBER_FIELD)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(FirebaseAuthTestTags.VerificationCode.CODE_FIELD)
            .assertCountEquals(0)
    }

    private fun assertAtCodeEntry() {
        composeTestRule.onNodeWithTag(FirebaseAuthTestTags.VerificationCode.CODE_FIELD)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(FirebaseAuthTestTags.PhoneNumber.PHONE_NUMBER_FIELD)
            .assertCountEquals(0)
    }

    /** No method picker on screen, so the step move stayed inside the flow. */
    private fun assertStillInTheFlow() {
        composeTestRule.onAllNodesWithTag(FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST)
            .assertCountEquals(0)
    }

    /** Mounts the reauthentication surface with its phone flow open at [startStep]. */
    private fun startReauthSheet(
        startStep: AuthRoute.Destination = AuthRoute.Phone.EnterPhoneNumber,
    ) {
        composeTestRule.setContent { ReauthPhoneSheet(startStep = startStep) }
        composeTestRule.waitForIdle()
    }

    /**
     * The reauthentication surface as an entry on a host's back stack, which is the only way it
     * exists: a `NavDisplay` with one non-reauthentication entry underneath it.
     */
    @Composable
    private fun ReauthPhoneSheet(startStep: AuthRoute.Destination) {
        val config = phoneReauthConfiguration()
        val user = remember {
            val info = mock(UserInfo::class.java)
            `when`(info.providerId).thenReturn(PhoneAuthProvider.PROVIDER_ID)
            mock(FirebaseUser::class.java).also {
                `when`(it.email).thenReturn(null)
                `when`(it.uid).thenReturn(REAUTH_UID)
                `when`(it.providerData).thenReturn(listOf(info))
            }
        }
        val request = remember {
            AuthState.Reauthentication.Request(
                requestId = REQUEST_ID,
                user = user,
                reason = null,
            ).also { reauthRequest = it }
        }
        val backStack = rememberNavBackStack(
            AuthRoute.Success,
            AuthRoute.Reauth(REQUEST_ID, REAUTH_UID, startStep),
        )
        SideEffect { reauthBackStack = backStack }
        val surface = remember {
            mutableStateOf(
                AuthState.Reauthentication.Required(request).toReauthSurface(config)
            )
        }
        val onDismiss: () -> Unit = { reauthDismissals++ }
        val strategy = remember {
            ReauthSceneStrategy(
                surface = surface,
                onDismissRequest = onDismiss,
                transitionSpec = DefaultAuthContentTransform,
                popTransitionSpec = DefaultAuthContentTransform,
            )
        }
        val stringProvider = remember { DefaultAuthUIStringProvider(applicationContext) }
        // Above the display, like the host: a step switch disposes whatever the step it left held.
        val phoneFlowState = rememberPhoneAuthFlowState(config)
        val reauthFlowState = rememberReauthFlowState()
        SideEffect {
            reauthHolder = reauthFlowState
            reauthFlowState.arm(AuthState.Reauthentication.Required(request))
        }
        CompositionLocalProvider(LocalAuthUIStringProvider provides stringProvider) {
            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(strategy),
                onBack = {
                    val top = backStack.lastOrNull()
                    if (top is AuthRoute.Reauth &&
                        backStack.getOrNull(backStack.lastIndex - 1) !is AuthRoute.Reauth
                    ) {
                        onDismiss()
                    } else {
                        backStack.popOrNull()
                    }
                },
                entryProvider = entryProvider {
                    entry<AuthRoute.Success> { Box(modifier = Modifier.fillMaxSize()) {} }
                    reauthDestinations(
                        backStack = backStack,
                        authUI = authUI,
                        activity = null,
                        context = applicationContext,
                        configuration = config,
                        stringProvider = stringProvider,
                        surface = surface,
                        reauthFlowState = reauthFlowState,
                        phoneFlowState = phoneFlowState,
                        emailContent = null,
                        phoneContent = null,
                        mfaChallengeContent = null,
                        reauthContent = null,
                        customMethodPickerLayout = null,
                        onDismiss = onDismiss,
                        onLeaveStep = {
                            if (backStack.getOrNull(backStack.lastIndex - 1) is AuthRoute.Reauth) {
                                backStack.popOrNull()
                            } else {
                                onDismiss()
                            }
                        },
                    )
                },
            )
        }
    }

    /** The reauthentication phase Firebase's `onCodeSent` callback ends up published as. */
    private fun sendReauthCode(verificationId: String = "reauth-verification-id") {
        composeTestRule.runOnIdle {
            requireNotNull(reauthHolder).moveTo(
                AuthState.Reauthentication.PhoneNumberVerificationRequired(
                    request = requireNotNull(reauthRequest),
                    verificationId = verificationId,
                    forceResendingToken = mock(PhoneAuthProvider.ForceResendingToken::class.java),
                )
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun emailAndPhoneConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                )
            )
            // timeout = 0 keeps the resend countdown at zero, so no 1-second ticking effect is
            // left pending between assertions.
            provider(
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = "US",
                    allowedCountries = null,
                    timeout = 0L,
                )
            )
        }
        isCredentialManagerEnabled = false
        // The default fades would keep the destination being left composed alongside its
        // successor, which the "not on screen" assertions above cannot tell from a step that never
        // moved. Recording is what pins each step having a destination of its own to animate to.
        transitions = AuthUITransitions(
            transitionSpec = {
                enteredRoutes += targetState.authRoute()
                EnterTransition.None togetherWith ExitTransition.None
            },
            popTransitionSpec = {
                enteredRoutes += targetState.authRoute()
                EnterTransition.None togetherWith ExitTransition.None
            },
            predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        )
    }

    /** Phone alone, so the surface's own start step is the phone flow's rather than the picker. */
    private fun phoneReauthConfiguration(): AuthUIConfiguration = authUIConfiguration {
        context = applicationContext
        providers {
            provider(
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = "US",
                    allowedCountries = null,
                    timeout = 0L,
                )
            )
        }
        isCredentialManagerEnabled = false
    }.copy(
        isAnonymousUpgradeEnabled = false,
        isCredentialLinkingEnabled = false,
        isNewEmailAccountsAllowed = false,
        isReauthenticationMode = true,
    )

    private companion object {
        const val AUTHENTICATED_TAG = "authenticated-destination"
        const val PHONE_PROVIDER_LABEL = "Sign in with phone"
        const val PHONE_NUMBER = "5555550123"
        const val FULL_PHONE_NUMBER = "+15555550123"
        const val REQUEST_ID = "reauth-request-id"
        const val REAUTH_UID = "reauth-uid"
    }
}
