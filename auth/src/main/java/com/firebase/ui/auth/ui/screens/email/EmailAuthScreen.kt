/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.ui.screens.email

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.auth_provider.createOrLinkUserWithEmailAndPassword
import com.firebase.ui.auth.configuration.auth_provider.sendPasswordResetEmail
import com.firebase.ui.auth.configuration.auth_provider.sendSignInLinkToEmail
import com.firebase.ui.auth.configuration.auth_provider.signInWithEmailAndPassword
import com.firebase.ui.auth.configuration.auth_provider.signInWithEmailLink
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.ui.components.LocalTopLevelDialogController
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.launch

enum class EmailAuthMode {
    SignIn,
    EmailLinkSignIn,
    SignUp,
    ResetPassword,
}

/**
 * A class passed to the content slot, containing all the necessary information to render custom
 * UIs for sign-in, sign-up, and password reset flows.
 *
 * @param mode An enum representing the current UI mode. Use a when expression on this to render
 * the correct screen.
 * @param isLoading true when an asynchronous operation (like signing in or sending an email)
 * is in progress.
 * @param error An optional error message to display to the user.
 * @param email The current value of the email input field.
 * @param onEmailChange (Modes: [EmailAuthMode.SignIn], [EmailAuthMode.SignUp],
 * [EmailAuthMode.ResetPassword]) A callback to be invoked when the email input changes.
 * @param password An optional custom layout composable for the provider buttons.
 * @param onPasswordChange (Modes: [EmailAuthMode.SignIn], [EmailAuthMode.SignUp]) The current
 * value of the password input field.
 * @param confirmPassword (Mode: [EmailAuthMode.SignUp]) A callback to be invoked when the password
 * input changes.
 * @param onConfirmPasswordChange (Mode: [EmailAuthMode.SignUp]) A callback to be invoked when
 * the password confirmation input changes.
 * @param displayName (Mode: [EmailAuthMode.SignUp]) The current value of the display name field.
 * @param onDisplayNameChange (Mode: [EmailAuthMode.SignUp]) A callback to be invoked when the
 * display name input changes.
 * @param onSignInClick (Mode: [EmailAuthMode.SignIn]) A callback to be invoked to attempt a
 * sign-in with the provided credentials.
 * @param onSignUpClick (Mode: [EmailAuthMode.SignUp]) A callback to be invoked to attempt to
 * create a new account.
 * @param onSendResetLinkClick (Mode: [EmailAuthMode.ResetPassword]) A callback to be invoked to
 * send a password reset email.
 * @param resetLinkSent (Mode: [EmailAuthMode.ResetPassword]) true after the password reset link
 * has been successfully sent.
 * @param emailSignInLinkSent (Mode: [EmailAuthMode.SignIn]) true after the email sign in link has
 * been successfully sent.
 * @param onGoToSignUp A callback to switch the UI to the SignUp mode.
 * @param onGoToSignIn A callback to switch the UI to the SignIn mode.
 * @param onGoToResetPassword A callback to switch the UI to the ResetPassword mode.
 */
class EmailAuthContentState(
    val mode: EmailAuthMode,
    val isLoading: Boolean = false,
    val error: String? = null,
    val email: String,
    val onEmailChange: (String) -> Unit,
    val password: String,
    val onPasswordChange: (String) -> Unit,
    val confirmPassword: String,
    val onConfirmPasswordChange: (String) -> Unit,
    val displayName: String,
    val onDisplayNameChange: (String) -> Unit,
    val onSignInClick: () -> Unit,
    val onSignInEmailLinkClick: () -> Unit,
    val onSignUpClick: () -> Unit,
    val onSendResetLinkClick: () -> Unit,
    val resetLinkSent: Boolean = false,
    val emailSignInLinkSent: Boolean = false,
    val onGoToSignUp: () -> Unit,
    val onGoToSignIn: () -> Unit,
    val onGoToResetPassword: () -> Unit,
    val onGoToEmailLinkSignIn: () -> Unit,
)

/**
 * A stateful composable that manages the logic for all email-based authentication flows,
 * including sign-in, sign-up, and password reset. It exposes the state for the current mode to
 * a custom UI via a trailing lambda (slot), allowing for complete visual customization.
 *
 * @param configuration
 * @param onSuccess
 * @param onError
 * @param onCancel
 * @param content
 */
@Composable
fun EmailAuthScreen(
    context: Context,
    configuration: AuthUIConfiguration,
    authUI: FirebaseAuthUI,
    credentialForLinking: AuthCredential? = null,
    emailLinkFromDifferentDevice: String? = null,
    onSuccess: (AuthResult) -> Unit,
    onError: (AuthException) -> Unit,
    onCancel: () -> Unit,
    content: @Composable ((EmailAuthContentState) -> Unit)? = null,
) {
    val provider = configuration.providers.filterIsInstance<AuthProvider.Email>().first()
    val stringProvider = LocalAuthUIStringProvider.current
    val dialogController = LocalTopLevelDialogController.current
    val coroutineScope = rememberCoroutineScope()

    // Start in EmailLinkSignIn mode if coming from cross-device flow
    val initialMode = if (emailLinkFromDifferentDevice != null && provider.isEmailLinkSignInEnabled) {
        EmailAuthMode.EmailLinkSignIn
    } else {
        EmailAuthMode.SignIn
    }
    val mode = rememberSaveable { mutableStateOf(initialMode) }
    val displayNameValue = rememberSaveable { mutableStateOf("") }
    val emailTextValue = rememberSaveable { mutableStateOf("") }
    val passwordTextValue = rememberSaveable { mutableStateOf("") }
    val confirmPasswordTextValue = rememberSaveable { mutableStateOf("") }

    // Used for clearing text fields when switching EmailAuthMode changes
    val textValues = listOf(
        displayNameValue,
        emailTextValue,
        passwordTextValue,
        confirmPasswordTextValue
    )

    val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
    val isLoading = authState is AuthState.Loading
    val authCredentialForLinking = remember { credentialForLinking }
    val errorMessage =
        if (authState is AuthState.Error) (authState as AuthState.Error).exception.message else null
    val resetLinkSent = authState is AuthState.PasswordResetLinkSent
    val emailSignInLinkSent = authState is AuthState.EmailSignInLinkSent

    LaunchedEffect(authState) {
        Log.d("EmailAuthScreen", "Current state: $authState")
        when (val state = authState) {
            is AuthState.Success -> {
                state.result?.let { result ->
                    onSuccess(result)
                }
            }

            is AuthState.Error -> {
                val exception = AuthException.from(state.exception)
                onError(exception)
                dialogController?.showErrorDialog(
                    exception = exception,
                    onRetry = { ex ->
                        when (ex) {
                            is AuthException.InvalidCredentialsException -> {
                                // User can retry sign in with corrected credentials
                            }

                            is AuthException.EmailAlreadyInUseException -> {
                                // Switch to sign-in mode
                                mode.value = EmailAuthMode.SignIn
                            }

                            else -> Unit
                        }
                    },
                    onDismiss = {
                        // Dialog dismissed
                    }
                )
            }

            is AuthState.Cancelled -> {
                onCancel()
            }

            else -> Unit
        }
    }

    val state = EmailAuthContentState(
        mode = mode.value,
        displayName = displayNameValue.value,
        email = emailTextValue.value,
        password = passwordTextValue.value,
        confirmPassword = confirmPasswordTextValue.value,
        isLoading = isLoading,
        error = errorMessage,
        resetLinkSent = resetLinkSent,
        emailSignInLinkSent = emailSignInLinkSent,
        onEmailChange = { email ->
            emailTextValue.value = email
        },
        onPasswordChange = { password ->
            passwordTextValue.value = password
        },
        onConfirmPasswordChange = { confirmPassword ->
            confirmPasswordTextValue.value = confirmPassword
        },
        onDisplayNameChange = { displayName ->
            displayNameValue.value = displayName
        },
        onSignInClick = {
            coroutineScope.launch {
                try {
                    authUI.signInWithEmailAndPassword(
                        context = context,
                        config = configuration,
                        email = emailTextValue.value,
                        password = passwordTextValue.value,
                        credentialForLinking = authCredentialForLinking,
                    )
                } catch (e: Exception) {
                    onError(AuthException.from(e))
                }
            }
        },
        onSignInEmailLinkClick = {
            coroutineScope.launch {
                try {
                    if (emailLinkFromDifferentDevice != null) {
                        authUI.signInWithEmailLink(
                            context = context,
                            config = configuration,
                            provider = provider,
                            email = emailTextValue.value,
                            emailLink = emailLinkFromDifferentDevice,
                        )
                    } else {
                        authUI.sendSignInLinkToEmail(
                            context = context,
                            config = configuration,
                            provider = provider,
                            email = emailTextValue.value,
                            credentialForLinking = authCredentialForLinking,
                        )
                    }
                } catch (e: Exception) {
                    onError(AuthException.from(e))
                }
            }
        },
        onSignUpClick = {
            coroutineScope.launch {
                try {
                    authUI.createOrLinkUserWithEmailAndPassword(
                        context = context,
                        config = configuration,
                        provider = provider,
                        name = displayNameValue.value,
                        email = emailTextValue.value,
                        password = passwordTextValue.value,
                    )
                } catch (e: Exception) {

                }
            }
        },
        onSendResetLinkClick = {
            coroutineScope.launch {
                try {
                    authUI.sendPasswordResetEmail(
                        email = emailTextValue.value,
                        actionCodeSettings = configuration.passwordResetActionCodeSettings,
                    )
                } catch (e: Exception) {

                }
            }
        },
        onGoToSignUp = {
            textValues.forEach { it.value = "" }
            mode.value = EmailAuthMode.SignUp
        },
        onGoToSignIn = {
            textValues.forEach { it.value = "" }
            mode.value = EmailAuthMode.SignIn
        },
        onGoToResetPassword = {
            textValues.forEach { it.value = "" }
            mode.value = EmailAuthMode.ResetPassword
        },
        onGoToEmailLinkSignIn = {
            textValues.forEach { it.value = "" }
            mode.value = EmailAuthMode.EmailLinkSignIn
        },
    )

    if (content != null) {
        content(state)
    } else {
        DefaultEmailAuthContent(
            configuration = configuration,
            state = state,
            onCancel = onCancel
        )
    }
}

@Composable
private fun DefaultEmailAuthContent(
    configuration: AuthUIConfiguration,
    state: EmailAuthContentState,
    onCancel: () -> Unit,
) {
    when (state.mode) {
        EmailAuthMode.SignIn -> {
            SignInUI(
                configuration = configuration,
                email = state.email,
                isLoading = state.isLoading,
                emailSignInLinkSent = state.emailSignInLinkSent,
                password = state.password,
                onEmailChange = state.onEmailChange,
                onPasswordChange = state.onPasswordChange,
                onSignInClick = state.onSignInClick,
                onGoToSignUp = state.onGoToSignUp,
                onGoToResetPassword = state.onGoToResetPassword,
                onGoToEmailLinkSignIn = state.onGoToEmailLinkSignIn,
                onNavigateBack = onCancel
            )
        }

        EmailAuthMode.EmailLinkSignIn -> {
            SignInEmailLinkUI(
                configuration = configuration,
                email = state.email,
                isLoading = state.isLoading,
                emailSignInLinkSent = state.emailSignInLinkSent,
                onEmailChange = state.onEmailChange,
                onSignInWithEmailLink = state.onSignInEmailLinkClick,
                onGoToSignIn = state.onGoToSignIn,
                onGoToResetPassword = state.onGoToResetPassword,
                onNavigateBack = onCancel
            )
        }

        EmailAuthMode.SignUp -> {
            SignUpUI(
                configuration = configuration,
                isLoading = state.isLoading,
                displayName = state.displayName,
                email = state.email,
                password = state.password,
                confirmPassword = state.confirmPassword,
                onDisplayNameChange = state.onDisplayNameChange,
                onEmailChange = state.onEmailChange,
                onPasswordChange = state.onPasswordChange,
                onConfirmPasswordChange = state.onConfirmPasswordChange,
                onSignUpClick = state.onSignUpClick,
                onGoToSignIn = state.onGoToSignIn
            )
        }

        EmailAuthMode.ResetPassword -> {
            ResetPasswordUI(
                configuration = configuration,
                isLoading = state.isLoading,
                email = state.email,
                resetLinkSent = state.resetLinkSent,
                onEmailChange = state.onEmailChange,
                onSendResetLink = state.onSendResetLinkClick,
                onGoToSignIn = state.onGoToSignIn
            )
        }
    }
}
