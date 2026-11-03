package com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.ui.components.CountrySelector
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthPage
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton
import com.firebaseui.android.demo.auth.fullcustomization.common.FullCustomizationTextField
import com.firebaseui.android.demo.auth.fullcustomization.common.themeFieldBorder
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProErrorText
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProHeader
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProPage
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProPhoneField
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProTextButton
import com.firebaseui.android.demo.auth.fullcustomization.professional.isProfessional
import com.firebaseui.android.demo.auth.fullcustomization.theme.LocalFullCustomizationStyle

@Composable
fun ConfigureSmsStep(state: MfaEnrollmentContentState) {
    if (isProfessional()) {
        ProConfigureSms(state)
        return
    }

    AuthPage(
        mascot = R.drawable.full_customization_phone_mascot,
        mascotDescription = "doggo - cute phone sign-in mascot",
        title = "Add your number",
        cardContentDescription = "mfa - sms setup card",
        card = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // CountrySelector needs a non-null country, so skip the step while it resolves.
                state.selectedCountry?.let { country ->
                    Surface(
                        color = LocalFullCustomizationStyle.current.fieldContainer,
                        shape = LocalFullCustomizationStyle.current.fieldShape,
                        modifier = Modifier
                            .requiredHeight(56.dp)
                            .themeFieldBorder()
                            .semantics { contentDescription = "country code selector" },
                    ) {
                        CountrySelector(
                            selectedCountry = country,
                            onCountrySelected = state.onCountrySelected,
                            enabled = !state.isLoading,
                        )
                    }
                }

                FullCustomizationTextField(
                    value = state.phoneNumber,
                    onValueChange = state.onPhoneNumberChange,
                    placeholder = "Phone number",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    enabled = !state.isLoading,
                    isError = state.hasError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "text-field - mfa phone number input" },
                )
            }

            Text(
                text = state.error
                    ?: "We'll text a code to this number whenever you sign in. " +
                    "Message & data rates may apply.",
                style = MaterialTheme.typography.bodyLarge,
                color = if (state.hasError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        actions = {
            CtaButton(
                text = "Send code",
                onClick = state.onSendSmsCodeClick,
                enabled = state.isValid && !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.semantics {
                    contentDescription = "button - send mfa sms code"
                },
            )

            if (state.canGoBack) {
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = state.onBackClick,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Pick a different method")
                }
            }
        },
    )
}

/** The professional SMS step: the phone sign-in screen's combined field, as the iOS demo uses. */
@Composable
private fun ProConfigureSms(state: MfaEnrollmentContentState) {
    ProPage(header = {
        ProHeader(
            icon = Icons.Outlined.Sms,
            title = "Add your phone number",
            subtitle = "We'll text a code to this number whenever you sign in.",
        )
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProPhoneField(
                phoneNumber = state.phoneNumber,
                onPhoneNumberChange = state.onPhoneNumberChange,
                selectedCountry = state.selectedCountry,
                onCountrySelected = state.onCountrySelected,
                enabled = !state.isLoading,
                contentDescription = "text-field - mfa phone number input",
            )
            ProErrorText(state.error)
            CtaButton(
                text = "Send code",
                onClick = state.onSendSmsCodeClick,
                enabled = state.isValid && !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.semantics { contentDescription = "button - send mfa sms code" },
            )
        }
        if (state.canGoBack) {
            ProTextButton(
                title = "Pick a different method",
                onClick = state.onBackClick,
                enabled = !state.isLoading,
                contentDescription = "button - pick a different method",
            )
        }
    }
}
