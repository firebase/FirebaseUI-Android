/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.ui.screens.phone

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.ui.components.LocalTopLevelDialogController
import com.firebase.ui.auth.ui.components.TopLevelDialogController
import com.firebase.ui.auth.ui.components.rememberTopLevelDialogController
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.auth.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class PhoneAuthScreenVerificationLifecycleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var app: FirebaseApp
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var authUI: FirebaseAuthUI
    private lateinit var configuration: AuthUIConfiguration
    private var capturedState: PhoneAuthContentState? = null
    private val reportedErrors = mutableListOf<AuthException>()

    @Before
    fun setUp() {
        FirebaseAuthUI.clearInstanceCache()
        context = ApplicationProvider.getApplicationContext()
        FirebaseApp.getApps(context).forEach { it.delete() }
        app = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )

        mockAuth = mock(FirebaseAuth::class.java)
        `when`(mockAuth.app).thenReturn(app)
        authUI = FirebaseAuthUI.create(app, mockAuth)

        // timeout = 0 keeps the resend countdown at zero, so resend is available immediately and
        // no 1-second ticking effect is left pending between assertions.
        configuration = phoneConfiguration(timeout = 0L)
    }

    private fun phoneConfiguration(timeout: Long): AuthUIConfiguration = authUIConfiguration {
        context = this@PhoneAuthScreenVerificationLifecycleTest.context
        providers {
            provider(
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = null,
                    allowedCountries = null,
                    timeout = timeout
                )
            )
        }
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        FirebaseApp.getApps(context).forEach { it.delete() }
    }

    /**
     * Looks up the callbacks stashed inside [PhoneAuthOptions]. There's no public accessor - only
     * an obfuscated zero-arg method returning
     * [PhoneAuthProvider.OnVerificationStateChangedCallbacks], so we locate it reflectively and
     * assert exactly one such method exists.
     */
    private fun extractCallbacks(options: PhoneAuthOptions): OnVerificationStateChangedCallbacks {
        val candidates = PhoneAuthOptions::class.java.declaredMethods.filter {
            it.parameterCount == 0 &&
                it.returnType == OnVerificationStateChangedCallbacks::class.java
        }
        check(candidates.size == 1) {
            "Expected exactly one zero-arg accessor returning " +
                "OnVerificationStateChangedCallbacks on PhoneAuthOptions, found " +
                "${candidates.size}: $candidates"
        }
        return candidates.single().also { it.isAccessible = true }
            .invoke(options) as OnVerificationStateChangedCallbacks
    }

    /** The callbacks Firebase was handed by the most recent verification attempt. */
    private fun latestCallbacks(
        statics: MockedStatic<PhoneAuthProvider>
    ): OnVerificationStateChangedCallbacks {
        val captor = ArgumentCaptor.forClass(PhoneAuthOptions::class.java)
        statics.verify({ PhoneAuthProvider.verifyPhoneNumber(captor.capture()) }, atLeastOnce())
        return extractCallbacks(captor.allValues.last())
    }

    private fun stubGetCredential(
        statics: MockedStatic<PhoneAuthProvider>,
        credential: PhoneAuthCredential,
    ) {
        statics.`when`<PhoneAuthCredential> {
            PhoneAuthProvider.getCredential(any(), any())
        }.thenReturn(credential)
    }

    /**
     * @param withDialogs installs a real [TopLevelDialogController] and renders its dialog, so
     * tests can assert on what the screen actually puts in front of the user.
     */
    private fun setScreenContent(withDialogs: Boolean = false) {
        composeTestRule.setContent {
            val controller = rememberTopLevelDialogController(configuration.stringProvider) {
                AuthState.Idle
            }
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides configuration.stringProvider,
                LocalTopLevelDialogController provides controller.takeIf { withDialogs },
            ) {
                PhoneAuthScreen(
                    context = context,
                    configuration = configuration,
                    authUI = authUI,
                    onSuccess = {},
                    onError = { reportedErrors += it },
                    onCancel = {},
                ) { state -> capturedState = state }
            }
            if (withDialogs) controller.CurrentDialog()
        }
        composeTestRule.waitForIdle()
    }

    /**
     * Cancelling a verification lands its terminal emission a dispatch or two later, so pump the
     * clock until nothing is left in flight.
     */
    private fun settle() {
        repeat(3) { composeTestRule.waitForIdle() }
    }

    private fun onUi(block: (PhoneAuthContentState) -> Unit) {
        composeTestRule.runOnUiThread { block(capturedState!!) }
        composeTestRule.waitForIdle()
    }

    private fun sendCode(
        statics: MockedStatic<PhoneAuthProvider>
    ): OnVerificationStateChangedCallbacks {
        onUi { it.onPhoneNumberChange("5555550123") }
        onUi { it.onSendCodeClick() }
        return latestCallbacks(statics)
    }

    private fun codeSent(callbacks: OnVerificationStateChangedCallbacks, verificationId: String) {
        composeTestRule.runOnUiThread {
            callbacks.onCodeSent(
                verificationId,
                mock(PhoneAuthProvider.ForceResendingToken::class.java)
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun submitCode(code: String) {
        onUi { it.onVerificationCodeChange(code) }
        onUi { it.onVerifyCodeClick() }
    }

    private fun autoVerified(
        callbacks: OnVerificationStateChangedCallbacks,
        credential: PhoneAuthCredential,
    ) {
        composeTestRule.runOnUiThread { callbacks.onVerificationCompleted(credential) }
        composeTestRule.waitForIdle()
    }

    /** No email on the user, so the success state isn't diverted to RequiresEmailVerification. */
    private fun signedInResult(): AuthResult {
        val result = mock(AuthResult::class.java)
        `when`(result.user).thenReturn(mock(FirebaseUser::class.java))
        return result
    }

    /** A signed-in user linked to the phone provider, as a reauthentication requires. */
    private fun phoneUser(): FirebaseUser {
        val providerInfo = mock(UserInfo::class.java)
        `when`(providerInfo.providerId).thenReturn("phone")
        val user = mock(FirebaseUser::class.java)
        `when`(user.providerData).thenReturn(listOf(providerInfo))
        `when`(user.uid).thenReturn("uid-phone")
        `when`(user.email).thenReturn(null)
        return user
    }

    private fun multiFactorException(): FirebaseAuthMultiFactorException {
        val resolver = mock(MultiFactorResolver::class.java)
        `when`(resolver.hints).thenReturn(emptyList())
        val exception = mock(FirebaseAuthMultiFactorException::class.java)
        `when`(exception.resolver).thenReturn(resolver)
        return exception
    }

    @Test
    fun `late auto-verification does not start a second sign-in during manual submit`() {
        val credential = mock(PhoneAuthCredential::class.java)
        // Never completed: the manually submitted code stays in flight.
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(TaskCompletionSource<AuthResult>().task)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent()
            val callbacks = sendCode(statics)
            codeSent(callbacks, "verification-id-1")
            submitCode("123456")

            autoVerified(callbacks, credential)

            verify(mockAuth, times(1)).signInWithCredential(any())
        }
    }

    @Test
    fun `suppressed auto-verification leaves the manual submit's loading state intact`() {
        val credential = mock(PhoneAuthCredential::class.java)
        // Never completed: the manually submitted code stays in flight.
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(TaskCompletionSource<AuthResult>().task)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent()
            val callbacks = sendCode(statics)
            codeSent(callbacks, "verification-id-1")
            submitCode("123456")
            assertThat(capturedState!!.isLoading).isTrue()

            autoVerified(callbacks, credential)

            // isLoading gates Verify and Resend. Dropping it mid-sign-in re-enables both, so a
            // second tap would start a duplicate sign-in with the same credential.
            // Loading also means the credential was consumed: SMSAutoVerified is no longer the
            // current state, so it cannot leak to a freshly composed screen.
            assertThat(capturedState!!.isLoading).isTrue()
            verify(mockAuth, times(1)).signInWithCredential(any())
        }
    }

    @Test
    fun `a cooldown-rejected send leaves the in-flight verification alive`() {
        configuration = phoneConfiguration(timeout = 60L)
        val credential = mock(PhoneAuthCredential::class.java)
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(TaskCompletionSource<AuthResult>().task)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent()
            val first = sendCode(statics)
            codeSent(first, "verification-id-1")

            // Same number inside the cooldown window, so this attempt is rejected.
            onUi { it.onSendCodeClick() }
            assertThat(reportedErrors.single())
                .isInstanceOf(AuthException.PhoneVerificationCooldownException::class.java)

            // The rejected duplicate must not have torn down the healthy live attempt.
            autoVerified(first, credential)
            verify(mockAuth, times(1)).signInWithCredential(any())
        }
    }

    @Test
    fun `verification-required restarts the resend countdown`() {
        configuration = phoneConfiguration(timeout = 60L)
        val credential = mock(PhoneAuthCredential::class.java)
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(TaskCompletionSource<AuthResult>().task)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent()
            onUi { it.onPhoneNumberChange("5555550123") }
            onUi { it.onSendCodeClick() }
            codeSent(latestCallbacks(statics), "verification-id-1")
            assertThat(capturedState!!.resendTimer).isEqualTo(60)

            onUi { it.onChangeNumberClick() }
            assertThat(capturedState!!.resendTimer).isEqualTo(0)

            // A different number so the cooldown check accepts the second attempt.
            onUi { it.onPhoneNumberChange("5555550124") }
            onUi { it.onSendCodeClick() }
            codeSent(latestCallbacks(statics), "verification-id-2")

            // Only the PhoneNumberVerificationRequired branch restarts this countdown.
            assertThat(capturedState!!.resendTimer).isEqualTo(60)
        }
    }

    @Test
    fun `auto-verification signs in when no manual submit is in flight`() {
        val credential = mock(PhoneAuthCredential::class.java)
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(TaskCompletionSource<AuthResult>().task)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent()
            val callbacks = sendCode(statics)
            codeSent(callbacks, "verification-id-1")

            autoVerified(callbacks, credential)

            verify(mockAuth, times(1)).signInWithCredential(any())
        }
    }

    @Test
    fun `guard is released when submitting a code returns null without throwing`() {
        val credential = mock(PhoneAuthCredential::class.java)
        // RequiresMfa: submitVerificationCode returns null and never throws.
        val mfaTask = Tasks.forException<AuthResult>(multiFactorException())
        `when`(mockAuth.signInWithCredential(any())).thenReturn(mfaTask)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent()
            val callbacks = sendCode(statics)
            codeSent(callbacks, "verification-id-1")
            submitCode("123456")
            verify(mockAuth, times(1)).signInWithCredential(any())

            autoVerified(callbacks, credential)

            // A catch-only reset would leave the guard latched here and block this second sign-in.
            verify(mockAuth, times(2)).signInWithCredential(any())
        }
    }

    @Test
    fun `resend re-fires the verification-required state with the new verification id`() {
        val credential = mock(PhoneAuthCredential::class.java)
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(TaskCompletionSource<AuthResult>().task)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent()
            val first = sendCode(statics)
            codeSent(first, "verification-id-1")

            onUi { it.onResendCodeClick() }
            // The resend's Loading state reaches the screen, so the
            // PhoneNumberVerificationRequired that follows is never conflated with the previous
            // one - which is what restarts the resend countdown.
            assertThat(capturedState!!.isLoading).isTrue()

            codeSent(latestCallbacks(statics), "verification-id-2")
            submitCode("123456")

            // Only the PhoneNumberVerificationRequired branch writes the verification id.
            statics.verify { PhoneAuthProvider.getCredential("verification-id-2", "123456") }
        }
    }

    @Test
    fun `resend cancels the superseded verification attempt`() {
        val credential = mock(PhoneAuthCredential::class.java)
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(TaskCompletionSource<AuthResult>().task)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent()
            val first = sendCode(statics)
            codeSent(first, "verification-id-1")

            onUi { it.onResendCodeClick() }
            val second = latestCallbacks(statics)
            assertThat(second).isNotSameInstanceAs(first)

            // The superseded attempt auto-verifies late; its emissions must be dropped.
            autoVerified(first, credential)
            verify(mockAuth, never()).signInWithCredential(any())

            // The live attempt still drives the screen.
            codeSent(second, "verification-id-2")
            autoVerified(second, credential)
            verify(mockAuth, times(1)).signInWithCredential(any())
        }
    }

    @Test
    fun `a successful sign-in reports no error to the host`() {
        val credential = mock(PhoneAuthCredential::class.java)
        // Stubbed outside the `when` chain: mocking inside it trips Mockito's unfinished stubbing.
        val signedIn = Tasks.forResult(signedInResult())
        `when`(mockAuth.signInWithCredential(any())).thenReturn(signedIn)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent(withDialogs = true)
            val callbacks = sendCode(statics)
            codeSent(callbacks, "verification-id-1")
            submitCode("123456")
            settle()

            // Signing in tears down the still-open verification. That teardown is bookkeeping, so
            // it must not reach the host as a failure or pop a recovery dialog over the success.
            assertThat(reportedErrors).isEmpty()
            composeTestRule
                .onNodeWithText(configuration.stringProvider.authCancelledRecoveryMessage)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `a successful sign-in publishes no Error into authStateFlow`() {
        val credential = mock(PhoneAuthCredential::class.java)
        val signedIn = Tasks.forResult(signedInResult())
        `when`(mockAuth.signInWithCredential(any())).thenReturn(signedIn)

        val observed = mutableListOf<AuthState>()
        val collector = CoroutineScope(Dispatchers.Main.immediate).launch {
            authUI.authStateFlow().collect { observed += it }
        }

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent(withDialogs = true)
            val callbacks = sendCode(statics)
            codeSent(callbacks, "verification-id-1")
            submitCode("123456")
            settle()
        }
        collector.cancel()

        // authStateFlow is shared with the host, whose own error handling does not filter
        // cancellations - so tearing the verification down on success must publish no Error at all.
        assertThat(observed.filterIsInstance<AuthState.Success>()).isNotEmpty()
        assertThat(observed.filterIsInstance<AuthState.Error>()).isEmpty()
    }

    @Test
    fun `change-number reports no error to the host`() {
        val credential = mock(PhoneAuthCredential::class.java)
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(TaskCompletionSource<AuthResult>().task)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent(withDialogs = true)
            codeSent(sendCode(statics), "verification-id-1")

            onUi { it.onChangeNumberClick() }
            settle()

            assertThat(reportedErrors).isEmpty()
            composeTestRule
                .onNodeWithText(configuration.stringProvider.authCancelledRecoveryMessage)
                .assertDoesNotExist()
        }
    }

    @Test
    fun `change-number clears the loading state left behind by the cancelled attempt`() {
        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            setScreenContent()
            sendCode(statics)
            assertThat(capturedState!!.isLoading).isTrue()

            onUi { it.onChangeNumberClick() }
            settle()

            assertThat(capturedState!!.isLoading).isFalse()
        }
    }

    @Test
    fun `a failed sign-in reports exactly one error`() {
        val credential = mock(PhoneAuthCredential::class.java)
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(Tasks.forException(Exception("sign-in blew up")))

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent()
            val callbacks = sendCode(statics)
            codeSent(callbacks, "verification-id-1")

            autoVerified(callbacks, credential)
            settle()

            // The failure also tears down the verification, which must not append a second,
            // spurious cancellation error behind the real one.
            assertThat(reportedErrors.map { it.message }).containsExactly("sign-in blew up")
        }
    }

    /**
     * The same teardown, but while a reauthentication request is armed. The failure is folded into
     * [AuthState.Reauthentication.AttemptFailed], so a `when` that only tears down on
     * [AuthState.Error] leaves the verification open and the late auto-retrieval below starts a
     * second reauthentication the user never asked for. `resend cancels the superseded
     * verification attempt` above establishes that a cancelled attempt's emissions are dropped.
     */
    @Test
    fun `a failed reauthentication attempt cancels the in-flight verification`() {
        configuration = phoneConfiguration(timeout = 0L).copy(isReauthenticationMode = true)
        val user = phoneUser()
        `when`(mockAuth.currentUser).thenReturn(user)
        `when`(user.reauthenticate(any())).thenReturn(Tasks.forException(Exception("wrong code")))
        val credential = mock(PhoneAuthCredential::class.java)

        val observed = mutableListOf<AuthState>()
        val collector = CoroutineScope(Dispatchers.Main.immediate).launch {
            authUI.authStateFlow().collect { observed += it }
        }
        // What FirebaseAuthScreen does: register a drainer so ordinary states are folded into the
        // armed request, then arm it.
        authUI.addReauthenticationDrainer()
        authUI.updateAuthState(AuthState.Reauthentication.Required(user))

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent()
            val callbacks = sendCode(statics)
            codeSent(callbacks, "verification-id-1")

            submitCode("123456")
            settle()
            verify(user, times(1)).reauthenticate(any())
            // Precondition: the failure really did reach the screen as the reauthentication phase.
            assertThat(observed.filterIsInstance<AuthState.Reauthentication.AttemptFailed>())
                .isNotEmpty()

            autoVerified(callbacks, credential)
            settle()
            verify(user, times(1)).reauthenticate(any())
        }
        collector.cancel()
    }

    @Test
    fun `a cooldown-rejected send still reports its cooldown error`() {
        configuration = phoneConfiguration(timeout = 60L)
        val credential = mock(PhoneAuthCredential::class.java)
        `when`(mockAuth.signInWithCredential(any()))
            .thenReturn(TaskCompletionSource<AuthResult>().task)

        mockStatic(PhoneAuthProvider::class.java).use { statics ->
            stubGetCredential(statics, credential)
            setScreenContent(withDialogs = true)
            codeSent(sendCode(statics), "verification-id-1")

            // Same number inside the cooldown window, so this attempt is rejected.
            onUi { it.onSendCodeClick() }
            settle()

            // Not reporting cancellations must not also swallow the "wait N seconds" message.
            assertThat(reportedErrors.single())
                .isInstanceOf(AuthException.PhoneVerificationCooldownException::class.java)
            composeTestRule
                .onNodeWithText(configuration.stringProvider.errorDialogTitle)
                .assertExists()
        }
    }
}
