package com.firebase.ui.auth.compose.configuration.auth_provider

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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun FirebaseAuthUI.rememberSignInWithFacebookLauncher(
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
                    // val cancelledException = AuthException.AuthCancelledException(
                    //     message = "Sign in with facebook was cancelled",
                    // )
                    // updateAuthState(AuthState.Error(cancelledException))
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

// Your app can only have one person at a time logged in, and LoginManager sets the current
// AccessToken and Profile for that person. The FacebookSDK saves this data in shared preferences
// and sets at the beginning of the session. You can see if a person is already logged in by
// checking AccessToken.getCurrentAccessToken() and Profile.getCurrentProfile().
//
// You can load AccessToken.getCurrentAccessToken with the SDK from cache or from an app book
// mark when your app launches from a cold start. You should check its validity in your Activity's
// onCreate method:
//
// AccessToken accessToken = AccessToken.getCurrentAccessToken();
// boolean isLoggedIn = accessToken != null && !accessToken.isExpired();

internal suspend fun FirebaseAuthUI.signInWithFacebook(
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

