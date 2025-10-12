package com.firebase.ui.auth.compose.ui.screens

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.testutil.awaitWithLooper
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.actionCodeSettings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.net.HttpURLConnection
import java.net.URL

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class EmailAuthScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context

    private lateinit var stringProvider: AuthUIStringProvider

    lateinit var authUI: FirebaseAuthUI

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

        // Clear emulator data
        clearEmulatorData()
    }

    @After
    fun tearDown() {
        // Clean up after each test to prevent test pollution
        FirebaseAuthUI.clearInstanceCache()

        // Clear emulator data
        clearEmulatorData()
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

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
        }

        composeTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()
    }

    @Test
    fun `unverified email sign-in emits RequiresEmailVerification auth state`() {
        val email = "test@example.com"
        val password = "test123"

        // Setup: Create a fresh unverified user
        ensureFreshUser(email, password)

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

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(email)
        composeTestRule.onNodeWithText(stringProvider.passwordHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(password)
        composeTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to RequiresEmailVerification
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeTestRule.waitUntil {
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
        val email = "test@example.com"
        val password = "test123"

        // Setup: Create a fresh unverified user
        val user = ensureFreshUser(email, password)

        requireNotNull(user) { "Failed to create user" }

        // Verify email using Firebase Auth Emulator OOB codes flow
        verifyEmailInEmulator(user = user)

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

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(email)
        composeTestRule.onNodeWithText(stringProvider.passwordHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(password)
        composeTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to Success (since email is verified)
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeTestRule.waitUntil(timeoutMillis = 5000) {
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
        val email = "test@example.com"
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

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        composeTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringProvider.signupPageTitle.uppercase())
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithText(stringProvider.signupPageTitle)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .assertIsDisplayed()
            .performTextInput(email)
        composeTestRule.onNodeWithText(stringProvider.nameHint)
            .assertIsDisplayed()
            .performTextInput(name)
        composeTestRule.onNodeWithText(stringProvider.passwordHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(password)
        composeTestRule.onNodeWithText(stringProvider.confirmPasswordHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(password)
        composeTestRule.onNodeWithText(stringProvider.signupPageTitle.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to RequiresEmailVerification
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeTestRule.waitUntil {
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
        val email = "test@example.com"
        val password = "test123"

        // Setup: Create a fresh user
        ensureFreshUser(email, password)

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

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        composeTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringProvider.troubleSigningIn)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithText(stringProvider.recoverPasswordPageTitle)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(email)
        composeTestRule.onNodeWithText(stringProvider.sendButtonText.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to PasswordResetLinkSent
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeTestRule.waitUntil {
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
        composeTestRule.onNodeWithText(stringProvider.recoverPasswordLinkSentDialogTitle)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringProvider.recoverPasswordLinkSentDialogBody(email))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringProvider.dismissAction)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.recoverPasswordLinkSentDialogTitle)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()
    }

    @Test
    fun `email link sign in emits EmailSignInLinkSent auth state and shows dialog`() {
        val email = "test@example.com"
        val password = "test123"

        // Setup: Create a fresh user
        ensureFreshUser(email, password)

        // Sign out
        authUI.auth.signOut()
        shadowOf(Looper.getMainLooper()).idle()

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

        // Track auth state changes
        var currentAuthState: AuthState = AuthState.Idle

        composeTestRule.setContent {
            FirebaseAuthScreen(configuration = configuration)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        composeTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(email)
        composeTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to EmailSignInLinkSent
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeTestRule.waitUntil {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state during wait: $currentAuthState")
            currentAuthState is AuthState.EmailSignInLinkSent
        }

        // Ensure final recomposition is complete before assertions
        shadowOf(Looper.getMainLooper()).idle()

        // Verify the auth state and user properties
        println("TEST: Verifying final auth state: $currentAuthState")
        assertThat(currentAuthState)
            .isInstanceOf(AuthState.EmailSignInLinkSent::class.java)
        assertThat(authUI.auth.currentUser).isNull()
        composeTestRule.onNodeWithText(stringProvider.emailSignInLinkSentDialogTitle)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringProvider.emailSignInLinkSentDialogBody(email))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(stringProvider.dismissAction)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(stringProvider.emailSignInLinkSentDialogTitle)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()
    }

    @Composable
    private fun FirebaseAuthScreen(
        configuration: AuthUIConfiguration,
        onSuccess: ((AuthResult) -> Unit) = {},
        onError: ((AuthException) -> Unit) = {},
        onCancel: (() -> Unit) = {}
    ) {
        EmailAuthScreen(
            context = applicationContext,
            configuration = configuration,
            authUI = authUI,
            onSuccess = onSuccess,
            onError = onError,
            onCancel = onCancel,
        ) { state ->
            when (state.mode) {
                EmailAuthMode.SignIn -> {
                    SignInUI(
                        configuration = configuration,
                        email = state.email,
                        isLoading = state.isLoading,
                        emailSignInLinkSent = state.emailSignInLinkSent,
                        password = state.password,
                        onEmailChange = state.onEmailChange,
                        onPasswordChange = state.onPasswordChange,
                        onSignInClick = state.onSignInClick,
                        onGoToSignUp = state.onGoToSignUp,
                        onGoToResetPassword = state.onGoToResetPassword,
                    )
                }

                EmailAuthMode.SignUp -> {
                    SignUpUI(
                        configuration = configuration,
                        isLoading = state.isLoading,
                        displayName = state.displayName,
                        email = state.email,
                        password = state.password,
                        confirmPassword = state.confirmPassword,
                        onDisplayNameChange = state.onDisplayNameChange,
                        onEmailChange = state.onEmailChange,
                        onPasswordChange = state.onPasswordChange,
                        onConfirmPasswordChange = state.onConfirmPasswordChange,
                        onSignUpClick = state.onSignUpClick,
                        onGoToSignIn = state.onGoToSignIn,
                    )
                }

                EmailAuthMode.ResetPassword -> {
                    ResetPasswordUI(
                        configuration = configuration,
                        isLoading = state.isLoading,
                        email = state.email,
                        resetLinkSent = state.resetLinkSent,
                        onEmailChange = state.onEmailChange,
                        onSendResetLink = state.onSendResetLinkClick,
                        onGoToSignIn = state.onGoToSignIn
                    )
                }
            }
        }
    }

    /**
     * Clears all data from the Firebase Auth Emulator.
     *
     * This function calls the emulator's clear data endpoint to remove all accounts,
     * OOB codes, and other authentication data. This ensures test isolation by providing
     * a clean slate for each test.
     *
     * @param emulatorHost The emulator host (default: "127.0.0.1")
     * @param emulatorPort The emulator port (default: 9099)
     *
     * @throws Exception if the clear operation fails
     */
    private fun clearEmulatorData(
        projectId: String = "fake-project-id",
        emulatorHost: String = "127.0.0.1",
        emulatorPort: Int = 9099
    ) {
        val clearUrl =
            URL("http://$emulatorHost:$emulatorPort/emulator/v1/projects/$projectId/accounts")
        val clearConnection = clearUrl.openConnection() as HttpURLConnection

        try {
            clearConnection.requestMethod = "DELETE"
            clearConnection.connectTimeout = 5000
            clearConnection.readTimeout = 5000

            val responseCode = clearConnection.responseCode
            if (responseCode !in 200..299) {
                println("WARNING: Failed to clear emulator data: HTTP $responseCode")
            } else {
                println("TEST: Cleared emulator data")
            }
        } catch (e: Exception) {
            println("WARNING: Exception while clearing emulator data: ${e.message}")
        } finally {
            clearConnection.disconnect()
        }
    }

    /**
     * Ensures a fresh user exists in the Firebase emulator with the given credentials.
     * If a user already exists, they will be deleted first.
     * The user will be signed out after creation, leaving an unverified account ready for testing.
     *
     * This function uses coroutines and automatically handles Robolectric's main looper.
     */
    private fun ensureFreshUser(email: String, password: String): FirebaseUser? {
        println("TEST: Ensuring fresh user for $email")
        // Try to sign in - if successful, user exists and should be deleted
        try {
            authUI.auth.signInWithEmailAndPassword(email, password).awaitWithLooper()
                .also { result ->
                    println("TEST: User exists (${result.user?.uid}), deleting...")
                    // User exists, delete them
                    result.user?.delete()?.awaitWithLooper()
                    println("TEST: User deleted")
                }
        } catch (_: Exception) {
            // User doesn't exist - this is expected
        }

        // Create fresh user
        return authUI.auth.createUserWithEmailAndPassword(email, password).awaitWithLooper()
            .user
    }

    /**
     * Verifies a user's email in the Firebase Auth Emulator by simulating the complete
     * email verification flow.
     *
     * This function:
     * 1. Sends a verification email using sendEmailVerification()
     * 2. Retrieves the OOB (out-of-band) code from the emulator's OOB codes endpoint
     * 3. Applies the action code to complete email verification
     *
     * This approach works with the Firebase Auth Emulator's documented API and simulates
     * the real email verification flow that would occur in production.
     *
     * @param user The FirebaseUser whose email should be verified
     * @param emulatorHost The emulator host (default: "127.0.0.1")
     * @param emulatorPort The emulator port (default: 9099)
     *
     * @throws Exception if the verification flow fails
     */
    private fun verifyEmailInEmulator(
        user: FirebaseUser,
        emulatorHost: String = "127.0.0.1",
        emulatorPort: Int = 9099
    ) {
        println("TEST: Starting email verification for user ${user.uid}")

        // Step 1: Send verification email to generate an OOB code
        user.sendEmailVerification().awaitWithLooper()
        println("TEST: Sent email verification request")

        // Give the emulator time to process and store the OOB code
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(100)

        // Step 2: Retrieve OOB codes from the emulator
        val projectId = authUI.app.options.projectId
            ?: throw IllegalStateException("Project ID is required")
        println("TEST: Using project ID: $projectId")

        val oobUrl =
            URL("http://$emulatorHost:$emulatorPort/emulator/v1/projects/$projectId/oobCodes")
        val oobConnection = oobUrl.openConnection() as HttpURLConnection

        val oobCodesJson = try {
            oobConnection.requestMethod = "GET"
            oobConnection.connectTimeout = 5000
            oobConnection.readTimeout = 5000

            val responseCode = oobConnection.responseCode
            if (responseCode != 200) {
                throw Exception("Failed to get OOB codes: HTTP $responseCode")
            }

            oobConnection.inputStream.bufferedReader().readText()
        } finally {
            oobConnection.disconnect()
        }

        println("TEST: OOB codes response: $oobCodesJson")

        // Step 3: Parse the response to find the VERIFY_EMAIL code for this user's email
        // Response format: {"oobCodes": [{"email": "...", "oobCode": "...", "oobLink": "...", "requestType": "..."}]}
        // We need to find an entry with both matching email AND requestType: "VERIFY_EMAIL"
        val verifyEmailPattern =
            """"email":"${user.email}","requestType":"VERIFY_EMAIL","oobCode":"([^"]+)"""".toRegex()

        val oobCodeMatch = verifyEmailPattern.find(oobCodesJson)
        val oobCode = oobCodeMatch?.groupValues?.get(1)
            ?: throw Exception("No VERIFY_EMAIL OOB code found for user email: ${user.email}")

        println("TEST: Found OOB code: $oobCode")

        // Step 4: Apply the action code to verify the email
        authUI.auth.applyActionCode(oobCode).awaitWithLooper()
        println("TEST: Applied action code")

        // Step 5: Reload the user to refresh their email verification status
        authUI.auth.currentUser?.reload()?.awaitWithLooper()
        shadowOf(Looper.getMainLooper()).idle()

        println("TEST: Email verified successfully for user ${user.uid}")
        println("TEST: User isEmailVerified: ${authUI.auth.currentUser?.isEmailVerified}")
    }

}