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
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AnonymousAuthProviderFirebaseAuthUITest {

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

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
    // signInAnonymously Tests
    // =============================================================================================

    @Test
    fun `signInAnonymously - successful anonymous sign in`() = runTest {
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.isAnonymous).thenReturn(true)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInAnonymously())
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        instance.signInAnonymously()

        verify(mockFirebaseAuth).signInAnonymously()

        val finalState = instance.authStateFlow().first { it is AuthState.Idle }
        assertThat(finalState).isInstanceOf(AuthState.Idle::class.java)
    }

    @Test
    fun `signInAnonymously - handles network error`() = runTest {
        val networkException = FirebaseNetworkException("Network error")
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(networkException)
        `when`(mockFirebaseAuth.signInAnonymously())
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        try {
            instance.signInAnonymously()
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.NetworkException) {
            assertThat(e.cause).isEqualTo(networkException)
        }

        val currentState = instance.authStateFlow().first { it is AuthState.Error }
        assertThat(currentState).isInstanceOf(AuthState.Error::class.java)
        val errorState = currentState as AuthState.Error
        assertThat(errorState.exception).isInstanceOf(AuthException.NetworkException::class.java)
    }

    @Test
    fun `signInAnonymously - handles cancellation`() = runTest {
        val cancellationException = CancellationException("Operation cancelled")
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(cancellationException)
        `when`(mockFirebaseAuth.signInAnonymously())
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        try {
            instance.signInAnonymously()
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.AuthCancelledException) {
            assertThat(e.message).contains("cancelled")
            assertThat(e.cause).isInstanceOf(CancellationException::class.java)
        }

        val currentState = instance.authStateFlow().first { it is AuthState.Error }
        assertThat(currentState).isInstanceOf(AuthState.Error::class.java)
        val errorState = currentState as AuthState.Error
        assertThat(errorState.exception).isInstanceOf(AuthException.AuthCancelledException::class.java)
    }

    @Test
    fun `signInAnonymously - handles generic exception`() = runTest {
        val genericException = RuntimeException("Something went wrong")
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(genericException)
        `when`(mockFirebaseAuth.signInAnonymously())
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        try {
            instance.signInAnonymously()
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.UnknownException) {
            assertThat(e.cause).isEqualTo(genericException)
        }

        val currentState = instance.authStateFlow().first { it is AuthState.Error }
        assertThat(currentState).isInstanceOf(AuthState.Error::class.java)
        val errorState = currentState as AuthState.Error
        assertThat(errorState.exception).isInstanceOf(AuthException.UnknownException::class.java)
    }

    // =============================================================================================
    // Anonymous Account Upgrade Tests
    // =============================================================================================

    @Test
    fun `Upgrade anonymous account with email and password when isAnonymousUpgradeEnabled`() = runTest {
        val mockAnonymousUser = mock(FirebaseUser::class.java)
        `when`(mockAnonymousUser.isAnonymous).thenReturn(true)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockAnonymousUser)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mock(AuthResult::class.java))
        `when`(mockAnonymousUser.linkWithCredential(ArgumentMatchers.any(AuthCredential::class.java)))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(AuthProvider.Anonymous)
                provider(emailProvider)
            }
            isAnonymousUpgradeEnabled = true
        }

        instance.createOrLinkUserWithEmailAndPassword(
            context = applicationContext,
            config = config,
            provider = emailProvider,
            name = null,
            email = "test@example.com",
            password = "Pass@123"
        )

        verify(mockAnonymousUser).linkWithCredential(ArgumentMatchers.any(AuthCredential::class.java))
    }

    @Test
    fun `Upgrade anonymous account throws AccountLinkingRequiredException on collision`() = runTest {
        val mockAnonymousUser = mock(FirebaseUser::class.java)
        `when`(mockAnonymousUser.isAnonymous).thenReturn(true)
        `when`(mockAnonymousUser.email).thenReturn(null)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockAnonymousUser)

        val collisionException = mock(FirebaseAuthUserCollisionException::class.java)
        `when`(collisionException.errorCode).thenReturn("ERROR_EMAIL_ALREADY_IN_USE")
        `when`(collisionException.email).thenReturn("test@example.com")

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(collisionException)
        `when`(mockAnonymousUser.linkWithCredential(ArgumentMatchers.any(AuthCredential::class.java)))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(AuthProvider.Anonymous)
                provider(emailProvider)
            }
            isAnonymousUpgradeEnabled = true
        }

        try {
            instance.createOrLinkUserWithEmailAndPassword(
                context = applicationContext,
                config = config,
                provider = emailProvider,
                name = null,
                email = "test@example.com",
                password = "Pass@123"
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.AccountLinkingRequiredException) {
            assertThat(e.cause).isEqualTo(collisionException)
            assertThat(e.email).isEqualTo("test@example.com")
            assertThat(e.credential).isNotNull()
        }

        val currentState = instance.authStateFlow().first { it is AuthState.Error }
        assertThat(currentState).isInstanceOf(AuthState.Error::class.java)
        val errorState = currentState as AuthState.Error
        assertThat(errorState.exception).isInstanceOf(AuthException.AccountLinkingRequiredException::class.java)
    }

    @Test
    fun `Upgrade anonymous account with credential when isAnonymousUpgradeEnabled`() = runTest {
        val mockAnonymousUser = mock(FirebaseUser::class.java)
        `when`(mockAnonymousUser.isAnonymous).thenReturn(true)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockAnonymousUser)

        val credential = EmailAuthProvider.getCredential("test@example.com", "Pass@123")
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockAnonymousUser)
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockAnonymousUser.linkWithCredential(credential))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(AuthProvider.Anonymous)
                provider(emailProvider)
            }
            isAnonymousUpgradeEnabled = true
        }

        val result = instance.signInAndLinkWithCredential(
            config = config,
            credential = credential
        )

        assertThat(result).isNotNull()
        verify(mockAnonymousUser).linkWithCredential(credential)
    }
}
