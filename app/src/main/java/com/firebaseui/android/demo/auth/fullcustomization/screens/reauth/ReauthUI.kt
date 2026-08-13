package com.firebaseui.android.demo.auth.fullcustomization.screens.reauth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.AuthState
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthPage
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton
import com.firebaseui.android.demo.auth.fullcustomization.common.EmailFieldIcon
import com.firebaseui.android.demo.auth.fullcustomization.common.FullCustomizationTextField
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Custom UI for `FirebaseAuthScreen.reauthContent`.
 *
 * Unlike the MFA slots, this one is handed only the [AuthState.ReauthenticationRequired] state —
 * no input values and no verify callback — so the re-authentication itself happens here. Providing
 * this slot also opts out of the library's default reauth bottom sheet, which is what would
 * otherwise drive the credential flow.
 *
 * @param onVerified invoked once the credential is accepted. The caller is responsible for
 * dismissing this UI and running [AuthState.ReauthenticationRequired.retryOperation] from a scope
 * that outlives this composable — see `FullCustomizationDemoActivity`.
 */
@Composable
fun ReauthUI(
    state: AuthState.ReauthenticationRequired,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
) {
    val email = state.user.email.orEmpty()
    val hasPasswordProvider = remember(state.user) {
        state.user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
    }

    if (!hasPasswordProvider) {
        UnsupportedProviderPage(reason = state.reason, onDismiss = onDismiss)
        return
    }

    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AuthPage(
        mascot = R.drawable.full_customization_mascot,
        mascotDescription = "doggo - cute welcome mascot",
        title = "Is that you?",
        cardContentDescription = "reauth - password card",
        card = {
            Text(
                text = state.reason ?: "Confirm your password to continue.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FullCustomizationTextField(
                value = email,
                onValueChange = {},
                enabled = false,
                leadingIcon = { EmailFieldIcon() },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "text-field - reauth email display" },
            )

            FullCustomizationTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = "Password",
                enabled = !isVerifying,
                isError = errorMessage != null,
                supportingText = errorMessage,
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
        },
        actions = {
            CtaButton(
                text = "Verify",
                onClick = {
                    coroutineScope.launch {
                        isVerifying = true
                        errorMessage = null
                        try {
                            state.user
                                .reauthenticate(
                                    EmailAuthProvider.getCredential(email, password)
                                )
                                .await()
                            // Neither call suspends, so cancellation can't interleave once
                            // onVerified() tears this composable down.
                            onVerified()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage
                                ?: "We couldn't verify that password."
                            isVerifying = false
                        }
                    }
                },
                enabled = password.isNotBlank() && !isVerifying,
                isLoading = isVerifying,
                modifier = Modifier.semantics { contentDescription = "button - verify reauth" },
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                enabled = !isVerifying,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel")
            }
        },
    )
}

/**
 * The library only routes here when at least one configured provider is linked to the user, so a
 * federated-only account still reaches this slot — it just can't be re-verified by password.
 */
@Composable
private fun UnsupportedProviderPage(reason: String?, onDismiss: () -> Unit) {
    AuthPage(
        mascot = R.drawable.full_customization_mascot,
        mascotDescription = "doggo - cute welcome mascot",
        title = "Is that you?",
        cardContentDescription = "reauth - unsupported provider card",
        card = {
            reason?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "This account signs in with a provider rather than a password, so this " +
                    "demo screen can't re-verify it. Sign out and back in to continue.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        },
        actions = {
            CtaButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.semantics { contentDescription = "button - dismiss reauth" },
            )
        },
    )
}
