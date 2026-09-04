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

import com.firebase.ui.auth.ReauthScopeProbe
import com.firebase.ui.auth.retryingReauth
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.R
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthScreenReauthIdleResetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    private lateinit var authUI: FirebaseAuthUI

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
        )!!

        `when`(mockFirebaseAuth.app).thenReturn(defaultApp)

        authUI = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach { app -> app.delete() }
    }

    @Test
    fun `wrong password error during reauth does not dismiss the reauth sheet`() {
        val probe = ReauthScopeProbe()
        val mockProviderInfo = mock(UserInfo::class.java)
        `when`(mockProviderInfo.providerId).thenReturn("password")
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("uid-password")
        `when`(mockUser.providerData).thenReturn(listOf(mockProviderInfo))

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
        }

        var capturedError: String? = null
        composeTestRule.setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = { state ->
                    probe.capture()
                    capturedError = state.error
                    Text(text = "Reauth UI", modifier = Modifier.testTag("reauth_marker"))
                }
            )
        }

        // Enter the reauth flow.
        composeTestRule.runOnIdle {
            authUI.pendingReauth.value = AuthState.Reauthentication.Required(mockUser)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_marker").assertIsDisplayed()

        // Wrong password entered inside the reauth flow becomes failure state on the same request.
        composeTestRule.runOnIdle {
            probe.emit(AuthState.Error(Exception("wrong password")))
        }
        composeTestRule.waitForIdle()

        // Custom reauth content owns the error presentation and the request remains active.
        assertThat(capturedError).isNotNull()
        composeTestRule.onNodeWithTag("reauth_marker").assertIsDisplayed()
    }

    /**
     * `FirebaseAuthUI.delete()` signs the user out as its *success* condition, so a successful
     * retry fires the AuthStateListener with a null current user while the request is still in
     * `RetryingOperation`. The listener's stale-state reset used to force `Idle` from every
     * `Reauthentication` phase, which cancelled the coroutine running the operation and left the
     * saved presentation to report `fui_error_reauth_interrupted` — over a deleted account.
     *
     * Screen-level tests mock [FirebaseAuth], so `addAuthStateListener` is inert; the listener is
     * captured off the mock and invoked from inside the retry operation itself, which is how this
     * test reaches that branch at all.
     */
    @Test
    fun `an operation that signs the user out is reported as completed, not interrupted`() {
        val probe = ReauthScopeProbe()
        val mockProviderInfo = mock(UserInfo::class.java)
        `when`(mockProviderInfo.providerId).thenReturn("password")
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("uid-password")
        `when`(mockUser.providerData).thenReturn(listOf(mockProviderInfo))
        `when`(mockUser.isEmailVerified).thenReturn(true)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockUser)

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
        }

        val observed = mutableListOf<AuthState>()
        composeTestRule.setContent {
            LaunchedEffect(Unit) { authUI.authStateFlow().collect { observed.add(it) } }
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                reauthContent = {
                    probe.capture()
                    Text(text = "Reauth UI", modifier = Modifier.testTag("reauth_marker"))
                }
            )
        }
        composeTestRule.waitForIdle()

        val listenerCaptor = ArgumentCaptor.forClass(AuthStateListener::class.java)
        verify(mockFirebaseAuth, atLeastOnce()).addAuthStateListener(listenerCaptor.capture())
        val listeners = listenerCaptor.allValues.toList()

        var operationStarted = false
        var operationCompleted = false
        composeTestRule.runOnIdle {
            authUI.pendingReauth.value = retryingReauth(mockUser) {
                    operationStarted = true
                    // Exactly what a successful delete() does: FirebaseAuth drops the user and
                    // notifies its listeners while the operation is running.
                    `when`(mockFirebaseAuth.currentUser).thenReturn(null)
                    listeners.forEach { it.onAuthStateChanged(mockFirebaseAuth) }
                    operationCompleted = true
                }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reauth_marker").assertIsDisplayed()

        // Credentials accepted for the same user, which drives the request into its retry phase.
        composeTestRule.runOnIdle {
            probe.emit(
                AuthState.Success(
                    result = null,
                    user = mockUser,
                    reauthenticatedUid = "uid-password",
                )
            )
        }
        composeTestRule.waitForIdle()

        val interruptedMessage = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.fui_error_reauth_interrupted)
        assertThat(operationStarted).isTrue()
        assertThat(operationCompleted).isTrue()
        assertThat(observed.filterIsInstance<AuthState.Error>().map { it.exception.message })
            .doesNotContain(interruptedMessage)
    }
}
