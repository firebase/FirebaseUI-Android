package com.firebaseui.android.demo.auth.fullcustomization.screens

import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.ui.screens.AuthSuccessUiContext
import com.firebase.ui.auth.util.displayIdentifier
import com.firebase.ui.auth.util.getDisplayEmail
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthPage
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton
import com.firebaseui.android.demo.auth.fullcustomization.common.FullCustomizationTextField
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "FullCustomizationDemo"

/**
 * Custom UI for `FirebaseAuthScreen.authenticatedContent`.
 *
 * Its main job in this demo is making the other slots reachable: "Set up two-factor" navigates to
 * the flow that `mfaEnrollmentContent` renders, and changing the password is a sensitive operation,
 * so wrapping it in [com.firebase.ui.auth.FirebaseAuthUI.withReauth] is what provokes
 * `reauthContent`.
 *
 * This slot also receives the email-verification and profile-completion states, which the library
 * would otherwise render itself — so they are handled here too rather than falling through to a
 * blank screen.
 */
@Composable
fun AuthenticatedUI(state: AuthState, uiContext: AuthSuccessUiContext) {
    when (state) {
        is AuthState.RequiresEmailVerification -> VerifyEmailPage(uiContext)
        is AuthState.RequiresProfileCompletion -> ProfileCompletionPage(state, uiContext)
        else -> SignedInPage(uiContext)
    }
}

@Composable
private fun SignedInPage(uiContext: AuthSuccessUiContext) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val authUI = uiContext.authUI
    // Read on every recomposition rather than remembering: the identifier has to follow the
    // current user, which changes across sign-out and reauth.
    val identifier = authUI.getCurrentUser().displayIdentifier()

    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    AuthPage(
        mascot = R.drawable.full_customization_mascot,
        mascotDescription = "doggo - cute welcome mascot",
        title = "You're in",
        cardContentDescription = "authenticated - account card",
        card = {
            Text(
                text = if (identifier.isNotBlank()) "Signed in as $identifier" else "Signed in",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = "Changing your password needs a recent sign-in, so it triggers the custom " +
                    "reauth screen.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FullCustomizationTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    statusMessage = null
                },
                label = "New password",
                enabled = !isUpdating,
                isError = isError,
                supportingText = statusMessage,
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
                    .semantics { contentDescription = "text-field - new password secure input" },
            )
        },
        actions = {
            CtaButton(
                text = "Change password",
                onClick = {
                    // lifecycleScope rather than rememberCoroutineScope: the reauth overlay
                    // replaces this screen mid-flight, and the retried operation has to outlive it.
                    lifecycleOwner.lifecycleScope.launch {
                        isUpdating = true
                        statusMessage = null
                        isError = false
                        try {
                            authUI.withReauth(
                                context,
                                reason = "Verify your identity to change your password",
                            ) {
                                authUI.getCurrentUser()?.updatePassword(newPassword)?.await()
                                Log.d(TAG, "Password changed successfully")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Password change failed", e)
                            isError = true
                            statusMessage = "Couldn't change the password. Try again."
                        } finally {
                            isUpdating = false
                        }
                    }
                },
                enabled = newPassword.length >= 6 && !isUpdating,
                isLoading = isUpdating,
                modifier = Modifier.semantics { contentDescription = "button - change password" },
            )

            Spacer(modifier = Modifier.height(16.dp))

            CtaButton(
                text = "Set up two-factor",
                onClick = uiContext.onManageMfa,
                enabled = !isUpdating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier.semantics { contentDescription = "button - manage mfa" },
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = uiContext.onSignOut,
                enabled = !isUpdating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(uiContext.stringProvider.signOutAction)
            }
        },
    )
}

@Composable
private fun VerifyEmailPage(uiContext: AuthSuccessUiContext) {
    val stringProvider = uiContext.stringProvider
    val user = uiContext.authUI.getCurrentUser()
    val emailLabel = user.getDisplayEmail(stringProvider.emailProvider)

    AuthPage(
        mascot = R.drawable.full_customization_mascot,
        mascotDescription = "doggo - cute welcome mascot",
        title = "Check your inbox",
        cardContentDescription = "authenticated - verify email card",
        card = {
            Text(
                text = stringProvider.verifyEmailInstruction(emailLabel),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        actions = {
            CtaButton(
                text = stringProvider.verifiedEmailAction,
                onClick = uiContext.onReloadUser,
                modifier = Modifier.semantics {
                    contentDescription = "button - recheck email verification"
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            CtaButton(
                text = stringProvider.resendVerificationEmailAction,
                onClick = { user?.sendEmailVerification() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier.semantics {
                    contentDescription = "button - resend verification email"
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = uiContext.onSignOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringProvider.signOutAction)
            }
        },
    )
}

@Composable
private fun ProfileCompletionPage(
    state: AuthState.RequiresProfileCompletion,
    uiContext: AuthSuccessUiContext,
) {
    val stringProvider = uiContext.stringProvider

    AuthPage(
        mascot = R.drawable.full_customization_mascot,
        mascotDescription = "doggo - cute welcome mascot",
        title = "Almost there",
        cardContentDescription = "authenticated - profile completion card",
        card = {
            Text(
                text = stringProvider.profileCompletionMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.missingFields.isNotEmpty()) {
                Text(
                    text = stringProvider.profileMissingFieldsMessage(
                        state.missingFields.joinToString()
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        actions = {
            TextButton(
                onClick = uiContext.onSignOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringProvider.signOutAction)
            }
        },
    )
}
