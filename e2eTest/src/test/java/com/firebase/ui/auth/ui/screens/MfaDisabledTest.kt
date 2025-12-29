package com.firebase.ui.auth.ui.screens

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.testutil.AUTH_STATE_WAIT_TIMEOUT_MS
import com.firebase.ui.auth.testutil.EmulatorAuthApi
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * E2E tests for MFA disabled functionality.
 *
 * Tests that when isMfaEnabled is false, the Manage MFA button is disabled
 * and attempting to access MFA enrollment shows an AuthCancelledException.
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class MfaDisabledTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var emulatorApi: EmulatorAuthApi

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)

        // Clear any existing Firebase apps
        FirebaseApp.getApps(applicationContext).forEach { app ->
            app.delete()
        }

        // Initialize default FirebaseApp
        val firebaseApp = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )

        authUI = FirebaseAuthUI.getInstance()
        authUI.auth.useEmulator("127.0.0.1", 9099)

        emulatorApi = EmulatorAuthApi(
            projectId = firebaseApp.options.projectId
                ?: throw IllegalStateException("Project ID is required for emulator interactions"),
            emulatorHost = "127.0.0.1",
            emulatorPort = 9099
        )

        // Clear emulator data
        emulatorApi.clearEmulatorData()
    }

    @After
    fun tearDown() {
        // Clean up after each test to prevent test pollution
        FirebaseAuthUI.clearInstanceCache()

        // Clear emulator data
        emulatorApi.clearEmulatorData()
    }

    @Test
    fun `Manage MFA button is disabled when isMfaEnabled is false`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            isMfaEnabled = false  // MFA disabled
            providers {
                provider(AuthProvider.Anonymous)
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
            }
        }

        var currentAuthState: AuthState = AuthState.Idle

        composeTestRule.setContent {
            FirebaseAuthScreen(
                authUI = authUI,
                configuration = configuration,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {}
            )
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Wait for the navigation to settle and UI to be ready
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Sign in anonymously to get to the success screen
        composeTestRule.onNodeWithText(stringProvider.signInAnonymously)
            .assertIsDisplayed  ()
            .performClick()
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to Success
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            currentAuthState is AuthState.Success
        }

        // Wait for UI to update
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Verify the Manage MFA button is displayed but disabled
        composeTestRule.onNodeWithText(stringProvider.manageMfaAction)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun `onManageMfa throws AuthCancelledException when MFA is disabled`() {
        val configuration = authUIConfiguration {
            context = applicationContext
            isMfaEnabled = false  // MFA disabled
            providers {
                provider(AuthProvider.Anonymous)
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
            }
        }

        var currentAuthState: AuthState = AuthState.Idle
        var capturedUiContext: AuthSuccessUiContext? = null

        composeTestRule.setContent {
            FirebaseAuthScreen(
                authUI = authUI,
                configuration = configuration,
                onSignInSuccess = {},
                onSignInFailure = {},
                onSignInCancelled = {},
                authenticatedContent = { _, uiContext ->
                    // Custom content that captures the uiContext
                    capturedUiContext = uiContext
                }
            )
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Wait for the navigation to settle and UI to be ready
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Sign in anonymously to get to the success screen
        composeTestRule.onNodeWithText(stringProvider.signInAnonymously)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to Success
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            currentAuthState is AuthState.Success
        }

        // Now call onManageMfa directly (simulating custom content calling it)
        assertThat(capturedUiContext).isNotNull()
        capturedUiContext?.onManageMfa?.invoke()

        // Wait for auth state to update
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Verify that auth state is now Error with AuthCancelledException
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            currentAuthState is AuthState.Error &&
                    (currentAuthState as AuthState.Error).exception is AuthException.AuthCancelledException
        }

        val errorState = currentAuthState as AuthState.Error
        assertThat(errorState.exception).isInstanceOf(AuthException.AuthCancelledException::class.java)
        assertThat(errorState.exception.message).contains("Multi-factor authentication is disabled")
    }
}
