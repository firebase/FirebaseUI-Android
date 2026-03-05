package com.firebase.ui.auth.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Base64
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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.testutil.AUTH_STATE_WAIT_TIMEOUT_MS
import com.firebase.ui.auth.testutil.EmulatorAuthApi
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class GoogleAuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var applicationContext: Context

    private lateinit var stringProvider: AuthUIStringProvider

    private lateinit var authUI: FirebaseAuthUI
    private lateinit var emulatorApi: EmulatorAuthApi

    @Mock
    private lateinit var mockCredentialManager: CredentialManager

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

        val testCredentialManagerProvider = object : AuthProvider.Google.CredentialManagerProvider {
            override suspend fun getGoogleCredential(
                context: Context,
                credentialManager: CredentialManager,
                serverClientId: String,
                filterByAuthorizedAccounts: Boolean,
                autoSelectEnabled: Boolean
            ): AuthProvider.Google.GoogleSignInResult {
                return AuthProvider.Google.DefaultCredentialManagerProvider().getGoogleCredential(
                    context = context,
                    credentialManager = mockCredentialManager,
                    serverClientId = serverClientId,
                    filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                    autoSelectEnabled = autoSelectEnabled
                )
            }
        }
        authUI.testCredentialManagerProvider = testCredentialManagerProvider

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
    fun `anonymous upgrade with google links anonymous user and emits Success auth state`() = runTest {
        val email = "anonymousupgrade@example.com"
        val name = "Anonymous Upgrade User"
        val photoUrl = "https://example.com/avatar.jpg"

        // Generate a JWT token for the Google account
        val mockIdToken = generateMockGoogleIdToken(
            email = email,
            name = name,
            photoUrl = photoUrl
        )
        val mockCredential = mock<GoogleIdTokenCredential> {
            on { type } doReturn GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            on { data } doReturn Bundle().apply {
                putString(
                    "com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN",
                    mockIdToken
                )
                putString(
                    "com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID",
                    email
                )
                putString(
                    "com.google.android.libraries.identity.googleid.BUNDLE_KEY_DISPLAY_NAME",
                    name
                )
                putParcelable(
                    "com.google.android.libraries.identity.googleid.BUNDLE_KEY_PROFILE_PICTURE_URI",
                    Uri.parse(photoUrl)
                )
            }
            on { displayName } doReturn name
            on { profilePictureUri } doReturn Uri.parse(photoUrl)
        }
        val mockResult = mock<GetCredentialResponse> {
            on { credential } doReturn mockCredential
        }
        whenever(mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()))
            .thenReturn(mockResult)

        // Track auth state changes
        var currentAuthState: AuthState = AuthState.Idle

        composeTestRule.setContent {
            val configuration = authUIConfiguration {
                context = applicationContext
                providers {
                    provider(AuthProvider.Anonymous)
                    provider(
                        AuthProvider.Google(
                            scopes = listOf("email"),
                            serverClientId = "test-server-client-id",
                        )
                    )
                }
                isAnonymousUpgradeEnabled = true
            }

            TestAuthScreen(configuration = configuration)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Wait for UI to be ready
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Step 1: Sign in anonymously
        println("TEST: Clicking anonymous sign-in button...")
        composeTestRule.onNodeWithText(stringProvider.signInAnonymously)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for anonymous auth to complete
        println("TEST: Waiting for anonymous auth state change...")
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state during anonymous sign-in: $currentAuthState")
            currentAuthState is AuthState.Success
        }

        // Verify anonymous user
        composeTestRule.onNodeWithText("isAnonymous - true")
            .assertIsDisplayed()
        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.isAnonymous).isTrue()

        val anonymousUserUID = authUI.auth.currentUser!!.uid
        println("TEST: Anonymous user UID: $anonymousUserUID")

        // Step 2: Click "Upgrade with Google" button
        println("TEST: Clicking 'Upgrade with Google' button...")
        composeTestRule.onNodeWithText("Upgrade with Google")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Step 3: Click the Google sign-in button on the method picker
        println("TEST: Scrolling to Google sign-in button...")
        composeTestRule
            .onNodeWithTag("AuthMethodPicker LazyColumn")
            .performScrollToNode(hasText(stringProvider.signInWithGoogle))

        println("TEST: Clicking Google sign-in button...")
        composeTestRule
            .onNode(hasText(stringProvider.signInWithGoogle))
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for Google auth to complete and link
        println("TEST: Waiting for Google auth and account linking...")
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state during Google linking: $currentAuthState, isAnonymous: ${authUI.auth.currentUser?.isAnonymous}")
            currentAuthState is AuthState.Success && authUI.auth.currentUser?.isAnonymous == false
        }

        // Verify the linked account
        println("TEST: Verifying linked account...")
        assertThat(currentAuthState).isInstanceOf(AuthState.Success::class.java)
        assertThat(authUI.auth.currentUser).isNotNull()

        // Verify UID is preserved (account was linked, not replaced)
        assertThat(authUI.auth.currentUser!!.uid).isEqualTo(anonymousUserUID)

        // Verify user is no longer anonymous
        assertThat(authUI.auth.currentUser!!.isAnonymous).isFalse()

        // Debug: Print user details
        println("TEST: User email: ${authUI.auth.currentUser!!.email}")
        println("TEST: User displayName: ${authUI.auth.currentUser!!.displayName}")
        println("TEST: User photoUrl: ${authUI.auth.currentUser!!.photoUrl}")
        println("TEST: User providerData: ${authUI.auth.currentUser!!.providerData.map { "${it.providerId}: ${it.displayName}, ${it.photoUrl}" }}")

        // Verify Google account details
        assertThat(authUI.auth.currentUser!!.email).isEqualTo(email)
        assertThat(authUI.auth.currentUser!!.displayName).isEqualTo(name)
        assertThat(authUI.auth.currentUser!!.photoUrl.toString()).isEqualTo(photoUrl)

        // Verify Google provider is linked
        val providerIds = authUI.auth.currentUser!!.providerData.map { it.providerId }
        assertThat(providerIds).contains("google.com")
    }

    @Test
    fun `sign in with google emits Success auth state`() = runTest {
        val email = "testuser@example.com"
        val name = "Test Example User"
        val photoUrl = "https://example.com/avatar.jpg"
        // Generate a JWT token with the test email
        val mockIdToken = generateMockGoogleIdToken(
            email = email,
            name = name,
            photoUrl = photoUrl
        )
        val mockCredential = mock<GoogleIdTokenCredential> {
            on { type } doReturn GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            on { data } doReturn Bundle().apply {
                putString(
                    "com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN",
                    mockIdToken
                )
                putString(
                    "com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID",
                    email
                )
                putString(
                    "com.google.android.libraries.identity.googleid.BUNDLE_KEY_DISPLAY_NAME",
                    name
                )
                putParcelable(
                    "com.google.android.libraries.identity.googleid.BUNDLE_KEY_PROFILE_PICTURE_URI",
                    Uri.parse(photoUrl)
                )
            }
            on { displayName } doReturn name
            on { profilePictureUri } doReturn Uri.parse(photoUrl)
        }
        val mockResult = mock<GetCredentialResponse> {
            on { credential } doReturn mockCredential
        }
        whenever(mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()))
            .thenReturn(mockResult)

        // Track auth state changes
        var currentAuthState: AuthState = AuthState.Idle

        composeTestRule.setContent {
            val configuration = authUIConfiguration {
                context = applicationContext
                providers {
                    provider(AuthProvider.Anonymous)
                    provider(
                        AuthProvider.Google(
                            scopes = listOf("email"),
                            serverClientId = "test-server-client-id",
                        )
                    )
                }
            }

            TestAuthScreen(configuration = configuration)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        // Scroll to the Google sign-in button
        composeTestRule
            .onNodeWithTag("AuthMethodPicker LazyColumn")
            .performScrollToNode(hasText(stringProvider.signInWithGoogle))

        // Click the actual Google sign-in button
        composeTestRule
            .onNode(hasText(stringProvider.signInWithGoogle))
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        println("TEST: Pumping looper after click...")
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for auth state to transition to Success
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
        assertThat(authUI.auth.currentUser!!.email).isEqualTo(email)
        assertThat(authUI.auth.currentUser!!.displayName).isEqualTo(name)
        assertThat(authUI.auth.currentUser!!.photoUrl.toString())
            .isEqualTo("https://example.com/avatar.jpg")
    }

    @Composable
    private fun TestAuthScreen(configuration: AuthUIConfiguration) {
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
                                        uiContext.onNavigate(AuthRoute.MethodPicker)
                                    }
                                ) {
                                    Text("Upgrade with Google")
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Generates a mock Google ID token (JWT) with the specified email.
     * This is useful for testing so that the token payload matches the test data.
     */
    private fun generateMockGoogleIdToken(
        email: String,
        sub: String = "test-user-id",
        name: String? = null,
        photoUrl: String? = null
    ): String {
        // JWT Header
        val header = """{"alg":"RS256","kid":"test"}"""

        // JWT Payload with dynamic email
        val payload = buildString {
            append("{")
            append("\"iss\":\"https://accounts.google.com\",")
            append("\"aud\":\"test-client-id\",")
            append("\"sub\":\"$sub\",")
            append("\"email\":\"$email\",")
            append("\"email_verified\":true")
            name?.let { append(",\"name\":\"$it\"") }
            photoUrl?.let { append(",\"picture\":\"$it\"") }
            append(",\"iat\":1689600000,\"exp\":1689603600")
            append("}")
        }

        // Base64 encode header and payload (URL-safe, no padding, no wrap)
        val encodedHeader = Base64.encodeToString(
            header.toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
        val encodedPayload = Base64.encodeToString(
            payload.toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )

        // Return JWT format: header.payload.signature
        // Signature doesn't need to be valid for testing
        return "$encodedHeader.$encodedPayload.mock-signature"
    }
}