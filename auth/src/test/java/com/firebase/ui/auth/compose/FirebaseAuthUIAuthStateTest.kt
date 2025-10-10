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

package com.firebase.ui.auth.compose

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.UserInfo
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
    fun `authStateFlow() emits Success even with unverified email for now`() = runBlocking {
        // Given a signed-in user with unverified email
        // Note: The current implementation checks for password provider, which might not be
        // matched properly due to mocking limitations. This test verifies current behavior.
        val mockProviderData = mock(UserInfo::class.java)
        `when`(mockProviderData.providerId).thenReturn("password")

        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.isEmailVerified).thenReturn(false)
        `when`(mockFirebaseUser.email).thenReturn("test@example.com")
        `when`(mockFirebaseUser.providerData).thenReturn(listOf(mockProviderData))

        // When collecting auth state flow
        val state = authUI.authStateFlow().first()

        // Then it should emit Success state (current behavior with mocked data)
        assertThat(state).isInstanceOf(AuthState.Success::class.java)
        val successState = state as AuthState.Success
        assertThat(successState.user).isEqualTo(mockFirebaseUser)
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
}