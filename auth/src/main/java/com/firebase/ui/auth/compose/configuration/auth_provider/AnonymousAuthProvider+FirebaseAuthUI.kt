package com.firebase.ui.auth.compose.configuration.auth_provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Creates a remembered launcher function for anonymous sign-in.
 *
 * @return A launcher function that starts the anonymous sign-in flow when invoked
 *
 * @see signInAnonymously
 * @see createOrLinkUserWithEmailAndPassword for upgrading anonymous accounts
 */
@Composable
internal fun FirebaseAuthUI.rememberAnonymousSignInHandler(): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    return remember(this) {
        {
            coroutineScope.launch {
                try {
                    signInAnonymously()
                } catch (e: Exception) {
                    // Error already handled via auth state flow in signInAnonymously()
                    // No additional action needed - ErrorRecoveryDialog will show automatically
                }
            }
        }
    }
}

/**
 * Signs in a user anonymously with Firebase Authentication.
 *
 * This method creates a temporary anonymous user account that can be used for testing
 * or as a starting point for users who want to try the app before creating a permanent
 * account. Anonymous users can later be upgraded to permanent accounts by linking
 * credentials (email/password, social providers, phone, etc.).
 *
 * **Flow:**
 * 1. Updates auth state to loading with "Signing in anonymously..." message
 * 2. Calls Firebase Auth's `signInAnonymously()` method
 * 3. Updates auth state to idle on success
 * 4. Handles cancellation and converts exceptions to [AuthException] types
 *
 * **Anonymous Account Benefits:**
 * - No user data collection required
 * - Immediate access to app features
 * - Can be upgraded to permanent account later
 * - Useful for guest users and app trials
 *
 * **Account Upgrade:**
 * Anonymous accounts can be upgraded to permanent accounts by calling methods like:
 * - [signInAndLinkWithCredential] with email/password or social credentials
 * - [createOrLinkUserWithEmailAndPassword] for email/password accounts
 * - [signInWithPhoneAuthCredential] for phone authentication
 *
 * **Example: Basic anonymous sign-in**
 * ```kotlin
 * try {
 *     firebaseAuthUI.signInAnonymously()
 *     // User is now signed in anonymously
 *     // Show app content or prompt for account creation
 * } catch (e: AuthException.AuthCancelledException) {
 *     // User cancelled the sign-in process
 * } catch (e: AuthException.NetworkException) {
 *     // Network error occurred
 * }
 * ```
 *
 * **Example: Anonymous sign-in with upgrade flow**
 * ```kotlin
 * // Step 1: Sign in anonymously
 * firebaseAuthUI.signInAnonymously()
 * 
 * // Step 2: Later, upgrade to permanent account
 * try {
 *     firebaseAuthUI.createOrLinkUserWithEmailAndPassword(
 *         context = context,
 *         config = authUIConfig,
 *         provider = emailProvider,
 *         name = "John Doe",
 *         email = "john@example.com",
 *         password = "SecurePass123!"
 *     )
 *     // Anonymous account upgraded to permanent email/password account
 * } catch (e: AuthException.AccountLinkingRequiredException) {
 *     // Email already exists - show account linking UI
 * }
 * ```
 *
 * @throws AuthException.AuthCancelledException if the coroutine is cancelled
 * @throws AuthException.NetworkException if a network error occurs
 * @throws AuthException.UnknownException for other authentication errors
 *
 * @see signInAndLinkWithCredential for upgrading anonymous accounts
 * @see createOrLinkUserWithEmailAndPassword for email/password upgrade
 * @see signInWithPhoneAuthCredential for phone authentication upgrade
 */
internal suspend fun FirebaseAuthUI.signInAnonymously() {
    try {
        updateAuthState(AuthState.Loading("Signing in anonymously..."))
        auth.signInAnonymously().await()
        updateAuthState(AuthState.Idle)
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in anonymously was cancelled",
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
