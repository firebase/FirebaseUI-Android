package com.firebaseui.android.demo.auth.fullcustomization.professional

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton
import com.firebaseui.android.demo.auth.fullcustomization.common.FullCustomizationTextField
import com.firebaseui.android.demo.auth.fullcustomization.common.providerIconPainter

/** The providers the sign-in screen shows itself, as the iOS demo does; the rest are one tap away. */
fun proInlineProviders(providers: List<AuthProvider>): List<AuthProvider> = listOfNotNull(
    providers.firstOrNull { it is AuthProvider.Google },
    providers.firstOrNull { it is AuthProvider.Phone },
)

private fun isValidEmail(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

/**
 * The professional sign-in screen: the providers first, then email and password on the same
 * screen. Creating an account, resetting a password and email-link sign-in are their own screens.
 *
 * @param onShowOtherMethods Opens the sheet with the remaining providers; null hides that button.
 * @param onBack Shown only where this screen was pushed over another.
 */
@Composable
fun ProSignIn(
    state: EmailAuthContentState,
    isEmailLinkOffered: Boolean,
    inlineProviders: List<AuthProvider> = emptyList(),
    onProviderSelected: (AuthProvider) -> Unit = {},
    onShowOtherMethods: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    val colors = proColors()
    ProPage(
        footer = { ProLegalText() },
        header = {
            onBack?.let { ProBackButton(onBack = it, enabled = !state.isLoading) }
            ProBrand()
            Spacer(modifier = Modifier.height(28.dp))
            ProHeader(title = "Sign in to your account", subtitle = "Welcome back. Choose how you'd like to continue.")
        },
    ) {
        if (inlineProviders.isNotEmpty() || onShowOtherMethods != null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                inlineProviders.forEach { provider ->
                    val title = if (provider is AuthProvider.Phone) "Continue with phone" else "Continue with Google"
                    ProOutlineButton(
                        title = title,
                        onClick = { onProviderSelected(provider) },
                        contentDescription = "button - $title",
                        enabled = !state.isLoading,
                    ) {
                        if (provider is AuthProvider.Phone) {
                            Icon(imageVector = Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.padding(1.dp))
                        } else {
                            providerIconPainter(provider)?.let { ProProviderMark(it) }
                        }
                    }
                }
                onShowOtherMethods?.let { show ->
                    ProOutlineButton(
                        title = "More sign-in options",
                        onClick = show,
                        contentDescription = "Other sign-in methods button",
                        enabled = !state.isLoading,
                    ) {
                        Icon(imageVector = Icons.Outlined.MoreHoriz, contentDescription = null)
                    }
                }
            }
            Column(modifier = Modifier.padding(vertical = 22.dp)) { ProDivider(label = "or continue with email") }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProEmailField(state)
            ProPasswordField(
                value = state.password,
                onValueChange = state.onPasswordChange,
                label = "Password",
                placeholder = "Enter your password",
                enabled = !state.isLoading,
                contentDescription = "text-field - password secure input",
                content = {
                    Text(
                        text = "Forgot password?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.accent,
                        modifier = Modifier
                            .clickable(enabled = !state.isLoading, role = Role.Button, onClick = state.onGoToResetPassword)
                            .semantics { contentDescription = "button - forgot password" },
                    )
                },
            )
            ProErrorText(state.error)
            CtaButton(
                text = "Sign in",
                onClick = state.onSignInClick,
                enabled = isValidEmail(state.email) && state.password.isNotBlank() && !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { contentDescription = "button - login" },
            )
        }
        if (isEmailLinkOffered) {
            Spacer(modifier = Modifier.height(4.dp))
            ProTextButton(
                title = "Email me a sign-in link",
                onClick = state.onGoToEmailLinkSignIn,
                contentDescription = "button - email link",
                enabled = !state.isLoading,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        ProPromptLink(
            prompt = "Don't have an account?",
            action = "Create account",
            onClick = state.onGoToSignUp,
            contentDescription = "button - create account",
        )
    }
}

/** Account creation, pushed over sign-in. */
@Composable
fun ProSignUp(state: EmailAuthContentState, onBack: () -> Unit) {
    val passwordsMatch = state.confirmPassword.isNotBlank() && state.confirmPassword == state.password
    val canSignUp = isValidEmail(state.email) && state.password.isNotBlank() && passwordsMatch && !state.isLoading

    ProPage(
        footer = { ProLegalText(prefix = "By creating an account, you agree to the") },
        header = {
            ProBackButton(onBack = onBack, enabled = !state.isLoading)
            ProHeader(title = "Create your account", subtitle = "Sign up with your email. It takes less than a minute.")
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProEmailField(state, contentDescription = "text-field - email address display")
            ProPasswordField(
                value = state.password,
                onValueChange = state.onPasswordChange,
                label = "Password",
                placeholder = "Create a password",
                enabled = !state.isLoading,
                contentDescription = "text-field - password",
            )
            ProPasswordField(
                value = state.confirmPassword,
                onValueChange = state.onConfirmPasswordChange,
                label = "Confirm password",
                placeholder = "Repeat your password",
                enabled = !state.isLoading,
                isError = state.confirmPassword.isNotBlank() && !passwordsMatch,
                supportingText = if (state.confirmPassword.isNotBlank() && !passwordsMatch) "Passwords don't match" else null,
                contentDescription = "text-field - confirm password",
            )
            ProErrorText(state.error)
            CtaButton(
                text = "Create account",
                onClick = state.onSignUpClick,
                enabled = canSignUp,
                isLoading = state.isLoading,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { contentDescription = "button - sign up" },
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        ProPromptLink(
            prompt = "Already have an account?",
            action = "Sign in",
            onClick = onBack,
            contentDescription = "button - back to sign in",
        )
    }
}

/** Password reset, pushed over sign-in; becomes "Check your inbox" once the link is sent. */
@Composable
fun ProResetPassword(state: EmailAuthContentState, onBack: () -> Unit) {
    if (state.resetLinkSent) {
        ProPage(header = {
            ProBackButton(onBack = onBack)
            ProHeader(
                icon = Icons.Outlined.MarkEmailUnread,
                title = "Check your inbox",
                subtitle = "We sent a link to reset your password to ${state.email}.",
            )
        }) {
            CtaButton(
                text = "Back to sign in",
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "button - back to sign in" },
            )
        }
        return
    }
    ProPage(header = {
        ProBackButton(onBack = onBack, enabled = !state.isLoading)
        ProHeader(
            icon = Icons.Outlined.Lock,
            title = "Reset your password",
            subtitle = "Enter the email you sign in with and we'll send you a link to choose a new password.",
        )
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProEmailField(state)
            ProErrorText(state.error)
            CtaButton(
                text = "Send reset link",
                onClick = state.onSendResetLinkClick,
                enabled = isValidEmail(state.email) && !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.semantics { contentDescription = "button - send reset link" },
            )
        }
    }
}

/** Email-link sign-in, pushed over sign-in; becomes "Check your inbox" once the link is sent. */
@Composable
fun ProEmailLink(state: EmailAuthContentState, onBack: () -> Unit) {
    if (state.emailSignInLinkSent) {
        ProPage(header = {
            ProBackButton(onBack = onBack)
            ProHeader(
                icon = Icons.Outlined.MarkEmailUnread,
                title = "Check your inbox",
                subtitle = "We sent a sign-in link to ${state.email}. Open it on this device to finish signing in.",
            )
        }) {
            ProTextButton(
                title = "Use a different email",
                onClick = onBack,
                contentDescription = "button - use a different email",
            )
        }
        return
    }
    ProPage(header = {
        ProBackButton(onBack = onBack, enabled = !state.isLoading)
        ProHeader(
            icon = Icons.Outlined.Email,
            title = "Sign in with a link",
            subtitle = "We'll email you a link that signs you in, no password needed.",
        )
    }) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProEmailField(state)
            ProErrorText(state.error)
            CtaButton(
                text = "Send link",
                onClick = state.onSignInEmailLinkClick,
                enabled = isValidEmail(state.email) && !state.isLoading,
                isLoading = state.isLoading,
                modifier = Modifier.semantics { contentDescription = "button - send login link" },
            )
        }
    }
}

@Composable
private fun ProEmailField(state: EmailAuthContentState, contentDescription: String = "text-field - email address input") {
    FullCustomizationTextField(
        value = state.email,
        onValueChange = state.onEmailChange,
        label = "Email",
        placeholder = "name@company.com",
        // Locked while reauthenticating: the address is the signed-in user's.
        enabled = !state.isLoading && !state.isEmailLocked,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
    )
}

@Composable
private fun ProPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    enabled: Boolean,
    contentDescription: String,
    isError: Boolean = false,
    supportingText: String? = null,
    /** An action on the label row, such as "Forgot password?". */
    content: (@Composable () -> Unit)? = null,
) {
    var revealed by remember { mutableStateOf(false) }
    FullCustomizationTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        labelAction = content,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { revealed = !revealed }) {
                Icon(
                    imageVector = if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (revealed) "Hide password" else "Show password",
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
    )
}
