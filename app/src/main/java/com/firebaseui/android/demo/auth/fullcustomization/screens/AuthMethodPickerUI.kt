package com.firebaseui.android.demo.auth.fullcustomization.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebase.ui.auth.ui.screens.email.EmailAuthMode
import com.firebaseui.android.demo.auth.fullcustomization.common.OtherSignInMethodsSheet
import com.firebaseui.android.demo.auth.fullcustomization.screens.email.pages.EmailEntryStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.email.pages.LoginStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.email.pages.SignUpStep
import kotlinx.coroutines.tasks.await

@Composable
fun AuthMethodPickerUI(
    state: EmailAuthContentState,
    otherProviders: List<AuthProvider>,
    onProviderSelected: (AuthProvider) -> Unit,
    tosUrl: String?,
    ppUrl: String?,
) {
    // Only "has the user chosen yet" is local; which form to show is state.mode, so the library's
    // own corrections (EmailAlreadyInUse -> SignIn, UserNotFound -> SignUp) actually move the UI.
    var chosen by rememberSaveable { mutableStateOf(false) }
    var showOtherMethods by remember { mutableStateOf(false) }

    // Password/confirmPassword are hoisted in EmailAuthContentState, not local to LoginStep/
    // SignUpStep — they survive a round trip back to EnterEmail, so a stale password typed for
    // one email could carry over if a different email also routes to the same step. Clear them
    // whenever the user backs out via "Use a different email".
    val onUseDifferentEmail: () -> Unit = {
        state.onPasswordChange("")
        state.onConfirmPasswordChange("")
        chosen = false
    }

    // customMethodPickerLayout is the NavHost's start destination and these steps are local
    // state, so without this the system back press would leave the auth flow entirely.
    BackHandler(enabled = chosen) { onUseDifferentEmail() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!chosen) {
            EmailEntryStep(
                email = state.email,
                onEmailChange = state.onEmailChange,
                isLoading = state.isLoading,
                onSignIn = { state.onGoToSignIn(); chosen = true },
                onCreateAccount = { state.onGoToSignUp(); chosen = true },
                onShowOtherMethods = { showOtherMethods = true },
            )
        } else {
            when (state.mode) {
                EmailAuthMode.SignUp -> SignUpStep(state, onUseDifferentEmail)
                // Reset-password and email-link are offered inline on the login form, which also
                // reports their "sent" states, so every mode has a screen and none can blank out.
                EmailAuthMode.SignIn,
                EmailAuthMode.ResetPassword,
                EmailAuthMode.EmailLinkSignIn -> LoginStep(state, onUseDifferentEmail)
            }
        }
    }

    if (showOtherMethods) {
        OtherSignInMethodsSheet(
            otherProviders = otherProviders,
            onProviderSelected = onProviderSelected,
            onDismissRequest = { showOtherMethods = false },
            tosUrl = tosUrl,
            ppUrl = ppUrl,
        )
    }
}

