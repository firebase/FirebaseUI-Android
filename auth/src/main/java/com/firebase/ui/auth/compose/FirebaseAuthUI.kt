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
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
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

    private val _authStateFlow = MutableStateFlow<AuthState>(AuthState.Idle)

    /**
     * Checks whether a user is currently signed in.
     *
     * This method directly mirrors the state of [FirebaseAuth] and returns true if there is
     * a currently signed-in user, false otherwise.
     *
     * **Example:**
     * ```kotlin
     * val authUI = FirebaseAuthUI.getInstance()
     * if (authUI.isSignedIn()) {
     *     // User is signed in
     *     navigateToHome()
     * } else {
     *     // User is not signed in
     *     navigateToLogin()
     * }
     * ```
     *
     * @return `true` if a user is signed in, `false` otherwise
     */
    fun isSignedIn(): Boolean = auth.currentUser != null

    /**
     * Returns the currently signed-in user, or null if no user is signed in.
     *
     * This method returns the same value as [FirebaseAuth.currentUser] and provides
     * direct access to the current user object.
     *
     * **Example:**
     * ```kotlin
     * val authUI = FirebaseAuthUI.getInstance()
     * val user = authUI.getCurrentUser()
     * user?.let {
     *     println("User email: ${it.email}")
     *     println("User ID: ${it.uid}")
     * }
     * ```
     *
     * @return The currently signed-in [FirebaseUser], or `null` if no user is signed in
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Returns a [Flow] that emits [AuthState] changes.
     *
     * This flow observes changes to the authentication state and emits appropriate
     * [AuthState] objects. The flow will emit:
     * - [AuthState.Idle] when there's no active authentication operation
     * - [AuthState.Loading] during authentication operations
     * - [AuthState.Success] when a user successfully signs in
     * - [AuthState.Error] when an authentication error occurs
     * - [AuthState.Cancelled] when authentication is cancelled
     * - [AuthState.RequiresMfa] when multi-factor authentication is needed
     * - [AuthState.RequiresEmailVerification] when email verification is needed
     *
     * The flow automatically emits [AuthState.Success] or [AuthState.Idle] based on
     * the current authentication state when collection starts.
     *
     * **Example:**
     * ```kotlin
     * val authUI = FirebaseAuthUI.getInstance()
     *
     * lifecycleScope.launch {
     *     authUI.authStateFlow().collect { state ->
     *         when (state) {
     *             is AuthState.Success -> {
     *                 // User is signed in
     *                 updateUI(state.user)
     *             }
     *             is AuthState.Error -> {
     *                 // Handle error
     *                 showError(state.exception.message)
     *             }
     *             is AuthState.Loading -> {
     *                 // Show loading indicator
     *                 showProgressBar()
     *             }
     *             // ... handle other states
     *         }
     *     }
     * }
     * ```
     *
     * @return A [Flow] of [AuthState] that emits authentication state changes
     */
    fun authStateFlow(): Flow<AuthState> = callbackFlow {
        // Set initial state based on current auth state
        val initialState = auth.currentUser?.let { user ->
            AuthState.Success(result = null, user = user, isNewUser = false)
        } ?: AuthState.Idle

        trySend(initialState)

        // Create auth state listener
        val authStateListener = AuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            val state = if (currentUser != null) {
                // Check if email verification is required
                if (!currentUser.isEmailVerified &&
                    currentUser.email != null &&
                    currentUser.providerData.any { it.providerId == "password" }) {
                    AuthState.RequiresEmailVerification(
                        user = currentUser,
                        email = currentUser.email!!
                    )
                } else {
                    AuthState.Success(
                        result = null,
                        user = currentUser,
                        isNewUser = false
                    )
                }
            } else {
                AuthState.Idle
            }
            trySend(state)
        }

        // Add listener
        auth.addAuthStateListener(authStateListener)

        // Also observe internal state changes
        _authStateFlow.value.let { currentState ->
            if (currentState !is AuthState.Idle && currentState !is AuthState.Success) {
                trySend(currentState)
            }
        }

        // Remove listener when flow collection is cancelled
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    /**
     * Updates the internal authentication state.
     * This method is intended for internal use by authentication operations.
     *
     * @param state The new [AuthState] to emit
     * @suppress This is an internal API
     */
    internal fun updateAuthState(state: AuthState) {
        _authStateFlow.value = state
    }

    companion object {
        /** Cache for singleton instances per FirebaseApp. Thread-safe via ConcurrentHashMap. */
        private val instanceCache = ConcurrentHashMap<String, FirebaseAuthUI>()

        /** Special key for the default app instance to distinguish from named instances. */
        private const val DEFAULT_APP_KEY = "__FIREBASE_UI_DEFAULT__"

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