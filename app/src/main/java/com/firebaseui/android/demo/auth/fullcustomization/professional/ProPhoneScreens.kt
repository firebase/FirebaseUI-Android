package com.firebaseui.android.demo.auth.fullcustomization.professional

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.ui.components.CountrySelector
import com.firebase.ui.auth.ui.components.VerificationCodeInputField
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton
import com.firebaseui.android.demo.auth.fullcustomization.common.FullCustomizationTextField
import com.firebaseui.android.demo.auth.fullcustomization.common.themeFieldBorder
import com.firebaseui.android.demo.auth.fullcustomization.theme.LocalFullCustomizationStyle

@Composable
fun ProPhoneEntry(state: PhoneAuthContentState) {
    val isPhoneValid = remember(state.phoneNumber) {
        android.util.Patterns.PHONE.matcher(state.phoneNumber).matches()
    }
    val style = LocalFullCustomizationStyle.current

    ProPage(title = "Sign in with phone", subtitle = "We'll text a code to this number.") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Phone number",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = style.fieldContainer,
                    shape = style.fieldShape,
                    modifier = Modifier
                        .requiredHeight(48.dp)
                        .themeFieldBorder()
                        .semantics { contentDescription = "country code selector" },
                ) {
                    CountrySelector(
                        selectedCountry = state.selectedCountry,
                        onCountrySelected = state.onCountrySelected,
                        enabled = !state.isLoading,
                    )
                }
                FullCustomizationTextField(
                    value = state.phoneNumber,
                    onValueChange = state.onPhoneNumberChange,
                    placeholder = "(555) 555-0100",
                    enabled = !state.isLoading,
                    isError = state.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "text-field - phone number input" },
                )
            }
            Text(
                text = state.error ?: "Message and data rates may apply.",
                style = MaterialTheme.typography.bodySmall,
                color = if (state.error != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        CtaButton(
            text = "Send code",
            onClick = state.onSendCodeClick,
            enabled = isPhoneValid && !state.isLoading,
            isLoading = state.isLoading,
            modifier = Modifier.semantics { contentDescription = "button - send verification code" },
        )
    }
}

@Composable
fun ProPhoneVerification(state: PhoneAuthContentState) {
    ProPage(
        title = "Enter the code",
        subtitle = "We sent a 6-digit code to ${state.fullPhoneNumber}.",
        topBar = { ProBackBar(onBack = state.onChangeNumberClick, enabled = !state.isLoading) },
    ) {
        VerificationCodeInputField(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "text-field - verification code input" },
            isError = state.error != null,
            errorMessage = state.error,
            onCodeChange = state.onVerificationCodeChange,
        )

        CtaButton(
            text = "Verify",
            onClick = state.onVerifyCodeClick,
            enabled = state.verificationCode.isNotBlank() && !state.isLoading,
            isLoading = state.isLoading,
            modifier = Modifier.semantics { contentDescription = "button - verify code" },
        )

        TextButton(
            onClick = state.onResendCodeClick,
            enabled = state.resendTimer == 0 && !state.isLoading,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = if (state.resendTimer > 0) "Resend code in ${state.resendTimer}s" else "Resend code",
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
