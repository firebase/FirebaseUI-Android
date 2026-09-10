package com.firebaseui.android.demo.auth.fullcustomization.screens.mfa.pages

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.ui.components.QrCodeImage
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthFieldShape
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthPage
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton

@Composable
fun ConfigureTotpStep(state: MfaEnrollmentContentState) {
    AuthPage(
        mascot = R.drawable.full_customization_mascot,
        mascotDescription = "doggo - cute security mascot",
        title = "Scan to set up",
        cardContentDescription = "mfa - totp setup card",
        card = {
            Text(
                text = "Scan this with your authenticator app, or type the key in by hand.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.totpQrCodeUrl?.let { url ->
                QrCodeImage(
                    content = url,
                    size = 200.dp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = AuthFieldShape,
                        )
                        .padding(12.dp)
                        .semantics { contentDescription = "mfa - totp qr code" },
                )
            }

            state.totpSecret?.sharedSecretKey?.let { key ->
                SelectionContainer {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "mfa - totp shared secret key" },
                    )
                }
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        actions = {
            CtaButton(
                text = "I've added it",
                onClick = state.onContinueToVerifyClick,
                enabled = state.isValid && !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.semantics {
                    contentDescription = "button - continue to mfa verification"
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
