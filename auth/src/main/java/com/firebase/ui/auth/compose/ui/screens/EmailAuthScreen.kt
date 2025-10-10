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

package com.firebase.ui.auth.compose.ui.screens

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
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.auth_provider.createOrLinkUserWithEmailAndPassword
import com.firebase.ui.auth.compose.configuration.auth_provider.sendPasswordResetEmail
import com.firebase.ui.auth.compose.configuration.auth_provider.sendSignInLinkToEmail
import com.firebase.ui.auth.compose.configuration.auth_provider.signInWithEmailAndPassword
import com.firebase.ui.auth.compose.configuration.auth_provider.signInWithEmailLink
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.ui.components.ErrorRecoveryDialog
import com.firebase.ui.auth.compose.util.EmailLinkPersistenceManager
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.launch

enum class EmailAuthMode {
    SignIn,
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
    val onSignUpClick: () -> Unit,
    val onSendResetLinkClick: () -> Unit,
    val resetLinkSent: Boolean = false,
    val emailSignInLinkSent: Boolean = false,
    val onGoToSignUp: () -> Unit,
    val onGoToSignIn: () -> Unit,
    val onGoToResetPassword: () -> Unit,
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
    onSuccess: (AuthResult) -> Unit,
    onError: (AuthException) -> Unit,
    onCancel: () -> Unit,
    content: @Composable ((EmailAuthContentState) -> Unit)? = null,
) {
    val provider = configuration.providers.filterIsInstance<AuthProvider.Email>().first()
    val stringProvider = DefaultAuthUIStringProvider(context)
    val coroutineScope = rememberCoroutineScope()

    val mode = rememberSaveable { mutableStateOf(EmailAuthMode.SignIn) }
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
    val errorMessage =
        if (authState is AuthState.Error) (authState as AuthState.Error).exception.message else null
    val resetLinkSent = authState is AuthState.PasswordResetLinkSent
    val emailSignInLinkSent = authState is AuthState.EmailSignInLinkSent

    val isErrorDialogVisible =
        remember(authState) { mutableStateOf(authState is AuthState.Error) }

    LaunchedEffect(authState) {
        Log.d("EmailAuthScreen", "Current state: $authState")
        when (val state = authState) {
            is AuthState.Success -> {
                state.result?.let { result ->
                    onSuccess(result)
                }
            }

            is AuthState.Error -> {
                onError(AuthException.from(state.exception))
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
                    if (provider.isEmailLinkSignInEnabled) {
                        authUI.sendSignInLinkToEmail(
                            context = context,
                            config = configuration,
                            provider = provider,
                            email = emailTextValue.value,
                            credentialForLinking = null,
                        )
                    } else {
                        authUI.signInWithEmailAndPassword(
                            context = context,
                            config = configuration,
                            email = emailTextValue.value,
                            password = passwordTextValue.value,
                            credentialForLinking = null,
                        )
                    }
                } catch (e: Exception) {

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
        }
    )

    if (isErrorDialogVisible.value) {
        ErrorRecoveryDialog(
            error = when ((authState as AuthState.Error).exception) {
                is AuthException -> (authState as AuthState.Error).exception as AuthException
                else -> AuthException
                    .from((authState as AuthState.Error).exception)
            },
            stringProvider = stringProvider,
            onRetry = { exception ->
                when (exception) {
                    is AuthException.InvalidCredentialsException -> state.onSignInClick()
                    is AuthException.EmailAlreadyInUseException -> state.onGoToSignIn()
                }
                isErrorDialogVisible.value = false
            },
            onDismiss = {
                isErrorDialogVisible.value = false
            },
        )
    }

    content?.invoke(state)
}
