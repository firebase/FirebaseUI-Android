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

package com.firebase.ui.auth.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [AuthFlowController] covering lifecycle management,
 * intent creation, state observation, and resource disposal.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class AuthFlowControllerTest {

    private lateinit var applicationContext: Context
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var configuration: AuthUIConfiguration

    @Mock
    private lateinit var mockActivity: Activity

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        applicationContext = ApplicationProvider.getApplicationContext()

        // Clear any existing Firebase apps
        FirebaseApp.getApps(applicationContext).forEach { app ->
            app.delete()
        }

        // Initialize default FirebaseApp
        FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )

        authUI = FirebaseAuthUI.getInstance()
        configuration = AuthUIConfiguration(
            context = applicationContext,
            providers = listOf(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                )
            )
        )
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(applicationContext).forEach { app ->
            try {
                app.delete()
            } catch (_: Exception) {
                // Ignore if already deleted
            }
        }
    }

    // =============================================================================================
    // Controller Creation Tests
    // =============================================================================================

    @Test
    fun `createAuthFlow() returns new controller instance`() {
        val controller = authUI.createAuthFlow(configuration)

        assertThat(controller).isNotNull()
        assertThat(controller.isDisposed()).isFalse()
    }

    @Test
    fun `createAuthFlow() returns different instances each time`() {
        val controller1 = authUI.createAuthFlow(configuration)
        val controller2 = authUI.createAuthFlow(configuration)

        // Each call should return a new controller instance
        assertThat(controller1).isNotEqualTo(controller2)
    }

    // =============================================================================================
    // Intent Creation Tests
    // =============================================================================================

    @Test
    fun `createIntent() returns valid Intent`() {
        val controller = authUI.createAuthFlow(configuration)
        val intent = controller.createIntent(applicationContext)

        assertThat(intent).isNotNull()
        assertThat(intent.component?.className).contains("FirebaseAuthActivity")
    }

    @Test
    fun `createIntent() contains configuration key`() {
        val controller = authUI.createAuthFlow(configuration)
        val intent = controller.createIntent(applicationContext)

        // Intent should contain a configuration key extra
        val configKey = intent.getStringExtra("com.firebase.ui.auth.compose.CONFIGURATION_KEY")
        assertThat(configKey).isNotNull()
        assertThat(configKey).isNotEmpty()
    }

    @Test
    fun `createIntent() throws IllegalStateException when disposed`() {
        val controller = authUI.createAuthFlow(configuration)
        controller.dispose()

        try {
            controller.createIntent(applicationContext)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("disposed")
        }
    }

    // =============================================================================================
    // Auth State Flow Tests
    // =============================================================================================

    @Test
    fun `authStateFlow observes state changes from FirebaseAuthUI`() = runTest {
        val controller = authUI.createAuthFlow(configuration)

        // Collect initial state
        val initialState = controller.authStateFlow.first()
        assertThat(initialState).isInstanceOf(AuthState.Idle::class.java)
    }

    @Test
    fun `authStateFlow emits Loading state when updated`() = runTest {
        val controller = authUI.createAuthFlow(configuration)

        // Update state
        authUI.updateAuthState(AuthState.Loading("Testing"))

        // Advance test scheduler to process all pending coroutines
        testScheduler.advanceUntilIdle()

        // Collect first state after update
        val state = controller.authStateFlow.first()

        // Should be Loading state
        assertThat(state).isInstanceOf(AuthState.Loading::class.java)
        assertThat((state as AuthState.Loading).message).isEqualTo("Testing")
    }

    @Test
    fun `authStateFlow throws IllegalStateException when disposed`() = runTest {
        val controller = authUI.createAuthFlow(configuration)
        controller.dispose()

        try {
            controller.authStateFlow.first()
            assertThat(false).isTrue() // Should not reach here
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("disposed")
        }
    }

    // =============================================================================================
    // Cancel Tests
    // =============================================================================================

    @Test
    fun `cancel() updates state to Cancelled`() = runTest {
        val controller = authUI.createAuthFlow(configuration)

        // Cancel the flow
        controller.cancel()

        // Advance test scheduler to process all pending coroutines
        testScheduler.advanceUntilIdle()

        // Collect first state after cancel
        val state = controller.authStateFlow.first()

        // Should be Cancelled state
        assertThat(state).isInstanceOf(AuthState.Cancelled::class.java)
    }

    @Test
    fun `cancel() throws IllegalStateException when disposed`() {
        val controller = authUI.createAuthFlow(configuration)
        controller.dispose()

        try {
            controller.cancel()
            assertThat(false).isTrue() // Should not reach here
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("disposed")
        }
    }

    // =============================================================================================
    // Dispose Tests
    // =============================================================================================

    @Test
    fun `dispose() marks controller as disposed`() {
        val controller = authUI.createAuthFlow(configuration)

        assertThat(controller.isDisposed()).isFalse()

        controller.dispose()

        assertThat(controller.isDisposed()).isTrue()
    }

    @Test
    fun `dispose() can be called multiple times safely`() {
        val controller = authUI.createAuthFlow(configuration)

        controller.dispose()
        controller.dispose() // Should not throw

        assertThat(controller.isDisposed()).isTrue()
    }

    @Test
    fun `dispose() prevents further operations`() {
        val controller = authUI.createAuthFlow(configuration)
        controller.dispose()

        // All operations should throw IllegalStateException
        try {
            controller.createIntent(applicationContext)
            assertThat(false).isTrue()
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("disposed")
        }

        try {
            controller.cancel()
            assertThat(false).isTrue()
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("disposed")
        }
    }

    @Test
    fun `isDisposed() returns correct state`() {
        val controller = authUI.createAuthFlow(configuration)

        assertThat(controller.isDisposed()).isFalse()

        controller.dispose()

        assertThat(controller.isDisposed()).isTrue()
    }

    // =============================================================================================
    // Deprecated Start Method Tests
    // =============================================================================================

    @Suppress("DEPRECATION")
    @Test
    fun `start() launches activity with correct intent`() {
        val controller = authUI.createAuthFlow(configuration)
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()

        controller.start(activity, AuthFlowController.RC_SIGN_IN)

        // Verify an activity was started
        val shadowActivity = org.robolectric.Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity

        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.component?.className).contains("FirebaseAuthActivity")
    }

    @Suppress("DEPRECATION")
    @Test
    fun `start() throws IllegalStateException when disposed`() {
        val controller = authUI.createAuthFlow(configuration)
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()

        controller.dispose()

        try {
            controller.start(activity, AuthFlowController.RC_SIGN_IN)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("disposed")
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `start() uses default request code when not specified`() {
        val controller = authUI.createAuthFlow(configuration)
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()

        controller.start(activity)

        // Verify an activity was started (request code verification is internal)
        val shadowActivity = org.robolectric.Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity

        assertThat(startedIntent).isNotNull()
    }

    // =============================================================================================
    // Thread Safety Tests
    // =============================================================================================

    @Test
    fun `dispose() is thread-safe`() {
        val controller = authUI.createAuthFlow(configuration)

        val threads = List(10) {
            Thread {
                controller.dispose()
            }
        }

        // Start all threads concurrently
        threads.forEach { it.start() }

        // Wait for all threads to complete
        threads.forEach { it.join() }

        // Controller should be disposed exactly once
        assertThat(controller.isDisposed()).isTrue()
    }

    @Test
    fun `multiple controllers can be created and disposed independently`() {
        val controller1 = authUI.createAuthFlow(configuration)
        val controller2 = authUI.createAuthFlow(configuration)
        val controller3 = authUI.createAuthFlow(configuration)

        controller1.dispose()

        // Other controllers should still be usable
        assertThat(controller1.isDisposed()).isTrue()
        assertThat(controller2.isDisposed()).isFalse()
        assertThat(controller3.isDisposed()).isFalse()

        // Can still create intents with non-disposed controllers
        val intent2 = controller2.createIntent(applicationContext)
        val intent3 = controller3.createIntent(applicationContext)

        assertThat(intent2).isNotNull()
        assertThat(intent3).isNotNull()

        controller2.dispose()
        controller3.dispose()

        assertThat(controller2.isDisposed()).isTrue()
        assertThat(controller3.isDisposed()).isTrue()
    }

    // =============================================================================================
    // Configuration Tests
    // =============================================================================================

    @Test
    fun `controller preserves configuration for intent creation`() {
        val customConfig = AuthUIConfiguration(
            context = applicationContext,
            providers = listOf(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                ),
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = null,
                    allowedCountries = null
                )
            ),
            tosUrl = "https://example.com/tos",
            privacyPolicyUrl = "https://example.com/privacy"
        )

        val controller = authUI.createAuthFlow(customConfig)
        val intent = controller.createIntent(applicationContext)

        // Intent should be created successfully with custom config
        assertThat(intent).isNotNull()
        assertThat(intent.component?.className).contains("FirebaseAuthActivity")
    }

    // =============================================================================================
    // Lifecycle Tests
    // =============================================================================================

    @Test
    fun `typical lifecycle - create, start, cancel, dispose`() = runTest {
        val controller = authUI.createAuthFlow(configuration)

        // Create intent
        val intent = controller.createIntent(applicationContext)
        assertThat(intent).isNotNull()

        // Cancel flow
        controller.cancel()

        // Advance test scheduler to process all pending coroutines
        testScheduler.advanceUntilIdle()

        // Verify cancelled state
        val state = controller.authStateFlow.first()
        assertThat(state).isInstanceOf(AuthState.Cancelled::class.java)

        // Dispose
        controller.dispose()
        assertThat(controller.isDisposed()).isTrue()
    }

    @Test
    fun `typical lifecycle - create, start, observe, dispose`() = runTest {
        val controller = authUI.createAuthFlow(configuration)

        // Create intent
        val intent = controller.createIntent(applicationContext)
        assertThat(intent).isNotNull()

        // Update state
        authUI.updateAuthState(AuthState.Loading("Signing in..."))

        // Advance test scheduler to process all pending coroutines
        testScheduler.advanceUntilIdle()

        // Observe state change
        val state = controller.authStateFlow.first()
        assertThat(state).isInstanceOf(AuthState.Loading::class.java)

        // Dispose
        controller.dispose()
        assertThat(controller.isDisposed()).isTrue()
    }
}
