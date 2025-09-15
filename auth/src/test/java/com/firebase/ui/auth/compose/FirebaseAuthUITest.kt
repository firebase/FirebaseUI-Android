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

import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
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
    private lateinit var mockFirebaseApp: FirebaseApp

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockSecondaryApp: FirebaseApp

    @Mock
    private lateinit var mockSecondaryAuth: FirebaseAuth

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Clear the instance cache before each test to ensure test isolation
        FirebaseAuthUI.clearInstanceCache()

        // Setup mock app names
        `when`(mockFirebaseApp.name).thenReturn("[DEFAULT]")
        `when`(mockSecondaryApp.name).thenReturn("secondary")
    }

    @After
    fun tearDown() {
        // Clean up after each test to prevent test pollution
        FirebaseAuthUI.clearInstanceCache()
    }

    // =============================================================================================
    // Singleton Behavior Tests
    // =============================================================================================

    @Test
    fun `getInstance() returns same instance for default app`() {
        // Mock the static FirebaseApp.getInstance() method
        mockStatic(FirebaseApp::class.java).use { firebaseAppMock ->
            firebaseAppMock.`when`<FirebaseApp> { FirebaseApp.getInstance() }
                .thenReturn(mockFirebaseApp)

            // Mock Firebase.auth property
            mockStatic(Firebase::class.java).use { firebaseMock ->
                firebaseMock.`when`<FirebaseAuth> { Firebase.auth }
                    .thenReturn(mockFirebaseAuth)

                // Get instance twice
                val instance1 = FirebaseAuthUI.getInstance()
                val instance2 = FirebaseAuthUI.getInstance()

                // Verify they are the same instance (singleton pattern)
                assertThat(instance1).isSameInstanceAs(instance2)
                assertThat(instance1.app).isSameInstanceAs(mockFirebaseApp)
                assertThat(instance1.auth).isSameInstanceAs(mockFirebaseAuth)

                // Verify only one instance is cached
                assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(1)
            }
        }
    }

    @Test
    fun `getInstance() throws descriptive exception when Firebase not initialized`() {
        mockStatic(FirebaseApp::class.java).use { firebaseAppMock ->
            firebaseAppMock.`when`<FirebaseApp> { FirebaseApp.getInstance() }
                .thenThrow(IllegalStateException("Firebase not initialized"))

            // Verify exception is thrown with helpful message
            try {
                FirebaseAuthUI.getInstance()
                // Should not reach here
                assertThat(false).isTrue()
            } catch (e: IllegalStateException) {
                assertThat(e.message).contains("Default FirebaseApp is not initialized")
                assertThat(e.message).contains("FirebaseApp.initializeApp(Context)")
                assertThat(e.cause).isNotNull()
            }
        }
    }

    // =============================================================================================
    // Multi-App Support Tests
    // =============================================================================================

    @Test
    fun `getInstance(app) returns distinct instances per FirebaseApp`() {
        mockStatic(Firebase::class.java).use { firebaseMock ->
            // Setup different auth instances for different apps
            firebaseMock.`when`<FirebaseAuth> {
                Firebase.auth(mockFirebaseApp)
            }.thenReturn(mockFirebaseAuth)

            firebaseMock.`when`<FirebaseAuth> {
                Firebase.auth(mockSecondaryApp)
            }.thenReturn(mockSecondaryAuth)

            // Get instances for different apps
            val defaultInstance = FirebaseAuthUI.getInstance(mockFirebaseApp)
            val secondaryInstance = FirebaseAuthUI.getInstance(mockSecondaryApp)

            // Verify they are different instances
            assertThat(defaultInstance).isNotSameInstanceAs(secondaryInstance)

            // Verify correct apps and auth instances are used
            assertThat(defaultInstance.app).isSameInstanceAs(mockFirebaseApp)
            assertThat(defaultInstance.auth).isSameInstanceAs(mockFirebaseAuth)
            assertThat(secondaryInstance.app).isSameInstanceAs(mockSecondaryApp)
            assertThat(secondaryInstance.auth).isSameInstanceAs(mockSecondaryAuth)

            // Verify both instances are cached
            assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(2)
        }
    }

    @Test
    fun `getInstance(app) returns same instance for same app`() {
        mockStatic(Firebase::class.java).use { firebaseMock ->
            firebaseMock.`when`<FirebaseAuth> {
                Firebase.auth(mockFirebaseApp)
            }.thenReturn(mockFirebaseAuth)

            // Get instance twice for the same app
            val instance1 = FirebaseAuthUI.getInstance(mockFirebaseApp)
            val instance2 = FirebaseAuthUI.getInstance(mockFirebaseApp)

            // Verify they are the same instance (caching works)
            assertThat(instance1).isSameInstanceAs(instance2)
            assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(1)
        }
    }

    // =============================================================================================
    // Custom Auth Injection Tests
    // =============================================================================================

    @Test
    fun `create() returns new instance with provided dependencies`() {
        // Create instances with custom auth
        val instance1 = FirebaseAuthUI.create(mockFirebaseApp, mockFirebaseAuth)
        val instance2 = FirebaseAuthUI.create(mockFirebaseApp, mockFirebaseAuth)

        // Verify they are different instances (no caching)
        assertThat(instance1).isNotSameInstanceAs(instance2)

        // Verify correct dependencies are used
        assertThat(instance1.app).isSameInstanceAs(mockFirebaseApp)
        assertThat(instance1.auth).isSameInstanceAs(mockFirebaseAuth)
        assertThat(instance2.app).isSameInstanceAs(mockFirebaseApp)
        assertThat(instance2.auth).isSameInstanceAs(mockFirebaseAuth)

        // Verify cache is not used for create()
        assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(0)
    }

    @Test
    fun `create() allows custom auth injection for multi-tenancy`() {
        // Create mock custom auth with tenant
        val customAuth = mock(FirebaseAuth::class.java)
        `when`(customAuth.tenantId).thenReturn("customer-tenant-123")

        // Create instance with custom auth
        val instance = FirebaseAuthUI.create(mockFirebaseApp, customAuth)

        // Verify custom auth is used
        assertThat(instance.auth).isSameInstanceAs(customAuth)
        assertThat(instance.auth.tenantId).isEqualTo("customer-tenant-123")
    }

    // =============================================================================================
    // Cache Isolation Tests
    // =============================================================================================

    @Test
    fun `getInstance() and getInstance(app) use separate cache entries`() {
        mockStatic(FirebaseApp::class.java).use { firebaseAppMock ->
            firebaseAppMock.`when`<FirebaseApp> { FirebaseApp.getInstance() }
                .thenReturn(mockFirebaseApp)

            mockStatic(Firebase::class.java).use { firebaseMock ->
                firebaseMock.`when`<FirebaseAuth> { Firebase.auth }
                    .thenReturn(mockFirebaseAuth)
                firebaseMock.`when`<FirebaseAuth> {
                    Firebase.auth(mockFirebaseApp)
                }.thenReturn(mockFirebaseAuth)

                // Get default instance via getInstance()
                val defaultInstance1 = FirebaseAuthUI.getInstance()

                // Get instance for default app via getInstance(app)
                val defaultInstance2 = FirebaseAuthUI.getInstance(mockFirebaseApp)

                // They should be different cached instances even though they're for the same app
                // because getInstance() uses a special cache key "[DEFAULT]"
                assertThat(defaultInstance1).isNotSameInstanceAs(defaultInstance2)
                assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(2)
            }
        }
    }

    // =============================================================================================
    // Thread Safety Tests
    // =============================================================================================

    @Test
    fun `getInstance() is thread-safe`() {
        mockStatic(FirebaseApp::class.java).use { firebaseAppMock ->
            firebaseAppMock.`when`<FirebaseApp> { FirebaseApp.getInstance() }
                .thenReturn(mockFirebaseApp)

            mockStatic(Firebase::class.java).use { firebaseMock ->
                firebaseMock.`when`<FirebaseAuth> { Firebase.auth }
                    .thenReturn(mockFirebaseAuth)

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
                    assertThat(instance).isSameInstanceAs(firstInstance)
                }

                // Only one instance should be cached
                assertThat(FirebaseAuthUI.getCacheSize()).isEqualTo(1)
            }
        }
    }
}