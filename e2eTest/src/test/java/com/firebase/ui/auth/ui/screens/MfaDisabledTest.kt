package com.firebase.ui.auth.ui.screens

import android.content.Context
import android.os.Looper
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
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
import com.firebase.ui.auth.testutil.ensureTestFirebaseApp
import com.google.common.truth.Truth.assertThat
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

        val firebaseApp = ensureTestFirebaseApp(applicationContext)
        authUI = FirebaseAuthUI.getInstance()

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
        // Clean up after each test to prevent test pollution. The FirebaseApp itself is
        // shared across test classes (see ensureTestFirebaseApp), so the client-side
        // session must be reset explicitly here rather than relying on app re-creation.
        authUI.auth.signOut()
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

    /**
     * Was `@Ignore`d as "flaky in CI due to timing issues". Two causes, neither of them the
     * product's:
     *
     * `capturedUiContext` was read off the back of the auth state, but `Success` is observable a
     * composition before `authenticatedContent` hands the context over — so the assertion could
     * run against a context that did not exist yet.
     *
     * More importantly, the error was asserted by *sampling* the current state.
     * `FirebaseAuthScreen` gives an error to the dialog controller and retracts it to
     * [AuthState.Idle] immediately, so `Error` is the current state for about a frame: waiting
     * longer made the test *less* likely to see it. The emissions are recorded instead.
     */
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
        // Every emission, not just the latest one. FirebaseAuthScreen hands an error to the dialog
        // controller and retracts it to Idle in the same breath, so sampling the current state can
        // step straight over the error this test is about.
        val emitted = mutableListOf<AuthState>()

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
                    Text("Custom authenticated content")
                }
            )
            LaunchedEffect(Unit) { authUI.authStateFlow().collect { emitted += it } }
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

        // Wait for the authenticated content to have handed over its context, not merely for the
        // auth state to say Success — the first happens a composition after the second.
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            currentAuthState is AuthState.Success && capturedUiContext != null
        }

        // Now call onManageMfa directly (simulating custom content calling it)
        assertThat(capturedUiContext).isNotNull()
        capturedUiContext?.onManageMfa?.invoke()

        // Wait for auth state to update
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Verify an Error carrying AuthCancelledException was emitted, whether or not it is still
        // the current state by the time this runs.
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            emitted.any {
                it is AuthState.Error && it.exception is AuthException.AuthCancelledException
            }
        }

        val errorState = emitted.last {
            it is AuthState.Error && it.exception is AuthException.AuthCancelledException
        } as AuthState.Error
        assertThat(errorState.exception).isInstanceOf(AuthException.AuthCancelledException::class.java)
        assertThat(errorState.exception.message).contains("Multi-factor authentication is disabled")
    }
}
