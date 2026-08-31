package com.firebaseui.android.demo.auth.fullcustomization.screens.email

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.fetchLegacySignInMethods
import com.firebaseui.android.demo.auth.fullcustomization.screens.email.pages.EmailEntryStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.email.pages.LoginStep
import com.firebaseui.android.demo.auth.fullcustomization.screens.email.pages.SignUpStep
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private enum class FlowStep { EnterEmail, Login, SignUp }

/**
 * Custom UI for `FirebaseAuthScreen.emailContent`.
 *
 * The method picker hosts its own email entry, so this slot only renders for email flows the
 * *library* navigates to: reauthentication, account linking, and email-already-in-use recovery.
 * Without it those flows fall back to the library's stock email screen, which is jarring inside a
 * demo whose whole premise is that nothing looks stock.
 *
 * There is no provider sheet here — the caller already committed to email — and an address supplied
 * by the library (as reauthentication does) skips straight to the password step.
 */
@Composable
fun EmailAuthUI(state: EmailAuthContentState, auth: FirebaseAuth) {
    var flowStep by remember {
        mutableStateOf(if (state.email.isBlank()) FlowStep.EnterEmail else FlowStep.Login)
    }
    var isCheckingEmail by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val onUseDifferentEmail: () -> Unit = {
        state.onPasswordChange("")
        state.onConfirmPasswordChange("")
        flowStep = FlowStep.EnterEmail
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

        when (flowStep) {
            FlowStep.EnterEmail -> EmailEntryStep(
                email = state.email,
                onEmailChange = state.onEmailChange,
                isLoading = state.isLoading || isCheckingEmail,
                onContinue = {
                    isCheckingEmail = true
                    coroutineScope.launch {
                        val signInMethods = fetchLegacySignInMethods(auth, state.email)
                        flowStep = if (signInMethods.isEmpty()) FlowStep.SignUp else FlowStep.Login
                        isCheckingEmail = false
                    }
                },
                // No sheet in this slot, so the affordance stays inert rather than opening an
                // empty one.
                onShowOtherMethods = {},
            )

            FlowStep.Login -> LoginStep(state = state, onUseDifferentEmail = onUseDifferentEmail)

            FlowStep.SignUp -> SignUpStep(state = state, onUseDifferentEmail = onUseDifferentEmail)
        }
    }
}
