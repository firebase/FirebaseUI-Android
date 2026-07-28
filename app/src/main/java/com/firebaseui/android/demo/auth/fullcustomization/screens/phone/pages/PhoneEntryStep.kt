package com.firebaseui.android.demo.auth.fullcustomization.screens.phone.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.ui.components.CountrySelector
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthFieldShape
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton
import com.firebaseui.android.demo.auth.fullcustomization.common.FullCustomizationTextField
import com.firebaseui.android.demo.auth.fullcustomization.common.HardOffsetShadow

@Composable
fun PhoneEntryStep(state: PhoneAuthContentState) {
    val isPhoneValid = remember(state.phoneNumber) {
        android.util.Patterns.PHONE.matcher(state.phoneNumber).matches()
    }

    // verticalScroll measures content with infinite max height, and Column distributes weights
    // against the MIN height when max is infinite (RowColumnMeasurePolicy.kt) — so
    // heightIn(min = viewport) makes the weighted spacers expand (centering content, anchoring
    // the CTA to the bottom) when everything fits, and collapse to zero (plain scrolling) when it
    // doesn't.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 40.dp, vertical = 24.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.full_customization_phone_mascot),
                    contentDescription = "doggo - cute phone sign-in mascot",
                    modifier = Modifier.size(72.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Login by phone number",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(24.dp))

                HardOffsetShadow(shape = AuthFieldShape, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "phone - sign in card" },
                        color = MaterialTheme.colorScheme.surface,
                        shape = AuthFieldShape,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
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
                                        selectedCountry = state.selectedCountry,
                                        onCountrySelected = state.onCountrySelected,
                                        enabled = !state.isLoading,
                                    )
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
                                    isError = state.error != null,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics { contentDescription = "text-field - phone number input" },
                                )
                            }

                            Text(
                                text = state.error
                                    ?: "By signing in with phone number, an SMS may be sent. " +
                                        "Message & data rates may apply.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (state.error != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            CtaButton(
                text = "Sign Up",
                onClick = state.onSendCodeClick,
                enabled = isPhoneValid && !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.semantics { contentDescription = "button - send verification code" },
            )
        }
    }
}
