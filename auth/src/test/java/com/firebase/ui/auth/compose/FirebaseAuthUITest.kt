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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.compose.configuration.AuthProvider
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.actionCodeSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [FirebaseAuthUI] covering singleton behavior, multi-app support,
 * and custom authentication injection for multi-tenancy scenarios.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FirebaseAuthUITest {

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    private lateinit var defaultApp: FirebaseApp
    private lateinit var secondaryApp: FirebaseApp

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        // Clear the instance cache before each test to ensure test isolation
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

        // Initialize secondary FirebaseApp
        secondaryApp = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key-2")
                .setApplicationId("fake-app-id-2")
                .setProjectId("fake-project-id-2")
                .build(),
            "secondary"
        )
    }

    @After
    fun tearDown() {
        // Clean up after each test to prevent test pollution
        FirebaseAuthUI.clearInstanceCache()

        // Clean up Firebase apps
        try {
            defaultApp.delete()
        } catch (_: Exception) {
            // Ignore if already deleted
        }
        try {
            secondaryApp.delete()
        } catch (_: Exception) {
            // Ignore if already deleted
        }
    }

    // =============================================================================================
    // Singleton Behavior Tests
    // =============================================================================================

    @Test
    fun `getInstance() returns same instance for default app`() {
        // Get instance twice
        val instance1 = FirebaseAuthUI.getInstance()
        val instance2 = FirebaseAuthUI.getInstance()

        // Verify they are the same instance (singleton pattern)
        assertThat(instance1).isEqualTo(instance2)
        assertThat(instance1.app.name).isEqualTo(FirebaseApp.DEFAULT_APP_NAME)

        // Verify only one instance is cached
        assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(1)
    }

    @Test
    fun `getInstance() works with initialized Firebase app`() {
        // Ensure we can get an instance when Firebase is properly initialized
        val instance = FirebaseAuthUI.getInstance()

        // Verify the instance uses the default app
        assertThat(instance.app).isEqualTo(defaultApp)
        assertThat(instance.auth).isNotNull()
    }

    // =============================================================================================
    // Multi-App Support Tests
    // =============================================================================================

    @Test
    fun `getInstance(app) returns distinct instances per FirebaseApp`() {
        // Get instances for different apps
        val defaultInstance = FirebaseAuthUI.getInstance(defaultApp)
        val secondaryInstance = FirebaseAuthUI.getInstance(secondaryApp)

        // Verify they are different instances
        assertThat(defaultInstance).isNotEqualTo(secondaryInstance)

        // Verify correct apps are used
        assertThat(defaultInstance.app).isEqualTo(defaultApp)
        assertThat(secondaryInstance.app).isEqualTo(secondaryApp)

        // Verify both instances are cached
        assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(2)
    }

    @Test
    fun `getInstance(app) returns same instance for same app`() {
        // Get instance twice for the same app
        val instance1 = FirebaseAuthUI.getInstance(defaultApp)
        val instance2 = FirebaseAuthUI.getInstance(defaultApp)

        // Verify they are the same instance (caching works)
        assertThat(instance1).isEqualTo(instance2)
        assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(1)
    }

    @Test
    fun `getInstance(app) with secondary app returns correct instance`() {
        // Get instance for secondary app
        val instance = FirebaseAuthUI.getInstance(secondaryApp)

        // Verify correct app is used
        assertThat(instance.app).isEqualTo(secondaryApp)
        assertThat(instance.app.name).isEqualTo("secondary")
    }

    // =============================================================================================
    // Custom Auth Injection Tests
    // =============================================================================================

    @Test
    fun `create() returns new instance with provided dependencies`() {
        // Create instances with custom auth
        val instance1 = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
        val instance2 = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)

        // Verify they are different instances (no caching)
        assertThat(instance1).isNotEqualTo(instance2)

        // Verify correct dependencies are used
        assertThat(instance1.app).isEqualTo(defaultApp)
        assertThat(instance1.auth).isEqualTo(mockFirebaseAuth)
        assertThat(instance2.app).isEqualTo(defaultApp)
        assertThat(instance2.auth).isEqualTo(mockFirebaseAuth)

        // Verify cache is not used for create()
        assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(0)
    }

    @Test
    fun `create() allows custom auth injection for multi-tenancy`() {
        // Create mock custom auth with tenant
        val customAuth = mock(FirebaseAuth::class.java)
        `when`(customAuth.tenantId).thenReturn("customer-tenant-123")

        // Create instance with custom auth
        val instance = FirebaseAuthUI.create(defaultApp, customAuth)

        // Verify custom auth is used
        assertThat(instance.auth).isEqualTo(customAuth)
        assertThat(instance.auth.tenantId).isEqualTo("customer-tenant-123")
    }

    @Test
    fun `create() with different auth instances returns different FirebaseAuthUI instances`() {
        // Create two different mock auth instances
        val auth1 = mock(FirebaseAuth::class.java)
        val auth2 = mock(FirebaseAuth::class.java)

        // Create instances with different auth
        val instance1 = FirebaseAuthUI.create(defaultApp, auth1)
        val instance2 = FirebaseAuthUI.create(defaultApp, auth2)

        // Verify they are different instances
        assertThat(instance1).isNotEqualTo(instance2)
        assertThat(instance1.auth).isEqualTo(auth1)
        assertThat(instance2.auth).isEqualTo(auth2)
    }

    // =============================================================================================
    // Cache Isolation Tests
    // =============================================================================================

    @Test
    fun `getInstance() and getInstance(app) use separate cache entries for default app`() {
        // Get default instance via getInstance()
        val defaultInstance1 = FirebaseAuthUI.getInstance()

        // Get instance for default app via getInstance(app)
        val defaultInstance2 = FirebaseAuthUI.getInstance(defaultApp)

        // They should be different cached instances even though they're for the same app
        // because getInstance() uses a special cache key "[DEFAULT]"
        assertThat(defaultInstance1).isNotEqualTo(defaultInstance2)
        assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(2)

        // But they should use the same underlying FirebaseApp
        assertThat(defaultInstance1.app).isEqualTo(defaultInstance2.app)
    }

    @Test
    fun `cache is properly isolated between different apps`() {
        // Create instances for different apps
        val instance1 = FirebaseAuthUI.getInstance()
        val instance2 = FirebaseAuthUI.getInstance(defaultApp)
        val instance3 = FirebaseAuthUI.getInstance(secondaryApp)

        // Verify all three instances are different
        assertThat(instance1).isNotEqualTo(instance2)
        assertThat(instance2).isNotEqualTo(instance3)
        assertThat(instance1).isNotEqualTo(instance3)

        // Verify cache size
        assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(3)

        // Clear cache
        FirebaseAuthUI.clearInstanceCache()
        assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(0)

        // Create new instances - should be different objects than before
        val newInstance1 = FirebaseAuthUI.getInstance()
        val newInstance2 = FirebaseAuthUI.getInstance(defaultApp)

        assertThat(newInstance1).isNotEqualTo(instance1)
        assertThat(newInstance2).isNotEqualTo(instance2)
    }

    // =============================================================================================
    // Thread Safety Tests
    // =============================================================================================

    @Test
    fun `getInstance() is thread-safe`() {
        val instances = mutableListOf<FirebaseAuthUI>()
        val threads = List(10) {
            Thread {
                instances.add(FirebaseAuthUI.getInstance())
            }
        }

        // Start all threads concurrently
        threads.forEach { it.start() }

        // Wait for all threads to complete
        threads.forEach { it.join() }

        // All instances should be the same (thread-safe singleton)
        val firstInstance = instances.first()
        instances.forEach { instance ->
            assertThat(instance).isEqualTo(firstInstance)
        }

        // Only one instance should be cached
        assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(1)
    }

    @Test
    fun `getInstance(app) is thread-safe`() {
        val instances = mutableListOf<FirebaseAuthUI>()
        val threads = List(10) {
            Thread {
                instances.add(FirebaseAuthUI.getInstance(secondaryApp))
            }
        }

        // Start all threads concurrently
        threads.forEach { it.start() }

        // Wait for all threads to complete
        threads.forEach { it.join() }

        // All instances should be the same (thread-safe singleton)
        val firstInstance = instances.first()
        instances.forEach { instance ->
            assertThat(instance).isEqualTo(firstInstance)
        }

        // Only one instance should be cached
        assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(1)
    }

    // =============================================================================================
    // Sign Out Tests
    // =============================================================================================

    @Test
    fun `signOut() successfully signs out user and updates state`() = runTest {
        // Setup mock auth
        val mockAuth = mock(FirebaseAuth::class.java)
        doNothing().`when`(mockAuth).signOut()

        // Create instance with mock auth
        val instance = FirebaseAuthUI.create(defaultApp, mockAuth)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Perform sign out
        instance.signOut(context)

        // Verify signOut was called on Firebase Auth
        verify(mockAuth).signOut()
    }

    @Test
    fun `signOut() handles Firebase exception and maps to AuthException`() = runTest {
        // Setup mock auth that throws exception
        val mockAuth = mock(FirebaseAuth::class.java)
        val runtimeException = RuntimeException("Network error")
        doThrow(runtimeException).`when`(mockAuth).signOut()

        // Create instance with mock auth
        val instance = FirebaseAuthUI.create(defaultApp, mockAuth)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Perform sign out and expect exception
        try {
            instance.signOut(context)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException) {
            assertThat(e).isInstanceOf(AuthException.UnknownException::class.java)
            assertThat(e.cause).isEqualTo(runtimeException)
        }
    }

    @Test
    fun `signOut() handles cancellation and maps to AuthCancelledException`() = runTest {
        // Setup mock auth
        val mockAuth = mock(FirebaseAuth::class.java)
        val cancellationException = CancellationException("Operation cancelled")
        doThrow(cancellationException).`when`(mockAuth).signOut()

        // Create instance with mock auth
        val instance = FirebaseAuthUI.create(defaultApp, mockAuth)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Perform sign out and expect cancellation exception
        try {
            instance.signOut(context)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.AuthCancelledException) {
            assertThat(e.message).contains("cancelled")
            assertThat(e.cause).isInstanceOf(CancellationException::class.java)
        }
    }

    // =============================================================================================
    // Delete Account Tests
    // =============================================================================================

    @Test
    fun `delete() successfully deletes user account and updates state`() = runTest {
        // Setup mock user and auth
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuth = mock(FirebaseAuth::class.java)
        val taskCompletionSource = TaskCompletionSource<Void>()
        taskCompletionSource.setResult(null) // Simulate successful deletion

        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.delete()).thenReturn(taskCompletionSource.task)

        // Create instance with mock auth
        val instance = FirebaseAuthUI.create(defaultApp, mockAuth)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Perform delete
        instance.delete(context)

        // Verify delete was called on user
        verify(mockUser).delete()
    }

    @Test
    fun `delete() throws UserNotFoundException when no user is signed in`() = runTest {
        // Setup mock auth with no current user
        val mockAuth = mock(FirebaseAuth::class.java)
        `when`(mockAuth.currentUser).thenReturn(null)

        // Create instance with mock auth
        val instance = FirebaseAuthUI.create(defaultApp, mockAuth)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Perform delete and expect exception
        try {
            instance.delete(context)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.UserNotFoundException) {
            assertThat(e.message).contains("No user is currently signed in")
        }
    }

    @Test
    fun `delete() handles recent login required exception`() = runTest {
        // Setup mock user and auth
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuth = mock(FirebaseAuth::class.java)
        val taskCompletionSource = TaskCompletionSource<Void>()
        val recentLoginException = FirebaseAuthRecentLoginRequiredException(
            "ERROR_REQUIRES_RECENT_LOGIN",
            "Recent login required"
        )
        taskCompletionSource.setException(recentLoginException)

        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.delete()).thenReturn(taskCompletionSource.task)

        // Create instance with mock auth
        val instance = FirebaseAuthUI.create(defaultApp, mockAuth)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Perform delete and expect mapped exception
        try {
            instance.delete(context)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.InvalidCredentialsException) {
            assertThat(e.message).contains("Recent login required")
            assertThat(e.cause).isEqualTo(recentLoginException)
        }
    }

    @Test
    fun `delete() handles cancellation and maps to AuthCancelledException`() = runTest {
        // Setup mock user and auth
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuth = mock(FirebaseAuth::class.java)
        val taskCompletionSource = TaskCompletionSource<Void>()
        val cancellationException = CancellationException("Operation cancelled")
        taskCompletionSource.setException(cancellationException)

        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.delete()).thenReturn(taskCompletionSource.task)

        // Create instance with mock auth
        val instance = FirebaseAuthUI.create(defaultApp, mockAuth)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Perform delete and expect cancellation exception
        try {
            instance.delete(context)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.AuthCancelledException) {
            assertThat(e.message).contains("cancelled")
            assertThat(e.cause).isInstanceOf(CancellationException::class.java)
        }
    }

    @Test
    fun `delete() handles Firebase network exception`() = runTest {
        // Setup mock user and auth
        val mockUser = mock(FirebaseUser::class.java)
        val mockAuth = mock(FirebaseAuth::class.java)
        val taskCompletionSource = TaskCompletionSource<Void>()
        val networkException = FirebaseException("Network error")
        taskCompletionSource.setException(networkException)

        `when`(mockAuth.currentUser).thenReturn(mockUser)
        `when`(mockUser.delete()).thenReturn(taskCompletionSource.task)

        // Create instance with mock auth
        val instance = FirebaseAuthUI.create(defaultApp, mockAuth)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Perform delete and expect mapped exception
        try {
            instance.delete(context)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: AuthException.NetworkException) {
            assertThat(e.message).contains("Network error")
            assertThat(e.cause).isEqualTo(networkException)
        }
    }

    // =============================================================================================
    // Email Provider Tests
    // =============================================================================================

    @Test
    fun `Create or link user with email and password without anonymous upgrade should succeed`() =
        runTest {
            val applicationContext = ApplicationProvider.getApplicationContext<Context>()
            val mockUser = mock(FirebaseUser::class.java)
            `when`(mockUser.email).thenReturn("test@example.com")
            `when`(mockFirebaseAuth.currentUser).thenReturn(mockUser)
            val taskCompletionSource = TaskCompletionSource<AuthResult>()
            taskCompletionSource.setResult(null)
            `when`(mockFirebaseAuth.createUserWithEmailAndPassword("test@example.com", "Pass@123"))
                .thenReturn(taskCompletionSource.task)

            val instance = FirebaseAuthUI.create(defaultApp, mockFirebaseAuth)
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
                config = config,
                provider = emailProvider,
                email = "test@example.com",
                password = "Pass@123"
            )

            verify(mockFirebaseAuth)
                .createUserWithEmailAndPassword("test@example.com", "Pass@123")

            val authState = instance.authStateFlow().first()
            assertThat(authState)
                .isEqualTo(AuthState.Success(result = null, user = mockUser))
            val successState = authState as AuthState.Success
            assertThat(successState.user.email).isEqualTo("test@example.com")
        }
}