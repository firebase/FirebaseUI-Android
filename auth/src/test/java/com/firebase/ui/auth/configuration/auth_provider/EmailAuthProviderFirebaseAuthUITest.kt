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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.R
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.PasswordRule
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.util.EmailLinkPersistenceManager
import com.firebase.ui.auth.util.MockPersistenceManager
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive unit tests for Email Authentication provider methods in FirebaseAuthUI.
 *
 * Tests cover all email auth methods:
 * - createOrLinkUserWithEmailAndPassword
 * - signInWithEmailAndPassword
 * - signInAndLinkWithCredential
 * - sendSignInLinkToEmail
 * - signInWithEmailLink
 * - sendPasswordResetEmail
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EmailAuthProviderFirebaseAuthUITest {

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockEmailAuthCredentialProvider: AuthProvider.Email.CredentialProvider

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
    // createOrLinkUserWithEmailAndPassword Tests
    // =============================================================================================

    @Test
    fun `Create user with email and password without anonymous upgrade should succeed`() = runTest {
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(null)
        `when`(mockFirebaseAuth.createUserWithEmailAndPassword("test@example.com", "Pass@123"))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
        }

        instance.createOrLinkUserWithEmailAndPassword(
            context = applicationContext,
            config = config,
            provider = emailProvider,
            name = null,
            email = "test@example.com",
            password = "Pass@123"
        )

        verify(mockFirebaseAuth)
            .createUserWithEmailAndPassword("test@example.com", "Pass@123")
    }

    @Test
    fun `Link user with email and password with anonymous upgrade should succeed`() = runTest {
        val mockCredential = mock(AuthCredential::class.java)
        `when`(mockEmailAuthCredentialProvider.getCredential("test@example.com", "Pass@123"))
            .thenReturn(mockCredential)
        val mockAnonymousUser = mock(FirebaseUser::class.java)
        `when`(mockAnonymousUser.isAnonymous).thenReturn(true)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockAnonymousUser)
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(null)
        `when`(
            mockFirebaseAuth.currentUser?.linkWithCredential(
                ArgumentMatchers.any(AuthCredential::class.java)
            )
        ).thenReturn(taskCompletionSource.task)
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
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
            password = "Pass@123",
            credentialProvider = mockEmailAuthCredentialProvider
        )

        verify(mockEmailAuthCredentialProvider).getCredential("test@example.com", "Pass@123")
        verify(mockAnonymousUser).linkWithCredential(mockCredential)
    }

    @Test
    fun `createOrLinkUserWithEmailAndPassword - rejects weak password`() = runTest {
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
        }

        try {
            instance.createOrLinkUserWithEmailAndPassword(
                context = applicationContext,
                config = config,
                provider = emailProvider,
                name = null,
                email = "test@example.com",
                password = "weak"
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: Exception) {
            assertThat(e.message).contains(
                applicationContext
                    .getString(R.string.fui_error_password_too_short)
                    .format(emailProvider.minimumPasswordLength)
            )
        }

        verify(mockFirebaseAuth, never())
            .createUserWithEmailAndPassword(anyString(), anyString())
    }

    @Test
    fun `createOrLinkUserWithEmailAndPassword - validates custom password rules`() = runTest {
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = listOf(PasswordRule.RequireUppercase)
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
        }

        try {
            instance.createOrLinkUserWithEmailAndPassword(
                context = applicationContext,
                config = config,
                provider = emailProvider,
                name = null,
                email = "test@example.com",
                password = "pass@123"
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: Exception) {
            assertThat(e.message).isEqualTo(applicationContext.getString(R.string.fui_error_password_missing_uppercase))
        }
    }

    @Test
    fun `createOrLinkUserWithEmailAndPassword - respects isNewAccountsAllowed setting`() = runTest {
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList(),
            isNewAccountsAllowed = false
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
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
        } catch (e: Exception) {
            assertThat(e.message)
                .isEqualTo(applicationContext.getString(R.string.fui_error_email_does_not_exist))
        }
    }

    @Test
    fun `createOrLinkUserWithEmailAndPassword - handles collision exception`() = runTest {
        val mockAnonymousUser = mock(FirebaseUser::class.java)
        `when`(mockAnonymousUser.isAnonymous).thenReturn(true)
        `when`(mockAnonymousUser.email).thenReturn("test@example.com")
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
            assertThat(e.email).isNotNull()
            assertThat(e.credential).isNotNull()

            val currentState = instance.authStateFlow().first { it is AuthState.Error }
            assertThat(currentState).isInstanceOf(AuthState.Error::class.java)
            val errorState = currentState as AuthState.Error
            assertThat(errorState.exception).isInstanceOf(AuthException.AccountLinkingRequiredException::class.java)
        }
    }

    // =============================================================================================
    // signInWithEmailAndPassword Tests
    // =============================================================================================

    @Test
    fun `signInWithEmailAndPassword - successful sign in`() = runTest {
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithEmailAndPassword("test@example.com", "Pass@123"))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
        }

        val result = instance.signInWithEmailAndPassword(
            context = applicationContext,
            config = config,
            email = "test@example.com",
            password = "Pass@123"
        )

        assertThat(result).isNotNull()
        assertThat(result?.user).isEqualTo(mockUser)
        verify(mockFirebaseAuth).signInWithEmailAndPassword("test@example.com", "Pass@123")
    }

    @Test
    fun `signInWithEmailAndPassword - handles invalid credentials`() = runTest {
        val invalidCredentialsException = FirebaseAuthInvalidCredentialsException(
            "ERROR_WRONG_PASSWORD",
            "Wrong password"
        )
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(invalidCredentialsException)
        `when`(mockFirebaseAuth.signInWithEmailAndPassword("test@example.com", "Pass@123"))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
        }

        try {
            instance.signInWithEmailAndPassword(
                context = applicationContext,
                config = config,
                email = "test@example.com",
                password = "Pass@123"
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.InvalidCredentialsException) {
            assertThat(e.cause).isEqualTo(invalidCredentialsException)
        }
    }

    @Test
    fun `signInWithEmailAndPassword - handles user not found`() = runTest {
        val userNotFoundException = FirebaseAuthInvalidUserException(
            "ERROR_USER_NOT_FOUND",
            "User not found"
        )
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(userNotFoundException)
        `when`(mockFirebaseAuth.signInWithEmailAndPassword("test@example.com", "Pass@123"))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
        }

        try {
            instance.signInWithEmailAndPassword(
                context = applicationContext,
                config = config,
                email = "test@example.com",
                password = "Pass@123"
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.UserNotFoundException) {
            assertThat(e.cause).isEqualTo(userNotFoundException)
        }
    }

    @Test
    fun `signInWithEmailAndPassword - links credential after sign in`() = runTest {
        val googleCredential = GoogleAuthProvider.getCredential("google-id-token", null)
        val mockUser = mock(FirebaseUser::class.java)
        val signInAuthResult = mock(AuthResult::class.java)
        `when`(signInAuthResult.user).thenReturn(mockUser)
        val signInTask = TaskCompletionSource<AuthResult>()
        signInTask.setResult(signInAuthResult)

        val linkAuthResult = mock(AuthResult::class.java)
        `when`(linkAuthResult.user).thenReturn(mockUser)
        val linkTask = TaskCompletionSource<AuthResult>()
        linkTask.setResult(linkAuthResult)

        `when`(mockFirebaseAuth.signInWithEmailAndPassword("test@example.com", "Pass@123"))
            .thenReturn(signInTask.task)
        `when`(mockUser.linkWithCredential(googleCredential))
            .thenReturn(linkTask.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
        }

        instance.signInWithEmailAndPassword(
            context = applicationContext,
            config = config,
            email = "test@example.com",
            password = "Pass@123",
            credentialForLinking = googleCredential
        )

        verify(mockUser).linkWithCredential(googleCredential)
    }

    // =============================================================================================
    // signInAndLinkWithCredential Tests
    // =============================================================================================

    @Test
    fun `signInAndLinkWithCredential - successful sign in with credential`() = runTest {
        val credential = GoogleAuthProvider.getCredential("google-id-token", null)
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithCredential(credential))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
        }

        val result = instance.signInAndLinkWithCredential(
            config = config,
            credential = credential
        )

        assertThat(result).isNotNull()
        assertThat(result?.user).isEqualTo(mockUser)
        verify(mockFirebaseAuth).signInWithCredential(credential)
    }

    @Test
    fun `signInAndLinkWithCredential - handles anonymous upgrade`() = runTest {
        val anonymousUser = mock(FirebaseUser::class.java)
        `when`(anonymousUser.isAnonymous).thenReturn(true)
        `when`(mockFirebaseAuth.currentUser).thenReturn(anonymousUser)

        val credential = GoogleAuthProvider.getCredential("google-id-token", null)
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(anonymousUser)
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(anonymousUser.linkWithCredential(credential))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
            isAnonymousUpgradeEnabled = true
        }

        val result = instance.signInAndLinkWithCredential(
            config = config,
            credential = credential
        )

        assertThat(result).isNotNull()
        verify(anonymousUser).linkWithCredential(credential)
        verify(mockFirebaseAuth, never()).signInWithCredential(credential)
    }

    @Test
    fun `signInAndLinkWithCredential - handles collision and throws AccountLinkingRequiredException`() = runTest {
        val anonymousUser = mock(FirebaseUser::class.java)
        `when`(anonymousUser.isAnonymous).thenReturn(true)
        `when`(mockFirebaseAuth.currentUser).thenReturn(anonymousUser)

        val credential = GoogleAuthProvider.getCredential("google-id-token", null)
        val updatedCredential = mock(AuthCredential::class.java)

        val collisionException = mock(FirebaseAuthUserCollisionException::class.java)
        `when`(collisionException.errorCode).thenReturn("ERROR_CREDENTIAL_ALREADY_IN_USE")
        `when`(collisionException.updatedCredential).thenReturn(updatedCredential)
        `when`(collisionException.email).thenReturn("test@example.com")

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(collisionException)
        `when`(anonymousUser.linkWithCredential(credential))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
            isAnonymousUpgradeEnabled = true
        }

        try {
            instance.signInAndLinkWithCredential(
                config = config,
                credential = credential
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.AccountLinkingRequiredException) {
            assertThat(e.email).isEqualTo("test@example.com")
            assertThat(e.credential).isEqualTo(updatedCredential)
            assertThat(e.cause).isEqualTo(collisionException)
        }

        val currentState = instance.authStateFlow().first { it is AuthState.Error }
        assertThat(currentState).isInstanceOf(AuthState.Error::class.java)
        val errorState = currentState as AuthState.Error
        assertThat(errorState.exception).isInstanceOf(AuthException.AccountLinkingRequiredException::class.java)
    }

    // =============================================================================================
    // sendPasswordResetEmail Tests
    // =============================================================================================

    @Test
    fun `sendPasswordResetEmail - successfully sends reset email`() = runTest {
        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setResult(null)
        `when`(mockFirebaseAuth.sendPasswordResetEmail(
            ArgumentMatchers.eq("test@example.com"),
            ArgumentMatchers.isNull()
        )).thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        instance.sendPasswordResetEmail("test@example.com")

        verify(mockFirebaseAuth).sendPasswordResetEmail(
            ArgumentMatchers.eq("test@example.com"),
            ArgumentMatchers.isNull()
        )

        val finalState = instance.authStateFlow().first { it is AuthState.PasswordResetLinkSent }
        assertThat(finalState).isInstanceOf(AuthState.PasswordResetLinkSent::class.java)
    }

    @Test
    fun `sendPasswordResetEmail - sends with ActionCodeSettings`() = runTest {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://myapp.com/resetPassword")
            .setHandleCodeInApp(false)
            .build()
        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setResult(null)
        `when`(mockFirebaseAuth.sendPasswordResetEmail("test@example.com", actionCodeSettings))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        instance.sendPasswordResetEmail("test@example.com", actionCodeSettings)

        verify(mockFirebaseAuth).sendPasswordResetEmail("test@example.com", actionCodeSettings)

        val finalState = instance.authStateFlow().first { it is AuthState.PasswordResetLinkSent }
        assertThat(finalState).isInstanceOf(AuthState.PasswordResetLinkSent::class.java)
    }

    @Test
    fun `sendPasswordResetEmail - handles user not found`() = runTest {
        val userNotFoundException = FirebaseAuthInvalidUserException(
            "ERROR_USER_NOT_FOUND",
            "User not found"
        )
        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setException(userNotFoundException)
        `when`(mockFirebaseAuth.sendPasswordResetEmail(
            ArgumentMatchers.eq("test@example.com"),
            ArgumentMatchers.isNull()
        )).thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        try {
            instance.sendPasswordResetEmail("test@example.com")
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.UserNotFoundException) {
            assertThat(e.cause).isEqualTo(userNotFoundException)
        }
    }

    @Test
    fun `sendPasswordResetEmail - handles invalid email`() = runTest {
        val invalidEmailException = FirebaseAuthInvalidCredentialsException(
            "ERROR_INVALID_EMAIL",
            "Invalid email"
        )
        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setException(invalidEmailException)
        `when`(mockFirebaseAuth.sendPasswordResetEmail(
            ArgumentMatchers.eq("test@example.com"),
            ArgumentMatchers.isNull()
        )).thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        try {
            instance.sendPasswordResetEmail("test@example.com")
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.InvalidCredentialsException) {
            assertThat(e.cause).isEqualTo(invalidEmailException)
        }
    }

    @Test
    fun `sendPasswordResetEmail - handles cancellation`() = runTest {
        val cancellationException = CancellationException("Operation cancelled")
        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setException(cancellationException)
        `when`(mockFirebaseAuth.sendPasswordResetEmail(
            ArgumentMatchers.eq("test@example.com"),
            ArgumentMatchers.isNull()
        )).thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        try {
            instance.sendPasswordResetEmail("test@example.com")
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.AuthCancelledException) {
            assertThat(e.message).contains("cancelled")
            assertThat(e.cause).isInstanceOf(CancellationException::class.java)
        }
    }

    // =============================================================================================
    // sendSignInLinkToEmail Tests
    // =============================================================================================

    @Test
    fun `sendSignInLinkToEmail - normal flow - successfully sends email link`() = runTest {
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("test-uid")
        `when`(mockUser.isAnonymous).thenReturn(false)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockUser)

        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://example.com")
            .setHandleCodeInApp(true)
            .setAndroidPackageName("com.test", true, null)
            .build()

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = actionCodeSettings,
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setResult(null)
        `when`(mockFirebaseAuth.sendSignInLinkToEmail(anyString(), any()))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        instance.sendSignInLinkToEmail(
            context = applicationContext,
            config = config,
            provider = provider,
            email = "test@example.com",
            credentialForLinking = null
        )

        verify(mockFirebaseAuth).sendSignInLinkToEmail(
            ArgumentMatchers.eq("test@example.com"),
            any()
        )

        val finalState = instance.authStateFlow().first { it is AuthState.EmailSignInLinkSent }
        assertThat(finalState).isInstanceOf(AuthState.EmailSignInLinkSent::class.java)
    }

    @Test
    fun `sendSignInLinkToEmail - with anonymous user - includes anonymous user ID in link`() = runTest {
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("anonymous-uid-123")
        `when`(mockUser.isAnonymous).thenReturn(true)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockUser)

        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://example.com")
            .setHandleCodeInApp(true)
            .setAndroidPackageName("com.test", true, null)
            .build()

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = actionCodeSettings,
            isEmailLinkForceSameDeviceEnabled = true,
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
            isAnonymousUpgradeEnabled = true
        }

        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setResult(null)
        `when`(mockFirebaseAuth.sendSignInLinkToEmail(anyString(), any()))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        instance.sendSignInLinkToEmail(
            context = applicationContext,
            config = config,
            provider = provider,
            email = "test@example.com",
            credentialForLinking = null
        )

        verify(mockFirebaseAuth).sendSignInLinkToEmail(
            ArgumentMatchers.eq("test@example.com"),
            any()
        )

        val finalState = instance.authStateFlow().first { it is AuthState.EmailSignInLinkSent }
        assertThat(finalState).isInstanceOf(AuthState.EmailSignInLinkSent::class.java)
    }

    @Test
    fun `sendSignInLinkToEmail - with credential for linking - includes provider ID in link`() = runTest {
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("test-uid")
        `when`(mockUser.isAnonymous).thenReturn(false)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockUser)

        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://example.com")
            .setHandleCodeInApp(true)
            .setAndroidPackageName("com.test", true, null)
            .build()

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = actionCodeSettings,
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val googleCredential = GoogleAuthProvider.getCredential("id-token", null)

        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setResult(null)
        `when`(mockFirebaseAuth.sendSignInLinkToEmail(anyString(), any()))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        instance.sendSignInLinkToEmail(
            context = applicationContext,
            config = config,
            provider = provider,
            email = "test@example.com",
            credentialForLinking = googleCredential
        )

        verify(mockFirebaseAuth).sendSignInLinkToEmail(
            ArgumentMatchers.eq("test@example.com"),
            any()
        )

        val finalState = instance.authStateFlow().first { it is AuthState.EmailSignInLinkSent }
        assertThat(finalState).isInstanceOf(AuthState.EmailSignInLinkSent::class.java)
    }

    @Test
    fun `sendSignInLinkToEmail - handles network error`() = runTest {
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.uid).thenReturn("test-uid")
        `when`(mockUser.isAnonymous).thenReturn(false)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockUser)

        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://example.com")
            .setHandleCodeInApp(true)
            .setAndroidPackageName("com.test", true, null)
            .build()

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = actionCodeSettings,
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val networkException = Exception("Network error")
        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setException(networkException)
        `when`(mockFirebaseAuth.sendSignInLinkToEmail(anyString(), any()))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        try {
            instance.sendSignInLinkToEmail(
                context = applicationContext,
                config = config,
                provider = provider,
                email = "test@example.com",
                credentialForLinking = null
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException) {
            assertThat(e).isNotNull()
        }
    }

    // =============================================================================================
    // signInWithEmailLink Tests - Same Device Flow
    // =============================================================================================

    @Test
    fun `signInWithEmailLink - invalid link format - throws InvalidEmailLinkException`() = runTest {
        `when`(mockFirebaseAuth.isSignInWithEmailLink(anyString())).thenReturn(false)

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://example.com")
                .setHandleCodeInApp(true)
                .build(),
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        try {
            instance.signInWithEmailLink(
                context = applicationContext,
                config = config,
                provider = provider,
                email = "test@example.com",
                emailLink = "https://invalid-link.com"
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.InvalidEmailLinkException) {
            assertThat(e).isNotNull()
        }
    }

    @Test
    fun `signInWithEmailLink - same device normal flow - successfully signs in`() = runTest {
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.email).thenReturn("test@example.com")
        `when`(mockUser.displayName).thenReturn("Test User")
        `when`(mockUser.photoUrl).thenReturn(null)
        `when`(mockUser.isAnonymous).thenReturn(false)

        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)

        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        `when`(mockFirebaseAuth.isSignInWithEmailLink(anyString())).thenReturn(true)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithCredential(any())).thenReturn(taskCompletionSource.task)

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://example.com")
                .setHandleCodeInApp(true)
                .build(),
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        // Create mock persistence manager with matching session
        val mockPersistence = MockPersistenceManager()
        mockPersistence.setSessionRecord(
            EmailLinkPersistenceManager.SessionRecord(
                sessionId = "session123",
                email = "test@example.com",
                anonymousUserId = null,
                credentialForLinking = null
            )
        )

        val emailLink = "https://example.com/__/auth/action?apiKey=key&mode=signIn&oobCode=code&continueUrl=https://example.com?ui_sid=session123"

        val result = instance.signInWithEmailLink(
            context = applicationContext,
            config = config,
            provider = provider,
            email = "test@example.com",
            emailLink = emailLink,
            persistenceManager = mockPersistence
        )

        assertThat(result).isNotNull()
        assertThat(result?.user).isEqualTo(mockUser)
    }

    @Test
    fun `signInWithEmailLink - anonymous upgrade flow - successfully links credential`() = runTest {
        val mockAnonUser = mock(FirebaseUser::class.java)
        `when`(mockAnonUser.uid).thenReturn("anon-uid-123")
        `when`(mockAnonUser.email).thenReturn(null)
        `when`(mockAnonUser.isAnonymous).thenReturn(true)

        val mockLinkedUser = mock(FirebaseUser::class.java)
        `when`(mockLinkedUser.uid).thenReturn("anon-uid-123")
        `when`(mockLinkedUser.email).thenReturn("test@example.com")
        `when`(mockLinkedUser.displayName).thenReturn("Test User")
        `when`(mockLinkedUser.isAnonymous).thenReturn(false)

        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockLinkedUser)

        `when`(mockFirebaseAuth.currentUser).thenReturn(mockAnonUser)
        `when`(mockFirebaseAuth.isSignInWithEmailLink(anyString())).thenReturn(true)

        val linkTaskSource = TaskCompletionSource<AuthResult>()
        linkTaskSource.setResult(mockAuthResult)
        `when`(mockAnonUser.linkWithCredential(any())).thenReturn(linkTaskSource.task)

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://example.com")
                .setHandleCodeInApp(true)
                .build(),
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
            isAnonymousUpgradeEnabled = true
        }

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        // Create mock persistence manager with matching session and anonymous user
        val mockPersistence = MockPersistenceManager()
        mockPersistence.setSessionRecord(
            EmailLinkPersistenceManager.SessionRecord(
                sessionId = "session123",
                email = "test@example.com",
                anonymousUserId = "anon-uid-123",
                credentialForLinking = null
            )
        )

        val emailLink = "https://example.com/__/auth/action?apiKey=key&mode=signIn&oobCode=code&continueUrl=https://example.com?ui_sid=session123&ui_auid=anon-uid-123"

        val result = instance.signInWithEmailLink(
            context = applicationContext,
            config = config,
            provider = provider,
            email = "test@example.com",
            emailLink = emailLink,
            persistenceManager = mockPersistence
        )

        assertThat(result).isNotNull()
        verify(mockAnonUser).linkWithCredential(any())
    }

    // =============================================================================================
    // signInWithEmailLink Tests - Cross-Device Flow
    // =============================================================================================

    @Test
    fun `signInWithEmailLink - different device with no session - throws EmailLinkPromptForEmailException`() = runTest {
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        `when`(mockFirebaseAuth.isSignInWithEmailLink(anyString())).thenReturn(true)

        val actionCodeTask = TaskCompletionSource<com.google.firebase.auth.ActionCodeResult>()
        actionCodeTask.setResult(mock(com.google.firebase.auth.ActionCodeResult::class.java))
        `when`(mockFirebaseAuth.checkActionCode(anyString())).thenReturn(actionCodeTask.task)

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://example.com")
                .setHandleCodeInApp(true)
                .build(),
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        // Create mock persistence with no session (cross-device)
        val mockPersistence = MockPersistenceManager()
        
        // Email link with different session ID (cross-device)
        val emailLink = "https://example.com/__/auth/action?apiKey=key&mode=signIn&oobCode=code123&continueUrl=https://example.com?ui_sid=different-session"

        try {
            instance.signInWithEmailLink(
                context = applicationContext,
                config = config,
                provider = provider,
                email = "", // Empty email triggers prompt
                emailLink = emailLink,
                persistenceManager = mockPersistence
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.EmailLinkPromptForEmailException) {
            assertThat(e).isNotNull()
        }

        verify(mockFirebaseAuth).checkActionCode("code123")
    }

    @Test
    fun `signInWithEmailLink - different device with provider linking - throws EmailLinkCrossDeviceLinkingException`() = runTest {
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        `when`(mockFirebaseAuth.isSignInWithEmailLink(anyString())).thenReturn(true)

        val actionCodeTask = TaskCompletionSource<com.google.firebase.auth.ActionCodeResult>()
        actionCodeTask.setResult(mock(com.google.firebase.auth.ActionCodeResult::class.java))
        `when`(mockFirebaseAuth.checkActionCode(anyString())).thenReturn(actionCodeTask.task)

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://example.com")
                .setHandleCodeInApp(true)
                .build(),
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        // Create mock persistence with no session (cross-device)
        val mockPersistence = MockPersistenceManager()
        
        // Email link with provider ID (cross-device linking)
        val emailLink = "https://example.com/__/auth/action?apiKey=key&mode=signIn&oobCode=code123&continueUrl=https://example.com?ui_sid=different-session&ui_pid=google.com"

        try {
            instance.signInWithEmailLink(
                context = applicationContext,
                config = config,
                provider = provider,
                email = "", // Empty email triggers prompt (which detects provider linking)
                emailLink = emailLink,
                persistenceManager = mockPersistence
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.EmailLinkCrossDeviceLinkingException) {
            assertThat(e).isNotNull()
            assertThat(e.providerName).isEqualTo("Google")
        }

        verify(mockFirebaseAuth).checkActionCode("code123")
    }

    @Test
    fun `signInWithEmailLink - force same device on different device - throws EmailLinkWrongDeviceException`() = runTest {
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        `when`(mockFirebaseAuth.isSignInWithEmailLink(anyString())).thenReturn(true)

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            isEmailLinkForceSameDeviceEnabled = true,
            emailLinkActionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://example.com")
                .setHandleCodeInApp(true)
                .build(),
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        // Email link with force same device bit
        val emailLink = "https://example.com/__/auth/action?apiKey=key&mode=signIn&oobCode=code&continueUrl=https://example.com?ui_sid=different-session&ui_sd=1"

        try {
            instance.signInWithEmailLink(
                context = applicationContext,
                config = config,
                provider = provider,
                email = "test@example.com",
                emailLink = emailLink
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.EmailLinkWrongDeviceException) {
            assertThat(e).isNotNull()
        }
    }

    @Test
    fun `signInWithEmailLink - different anonymous user - throws EmailLinkDifferentAnonymousUserException`() = runTest {
        val mockAnonUser = mock(FirebaseUser::class.java)
        `when`(mockAnonUser.uid).thenReturn("current-anon-uid")
        `when`(mockAnonUser.isAnonymous).thenReturn(true)

        `when`(mockFirebaseAuth.currentUser).thenReturn(mockAnonUser)
        `when`(mockFirebaseAuth.isSignInWithEmailLink(anyString())).thenReturn(true)

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://example.com")
                .setHandleCodeInApp(true)
                .build(),
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
            isAnonymousUpgradeEnabled = true
        }

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        // Create mock persistence with session for different anonymous user
        val mockPersistence = MockPersistenceManager()
        mockPersistence.setSessionRecord(
            EmailLinkPersistenceManager.SessionRecord(
                sessionId = "session123",
                email = "test@example.com",
                anonymousUserId = "different-anon-uid",
                credentialForLinking = null
            )
        )

        val emailLink = "https://example.com/__/auth/action?apiKey=key&mode=signIn&oobCode=code&continueUrl=https://example.com?ui_sid=session123&ui_auid=different-anon-uid"

        try {
            instance.signInWithEmailLink(
                context = applicationContext,
                config = config,
                provider = provider,
                email = "test@example.com",
                emailLink = emailLink,
                persistenceManager = mockPersistence
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.EmailLinkDifferentAnonymousUserException) {
            assertThat(e).isNotNull()
        }
    }

    @Test
    fun `signInWithEmailLink - empty email on same device - throws EmailMismatchException`() = runTest {
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        `when`(mockFirebaseAuth.isSignInWithEmailLink(anyString())).thenReturn(true)

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://example.com")
                .setHandleCodeInApp(true)
                .build(),
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        // Create mock persistence with session but check email parameter is empty
        val mockPersistence = MockPersistenceManager()
        mockPersistence.setSessionRecord(
            EmailLinkPersistenceManager.SessionRecord(
                sessionId = "session123",
                email = "stored@example.com",
                anonymousUserId = null,
                credentialForLinking = null
            )
        )

        val emailLink = "https://example.com/__/auth/action?apiKey=key&mode=signIn&oobCode=code&continueUrl=https://example.com?ui_sid=session123"

        try {
            instance.signInWithEmailLink(
                context = applicationContext,
                config = config,
                provider = provider,
                email = "", // Empty email
                emailLink = emailLink,
                persistenceManager = mockPersistence
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.EmailMismatchException) {
            assertThat(e).isNotNull()
        }
    }

    @Test
    fun `signInWithEmailLink - invalid action code on different device - throws InvalidEmailLinkException`() = runTest {
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        `when`(mockFirebaseAuth.isSignInWithEmailLink(anyString())).thenReturn(true)

        val actionCodeTask = TaskCompletionSource<com.google.firebase.auth.ActionCodeResult>()
        actionCodeTask.setException(Exception("Invalid action code"))
        `when`(mockFirebaseAuth.checkActionCode(anyString())).thenReturn(actionCodeTask.task)

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://example.com")
                .setHandleCodeInApp(true)
                .build(),
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        // Create mock persistence with different session (cross-device)
        val mockPersistence = MockPersistenceManager()
        mockPersistence.setSessionRecord(
            EmailLinkPersistenceManager.SessionRecord(
                sessionId = "local-session",
                email = "test@example.com",
                anonymousUserId = null,
                credentialForLinking = null
            )
        )

        val emailLink = "https://example.com/__/auth/action?apiKey=key&mode=signIn&oobCode=invalid-code&continueUrl=https://example.com?ui_sid=different-session"

        try {
            instance.signInWithEmailLink(
                context = applicationContext,
                config = config,
                provider = provider,
                email = "", // Empty email triggers validation which will fail
                emailLink = emailLink,
                persistenceManager = mockPersistence
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.InvalidEmailLinkException) {
            assertThat(e).isNotNull()
        }

        verify(mockFirebaseAuth).checkActionCode("invalid-code")
    }

    @Test
    fun `signInWithEmailLink - no session ID in link - throws InvalidEmailLinkException`() = runTest {
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        `when`(mockFirebaseAuth.isSignInWithEmailLink(anyString())).thenReturn(true)

        val provider = AuthProvider.Email(
            isEmailLinkSignInEnabled = true,
            emailLinkActionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://example.com")
                .setHandleCodeInApp(true)
                .build(),
            passwordValidationRules = emptyList()
        )

        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        // Create mock persistence (can be null since we expect validation error)
        val mockPersistence = MockPersistenceManager()

        val emailLink = "https://example.com/__/auth/action?apiKey=key&mode=signIn&oobCode=code&continueUrl=https://example.com"

        try {
            instance.signInWithEmailLink(
                context = applicationContext,
                config = config,
                provider = provider,
                email = "test@example.com",
                emailLink = emailLink,
                persistenceManager = mockPersistence
            )
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.InvalidEmailLinkException) {
            assertThat(e).isNotNull()
        }
    }
}
