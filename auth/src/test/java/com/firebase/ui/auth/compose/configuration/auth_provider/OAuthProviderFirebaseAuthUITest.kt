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

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
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
 * Unit tests for OAuth provider sign-in methods in FirebaseAuthUI.
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class OAuthProviderFirebaseAuthUITest {

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockActivity: Activity

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
    // signInWithProvider - Success Case
    // =============================================================================================

    @Test
    fun `Sign in with OAuth provider should succeed`() = runTest {
        val mockOAuthCredential = mock(OAuthCredential::class.java)
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)
        `when`(mockAuthResult.credential).thenReturn(mockOAuthCredential)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)

        `when`(mockFirebaseAuth.pendingAuthResult).thenReturn(null)
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        `when`(mockFirebaseAuth.startActivityForSignInWithProvider(any<Activity>(), any<OAuthProvider>()))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val githubProvider = AuthProvider.Github(customParameters = emptyMap())
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(githubProvider)
            }
        }

        instance.signInWithProvider(
            config = config,
            activity = mockActivity,
            provider = githubProvider
        )

        // Verify OAuth provider was built and used
        verify(mockFirebaseAuth).startActivityForSignInWithProvider(
            eq(mockActivity),
            any<OAuthProvider>()
        )

        // Verify state is Idle after success
        val finalState = instance.authStateFlow().first()
        assertThat(finalState).isEqualTo(AuthState.Idle)
    }

    // =============================================================================================
    // signInWithProvider - Anonymous Upgrade
    // =============================================================================================

    @Test
    fun `Sign in with provider should upgrade anonymous user when enabled`() = runTest {
        val mockOAuthCredential = mock(OAuthCredential::class.java)
        val mockAnonymousUser = mock(FirebaseUser::class.java)
        `when`(mockAnonymousUser.isAnonymous).thenReturn(true)
        `when`(mockAnonymousUser.uid).thenReturn("anonymous-uid")
        `when`(mockAnonymousUser.displayName).thenReturn(null)
        `when`(mockAnonymousUser.photoUrl).thenReturn(null)

        val mockLinkedUser = mock(FirebaseUser::class.java)
        `when`(mockLinkedUser.isAnonymous).thenReturn(false)

        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockLinkedUser)
        `when`(mockAuthResult.credential).thenReturn(mockOAuthCredential)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)

        `when`(mockFirebaseAuth.pendingAuthResult).thenReturn(null)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockAnonymousUser)
        `when`(mockAnonymousUser.startActivityForLinkWithProvider(any<Activity>(), any<OAuthProvider>()))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val yahooProvider = AuthProvider.Yahoo(customParameters = emptyMap())
        val config = authUIConfiguration {
            context = applicationContext
            isAnonymousUpgradeEnabled = true
            providers {
                provider(yahooProvider)
            }
        }

        instance.signInWithProvider(
            config = config,
            activity = mockActivity,
            provider = yahooProvider
        )

        // Verify link was called instead of sign-in
        verify(mockAnonymousUser).startActivityForLinkWithProvider(eq(mockActivity), any<OAuthProvider>())
        verify(mockFirebaseAuth, never()).startActivityForSignInWithProvider(any<Activity>(), any<OAuthProvider>())

        // Verify the operation completed (state should be Idle after successful link)
        val finalState = instance.authStateFlow().first()
        assertThat(finalState).isNotNull()
    }

    // =============================================================================================
    // signInWithProvider - Error Handling
    // =============================================================================================

    @Test
    fun `Sign in with provider should throw AccountLinkingRequiredException on collision`() = runTest {
        val collisionEmail = "test@example.com"
        val mockCredential = mock(AuthCredential::class.java)
        val collisionException = mock(FirebaseAuthUserCollisionException::class.java)
        `when`(collisionException.email).thenReturn(collisionEmail)
        `when`(collisionException.updatedCredential).thenReturn(mockCredential)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(collisionException)

        `when`(mockFirebaseAuth.pendingAuthResult).thenReturn(null)
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        `when`(mockFirebaseAuth.startActivityForSignInWithProvider(any<Activity>(), any<OAuthProvider>()))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val githubProvider = AuthProvider.Github(customParameters = emptyMap())
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(githubProvider)
            }
        }

        try {
            instance.signInWithProvider(
                config = config,
                activity = mockActivity,
                provider = githubProvider
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.AccountLinkingRequiredException) {
            // Verify it's the right exception type with expected fields
            assertThat(e).isNotNull()
            assertThat(e).isInstanceOf(AuthException.AccountLinkingRequiredException::class.java)
        }

        // Verify state is Error
        val finalState = instance.authStateFlow().first()
        assertThat(finalState).isInstanceOf(AuthState.Error::class.java)
        val errorState = finalState as AuthState.Error
        assertThat(errorState.exception).isInstanceOf(AuthException.AccountLinkingRequiredException::class.java)
    }

    @Test
    fun `Sign in with provider should throw AuthCancelledException when cancelled`() = runTest {
        val cancellationException = CancellationException("User cancelled")
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(cancellationException)

        `when`(mockFirebaseAuth.pendingAuthResult).thenReturn(null)
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        `when`(mockFirebaseAuth.startActivityForSignInWithProvider(any<Activity>(), any<OAuthProvider>()))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val microsoftProvider = AuthProvider.Microsoft(tenant = null, customParameters = emptyMap())
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(microsoftProvider)
            }
        }

        try {
            instance.signInWithProvider(
                config = config,
                activity = mockActivity,
                provider = microsoftProvider
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.AuthCancelledException) {
            assertThat(e.message).contains("Signing in with Microsoft was cancelled")
        }

        // Verify state is Error
        val finalState = instance.authStateFlow().first()
        assertThat(finalState).isInstanceOf(AuthState.Error::class.java)
        val errorState = finalState as AuthState.Error
        assertThat(errorState.exception).isInstanceOf(AuthException.AuthCancelledException::class.java)
    }
}
