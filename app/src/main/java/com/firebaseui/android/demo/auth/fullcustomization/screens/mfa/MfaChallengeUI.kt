package com.firebaseui.android.demo.auth.fullcustomization.screens.mfa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.ui.components.VerificationCodeInputField
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthPage
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton

/**
 * Custom UI for `FirebaseAuthScreen.mfaChallengeContent` — the second-factor prompt shown during
 * sign-in when the account has MFA enrolled.
 */
@Composable
fun MfaChallengeUI(state: MfaChallengeContentState) {
    val isSms = state.factorType == MfaFactor.Sms

    AuthPage(
        mascot = if (isSms) {
            R.drawable.full_customization_phone_mascot
        } else {
            R.drawable.full_customization_mascot
        },
        mascotDescription = "doggo - cute two-factor mascot",
        title = "One more step",
        cardContentDescription = "mfa - challenge card",
        card = {
            Text(
                text = if (isSms) {
                    "We sent a code to ${state.maskedPhoneNumber ?: "your phone"}."
                } else {
                    "Open your authenticator app and enter the 6-digit code for this account."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            VerificationCodeInputField(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "text-field - mfa challenge code input" },
                isError = state.hasError,
                errorMessage = state.error,
                onCodeChange = state.onVerificationCodeChange,
            )

            // canResend already covers "SMS factor and a resend callback exists".
            if (state.canResend) {
                Text(
                    text = if (state.resendTimer > 0) {
                        "Resend code in ${state.resendTimer}s"
                    } else {
                        "Resend code"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.resendTimer == 0 && !state.isLoading) {
                            state.onResendCodeClick?.invoke()
                        },
                )
            }
        },
        actions = {
            CtaButton(
                text = "Verify",
                onClick = state.onVerifyClick,
                enabled = state.isValid && !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.semantics {
                    contentDescription = "button - verify mfa challenge"
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = state.onCancelClick,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel")
            }
        },
    )
}
