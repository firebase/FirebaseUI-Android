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

package com.firebase.ui.auth.configuration.auth_provider

import com.firebase.ui.auth.flowScope
import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The ordered sequence of states each sign-in path publishes.
 *
 * Every other provider test asserts only the state it ends on, so an emission that is dropped,
 * duplicated or moved leaves all of them green. That is exactly the failure a mechanical sweep over
 * the ~87 `updateAuthState` call sites in provider code can introduce, so these record the order
 * itself: a golden net to refactor the provider receiver against, not a specification of anything
 * new.
 *
 * The states are read through `authStateFlow()`, so what they record is what a *consumer* observes.
 * That matters: the flow underneath is a `MutableStateFlow` and therefore conflating, so each task
 * below is completed only after the collector has been let run. A test that resolved its task up
 * front would record `[Idle, Success]` and prove nothing about the `Loading` in between.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SignInStateSequenceTest {

    private lateinit var mockFirebaseAuth: FirebaseAuth
    private lateinit var firebaseApp: FirebaseApp
    private lateinit var applicationContext: Context

    @Before
    fun setUp() {
        mockFirebaseAuth = mock(FirebaseAuth::class.java)
        FirebaseAuthUI.clearInstanceCache()
        applicationContext = ApplicationProvider.getApplicationContext()
        FirebaseApp.getApps(applicationContext).forEach { it.delete() }
        firebaseApp = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        runCatching { firebaseApp.delete() }
    }

    // =============================================================================================
    // Harness
    // =============================================================================================

    /** Records every state in order from now until the test ends. */
    private fun TestScope.record(instance: FirebaseAuthUI): List<String> {
        val recorded = mutableListOf<AuthState>()
        backgroundScope.launch { instance.authStateFlow().collect { recorded += it } }
        runCurrent()
        return object : AbstractList<String>() {
            override val size: Int get() = recorded.size
            override fun get(index: Int): String = recorded[index].label()
        }
    }

    /**
     * Lets the recorder catch up to [count] states, or fails.
     *
     * `advanceUntilIdle` is not enough: the recorder collects its own `authStateFlow()`, and an
     * idle scheduler does not mean that collection has been resumed. Yielding hands it turns until
     * it has.
     */
    private suspend fun awaitStates(states: List<String>, count: Int) {
        repeat(1_000) {
            if (states.size >= count) return
            yield()
        }
        throw AssertionError("Recorded only $states, expected $count states")
    }

    /** The state's identity, without the payload: what changed and in what order, not what it held. */
    private fun AuthState.label(): String = when (this) {
        is AuthState.Idle -> "Idle"
        is AuthState.Loading -> "Loading"
        is AuthState.Success -> "Success"
        is AuthState.Error -> "Error"
        is AuthState.Cancelled -> "Cancelled"
        is AuthState.Aborted -> "Aborted"
        is AuthState.RequiresEmailVerification -> "RequiresEmailVerification"
        is AuthState.RequiresProfileCompletion -> "RequiresProfileCompletion"
        is AuthState.RequiresMfa -> "RequiresMfa"
        is AuthState.PasswordResetLinkSent -> "PasswordResetLinkSent"
        is AuthState.EmailSignInLinkSent -> "EmailSignInLinkSent"
        is AuthState.PhoneNumberVerificationRequired -> "PhoneNumberVerificationRequired"
        is AuthState.SMSAutoVerified -> "SMSAutoVerified"
        is AuthState.Reauthentication -> "Reauthentication.${this::class.simpleName}"
        else -> this::class.simpleName ?: "?"
    }

    private fun configOf(vararg providers: AuthProvider): AuthUIConfiguration =
        authUIConfiguration {
            context = applicationContext
            providers { providers.forEach { provider(it) } }
        }

    private fun signedInResult(): Pair<AuthResult, FirebaseUser> {
        val user = mock(FirebaseUser::class.java)
        `when`(user.uid).thenReturn("uid-1")
        `when`(user.isEmailVerified).thenReturn(true)
        `when`(user.providerData).thenReturn(emptyList())
        val result = mock(AuthResult::class.java)
        `when`(result.user).thenReturn(user)
        return result to user
    }

    private fun emailProvider() = AuthProvider.Email(
        emailLinkActionCodeSettings = null,
        passwordValidationRules = emptyList(),
    )

    // =============================================================================================
    // Anonymous
    // =============================================================================================

    @Test
    fun `anonymous sign-in publishes Loading then Success`() = runTest {
        val (result, user) = signedInResult()
        `when`(user.isAnonymous).thenReturn(true)
        val task = TaskCompletionSource<AuthResult>()
        `when`(mockFirebaseAuth.signInAnonymously()).thenReturn(task.task)
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val states = record(instance)
        val config = configOf(AuthProvider.Anonymous, emailProvider())

        val job = launch { runCatching { instance.flowScope(config).signInAnonymously() } }
        runCurrent()
        task.setResult(result)
        runCurrent()
        job.join()
        awaitStates(states, 3)

        assertThat(states).containsExactly("Idle", "Loading", "Success").inOrder()
    }

    @Test
    fun `a failed anonymous sign-in publishes Loading then Error`() = runTest {
        val task = TaskCompletionSource<AuthResult>()
        `when`(mockFirebaseAuth.signInAnonymously()).thenReturn(task.task)
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val states = record(instance)
        val config = configOf(AuthProvider.Anonymous, emailProvider())

        val job = launch { runCatching { instance.flowScope(config).signInAnonymously() } }
        runCurrent()
        task.setException(FirebaseNetworkException("Network error"))
        runCurrent()
        job.join()
        awaitStates(states, 3)

        assertThat(states).containsExactly("Idle", "Loading", "Error").inOrder()
    }

    // =============================================================================================
    // Email
    // =============================================================================================

    @Test
    fun `email password sign-in publishes Loading then Success`() = runTest {
        val (result, _) = signedInResult()
        val task = TaskCompletionSource<AuthResult>()
        `when`(mockFirebaseAuth.signInWithEmailAndPassword("a@b.com", "pw1"))
            .thenReturn(task.task)
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val states = record(instance)
        val config = configOf(emailProvider())

        val job = launch {
            runCatching {
                instance.flowScope(config).signInWithEmailAndPassword(
                    context = applicationContext,
                    email = "a@b.com",
                    password = "pw1",
                    // The credential-manager save is unavailable under Robolectric and throws
                    // past this path's own handlers, which is its own bug and not this one's.
                    // Skipped here so the sequence recorded is the state machine's.
                    skipCredentialSave = true)
            }
        }
        runCurrent()
        task.setResult(result)
        runCurrent()
        job.join()
        awaitStates(states, 3)

        assertThat(states).containsExactly("Idle", "Loading", "Success").inOrder()
    }

    @Test
    fun `a rejected email password sign-in publishes Loading then Error`() = runTest {
        val task = TaskCompletionSource<AuthResult>()
        `when`(mockFirebaseAuth.signInWithEmailAndPassword("a@b.com", "wrong"))
            .thenReturn(task.task)
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val states = record(instance)
        val config = configOf(emailProvider())

        val job = launch {
            runCatching {
                instance.flowScope(config).signInWithEmailAndPassword(
                    context = applicationContext,
                    email = "a@b.com",
                    password = "wrong")
            }
        }
        runCurrent()
        task.setException(FirebaseNetworkException("Network error"))
        runCurrent()
        job.join()
        awaitStates(states, 3)

        assertThat(states).containsExactly("Idle", "Loading", "Error").inOrder()
    }

    // =============================================================================================
    // OAuth
    // =============================================================================================

    @Test
    fun `oauth sign-in publishes Loading then Success`() = runTest {
        val (result, _) = signedInResult()
        `when`(result.credential).thenReturn(mock(OAuthCredential::class.java))
        val activity = mock(Activity::class.java)
        val task = TaskCompletionSource<AuthResult>()
        `when`(mockFirebaseAuth.pendingAuthResult).thenReturn(null)
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        `when`(
            mockFirebaseAuth.startActivityForSignInWithProvider(
                any<Activity>(),
                any<OAuthProvider>(),
            )
        ).thenReturn(task.task)
        val github = AuthProvider.Github(customParameters = emptyMap())
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val states = record(instance)
        val config = configOf(github)

        val job = launch {
            runCatching {
                instance.flowScope(config).signInWithProvider(
                    applicationContext,
                    activity = activity,
                    provider = github)
            }
        }
        runCurrent()
        task.setResult(result)
        runCurrent()
        job.join()
        awaitStates(states, 3)

        assertThat(states).containsExactly("Idle", "Loading", "Success").inOrder()
    }

    // =============================================================================================
    // Phone
    // =============================================================================================

    @Test
    fun `phone verification publishes Loading then the code prompt`() = runTest {
        val phone = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = null,
        )
        val verifier = mock(AuthProvider.Phone.Verifier::class.java)
        `when`(
            verifier.verifyPhoneNumber(
                auth = any(),
                activity = anyOrNull(),
                phoneNumber = any(),
                timeout = eq(60L),
                forceResendingToken = anyOrNull(),
                multiFactorSession = anyOrNull(),
                isInstantVerificationEnabled = eq(true),
            )
        ).thenReturn(
            flowOf(
                AuthProvider.Phone.VerifyPhoneNumberResult.NeedsManualVerification(
                    verificationId = "verification-id-1",
                    token = mock(PhoneAuthProvider.ForceResendingToken::class.java),
                )
            )
        )
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val states = record(instance)
        val config = configOf(phone)

        instance.flowScope(config).verifyPhoneNumber(
            provider = phone,
            activity = null,
            phoneNumber = "+1234567890",
            verifier = verifier)
        runCurrent()

        // The verifier's flow is cold and already has its emission, so Loading and the prompt land
        // in the same turn: conflation means a consumer sees only the prompt.
        assertThat(states).containsExactly("Idle", "PhoneNumberVerificationRequired").inOrder()
    }

    @Test
    fun `phone credential sign-in publishes Loading then Success`() = runTest {
        val (result, _) = signedInResult()
        val credential = mock(PhoneAuthCredential::class.java)
        val task = TaskCompletionSource<AuthResult>()
        `when`(mockFirebaseAuth.signInWithCredential(credential)).thenReturn(task.task)
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        val phone = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = null,
        )
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val states = record(instance)
        val config = configOf(phone)

        val job = launch {
            runCatching {
                instance.flowScope(config).signInWithPhoneAuthCredential(
                    context = applicationContext,
                    credential = credential)
            }
        }
        runCurrent()
        task.setResult(result)
        runCurrent()
        job.join()
        awaitStates(states, 3)

        assertThat(states).containsExactly("Idle", "Loading", "Success").inOrder()
    }
}
