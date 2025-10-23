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

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Controller for managing the Firebase authentication flow lifecycle.
 *
 * This controller provides a lifecycle-safe way to start, monitor, and cancel
 * the authentication flow. It handles coroutine lifecycle, state listeners,
 * and resource cleanup automatically.
 *
 * **Usage Pattern:**
 * ```kotlin
 * class MyActivity : ComponentActivity() {
 *     private lateinit var authController: AuthFlowController
 *
 *     private val authLauncher = registerForActivityResult(
 *         ActivityResultContracts.StartActivityForResult()
 *     ) { result ->
 *         // Auth flow completed
 *     }
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         val authUI = FirebaseAuthUI.getInstance()
 *         val configuration = authUIConfiguration {
 *             providers = listOf(
 *                 AuthProvider.Email(),
 *                 AuthProvider.Google(...)
 *             )
 *         }
 *
 *         authController = authUI.createAuthFlow(configuration)
 *
 *         // Observe auth state
 *         lifecycleScope.launch {
 *             authController.authStateFlow.collect { state ->
 *                 when (state) {
 *                     is AuthState.Success -> {
 *                         // User signed in successfully
 *                         val user = state.user
 *                     }
 *                     is AuthState.Error -> {
 *                         // Handle error
 *                     }
 *                     is AuthState.Cancelled -> {
 *                         // User cancelled
 *                     }
 *                     else -> {}
 *                 }
 *             }
 *         }
 *
 *         // Start auth flow
 *         val intent = authController.createIntent(this)
 *         authLauncher.launch(intent)
 *     }
 *
 *     override fun onDestroy() {
 *         super.onDestroy()
 *         authController.dispose()
 *     }
 * }
 * ```
 *
 * **Lifecycle Management:**
 * - [createIntent] - Generate Intent to start the auth flow Activity
 * - [start] - Alternative to launch the flow (for Activity context)
 * - [cancel] - Cancel the ongoing auth flow, transitions to [AuthState.Cancelled]
 * - [dispose] - Release all resources (coroutines, listeners). Call in onDestroy()
 *
 * @property authUI The [FirebaseAuthUI] instance managing authentication
 * @property configuration The [AuthUIConfiguration] defining the auth flow behavior
 *
 * @since 10.0.0
 */
class AuthFlowController internal constructor(
    private val authUI: FirebaseAuthUI,
    private val configuration: AuthUIConfiguration
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val isDisposed = AtomicBoolean(false)
    private var stateCollectionJob: Job? = null

    /**
     * Flow of [AuthState] changes during the authentication flow.
     *
     * Subscribe to this flow to observe authentication state changes.
     * The flow is backed by the [FirebaseAuthUI.authStateFlow] and will
     * emit states like:
     * - [AuthState.Idle] - No active authentication
     * - [AuthState.Loading] - Authentication in progress
     * - [AuthState.Success] - User signed in successfully
     * - [AuthState.Error] - Authentication error occurred
     * - [AuthState.Cancelled] - User cancelled the flow
     * - [AuthState.RequiresMfa] - Multi-factor authentication required
     * - [AuthState.RequiresEmailVerification] - Email verification required
     */
    val authStateFlow: Flow<AuthState>
        get() {
            checkNotDisposed()
            return authUI.authStateFlow()
        }

    /**
     * Creates an Intent to launch the Firebase authentication flow.
     *
     * Use this method with [ActivityResultLauncher] to start the auth flow
     * and handle the result in a lifecycle-aware manner.
     *
     * **Example:**
     * ```kotlin
     * val authLauncher = registerForActivityResult(
     *     ActivityResultContracts.StartActivityForResult()
     * ) { result ->
     *     if (result.resultCode == Activity.RESULT_OK) {
     *         // Auth flow completed successfully
     *     } else {
     *         // Auth flow cancelled or error
     *     }
     * }
     *
     * val intent = authController.createIntent(this)
     * authLauncher.launch(intent)
     * ```
     *
     * @param context Android [Context] to create the Intent
     * @return [Intent] configured to launch the auth flow Activity
     * @throws IllegalStateException if the controller has been disposed
     */
    fun createIntent(context: Context): Intent {
        checkNotDisposed()
        return FirebaseAuthActivity.createIntent(context, configuration)
    }

    /**
     * Starts the Firebase authentication flow.
     *
     * This method launches the auth flow Activity from the provided [Activity] context.
     * For better lifecycle management, prefer using [createIntent] with
     * [ActivityResultLauncher] instead.
     *
     * **Note:** This method uses [Activity.startActivityForResult] which is deprecated.
     * Consider using [createIntent] with the Activity Result API instead.
     *
     * @param activity The [Activity] to launch from
     * @param requestCode Request code for [Activity.onActivityResult]
     * @throws IllegalStateException if the controller has been disposed
     *
     * @see createIntent
     */
    @Deprecated(
        message = "Use createIntent() with ActivityResultLauncher instead",
        replaceWith = ReplaceWith("createIntent(activity)"),
        level = DeprecationLevel.WARNING
    )
    fun start(activity: Activity, requestCode: Int = RC_SIGN_IN) {
        checkNotDisposed()
        val intent = createIntent(activity)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Cancels the ongoing authentication flow.
     *
     * This method transitions the auth state to [AuthState.Cancelled] and
     * signals the auth flow to terminate. The auth flow Activity will finish
     * and return [Activity.RESULT_CANCELED].
     *
     * **Example:**
     * ```kotlin
     * // User clicked a "Cancel" button
     * cancelButton.setOnClickListener {
     *     authController.cancel()
     * }
     * ```
     *
     * @throws IllegalStateException if the controller has been disposed
     */
    fun cancel() {
        checkNotDisposed()
        authUI.updateAuthState(AuthState.Cancelled)
    }

    /**
     * Disposes the controller and releases all resources.
     *
     * This method:
     * - Cancels all coroutines in the controller scope
     * - Stops listening to auth state changes
     * - Marks the controller as disposed
     *
     * Call this method in your Activity's `onDestroy()` to prevent memory leaks.
     *
     * **Important:** Once disposed, this controller cannot be reused. Create a new
     * controller if you need to start another auth flow.
     *
     * **Example:**
     * ```kotlin
     * override fun onDestroy() {
     *     super.onDestroy()
     *     authController.dispose()
     * }
     * ```
     *
     * @throws IllegalStateException if already disposed (when called multiple times)
     */
    fun dispose() {
        if (isDisposed.compareAndSet(false, true)) {
            stateCollectionJob?.cancel()
            coroutineScope.cancel()
        }
    }

    /**
     * Checks if the controller has been disposed.
     *
     * @return `true` if disposed, `false` otherwise
     */
    fun isDisposed(): Boolean = isDisposed.get()

    private fun checkNotDisposed() {
        check(!isDisposed.get()) {
            "AuthFlowController has been disposed. Create a new controller to start another auth flow."
        }
    }

    internal fun startStateCollection() {
        if (stateCollectionJob == null || stateCollectionJob?.isActive == false) {
            stateCollectionJob = authUI.authStateFlow()
                .onEach { state ->
                    // Optional: Add logging or side effects here
                }
                .launchIn(coroutineScope)
        }
    }

    companion object {
        /**
         * Request code for the sign-in activity result.
         *
         * Use this constant when calling [start] with `startActivityForResult`.
         */
        const val RC_SIGN_IN = 9001
    }
}
