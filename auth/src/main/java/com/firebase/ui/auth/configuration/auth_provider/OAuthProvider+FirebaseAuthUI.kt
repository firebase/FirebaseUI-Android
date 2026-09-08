package com.firebase.ui.auth.configuration.auth_provider

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.firebase.ui.auth.AuthFlowScope
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider.Companion.canUpgradeAnonymous
import com.firebase.ui.auth.util.SignInPreferenceManager
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Remembers a callback that runs [signInWithProvider] in the composition's scope.
 *
 * Rebuilt on every recomposition so it always captures the latest parameters, and resolves the
 * host Activity itself.
 */
@Composable
internal fun AuthFlowScope.rememberOAuthSignInHandler(
    context: Context,
    activity: Activity?,
    provider: AuthProvider.OAuth,
    onSignInFailure: (AuthException) -> Unit = {},
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    activity ?: throw IllegalStateException(
        "OAuth sign-in requires an Activity. " +
                "Ensure FirebaseAuthScreen is used within an Activity."
    )

    return {
        coroutineScope.launch {
            try {
                signInWithProvider(
                    context = context,
                    activity = activity,
                    provider = provider
                )
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
 * Signs in with an OAuth provider — GitHub, Microsoft, Yahoo, Apple, Twitter, or a custom
 * OIDC/SAML provider.
 *
 * Uses Firebase's native OAuth flow, then hands the credential to
 * [signInAndLinkWithCredential], which owns anonymous upgrade and collision handling.
 */
internal suspend fun AuthFlowScope.signInWithProvider(
    context: Context,
    activity: Activity,
    provider: AuthProvider.OAuth,
) {
    try {
        emit(AuthState.Loading(config.stringProvider.loadingSigningInWithProvider(provider.providerName)))

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
            val authResult = pendingResult.await()
            val credential = authResult.credential as? OAuthCredential

            if (credential != null) {
                // Complete the pending sign-in/link flow
                signInAndLinkWithCredential(
                    credential = credential,
                    provider = provider,
                    displayName = authResult.user?.displayName,
                    photoUrl = authResult.user?.photoUrl,
                )
            }
            return
        }

        // Determine if we should upgrade anonymous user, reauthenticate, or do normal sign-in
        val authResult = when {
            canUpgradeAnonymous(config, auth) ->
                auth.currentUser?.startActivityForLinkWithProvider(activity, oauthProvider)?.await()
            config.isReauthenticationMode -> {
                val currentUser = auth.currentUser
                    ?: throw AuthException.UserNotFoundException(message = "No user is currently signed in for reauthentication")
                currentUser.startActivityForReauthenticateWithProvider(activity, oauthProvider).await()
            }
            else ->
                auth.startActivityForSignInWithProvider(activity, oauthProvider).await()
        }

        // Extract OAuth credential and complete sign-in
        val credential = authResult?.credential as? OAuthCredential
        if (credential != null) {
            // The user is already signed in via startActivityForSignInWithProvider/startActivityForLinkWithProvider

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
                    android.util.Log.d("OAuthProvider", "Sign-in preference saved for: $identifier (${provider.providerId})")
                }
            } catch (e: Exception) {
                // Failed to save preference - log but don't break auth flow
                android.util.Log.w("OAuthProvider", "Failed to save sign-in preference", e)
            }

            if (config.isReauthenticationMode) {
                val reauthenticatedUser = auth.currentUser
                    ?: throw AuthException.UserNotFoundException(
                        message = "No user is currently signed in for reauthentication"
                    )
                emit(
                    AuthState.Success(
                        result = authResult,
                        user = reauthenticatedUser,
                        reauthenticatedUid = reauthenticatedUser.uid,
                    )
                )
            } else {
                emitResult(authResult)
            }
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
        emit(AuthState.Error(accountLinkingException))
        throw accountLinkingException
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Signing in with ${provider.providerName} was cancelled",
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
