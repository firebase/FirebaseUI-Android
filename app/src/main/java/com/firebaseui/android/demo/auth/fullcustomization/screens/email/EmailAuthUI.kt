package com.firebaseui.android.demo.auth.fullcustomization.screens.email

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebase.ui.auth.ui.screens.email.EmailAuthMode
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.screens.email.pages.EmailEntryStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.email.pages.LoginStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.email.pages.SignUpStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.reauth.ReauthEmailStep

/**
 * Custom UI for `FirebaseAuthScreen.emailContent`.
 *
 * The method picker hosts its own email entry, so this slot only renders for email flows the
 * *library* navigates to: reauthentication, account linking, and email-already-in-use recovery.
 * Without it those flows fall back to the library's stock email screen, which is jarring inside a
 * demo whose whole premise is that nothing looks stock.
 *
 * An address supplied by the library (as reauthentication does) skips the choice entirely — the
 * caller already knows who is signing in.
 */
@Composable
fun EmailAuthUI(state: EmailAuthContentState) {
    // isEmailLocked is only ever true in reauthentication mode (EmailAuthScreen sets it from
    // isReauthenticationMode and a prefilled address), and reauth composes this slot inside a modal
    // bottom sheet, so it needs its own sheet-shaped screen rather than the sign-in page.
    if (state.isEmailLocked) {
        ReauthEmailStep(state)
        return
    }

    var chosen by rememberSaveable { mutableStateOf(state.email.isNotBlank()) }

    val onUseDifferentEmail: () -> Unit = {
        state.onPasswordChange("")
        state.onConfirmPasswordChange("")
        chosen = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // The email pages don't paint their own background — MainUI and PhoneSignInUI do it for
        // theirs — so this slot has to, or the screen renders on bare surface colour.
        Image(
            painter = painterResource(id = R.drawable.custom_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        if (!chosen) {
            EmailEntryStep(
                email = state.email,
                onEmailChange = state.onEmailChange,
                isLoading = state.isLoading,
                onSignIn = {
                    state.onGoToSignIn()
                    chosen = true
                },
                onCreateAccount = {
                    state.onGoToSignUp()
                    chosen = true
                },
                // No provider sheet in this slot — the caller already committed to email.
                onShowOtherMethods = {},
            )
        } else {
            when (state.mode) {
                EmailAuthMode.SignUp -> SignUpStep(state, onUseDifferentEmail)
                EmailAuthMode.SignIn,
                EmailAuthMode.ResetPassword,
                EmailAuthMode.EmailLinkSignIn -> LoginStep(state, onUseDifferentEmail)
            }
        }
    }
}
