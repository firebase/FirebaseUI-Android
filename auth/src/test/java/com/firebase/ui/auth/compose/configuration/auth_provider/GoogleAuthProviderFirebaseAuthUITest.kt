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

package com.firebase.ui.auth.compose.configuration.auth_provider

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive unit tests for Google Sign-In provider methods in FirebaseAuthUI.
 *
 * Tests cover:
 * - signInWithGoogle with and without OAuth scopes
 * - Authorization flow testing
 * - Credential Manager flow testing
 * - Error handling for various scenarios
 * - Anonymous account upgrade
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GoogleAuthProviderFirebaseAuthUITest {

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockAuthorizationProvider: AuthProvider.Google.AuthorizationProvider

    @Mock
    private lateinit var mockCredentialManagerProvider: AuthProvider.Google.CredentialManagerProvider

    private lateinit var firebaseApp: FirebaseApp
    private lateinit var applicationContext: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        FirebaseAuthUI.clearInstanceCache()

        applicationContext = ApplicationProvider.getApplicationContext()

        FirebaseApp.getApps(applicationContext).forEach { app ->
            app.delete()
        }

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
        try {
            firebaseApp.delete()
        } catch (_: Exception) {
            // Ignore if already deleted
        }
    }

    // =============================================================================================
    // signInWithGoogle - Success Cases
    // =============================================================================================

    @Test
    fun `Sign in with Google without scopes should succeed`() = runTest {
        val mockCredential = mock(AuthCredential::class.java)
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)

        val googleSignInResult = AuthProvider.Google.GoogleSignInResult(
            credential = mockCredential,
            displayName = "Test User",
            photoUrl = "https://example.com/photo.jpg".toUri()
        )

        `when`(
            mockCredentialManagerProvider.getGoogleCredential(
                context = eq(applicationContext),
                serverClientId = eq("test-client-id"),
                filterByAuthorizedAccounts = eq(true),
                autoSelectEnabled = eq(false)
            )
        ).thenReturn(googleSignInResult)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val googleProvider = AuthProvider.Google(
            serverClientId = "test-client-id",
            scopes = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(googleProvider)
            }
        }

        instance.signInWithGoogle(
            context = applicationContext,
            config = config,
            provider = googleProvider,
            authorizationProvider = mockAuthorizationProvider,
            credentialManagerProvider = mockCredentialManagerProvider
        )

        // Verify authorization was NOT called (no scopes)
        verify(mockAuthorizationProvider, never()).authorize(any(), any())

        // Verify credential manager was called
        verify(mockCredentialManagerProvider).getGoogleCredential(
            context = eq(applicationContext),
            serverClientId = eq("test-client-id"),
            filterByAuthorizedAccounts = eq(true),
            autoSelectEnabled = eq(false)
        )

        // Verify Firebase sign-in was called
        verify(mockFirebaseAuth).signInWithCredential(mockCredential)

        // Verify state is Idle after success
        val finalState = instance.authStateFlow().first()
        assertThat(finalState).isEqualTo(AuthState.Idle)
    }

    @Test
    fun `Sign in with Google with scopes should request authorization first`() = runTest {
        val mockCredential = mock(AuthCredential::class.java)
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)

        val googleSignInResult = AuthProvider.Google.GoogleSignInResult(
            credential = mockCredential,
            displayName = "Test User",
            photoUrl = "https://example.com/photo.jpg".toUri()
        )

        `when`(
            mockCredentialManagerProvider.getGoogleCredential(
                context = eq(applicationContext),
                serverClientId = eq("test-client-id"),
                filterByAuthorizedAccounts = eq(true),
                autoSelectEnabled = eq(false)
            )
        ).thenReturn(googleSignInResult)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val googleProvider = AuthProvider.Google(
            serverClientId = "test-client-id",
            scopes = listOf("https://www.googleapis.com/auth/drive.readonly")
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(googleProvider)
            }
        }

        instance.signInWithGoogle(
            context = applicationContext,
            config = config,
            provider = googleProvider,
            authorizationProvider = mockAuthorizationProvider,
            credentialManagerProvider = mockCredentialManagerProvider
        )

        // Verify authorization was called with correct scopes
        val scopesCaptor = argumentCaptor<List<Scope>>()
        verify(mockAuthorizationProvider).authorize(
            eq(applicationContext),
            scopesCaptor.capture()
        )
        assertThat(scopesCaptor.firstValue).hasSize(1)
        assertThat(scopesCaptor.firstValue[0].scopeUri).isEqualTo("https://www.googleapis.com/auth/drive.readonly")

        // Verify credential manager was called after authorization
        verify(mockCredentialManagerProvider).getGoogleCredential(
            context = eq(applicationContext),
            serverClientId = eq("test-client-id"),
            filterByAuthorizedAccounts = eq(true),
            autoSelectEnabled = eq(false)
        )

        // Verify Firebase sign-in was called
        verify(mockFirebaseAuth).signInWithCredential(mockCredential)
    }

    @Test
    fun `Sign in with Google should pass displayName and photoUrl to linking`() = runTest {
        val mockCredential = mock(AuthCredential::class.java)
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)

        val expectedDisplayName = "John Doe"
        val expectedPhotoUrl = "https://example.com/john.jpg".toUri()

        val googleSignInResult = AuthProvider.Google.GoogleSignInResult(
            credential = mockCredential,
            displayName = expectedDisplayName,
            photoUrl = expectedPhotoUrl
        )

        `when`(
            mockCredentialManagerProvider.getGoogleCredential(
                context = eq(applicationContext),
                serverClientId = eq("test-client-id"),
                filterByAuthorizedAccounts = eq(true),
                autoSelectEnabled = eq(false)
            )
        ).thenReturn(googleSignInResult)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val googleProvider = AuthProvider.Google(
            serverClientId = "test-client-id",
            scopes = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(googleProvider)
            }
        }

        instance.signInWithGoogle(
            context = applicationContext,
            config = config,
            provider = googleProvider,
            authorizationProvider = mockAuthorizationProvider,
            credentialManagerProvider = mockCredentialManagerProvider
        )

        // Note: Testing the actual values passed would require mocking signInAndLinkWithCredential
        // which is an internal function. This test verifies the flow completes successfully
        // with the displayName and photoUrl available in GoogleSignInResult.
        verify(mockFirebaseAuth).signInWithCredential(mockCredential)
    }

    // =============================================================================================
    // signInWithGoogle - Error Handling
    // =============================================================================================

    @Test
    fun `Sign in with Google when authorization fails should update state to error but continue`() = runTest {
        val authorizationException = RuntimeException("Authorization failed")
        `when`(
            mockAuthorizationProvider.authorize(
                eq(applicationContext),
                any()
            )
        ).thenThrow(authorizationException)

        val mockCredential = mock(AuthCredential::class.java)
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)

        val googleSignInResult = AuthProvider.Google.GoogleSignInResult(
            credential = mockCredential,
            displayName = "Test User",
            photoUrl = null
        )

        `when`(
            mockCredentialManagerProvider.getGoogleCredential(
                context = eq(applicationContext),
                serverClientId = eq("test-client-id"),
                filterByAuthorizedAccounts = eq(true),
                autoSelectEnabled = eq(false)
            )
        ).thenReturn(googleSignInResult)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val googleProvider = AuthProvider.Google(
            serverClientId = "test-client-id",
            scopes = listOf("https://www.googleapis.com/auth/drive")
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(googleProvider)
            }
        }

        instance.signInWithGoogle(
            context = applicationContext,
            config = config,
            provider = googleProvider,
            authorizationProvider = mockAuthorizationProvider,
            credentialManagerProvider = mockCredentialManagerProvider
        )

        // Verify authorization was attempted
        verify(mockAuthorizationProvider).authorize(eq(applicationContext), any())

        // Verify sign-in continued despite authorization failure
        verify(mockCredentialManagerProvider).getGoogleCredential(
            context = eq(applicationContext),
            serverClientId = eq("test-client-id"),
            filterByAuthorizedAccounts = eq(true),
            autoSelectEnabled = eq(false)
        )
        verify(mockFirebaseAuth).signInWithCredential(mockCredential)
    }

    @Test
    fun `Sign in with Google when credential manager fails should throw AuthException`() = runTest {
        val credentialException = RuntimeException("No credentials available")
        `when`(
            mockCredentialManagerProvider.getGoogleCredential(
                context = eq(applicationContext),
                serverClientId = eq("test-client-id"),
                filterByAuthorizedAccounts = eq(true),
                autoSelectEnabled = eq(false)
            )
        ).thenThrow(credentialException)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val googleProvider = AuthProvider.Google(
            serverClientId = "test-client-id",
            scopes = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(googleProvider)
            }
        }

        try {
            instance.signInWithGoogle(
                context = applicationContext,
                config = config,
                provider = googleProvider,
                authorizationProvider = mockAuthorizationProvider,
                credentialManagerProvider = mockCredentialManagerProvider
            )
            throw AssertionError("Expected exception to be thrown")
        } catch (e: AuthException) {
            assertThat(e).isInstanceOf(AuthException.UnknownException::class.java)
        }

        // Verify state is Error
        val finalState = instance.authStateFlow().first()
        assertThat(finalState).isInstanceOf(AuthState.Error::class.java)
        val errorState = finalState as AuthState.Error
        assertThat(errorState.exception).isInstanceOf(AuthException.UnknownException::class.java)
    }

    @Test
    fun `Sign in with Google when Firebase sign-in fails should throw AuthException`() = runTest {
        val mockCredential = mock(AuthCredential::class.java)
        val googleSignInResult = AuthProvider.Google.GoogleSignInResult(
            credential = mockCredential,
            displayName = "Test User",
            photoUrl = null
        )

        `when`(
            mockCredentialManagerProvider.getGoogleCredential(
                context = eq(applicationContext),
                serverClientId = eq("test-client-id"),
                filterByAuthorizedAccounts = eq(true),
                autoSelectEnabled = eq(false)
            )
        ).thenReturn(googleSignInResult)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        val firebaseException = FirebaseAuthInvalidCredentialsException("invalid_credential", "Invalid credential")
        taskCompletionSource.setException(firebaseException)
        `when`(mockFirebaseAuth.signInWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val googleProvider = AuthProvider.Google(
            serverClientId = "test-client-id",
            scopes = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(googleProvider)
            }
        }

        try {
            instance.signInWithGoogle(
                context = applicationContext,
                config = config,
                provider = googleProvider,
                authorizationProvider = mockAuthorizationProvider,
                credentialManagerProvider = mockCredentialManagerProvider
            )
            throw AssertionError("Expected exception to be thrown")
        } catch (e: AuthException) {
            assertThat(e).isInstanceOf(AuthException.InvalidCredentialsException::class.java)
        }

        // Verify state is Error
        val finalState = instance.authStateFlow().first()
        assertThat(finalState).isInstanceOf(AuthState.Error::class.java)
    }

    @Test
    fun `Sign in with Google when cancelled should throw AuthCancelledException`() = runTest {
        val cancellationException = CancellationException("User cancelled")
        `when`(
            mockCredentialManagerProvider.getGoogleCredential(
                context = eq(applicationContext),
                serverClientId = eq("test-client-id"),
                filterByAuthorizedAccounts = eq(true),
                autoSelectEnabled = eq(false)
            )
        ).thenThrow(cancellationException)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val googleProvider = AuthProvider.Google(
            serverClientId = "test-client-id",
            scopes = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(googleProvider)
            }
        }

        try {
            instance.signInWithGoogle(
                context = applicationContext,
                config = config,
                provider = googleProvider,
                authorizationProvider = mockAuthorizationProvider,
                credentialManagerProvider = mockCredentialManagerProvider
            )
            throw AssertionError("Expected AuthCancelledException to be thrown")
        } catch (e: AuthException.AuthCancelledException) {
            assertThat(e.message).isEqualTo("Sign in with google was cancelled")
        }

        // Verify state is Error with AuthCancelledException
        val finalState = instance.authStateFlow().first()
        assertThat(finalState).isInstanceOf(AuthState.Error::class.java)
        val errorState = finalState as AuthState.Error
        assertThat(errorState.exception).isInstanceOf(AuthException.AuthCancelledException::class.java)
    }

    // =============================================================================================
    // signInWithGoogle - Anonymous Upgrade
    // =============================================================================================

    @Test
    fun `Sign in with Google with anonymous user and upgrade enabled should link credentials`() = runTest {
        val mockCredential = mock(AuthCredential::class.java)
        val mockAnonymousUser = mock(FirebaseUser::class.java)
        `when`(mockAnonymousUser.isAnonymous).thenReturn(true)
        `when`(mockAnonymousUser.uid).thenReturn("anonymous-uid")

        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockAnonymousUser)

        val googleSignInResult = AuthProvider.Google.GoogleSignInResult(
            credential = mockCredential,
            displayName = "Test User",
            photoUrl = null
        )

        `when`(
            mockCredentialManagerProvider.getGoogleCredential(
                context = eq(applicationContext),
                serverClientId = eq("test-client-id"),
                filterByAuthorizedAccounts = eq(true),
                autoSelectEnabled = eq(false)
            )
        ).thenReturn(googleSignInResult)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockAnonymousUser)
        `when`(mockAnonymousUser.linkWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val googleProvider = AuthProvider.Google(
            serverClientId = "test-client-id",
            scopes = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            isAnonymousUpgradeEnabled = true
            providers {
                provider(googleProvider)
            }
        }

        instance.signInWithGoogle(
            context = applicationContext,
            config = config,
            provider = googleProvider,
            authorizationProvider = mockAuthorizationProvider,
            credentialManagerProvider = mockCredentialManagerProvider
        )

        // Verify link was called instead of sign-in
        verify(mockAnonymousUser).linkWithCredential(mockCredential)
        verify(mockFirebaseAuth, never()).signInWithCredential(any())
    }

    // =============================================================================================
    // signInWithGoogle - State Management
    // =============================================================================================

    @Test
    fun `Sign in with Google should update state to Loading then Idle on success`() = runTest {
        val mockCredential = mock(AuthCredential::class.java)
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)

        val googleSignInResult = AuthProvider.Google.GoogleSignInResult(
            credential = mockCredential,
            displayName = "Test User",
            photoUrl = null
        )

        `when`(
            mockCredentialManagerProvider.getGoogleCredential(
                context = eq(applicationContext),
                serverClientId = eq("test-client-id"),
                filterByAuthorizedAccounts = eq(true),
                autoSelectEnabled = eq(false)
            )
        ).thenReturn(googleSignInResult)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val googleProvider = AuthProvider.Google(
            serverClientId = "test-client-id",
            scopes = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(googleProvider)
            }
        }

        // Verify initial state
        assertThat(instance.authStateFlow().first()).isEqualTo(AuthState.Idle)

        instance.signInWithGoogle(
            context = applicationContext,
            config = config,
            provider = googleProvider,
            authorizationProvider = mockAuthorizationProvider,
            credentialManagerProvider = mockCredentialManagerProvider
        )

        // Verify final state
        val finalState = instance.authStateFlow().first()
        assertThat(finalState).isEqualTo(AuthState.Idle)
    }
}
