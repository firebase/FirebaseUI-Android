package com.firebase.ui.auth.compose

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Composable that handles Firebase Auth UI sign-in and automatically swaps to [signedInContent]
 * while a user is authenticated.
 *
 * @param providers Identity-providers to show in Firebase UI
 * @param onSignInResult Callback with the raw Firebase UI result
 * @param signedInContent UI displayed when [FirebaseAuth.getInstance().currentUser] ≠ null
 * @param theme Custom theme for Firebase UI (default = library default)
 * @param logo Drawable resource for the logo in Firebase UI
 * @param tosUrl Terms-of-service URL (optional)
 * @param privacyPolicyUrl Privacy-policy URL (optional)
 * @param enableCredentials Whether to enable Google Credential Manager / Smart Lock
 * @param enableAnonymousUpgrade Auto-upgrade anonymous users if `true`
 */
@Composable
fun FirebaseAuthUI(
        providers: List<IdpConfig>,
        signedInContent: @Composable () -> Unit,
        onSignInResult: (FirebaseAuthUIAuthenticationResult) -> Unit = {},
        theme: Int = AuthUI.getDefaultTheme(),
        logo: Int = AuthUI.NO_LOGO,
        tosUrl: String? = null,
        privacyPolicyUrl: String? = null,
        enableCredentials: Boolean = true,
        enableAnonymousUpgrade: Boolean = false
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val authUI = remember { AuthUI.getInstance() }
    val scope = rememberCoroutineScope()

    /* ------------- 1) Observe auth state ------------- */
    val firebaseUser by
            produceState(initialValue = auth.currentUser, auth) {
                val listener =
                        FirebaseAuth.AuthStateListener { firebaseAuth ->
                            value = firebaseAuth.currentUser
                        }
                auth.addAuthStateListener(listener)
                awaitDispose { auth.removeAuthStateListener(listener) }
            }

    /* ------------- 2) If signed in, show caller-provided screen ------------- */
    if (firebaseUser != null) {
        signedInContent()
        return
    }

    /* ------------- 3) Otherwise prepare & launch Firebase UI sign-in ------------- */
    val signInLauncher =
            rememberLauncherForActivityResult(
                    contract = FirebaseAuthUIActivityResultContract(),
                    onResult = onSignInResult
            )

    val signInIntent =
            remember(
                    providers,
                    theme,
                    logo,
                    tosUrl,
                    privacyPolicyUrl,
                    enableCredentials,
                    enableAnonymousUpgrade
            ) {
                authUI.createSignInIntentBuilder()
                        .setTheme(theme)
                        .setLogo(logo)
                        .setAvailableProviders(providers)
                        .setCredentialManagerEnabled(enableCredentials)
                        .apply {
                            if (tosUrl != null && privacyPolicyUrl != null) {
                                setTosAndPrivacyPolicyUrls(tosUrl, privacyPolicyUrl)
                            }
                            if (enableAnonymousUpgrade && auth.currentUser?.isAnonymous == true) {
                                enableAnonymousUsersAutoUpgrade()
                            }
                        }
                        .build()
                        .apply {
                            addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        }
            }

    /* ­------------- 4) Launch once per composable lifetime ------------- */
    LaunchedEffect(signInIntent) {
        // Launch from a coroutine so we’re safe even inside composition
        scope.launch { signInLauncher.launch(signInIntent) }
    }

    /* Optional: lightweight in-place progress indicator while Firebase UI Activity starts */
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
