package com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthPage
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton

@Composable
fun RecoveryCodesStep(state: MfaEnrollmentContentState) {
    AuthPage(
        mascot = R.drawable.full_customization_mascot,
        mascotDescription = "doggo - cute security mascot",
        title = "Save these codes",
        cardContentDescription = "mfa - recovery codes card",
        card = {
            Text(
                text = "Keep these somewhere safe. Each one gets you back in once if you lose " +
                    "your second factor.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SelectionContainer {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "mfa - recovery code list" },
                ) {
                    state.recoveryCodes.orEmpty().forEach { code ->
                        Text(
                            text = code,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        actions = {
            CtaButton(
                text = "I've saved them",
                onClick = state.onCodesSavedClick,
                enabled = state.isValid && !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.semantics {
                    contentDescription = "button - confirm recovery codes saved"
                },
            )
        },
    )
}
