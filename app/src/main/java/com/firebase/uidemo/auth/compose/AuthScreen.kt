package com.firebase.uidemo.auth.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.uidemo.R

@Composable
fun AuthScreen(
    onSignInResult: (FirebaseAuthUIAuthenticationResult) -> Unit
) {
    val providers = listOf(
        IdpConfig.GoogleBuilder().build(),
        IdpConfig.EmailBuilder().build(),
    )

    FirebaseAuthUI(
        providers = providers,
        onSignInResult = { result -> /* optional logging */ },

        signedInContent = {
            SignedInScreen(idpResponse = null) {
            }
        },

        theme = R.style.AppTheme,
        logo = R.drawable.firebase_auth_120dp,
        tosUrl = "https://www.google.com/policies/terms/",
        privacyPolicyUrl = "https://www.google.com/policies/privacy/"
    )
} 