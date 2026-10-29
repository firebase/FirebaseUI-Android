package com.firebaseui.android.demo.auth.fullcustomization.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.ui.screens.AuthSuccessUiContext
import com.firebase.ui.auth.util.displayIdentifier
import com.firebase.ui.auth.util.getDisplayEmail
import com.firebaseui.android.demo.R
import com.firebaseui.android.demo.auth.fullcustomization.common.AuthPage
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton
import com.firebaseui.android.demo.auth.fullcustomization.common.FullCustomizationTextField
import com.firebaseui.android.demo.auth.fullcustomization.common.secondaryCtaColors
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProAvatar
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProGroup
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProHeader
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProPage
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProRow
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProRowSeparator
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProSectionLabel
import com.firebaseui.android.demo.auth.fullcustomization.professional.isProfessional
import com.firebaseui.android.demo.auth.fullcustomization.professional.proColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "FullCustomizationDemo"

/**
 * Custom UI for `FirebaseAuthScreen.authenticatedContent`.
 *
 * Its main job in this demo is making the other slots reachable: the two-factor button navigates to
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
    // Not remembered: the identifier changes across sign-out and reauth.
    val identifier = authUI.getCurrentUser().displayIdentifier()

    // Refreshes the cached factor list on return from MFA; keyed on Unit so it cannot loop.
    LaunchedEffect(Unit) { uiContext.onReloadUser() }
    val enrolledFactors = authUI.getCurrentUser()?.multiFactor?.enrolledFactors.orEmpty()

    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val changePassword: () -> Unit = {
        // lifecycleScope: the reauth overlay replaces this screen; the retry outlives it.
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
            } catch (e: AuthException.AuthCancelledException) {
                // Declined, not failed: the password is unchanged, so report neither.
                Log.d(TAG, "Reauthentication declined", e)
            } catch (e: Exception) {
                Log.e(TAG, "Password change failed", e)
                isError = true
                statusMessage = "Couldn't change the password. Try again."
            } finally {
                isUpdating = false
            }
        }
    }

    val passwordField: @Composable () -> Unit = {
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
    }

    if (isProfessional()) {
        ProAccountPage(
            uiContext = uiContext,
            identifier = identifier,
            hasTwoFactor = enrolledFactors.isNotEmpty(),
            canChangePassword = newPassword.length >= 6 && !isUpdating,
            isUpdating = isUpdating,
            onChangePassword = changePassword,
        ) {
            passwordField()
        }
        return
    }

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

            passwordField()
        },
        actions = {
            CtaButton(
                text = "Change password",
                onClick = changePassword,
                enabled = newPassword.length >= 6 && !isUpdating,
                isLoading = isUpdating,
                modifier = Modifier.semantics { contentDescription = "button - change password" },
            )

            Spacer(modifier = Modifier.height(16.dp))

            CtaButton(
                // Relabelled, not disabled: SelectFactorStep is the only place a factor can be removed.
                text = if (enrolledFactors.isEmpty()) "Set up two-factor" else "Manage two-factor",
                onClick = uiContext.onManageMfa,
                enabled = !isUpdating,
                colors = secondaryCtaColors(),
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
                colors = secondaryCtaColors(),
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

/**
 * The professional account screen, as the iOS demo draws it: a profile line, a Security section of
 * settings rows, and sign-out on its own group. The password row opens the change form in place.
 */
@Composable
private fun ProAccountPage(
    uiContext: AuthSuccessUiContext,
    identifier: String,
    hasTwoFactor: Boolean,
    canChangePassword: Boolean,
    isUpdating: Boolean,
    onChangePassword: () -> Unit,
    /** The new-password field, shown when the password row is opened. */
    content: @Composable () -> Unit,
) {
    val colors = proColors()
    val user = uiContext.authUI.getCurrentUser()
    val providerIds = user?.providerData?.map { it.providerId }.orEmpty().filter { it != "firebase" }
    // Only accounts that sign in with a password can change one.
    val hasPassword = "password" in providerIds
    val signInMethod = when (providerIds.firstOrNull()) {
        "password" -> "email"
        "phone" -> "phone"
        "google.com" -> "Google"
        "apple.com" -> "Apple"
        "github.com" -> "GitHub"
        null -> "a temporary account"
        else -> providerIds.first()
    }
    val email = user?.email?.takeIf { it.isNotBlank() }
    var showPasswordForm by rememberSaveable { mutableStateOf(false) }

    ProPage(
        modifier = Modifier.semantics { contentDescription = "authenticated - account card" },
        header = { ProHeader(title = "Account") },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "profile" },
        ) {
            ProAvatar(identity = identifier)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = identifier.ifBlank { "Signed in" },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
                Text(text = "Signed in with $signInMethod", fontSize = 14.sp, color = colors.muted)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        ProSectionLabel(text = "Security")
        ProGroup {
            if (hasPassword) {
                ProRow(
                    icon = Icons.Outlined.Key,
                    title = "Password",
                    value = if (showPasswordForm) "Cancel" else "Change",
                    contentDescription = "button - password row",
                    onClick = { showPasswordForm = !showPasswordForm },
                    enabled = !isUpdating,
                )
                if (showPasswordForm) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    ) {
                        content()
                        CtaButton(
                            text = "Update password",
                            onClick = onChangePassword,
                            enabled = canChangePassword,
                            isLoading = isUpdating,
                            modifier = Modifier.semantics { contentDescription = "button - change password" },
                        )
                    }
                }
                ProRowSeparator()
            }
            ProRow(
                icon = Icons.Outlined.Security,
                title = "Two-factor authentication",
                value = if (hasTwoFactor) "On" else "Off",
                contentDescription = "button - manage mfa",
                onClick = uiContext.onManageMfa,
                enabled = !isUpdating,
            )
            if (email != null) {
                ProRowSeparator()
                val verified = user?.isEmailVerified == true
                ProRow(
                    icon = Icons.Outlined.Email,
                    title = "Email",
                    value = if (verified) "Verified" else "Verify",
                    valueTint = if (verified) colors.muted else colors.accent,
                    showsChevron = false,
                    contentDescription = "button - verify email",
                    onClick = if (verified) null else ({ user?.sendEmailVerification(); Unit }),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        ProGroup {
            ProRow(
                title = uiContext.stringProvider.signOutAction,
                tint = colors.accent,
                showsChevron = false,
                contentDescription = "button - sign out",
                onClick = uiContext.onSignOut,
                enabled = !isUpdating,
            )
        }
    }
}
