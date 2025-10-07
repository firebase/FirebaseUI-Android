package com.firebase.ui.auth.compose.ui.components

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun TermsAndPrivacyForm(
    modifier: Modifier = Modifier,
    tosUrl: String?,
    ppUrl: String?
) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = modifier,
    ) {
        TextButton(
            onClick = {
                tosUrl?.let {
                    uriHandler.openUri(it)
                }
            },
            contentPadding = PaddingValues.Zero,
        ) {
            Text(
                text = "Terms of Service",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        TextButton(
            onClick = {
                ppUrl?.let {
                    uriHandler.openUri(it)
                }
            },
            contentPadding = PaddingValues.Zero,
        ) {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}