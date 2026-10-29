package com.firebaseui.android.demo.auth.fullcustomization.professional

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firebase.ui.auth.data.CountryData
import com.firebase.ui.auth.ui.components.CountrySelector
import com.firebase.ui.auth.ui.components.VerificationCodeInputField
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton

@Composable
fun ProPhoneEntry(state: PhoneAuthContentState) {
    val isPhoneValid = remember(state.phoneNumber) {
        android.util.Patterns.PHONE.matcher(state.phoneNumber).matches()
    }
    ProPage(header = {
        ProHeader(
            icon = Icons.Outlined.Phone,
            title = "Sign in with your phone",
            subtitle = "We'll text you a 6-digit code to confirm it's you. Message and data rates may apply.",
        )
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProPhoneField(
                phoneNumber = state.phoneNumber,
                onPhoneNumberChange = state.onPhoneNumberChange,
                selectedCountry = state.selectedCountry,
                onCountrySelected = state.onCountrySelected,
                enabled = !state.isLoading,
                contentDescription = "text-field - phone number input",
            )
            ProErrorText(state.error)
            CtaButton(
                text = "Send code",
                onClick = state.onSendCodeClick,
                enabled = isPhoneValid && !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.semantics { contentDescription = "button - send verification code" },
            )
        }
    }
}

@Composable
fun ProPhoneVerification(state: PhoneAuthContentState) {
    ProPage(header = {
        ProHeader(
            icon = Icons.Outlined.Pin,
            title = "Enter the code",
            subtitle = "We sent a 6-digit code to ${state.fullPhoneNumber}.",
        )
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
        }
        Spacer(modifier = Modifier.height(4.dp))
        ProTextButton(
            title = if (state.resendTimer > 0) "Resend code in ${state.resendTimer}s" else "Resend code",
            onClick = state.onResendCodeClick,
            enabled = state.resendTimer == 0 && !state.isLoading,
            contentDescription = "button - resend code",
        )
        ProTextButton(
            title = "Use a different number",
            onClick = state.onChangeNumberClick,
            enabled = !state.isLoading,
            contentDescription = "button - use a different number",
        )
    }
}

/** The library's country picker and the number in one bordered box, split by a hairline. */
@Composable
fun ProPhoneField(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountry: CountryData?,
    onCountrySelected: (CountryData) -> Unit,
    enabled: Boolean,
    contentDescription: String,
) {
    val colors = proColors()
    val shape = RoundedCornerShape(10.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = "Phone number", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.text)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(colors.field, shape)
                .border(1.dp, colors.border, shape)
                .padding(start = 4.dp, end = 14.dp),
        ) {
            selectedCountry?.let { country ->
                Box(modifier = Modifier.semantics { this.contentDescription = "country code selector" }) {
                    CountrySelector(
                        selectedCountry = country,
                        onCountrySelected = onCountrySelected,
                        enabled = enabled,
                    )
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(colors.hairline))
            }
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .weight(1f)
                    .semantics { this.contentDescription = contentDescription },
                decorationBox = { inner ->
                    if (phoneNumber.isEmpty()) {
                        Text(text = "555 123 4567", fontSize = 16.sp, color = colors.placeholder)
                    }
                    inner()
                },
            )
        }
    }
}
