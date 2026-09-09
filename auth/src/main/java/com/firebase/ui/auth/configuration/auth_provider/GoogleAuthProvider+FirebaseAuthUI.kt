package com.firebase.ui.auth.configuration.auth_provider

import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.firebase.ui.auth.AuthFlowScope
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.util.EmailLinkPersistenceManager
import com.firebase.ui.auth.util.SignInPreferenceManager
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Remembers a callback that runs [signInWithGoogle] in the composition's scope.
 *
 * Rebuilt on every recomposition, so it always captures the latest parameters.
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
 * Signs in with Google through Credential Manager.
 *
 * Requests OAuth authorization first when [AuthProvider.Google.scopes] is non-empty, then hands
 * the credential to [signInAndLinkWithCredential], which owns anonymous upgrade and collision
 * handling.
 *
 * Dismissing the Credential Manager sheet is not an error: it emits [AuthState.Cancelled] and
 * returns normally rather than throwing, so the flow stays open on the method picker.
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
 * **Note:** This does not sign out from Firebase Auth itself. Call [com.firebase.ui.auth.FirebaseAuthUI.signOut]
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