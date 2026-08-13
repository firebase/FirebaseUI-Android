package com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.TotpMultiFactorGenerator

@Composable
fun SelectFactorStep(state: MfaEnrollmentContentState) {
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
                    // The first factor carries the primary CTA colour; the rest read as
                    // alternatives, matching how LoginStep tiers its two CTAs.
                    colors = if (index == 0) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
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
