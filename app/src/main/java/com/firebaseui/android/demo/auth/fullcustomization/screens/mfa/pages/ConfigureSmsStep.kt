package com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.pages

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
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
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthFieldShape
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthPage
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton
import com.firebaseui.android.demo.auth.fullcustomization.common.FullCustomizationTextField

@Composable
fun ConfigureSmsStep(state: MfaEnrollmentContentState) {
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
                // CountrySelector needs a non-null country; the library's own default UI skips the
                // whole step while the country is still resolving, so match that.
                state.selectedCountry?.let { country ->
                    Surface(
                        color = Color.White,
                        shape = AuthFieldShape,
                        modifier = Modifier
                            .requiredHeight(56.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = AuthFieldShape,
                            )
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
