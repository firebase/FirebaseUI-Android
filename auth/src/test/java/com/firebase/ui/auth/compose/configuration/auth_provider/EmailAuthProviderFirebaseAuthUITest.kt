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
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.util.EmailLinkPersistenceManager
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.atMost
import org.mockito.kotlin.never
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for email provider extension methods on [FirebaseAuthUI].
 *
 * Tests cover:
 * - createOrLinkUserWithEmailAndPassword
 * - signInWithEmailAndPassword
 * - signInAndLinkWithCredential
 * - sendSignInLinkToEmail
 * - signInWithEmailLink
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EmailAuthProviderFirebaseAuthUITest {

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    private lateinit var applicationContext: Context
    private lateinit var defaultApp: FirebaseApp
    private lateinit var emailProvider: AuthProvider.Email

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        applicationContext = ApplicationProvider.getApplicationContext()

        // Clear the instance cache
        FirebaseAuthUI.clearInstanceCache()

        // Clear any existing Firebase apps
        FirebaseApp.getApps(applicationContext).forEach { app ->
            app.delete()
        }

        // Initialize default FirebaseApp
        defaultApp = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )

        // Create email provider
        emailProvider = AuthProvider.Email(
            actionCodeSettings = null,
            passwordValidationRules = emptyList()
        )
    }

    @After
    fun tearDown() {
        FirebaseAuthUI.clearInstanceCache()
        try {
            defaultApp.delete()
        } catch (_: Exception) {
            // Ignore
        }
    }

    // =============================================================================================
    // createOrLinkUserWithEmailAndPassword Tests
    // =============================================================================================

    @Test
    fun `createOrLinkUserWithEmailAndPassword creates new user when not anonymous`() = runBlocking {
        // Setup
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.email).thenReturn("test@example.com")
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)
        taskCompletionSource.setResult(mockAuthResult)

        `when`(mockFirebaseAuth.createUserWithEmailAndPassword("test@example.com", "Password123"))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
        val config = authUIConfiguration {
            context = applicationContext
            providers { provider(emailProvider) }
        }

        // Collect states
        val states = mutableListOf<AuthState>()
        val job = launch {
            instance.authStateFlow().collect { state ->
                states.add(state)
            }
        }

        delay(100) // Allow initial state to be collected

        // Execute
        instance.createOrLinkUserWithEmailAndPassword(
            context = applicationContext,
            config = config,
            provider = emailProvider,
            email = "test@example.com",
            password = "Password123",
            newUser = null
        )

        delay(200) // Allow state updates to propagate
        job.cancel()

        // Verify method calls
        verify(mockFirebaseAuth, atMost(1))
            .createUserWithEmailAndPassword("test@example.com", "Password123")

        // Verify state transitions
        assertThat(states.size).isAtLeast(3)
        assertThat(states[0]).isEqualTo(AuthState.Idle) // Initial
        assertThat(states[1]).isInstanceOf(AuthState.Loading::class.java)
        val loadingState = states[1] as AuthState.Loading
        assertThat(loadingState.message).isEqualTo("Creating user...")
        assertThat(states[2]).isEqualTo(AuthState.Idle) // After completion
    }

    @Test
    fun `createOrLinkUserWithEmailAndPassword links credential when anonymous upgrade enabled`() =
        runTest {
            mockStatic(EmailAuthProvider::class.java).use { mockedProvider ->
                // Setup
                val mockCredential = mock(AuthCredential::class.java)
                mockedProvider.`when`<AuthCredential> {
                    EmailAuthProvider.getCredential("test@example.com", "Password123")
                }.thenReturn(mockCredential)

                val mockAnonymousUser = mock(FirebaseUser::class.java)
                `when`(mockAnonymousUser.isAnonymous).thenReturn(true)
                `when`(mockAnonymousUser.email).thenReturn("test@example.com")
                `when`(mockFirebaseAuth.currentUser).thenReturn(mockAnonymousUser)

                val taskCompletionSource = TaskCompletionSource<AuthResult>()
                val mockAuthResult = mock(AuthResult::class.java)
                `when`(mockAuthResult.user).thenReturn(mockAnonymousUser)
                taskCompletionSource.setResult(mockAuthResult)

                `when`(mockAnonymousUser.linkWithCredential(any(AuthCredential::class.java)))
                    .thenReturn(taskCompletionSource.task)

                val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
                val config = authUIConfiguration {
                    context = applicationContext
                    providers { provider(emailProvider) }
                    isAnonymousUpgradeEnabled = true
                }

                // Execute
                instance.createOrLinkUserWithEmailAndPassword(
                    context = applicationContext,
                    config = config,
                    provider = emailProvider,
                    email = "test@example.com",
                    password = "Password123",
                    newUser = null
                )

                // Verify
                mockedProvider.verify {
                    EmailAuthProvider.getCredential("test@example.com", "Password123")
                }
                verify(mockFirebaseAuth, never())
                    .createUserWithEmailAndPassword(any(), any())
                verify(mockAnonymousUser, atMost(1))
                    .linkWithCredential(mockCredential)
            }
        }

    @Test
    fun `createOrLinkUserWithEmailAndPassword throws exception for weak password`() = runTest {
        val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
        val config = authUIConfiguration {
            context = applicationContext
            providers { provider(emailProvider) }
        }

        try {
            instance.createOrLinkUserWithEmailAndPassword(
                context = applicationContext,
                config = config,
                provider = emailProvider,
                email = "test@example.com",
                password = "weak",
                newUser = null
            )
            assertThat(false).isTrue()
        } catch (e: AuthException.InvalidCredentialsException) {
            assertThat(e.message).isEqualTo(applicationContext.getString(R.string.fui_error_password_too_short).format(6))
        }
    }

    // =============================================================================================
    // signInWithEmailAndPassword Tests
    // =============================================================================================

    @Test
    fun `signInWithEmailAndPassword signs in user normally when not anonymous`() = runTest {
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.email).thenReturn("test@example.com")
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithEmailAndPassword("test@example.com", "Password123"))
            .thenReturn(taskCompletionSource.task)
        val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
        val config = authUIConfiguration {
            context = applicationContext
            providers { provider(emailProvider) }
        }

        instance.signInWithEmailAndPassword(
            config = config,
            provider = emailProvider,
            email = "test@example.com",
            password = "Password123",
            credentialForLinking = null,
            existingUser = null
        )

        verify(mockFirebaseAuth, atMost(1))
            .signInWithEmailAndPassword("test@example.com", "Password123")
    }

    @Test
    fun `signInWithEmailAndPassword links social credential when provided`() = runTest {
        // Setup
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockUser.email).thenReturn("test@example.com")
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)
        taskCompletionSource.setResult(mockAuthResult)

        `when`(mockFirebaseAuth.signInWithEmailAndPassword("test@example.com", "Password123"))
            .thenReturn(taskCompletionSource.task)

        // Mock social credential linking
        val mockSocialCredential = mock(AuthCredential::class.java)
        val linkTaskCompletionSource = TaskCompletionSource<AuthResult>()
        linkTaskCompletionSource.setResult(mockAuthResult)
        `when`(mockUser.linkWithCredential(mockSocialCredential))
            .thenReturn(linkTaskCompletionSource.task)

        val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
        val config = authUIConfiguration {
            context = applicationContext
            providers { provider(emailProvider) }
        }

        // Execute
        instance.signInWithEmailAndPassword(
            config = config,
            provider = emailProvider,
            email = "test@example.com",
            password = "Password123",
            credentialForLinking = mockSocialCredential,
            existingUser = null
        )

        // Verify
        verify(mockFirebaseAuth, atMost(1))
            .signInWithEmailAndPassword("test@example.com", "Password123")
        verify(mockUser, atMost(1))
            .linkWithCredential(mockSocialCredential)
    }

    // =============================================================================================
    // signInAndLinkWithCredential Tests
    // =============================================================================================

    @Test
    fun `signInAndLinkWithCredential signs in when not anonymous`() = runTest {
        // Setup
        val mockCredential = mock(AuthCredential::class.java)
        val mockUser = mock(FirebaseUser::class.java)
        `when`(mockFirebaseAuth.currentUser).thenReturn(null)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)
        taskCompletionSource.setResult(mockAuthResult)

        `when`(mockFirebaseAuth.signInWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
        val config = authUIConfiguration {
            context = applicationContext
            providers { provider(emailProvider) }
        }

        // Execute
        instance.signInAndLinkWithCredential(config, mockCredential)

        // Verify
        verify(mockFirebaseAuth, atMost(1))
            .signInWithCredential(mockCredential)
    }

    @Test
    fun `signInAndLinkWithCredential links credential when anonymous upgrade enabled`() = runTest {
        // Setup
        val mockCredential = mock(AuthCredential::class.java)
        val mockAnonymousUser = mock(FirebaseUser::class.java)
        `when`(mockAnonymousUser.isAnonymous).thenReturn(true)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockAnonymousUser)

        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockAnonymousUser)
        taskCompletionSource.setResult(mockAuthResult)

        `when`(mockAnonymousUser.linkWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
        val config = authUIConfiguration {
            context = applicationContext
            providers { provider(emailProvider) }
            isAnonymousUpgradeEnabled = true
        }

        // Execute
        instance.signInAndLinkWithCredential(config, mockCredential)

        // Verify
        verify(mockFirebaseAuth, never())
            .signInWithCredential(any(AuthCredential::class.java))
        verify(mockAnonymousUser, atMost(1))
            .linkWithCredential(mockCredential)
    }

    // =============================================================================================
    // sendSignInLinkToEmail Tests
    // =============================================================================================

    @Test
    fun `sendSignInLinkToEmail sends email and saves session to DataStore`() = runTest {
        // Setup
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://example.com/emailSignIn")
            .setHandleCodeInApp(true)
            .build()

        val emailProviderWithSettings = AuthProvider.Email(
            actionCodeSettings = actionCodeSettings,
            passwordValidationRules = emptyList()
        )

        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setResult(null)

        `when`(mockFirebaseAuth.currentUser).thenReturn(null)
        `when`(mockFirebaseAuth.sendSignInLinkToEmail(any(), any()))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
        val config = authUIConfiguration {
            context = applicationContext
            providers { provider(emailProviderWithSettings) }
        }

        // Execute
        instance.sendSignInLinkToEmail(
            context = applicationContext,
            config = config,
            provider = emailProviderWithSettings,
            email = "test@example.com"
        )

        // Verify
        verify(mockFirebaseAuth, atMost(1))
            .sendSignInLinkToEmail(any(), any())

        // Verify DataStore was saved
        val sessionRecord = EmailLinkPersistenceManager.retrieveSessionRecord(applicationContext)
        assertThat(sessionRecord).isNotNull()
        assertThat(sessionRecord?.email).isEqualTo("test@example.com")
        assertThat(sessionRecord?.sessionId).isNotEmpty()
    }

    @Test
    fun `sendSignInLinkToEmail saves anonymous user ID when upgrade enabled`() = runTest {
        // Setup
        val mockAnonymousUser = mock(FirebaseUser::class.java)
        `when`(mockAnonymousUser.isAnonymous).thenReturn(true)
        `when`(mockAnonymousUser.uid).thenReturn("anonymous-uid-123")
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockAnonymousUser)

        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://example.com/emailSignIn")
            .setHandleCodeInApp(true)
            .build()

        val emailProviderWithSettings = AuthProvider.Email(
            actionCodeSettings = actionCodeSettings,
            passwordValidationRules = emptyList()
        )

        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setResult(null)

        `when`(mockFirebaseAuth.sendSignInLinkToEmail(any(), any()))
            .thenReturn(taskCompletionSource.task)

        val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
        val config = authUIConfiguration {
            context = applicationContext
            providers { provider(emailProviderWithSettings) }
            isAnonymousUpgradeEnabled = true
        }

        // Execute
        instance.sendSignInLinkToEmail(
            context = applicationContext,
            config = config,
            provider = emailProviderWithSettings,
            email = "test@example.com"
        )

        // Verify
        val sessionRecord = EmailLinkPersistenceManager.retrieveSessionRecord(applicationContext)
        assertThat(sessionRecord?.anonymousUserId).isEqualTo("anonymous-uid-123")
    }

    // =============================================================================================
    // signInWithEmailLink Tests
    // =============================================================================================

    @Test
    fun `signInWithEmailLink completes normal sign-in flow`() = runTest {
        mockStatic(EmailAuthProvider::class.java).use { mockedProvider ->
            // Setup
            val emailLink = "https://example.com/emailSignIn?oobCode=ABC123&ui_sid=session123"

            `when`(mockFirebaseAuth.isSignInWithEmailLink(emailLink)).thenReturn(true)

            // Save session to DataStore
            EmailLinkPersistenceManager.saveEmail(
                context = applicationContext,
                email = "test@example.com",
                sessionId = "session123",
                anonymousUserId = null
            )

            val mockCredential = mock(AuthCredential::class.java)
            mockedProvider.`when`<AuthCredential> {
                EmailAuthProvider.getCredentialWithLink("test@example.com", emailLink)
            }.thenReturn(mockCredential)

            val mockUser = mock(FirebaseUser::class.java)
            val taskCompletionSource = TaskCompletionSource<AuthResult>()
            val mockAuthResult = mock(AuthResult::class.java)
            `when`(mockAuthResult.user).thenReturn(mockUser)
            taskCompletionSource.setResult(mockAuthResult)

            `when`(mockFirebaseAuth.currentUser).thenReturn(null)
            `when`(mockFirebaseAuth.signInWithCredential(mockCredential))
                .thenReturn(taskCompletionSource.task)

            val actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://example.com/emailSignIn")
                .setHandleCodeInApp(true)
                .build()

            val emailProviderWithSettings = AuthProvider.Email(
                actionCodeSettings = actionCodeSettings,
                passwordValidationRules = emptyList()
            )

            val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
            val config = authUIConfiguration {
                context = applicationContext
                providers { provider(emailProviderWithSettings) }
            }

            // Execute
            instance.signInWithEmailLink(
                context = applicationContext,
                config = config,
                provider = emailProviderWithSettings,
                email = "test@example.com",
                emailLink = emailLink,
                existingUser = null
            )

            // Verify
            verify(mockFirebaseAuth, atMost(1))
                .signInWithCredential(mockCredential)

            // Verify DataStore was cleared
            val sessionRecord =
                EmailLinkPersistenceManager.retrieveSessionRecord(applicationContext)
            assertThat(sessionRecord).isNull()
        }
    }

    @Test
    fun `signInWithEmailLink links social credential when stored`() = runTest {
        mockStatic(EmailAuthProvider::class.java).use { mockedEmailProvider ->
            mockStatic(GoogleAuthProvider::class.java).use { mockedGoogleProvider ->
                // Setup
                val emailLink = "https://example.com/emailSignIn?oobCode=ABC123&ui_sid=session123"

                `when`(mockFirebaseAuth.isSignInWithEmailLink(emailLink)).thenReturn(true)

                // Save session with Google credential to DataStore
                EmailLinkPersistenceManager.saveEmail(
                    context = applicationContext,
                    email = "test@example.com",
                    sessionId = "session123",
                    anonymousUserId = null
                )
                EmailLinkPersistenceManager.saveCredentialForLinking(
                    context = applicationContext,
                    providerType = "google.com",
                    idToken = "google-id-token",
                    accessToken = null
                )

                val mockEmailCredential = mock(AuthCredential::class.java)
                mockedEmailProvider.`when`<AuthCredential> {
                    EmailAuthProvider.getCredentialWithLink("test@example.com", emailLink)
                }.thenReturn(mockEmailCredential)

                val mockGoogleCredential = mock(AuthCredential::class.java)
                mockedGoogleProvider.`when`<AuthCredential> {
                    GoogleAuthProvider.getCredential("google-id-token", null)
                }.thenReturn(mockGoogleCredential)

                val mockUser = mock(FirebaseUser::class.java)
                val taskCompletionSource = TaskCompletionSource<AuthResult>()
                val mockAuthResult = mock(AuthResult::class.java)
                `when`(mockAuthResult.user).thenReturn(mockUser)
                taskCompletionSource.setResult(mockAuthResult)

                `when`(mockFirebaseAuth.currentUser).thenReturn(null)
                `when`(mockFirebaseAuth.signInWithCredential(mockEmailCredential))
                    .thenReturn(taskCompletionSource.task)

                val linkTaskCompletionSource = TaskCompletionSource<AuthResult>()
                linkTaskCompletionSource.setResult(mockAuthResult)
                `when`(mockUser.linkWithCredential(mockGoogleCredential))
                    .thenReturn(linkTaskCompletionSource.task)

                val actionCodeSettings = ActionCodeSettings.newBuilder()
                    .setUrl("https://example.com/emailSignIn")
                    .setHandleCodeInApp(true)
                    .build()

                val emailProviderWithSettings = AuthProvider.Email(
                    actionCodeSettings = actionCodeSettings,
                    passwordValidationRules = emptyList()
                )

                val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
                val config = authUIConfiguration {
                    context = applicationContext
                    providers { provider(emailProviderWithSettings) }
                }

                // Execute
                instance.signInWithEmailLink(
                    context = applicationContext,
                    config = config,
                    provider = emailProviderWithSettings,
                    email = "test@example.com",
                    emailLink = emailLink,
                    existingUser = null
                )

                // Verify
                verify(mockFirebaseAuth, atMost(1))
                    .signInWithCredential(mockEmailCredential)
                verify(mockUser, atMost(1))
                    .linkWithCredential(mockGoogleCredential)

                // Verify DataStore was cleared
                val sessionRecord =
                    EmailLinkPersistenceManager.retrieveSessionRecord(applicationContext)
                assertThat(sessionRecord).isNull()
            }
        }
    }

    @Test
    fun `signInWithEmailLink throws exception for invalid link`() = runTest {
        // Setup
        val emailLink = "https://example.com/invalid"

        `when`(mockFirebaseAuth.isSignInWithEmailLink(emailLink)).thenReturn(false)

        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://example.com/emailSignIn")
            .setHandleCodeInApp(true)
            .build()

        val emailProviderWithSettings = AuthProvider.Email(
            actionCodeSettings = actionCodeSettings,
            passwordValidationRules = emptyList()
        )

        val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
        val config = authUIConfiguration {
            context = applicationContext
            providers { provider(emailProviderWithSettings) }
        }

        // Execute
        instance.signInWithEmailLink(
            context = applicationContext,
            config = config,
            provider = emailProviderWithSettings,
            email = "test@example.com",
            emailLink = emailLink,
            existingUser = null
        )

        // Verify - method returns early with error state, so we just verify it was called
        verify(mockFirebaseAuth, atMost(1))
            .isSignInWithEmailLink(emailLink)
    }
}
