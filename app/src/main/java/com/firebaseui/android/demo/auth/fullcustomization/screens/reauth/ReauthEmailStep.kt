package com.firebaseui.android.demo.auth.fullcustomization.screens.reauth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton
import com.firebaseui.android.demo.auth.fullcustomization.common.FullCustomizationTextField

/**
 * The email step of reauthentication, for `FirebaseAuthScreen.emailContent` when the library has
 * locked the address.
 *
 * Sign-in's own page is the wrong screen here twice over. The library composes the reauth email
 * step inside a modal bottom sheet, so a full-bleed background and a viewport-height layout fight
 * the sheet rather than sit in it; and reauthentication turns off sign-up and email-link sign-in
 * (`isEmailSignUpOffered`/`isEmailLinkSignInOffered` both return false in that mode), so the
 * affordances that page offers alongside the password are dead. This is the one thing the user can
 * actually do: confirm the password for an address they cannot change.
 *
 * Password reset stays, because it still works — the link is sent while the sheet is up.
 */
@Composable
fun ReauthEmailStep(state: EmailAuthContentState) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .semantics { contentDescription = "reauth - email password card" },
    ) {
        Image(
            painter = painterResource(id = R.drawable.full_customization_mascot),
            contentDescription = "doggo - cute welcome mascot",
            modifier = Modifier.size(56.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Confirm it's you",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the password for ${state.email}.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        FullCustomizationTextField(
            value = state.password,
            onValueChange = state.onPasswordChange,
            label = "Password",
            enabled = !state.isLoading,
            isError = state.error != null,
            supportingText = state.error,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = null,
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "text-field - reauth password secure input" },
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (state.resetLinkSent) "Reset link sent!" else "Forgot password?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !state.resetLinkSent && !state.isLoading) {
                    state.onSendResetLinkClick()
                },
        )

        Spacer(modifier = Modifier.height(24.dp))

        CtaButton(
            text = "Confirm",
            onClick = state.onSignInClick,
            enabled = state.password.isNotBlank() && !state.isLoading,
            isLoading = state.isLoading,
            modifier = Modifier.semantics { contentDescription = "button - confirm reauth" },
        )
    }
}
