package com.firebase.ui.auth.compose.configuration.auth_provider

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Creates a remembered callback for Google Sign-In that can be invoked from UI components.
 *
 * This Composable function returns a lambda that, when invoked, initiates the Google Sign-In
 * flow using [signInWithGoogle]. The callback is stable across recompositions and automatically
 * handles coroutine scoping and error state management.
 *
 * **Usage:**
 * ```kotlin
 * val onSignInWithGoogle = authUI.rememberGoogleSignInHandler(
 *     context = context,
 *     config = configuration,
 *     provider = googleProvider
 * )
 *
 * Button(onClick = onSignInWithGoogle) {
 *     Text("Sign in with Google")
 * }
 * ```
 *
 * **Error Handling:**
 * - Catches all exceptions and converts them to [AuthException]
 * - Automatically updates [AuthState.Error] on failures
 * - Logs errors for debugging purposes
 *
 * @param context Android context for Credential Manager
 * @param config Authentication UI configuration
 * @param provider Google provider configuration with server client ID and optional scopes
 * @return A callback function that initiates Google Sign-In when invoked
 *
 * @see signInWithGoogle
 * @see AuthProvider.Google
 */
@Composable
internal fun FirebaseAuthUI.rememberGoogleSignInHandler(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider.Google,
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    return remember(this) {
        {
            coroutineScope.launch {
                try {
                    signInWithGoogle(context, config, provider)
                } catch (e: AuthException) {
                    // Log.d("rememberGoogleSignInHandler", "exception: $e")
                    updateAuthState(AuthState.Error(e))
                } catch (e: Exception) {
                    val authException = AuthException.from(e)
                    updateAuthState(AuthState.Error(authException))
                }
            }
        }
    }
}

/**
 * Signs in with Google using Credential Manager and optionally requests OAuth scopes.
 *
 * This function implements Google Sign-In using Android's Credential Manager API with
 * comprehensive error handling.
 *
 * **Flow:**
 * 1. If [AuthProvider.Google.scopes] are specified, requests OAuth authorization first
 * 2. Attempts sign-in using Credential Manager
 * 3. Creates Firebase credential and calls [signInAndLinkWithCredential]
 *
 * **Scopes Behavior:**
 * - If [AuthProvider.Google.scopes] is not empty, requests OAuth authorization before sign-in
 * - Basic profile, email, and ID token are always included automatically
 * - Scopes are requested using the AuthorizationClient API
 *
 * **Error Handling:**
 * - [GoogleIdTokenParsingException]: Library version mismatch
 * - [NoCredentialException]: No Google accounts on device
 * - [GetCredentialException]: User cancellation, configuration errors, or no credentials
 * - Configuration errors trigger detailed developer guidance logs
 *
 * @param context Android context for Credential Manager
 * @param config Authentication UI configuration
 * @param provider Google provider configuration with optional scopes
 * @param authorizationProvider Provider for OAuth scopes authorization (for testing)
 * @param credentialManagerProvider Provider for Credential Manager flow (for testing)
 *
 * @throws AuthException.InvalidCredentialsException if token parsing fails
 * @throws AuthException.AuthCancelledException if user cancels or no accounts found
 * @throws AuthException if sign-in or linking fails
 *
 * @see AuthProvider.Google
 * @see signInAndLinkWithCredential
 */
internal suspend fun FirebaseAuthUI.signInWithGoogle(
    context: Context,
    config: AuthUIConfiguration,
    provider: AuthProvider.Google,
    authorizationProvider: AuthProvider.Google.AuthorizationProvider = AuthProvider.Google.DefaultAuthorizationProvider(),
    credentialManagerProvider: AuthProvider.Google.CredentialManagerProvider = AuthProvider.Google.DefaultCredentialManagerProvider(),
) {
    try {
        updateAuthState(AuthState.Loading("Signing in with google..."))

        // Request OAuth scopes if specified (before sign-in)
        if (provider.scopes.isNotEmpty()) {
            try {
                val requestedScopes = provider.scopes.map { Scope(it) }
                authorizationProvider.authorize(context, requestedScopes)
                // Log.d("GoogleSignIn", "Successfully authorized scopes: ${provider.scopes}")
            } catch (e: Exception) {
                // Log.w("GoogleSignIn", "Failed to authorize scopes: ${provider.scopes}", e)
                // Continue with sign-in even if scope authorization fails
                val authException = AuthException.from(e)
                updateAuthState(AuthState.Error(authException))
            }
        }

        val result = credentialManagerProvider.getGoogleCredential(
            context = context,
            serverClientId = provider.serverClientId!!,
            filterByAuthorizedAccounts = true,
            autoSelectEnabled = false
        )

        signInAndLinkWithCredential(
            config = config,
            credential = result.credential,
            provider = provider,
            displayName = result.displayName,
            photoUrl = result.photoUrl,
        )
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in with google was cancelled",
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

/**
 * Signs out from Google and clears credential state.
 *
 * This function clears the cached Google credentials, ensuring that the account picker
 * will be shown on the next sign-in attempt instead of automatically signing in with
 * the previously used account.
 *
 * **When to call:**
 * - After user explicitly signs out
 * - Before allowing user to select a different Google account
 * - When switching between accounts
 *
 * **Note:** This does not sign out from Firebase Auth itself. Call [FirebaseAuth.signOut]
 * separately if you need to sign out from Firebase.
 *
 * @param context Android context for Credential Manager
 */
suspend fun FirebaseAuthUI.signOutFromGoogle(context: Context) {
    try {
        val credentialManager = CredentialManager.create(context)
        credentialManager.clearCredentialState(
            ClearCredentialStateRequest()
        )
        // Log.d("GoogleSignIn", "Cleared Google credential state")
    } catch (e: Exception) {
        // Log.w("GoogleSignIn", "Failed to clear credential state", e)
    }
}