package com.firebase.ui.auth.configuration.auth_provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.firebase.ui.auth.AuthFlowScope
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Creates a remembered launcher function for anonymous sign-in.
 *
 * @param onSignInFailure Callback invoked with the resulting [AuthException] on failure
 * @return A launcher function that starts the anonymous sign-in flow when invoked
 *
 * @see signInAnonymously
 * @see createOrLinkUserWithEmailAndPassword for upgrading anonymous accounts
 */
@Composable
internal fun AuthFlowScope.rememberAnonymousSignInHandler(
    onSignInFailure: (AuthException) -> Unit = {},
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    return {
        coroutineScope.launch {
            try {
                signInAnonymously()
            } catch (e: AuthException) {
                // Already an AuthException, don't re-wrap it
                emit(AuthState.Error(e))
                if (e !is AuthException.AuthCancelledException) onSignInFailure(e)
            } catch (e: Exception) {
                val authException = AuthException.from(e, config.stringProvider)
                emit(AuthState.Error(authException))
                if (authException !is AuthException.AuthCancelledException) onSignInFailure(authException)
            }
        }
    }
}

/**
 * Signs the user in anonymously.
 *
 * The account is temporary: linking a credential to it later upgrades it in place, which is
 * what anonymous upgrade does for every other provider.
 */
internal suspend fun AuthFlowScope.signInAnonymously() {
    try {
        emit(AuthState.Loading(config.stringProvider.loadingSigningInAnonymously))
        val result = auth.signInAnonymously().await()
        emitResult(result, defaultIsNewUser = true)
    } catch (e: CancellationException) {
        val cancelledException = AuthException.AuthCancelledException(
            message = "Sign in anonymously was cancelled",
            cause = e
        )
        emit(AuthState.Error(cancelledException))
        throw cancelledException
    } catch (e: AuthException) {
        emit(AuthState.Error(e))
        throw e
    } catch (e: Exception) {
        val authException = AuthException.from(e, config.stringProvider)
        emit(AuthState.Error(authException))
        throw authException
    }
}
