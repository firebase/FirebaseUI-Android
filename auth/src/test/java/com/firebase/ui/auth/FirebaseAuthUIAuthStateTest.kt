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

package com.firebase.ui.auth

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import android.content.Context
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.UserInfo
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [FirebaseAuthUI] auth state management functionality including
 * isSignedIn(), getCurrentUser(), and authStateFlow() methods.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FirebaseAuthUIAuthStateTest {

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockFirebaseUser: FirebaseUser

    @Mock
    private lateinit var mockAuthResult: AuthResult

    @Mock
    private lateinit var mockMultiFactorResolver: MultiFactorResolver

    private lateinit var defaultApp: FirebaseApp
    private lateinit var authUI: FirebaseAuthUI

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        // Clear the instance cache before each test
        FirebaseAuthUI.clearInstanceCache()

        // Clear any existing Firebase apps
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FirebaseApp.getApps(context).forEach { app ->
            app.delete()
        }

        // Initialize default FirebaseApp
        defaultApp = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )

        // Create FirebaseAuthUI instance with mock auth
        authUI = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
    }

    @After
    fun tearDown() {
        // Clean up after each test
        FirebaseAuthUI.clearInstanceCache()
        try {
            defaultApp.delete()
        } catch (_: Exception) {
            // Ignore if already deleted
        }
    }

    // =============================================================================================
    // isSignedIn() Tests
    // =============================================================================================

    @Test
    fun `isSignedIn() returns true when user is signed in`() {
        // Given a signed-in user
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)

        // When checking if signed in
        val isSignedIn = authUI.isSignedIn()

        // Then it should return true
        assertThat(isSignedIn).isTrue()
    }

    @Test
    fun `isSignedIn() returns false when user is not signed in`() {
        // Given no signed-in user
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        // When checking if signed in
        val isSignedIn = authUI.isSignedIn()

        // Then it should return false
        assertThat(isSignedIn).isFalse()
    }

    // =============================================================================================
    // getCurrentUser() Tests
    // =============================================================================================

    @Test
    fun `getCurrentUser() returns user when signed in`() {
        // Given a signed-in user
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)

        // When getting current user
        val currentUser = authUI.getCurrentUser()

        // Then it should return the user
        assertThat(currentUser).isEqualTo(mockFirebaseUser)
    }

    @Test
    fun `getCurrentUser() returns null when not signed in`() {
        // Given no signed-in user
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        // When getting current user
        val currentUser = authUI.getCurrentUser()

        // Then it should return null
        assertThat(currentUser).isNull()
    }

    // =============================================================================================
    // authStateFlow() Tests
    // =============================================================================================

    @Test
    fun `authStateFlow() emits Idle when no user is signed in`() = runBlocking {
        // Given no signed-in user
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        // When collecting auth state flow
        val state = authUI.authStateFlow().first()

        // Then it should emit Idle state
        assertThat(state).isEqualTo(AuthState.Idle)
    }

    @Test
    fun `authStateFlow() emits Success when user is signed in`() = runBlocking {
        // Given a signed-in user
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.isEmailVerified).thenReturn(true)
        `when`(mockFirebaseUser.email).thenReturn("test@example.com")
        `when`(mockFirebaseUser.uid).thenReturn("test-uid")
        `when`(mockFirebaseUser.providerData).thenReturn(emptyList())

        // When collecting auth state flow
        val state = authUI.authStateFlow().first()

        // Then it should emit Success state
        assertThat(state).isInstanceOf(AuthState.Success::class.java)
        val successState = state as AuthState.Success
        assertThat(successState.user).isEqualTo(mockFirebaseUser)
        assertThat(successState.isNewUser).isFalse()
    }

    @Test
    fun `authStateFlow() emits RequiresEmailVerification for unverified password users`() = runBlocking {
        // Given a signed-in user with unverified email using password authentication
        val mockProviderData = mock(UserInfo::class.java)
        `when`(mockProviderData.providerId).thenReturn("password")

        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.isEmailVerified).thenReturn(false)
        `when`(mockFirebaseUser.email).thenReturn("test@example.com")
        `when`(mockFirebaseUser.providerData).thenReturn(listOf(mockProviderData))

        // When collecting auth state flow
        val state = authUI.authStateFlow().first()

        // Then it should emit RequiresEmailVerification state
        assertThat(state).isInstanceOf(AuthState.RequiresEmailVerification::class.java)
        val verificationState = state as AuthState.RequiresEmailVerification
        assertThat(verificationState.user).isEqualTo(mockFirebaseUser)
        assertThat(verificationState.email).isEqualTo("test@example.com")
    }

    @Test
    fun `authStateFlow() responds to auth state changes`() = runBlocking {
        // Given initial state with no user
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        // Capture the auth state listener
        val listenerCaptor = ArgumentCaptor.forClass(AuthStateListener::class.java)

        // Start collecting the flow
        val states = mutableListOf<AuthState>()
        val job = launch {
            authUI.authStateFlow().take(3).toList(states)
        }

        // Wait for listener to be registered
        delay(100)
        verify(mockFirebaseAuth).addAuthStateListener(listenerCaptor.capture())

        // Simulate user sign-in
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.isEmailVerified).thenReturn(true)
        `when`(mockFirebaseUser.providerData).thenReturn(emptyList())
        listenerCaptor.value.onAuthStateChanged(mockFirebaseAuth)

        // Simulate user sign-out
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        listenerCaptor.value.onAuthStateChanged(mockFirebaseAuth)

        // Wait for all states to be collected
        job.join()

        // Verify the emitted states
        assertThat(states).hasSize(3)
        assertThat(states[0]).isEqualTo(AuthState.Idle) // Initial state
        assertThat(states[1]).isInstanceOf(AuthState.Success::class.java) // After sign-in
        assertThat(states[2]).isEqualTo(AuthState.Idle) // After sign-out
    }

    /**
     * A host calling raw `auth.signOut()` while a reauthentication is armed used to leave the
     * internal state at Reauthentication.Required: the combine keeps preferring it, so the reauth UI
     * stays up over a signed-out session and every provider fails with an untranslated "no user".
     */
    @Test
    fun `authStateFlow() clears an armed Reauthentication Required when the user signs out`() =
        runBlocking {
            `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
            `when`(mockFirebaseUser.isEmailVerified).thenReturn(true)
            `when`(mockFirebaseUser.providerData).thenReturn(emptyList())

            val listenerCaptor = ArgumentCaptor.forClass(AuthStateListener::class.java)
            val states = mutableListOf<AuthState>()
            // Collected open-endedly and cancelled below: a fixed `take` would hang rather than
            // fail when the sign-out emission never arrives.
            val job = launch { authUI.authStateFlow().toList(states) }

            delay(100)
            verify(mockFirebaseAuth).addAuthStateListener(listenerCaptor.capture())

            authUI.updateAuthState(
                AuthState.Reauthentication.Required(mockFirebaseUser, reason = "Confirm it is you")
            )
            delay(100)
            assertThat(states.last())
                .isInstanceOf(AuthState.Reauthentication.Required::class.java)

            // The host signs out behind the library's back, e.g. authUI.auth.signOut().
            `when`(mockFirebaseAuth.currentUser).thenReturn(null)
            listenerCaptor.value.onAuthStateChanged(mockFirebaseAuth)
            delay(200)
            job.cancel()

            assertThat(states.last()).isEqualTo(AuthState.Idle)
        }

    @Test
    fun `authStateFlow() removes listener when flow is cancelled`() = runBlocking {
        // Given auth state flow
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        // Capture the auth state listener
        val listenerCaptor = ArgumentCaptor.forClass(AuthStateListener::class.java)

        // Start collecting the flow
        val job = launch {
            authUI.authStateFlow().first()
        }

        // Wait for the job to complete
        job.join()

        // Verify that the listener was added and then removed
        verify(mockFirebaseAuth).addAuthStateListener(listenerCaptor.capture())
        verify(mockFirebaseAuth).removeAuthStateListener(listenerCaptor.value)
    }

    // =============================================================================================
    // reloadUser() Tests
    // =============================================================================================

    /** Completed reload/token tasks, so `reloadUser()` runs straight through. */
    private fun stubReloadTasks() {
        `when`(mockFirebaseUser.reload()).thenReturn(Tasks.forResult<Void>(null))
        `when`(mockFirebaseUser.getIdToken(true))
            .thenReturn(Tasks.forResult(mock(GetTokenResult::class.java)))
    }

    /**
     * Pins the flow to a verification state a phone-only user cannot satisfy. authStateFlow()
     * prefers any non-Idle internal state, so these tests fail unless reloadUser() republishes.
     */
    private fun pinToEmailVerification() {
        authUI.updateAuthState(
            AuthState.RequiresEmailVerification(user = mockFirebaseUser, email = "")
        )
    }

    @Test
    fun `reloadUser() republishes Success for phone-only users`() = runTest {
        // Given a phone-only user stranded on email verification
        val mockProviderData = mock(UserInfo::class.java)
        `when`(mockProviderData.providerId).thenReturn("phone")

        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.uid).thenReturn("test-uid")
        `when`(mockFirebaseUser.isEmailVerified).thenReturn(false)
        `when`(mockFirebaseUser.email).thenReturn(null)
        `when`(mockFirebaseUser.providerData).thenReturn(listOf(mockProviderData))
        stubReloadTasks()
        pinToEmailVerification()

        // When reloading the user
        authUI.reloadUser()

        // Then the stranding state is replaced with Success
        assertThat(authUI.authStateFlow().first()).isInstanceOf(AuthState.Success::class.java)
    }

    @Test
    fun `reloadUser() republishes Success for federated users with an unverified email`() = runTest {
        // Given a Google user whose email Firebase reports as unverified
        val mockProviderData = mock(UserInfo::class.java)
        `when`(mockProviderData.providerId).thenReturn("google.com")

        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.uid).thenReturn("test-uid")
        `when`(mockFirebaseUser.isEmailVerified).thenReturn(false)
        `when`(mockFirebaseUser.email).thenReturn("test@example.com")
        `when`(mockFirebaseUser.providerData).thenReturn(listOf(mockProviderData))
        stubReloadTasks()
        pinToEmailVerification()

        // When reloading the user
        authUI.reloadUser()

        // Then it is Success - there is no password credential to verify
        assertThat(authUI.authStateFlow().first()).isInstanceOf(AuthState.Success::class.java)
    }

    @Test
    fun `reloadUser() keeps RequiresEmailVerification for an unverified password user`() = runTest {
        // Given an unverified password user holding a phone credential too
        val mockPhoneProvider = mock(UserInfo::class.java)
        `when`(mockPhoneProvider.providerId).thenReturn("phone")
        val mockPasswordProvider = mock(UserInfo::class.java)
        `when`(mockPasswordProvider.providerId).thenReturn("password")

        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.uid).thenReturn("test-uid")
        `when`(mockFirebaseUser.isEmailVerified).thenReturn(false)
        `when`(mockFirebaseUser.email).thenReturn("test@example.com")
        `when`(mockFirebaseUser.providerData)
            .thenReturn(listOf(mockPhoneProvider, mockPasswordProvider))
        stubReloadTasks()
        pinToEmailVerification()

        // When reloading the user
        authUI.reloadUser()

        // Then verification is still required, and the blank email is replaced with the real one
        val state = authUI.authStateFlow().first()
        assertThat(state).isInstanceOf(AuthState.RequiresEmailVerification::class.java)
        assertThat((state as AuthState.RequiresEmailVerification).email)
            .isEqualTo("test@example.com")
    }

    @Test
    fun `reloadUser() publishes nothing when the user signs out mid-reload`() = runTest {
        // Given a user who signs out while their reload is in flight. Driving the sign-out from
        // reload() itself keeps the ordering deterministic instead of dispatcher-dependent.
        var signedIn = true
        `when`(mockFirebaseAuth.currentUser).thenAnswer { if (signedIn) mockFirebaseUser else null }
        `when`(mockFirebaseUser.reload()).thenAnswer {
            signedIn = false
            Tasks.forResult<Void>(null)
        }
        `when`(mockFirebaseUser.getIdToken(true))
            .thenReturn(Tasks.forResult(mock(GetTokenResult::class.java)))
        `when`(mockFirebaseUser.uid).thenReturn("test-uid")
        `when`(mockFirebaseUser.isEmailVerified).thenReturn(true)
        `when`(mockFirebaseUser.providerData).thenReturn(emptyList())

        // When the reload finishes after the user is gone
        authUI.reloadUser()

        // Then no Success is published for the departed user
        assertThat(authUI.authStateFlow().first()).isEqualTo(AuthState.Idle)
    }

    // =============================================================================================
    // Internal State Update Tests
    // =============================================================================================

    @Test
    fun `updateAuthState() updates internal state flow`() = runBlocking {
        // Given initial idle state
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        // Start collecting the flow to capture initial state
        val states = mutableListOf<AuthState>()
        val job = launch {
            authUI.authStateFlow().take(3).toList(states)
        }

        // Wait for initial state to be collected
        delay(100)

        // When updating auth state internally
        authUI.updateAuthState(AuthState.Loading("Signing in..."))

        // Wait for state update to propagate
        delay(100)

        // Update state again
        authUI.updateAuthState(AuthState.Cancelled)

        job.join()

        // Verify the emitted states
        assertThat(states).hasSize(3)
        assertThat(states[0]).isEqualTo(AuthState.Idle) // Initial state
        assertThat(states[1]).isInstanceOf(AuthState.Loading::class.java) // After first update
        assertThat(states[2]).isEqualTo(AuthState.Cancelled) // After second update
    }

    // =============================================================================================
    // Stale one-off AuthState regression tests
    // =============================================================================================

    @Test
    fun `Error does not leak to a fresh collector after being consumed`() = runBlocking {
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        authUI.updateAuthState(AuthState.Error(Exception("boom")))
        authUI.updateAuthState(AuthState.Idle)

        // A brand-new collector (simulating a freshly created Activity) must see Idle.
        assertThat(authUI.authStateFlow().first()).isEqualTo(AuthState.Idle)
    }

    @Test
    fun `Cancelled does not leak to a fresh collector after being consumed`() = runBlocking {
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        authUI.updateAuthState(AuthState.Cancelled)
        authUI.updateAuthState(AuthState.Idle)

        assertThat(authUI.authStateFlow().first()).isEqualTo(AuthState.Idle)
    }

    @Test
    fun `SMSAutoVerified does not leak to a fresh collector after being consumed`() = runBlocking {
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        val credential = mock(com.google.firebase.auth.PhoneAuthCredential::class.java)

        authUI.updateAuthState(AuthState.SMSAutoVerified(credential))
        authUI.updateAuthState(AuthState.Idle)

        assertThat(authUI.authStateFlow().first()).isEqualTo(AuthState.Idle)
    }

    @Test
    fun `Error left uncleared still leaks to a fresh collector (pins down the bug being fixed)`() =
        runBlocking {
            `when`(mockFirebaseAuth.currentUser).thenReturn(null)

            // No consuming reset here — documents the pre-fix leaking behavior.
            authUI.updateAuthState(AuthState.Error(Exception("boom")))

            assertThat(authUI.authStateFlow().first()).isInstanceOf(AuthState.Error::class.java)
        }

    // =============================================================================================
    // AuthState Class Tests
    // =============================================================================================

    @Test
    fun `AuthState Success contains correct properties`() {
        // Create Success state
        val state = AuthState.Success(
            result = mockAuthResult,
            user = mockFirebaseUser,
            isNewUser = true
        )

        // Verify properties
        assertThat(state.result).isEqualTo(mockAuthResult)
        assertThat(state.user).isEqualTo(mockFirebaseUser)
        assertThat(state.isNewUser).isTrue()
    }

    @Test
    fun `AuthState Error contains exception and recoverability`() {
        // Create Error state
        val exception = Exception("Test error")
        val state = AuthState.Error(
            exception = exception,
            isRecoverable = false
        )

        // Verify properties
        assertThat(state.exception).isEqualTo(exception)
        assertThat(state.isRecoverable).isFalse()
    }

    @Test
    fun `AuthState RequiresMfa contains resolver`() {
        // Create RequiresMfa state
        val state = AuthState.RequiresMfa(
            resolver = mockMultiFactorResolver,
            hint = "Use SMS"
        )

        // Verify properties
        assertThat(state.resolver).isEqualTo(mockMultiFactorResolver)
        assertThat(state.hint).isEqualTo("Use SMS")
    }

    @Test
    fun `AuthState Loading can contain message`() {
        // Create Loading state with message
        val state = AuthState.Loading("Processing...")

        // Verify properties
        assertThat(state.message).isEqualTo("Processing...")
    }

    @Test
    fun `AuthState RequiresProfileCompletion contains missing fields`() {
        // Create RequiresProfileCompletion state
        val missingFields = listOf("displayName", "photoUrl")
        val state = AuthState.RequiresProfileCompletion(
            user = mockFirebaseUser,
            missingFields = missingFields
        )

        // Verify properties
        assertThat(state.user).isEqualTo(mockFirebaseUser)
        assertThat(state.missingFields).containsExactly("displayName", "photoUrl")
    }

    // =============================================================================================
    // delete() Reauthentication.Required state Tests
    // =============================================================================================

    @Test
    fun `delete() emits Reauthentication Required state when recent login required`() = runTest {
        val mockUser = mock(FirebaseUser::class.java)
        val tcs = TaskCompletionSource<Void>()
        tcs.setException(
            FirebaseAuthRecentLoginRequiredException(
                "ERROR_REQUIRES_RECENT_LOGIN", "Recent login required"
            )
        )
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.delete()).thenReturn(tcs.task)

        val context = ApplicationProvider.getApplicationContext<Context>()

        val call = launch { authUI.delete(context) }
        runCurrent()

        assertThat(authUI.authStateFlow().first())
            .isInstanceOf(AuthState.Reauthentication.Required::class.java)
        val state = authUI.authStateFlow().first() as AuthState.Reauthentication.Required
        assertThat(state.user).isEqualTo(mockUser)

        state.request.resolve(false)
        call.join()
    }

    @Test
    fun `delete() arms a resumable request rather than throwing`() = runTest {
        val mockUser = mock(FirebaseUser::class.java)
        val tcs = TaskCompletionSource<Void>()
        tcs.setException(
            FirebaseAuthRecentLoginRequiredException(
                "ERROR_REQUIRES_RECENT_LOGIN", "Recent login required"
            )
        )
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.delete()).thenReturn(tcs.task)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val call = launch { authUI.delete(context) }
        runCurrent()

        val state = authUI.authStateFlow().first() as AuthState.Reauthentication.Required
        assertThat(state.request.hasPendingOperation).isTrue()
        assertThat(state.request.isResumable).isTrue()
        // One path for this condition now: it arms and waits, where it used to arm *and* throw an
        // InvalidCredentialsException the caller had to catch and ignore.
        assertThat(call.isActive).isTrue()

        state.request.resolve(false)
        call.join()
    }

    /**
     * `withReauth`/`delete` are public and can arm a request with no [FirebaseAuthScreen]
     * composed. Folding is the composed screen's, so the setter stays a plain setter and the app's
     * own collector keeps seeing ordinary states.
     */
    @Test
    fun `a Success reaches collectors while an undrainable request is armed`() = runTest {
        `when`(mockFirebaseUser.uid).thenReturn("uid-reauth")
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        authUI.updateAuthState(AuthState.Reauthentication.Required(mockFirebaseUser))
        assertThat(authUI.authStateFlow().first())
            .isInstanceOf(AuthState.Reauthentication.Required::class.java)

        authUI.updateAuthState(AuthState.Success(result = null, user = mockFirebaseUser))

        val observed = authUI.authStateFlow().first()
        assertThat(observed).isInstanceOf(AuthState.Success::class.java)
        assertThat(observed).isNotInstanceOf(AuthState.Reauthentication::class.java)
    }

    /** The same for Idle: an undrainable arming is replaced, not made permanent. */
    @Test
    fun `an Idle write clears an undrainable armed request`() = runTest {
        `when`(mockFirebaseUser.uid).thenReturn("uid-reauth")
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        authUI.updateAuthState(AuthState.Reauthentication.Required(mockFirebaseUser))

        authUI.updateAuthState(AuthState.Idle)

        assertThat(authUI.authStateFlow().first())
            .isNotInstanceOf(AuthState.Reauthentication::class.java)
    }

    // =============================================================================================
    // withReauth() Tests
    // =============================================================================================

    @Test
    fun `withReauth() executes operation normally when no reauth needed`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        var callCount = 0

        authUI.withReauth(context) { callCount++ }

        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `withReauth() arms a resumable request and suspends instead of throwing`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)

        val call = launch {
            authUI.withReauth(context, reason = "Verify identity to change email") {
                throw FirebaseAuthRecentLoginRequiredException(
                    "ERROR_REQUIRES_RECENT_LOGIN", "Recent login required"
                )
            }
        }
        runCurrent()

        val state = authUI.authStateFlow().first() as AuthState.Reauthentication.Required
        assertThat(state.user).isEqualTo(mockFirebaseUser)
        assertThat(state.reason).isEqualTo("Verify identity to change email")
        assertThat(state.request.hasPendingOperation).isTrue()
        // Parked on its own half of the request, so the retry will run here rather than anywhere
        // the library would have to hold on to it.
        assertThat(call.isActive).isTrue()

        state.request.resolve(false)
        call.join()
    }

    @Test
    fun `withReauth() re-runs the operation when its request resolves to a retry`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        var callCount = 0

        val call = launch {
            authUI.withReauth(context) {
                callCount++
                if (callCount == 1) throw FirebaseAuthRecentLoginRequiredException(
                    "ERROR_REQUIRES_RECENT_LOGIN", "Recent login required"
                )
            }
        }
        runCurrent()
        val state = authUI.authStateFlow().first() as AuthState.Reauthentication.Required
        assertThat(callCount).isEqualTo(1)

        state.request.resolve(true)
        call.join()

        assertThat(callCount).isEqualTo(2)
    }

    @Test
    fun `withReauth() leaves the operation alone when its request resolves without a retry`() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
            var callCount = 0

            val call = launch {
                authUI.withReauth(context) {
                    callCount++
                    if (callCount == 1) throw FirebaseAuthRecentLoginRequiredException(
                        "ERROR_REQUIRES_RECENT_LOGIN", "Recent login required"
                    )
                }
            }
            runCurrent()
            val state = authUI.authStateFlow().first() as AuthState.Reauthentication.Required

            state.request.resolve(false)
            call.join()

            assertThat(callCount).isEqualTo(1)
        }

    /**
     * The caller's scope died while the sheet was up. Nothing can resume the operation, and the
     * request says so rather than presenting as one that can still complete.
     */
    @Test
    fun `a cancelled caller leaves its request unresumable`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        var callCount = 0

        val call = launch {
            authUI.withReauth(context) {
                callCount++
                if (callCount == 1) throw FirebaseAuthRecentLoginRequiredException(
                    "ERROR_REQUIRES_RECENT_LOGIN", "Recent login required"
                )
            }
        }
        runCurrent()
        val state = authUI.authStateFlow().first() as AuthState.Reauthentication.Required
        assertThat(state.request.isResumable).isTrue()

        call.cancel()
        call.join()

        assertThat(state.request.isResumable).isFalse()
        // Resolving a dead request is a no-op, not a crash, and runs nothing.
        state.request.resolve(true)
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `withReauth() propagates non-reauth exceptions`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        val cause = RuntimeException("Network error")
        var thrown: Exception? = null

        try {
            authUI.withReauth(context) { throw cause }
        } catch (e: Exception) {
            thrown = e
        }

        assertThat(thrown).isEqualTo(cause)
    }

    @Test
    fun `delete() retries the deletion when its request resolves to a retry`() = runTest {
        val mockUser = mock(FirebaseUser::class.java)

        val failTcs = TaskCompletionSource<Void>()
        failTcs.setException(
            FirebaseAuthRecentLoginRequiredException(
                "ERROR_REQUIRES_RECENT_LOGIN", "Recent login required"
            )
        )
        val successTcs = TaskCompletionSource<Void>()
        successTcs.setResult(null)

        `when`(mockFirebaseAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.delete())
            .thenReturn(failTcs.task)
            .thenReturn(successTcs.task)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val call = launch { authUI.delete(context) }
        runCurrent()

        val state = authUI.authStateFlow().first() as AuthState.Reauthentication.Required
        state.request.resolve(true)
        call.join()

        verify(mockUser, times(2)).delete()
    }
}
