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
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.util.EmailLinkPersistenceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Creates a remembered launcher function for Facebook sign-in.
 *
 * Returns a launcher function that initiates the Facebook sign-in flow. Automatically handles
 * profile data fetching, Firebase credential creation, anonymous account upgrades, and account
 * linking when an email collision occurs.
 *
 * @param context Android context for DataStore access when saving credentials for linking
 * @param config The [AuthUIConfiguration] containing authentication settings
 * @param provider The [AuthProvider.Facebook] configuration with scopes and credential provider
 *
 * @return A launcher function that starts the Facebook sign-in flow when invoked
 *
 * @see signInWithFacebook
 */
@Composable
internal fun FirebaseAuthUI.rememberSignInWithFacebookLauncher(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider.Facebook,
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    val callbackManager = remember { CallbackManager.Factory.create() }
    val loginManager = LoginManager.getInstance()

    val launcher = rememberLauncherForActivityResult(
        loginManager.createLogInActivityResultContract(
            callbackManager,
            null
        ),
        onResult = {},
    )

    DisposableEffect(Unit) {
        loginManager.registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    coroutineScope.launch {
                        try {
                            signInWithFacebook(
                                context = context,
                                config = config,
                                provider = provider,
                                accessToken = result.accessToken,
                            )
                        } catch (e: AuthException) {
                            // Already an AuthException, don't re-wrap it
                            updateAuthState(AuthState.Error(e))
                        } catch (e: Exception) {
                            val authException = AuthException.from(e)
                            updateAuthState(AuthState.Error(authException))
                        }
                    }
                }

                override fun onCancel() {
                    updateAuthState(AuthState.Idle)
                }

                override fun onError(error: FacebookException) {
                    Log.e("FacebookAuthProvider", "Error during Facebook sign in", error)
                    val authException = AuthException.from(error)
                    updateAuthState(
                        AuthState.Error(
                            authException
                        )
                    )
                }
            })

        onDispose { loginManager.unregisterCallback(callbackManager) }
    }

    return {
        updateAuthState(
            AuthState.Loading("Signing in with facebook...")
        )
        launcher.launch(provider.scopes)
    }
}

/**
 * Signs in a user with Facebook by converting a Facebook access token to a Firebase credential.
 *
 * Fetches user profile data from Facebook Graph API, creates a Firebase credential, and signs in
 * or upgrades an anonymous account. Handles account collisions by saving the Facebook credential
 * for linking and throwing [AuthException.AccountLinkingRequiredException].
 *
 * @param context Android context for DataStore access when saving credentials for linking
 * @param config The [AuthUIConfiguration] containing authentication settings
 * @param provider The [AuthProvider.Facebook] configuration
 * @param accessToken The Facebook [AccessToken] from successful login
 * @param credentialProvider Creates Firebase credentials from Facebook tokens
 *
 * @throws AuthException.AccountLinkingRequiredException if an account exists with the same email
 * @throws AuthException.AuthCancelledException if the coroutine is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 * @throws AuthException.InvalidCredentialsException if the Facebook token is invalid
 *
 * @see rememberSignInWithFacebookLauncher
 * @see signInAndLinkWithCredential
 */
internal suspend fun FirebaseAuthUI.signInWithFacebook(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider.Facebook,
    accessToken: AccessToken,
    credentialProvider: AuthProvider.Facebook.CredentialProvider = AuthProvider.Facebook.DefaultCredentialProvider(),
) {
    try {
        updateAuthState(
            AuthState.Loading("Signing in with facebook...")
        )
        val profileData = provider.fetchFacebookProfile(accessToken)
        val credential = credentialProvider.getCredential(accessToken.token)
        signInAndLinkWithCredential(
            config = config,
            credential = credential,
            provider = provider,
            displayName = profileData?.displayName,
            photoUrl = profileData?.photoUrl,
        )
    } catch (e: AuthException.AccountLinkingRequiredException) {
        // Account collision occurred - save Facebook credential for linking after email link sign-in
        // This happens when a user tries to sign in with Facebook but an email link account exists
        EmailLinkPersistenceManager.saveCredentialForLinking(
            context = context,
            providerType = provider.providerId,
            idToken = null,
            accessToken = accessToken.token
        )

        // Re-throw to let UI handle the account linking flow
        updateAuthState(AuthState.Error(e))
        throw e
    } catch (e: FacebookException) {
        val authException = AuthException.from(e)
        updateAuthState(AuthState.Error(authException))
        throw authException
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in with facebook was cancelled",
            cause = e
        )
        updateAuthState(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        updateAuthState(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e)
        updateAuthState(AuthState.Error(authException))
        throw authException
    }
}

