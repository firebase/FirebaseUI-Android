package com.firebase.ui.auth.ui.screens

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
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
import com.firebase.ui.auth.testutil.awaitWithLooper
import com.firebase.ui.auth.testutil.ensureFreshUser
import com.firebase.ui.auth.testutil.generateMockGoogleIdToken
import com.firebase.ui.auth.testutil.verifyEmailInEmulator
import com.firebase.ui.auth.util.CountryUtils
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume
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
import org.robolectric.annotation.LooperMode

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class CredentialLinkingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockCredentialManager: CredentialManager

    private lateinit var applicationContext: Context
    private lateinit var stringProvider: AuthUIStringProvider
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var emulatorApi: EmulatorAuthApi

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        applicationContext = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(applicationContext)

        FirebaseApp.getApps(applicationContext).forEach { app ->
            app.delete()
        }

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

        authUI.testCredentialManagerProvider = object : AuthProvider.Google.CredentialManagerProvider {
            override suspend fun getGoogleCredential(
                context: Context,
                credentialManager: CredentialManager,
                serverClientId: String,
                filterByAuthorizedAccounts: Boolean,
                autoSelectEnabled: Boolean,
            ): AuthProvider.Google.GoogleSignInResult {
                return AuthProvider.Google.DefaultCredentialManagerProvider().getGoogleCredential(
                    context = context,
                    credentialManager = mockCredentialManager,
                    serverClientId = serverClientId,
                    filterByAuthorizedAccounts = filterByAuthorizedAccounts,
                    autoSelectEnabled = autoSelectEnabled,
                )
            }

            override suspend fun clearCredentialState(context: Context, credentialManager: CredentialManager) {}
        }

        emulatorApi = EmulatorAuthApi(
            projectId = firebaseApp.options.projectId
                ?: throw IllegalStateException("Project ID is required for emulator interactions"),
            emulatorHost = "127.0.0.1",
            emulatorPort = 9099
        )

        emulatorApi.clearEmulatorData()
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        emulatorApi.clearEmulatorData()
    }

    @Test
    fun `isCredentialLinkingEnabled links phone to existing email user preserving UID`() {
        val email = "credentiallink@example.com"
        val password = "Test@123"
        val phone = "2025550123"
        val country = CountryUtils.findByCountryCode("US")!!

        // Step 1: Create an email/password user, verify their email, and sign in
        println("TEST: Creating email/password user...")
        val createdUser = ensureFreshUser(authUI, email, password)
        requireNotNull(createdUser) { "Failed to create user" }

        println("TEST: Verifying email in emulator...")
        verifyEmailInEmulator(authUI, emulatorApi, createdUser)

        val signInResult = authUI.auth.signInWithEmailAndPassword(email, password).awaitWithLooper()
        val originalUID = signInResult.user!!.uid
        println("TEST: Signed in as $email, UID: $originalUID")

        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.isAnonymous).isFalse()
        assertThat(authUI.auth.currentUser!!.isEmailVerified).isTrue()

        // Step 2: Set up auth screen with isCredentialLinkingEnabled + phone provider
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = country.countryCode,
                        allowedCountries = null,
                        timeout = 60L,
                    )
                )
            }
            isCredentialLinkingEnabled = true
            isCredentialManagerEnabled = false
        }

        var currentAuthState: AuthState = AuthState.Idle

        composeTestRule.setContent {
            TestAuthScreen(configuration = configuration)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for the authenticated content to render
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            currentAuthState is AuthState.Success
        }

        // Step 3: Navigate to phone auth from the authenticated content slot
        composeTestRule.onNodeWithText("Link Phone")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Step 4: Enter phone number and request verification code
        println("TEST: Entering phone number...")
        composeTestRule.onNodeWithText(stringProvider.phoneNumberHint)
            .assertIsDisplayed()
            .performTextInput(phone)

        composeTestRule.onNodeWithText(stringProvider.sendVerificationCode.uppercase())
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Step 5: Fetch verification code from emulator
        println("TEST: Fetching phone verification code...")
        var phoneCode: String? = null
        var retries = 0
        val maxRetries = 5
        while (phoneCode == null && retries < maxRetries) {
            Thread.sleep(if (retries == 0) 200L else 500L * retries)
            shadowOf(Looper.getMainLooper()).idle()
            try {
                phoneCode = emulatorApi.fetchVerifyPhoneCode(phone)
                println("TEST: Found phone code after ${retries + 1} attempts")
            } catch (e: Exception) {
                retries++
                if (retries >= maxRetries) {
                    Assume.assumeTrue(
                        "Skipping test: Firebase Auth Emulator not available. Error: ${e.message}",
                        false
                    )
                }
                println("TEST: Phone code not found yet, retrying... (attempt $retries/$maxRetries)")
            }
        }
        requireNotNull(phoneCode) { "Phone code should not be null at this point" }

        // Step 6: Enter verification code
        println("TEST: Entering verification code: $phoneCode")
        val textFields = composeTestRule.onAllNodes(hasSetTextAction())
        phoneCode.forEachIndexed { index, digit ->
            composeTestRule.waitForIdle()
            textFields[index].performTextInput(digit.toString())
        }

        composeTestRule.onNodeWithText(stringProvider.verifyPhoneNumber.uppercase())
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Step 7: Wait for success
        println("TEST: Waiting for auth state change after phone verification...")
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state: $currentAuthState")
            currentAuthState is AuthState.Success
        }

        // Step 8: Verify the UID is preserved (linking happened, not a new account)
        val linkedUser = authUI.auth.currentUser!!
        println("TEST: Original UID: $originalUID, Linked UID: ${linkedUser.uid}")
        assertThat(linkedUser.uid).isEqualTo(originalUID)
        assertThat(linkedUser.email).isEqualTo(email)
        assertThat(linkedUser.phoneNumber).isEqualTo(
            CountryUtils.formatPhoneNumber(country.dialCode, phone)
        )
        val providerIds = linkedUser.providerData.map { it.providerId }
        assertThat(providerIds).contains("password")
        assertThat(providerIds).contains("phone")
    }

    @Test
    fun `isCredentialLinkingEnabled links Google to existing email user preserving UID`() = runTest {
        val email = "googlelinktest@example.com"
        val password = "Test@123"
        val googleEmail = "googlelinktest@gmail.com"
        val googleName = "Google Link Test User"
        val googlePhotoUrl = "https://example.com/avatar.jpg"

        // Step 1: Create an email/password user, verify their email, and sign in
        println("TEST: Creating email/password user...")
        val createdUser = ensureFreshUser(authUI, email, password)
        requireNotNull(createdUser) { "Failed to create user" }

        println("TEST: Verifying email in emulator...")
        verifyEmailInEmulator(authUI, emulatorApi, createdUser)

        val signInResult = authUI.auth.signInWithEmailAndPassword(email, password).awaitWithLooper()
        val originalUID = signInResult.user!!.uid
        println("TEST: Signed in as $email, UID: $originalUID")

        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.isAnonymous).isFalse()
        assertThat(authUI.auth.currentUser!!.isEmailVerified).isTrue()

        // Step 2: Configure mock Google credential
        val mockIdToken = generateMockGoogleIdToken(
            email = googleEmail,
            name = googleName,
            photoUrl = googlePhotoUrl,
        )
        val mockCredential = mock<GoogleIdTokenCredential> {
            on { type } doReturn GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            on { data } doReturn Bundle().apply {
                putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN", mockIdToken)
                putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID", googleEmail)
                putString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_DISPLAY_NAME", googleName)
                putParcelable("com.google.android.libraries.identity.googleid.BUNDLE_KEY_PROFILE_PICTURE_URI", Uri.parse(googlePhotoUrl))
            }
            on { displayName } doReturn googleName
            on { profilePictureUri } doReturn Uri.parse(googlePhotoUrl)
        }
        val mockResult = mock<GetCredentialResponse> {
            on { credential } doReturn mockCredential
        }
        whenever(mockCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()))
            .thenReturn(mockResult)

        // Step 3: Set up auth screen with isCredentialLinkingEnabled + Google provider
        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf("email"),
                        serverClientId = "test-server-client-id",
                    )
                )
            }
            isCredentialLinkingEnabled = true
            isCredentialManagerEnabled = false
        }

        var currentAuthState: AuthState = AuthState.Idle

        composeTestRule.setContent {
            TestAuthScreen(configuration = configuration)
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Wait for authenticated content to render
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            currentAuthState is AuthState.Success
        }

        // Step 4: Click "Link Google" from authenticated content
        composeTestRule.onNodeWithText("Link Google")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Step 5: Click the Google sign-in button on the method picker
        println("TEST: Clicking Google sign-in button...")
        composeTestRule
            .onNodeWithTag("AuthMethodPicker LazyColumn")
            .performScrollToNode(hasText(stringProvider.signInWithGoogle))
        composeTestRule
            .onNode(hasText(stringProvider.signInWithGoogle))
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Step 6: Wait for linking to complete
        println("TEST: Waiting for Google linking to complete...")
        composeTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            println("TEST: Auth state: $currentAuthState")
            currentAuthState is AuthState.Success
        }

        // Step 7: Verify the UID is preserved and Google provider is added
        val linkedUser = authUI.auth.currentUser!!
        println("TEST: Original UID: $originalUID, Linked UID: ${linkedUser.uid}")
        assertThat(linkedUser.uid).isEqualTo(originalUID)
        assertThat(linkedUser.email).isEqualTo(email)
        val providerIds = linkedUser.providerData.map { it.providerId }
        assertThat(providerIds).contains("password")
        assertThat(providerIds).contains("google.com")
    }

    @Composable
    private fun TestAuthScreen(configuration: AuthUIConfiguration) {
        composeTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        FirebaseAuthScreen(
            configuration = configuration,
            authUI = authUI,
            onSignInSuccess = {},
            onSignInFailure = { _: AuthException -> },
            onSignInCancelled = {},
            authenticatedContent = { state, uiContext ->
                when (state) {
                    is AuthState.Success -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("UID - ${state.user.uid}")
                            Text("Email - ${state.user.email}")
                            Text("Phone - ${state.user.phoneNumber}")
                            Button(onClick = { uiContext.onNavigate(AuthRoute.Phone) }) {
                                Text("Link Phone")
                            }
                            Button(onClick = { uiContext.onNavigate(AuthRoute.MethodPicker) }) {
                                Text("Link Google")
                            }
                        }
                    }
                }
            }
        )
    }
}
