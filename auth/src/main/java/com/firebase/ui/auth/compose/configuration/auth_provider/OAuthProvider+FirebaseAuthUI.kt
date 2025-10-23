package com.firebase.ui.auth.compose.configuration.auth_provider

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider.Companion.canUpgradeAnonymous
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Creates a Composable handler for OAuth provider sign-in.
 *
 * This function creates a remember-scoped sign-in handler that can be invoked
 * from button clicks or other UI events. It automatically handles:
 * - Activity retrieval from LocalActivity
 * - Coroutine scope management
 * - Error handling and state updates
 *
 * **Usage:**
 * ```kotlin
 * val onSignInWithGitHub = authUI.rememberOAuthSignInHandler(
 *     context = context,
 *     config = configuration,
 *     provider = githubProvider
 * )
 *
 * Button(onClick = onSignInWithGitHub) {
 *     Text("Sign in with GitHub")
 * }
 * ```
 *
 * @param context Android context
 * @param config Authentication UI configuration
 * @param provider OAuth provider configuration
 *
 * @return Lambda that triggers OAuth sign-in when invoked
 *
 * @throws IllegalStateException if LocalActivity.current is null
 *
 * @see signInWithProvider
 */
@Composable
fun FirebaseAuthUI.rememberOAuthSignInHandler(
    context: Context,
    activity: Activity?,
    config: AuthUIConfiguration,
    provider: AuthProvider.OAuth,
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    activity ?: throw IllegalStateException(
        "OAuth sign-in requires an Activity. " +
                "Ensure FirebaseAuthScreen is used within an Activity."
    )

    return remember(this, provider.providerId) {
        {
            coroutineScope.launch {
                try {
                    signInWithProvider(
                        context = context,
                        config = config,
                        activity = activity,
                        provider = provider
                    )
                } catch (e: AuthException) {
                    // Log.e("OAuthSignIn", "OAuth sign-in failed for ${provider.providerName}", e)
                    updateAuthState(AuthState.Error(e))
                } catch (e: Exception) {
                    val authException = AuthException.from(e)
                    // Log.e("OAuthSignIn", "OAuth sign-in failed for ${provider.providerName}", e)
                    updateAuthState(AuthState.Error(authException))
                }
            }
        }
    }
}

/**
 * Signs in with an OAuth provider (GitHub, Microsoft, Yahoo, Apple, Twitter).
 *
 * This function implements OAuth provider authentication using Firebase's native OAuthProvider.
 * It handles both normal sign-in flow and anonymous user upgrade flow.
 *
 * **Supported Providers:**
 * - GitHub (github.com)
 * - Microsoft (microsoft.com)
 * - Yahoo (yahoo.com)
 * - Apple (apple.com)
 * - Twitter (twitter.com)
 *
 * **Flow:**
 * 1. Checks for pending auth results (e.g., from app restart during OAuth flow)
 * 2. If anonymous upgrade is enabled and user is anonymous, links credential to anonymous account
 * 3. Otherwise, performs normal sign-in
 * 4. Updates auth state to Idle on success
 *
 * **Anonymous Upgrade:**
 * If [AuthUIConfiguration.isAnonymousUpgradeEnabled] is true and a user is currently signed in
 * anonymously, this will attempt to link the OAuth credential to the anonymous account instead
 * of creating a new account.
 *
 * **Error Handling:**
 * - [AuthException.AuthCancelledException]: User cancelled OAuth flow
 * - [AuthException.AccountLinkingRequiredException]: Account collision (email already exists)
 * - [AuthException]: Other authentication errors
 *
 * @param context Android context
 * @param config Authentication UI configuration
 * @param activity Activity for OAuth flow
 * @param provider OAuth provider configuration with scopes and custom parameters
 *
 * @throws AuthException.AuthCancelledException if user cancels
 * @throws AuthException.AccountLinkingRequiredException if account collision occurs
 * @throws AuthException if OAuth flow or sign-in fails
 *
 * @see AuthProvider.OAuth
 * @see signInAndLinkWithCredential
 */
internal suspend fun FirebaseAuthUI.signInWithProvider(
    context: Context,
    config: AuthUIConfiguration,
    activity: Activity,
    provider: AuthProvider.OAuth,
) {
    try {
        updateAuthState(AuthState.Loading("Signing in with ${provider.providerName}..."))

        // Build OAuth provider with scopes and custom parameters
        val oauthProvider = OAuthProvider
            .newBuilder(provider.providerId)
            .apply {
                // Add scopes if provided
                if (provider.scopes.isNotEmpty()) {
                    scopes = provider.scopes
                }
                // Add custom parameters if provided
                provider.customParameters.forEach { (key, value) ->
                    addCustomParameter(key, value)
                }
            }
            .build()

        // Check for pending auth result (e.g., app was killed during OAuth flow)
        val pendingResult = auth.pendingAuthResult
        if (pendingResult != null) {
            // Log.d("OAuthSignIn", "Found pending auth result, completing sign-in")
            val authResult = pendingResult.await()
            val credential = authResult.credential as? OAuthCredential

            if (credential != null) {
                // Complete the pending sign-in/link flow
                signInAndLinkWithCredential(
                    config = config,
                    credential = credential,
                    provider = provider,
                    displayName = authResult.user?.displayName,
                    photoUrl = authResult.user?.photoUrl,
                )
            }
            updateAuthState(AuthState.Idle)
            return
        }

        // Determine if we should upgrade anonymous user or do normal sign-in
        val authResult = if (canUpgradeAnonymous(config, auth)) {
            // Log.d("OAuthSignIn", "Upgrading anonymous user with ${provider.providerName}")
            auth.currentUser?.startActivityForLinkWithProvider(activity, oauthProvider)?.await()
        } else {
            // Log.d("OAuthSignIn", "Normal sign-in with ${provider.providerName}")
            auth.startActivityForSignInWithProvider(activity, oauthProvider).await()
        }

        // Extract OAuth credential and complete sign-in
        val credential = authResult?.credential as? OAuthCredential
        if (credential != null) {
            // Log.d(
            //     "OAuthSignIn",
            //     "Successfully obtained OAuth credential for ${provider.providerName}"
            // )
            // The user is already signed in via startActivityForSignInWithProvider/startActivityForLinkWithProvider
            // Just update state to Idle
            updateAuthState(AuthState.Idle)
        } else {
            throw AuthException.UnknownException(
                message = "OAuth sign-in did not return a valid credential"
            )
        }

    } catch (e: FirebaseAuthUserCollisionException) {
        // Account collision: account already exists with different sign-in method
        val email = e.email
        val credential = e.updatedCredential

        val accountLinkingException = AuthException.AccountLinkingRequiredException(
            message = "An account already exists with the email ${email ?: ""}. " +
                    "Please sign in with your existing account to link " +
                    "your ${provider.providerName} account.",
            email = email,
            credential = credential,
            cause = e
        )
        updateAuthState(AuthState.Error(accountLinkingException))
        throw accountLinkingException
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Signing in with ${provider.providerName} was cancelled",
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
