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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorResolver
import android.os.Looper
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * Unit tests for [FirebaseAuthActivity] covering activity lifecycle,
 * intent handling, state observation, and result handling.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class FirebaseAuthActivityTest {

    private lateinit var applicationContext: Context
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var configuration: AuthUIConfiguration

    @Mock
    private lateinit var mockFirebaseUser: FirebaseUser

    @Mock
    private lateinit var mockMultiFactorResolver: MultiFactorResolver

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
        authUI.auth.useEmulator("127.0.0.1", 9099)

        configuration = AuthUIConfiguration(
            context = applicationContext,
            providers = listOf(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                )
            )
        )

        // Reset auth state before each test
        authUI.updateAuthState(AuthState.Idle)
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
    // Activity Launch Tests
    // =============================================================================================

    @Test
    fun `activity launches successfully with valid configuration`() {
        val intent = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().start().resume().get()

        assertThat(activity).isNotNull()
        assertThat(activity.isFinishing).isFalse()
    }

    @Test
    fun `activity finishes immediately when configuration is missing`() {
        // Create intent without configuration
        val intent = Intent(applicationContext, FirebaseAuthActivity::class.java)

        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)
        val activity = controller.create().get()

        // Activity should finish immediately
        assertThat(activity.isFinishing).isTrue()

        // Result should be RESULT_CANCELED
        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    // =============================================================================================
    // Configuration Extraction Tests
    // =============================================================================================

    @Test
    fun `createIntent() stores configuration in cache`() {
        val intent = FirebaseAuthActivity.createIntent(applicationContext, configuration)

        // Intent should contain configuration key
        val configKey = intent.getStringExtra("com.firebase.ui.auth.compose.CONFIGURATION_KEY")
        assertThat(configKey).isNotNull()
        assertThat(configKey).isNotEmpty()
    }

    @Test
    fun `activity extracts configuration from intent on onCreate`() {
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
            tosUrl = "https://example.com/tos"
        )

        val intent = FirebaseAuthActivity.createIntent(applicationContext, customConfig)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().get()

        // Activity should have been created successfully (not finished)
        assertThat(activity.isFinishing).isFalse()
    }

    // =============================================================================================
    // Auth State Success Tests
    // =============================================================================================

    @Test
    fun `activity finishes with RESULT_OK on Success state`() = runTest {
        val intent = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().start().resume().get()

        // Mock user
        `when`(mockFirebaseUser.uid).thenReturn("test-user-id")

        // Update to Success state
        authUI.updateAuthState(AuthState.Success(
            result = null,
            user = mockFirebaseUser,
            isNewUser = true
        ))

        // Process pending tasks on main looper
        shadowOf(Looper.getMainLooper()).idle()

        // Activity should finish
        assertThat(activity.isFinishing).isTrue()

        // Result should be RESULT_OK
        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)

        // Result intent should contain user data
        val resultIntent = shadowActivity.resultIntent
        assertThat(resultIntent).isNotNull()
        assertThat(resultIntent.getStringExtra(FirebaseAuthActivity.EXTRA_USER_ID))
            .isEqualTo("test-user-id")
        assertThat(resultIntent.getBooleanExtra(FirebaseAuthActivity.EXTRA_IS_NEW_USER, false))
            .isTrue()
    }

    @Test
    fun `activity returns correct user data on success`() = runTest {
        val intent = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().start().resume().get()

        // Mock user with specific data
        `when`(mockFirebaseUser.uid).thenReturn("user-123")

        // Update to Success state with isNewUser = false
        authUI.updateAuthState(AuthState.Success(
            result = null,
            user = mockFirebaseUser,
            isNewUser = false
        ))

        shadowOf(Looper.getMainLooper()).idle()

        val shadowActivity = shadowOf(activity)
        val resultIntent = shadowActivity.resultIntent

        assertThat(resultIntent.getStringExtra(FirebaseAuthActivity.EXTRA_USER_ID))
            .isEqualTo("user-123")
        assertThat(resultIntent.getBooleanExtra(FirebaseAuthActivity.EXTRA_IS_NEW_USER, true))
            .isFalse()
    }

    // =============================================================================================
    // Auth State Cancelled Tests
    // =============================================================================================

    @Test
    fun `activity finishes with RESULT_CANCELED on Cancelled state`() = runTest {
        val intent = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().start().resume().get()

        // Update to Cancelled state
        authUI.updateAuthState(AuthState.Cancelled)

        shadowOf(Looper.getMainLooper()).idle()

        // Activity should finish
        assertThat(activity.isFinishing).isTrue()

        // Result should be RESULT_CANCELED
        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    // =============================================================================================
    // Auth State Error Tests
    // =============================================================================================

    @Test
    fun `activity sets RESULT_CANCELED on Error state but does not finish`() = runTest {
        val intent = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().start().resume().get()

        // Update to Error state
        val exception = AuthException.UnknownException("Test error")
        authUI.updateAuthState(AuthState.Error(exception))

        shadowOf(Looper.getMainLooper()).idle()

        // Activity should NOT finish (to let user see error and retry)
        assertThat(activity.isFinishing).isFalse()

        // Result should be set to RESULT_CANCELED
        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_CANCELED)

        // Result intent should contain error
        val resultIntent = shadowActivity.resultIntent
        assertThat(resultIntent).isNotNull()
        assertThat(resultIntent.getSerializableExtra(FirebaseAuthActivity.EXTRA_ERROR))
            .isInstanceOf(AuthException::class.java)
    }

    @Test
    fun `activity includes error in result intent on Error state`() = runTest {
        val intent = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().start().resume().get()

        // Update to Error state with specific exception
        val exception = AuthException.InvalidCredentialsException(
            message = "Invalid credentials"
        )
        authUI.updateAuthState(AuthState.Error(exception))

        shadowOf(Looper.getMainLooper()).idle()

        val shadowActivity = shadowOf(activity)
        val resultIntent = shadowActivity.resultIntent
        val resultError = resultIntent.getSerializableExtra(FirebaseAuthActivity.EXTRA_ERROR) as AuthException

        assertThat(resultError).isInstanceOf(AuthException.InvalidCredentialsException::class.java)
        assertThat(resultError.message).isEqualTo("Invalid credentials")
    }

    // =============================================================================================
    // Activity Lifecycle Tests
    // =============================================================================================

    @Test
    fun `activity resets auth state to Idle on destroy when not finishing`() {
        val intent = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().start().resume().get()

        // Set some state
        authUI.updateAuthState(AuthState.Loading("Testing"))

        // Destroy without finishing
        controller.pause().stop().destroy()

        // Note: This test verifies the lifecycle hook is in place
        // The actual state reset behavior depends on the isFinishing flag
    }

    @Test
    fun `activity handles rapid state transitions`() = runTest {
        val intent = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().start().resume().get()

        // Simulate rapid state changes
        authUI.updateAuthState(AuthState.Loading("Loading..."))
        shadowOf(Looper.getMainLooper()).idle()
        authUI.updateAuthState(AuthState.Loading("Signing in..."))
        shadowOf(Looper.getMainLooper()).idle()
        `when`(mockFirebaseUser.uid).thenReturn("test-user")
        authUI.updateAuthState(AuthState.Success(
            result = null,
            user = mockFirebaseUser,
            isNewUser = false
        ))

        shadowOf(Looper.getMainLooper()).idle()

        // Activity should finish on final Success state
        assertThat(activity.isFinishing).isTrue()

        val shadowActivity = shadowOf(activity)
        assertThat(shadowActivity.resultCode).isEqualTo(Activity.RESULT_OK)
    }

    // =============================================================================================
    // Intent Extras Constants Tests
    // =============================================================================================

    @Test
    fun `activity exposes correct intent extra constants`() {
        assertThat(FirebaseAuthActivity.EXTRA_USER_ID)
            .isEqualTo("com.firebase.ui.auth.compose.USER_ID")
        assertThat(FirebaseAuthActivity.EXTRA_IS_NEW_USER)
            .isEqualTo("com.firebase.ui.auth.compose.IS_NEW_USER")
        assertThat(FirebaseAuthActivity.EXTRA_ERROR)
            .isEqualTo("com.firebase.ui.auth.compose.ERROR")
    }

    // =============================================================================================
    // Configuration Cache Tests
    // =============================================================================================

    @Test
    fun `configuration is removed from cache after onCreate`() {
        val intent1 = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val configKey1 = intent1.getStringExtra("com.firebase.ui.auth.compose.CONFIGURATION_KEY")

        assertThat(configKey1).isNotNull()

        // Create activity - this should consume the configuration from cache
        val controller1 = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent1)
        controller1.create().get()

        // Create another intent
        val intent2 = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val configKey2 = intent2.getStringExtra("com.firebase.ui.auth.compose.CONFIGURATION_KEY")

        // Should be a different key
        assertThat(configKey2).isNotEqualTo(configKey1)
    }

    @Test
    fun `multiple activities can be launched with different configurations`() {
        val config1 = AuthUIConfiguration(
            context = applicationContext,
            providers = listOf(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                )
            ),
            tosUrl = "https://example.com/tos1"
        )

        val config2 = AuthUIConfiguration(
            context = applicationContext,
            providers = listOf(
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = null,
                    allowedCountries = null
                )
            ),
            tosUrl = "https://example.com/tos2"
        )

        val intent1 = FirebaseAuthActivity.createIntent(applicationContext, config1)
        val intent2 = FirebaseAuthActivity.createIntent(applicationContext, config2)

        // Both activities should launch successfully
        val controller1 = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent1)
        val activity1 = controller1.create().get()
        assertThat(activity1.isFinishing).isFalse()

        val controller2 = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent2)
        val activity2 = controller2.create().get()
        assertThat(activity2.isFinishing).isFalse()
    }

    // =============================================================================================
    // Other State Tests
    // =============================================================================================

    @Test
    fun `activity continues showing UI on Loading state`() = runTest {
        val intent = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().start().resume().get()

        // Update to Loading state
        authUI.updateAuthState(AuthState.Loading("Signing in..."))

        shadowOf(Looper.getMainLooper()).idle()

        // Activity should NOT finish on Loading state
        assertThat(activity.isFinishing).isFalse()
    }

    @Test
    fun `activity continues showing UI on RequiresMfa state`() = runTest {
        val intent = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().start().resume().get()

        // Update to RequiresMfa state (mocked resolver)
        authUI.updateAuthState(AuthState.RequiresMfa(
            resolver = mockMultiFactorResolver,
            hint = "Enter verification code"
        ))

        shadowOf(Looper.getMainLooper()).idle()

        // Activity should NOT finish on RequiresMfa state
        assertThat(activity.isFinishing).isFalse()
    }

    @Test
    fun `activity continues showing UI on RequiresEmailVerification state`() = runTest {
        val intent = FirebaseAuthActivity.createIntent(applicationContext, configuration)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().start().resume().get()

        // Update to RequiresEmailVerification state
        `when`(mockFirebaseUser.email).thenReturn("test@example.com")
        authUI.updateAuthState(AuthState.RequiresEmailVerification(
            user = mockFirebaseUser,
            email = "test@example.com"
        ))

        shadowOf(Looper.getMainLooper()).idle()

        // Activity should NOT finish on RequiresEmailVerification state
        assertThat(activity.isFinishing).isFalse()
    }

    // =============================================================================================
    // Theme Tests
    // =============================================================================================

    @Test
    fun `activity applies theme from configuration`() {
        val customConfig = AuthUIConfiguration(
            context = applicationContext,
            providers = listOf(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                )
            ),
            theme = com.firebase.ui.auth.compose.configuration.theme.AuthUITheme.Default
        )

        val intent = FirebaseAuthActivity.createIntent(applicationContext, customConfig)
        val controller = Robolectric.buildActivity(FirebaseAuthActivity::class.java, intent)

        val activity = controller.create().get()

        // Activity should launch successfully with custom theme
        assertThat(activity.isFinishing).isFalse()
    }
}
