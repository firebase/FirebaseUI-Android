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
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.ui.screens.FirebaseAuthScreen
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Activity that hosts the Firebase authentication flow UI.
 *
 * This activity displays the [FirebaseAuthScreen] composable and manages
 * the authentication flow lifecycle. It automatically finishes when the user
 * signs in successfully or cancels the flow.
 *
 * **Do not launch this Activity directly.**
 * Use [AuthFlowController] to start the auth flow:
 *
 * ```kotlin
 * val authUI = FirebaseAuthUI.getInstance()
 * val configuration = authUIConfiguration {
 *     providers = listOf(AuthProvider.Email(), AuthProvider.Google(...))
 * }
 * val controller = authUI.createAuthFlow(configuration)
 * val intent = controller.createIntent(context)
 * launcher.launch(intent)
 * ```
 *
 * **Result Codes:**
 * - [Activity.RESULT_OK] - User signed in successfully
 * - [Activity.RESULT_CANCELED] - User cancelled or error occurred
 *
 * **Result Data:**
 * - [EXTRA_USER_ID] - User ID string (when RESULT_OK)
 * - [EXTRA_IS_NEW_USER] - Boolean indicating if user is new (when RESULT_OK)
 * - [EXTRA_ERROR] - [AuthException] when an error occurs
 *
 * **Note:** To get the full user object after successful sign-in, use:
 * ```kotlin
 * FirebaseAuth.getInstance().currentUser
 * ```
 *
 * @see AuthFlowController
 * @see FirebaseAuthScreen
 * @since 10.0.0
 */
class FirebaseAuthActivity : ComponentActivity() {

    private lateinit var authUI: FirebaseAuthUI
    private lateinit var configuration: AuthUIConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract configuration from cache using UUID key
        val configKey = intent.getStringExtra(EXTRA_CONFIGURATION_KEY)
        configuration = if (configKey != null) {
            configurationCache.remove(configKey)
        } else {
            null
        } ?: run {
            // Missing configuration, finish with error
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        authUI = FirebaseAuthUI.getInstance()

        // Observe auth state to automatically finish when done
        lifecycleScope.launch {
            authUI.authStateFlow().collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        // User signed in successfully
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_USER_ID, state.user.uid)
                            putExtra(EXTRA_IS_NEW_USER, state.isNewUser)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                    is AuthState.Cancelled -> {
                        // User cancelled the flow
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                    is AuthState.Error -> {
                        // Error occurred, finish with error info
                        val resultIntent = Intent().apply {
                            putExtra(EXTRA_ERROR, state.exception)
                        }
                        setResult(RESULT_CANCELED, resultIntent)
                        // Don't finish on error, let user see error and retry
                    }
                    else -> {
                        // Other states, keep showing UI
                    }
                }
            }
        }

        // Set up Compose UI
        setContent {
            AuthUITheme(theme = configuration.theme) {
                FirebaseAuthScreen(
                    authUI = authUI,
                    configuration = configuration,
                    onSignInSuccess = { authResult ->
                        // State flow will handle finishing
                    },
                    onSignInFailure = { exception ->
                        // State flow will handle error
                    },
                    onSignInCancelled = {
                        authUI.updateAuthState(AuthState.Cancelled)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Reset auth state when activity is destroyed
        if (!isFinishing) {
            authUI.updateAuthState(AuthState.Idle)
        }
    }

    companion object {
        private const val EXTRA_CONFIGURATION_KEY = "com.firebase.ui.auth.compose.CONFIGURATION_KEY"

        /**
         * Intent extra key for user ID on successful sign-in.
         * Use [com.google.firebase.auth.FirebaseAuth.getInstance().currentUser] to get the full user object.
         */
        const val EXTRA_USER_ID = "com.firebase.ui.auth.compose.USER_ID"

        /**
         * Intent extra key for isNewUser flag on successful sign-in.
         */
        const val EXTRA_IS_NEW_USER = "com.firebase.ui.auth.compose.IS_NEW_USER"

        /**
         * Intent extra key for [AuthException] on error.
         */
        const val EXTRA_ERROR = "com.firebase.ui.auth.compose.ERROR"

        /**
         * Cache for configurations passed through Intents.
         * Uses UUID keys to avoid serialization issues with Context references.
         */
        private val configurationCache = ConcurrentHashMap<String, AuthUIConfiguration>()

        /**
         * Creates an Intent to launch the Firebase authentication flow.
         *
         * @param context Android [Context]
         * @param configuration [AuthUIConfiguration] defining the auth flow
         * @return Configured [Intent] to start [FirebaseAuthActivity]
         */
        internal fun createIntent(
            context: Context,
            configuration: AuthUIConfiguration
        ): Intent {
            val configKey = UUID.randomUUID().toString()
            configurationCache[configKey] = configuration

            return Intent(context, FirebaseAuthActivity::class.java).apply {
                putExtra(EXTRA_CONFIGURATION_KEY, configKey)
            }
        }
    }
}
