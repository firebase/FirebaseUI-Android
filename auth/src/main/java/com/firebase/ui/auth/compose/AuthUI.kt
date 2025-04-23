package com.firebase.ui.auth.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth

/**
 * A composable function that provides Firebase Auth UI functionality.
 * 
 * @param providers List of identity providers to show in the UI
 * @param onSignInResult Callback for handling sign-in results
 * @param theme Theme resource ID for the UI
 * @param logo Logo resource ID for the UI
 * @param tosUrl Terms of service URL
 * @param privacyPolicyUrl Privacy policy URL
 * @param enableCredentials Whether to enable credential manager
 * @param enableAnonymousUpgrade Whether to enable anonymous user upgrade
 */
@Composable
fun FirebaseAuthUI(
    providers: List<IdpConfig>,
    onSignInResult: (FirebaseAuthUIAuthenticationResult) -> Unit,
    theme: Int = AuthUI.getDefaultTheme(),
    logo: Int = AuthUI.NO_LOGO,
    tosUrl: String? = null,
    privacyPolicyUrl: String? = null,
    enableCredentials: Boolean = true,
    enableAnonymousUpgrade: Boolean = false
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val authUI = remember { AuthUI.getInstance() }
    
    val signInLauncher = rememberLauncherForActivityResult(
        contract = FirebaseAuthUIActivityResultContract(),
        onResult = onSignInResult
    )

    val signInIntent = remember(providers, theme, logo, tosUrl, privacyPolicyUrl, enableCredentials, enableAnonymousUpgrade) {
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

    LaunchedEffect(Unit) {
        signInLauncher.launch(signInIntent)
    }
} 