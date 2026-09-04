package com.firebase.ui.auth.ui.screens

import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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
import com.firebase.ui.auth.testutil.ensureTestFirebaseApp
import com.firebase.ui.auth.testutil.verifyEmailInEmulator
import com.firebase.ui.auth.ui.screens.reauth.ReauthContentState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.yield
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

    /** Runs `withReauth` on the looper these tests already pump. */
    private val reauthScope = CoroutineScope(Dispatchers.Main.immediate)

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

        val firebaseApp = ensureTestFirebaseApp(applicationContext)
        authUI = FirebaseAuthUI.getInstance()

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
        reauthScope.cancel()
        authUI.auth.signOut()
        FirebaseAuthUI.clearInstanceCache()
        emulatorApi.clearEmulatorData()
    }

    /**
     * Full cycle: sign in via the main flow, then emit Reauthentication.Required to simulate a
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
        var attempts = 0

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

        // Step 2: Emit Reauthentication.Required to simulate an operation requiring reauth.
        // A real sensitive operation: the first attempt fails the way Firebase fails
        // one, so `withReauth` raises the request itself rather than the test poking
        // a state object. It suspends here until the sheet resolves it.
        reauthScope.launch {
            runCatching {
                authUI.withReauth(applicationContext, reason = "Please verify your identity to continue") {
                    if (attempts++ == 0) throw FirebaseAuthRecentLoginRequiredException(
                        "ERROR_REQUIRES_RECENT_LOGIN", "Recent login required"
                    )
            retryOperationCalled = true
                }
            }
        }

        shadowOf(Looper.getMainLooper()).idle()

        // Wait for the reauth bottom sheet email form to appear (now the only email form visible).
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeAndroidTestRule.onAllNodesWithText(stringProvider.emailHint)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeAndroidTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .assertTextContains(email)
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
     * Verifies the [ReauthContentState] contract for the custom reauthContent slot: it receives the
     * reauthenticating user, the reason, and the configured providers already filtered to the ones
     * linked to that user; dismissing it drops the pending retry operation without firing it.
     *
     * The user stays signed in, as they always are during reauthentication. That is why dismissing
     * does *not* leave the state on [AuthState.Idle]: `onDismiss` resets the library's internal
     * state, and `authStateFlow()` then falls back to the live session, which is an
     * [AuthState.Success] for the session that already existed.
     */
    @Test
    fun `custom reauthContent receives linked providers and dismisses without retrying`() {
        val email = "reauth-custom-${System.currentTimeMillis()}@example.com"
        val password = "test123"

        val user = ensureFreshUser(authUI, email, password)
        requireNotNull(user) { "Failed to create user" }

        try {
            verifyEmailInEmulator(authUI, emulatorApi, user)
        } catch (e: Exception) {
            Assume.assumeTrue(
                "Skipping: Firebase Auth Emulator OOB codes not available. Error: ${e.message}",
                false
            )
        }

        val capturedUser = requireNotNull(authUI.auth.currentUser) { "User must be signed in after creation" }

        var currentAuthState: AuthState = AuthState.Idle
        var retryOperationCalled = false
        var attempts = 0
        var capturedState: ReauthContentState? = null
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
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = null
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
                    reauthContent = { reauthState ->
                        capturedState = reauthState
                        Column {
                            Text("REAUTH REQUIRED - ${reauthState.reason}")
                            Button(onClick = reauthState.onDismiss) { Text("DISMISS REAUTH") }
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

        // Emit Reauthentication.Required to trigger the custom reauthContent slot.
        // A real sensitive operation: the first attempt fails the way Firebase fails
        // one, so `withReauth` raises the request itself rather than the test poking
        // a state object. It suspends here until the sheet resolves it.
        reauthScope.launch {
            runCatching {
                authUI.withReauth(applicationContext, reason = expectedReason) {
                    if (attempts++ == 0) throw FirebaseAuthRecentLoginRequiredException(
                        "ERROR_REQUIRES_RECENT_LOGIN", "Recent login required"
                    )
            retryOperationCalled = true
                }
            }
        }

        shadowOf(Looper.getMainLooper()).idle()

        // Verify the custom reauth content is displayed with the correct reason.
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeAndroidTestRule.onAllNodesWithText("REAUTH REQUIRED - $expectedReason")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeAndroidTestRule.onNodeWithText("REAUTH REQUIRED - $expectedReason")
            .assertIsDisplayed()

        val state = requireNotNull(capturedState) { "reauthContent was never composed" }
        assertThat(state.user.uid).isEqualTo(capturedUser.uid)
        assertThat(state.reason).isEqualTo(expectedReason)
        assertThat(state.providers.map { it.providerId }).containsExactly("password")

        composeAndroidTestRule.onNodeWithText("DISMISS REAUTH").performClick()

        shadowOf(Looper.getMainLooper()).idle()

        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeAndroidTestRule.onAllNodesWithText("CONTENT").fetchSemanticsNodes().isNotEmpty()
        }

        composeAndroidTestRule.onAllNodesWithText("REAUTH REQUIRED - $expectedReason")
            .assertCountEquals(0)
        val observedState = currentAuthState
        assertThat(observedState).isInstanceOf(AuthState.Success::class.java)
        assertThat((observedState as AuthState.Success).user.uid).isEqualTo(capturedUser.uid)
        assertThat(observedState.result).isNull()
        assertThat(retryOperationCalled).isFalse()
    }

    /**
     * The custom slot only picks a provider: selecting email makes the library present its own
     * email sub-flow (prefilled with the user's address), and completing it fires the pending
     * retry operation — mirroring the default bottom sheet path.
     */
    @Test
    fun `reauth through the custom slot email sub-flow triggers the retry operation`() {
        val email = "reauth-slot-email-${System.currentTimeMillis()}@example.com"
        val password = "test123"

        val user = ensureFreshUser(authUI, email, password)
        requireNotNull(user) { "Failed to create user" }

        try {
            verifyEmailInEmulator(authUI, emulatorApi, user)
        } catch (e: Exception) {
            Assume.assumeTrue(
                "Skipping: Firebase Auth Emulator OOB codes not available. Error: ${e.message}",
                false
            )
        }

        val signedInUser = requireNotNull(authUI.auth.currentUser) { "User must be signed in" }

        var retryOperationCalled = false
        var attempts = 0

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
                    reauthContent = { reauthState ->
                        Column {
                            Text("PICK A PROVIDER")
                            reauthState.providers.forEach { provider ->
                                Button(
                                    onClick = { reauthState.onProviderSelected(provider) }
                                ) { Text("USE ${provider.providerId}") }
                            }
                        }
                    },
                ) { _, _ ->
                    Text("AUTHENTICATED")
                }
            }
        }

        shadowOf(Looper.getMainLooper()).idle()

        // A real sensitive operation: the first attempt fails the way Firebase fails
        // one, so `withReauth` raises the request itself rather than the test poking
        // a state object. It suspends here until the sheet resolves it.
        reauthScope.launch {
            runCatching {
                authUI.withReauth(applicationContext, reason = "Please verify your identity to continue") {
                    if (attempts++ == 0) throw FirebaseAuthRecentLoginRequiredException(
                        "ERROR_REQUIRES_RECENT_LOGIN", "Recent login required"
                    )
            retryOperationCalled = true
                }
            }
        }

        shadowOf(Looper.getMainLooper()).idle()

        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeAndroidTestRule.onAllNodesWithText("USE password")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeAndroidTestRule.onNodeWithText("USE password").performClick()
        shadowOf(Looper.getMainLooper()).idle()

        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeAndroidTestRule.onAllNodesWithText(email).fetchSemanticsNodes().isNotEmpty()
        }

        composeAndroidTestRule.onNodeWithText(stringProvider.passwordHint)
            .performScrollTo()
            .performTextInput(password)
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .performClick()

        shadowOf(Looper.getMainLooper()).idle()

        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            retryOperationCalled
        }

        assertThat(retryOperationCalled).isTrue()
    }

    @Test
    fun `wrong password during reauth does not fire the pending retry operation`() {
        val email = "reauth-wrong-pw-${System.currentTimeMillis()}@example.com"
        val password = "test123"
        val wrongPassword = "wrong-password"

        val user = ensureFreshUser(authUI, email, password)
        requireNotNull(user) { "Failed to create user" }

        try {
            verifyEmailInEmulator(authUI, emulatorApi, user)
        } catch (e: Exception) {
            Assume.assumeTrue(
                "Skipping: Firebase Auth Emulator OOB codes not available. Error: ${e.message}",
                false
            )
        }

        authUI.auth.signOut()
        shadowOf(Looper.getMainLooper()).idle()

        var currentAuthState: AuthState = AuthState.Idle
        var retryOperationCalled = false
        var attempts = 0

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

        // Step 1: complete initial sign-in via the main screen form (correct password).
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
        composeAndroidTestRule.onNodeWithText("AUTHENTICATED").assertIsDisplayed()

        val signedInUser = requireNotNull(authUI.auth.currentUser) { "User must be signed in" }

        // Step 2: emit Reauthentication.Required with a retryOperation.
        // A real sensitive operation: the first attempt fails the way Firebase fails
        // one, so `withReauth` raises the request itself rather than the test poking
        // a state object. It suspends here until the sheet resolves it.
        reauthScope.launch {
            runCatching {
                authUI.withReauth(applicationContext, reason = "Please verify your identity to continue") {
                    if (attempts++ == 0) throw FirebaseAuthRecentLoginRequiredException(
                        "ERROR_REQUIRES_RECENT_LOGIN", "Recent login required"
                    )
            retryOperationCalled = true
                }
            }
        }

        shadowOf(Looper.getMainLooper()).idle()

        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeAndroidTestRule.onAllNodesWithText(stringProvider.emailHint)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeAndroidTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .assertTextContains(email)
        composeAndroidTestRule.onNodeWithText(stringProvider.passwordHint)
            .performScrollTo()
            .performTextInput(wrongPassword)
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .performClick()

        shadowOf(Looper.getMainLooper()).idle()

        // The error dialog surfaces the failed reauth attempt.
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeAndroidTestRule.onAllNodesWithText(stringProvider.errorDialogTitle)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Dismiss the error dialog, which self-consumes Error -> Idle on the shared authUI.
        composeAndroidTestRule.onNodeWithText(stringProvider.dismissAction).performClick()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(retryOperationCalled).isFalse()
    }

    /**
     * End-to-end cover for a sensitive operation that signs the user out as its own success
     * condition — `delete()` is the canonical one. `signOut()` stands in for it: same listener
     * path, same `currentUser == null`, without depending on the emulator honouring a
     * recent-login check.
     *
     * This is coverage of the full cycle, not a proof of the `isReauthenticated` guard in
     * `FirebaseAuthUI`'s auth-state listener: removing that guard does not fail this test. Real
     * `FirebaseAuth` posts its listener notification to the looper, so whether the request is
     * cleared before or after the retry coroutine resumes is not deterministic here. The guard is
     * pinned by `FirebaseAuthScreenReauthIdleResetTest.an operation that signs the user out is
     * reported as completed, not interrupted`, which mocks `FirebaseAuth` and fires the listener
     * synchronously from inside the operation to force the ordering.
     */
    @Test
    fun `an operation that signs the user out completes instead of reporting an interruption`() {
        val email = "reauth-signout-${System.currentTimeMillis()}@example.com"
        val password = "test123"

        val user = ensureFreshUser(authUI, email, password)
        requireNotNull(user) { "Failed to create user" }

        try {
            verifyEmailInEmulator(authUI, emulatorApi, user)
        } catch (e: Exception) {
            Assume.assumeTrue(
                "Skipping: Firebase Auth Emulator OOB codes not available. Error: ${e.message}",
                false
            )
        }

        authUI.auth.signOut()
        shadowOf(Looper.getMainLooper()).idle()

        var currentAuthState: AuthState = AuthState.Idle
        var retryOperationStarted = false
        var attempts = 0
        var retryOperationCompleted = false

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

        // Step 1: initial sign-in through the main screen.
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
        composeAndroidTestRule.onNodeWithText("AUTHENTICATED").assertIsDisplayed()

        val signedInUser = requireNotNull(authUI.auth.currentUser) { "User must be signed in" }

        // Step 2: arm a request whose operation signs the user out, as delete() would.
        // A real sensitive operation: the first attempt fails the way Firebase fails
        // one, so `withReauth` raises the request itself rather than the test poking
        // a state object. It suspends here until the sheet resolves it.
        reauthScope.launch {
            runCatching {
                authUI.withReauth(applicationContext, reason = "Please verify your identity to continue") {
                    if (attempts++ == 0) throw FirebaseAuthRecentLoginRequiredException(
                        "ERROR_REQUIRES_RECENT_LOGIN", "Recent login required"
                    )
            retryOperationStarted = true
            authUI.auth.signOut()
            // A suspension point makes a dropped operation observable: the retry runs on
            // this scope, so anything that cancelled it would stop here.
            yield()
            retryOperationCompleted = true
                }
            }
        }

        shadowOf(Looper.getMainLooper()).idle()
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            composeAndroidTestRule.onAllNodesWithText(stringProvider.emailHint)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Step 3: reauthenticate, which runs the signing-out operation.
        composeAndroidTestRule.onNodeWithText(stringProvider.passwordHint)
            .performScrollTo()
            .performTextInput(password)
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .performClick()

        shadowOf(Looper.getMainLooper()).idle()
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            retryOperationStarted && currentAuthState !is AuthState.Reauthentication
        }

        // Settle before asserting an absence: dropping the request mid-retry surfaces the
        // interruption a frame or two later, and asserting too early would miss it.
        repeat(5) {
            shadowOf(Looper.getMainLooper()).idle()
            composeAndroidTestRule.waitForIdle()
        }

        // The operation ran to completion, and its sign-out is the outcome — not an interruption.
        assertThat(retryOperationStarted).isTrue()
        assertThat(retryOperationCompleted).isTrue()
        assertThat(authUI.auth.currentUser).isNull()
        assertThat(currentAuthState).isInstanceOf(AuthState.Idle::class.java)
        composeAndroidTestRule.onAllNodesWithText(stringProvider.errorDialogTitle)
            .assertCountEquals(0)
    }

}
