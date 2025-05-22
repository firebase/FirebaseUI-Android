package com.firebase.ui.auth.compose

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth

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
        enableAnonymousUpgrade: Boolean = false,
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val authUI = remember { AuthUI.getInstance() }

    val firebaseUser by
            produceState(initialValue = auth.currentUser, auth) {
                val listener = FirebaseAuth.AuthStateListener { value = it.currentUser }
                auth.addAuthStateListener(listener)
                awaitDispose { auth.removeAuthStateListener(listener) }
            }

    if (firebaseUser != null) {
        signedInContent()
        return
    }

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
            }

    var signInAttempted by rememberSaveable { mutableStateOf(false) }

    val launcher =
            rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
                onSignInResult(result)

                signInAttempted = result.resultCode != Activity.RESULT_OK
            }

    LaunchedEffect(Unit) {
        if (!signInAttempted) {
            signInAttempted = true
            launcher.launch(signInIntent)
        }
    }

    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
}
