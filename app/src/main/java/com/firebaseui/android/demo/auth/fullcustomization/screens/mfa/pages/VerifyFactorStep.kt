package com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.pages

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
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.ui.components.VerificationCodeInputField
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthPage
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton

@Composable
fun VerifyFactorStep(state: MfaEnrollmentContentState) {
    val isSms = state.selectedFactor == MfaFactor.Sms
    val fullPhoneNumber = "${state.selectedCountry?.dialCode ?: ""}${state.phoneNumber}"

    AuthPage(
        mascot = if (isSms) {
            R.drawable.full_customization_phone_mascot
        } else {
            R.drawable.full_customization_mascot
        },
        mascotDescription = "doggo - cute two-factor mascot",
        title = "Confirm the code",
        cardContentDescription = "mfa - enrollment verification card",
        card = {
            Text(
                text = if (isSms) {
                    "We sent a code to $fullPhoneNumber."
                } else {
                    "Enter the 6-digit code your authenticator app is showing right now."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            VerificationCodeInputField(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "text-field - mfa enrollment code input" },
                isError = state.hasError,
                errorMessage = state.error,
                onCodeChange = state.onVerificationCodeChange,
            )

            // onResendCodeClick is null for TOTP, where there is nothing to resend.
            state.onResendCodeClick?.let { onResend ->
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
                            onResend()
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
                    contentDescription = "button - verify mfa enrollment"
                },
            )

            if (state.canGoBack) {
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = state.onBackClick,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Back")
                }
            }
        },
    )
}
