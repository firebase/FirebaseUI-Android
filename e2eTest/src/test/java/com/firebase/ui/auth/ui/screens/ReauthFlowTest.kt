package com.firebase.ui.auth.ui.screens

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.testutil.AUTH_STATE_WAIT_TIMEOUT_MS
import com.firebase.ui.auth.testutil.EmulatorAuthApi
import com.firebase.ui.auth.testutil.ensureFreshUser
import com.firebase.ui.auth.testutil.verifyEmailInEmulator
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class ReauthFlowTest {

    @get:Rule
    val composeAndroidTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: DefaultAuthUIStringProvider
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var emulatorApi: EmulatorAuthApi

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)

        FirebaseApp.getApps(applicationContext).forEach { it.delete() }

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
                ?: throw IllegalStateException("Project ID is required"),
            emulatorHost = "127.0.0.1",
            emulatorPort = 9099,
        )

        emulatorApi.clearEmulatorData()
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        emulatorApi.clearEmulatorData()
    }

    /**
     * Full cycle: sign in via the main flow, then emit ReauthenticationRequired to simulate a
     * sensitive operation. Verifies the default ModalBottomSheet reauth UI appears, completing
     * reauthentication triggers the pending retry operation.
     *
     * The initial sign-in must complete first so the main screen shows the authenticated view —
     * this avoids having two simultaneous email input forms (one in the main sign-in screen and
     * one in the reauth bottom sheet).
     */
    @Test
    fun `reauth bottom sheet appears and triggers retry operation on successful reauthentication`() {
        val email = "reauth-test-${System.currentTimeMillis()}@example.com"
        val password = "test123"

        val user = ensureFreshUser(authUI, email, password)
        requireNotNull(user) { "Failed to create user" }

        // Email must be verified so sign-in (both initial and reauth) resolves to Success.
        try {
            verifyEmailInEmulator(authUI, emulatorApi, user)
        } catch (e: Exception) {
            Assume.assumeTrue(
                "Skipping: Firebase Auth Emulator OOB codes not available. Error: ${e.message}",
                false
            )
        }

        // Sign out so the screen starts on the sign-in form.
        authUI.auth.signOut()
        shadowOf(Looper.getMainLooper()).idle()

        var currentAuthState: AuthState = AuthState.Idle
        var retryOperationCalled = false

        val configuration = authUIConfiguration {
            context = applicationContext
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

        composeAndroidTestRule.setContent {
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(applicationContext)
            ) {
                FirebaseAuthScreen(
                    configuration = configuration,
                    authUI = authUI,
                    onSignInSuccess = {},
                    onSignInFailure = {},
                    onSignInCancelled = {},
                ) { state, _ ->
                    if (state is AuthState.Success) Text("AUTHENTICATED") else Text("NOT AUTHENTICATED")
                }
                val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
                currentAuthState = authState
            }
        }

        shadowOf(Looper.getMainLooper()).idle()

        // Step 1: Complete initial sign-in via the main screen form.
        composeAndroidTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .performTextInput(email)
        composeAndroidTestRule.onNodeWithText(stringProvider.passwordHint)
            .performScrollTo()
            .performTextInput(password)
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .performClick()

        shadowOf(Looper.getMainLooper()).idle()

        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            currentAuthState is AuthState.Success
        }

        // Main screen now shows authenticated content — no email form visible.
        composeAndroidTestRule.onNodeWithText("AUTHENTICATED").assertIsDisplayed()

        val signedInUser = requireNotNull(authUI.auth.currentUser) { "User must be signed in" }

        // Step 2: Emit ReauthenticationRequired to simulate a sensitive operation requiring reauth.
        authUI.updateAuthState(
            AuthState.ReauthenticationRequired(
                user = signedInUser,
                reason = "Please verify your identity to continue",
                retryOperation = { retryOperationCalled = true },
            )
        )

        shadowOf(Looper.getMainLooper()).idle()

        // Wait for the reauth bottom sheet email form to appear (now the only email form visible).
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeAndroidTestRule.onAllNodesWithText(stringProvider.emailHint)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Step 3: Enter credentials in the reauth bottom sheet.
        composeAndroidTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .performTextInput(email)
        composeAndroidTestRule.onNodeWithText(stringProvider.passwordHint)
            .performScrollTo()
            .performTextInput(password)
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .performClick()

        shadowOf(Looper.getMainLooper()).idle()

        // Verify the retry operation fires after successful reauthentication.
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            retryOperationCalled
        }

        assertThat(retryOperationCalled).isTrue()
    }

    /**
     * Verifies that when reauthContent is provided, it receives the ReauthenticationRequired state
     * and calling onDismiss resets the auth state to Idle.
     */
    @Test
    fun `custom reauthContent receives ReauthenticationRequired state and dismisses to Idle`() {
        val email = "reauth-custom-${System.currentTimeMillis()}@example.com"
        val password = "test123"

        val user = ensureFreshUser(authUI, email, password)
        requireNotNull(user) { "Failed to create user" }

        val capturedUser = requireNotNull(authUI.auth.currentUser) { "User must be signed in after creation" }
        authUI.auth.signOut()
        shadowOf(Looper.getMainLooper()).idle()

        var currentAuthState: AuthState = AuthState.Idle
        val expectedReason = "Sensitive operation requires sign-in"

        val configuration = authUIConfiguration {
            context = applicationContext
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

        composeAndroidTestRule.setContent {
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(applicationContext)
            ) {
                FirebaseAuthScreen(
                    configuration = configuration,
                    authUI = authUI,
                    onSignInSuccess = {},
                    onSignInFailure = {},
                    onSignInCancelled = {},
                    reauthContent = { reauthState, onDismiss ->
                        Column {
                            Text("REAUTH REQUIRED - ${reauthState.reason}")
                            Button(onClick = onDismiss) { Text("DISMISS REAUTH") }
                        }
                    },
                ) { _, _ ->
                    Text("CONTENT")
                }
                val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
                currentAuthState = authState
            }
        }

        shadowOf(Looper.getMainLooper()).idle()

        // Emit ReauthenticationRequired to trigger the custom reauthContent slot.
        authUI.updateAuthState(
            AuthState.ReauthenticationRequired(
                user = capturedUser,
                reason = expectedReason,
            )
        )

        shadowOf(Looper.getMainLooper()).idle()

        // Verify the custom reauth content is displayed with the correct reason.
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeAndroidTestRule.onAllNodesWithText("REAUTH REQUIRED - $expectedReason")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeAndroidTestRule.onNodeWithText("REAUTH REQUIRED - $expectedReason")
            .assertIsDisplayed()

        // Dismiss the custom reauth UI via the onDismiss callback.
        composeAndroidTestRule.onNodeWithText("DISMISS REAUTH").performClick()

        shadowOf(Looper.getMainLooper()).idle()

        // Verify that dismissing resets auth state to Idle.
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            currentAuthState is AuthState.Idle
        }

        assertThat(currentAuthState).isInstanceOf(AuthState.Idle::class.java)
    }
}
