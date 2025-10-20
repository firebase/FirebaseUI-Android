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
import androidx.test.core.app.ApplicationProvider
import com.facebook.AccessToken
import com.facebook.FacebookException
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider.Facebook.FacebookProfileData
import com.firebase.ui.auth.compose.util.EmailLinkPersistenceManager
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for Facebook Authentication provider methods in FirebaseAuthUI.
 **/
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FacebookAuthProviderFirebaseAuthUITest {

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockFBAuthCredentialProvider: AuthProvider.Facebook.CredentialProvider

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

    @Test
    fun `signInWithFacebook - successful sign in signs user in and emits Success authState`() = runTest {
        val authStateListeners = mutableListOf<AuthStateListener>()
        doAnswer { invocation ->
            val listener = invocation.getArgument<AuthStateListener>(0)
            authStateListeners += listener
            null
        }.whenever(mockFirebaseAuth).addAuthStateListener(any())
        doAnswer { invocation ->
            val listener = invocation.getArgument<AuthStateListener>(0)
            authStateListeners -= listener
            null
        }.whenever(mockFirebaseAuth).removeAuthStateListener(any())
        whenever(mockFirebaseAuth.currentUser).thenReturn(null)

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val provider = spy(AuthProvider.Facebook(
            applicationId = "000000000000"
        ))
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val mockAccessToken = mock<AccessToken> {
            on { token } doReturn "random-token"
        }
        val mockCredential = mock<AuthCredential>()
        val mockUser = mock<FirebaseUser>()
        val mockAuthResult = mock<AuthResult>()
        whenever(mockAuthResult.user).thenReturn(mockUser)
        whenever(mockUser.isEmailVerified).thenReturn(true)
        whenever(mockUser.providerData).thenReturn(emptyList())
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        whenever(mockFirebaseAuth.signInWithCredential(mockCredential))
            .thenReturn(taskCompletionSource.task)
        doReturn(
            FacebookProfileData(
                displayName = "Test User",
                email = "test@example.com",
                photoUrl = Uri.parse("https://someurl.com/photo.png")
            )
        ).whenever(provider).fetchFacebookProfile(any())
        whenever(mockFBAuthCredentialProvider.getCredential("random-token"))
            .thenReturn(mockCredential)

        val successStateDeferred = async {
            instance.authStateFlow().first { it is AuthState.Success }
        }

        instance.signInWithFacebook(
            context = applicationContext,
            config = config,
            provider = provider,
            accessToken = mockAccessToken,
            credentialProvider = mockFBAuthCredentialProvider
        )

        whenever(mockFirebaseAuth.currentUser).thenReturn(mockUser)
        authStateListeners.forEach { listener ->
            listener.onAuthStateChanged(mockFirebaseAuth)
        }

        val successState = successStateDeferred.await() as AuthState.Success
        assertThat(successState.user).isEqualTo(mockUser)
        verify(mockFBAuthCredentialProvider).getCredential("random-token")
        verify(mockFirebaseAuth).signInWithCredential(mockCredential)
    }

    @Test
    fun `signInWithFacebook - handles account collision by saving credential and emitting error`() = runTest {
        EmailLinkPersistenceManager.clear(applicationContext)
        EmailLinkPersistenceManager.saveEmail(
            context = applicationContext,
            email = "link@example.com",
            sessionId = "session-id",
            anonymousUserId = null
        )

        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val provider = spy(AuthProvider.Facebook(
            applicationId = "000000000000"
        ))
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val mockAccessToken = mock<AccessToken> {
            on { token } doReturn "collision-token"
        }
        val mockCredential = mock<AuthCredential>()
        val collisionException = mock<FirebaseAuthUserCollisionException> {
            on { errorCode } doReturn "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL"
            on { email } doReturn "existing@example.com"
        }
        val failingTask = TaskCompletionSource<AuthResult>()
        failingTask.setException(collisionException)
        whenever(mockFirebaseAuth.signInWithCredential(mockCredential))
            .thenReturn(failingTask.task)
        doReturn(null).whenever(provider).fetchFacebookProfile(any())
        whenever(mockFBAuthCredentialProvider.getCredential("collision-token"))
            .thenReturn(mockCredential)

        try {
            instance.signInWithFacebook(
                context = applicationContext,
                config = config,
                provider = provider,
                accessToken = mockAccessToken,
                credentialProvider = mockFBAuthCredentialProvider
            )
            assertThat(false).isTrue()
        } catch (e: AuthException.AccountLinkingRequiredException) {
            assertThat(e.cause).isEqualTo(collisionException)
            val currentState = instance.authStateFlow().first { it is AuthState.Error }
            assertThat(currentState).isInstanceOf(AuthState.Error::class.java)
            val errorState = currentState as AuthState.Error
            assertThat(errorState.exception).isInstanceOf(AuthException.AccountLinkingRequiredException::class.java)

            val sessionRecord = EmailLinkPersistenceManager.retrieveSessionRecord(applicationContext)
            assertThat(sessionRecord).isNotNull()
            assertThat(sessionRecord?.credentialForLinking).isNotNull()
            assertThat(sessionRecord?.credentialForLinking?.provider)
                .isEqualTo(provider.providerId)
        } finally {
            EmailLinkPersistenceManager.clear(applicationContext)
        }
    }

    @Test
    fun `signInWithFacebook - converts FacebookException into AuthException`() = runTest {
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val provider = spy(AuthProvider.Facebook(
            applicationId = "000000000000"
        ))
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(provider)
            }
        }

        val mockAccessToken = mock<AccessToken> {
            on { token } doReturn "error-token"
        }
        doAnswer {
            throw FacebookException("Graph error")
        }.whenever(provider).fetchFacebookProfile(any())

        try {
            instance.signInWithFacebook(
                context = applicationContext,
                config = config,
                provider = provider,
                accessToken = mockAccessToken,
                credentialProvider = mockFBAuthCredentialProvider
            )
            assertThat(false).isTrue()
        } catch (e: AuthException) {
            val currentState = instance.authStateFlow().first { it is AuthState.Error }
            assertThat(currentState).isInstanceOf(AuthState.Error::class.java)
            val errorState = currentState as AuthState.Error
            assertThat(errorState.exception).isEqualTo(e)
            assertThat(e).isInstanceOf(AuthException.UnknownException::class.java)
        }
    }
}
