package com.firebase.ui.auth.configuration.auth_provider

import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.firebase.ui.auth.AuthFlowScope
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.util.EmailLinkPersistenceManager
import com.firebase.ui.auth.util.SignInPreferenceManager
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Creates a remembered callback for Google Sign-In that can be invoked from UI components.
 *
 * This Composable function returns a lambda that, when invoked, initiates the Google Sign-In
 * flow using [signInWithGoogle]. The callback is rebuilt on every recomposition so it always
 * captures the latest parameters, and handles coroutine scoping and error state management.
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
 * @param onSignInFailure Callback invoked with the resulting [AuthException] on failure
 * @return A callback function that initiates Google Sign-In when invoked
 *
 * @see signInWithGoogle
 * @see AuthProvider.Google
 */
@Composable
internal fun AuthFlowScope.rememberGoogleSignInHandler(
    context: Context,
    provider: AuthProvider.Google,
    onSignInFailure: (AuthException) -> Unit = {},
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    return {
        coroutineScope.launch {
            try {
                signInWithGoogle(context, provider)
            } catch (e: AuthException) {
                emit(AuthState.Error(e))
                if (e !is AuthException.AuthCancelledException) onSignInFailure(e)
            } catch (e: Exception) {
                val authException = AuthException.from(e, context)
                emit(AuthState.Error(authException))
                if (authException !is AuthException.AuthCancelledException) onSignInFailure(authException)
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
 * - [GetCredentialCancellationException]: User dismissed the Credential Manager sheet -
 *   updates [AuthState.Cancelled] and does not throw
 * - [GetCredentialException]: Configuration errors or no credentials
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
internal suspend fun AuthFlowScope.signInWithGoogle(
    context: Context,
    provider: AuthProvider.Google,
    authorizationProvider: AuthProvider.Google.AuthorizationProvider = AuthProvider.Google.DefaultAuthorizationProvider(),
    credentialManagerProvider: AuthProvider.Google.CredentialManagerProvider = AuthProvider.Google.DefaultCredentialManagerProvider(),
) {
    var idTokenFromResult: String? = null
    try {
        emit(AuthState.Loading(config.stringProvider.loadingSigningInWithGoogle))

        // Request OAuth scopes if specified (before sign-in)
        if (provider.scopes.isNotEmpty()) {
            try {
                val requestedScopes = provider.scopes.map { Scope(it) }
                authorizationProvider.authorize(context, requestedScopes)
            } catch (e: Exception) {
                // Continue with sign-in even if scope authorization fails
                val authException = AuthException.from(e, context)
                emit(AuthState.Error(authException))
            }
        }

        // Try with configured filterByAuthorizedAccounts setting
        // If default (true), fallback to false if no authorized accounts found
        // See: https://developer.android.com/identity/sign-in/credential-manager-siwg#siwg-button
        val result = if (provider.filterByAuthorizedAccounts) {
            // Default behavior: Try authorized accounts first, fallback to all accounts
            try {
                (this.credentialManagerProvider ?: credentialManagerProvider).getGoogleCredential(
                    context = context,
                    credentialManager = CredentialManager.create(context),
                    serverClientId = provider.serverClientId!!,
                    filterByAuthorizedAccounts = true,
                    autoSelectEnabled = provider.autoSelectEnabled
                )
            } catch (e: NoCredentialException) {
                // No authorized accounts found, try again with all accounts for sign-up flow
                Log.d("GoogleAuthProvider", "No authorized accounts found, showing all Google accounts for sign-up")
                try {
                    (this.credentialManagerProvider ?: credentialManagerProvider).getGoogleCredential(
                        context = context,
                        credentialManager = CredentialManager.create(context),
                        serverClientId = provider.serverClientId!!,
                        filterByAuthorizedAccounts = false,
                        autoSelectEnabled = provider.autoSelectEnabled
                    )
                } catch (fallbackException: NoCredentialException) {
                    // Credential Manager doesn't distinguish "no account on device" from
                    // developer-side misconfiguration, so log the possible causes for
                    // debugging. Never surfaced to end users: the overwhelming majority
                    // hitting this genuinely have no account, and Firebase Console
                    // guidance would just confuse them.
                    Log.w(
                        "GoogleAuthProvider",
                        "No credential returned from Credential Manager after trying both " +
                            "authorized and all accounts. Possible causes: (1) no Google " +
                            "account on this device, (2) no Android OAuth client / SHA-1 " +
                            "registered for this app's package + signing certificate in the " +
                            "Firebase console, or (3) the Credential Manager Google ID " +
                            "provider is unavailable on this device.",
                        fallbackException
                    )
                    // No Google accounts available on device at all
                    throw AuthException.UnknownException(
                        message = "No Google accounts available.\n\nPlease add a Google account to your device and try again.",
                        cause = fallbackException
                    )
                }
            }
        } else {
            // Developer explicitly wants to show all accounts (no fallback needed)
            (this.credentialManagerProvider ?: credentialManagerProvider).getGoogleCredential(
                context = context,
                credentialManager = CredentialManager.create(context),
                serverClientId = provider.serverClientId!!,
                filterByAuthorizedAccounts = false,
                autoSelectEnabled = provider.autoSelectEnabled
            )
        }
        idTokenFromResult = result.idToken

        signInAndLinkWithCredential(
            credential = result.credential,
            provider = provider,
            displayName = result.displayName,
            photoUrl = result.photoUrl,
        )

        // Save sign-in preference for "Continue as..." feature
        try {
            val user = auth.currentUser
            val identifier = user?.email
            if (identifier != null) {
                SignInPreferenceManager.saveLastSignIn(
                    context = context,
                    providerId = provider.providerId,
                    identifier = identifier
                )
                Log.d("GoogleAuthProvider", "Sign-in preference saved for: $identifier")
            }
        } catch (e: Exception) {
            // Failed to save preference - log but don't break auth flow
            android.util.Log.w("GoogleAuthProvider", "Failed to save sign-in preference", e)
        }
    } catch (e: AuthException.AccountLinkingRequiredException) {
        // Account collision occurred - save Facebook credential for linking after email link sign-in
        // This happens when a user tries to sign in with Facebook but an email link account exists
        EmailLinkPersistenceManager.default.saveCredentialForLinking(
            context = context,
            providerType = provider.providerId,
            idToken = idTokenFromResult,
            accessToken = null
        )

        // Re-throw to let UI handle the account linking flow
        emit(AuthState.Error(e))
        throw e
    } catch (e: GetCredentialCancellationException) {
        // User dismissed the Credential Manager sheet - this is a normal user action,
        // not an error, so it goes to AuthState.Cancelled instead of AuthState.Error.
        // Swallow (don't rethrow) so rememberGoogleSignInHandler's catch block doesn't
        // overwrite this state with AuthState.Error.
        emit(AuthState.Cancelled)

    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in with google was cancelled",
            cause = e
        )
        emit(AuthState.Error(cancelledException))
        throw cancelledException

    } catch (e: AuthException) {
        emit(AuthState.Error(e))
        throw e

    } catch (e: Exception) {
        val authException = AuthException.from(e, context)
        emit(AuthState.Error(authException))
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
 * **Note:** This does not sign out from Firebase Auth itself. Call [FirebaseAuthUI.signOut]
 * separately if you need to sign out from Firebase.
 *
 * @param context Android context for Credential Manager
 */
internal suspend fun signOutFromGoogle(
    auth: FirebaseAuth,
    context: Context,
    credentialManagerProvider: AuthProvider.Google.CredentialManagerProvider = AuthProvider.Google.DefaultCredentialManagerProvider(),
) {
    try {
        if (Provider.fromId(auth.currentUser?.providerId) != Provider.GOOGLE) return
        credentialManagerProvider.clearCredentialState(
            context = context,
            credentialManager = CredentialManager.create(context)
        )
    } catch (e: Exception) {
        Log.e("GoogleAuthProvider", "Error during Google sign out", e)
    }
}