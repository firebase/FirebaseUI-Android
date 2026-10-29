package com.firebaseui.android.demo.auth.fullcustomization.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.firebaseui.android.demo.auth.fullcustomization.professional.ProPage
import com.firebaseui.android.demo.auth.fullcustomization.professional.isProfessional
import com.firebaseui.android.demo.auth.fullcustomization.theme.LocalFullCustomizationStyle
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
 * The professional account screen: a settings list rather than a card and stacked buttons. The
 * password row opens the change-password form in place; two-factor goes to enrollment.
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
    val user = uiContext.authUI.getCurrentUser()
    val name = user?.displayName?.takeIf { it.isNotBlank() }
    // Letters only: a phone-only account has none, and gets the person glyph instead.
    val initials = (name ?: identifier.substringBefore('@')).split(" ", ".", "_", "-")
        .mapNotNull { part -> part.firstOrNull { it.isLetter() } }.take(2)
        .joinToString("") { it.uppercase() }
    var showPasswordForm by rememberSaveable { mutableStateOf(false) }
    val style = LocalFullCustomizationStyle.current

    ProPage(
        title = "Account",
        modifier = Modifier.semantics { contentDescription = "authenticated - account card" },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(style.secondaryButton.container, CircleShape),
            ) {
                if (initials.isNotEmpty()) {
                    Text(text = initials, color = style.secondaryButton.content, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(imageVector = Icons.Outlined.Person, contentDescription = null, tint = style.secondaryButton.content)
                }
            }
            Column {
                Text(
                    text = name ?: identifier,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (name != null) {
                    Text(
                        text = "Signed in as $identifier",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = "SECURITY",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        )
        ProSettingsGroup {
            ProSettingsRow(
                label = "Password",
                value = if (showPasswordForm) "Cancel" else "Change",
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 16.dp))
            ProSettingsRow(
                label = "Two-factor authentication",
                value = if (hasTwoFactor) "On" else "Off",
                onClick = uiContext.onManageMfa,
                enabled = !isUpdating,
                modifier = Modifier.semantics { contentDescription = "button - manage mfa" },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 16.dp))
            ProSettingsRow(label = "Email verified", value = if (user?.isEmailVerified == true) "Yes" else "No")
        }

        ProSettingsGroup {
            ProSettingsRow(
                label = uiContext.stringProvider.signOutAction,
                labelColor = MaterialTheme.colorScheme.error,
                onClick = uiContext.onSignOut,
                enabled = !isUpdating,
            )
        }
    }
}

@Composable
private fun ProSettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = LocalFullCustomizationStyle.current.cardFill,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

/** One settings row: a label, an optional value, and a chevron when it does something. */
@Composable
private fun ProSettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        value?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null && labelColor == MaterialTheme.colorScheme.onSurface) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
