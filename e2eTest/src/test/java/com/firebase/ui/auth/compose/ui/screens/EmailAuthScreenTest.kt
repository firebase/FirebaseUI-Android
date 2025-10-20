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
import com.firebase.ui.auth.compose.testutil.AUTH_STATE_WAIT_TIMEOUT_MS
import com.firebase.ui.auth.compose.testutil.EmulatorAuthApi
import com.firebase.ui.auth.compose.testutil.awaitWithLooper
import com.firebase.ui.auth.compose.testutil.ensureFreshUser
import com.firebase.ui.auth.compose.testutil.verifyEmailInEmulator
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthResult
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

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class EmailAuthScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context

    private lateinit var stringProvider: AuthUIStringProvider

    lateinit var authUI: FirebaseAuthUI
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
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
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
        val user = ensureFreshUser(authUI, email, password)

        requireNotNull(user) { "Failed to create user" }

        // Verify email using Firebase Auth Emulator OOB codes flow
        verifyEmailInEmulator(authUI, emulatorApi, user)

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
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
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
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
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
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
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
        ensureFreshUser(authUI, email, password)

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
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
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
        onCancel: (() -> Unit) = {},
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
}
