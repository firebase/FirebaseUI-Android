package com.firebase.ui.auth.compose.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ResetPasswordUI(
    email: String,
    resetLinkSent: Boolean,
    onEmailChange: (String) -> Unit,
    onSendResetLink: () -> Unit,
) {

}

@Preview
@Composable
fun PreviewResetPasswordUI() {
    ResetPasswordUI(
        email = "",
        resetLinkSent = false,
        onEmailChange = { email -> },
        onSendResetLink = {},
    )
}