package com.firebase.ui.auth.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.testutil.AUTH_STATE_WAIT_TIMEOUT_MS
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.firebase.ui.auth.testutil.EmailLinkTestActivity
import com.firebase.ui.auth.testutil.EmulatorAuthApi
import com.firebase.ui.auth.testutil.awaitWithLooper
import com.firebase.ui.auth.testutil.ensureTestFirebaseApp
import com.firebase.ui.auth.util.EmailLinkParser
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.actionCodeSettings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * End-to-end coverage for email link ("magic link") sign-in, driven entirely through screen taps
 * against the Firebase Auth emulator.
 *
 * Each test walks the same spine — send the link from the email screen, pull the real link back
 * out of the emulator, then hand it to the app as an `ACTION_VIEW` deep link — and differs only in
 * the device/session the link is opened on and whether an anonymous user is being upgraded.
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class EmailLinkAuthScreenTest {
    @get:Rule
    val composeAndroidTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var applicationContext: Context

    private lateinit var stringProvider: AuthUIStringProvider

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
                ?: throw IllegalStateException("Project ID is required for emulator interactions"),
            emulatorHost = "127.0.0.1",
            emulatorPort = 9099
        )

        // The FirebaseApp is shared across test classes, so a class that died before its
        // tearDown can leave a user signed in here.
        authUI.auth.signOut()

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
    fun `email link sign in emits EmailSignInLinkSent auth state, shows dialog and handles deep link sign in`() {
        val email = "emaillink-test-${System.currentTimeMillis()}@example.com"

        var currentAuthState: AuthState = AuthState.Idle
        var pendingEmailLink by mutableStateOf<String?>(null)

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(
                configuration = emailLinkConfiguration(forceSameDevice = true),
                emailLink = pendingEmailLink
            )
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        assertDirectEmailStart()
        submitEmailLinkRequest(email)
        awaitEmailLinkSentDialog()

        // The link alone doesn't sign anyone in; that only happens once it's opened.
        assertThat(authUI.auth.currentUser).isNull()
        dismissEmailLinkSentDialog(email)
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()

        pendingEmailLink = deliverDeepLink(awaitSignInLink(email))

        waitForSuccessState { currentAuthState }

        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.email).isEqualTo(email)

        composeAndroidTestRule.onNodeWithText("AUTHENTICATED - $email")
            .assertIsDisplayed()
    }

    @Test
    fun `anonymous upgrade email link sign in links the credential onto the anonymous user`() {
        val email = "emaillink-anon-upgrade-${System.currentTimeMillis()}@example.com"

        var currentAuthState: AuthState = AuthState.Idle
        var pendingEmailLink by mutableStateOf<String?>(null)

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(
                configuration = anonymousUpgradeConfiguration(forceSameDevice = true),
                emailLink = pendingEmailLink
            )
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        signInAnonymously { currentAuthState }
        val anonymousUid = authUI.auth.currentUser!!.uid

        goToEmailScreenFromAuthenticatedContent()
        submitEmailLinkRequest(email)

        // The confirmation has to survive here too: the anonymous session re-asserts itself as
        // Success right after the link is sent, and that must not navigate the user away.
        awaitEmailLinkSentDialog()

        // The direct regression: the confirmation is only reachable because the flow stayed on
        // the email link step. Without the guard the back stack resets and both disappear.
        composeAndroidTestRule.onNodeWithTag(FirebaseAuthTestTags.EmailLink.SEND_LINK_BUTTON)
            .assertIsDisplayed()

        dismissEmailLinkSentDialog(email)

        pendingEmailLink = deliverDeepLink(awaitSignInLink(email))

        // The auth state is already Success from the anonymous sign-in, so the upgrade has to be
        // observed on the user itself rather than on a state transition.
        waitUntil("anonymous user to be upgraded") {
            authUI.auth.currentUser?.isAnonymous == false
        }

        // Same uid means the email credential was linked onto the existing anonymous account
        // rather than swapped for a brand new one — the whole point of the upgrade flow.
        assertThat(authUI.auth.currentUser!!.uid).isEqualTo(anonymousUid)
        assertThat(authUI.auth.currentUser!!.email).isEqualTo(email)
        assertThat(authUI.auth.currentUser!!.providerData.map { it.providerId })
            .contains("password")

        composeAndroidTestRule.onNodeWithText("UID - $anonymousUid")
            .assertIsDisplayed()
        composeAndroidTestRule.onNodeWithText("isAnonymous - false")
            .assertIsDisplayed()
    }

    @Test
    fun `cross-device email link asks for the email address instead of signing in`() {
        val email = "emaillink-cross-device-${System.currentTimeMillis()}@example.com"

        var pendingEmailLink by mutableStateOf<String?>(null)

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(
                configuration = emailLinkConfiguration(forceSameDevice = false),
                emailLink = pendingEmailLink
            )
        }

        assertDirectEmailStart()

        // A device that didn't send the link has no stored email link session. Completing one
        // sign-in is the only supported way to reach that state — sending a link stores the
        // session and the library only clears it on success — so do that first and step back out.
        clearStoredEmailLinkSession { link -> pendingEmailLink = link }

        // Sending the link from this screen would store its session again, which is the one thing
        // a second device doesn't have, so the link is minted the way the other device would have
        // minted it and this device only ever sees the link itself.
        pendingEmailLink = deliverDeepLink(sendSignInLinkFromAnotherDevice(email))

        // With no stored session the app can't know which address the link was sent to, so it
        // asks rather than signing anyone in.
        awaitErrorDialog(stringProvider.emailLinkPromptForEmailMessage)
        assertThat(authUI.auth.currentUser).isNull()
    }

    @Test
    fun `cross-device email link sign in completes once the user confirms their email address`() {
        val email = "emaillink-cross-device-${System.currentTimeMillis()}@example.com"

        var currentAuthState: AuthState = AuthState.Idle
        var pendingEmailLink by mutableStateOf<String?>(null)

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(
                configuration = emailLinkConfiguration(forceSameDevice = false),
                emailLink = pendingEmailLink
            )
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        assertDirectEmailStart()
        clearStoredEmailLinkSession { link -> pendingEmailLink = link }
        pendingEmailLink = deliverDeepLink(sendSignInLinkFromAnotherDevice(email))

        awaitErrorDialog(stringProvider.emailLinkPromptForEmailMessage)
        composeAndroidTestRule.onNodeWithText(stringProvider.continueText)
            .assertIsDisplayed()
            .performClick()
        composeAndroidTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Recovery lands back on the email screen already in email link mode, so entering the
        // address completes the link that was opened rather than sending a new one.
        submitEmailAddress(email)

        waitForSuccessState { currentAuthState }

        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.email).isEqualTo(email)

        composeAndroidTestRule.onNodeWithText("AUTHENTICATED - $email")
            .assertIsDisplayed()
    }

    @Test
    fun `cross-device email link opened from the method picker seeds the email screen`() {
        val email = "emaillink-picker-${System.currentTimeMillis()}@example.com"

        var currentAuthState: AuthState = AuthState.Idle
        var pendingEmailLink by mutableStateOf<String?>(null)

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(
                configuration = methodPickerEmailLinkConfiguration(),
                emailLink = pendingEmailLink
            )
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        composeAndroidTestRule.waitForIdle()
        openEmailProviderFromMethodPicker()
        clearStoredEmailLinkSession { link -> pendingEmailLink = link }

        // The precondition this test exists for: sitting on the method picker means the email
        // screen isn't composed, so FirebaseAuthScreen — not EmailAuthScreen — is the one that
        // shows the dialog and has to carry the link over to a destination that doesn't exist yet.
        composeAndroidTestRule.onNodeWithText(stringProvider.signInWithEmail)
            .assertIsDisplayed()

        pendingEmailLink = deliverDeepLink(sendSignInLinkFromAnotherDevice(email))

        awaitErrorDialog(stringProvider.emailLinkPromptForEmailMessage)
        composeAndroidTestRule.onNodeWithText(stringProvider.continueText)
            .assertIsDisplayed()
            .performClick()
        composeAndroidTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Recovery lands on the email link step, created fresh and seeded with the link.
        composeAndroidTestRule.onNodeWithTag(FirebaseAuthTestTags.EmailLink.SEND_LINK_BUTTON)
            .performScrollTo()
            .assertIsDisplayed()

        submitEmailAddress(email)

        waitForSuccessState { currentAuthState }

        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.email).isEqualTo(email)
        composeAndroidTestRule.onNodeWithText("AUTHENTICATED - $email")
            .assertIsDisplayed()
    }

    @Test
    fun `cross-device email link for a pending provider link completes after confirming the email`() {
        val email = "emaillink-cross-device-link-${System.currentTimeMillis()}@example.com"

        var currentAuthState: AuthState = AuthState.Idle
        var pendingEmailLink by mutableStateOf<String?>(null)

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(
                configuration = emailLinkConfiguration(forceSameDevice = false),
                emailLink = pendingEmailLink
            )
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        assertDirectEmailStart()
        clearStoredEmailLinkSession { link -> pendingEmailLink = link }

        // A link sent while a social credential was waiting to be linked carries the provider id.
        // The linking half can't be done from here — that credential lives on the other device —
        // so the user is told as much and offered the plain sign-in instead.
        pendingEmailLink = deliverDeepLink(
            sendSignInLinkFromAnotherDevice(email, providerId = GoogleAuthProvider.PROVIDER_ID)
        )

        awaitErrorDialog(stringProvider.emailLinkCrossDeviceLinkingMessage(GOOGLE_PROVIDER_NAME))
        composeAndroidTestRule.onNodeWithText(stringProvider.continueText)
            .assertIsDisplayed()
            .performClick()
        composeAndroidTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        submitEmailAddress(email)

        waitForSuccessState { currentAuthState }

        assertThat(authUI.auth.currentUser).isNotNull()
        assertThat(authUI.auth.currentUser!!.email).isEqualTo(email)
        composeAndroidTestRule.onNodeWithText("AUTHENTICATED - $email")
            .assertIsDisplayed()
    }

    @Test
    fun `email link carrying an anonymous user id is refused on another device`() {
        val email = "emaillink-foreign-anon-${System.currentTimeMillis()}@example.com"

        var pendingEmailLink by mutableStateOf<String?>(null)

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(
                configuration = emailLinkConfiguration(forceSameDevice = false),
                emailLink = pendingEmailLink
            )
        }

        assertDirectEmailStart()

        // An upgrade can only ever be completed where the anonymous session lives, so a link
        // carrying an anonymous user id is refused off-device regardless of its same-device bit.
        // No configuration can produce this pairing — AuthProvider.Email.validate forces
        // same-device whenever anonymous upgrade and email link are both enabled — so the link is
        // minted directly, which is also the shape a tampered or stale link would arrive in.
        pendingEmailLink = deliverDeepLink(
            sendSignInLinkFromAnotherDevice(email, anonymousUserId = "someOtherAnonymousUid")
        )

        awaitErrorDialog(stringProvider.emailLinkWrongDeviceMessage)
        assertThat(authUI.auth.currentUser).isNull()
    }

    @Test
    fun `force same device email link opened on another device shows the wrong device error`() {
        val email = "emaillink-force-same-device-${System.currentTimeMillis()}@example.com"

        var pendingEmailLink by mutableStateOf<String?>(null)

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(
                configuration = emailLinkConfiguration(forceSameDevice = true),
                emailLink = pendingEmailLink
            )
        }

        assertDirectEmailStart()
        submitEmailLinkRequest(email)
        awaitEmailLinkSentDialog()
        dismissEmailLinkSentDialog(email)

        pendingEmailLink = deliverDeepLink(asLinkFromAnotherDevice(awaitSignInLink(email)))

        // Force-same-device is stamped into the link itself, so opening it anywhere else is
        // refused outright — no prompt for the email address.
        awaitErrorDialog(stringProvider.emailLinkWrongDeviceMessage)
        assertThat(authUI.auth.currentUser).isNull()
    }

    @Test
    fun `anonymous upgrade email link opened as a different anonymous user shows the different user error`() {
        val email = "emaillink-anon-mismatch-${System.currentTimeMillis()}@example.com"

        var currentAuthState: AuthState = AuthState.Idle
        var pendingEmailLink by mutableStateOf<String?>(null)

        composeAndroidTestRule.setContent {
            TestFirebaseAuthScreen(
                configuration = anonymousUpgradeConfiguration(forceSameDevice = true),
                emailLink = pendingEmailLink
            )
            val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
            currentAuthState = authState
        }

        signInAnonymously { currentAuthState }
        val anonymousUid = authUI.auth.currentUser!!.uid

        goToEmailScreenFromAuthenticatedContent()
        submitEmailLinkRequest(email)

        val emailLink = awaitSignInLink(email)

        // Same device and same session, but the anonymous user the link was minted for is gone.
        // Swapping the session out from under the link isn't reachable through the UI, so it's
        // done directly on FirebaseAuth here.
        authUI.auth.signOut()
        authUI.auth.signInAnonymously().awaitWithLooper()
        val otherAnonymousUid = authUI.auth.currentUser!!.uid
        assertThat(otherAnonymousUid).isNotEqualTo(anonymousUid)

        pendingEmailLink = deliverDeepLink(emailLink)

        awaitErrorDialog(stringProvider.emailLinkDifferentAnonymousUserMessage)
        assertThat(authUI.auth.currentUser!!.uid).isEqualTo(otherAnonymousUid)
        assertThat(authUI.auth.currentUser!!.isAnonymous).isTrue()
    }

    // region Configuration

    private fun emailLinkProvider(forceSameDevice: Boolean) = AuthProvider.Email(
        isEmailLinkSignInEnabled = true,
        isEmailLinkForceSameDeviceEnabled = forceSameDevice,
        emailLinkActionCodeSettings = actionCodeSettings {
            // The continue URL - where to redirect after the email link is clicked
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

    private fun emailLinkConfiguration(forceSameDevice: Boolean) = authUIConfiguration {
        context = applicationContext
        providers {
            provider(emailLinkProvider(forceSameDevice))
        }
        isCredentialManagerEnabled = false
    }

    /** Two providers, so the flow starts on the method picker rather than straight on email. */
    private fun methodPickerEmailLinkConfiguration() = authUIConfiguration {
        context = applicationContext
        providers {
            provider(AuthProvider.Anonymous)
            provider(emailLinkProvider(forceSameDevice = false))
        }
        isCredentialManagerEnabled = false
    }

    private fun anonymousUpgradeConfiguration(forceSameDevice: Boolean) = authUIConfiguration {
        context = applicationContext
        providers {
            provider(AuthProvider.Anonymous)
            provider(emailLinkProvider(forceSameDevice))
        }
        isAnonymousUpgradeEnabled = true
        isCredentialManagerEnabled = false
    }

    // endregion

    // region Flow steps

    private fun assertDirectEmailStart() {
        composeAndroidTestRule.waitForIdle()
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault)
            .assertIsDisplayed()
    }

    private fun openEmailProviderFromMethodPicker() {
        composeAndroidTestRule.onNodeWithText(stringProvider.signInWithEmail)
            .assertIsDisplayed()
            .performClick()
        assertDirectEmailStart()
    }

    private fun signInAnonymously(currentAuthState: () -> AuthState) {
        composeAndroidTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        composeAndroidTestRule.onNodeWithText(stringProvider.signInAnonymously)
            .assertIsDisplayed()
            .performClick()
        composeAndroidTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        waitUntil("anonymous sign-in to succeed") { currentAuthState() is AuthState.Success }
        composeAndroidTestRule.onNodeWithText("isAnonymous - true")
            .assertIsDisplayed()
    }

    private fun goToEmailScreenFromAuthenticatedContent() {
        composeAndroidTestRule.onNodeWithText(UPGRADE_WITH_EMAIL)
            .assertIsDisplayed()
            .performClick()
        composeAndroidTestRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * Runs one throwaway email link sign-in to completion and signs back out, leaving no stored
     * session behind. Tests in this class share a process — and so a single DataStore — so a
     * session left over from an earlier test would otherwise make this device look like the one
     * that sent the link.
     */
    private fun clearStoredEmailLinkSession(deliverLink: (String) -> Unit) {
        val throwawayEmail = "emaillink-session-reset-${System.currentTimeMillis()}@example.com"

        submitEmailLinkRequest(throwawayEmail)
        awaitEmailLinkSentDialog()
        dismissEmailLinkSentDialog(throwawayEmail)
        deliverLink(deliverDeepLink(awaitSignInLink(throwawayEmail)))

        // Wait on the authenticated content rather than on FirebaseAuth: the library clears the
        // stored session before it publishes Success, so the rendered user is the signal that the
        // clear has actually landed.
        waitUntil("throwaway sign-in to complete and clear the stored session") {
            composeAndroidTestRule.onAllNodesWithText("AUTHENTICATED - $throwawayEmail")
                .fetchSemanticsNodes().isNotEmpty()
        }
        // Signing out sends the flow back to its start route, which is where a device that
        // never sent a link would be sitting. FirebaseAuthUI.signOut needs the Facebook SDK,
        // which this module doesn't depend on, so go through FirebaseAuth directly.
        authUI.auth.signOut()
        waitUntil("sign-out to return to the start of the flow") {
            authUI.auth.currentUser == null
        }
        shadowOf(Looper.getMainLooper()).idle()
        composeAndroidTestRule.waitForIdle()
    }

    private fun submitEmailLinkRequest(email: String) {
        switchToEmailLinkMode()
        submitEmailAddress(email)
    }

    private fun switchToEmailLinkMode() {
        composeAndroidTestRule.onNodeWithText(stringProvider.signInWithEmailLink.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
    }

    private fun submitEmailAddress(email: String) {
        composeAndroidTestRule.onNodeWithText(stringProvider.emailHint)
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput(email)
        composeAndroidTestRule.onNodeWithText(stringProvider.signInDefault.uppercase())
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        shadowOf(Looper.getMainLooper()).idle()
        composeAndroidTestRule.waitForIdle()
    }

    private fun awaitEmailLinkSentDialog() {
        // Wait for the "email link sent" dialog to appear, rather than polling the auth state:
        // the screen resets AuthState back to Idle immediately after consuming
        // EmailSignInLinkSent (so a second, independent authStateFlow() collector — like the
        // tests' currentAuthState — can miss the transient value entirely per StateFlow's
        // conflation contract), whereas the dialog's visibility is latched in local Compose
        // state that isn't reset the same way, so it's a reliable, non-racy signal.
        waitUntil("email link sent dialog") {
            composeAndroidTestRule.onAllNodesWithText(stringProvider.emailSignInLinkSentDialogTitle)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Ensure final recomposition is complete before assertions
        shadowOf(Looper.getMainLooper()).idle()
        composeAndroidTestRule.waitForIdle()
    }

    private fun dismissEmailLinkSentDialog(email: String) {
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
    }

    // endregion

    // region Email link plumbing

    /**
     * Waits for the sign-in link to reach the emulator's outbox. Sending is fire-and-forget from
     * the screen's point of view, so in flows that don't show the confirmation dialog there is
     * nothing on-screen to synchronise on.
     */
    private fun awaitSignInLink(email: String, timeoutMs: Long = LINK_DELIVERY_TIMEOUT_MS): String {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastError: Exception? = null

        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            try {
                return emulatorApi.fetchEmailSignInLink(email)
            } catch (e: Exception) {
                lastError = e
                Thread.sleep(POLL_INTERVAL_MS)
            }
        }

        // Fail rather than skip: a skip here reports the whole class green when the emulator is
        // merely slow, including the one test that pins the production fix.
        throw AssertionError(
            "No EMAIL_SIGNIN link for $email reached the Firebase Auth Emulator within " +
                    "${timeoutMs}ms. Ensure it is running on localhost:9099. " +
                    "Last error: ${lastError?.message}"
        )
    }

    /**
     * Sends a sign-in link straight through [com.google.firebase.auth.FirebaseAuth], stamping the
     * continue URL with a session id this device never saved. That's what makes it a link from
     * somewhere else: going through the screen would store the session locally and the app would
     * recognise the link as its own.
     */
    private fun sendSignInLinkFromAnotherDevice(
        email: String,
        anonymousUserId: String? = null,
        providerId: String? = null,
    ): String {
        authUI.auth.sendSignInLinkToEmail(
            email,
            actionCodeSettings {
                url = "https://fake-project-id.firebaseapp.com" +
                        "?${EmailLinkParser.LinkParameters.SESSION_IDENTIFIER}=$OTHER_DEVICE_SESSION_ID" +
                        "&${EmailLinkParser.LinkParameters.FORCE_SAME_DEVICE_IDENTIFIER}=0" +
                        (anonymousUserId?.let {
                            "&${EmailLinkParser.LinkParameters.ANONYMOUS_USER_ID_IDENTIFIER}=$it"
                        } ?: "") +
                        (providerId?.let {
                            "&${EmailLinkParser.LinkParameters.PROVIDER_ID_IDENTIFIER}=$it"
                        } ?: "")
                handleCodeInApp = true
                setAndroidPackageName(
                    "fake.project.id",
                    true,
                    null
                )
            }
        ).awaitWithLooper()
        return awaitSignInLink(email)
    }

    /**
     * Hands [emailLink] to the app the way Android does when the link is tapped in a mail client —
     * an `ACTION_VIEW` intent that launches an activity, which reads the link back off the intent.
     * Returns the link as the activity saw it, ready to be pushed into the screen under test.
     */
    private fun deliverDeepLink(emailLink: String): String {
        val deepLinkUri = Uri.parse(emailLink)
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, deepLinkUri)

        assertThat(authUI.canHandleIntent(deepLinkIntent)).isTrue()

        val extractedEmailLink =
            ActivityScenario.launch<EmailLinkTestActivity>(deepLinkIntent).use { scenario ->
                var emailLinkFromIntent: String? = null

                scenario.onActivity { activity ->
                    assertThat(activity.intent.action).isEqualTo(Intent.ACTION_VIEW)
                    assertThat(activity.intent.data).isEqualTo(deepLinkUri)
                    assertThat(activity.emailLinkFromIntent).isEqualTo(emailLink)

                    emailLinkFromIntent = activity.emailLinkFromIntent
                }

                emailLinkFromIntent
            }

        requireNotNull(extractedEmailLink) { "Failed to extract email link from intent" }
        shadowOf(Looper.getMainLooper()).idle()
        return extractedEmailLink
    }

    /**
     * Rewrites the link's `ui_sid` so it no longer matches the session id this device saved when it
     * sent the link, which is exactly what `AuthProvider.Email.isDifferentDevice` keys off. The
     * session id lives in the (percent-encoded) continue URL and is independent of the `oobCode`,
     * so the link still validates against the emulator — it just looks like it arrived somewhere
     * else. Rewriting is preferred over clearing the saved session directly: the persistence
     * manager is `internal` and backed by a process-wide DataStore cache, so the only supported
     * way to clear it is [clearStoredEmailLinkSession], which costs a whole extra sign-in.
     */
    private fun asLinkFromAnotherDevice(emailLink: String): String {
        val rewritten = SESSION_ID_PARAM.replace(emailLink) { match ->
            match.groupValues[1] + "0".repeat(match.groupValues[2].length)
        }
        require(rewritten != emailLink) {
            "Expected a ui_sid parameter to rewrite in the email link: $emailLink"
        }
        return rewritten
    }

    // endregion

    // region Waiting

    private fun waitForSuccessState(currentAuthState: () -> AuthState) {
        waitUntil("auth state to become Success") { currentAuthState() is AuthState.Success }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun awaitErrorDialog(message: String) {
        waitUntil("error dialog: $message") {
            composeAndroidTestRule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
        shadowOf(Looper.getMainLooper()).idle()
        composeAndroidTestRule.waitForIdle()

        composeAndroidTestRule.onNodeWithText(stringProvider.errorDialogTitle)
            .assertIsDisplayed()
        composeAndroidTestRule.onNodeWithText(message)
            .assertIsDisplayed()
    }

    private fun waitUntil(what: String, condition: () -> Boolean) {
        composeAndroidTestRule.waitUntil(timeoutMillis = AUTH_STATE_WAIT_TIMEOUT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    // endregion

    @Composable
    private fun TestFirebaseAuthScreen(
        configuration: AuthUIConfiguration,
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
            ) { state, uiContext ->
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state is AuthState.Success) {
                        Text("AUTHENTICATED - ${state.user.email}", textAlign = TextAlign.Center)
                        Text("UID - ${state.user.uid}", textAlign = TextAlign.Center)
                        Text(
                            "isAnonymous - ${state.user.isAnonymous}",
                            textAlign = TextAlign.Center
                        )
                        if (state.user.isAnonymous) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { uiContext.onNavigate(AuthRoute.Email) }) {
                                Text(UPGRADE_WITH_EMAIL)
                            }
                        }
                    } else {
                        Text("NOT AUTHENTICATED")
                    }
                }
            }
        }
    }

    private companion object {
        const val UPGRADE_WITH_EMAIL = "Upgrade with Email"

        /** Stands in for the session id the *other* device saved when it sent the link. */
        const val OTHER_DEVICE_SESSION_ID = "otherdevic"

        /** Mirrors the internal Provider enum's display name for google.com. */
        const val GOOGLE_PROVIDER_NAME = "Google"

        const val LINK_DELIVERY_TIMEOUT_MS = 10_000L
        const val POLL_INTERVAL_MS = 50L

        /** Matches `ui_sid=<value>` in both its plain and percent-encoded forms. */
        val SESSION_ID_PARAM = Regex("(ui_sid(?:=|%3D))([A-Za-z0-9]+)", RegexOption.IGNORE_CASE)
    }
}
