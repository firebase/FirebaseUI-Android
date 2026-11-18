package com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthPage
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton
import com.firebaseui.android.demo.auth.fullcustomization.common.primaryCtaColors
import com.firebaseui.android.demo.auth.fullcustomization.common.secondaryCtaColors
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProChoiceRow
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProErrorText
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProGroup
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProHeader
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProPage
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProRow
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProRowSeparator
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProSectionLabel
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProTextButton
import com.firebaseui.android.demo.auth.fullcustomization.professional.isProfessional
import com.firebaseui.android.demo.auth.fullcustomization.professional.proColors
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.TotpMultiFactorGenerator

@Composable
fun SelectFactorStep(state: MfaEnrollmentContentState) {
    if (isProfessional()) {
        ProSelectFactor(state)
        return
    }

    AuthPage(
        mascot = R.drawable.full_customization_mascot,
        mascotDescription = "doggo - cute security mascot",
        title = "Secure your account",
        cardContentDescription = "mfa - factor selection card",
        card = {
            Text(
                text = "Add a second step to sign-in, so a password on its own isn't enough.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.enrolledFactors.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Already on this account",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    state.enrolledFactors.forEach { info ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = enrolledFactorLabel(info),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = { state.onUnenrollFactor(info) },
                                enabled = !state.isLoading,
                                modifier = Modifier.semantics {
                                    contentDescription =
                                        "button - remove factor ${enrolledFactorLabel(info)}"
                                },
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        },
        actions = {
            state.availableFactors.forEachIndexed { index, factor ->
                if (index > 0) Spacer(modifier = Modifier.height(16.dp))

                CtaButton(
                    text = factorCtaLabel(factor),
                    onClick = { state.onFactorSelected(factor) },
                    enabled = !state.isLoading,
                    // First factor takes the primary CTA colour; the rest read as alternatives.
                    colors = if (index == 0) {
                        primaryCtaColors()
                    } else {
                        secondaryCtaColors()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "button - enroll ${factorCtaLabel(factor)}"
                    },
                )
            }

            state.onSkipClick?.let { onSkip ->
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onSkip,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Not now")
                }
            }
        },
    )
}

/**
 * The professional method choice, as the iOS demo draws it: the account's methods with Remove,
 * then the available ones as radio rows and one Continue.
 */
@Composable
private fun ProSelectFactor(state: MfaEnrollmentContentState) {
    val colors = proColors()
    var selected by remember(state.availableFactors) { mutableStateOf(state.availableFactors.firstOrNull()) }

    ProPage(header = {
        ProHeader(
            icon = Icons.Outlined.Security,
            title = "Set up two-factor authentication",
            subtitle = "Add a second step to sign-in, so a password on its own isn't enough.",
        )
    }) {
        if (state.enrolledFactors.isNotEmpty()) {
            ProSectionLabel(text = "Your methods")
            ProGroup {
                state.enrolledFactors.forEachIndexed { index, info ->
                    if (index > 0) ProRowSeparator(inset = 16.dp)
                    ProRow(
                        title = enrolledFactorLabel(info),
                        value = "Remove",
                        valueTint = colors.danger,
                        showsChevron = false,
                        contentDescription = "button - remove factor ${enrolledFactorLabel(info)}",
                        onClick = { state.onUnenrollFactor(info) },
                        enabled = !state.isLoading,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (state.availableFactors.isNotEmpty()) {
            ProSectionLabel(text = "Choose a method")
            ProGroup {
                state.availableFactors.forEachIndexed { index, factor ->
                    if (index > 0) ProRowSeparator(inset = 66.dp)
                    ProChoiceRow(
                        icon = if (factor == MfaFactor.Sms) Icons.Outlined.Sms else Icons.Outlined.PhoneAndroid,
                        title = if (factor == MfaFactor.Sms) "Text message" else "Authenticator app",
                        detail = if (factor == MfaFactor.Sms) {
                            "We'll text a code to your phone."
                        } else {
                            "Use a code from an app like Google Authenticator."
                        },
                        isSelected = selected == factor,
                        onClick = { selected = factor },
                        contentDescription = "button - choose ${factorCtaLabel(factor)}",
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ProErrorText(state.error)
        Spacer(modifier = Modifier.height(12.dp))
        if (state.availableFactors.isNotEmpty()) {
            CtaButton(
                text = "Continue",
                onClick = { selected?.let(state.onFactorSelected) },
                enabled = selected != null && !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.semantics { contentDescription = "button - start enrollment" },
            )
        }
        state.onSkipClick?.let { onSkip ->
            ProTextButton(
                title = "Not now",
                onClick = onSkip,
                enabled = !state.isLoading,
                contentDescription = "button - not now",
            )
        }
    }
}

private fun factorCtaLabel(factor: MfaFactor): String = when (factor) {
    MfaFactor.Sms -> "Use text message"
    MfaFactor.Totp -> "Use an authenticator app"
}

/**
 * SMS factors carry the phone number as their display name; TOTP factors are often unnamed, so
 * fall back to the factor id.
 */
private fun enrolledFactorLabel(info: MultiFactorInfo): String {
    val fallback = when (info.factorId) {
        PhoneMultiFactorGenerator.FACTOR_ID -> "Text message"
        TotpMultiFactorGenerator.FACTOR_ID -> "Authenticator app"
        else -> info.factorId
    }
    return info.displayName?.takeIf { it.isNotBlank() } ?: fallback
}
