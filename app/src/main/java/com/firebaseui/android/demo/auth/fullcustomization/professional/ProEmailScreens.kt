package com.firebaseui.android.demo.auth.fullcustomization.professional

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebaseui.android.demo.auth.fullcustomization.common.CtaButton
import com.firebaseui.android.demo.auth.fullcustomization.common.FullCustomizationTextField
import com.firebaseui.android.demo.auth.fullcustomization.common.secondaryCtaColors

/** The providers the landing shows inline, in this order; the rest stay in the sheet. */
fun proInlineProviders(providers: List<AuthProvider>): List<AuthProvider> = listOfNotNull(
    providers.firstOrNull { it is AuthProvider.Google },
    providers.firstOrNull { it is AuthProvider.Apple },
    providers.firstOrNull { it is AuthProvider.Phone },
)

private fun inlineLabel(provider: AuthProvider): String = when (provider) {
    is AuthProvider.Google -> "Continue with Google"
    is AuthProvider.Apple -> "Continue with Apple"
    is AuthProvider.Phone -> "Continue with phone"
    else -> "Continue"
}

/**
 * The landing: the common providers inline, then the email form. One Continue goes to the
 * password step and account creation is a link, rather than two equal buttons.
 *
 * @param onShowOtherMethods Opens the sheet with the remaining providers; null hides the link.
 */
@Composable
fun ProLanding(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    onContinue: () -> Unit,
    onCreateAccount: () -> Unit,
    inlineProviders: List<AuthProvider>,
    onProviderSelected: (AuthProvider) -> Unit,
    onShowOtherMethods: (() -> Unit)?,
) {
    val isEmailValid = remember(email) { android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() }
    val showEmailError = email.isNotBlank() && !isEmailValid

    ProPage(title = "Sign in", subtitle = "Use your work account to continue.") {
        if (inlineProviders.isNotEmpty() || onShowOtherMethods != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                inlineProviders.forEach { provider ->
                    ProProviderButton(
                        provider = provider,
                        label = inlineLabel(provider),
                        onClick = { onProviderSelected(provider) },
                        enabled = !isLoading,
                    )
                }
                onShowOtherMethods?.let { show ->
                    TextButton(
                        onClick = show,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .semantics { contentDescription = "Other sign-in methods button" },
                    ) {
                        Text("More sign-in options", fontWeight = FontWeight.Medium)
                    }
                }
            }
            ProOrDivider()
        }

        FullCustomizationTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            placeholder = "name@company.com",
            enabled = !isLoading,
            isError = showEmailError,
            supportingText = if (showEmailError) "Enter a valid email address" else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "text-field - email address input" },
        )

        CtaButton(
            text = "Continue",
            onClick = onContinue,
            enabled = isEmailValid && !isLoading,
            isLoading = isLoading,
            modifier = Modifier.semantics { contentDescription = "button - sign in" },
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "New here?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onCreateAccount,
                enabled = !isLoading,
                modifier = Modifier.semantics { contentDescription = "button - create account" },
            ) {
                Text("Create an account", fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** The password step: back in the top bar, "Forgot password?" on the label row. */
@Composable
fun ProLogin(state: EmailAuthContentState, onUseDifferentEmail: () -> Unit) {
    var passwordVisible by remember { mutableStateOf(false) }

    ProPage(
        title = "Enter your password",
        subtitle = state.email,
        topBar = { ProBackBar(onBack = onUseDifferentEmail, enabled = !state.isLoading) },
    ) {
        FullCustomizationTextField(
            value = state.password,
            onValueChange = state.onPasswordChange,
            label = "Password",
            labelAction = {
                TextButton(
                    onClick = state.onSendResetLinkClick,
                    enabled = !state.resetLinkSent && !state.isLoading,
                ) {
                    Text(
                        text = if (state.resetLinkSent) "Reset link sent" else "Forgot password?",
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            enabled = !state.isLoading,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "text-field - password secure input" },
        )

        CtaButton(
            text = "Sign in",
            onClick = state.onSignInClick,
            enabled = state.password.isNotBlank() && !state.isLoading,
            isLoading = state.isLoading,
            modifier = Modifier.semantics { contentDescription = "button - login" },
        )

        CtaButton(
            text = if (state.emailSignInLinkSent) "Sign-in link sent" else "Email me a sign-in link",
            onClick = state.onSignInEmailLinkClick,
            enabled = !state.isLoading,
            colors = secondaryCtaColors(),
            modifier = Modifier.semantics { contentDescription = "button - send login link" },
        )
    }
}

/** Account creation: one column, the same checks as the shared sign-up step. */
@Composable
fun ProSignUp(state: EmailAuthContentState, onUseDifferentEmail: () -> Unit) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var confirmEmail by remember { mutableStateOf("") }

    // Trimmed and case-insensitive: the default keyboard auto-capitalises on many IMEs.
    val emailsMatch = confirmEmail.isNotBlank() &&
        confirmEmail.trim().equals(state.email.trim(), ignoreCase = true)
    val passwordsMatch = state.confirmPassword.isNotBlank() && state.confirmPassword == state.password
    val canSignUp = firstName.isNotBlank() && lastName.isNotBlank() && emailsMatch &&
        state.password.isNotBlank() && passwordsMatch && !state.isLoading

    ProPage(
        title = "Create your account",
        topBar = { ProBackBar(onBack = onUseDifferentEmail, enabled = !state.isLoading) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FullCustomizationTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = "First name",
                enabled = !state.isLoading,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "text-field - first name" },
            )
            FullCustomizationTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = "Last name",
                enabled = !state.isLoading,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "text-field - last name" },
            )
        }
        FullCustomizationTextField(
            value = state.email,
            onValueChange = state.onEmailChange,
            label = "Email",
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "text-field - email address display" },
        )
        FullCustomizationTextField(
            value = confirmEmail,
            onValueChange = { confirmEmail = it },
            label = "Confirm email",
            enabled = !state.isLoading,
            isError = confirmEmail.isNotBlank() && !emailsMatch,
            supportingText = if (confirmEmail.isNotBlank() && !emailsMatch) "Emails don't match" else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "text-field - confirm email" },
        )
        FullCustomizationTextField(
            value = state.password,
            onValueChange = state.onPasswordChange,
            label = "Password",
            enabled = !state.isLoading,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "text-field - password" },
        )
        FullCustomizationTextField(
            value = state.confirmPassword,
            onValueChange = state.onConfirmPasswordChange,
            label = "Confirm password",
            enabled = !state.isLoading,
            isError = state.confirmPassword.isNotBlank() && !passwordsMatch,
            supportingText = if (state.confirmPassword.isNotBlank() && !passwordsMatch) "Passwords don't match" else null,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "text-field - confirm password" },
        )
        CtaButton(
            text = "Create account",
            onClick = {
                state.onDisplayNameChange("$firstName $lastName".trim())
                state.onSignUpClick()
            },
            enabled = canSignUp,
            isLoading = state.isLoading,
            modifier = Modifier.semantics { contentDescription = "button - sign up" },
        )
    }
}
