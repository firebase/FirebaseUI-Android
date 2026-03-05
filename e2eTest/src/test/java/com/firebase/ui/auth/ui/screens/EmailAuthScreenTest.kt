package com.firebase.ui.auth.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.PasswordRule
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.testutil.AUTH_STATE_WAIT_TIMEOUT_MS
import com.firebase.ui.auth.testutil.EmailLinkTestActivity
import com.firebase.ui.auth.testutil.EmulatorAuthApi
import com.firebase.ui.auth.testutil.ensureFreshUser
import com.firebase.ui.auth.testutil.verifyEmailInEmulator
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.actionCodeSettings
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class EmailAuthScreenTest {
    @get:Rule
    val composeAndroidTestRule = createAndroidComposeRule<ComponentActivity>()

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
    fun `initial EmailAuthMode is SignIn`() {
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
        }

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(configuration = configuration, authUI = authUI)
        }

        // Click on email provider in AuthMethodPicker
        composeAndroidTestRule.onNodeWithText(stringProvider.signInWithEmail)
            .assertIsDisplayed()
            .performClick()

        composeAndroidTestRule.waitForIdle()

        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()
    }

    @Test
    fun `unverified email sign-in emits RequiresEmailVerification auth state`() {
        val email = "unverified-test-${System.currentTimeMillis()}@example.com"
        val password = "test123"

        // Setup: Create a fresh unverified user
        ensureFreshUser(authUI, email, password)

        // Sign out
        authUI.auth.signOut()
        shadowOf(Looper.getMainLooper()).idle()

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
        }

        // Track auth state changes
        var currentAuthState: AuthState = AuthState.Idle

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(configuration = configuration, authUI = authUI)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Click on email provider in AuthMethodPicker
        composeAndroidTestRule.onNodeWithText(stringProvider.signInWithEmail)
            .assertIsDisplayed()
            .performClick()

        composeAndroidTestRule.waitForIdle()

        composeAndroidTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(email)
        composeAndroidTestRule.onNodeWithText(stringProvider.passwordHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(password)
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to RequiresEmailVerification
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state during wait: $currentAuthState")
            currentAuthState is AuthState.RequiresEmailVerification
        }

        // Ensure final recomposition is complete before assertions
        shadowOf(Looper.getMainLooper()).idle()

        // Verify the auth state and user properties
        println("TEST: Verifying final auth state: $currentAuthState")
        assertThat(currentAuthState)
            .isInstanceOf(AuthState.RequiresEmailVerification::class.java)
        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.isEmailVerified).isEqualTo(false)
        assertThat(authUI.auth.currentUser!!.email).isEqualTo(email)
    }

    @Test
    fun `verified email sign-in emits Success auth state`() {
        val email = "verified-test-${System.currentTimeMillis()}@example.com"
        val password = "test123"

        // Setup: Create a fresh unverified user
        val user = ensureFreshUser(authUI, email, password)

        requireNotNull(user) { "Failed to create user" }

        // Verify email using Firebase Auth Emulator OOB codes flow
        // NOTE: This test requires Firebase Auth Emulator to be running on localhost:9099
        // Start the emulator with: firebase emulators:start --only auth
        try {
            verifyEmailInEmulator(authUI, emulatorApi, user)
        } catch (e: Exception) {
            // If we can't fetch OOB codes, the emulator might not be configured correctly
            // or might not be running. Skip this test with a clear message.
            Assume.assumeTrue(
                "Skipping test: Firebase Auth Emulator OOB codes endpoint not available. " +
                        "Ensure emulator is running on localhost:9099. Error: ${e.message}",
                false
            )
        }

        // Sign out
        authUI.auth.signOut()
        shadowOf(Looper.getMainLooper()).idle()

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
        }

        // Track auth state changes
        var currentAuthState: AuthState = AuthState.Idle

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(configuration = configuration, authUI = authUI)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Click on email provider in AuthMethodPicker
        composeAndroidTestRule.onNodeWithText(stringProvider.signInWithEmail)
            .assertIsDisplayed()
            .performClick()

        composeAndroidTestRule.waitForIdle()

        composeAndroidTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(email)
        composeAndroidTestRule.onNodeWithText(stringProvider.passwordHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(password)
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to Success (since email is verified)
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state during wait: $currentAuthState")
            currentAuthState is AuthState.Success
        }

        // Ensure final recomposition is complete before assertions
        shadowOf(Looper.getMainLooper()).idle()

        // Verify the auth state and user properties
        println("TEST: Verifying final auth state: $currentAuthState")
        assertThat(currentAuthState)
            .isInstanceOf(AuthState.Success::class.java)
        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.isEmailVerified).isEqualTo(true)
        assertThat(authUI.auth.currentUser!!.email).isEqualTo(email)
    }

    @Test
    fun `new email sign-up emits RequiresEmailVerification auth state`() {
        val name = "Test User"
        val email = "signup-test-${System.currentTimeMillis()}@example.com"
        val password = "Test@123"

        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = listOf(
                            PasswordRule.MinimumLength(8),
                            PasswordRule.RequireLowercase,
                            PasswordRule.RequireUppercase
                        )
                    )
                )
            }
        }

        // Track auth state changes
        var currentAuthState: AuthState = AuthState.Idle

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(configuration = configuration, authUI = authUI)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Click on email provider in AuthMethodPicker
        composeAndroidTestRule.onNodeWithText(stringProvider.signInWithEmail)
            .assertIsDisplayed()
            .performClick()

        composeAndroidTestRule.waitForIdle()

        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()
        composeAndroidTestRule.onNodeWithText(stringProvider.signupPageTitle.uppercase())
            .assertIsDisplayed()
            .performClick()
        composeAndroidTestRule.onNodeWithText(stringProvider.signupPageTitle)
            .assertIsDisplayed()
        composeAndroidTestRule.onNodeWithText(stringProvider.emailHint)
            .assertIsDisplayed()
            .performTextInput(email)
        composeAndroidTestRule.onNodeWithText(stringProvider.nameHint)
            .assertIsDisplayed()
            .performTextInput(name)
        composeAndroidTestRule.onNodeWithText(stringProvider.passwordHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(password)
        composeAndroidTestRule.onNodeWithText(stringProvider.confirmPasswordHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(password)
        composeAndroidTestRule.onNodeWithText(stringProvider.signupPageTitle.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to RequiresEmailVerification
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state during wait: $currentAuthState")
            currentAuthState is AuthState.RequiresEmailVerification
        }

        // Ensure final recomposition is complete before assertions
        shadowOf(Looper.getMainLooper()).idle()

        // Verify the auth state and user properties
        println("TEST: Verifying final auth state: $currentAuthState")
        assertThat(currentAuthState)
            .isInstanceOf(AuthState.RequiresEmailVerification::class.java)
        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.isEmailVerified).isEqualTo(false)
        assertThat(authUI.auth.currentUser!!.email).isEqualTo(email)
    }

    @Test
    fun `trouble signing in emits PasswordResetLinkSent auth state and shows dialog`() {
        val email = "trouble-test-${System.currentTimeMillis()}@example.com"
        val password = "test123"

        // Setup: Create a fresh user
        ensureFreshUser(authUI, email, password)

        // Sign out
        authUI.auth.signOut()
        shadowOf(Looper.getMainLooper()).idle()

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
        }

        // Track auth state changes
        var currentAuthState: AuthState = AuthState.Idle

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(configuration = configuration, authUI = authUI)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Click on email provider in AuthMethodPicker
        composeAndroidTestRule.onNodeWithText(stringProvider.signInWithEmail)
            .assertIsDisplayed()
            .performClick()

        composeAndroidTestRule.waitForIdle()

        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()
        composeAndroidTestRule.onNodeWithText(stringProvider.troubleSigningIn)
            .assertIsDisplayed()
            .performClick()
        composeAndroidTestRule.onNodeWithText(stringProvider.recoverPasswordPageTitle)
            .assertIsDisplayed()
        composeAndroidTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(email)
        composeAndroidTestRule.onNodeWithText(stringProvider.sendButtonText.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to PasswordResetLinkSent
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state during wait: $currentAuthState")
            currentAuthState is AuthState.PasswordResetLinkSent
        }

        // Ensure final recomposition is complete before assertions
        shadowOf(Looper.getMainLooper()).idle()

        // Verify the auth state and user properties
        println("TEST: Verifying final auth state: $currentAuthState")
        assertThat(currentAuthState)
            .isInstanceOf(AuthState.PasswordResetLinkSent::class.java)
        assertThat(authUI.auth.currentUser).isNull()
        composeAndroidTestRule.onNodeWithText(stringProvider.recoverPasswordLinkSentDialogTitle)
            .assertIsDisplayed()
        composeAndroidTestRule.onNodeWithText(stringProvider.recoverPasswordLinkSentDialogBody(email))
            .assertIsDisplayed()
        composeAndroidTestRule.onNodeWithText(stringProvider.dismissAction)
            .assertIsDisplayed()
            .performClick()
        composeAndroidTestRule.waitForIdle()
        composeAndroidTestRule.onNodeWithText(stringProvider.recoverPasswordLinkSentDialogTitle)
            .assertIsNotDisplayed()
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()
    }

    @Test
    fun `email link sign in emits EmailSignInLinkSent auth state, shows dialog and handles deep link sign in`() {
        val email = "emaillink-test-${System.currentTimeMillis()}@example.com"

        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        isEmailLinkSignInEnabled = true,
                        isEmailLinkForceSameDeviceEnabled = true,
                        emailLinkActionCodeSettings = actionCodeSettings {
                            // The continue URL - where to redirect after email link is clicked
                            url = "https://fake-project-id.firebaseapp.com"
                            handleCodeInApp = true
                            setAndroidPackageName(
                                "fake.project.id",
                                true,
                                null
                            )
                        },
                        passwordValidationRules = emptyList()
                    )
                )
            }
        }

        // Track auth state changes and email link (lifted state)
        var currentAuthState: AuthState = AuthState.Idle
        var pendingEmailLink by mutableStateOf<String?>(null)

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                emailLink = pendingEmailLink
            )
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Click on email provider in AuthMethodPicker
        composeAndroidTestRule.onNodeWithText(stringProvider.signInWithEmail)
            .assertIsDisplayed()
            .performClick()

        composeAndroidTestRule.waitForIdle()

        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()

        // Click "Sign in with email link" button to switch to email link mode
        composeAndroidTestRule.onNodeWithText(stringProvider.signInWithEmailLink.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeAndroidTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(email)
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()
        composeAndroidTestRule.waitForIdle()

        // Wait for auth state to transition to EmailSignInLinkSent
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state during wait: $currentAuthState")
            currentAuthState is AuthState.EmailSignInLinkSent
        }

        // Ensure final recomposition is complete before assertions
        shadowOf(Looper.getMainLooper()).idle()
        composeAndroidTestRule.waitForIdle()

        // Verify the auth state and user properties
        println("TEST: Verifying auth state: $currentAuthState")
        assertThat(currentAuthState)
            .isInstanceOf(AuthState.EmailSignInLinkSent::class.java)
        assertThat(authUI.auth.currentUser).isNull()
        composeAndroidTestRule.onNodeWithText(stringProvider.emailSignInLinkSentDialogTitle)
            .assertIsDisplayed()
        composeAndroidTestRule.onNodeWithText(stringProvider.emailSignInLinkSentDialogBody(email))
            .assertIsDisplayed()
        composeAndroidTestRule.onNodeWithText(stringProvider.dismissAction)
            .assertIsDisplayed()
            .performClick()
        composeAndroidTestRule.waitForIdle()
        composeAndroidTestRule.onNodeWithText(stringProvider.emailSignInLinkSentDialogTitle)
            .assertIsNotDisplayed()
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()

        // Now test the deep link flow - fetch the email link from emulator
        println("TEST: Fetching email sign-in link from emulator...")
        val emailLinkFromEmulator = try {
            emulatorApi.fetchEmailSignInLink(email)
        } catch (e: Exception) {
            println("TEST: Failed to fetch email sign-in link: ${e.message}")
            // Skip the deep link verification if we can't fetch the link
            Assume.assumeTrue(
                "Skipping deep link test: Firebase Auth Emulator OOB codes endpoint not available. " +
                        "Ensure emulator is running on localhost:9099. Error: ${e.message}",
                false
            )
            null
        }

        requireNotNull(emailLinkFromEmulator) { "Email link should not be null at this point" }

        println("TEST: Fetched email sign-in link: $emailLinkFromEmulator")

        // Create a deep link Intent (simulates clicking email link on device)
        val deepLinkUri = Uri.parse(emailLinkFromEmulator)
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, deepLinkUri)

        // Verify the intent can be handled by Firebase Auth UI
        assertThat(authUI.canHandleIntent(deepLinkIntent)).isTrue()

        println("TEST: Launching EmailLinkTestActivity with deep link intent...")

        // Use ActivityScenario to launch EmailLinkTestActivity with the deep link intent
        // This properly simulates the Android deep link flow - when a user clicks the email link,
        // Android launches the app with an ACTION_VIEW intent
        val extractedEmailLink = ActivityScenario.launch<EmailLinkTestActivity>(deepLinkIntent).use { scenario ->
            var emailLinkFromIntent: String? = null

            scenario.onActivity { activity ->
                // Verify the intent was received correctly
                assertThat(activity.intent.action).isEqualTo(Intent.ACTION_VIEW)
                assertThat(activity.intent.data).isEqualTo(deepLinkUri)

                // Verify the activity extracted the email link
                assertThat(activity.emailLinkFromIntent).isNotNull()
                assertThat(activity.emailLinkFromIntent).isEqualTo(emailLinkFromEmulator)

                emailLinkFromIntent = activity.emailLinkFromIntent

                println("TEST: Email link extracted by activity: $emailLinkFromIntent")
            }

            emailLinkFromIntent
        }

        requireNotNull(extractedEmailLink) { "Failed to extract email link from intent" }

        println("TEST: Updating pendingEmailLink to trigger deep link sign-in in main test...")
        // Update the lifted state in the ORIGINAL test activity - this will trigger
        // FirebaseAuthScreen to handle the email link
        pendingEmailLink = extractedEmailLink

        shadowOf(Looper.getMainLooper()).idle()

        println("TEST: Waiting for auth state after deep link handling... Current state: $currentAuthState")

        // Wait for auth state to transition to Success after email link sign-in
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state during deep link wait: $currentAuthState")
            currentAuthState is AuthState.Success
        }

        // Ensure final recomposition is complete before assertions
        shadowOf(Looper.getMainLooper()).idle()

        // Verify the auth state and user properties after email link sign-in
        println("TEST: Verifying final auth state after email link sign-in: $currentAuthState")
        assertThat(currentAuthState)
            .isInstanceOf(AuthState.Success::class.java)
        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.email).isEqualTo(email)

        composeAndroidTestRule.onNodeWithText("AUTHENTICATED - $email")
            .assertIsDisplayed()
    }

    @Composable
    private fun TestFirebaseAuthScreen(
        configuration: AuthUIConfiguration,
        authUI: FirebaseAuthUI,
        emailLink: String? = null,
    ) {
        CompositionLocalProvider(
            LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(applicationContext)
        ) {
            FirebaseAuthScreen(
                configuration = configuration,
                authUI = authUI,
                emailLink = emailLink,
                onSignInSuccess = { result -> },
                onSignInFailure = { exception -> },
                onSignInCancelled = { }
            ) { state, context ->
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state is AuthState.Success) {
                        Text("AUTHENTICATED - ${state.user.email}")
                    } else {
                        Text("NOT AUTHENTICATED")
                    }
                }
            }
        }
    }
}
