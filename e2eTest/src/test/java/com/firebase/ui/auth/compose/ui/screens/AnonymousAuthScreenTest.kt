package com.firebase.ui.auth.compose.ui.screens

import android.content.Context
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.testutil.AUTH_STATE_WAIT_TIMEOUT_MS
import com.firebase.ui.auth.compose.testutil.EmulatorAuthApi
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

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AnonymousAuthScreenTest {
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
    fun `anonymous sign-in emits Success auth state`() {
        val configuration = authUIConfiguration {
            context = applicationContext
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

        // Track auth state changes
        var currentAuthState: AuthState = AuthState.Idle

        composeTestRule.setContent {
            TestAuthScreen(configuration = configuration)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Wait for the navigation to settle and UI to be ready
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.onNodeWithText(stringProvider.signInAnonymously)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to Success
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println(
                "TEST: Auth state during wait: $currentAuthState, isAnonymous" +
                        " - ${authUI.auth.currentUser?.isAnonymous}"
            )
            currentAuthState is AuthState.Success
        }

        composeTestRule.onNodeWithText("isAnonymous - true")
            .assertIsDisplayed()

        // Verify the auth state and user properties
        println("TEST: Verifying final auth state: $currentAuthState")
        assertThat(currentAuthState)
            .isInstanceOf(AuthState.Success::class.java)
        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.isAnonymous).isEqualTo(true)
    }

    @Test
    fun `anonymous upgrade enabled links new user sign-up and emits RequiresEmailVerification auth state`() {
        val name = "Anonymous Upgrade User"
        val email = "anonymousupgrade@example.com"
        val password = "Test@123"
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(AuthProvider.Anonymous)
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
            }
            isAnonymousUpgradeEnabled = true
        }

        // Track auth state changes
        var currentAuthState: AuthState = AuthState.Idle

        composeTestRule.setContent {
            TestAuthScreen(configuration = configuration)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Wait for the navigation to settle and UI to be ready
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        composeTestRule.onNodeWithText(stringProvider.signInAnonymously)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to Success
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println(
                "TEST: Auth state during wait: $currentAuthState, isAnonymous" +
                        " - ${authUI.auth.currentUser?.isAnonymous}"
            )
            currentAuthState is AuthState.Success
        }

        composeTestRule.onNodeWithText("isAnonymous - true")
            .assertIsDisplayed()

        assertThat(authUI.auth.currentUser!!.isAnonymous).isEqualTo(true)

        val anonymousUserUID = authUI.auth.currentUser!!.uid

        composeTestRule.onNodeWithText("Upgrade with Email")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(stringProvider.signupPageTitle.uppercase())
            .assertIsDisplayed()
            .performClick()
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

        composeTestRule.waitForIdle()
        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to RequiresEmailVerification
        println("TEST: Waiting for auth state change... Current state: $currentAuthState")
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state during wait: $currentAuthState")
            currentAuthState is AuthState.RequiresEmailVerification
        }

        // Verify the auth state and user properties
        println(
            "TEST: Verifying final auth state: $currentAuthState, " +
                    "anonymous user uid - $anonymousUserUID, linked user uid - " +
                    authUI.auth.currentUser!!.uid
        )
        assertThat(currentAuthState)
            .isInstanceOf(AuthState.RequiresEmailVerification::class.java)
        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.uid).isEqualTo(anonymousUserUID)
        assertThat(authUI.auth.currentUser!!.isAnonymous).isEqualTo(false)
        assertThat(authUI.auth.currentUser!!.email)
            .isEqualTo(email)
    }

    @Composable
    private fun TestAuthScreen(configuration: AuthUIConfiguration) {
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        FirebaseAuthScreen(
            configuration = configuration,
            authUI = authUI,
            onSignInSuccess = { result -> },
            onSignInFailure = { exception: AuthException -> },
            onSignInCancelled = {},
            authenticatedContent = { state, uiContext ->
                when (state) {
                    is AuthState.Success -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "Authenticated User - (Success): ${state.user.email}",
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "UID - ${state.user.uid}",
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "isAnonymous - ${state.user.isAnonymous}",
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Providers - " +
                                        "${state.user.providerData.map { it.providerId }}",
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.user.isAnonymous) {
                                Button(
                                    onClick = {
                                        uiContext.onNavigate(AuthRoute.Email)
                                    }
                                ) {
                                    Text("Upgrade with Email")
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}