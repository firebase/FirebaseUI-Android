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
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import org.mockito.Mockito.anyString
import org.mockito.MockitoAnnotations
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
            actionCodeSettings = null,
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
        mockStatic(EmailAuthProvider::class.java).use { mockedProvider ->
            val mockCredential = mock(AuthCredential::class.java)
            mockedProvider.`when`<AuthCredential> {
                EmailAuthProvider.getCredential("test@example.com", "Pass@123")
            }.thenReturn(mockCredential)
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
                actionCodeSettings = null,
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
                password = "Pass@123"
            )

            mockedProvider.verify {
                EmailAuthProvider.getCredential("test@example.com", "Pass@123")
            }
            verify(mockAnonymousUser).linkWithCredential(mockCredential)
        }
    }

    @Test
    fun `createOrLinkUserWithEmailAndPassword - rejects weak password`() = runTest {
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            actionCodeSettings = null,
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
            actionCodeSettings = null,
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
        } catch (e: Exception) {
            assertThat(e.message).isEqualTo(applicationContext.getString(R.string.fui_error_password_missing_uppercase))
        }
    }

    @Test
    fun `createOrLinkUserWithEmailAndPassword - respects isNewAccountsAllowed setting`() = runTest {
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            actionCodeSettings = null,
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

        val collisionException = FirebaseAuthUserCollisionException(
            "ERROR_EMAIL_ALREADY_IN_USE",
            "Email already in use"
        )
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(collisionException)
        `when`(mockAnonymousUser.linkWithCredential(ArgumentMatchers.any(AuthCredential::class.java)))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            actionCodeSettings = null,
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
        } catch (e: AuthException) {
            assertThat(e.cause).isEqualTo(collisionException)
            val currentState = instance.authStateFlow().first { it is AuthState.MergeConflict }
            assertThat(currentState).isInstanceOf(AuthState.MergeConflict::class.java)
            val mergeConflict = currentState as AuthState.MergeConflict
            assertThat(mergeConflict.pendingCredential).isNotNull()
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
            actionCodeSettings = null,
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
            actionCodeSettings = null,
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
            actionCodeSettings = null,
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
            actionCodeSettings = null,
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
            actionCodeSettings = null,
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
            actionCodeSettings = null,
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
    fun `signInAndLinkWithCredential - handles collision and emits MergeConflict`() = runTest {
        val anonymousUser = mock(FirebaseUser::class.java)
        `when`(anonymousUser.isAnonymous).thenReturn(true)
        `when`(mockFirebaseAuth.currentUser).thenReturn(anonymousUser)

        val credential = GoogleAuthProvider.getCredential("google-id-token", null)
        val updatedCredential = EmailAuthProvider.getCredential("test@example.com", "Pass@123")

        val collisionException = FirebaseAuthUserCollisionException(
            "ERROR_CREDENTIAL_ALREADY_IN_USE",
            "Credential already in use"
        )
        // Set updatedCredential using reflection
        val field = FirebaseAuthUserCollisionException::class.java.getDeclaredField("zza")
        field.isAccessible = true
        field.set(collisionException, updatedCredential)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(collisionException)
        `when`(anonymousUser.linkWithCredential(credential))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            actionCodeSettings = null,
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
        } catch (e: AuthException) {
            // Expected
        }

        val currentState = instance.authStateFlow().first { it is AuthState.MergeConflict }
        assertThat(currentState).isInstanceOf(AuthState.MergeConflict::class.java)
        val mergeConflict = currentState as AuthState.MergeConflict
        assertThat(mergeConflict.pendingCredential).isEqualTo(updatedCredential)
    }

    // =============================================================================================
    // sendPasswordResetEmail Tests
    // =============================================================================================

    @Test
    fun `sendPasswordResetEmail - successfully sends reset email`() = runTest {
        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setResult(null)
        `when`(mockFirebaseAuth.sendPasswordResetEmail("test@example.com"))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        val result = instance.sendPasswordResetEmail("test@example.com")

        assertThat(result).isEqualTo("test@example.com")
        verify(mockFirebaseAuth).sendPasswordResetEmail("test@example.com")

        val finalState = instance.authStateFlow().first()
        assertThat(finalState is AuthState.Idle).isTrue()
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

        val result = instance.sendPasswordResetEmail("test@example.com", actionCodeSettings)

        assertThat(result).isEqualTo("test@example.com")
        verify(mockFirebaseAuth).sendPasswordResetEmail("test@example.com", actionCodeSettings)
    }

    @Test
    fun `sendPasswordResetEmail - handles user not found`() = runTest {
        val userNotFoundException = FirebaseAuthInvalidUserException(
            "ERROR_USER_NOT_FOUND",
            "User not found"
        )
        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setException(userNotFoundException)
        `when`(mockFirebaseAuth.sendPasswordResetEmail("test@example.com"))
            .thenReturn(taskCompletionSource.task)

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
        `when`(mockFirebaseAuth.sendPasswordResetEmail("test@example.com"))
            .thenReturn(taskCompletionSource.task)

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
        `when`(mockFirebaseAuth.sendPasswordResetEmail("test@example.com"))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        try {
            instance.sendPasswordResetEmail("test@example.com")
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.AuthCancelledException) {
            assertThat(e.message).contains("cancelled")
            assertThat(e.cause).isInstanceOf(CancellationException::class.java)
        }
    }
}
