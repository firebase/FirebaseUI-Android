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

import androidx.annotation.RestrictTo
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.ConcurrentHashMap

/**
 * The central class that coordinates all authentication operations for Firebase Auth UI Compose.
 * This class manages UI state and provides methods for signing in, signing up, and managing
 * user accounts.
 *
 * <h2>Usage</h2>
 *
 * **Default app instance:**
 * ```kotlin
 * val authUI = FirebaseAuthUI.getInstance()
 * ```
 *
 * **Custom app instance:**
 * ```kotlin
 * val customApp = Firebase.app("secondary")
 * val authUI = FirebaseAuthUI.getInstance(customApp)
 * ```
 *
 * **Multi-tenancy with custom auth:**
 * ```kotlin
 * val customAuth = Firebase.auth(customApp).apply {
 *     tenantId = "my-tenant-id"
 * }
 * val authUI = FirebaseAuthUI.create(customApp, customAuth)
 * ```
 *
 * @property app The [FirebaseApp] instance used for authentication
 * @property auth The [FirebaseAuth] instance used for authentication operations
 *
 * @since 10.0.0
 */
class FirebaseAuthUI private constructor(
    val app: FirebaseApp,
    val auth: FirebaseAuth
) {
    companion object {
        /** Cache for singleton instances per FirebaseApp. Thread-safe via ConcurrentHashMap. */
        private val instanceCache = ConcurrentHashMap<String, FirebaseAuthUI>()

        /** Special key for the default app instance to distinguish from named instances. */
        private const val DEFAULT_APP_KEY = "[DEFAULT]"

        /**
         * Returns a cached singleton instance for the default Firebase app.
         *
         * This method ensures that the same instance is returned for the default app across the
         * entire application lifecycle. The instance is lazily created on first access and cached
         * for subsequent calls.
         *
         * **Example:**
         * ```kotlin
         * val authUI = FirebaseAuthUI.getInstance()
         * val user = authUI.auth.currentUser
         * ```
         *
         * @return The cached [FirebaseAuthUI] instance for the default app
         * @throws IllegalStateException if Firebase has not been initialized. Call
         *         `FirebaseApp.initializeApp(Context)` before using this method.
         */
        @JvmStatic
        fun getInstance(): FirebaseAuthUI {
            val defaultApp = try {
                FirebaseApp.getInstance()
            } catch (e: IllegalStateException) {
                throw IllegalStateException(
                    "Default FirebaseApp is not initialized. " +
                    "Make sure to call FirebaseApp.initializeApp(Context) first.",
                    e
                )
            }

            return instanceCache.getOrPut(DEFAULT_APP_KEY) {
                FirebaseAuthUI(defaultApp, Firebase.auth)
            }
        }

        /**
         * Returns a cached instance for a specific Firebase app.
         *
         * Each [FirebaseApp] gets its own distinct instance that is cached for subsequent calls
         * with the same app. This allows for multiple Firebase projects to be used within the
         * same application.
         *
         * **Example:**
         * ```kotlin
         * val secondaryApp = Firebase.app("secondary")
         * val authUI = FirebaseAuthUI.getInstance(secondaryApp)
         * ```
         *
         * @param app The [FirebaseApp] instance to use
         * @return The cached [FirebaseAuthUI] instance for the specified app
         */
        @JvmStatic
        fun getInstance(app: FirebaseApp): FirebaseAuthUI {
            val cacheKey = app.name
            return instanceCache.getOrPut(cacheKey) {
                FirebaseAuthUI(app, Firebase.auth(app))
            }
        }

        /**
         * Creates a new instance with custom configuration, useful for multi-tenancy.
         *
         * This method always returns a new instance and does **not** use caching, allowing for
         * custom [FirebaseAuth] configurations such as tenant IDs or custom authentication states.
         * Use this when you need fine-grained control over the authentication instance.
         *
         * **Example - Multi-tenancy:**
         * ```kotlin
         * val app = Firebase.app("tenant-app")
         * val auth = Firebase.auth(app).apply {
         *     tenantId = "customer-tenant-123"
         * }
         * val authUI = FirebaseAuthUI.create(app, auth)
         * ```
         *
         * @param app The [FirebaseApp] instance to use
         * @param auth The [FirebaseAuth] instance with custom configuration
         * @return A new [FirebaseAuthUI] instance with the provided dependencies
         */
        @JvmStatic
        fun create(app: FirebaseApp, auth: FirebaseAuth): FirebaseAuthUI {
            return FirebaseAuthUI(app, auth)
        }

        /**
         * Clears all cached instances. This method is intended for testing purposes only.
         *
         * @suppress This is an internal API and should not be used in production code.
         * @RestrictTo RestrictTo.Scope.TESTS
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.TESTS)
        internal fun clearInstanceCache() {
            instanceCache.clear()
        }

        /**
         * Returns the current number of cached instances. This method is intended for testing
         * purposes only.
         *
         * @return The number of cached [FirebaseAuthUI] instances
         * @suppress This is an internal API and should not be used in production code.
         * @RestrictTo RestrictTo.Scope.TESTS
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.TESTS)
        internal fun getCacheSize(): Int {
            return instanceCache.size
        }
    }
}