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
import androidx.compose.runtime.setValue
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
import com.firebase.ui.auth.credentialmanager.PasswordCredentialCancelledException
import com.firebase.ui.auth.credentialmanager.PasswordCredentialException
import com.firebase.ui.auth.credentialmanager.PasswordCredentialHandler
import com.firebase.ui.auth.credentialmanager.PasswordCredentialNotFoundException
import com.firebase.ui.auth.ui.components.LocalTopLevelDialogController
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
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
 * @param isEmailLocked true when the library fixed [email] and it must not be edited. Render the
 * email field read-only while it is true.
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
    val onRetrievedCredential: (Pair<String, String>) -> Unit,
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
    val isEmailLocked: Boolean = false,
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
    onContinueWithProvider: (String) -> Unit = {},
    onSuccess: (AuthResult) -> Unit,
    onError: (AuthException) -> Unit,
    onCancel: () -> Unit,
    prefillEmail: String? = null,
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
    val emailTextValue = rememberSaveable { mutableStateOf(prefillEmail ?: "") }
    val passwordTextValue = rememberSaveable { mutableStateOf("") }
    val confirmPasswordTextValue = rememberSaveable { mutableStateOf("") }

    val isEmailLocked = remember(prefillEmail, configuration.isReauthenticationMode) {
        configuration.isReauthenticationMode && !prefillEmail.isNullOrEmpty()
    }

    val isSignUpOffered = provider.isNewAccountsAllowed &&
            configuration.isNewEmailAccountsAllowed &&
            !configuration.isReauthenticationMode

    // Used for clearing text fields when switching EmailAuthMode changes
    val textValues = remember {
        listOf(
            displayNameValue,
            emailTextValue,
            passwordTextValue,
            confirmPasswordTextValue
        )
    }

    val resetTextValues: () -> Unit = remember(textValues, isEmailLocked, prefillEmail) {
        {
            textValues.forEach { it.value = "" }
            if (isEmailLocked) {
                emailTextValue.value = prefillEmail.orEmpty()
            }
        }
    }

    val authState by remember(authUI) { authUI.authStateFlow() }.collectAsState(AuthState.Idle)
    val isLoading = authState is AuthState.Loading ||
        authState is AuthState.Reauthentication.Authenticating
    val authCredentialForLinking = remember { credentialForLinking }
    val errorMessage = when (val state = authState) {
        is AuthState.Error -> state.exception.message
        is AuthState.Reauthentication.AttemptFailed -> state.exception.message
        else -> null
    }

    // Latched locally since these get consumed (reset to Idle) below — deriving directly from
    // authState would close ResetPasswordUI/SignInEmailLinkUI's dialogs as soon as it resets.
    var resetLinkSentLocal by rememberSaveable { mutableStateOf(false) }
    var emailSignInLinkSentLocal by rememberSaveable { mutableStateOf(false) }

    // Track if credentials were retrieved from Credential Manager
    val retrievedCredential = remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(authState) {
        Log.d("EmailAuthScreen", "Current state: $authState")
        when (val state = authState) {
            is AuthState.Success -> {
                state.result?.let { result ->
                    onSuccess(result)
                }
            }

            is AuthState.Error -> {
                val exception = AuthException.from(state.exception, stringProvider)
                onError(exception)
                dialogController?.showErrorDialog(
                    exception = exception,
                    errorState = state,
                    // Every branch below is inert while reauthenticating, so an action button
                    // would only dismiss the dialog — leave it without one.
                    onRetry = if (configuration.isReauthenticationMode) {
                        null
                    } else {
                        { ex: AuthException ->
                            when (ex) {
                                is AuthException.UserNotFoundException -> {
                                    if (isSignUpOffered) {
                                        // User not found, but new accounts are allowed, switch to sign-up
                                        mode.value = EmailAuthMode.SignUp
                                    }
                                }

                                is AuthException.InvalidCredentialsException -> {
                                    // User can retry sign in with corrected credentials
                                }

                                is AuthException.EmailAlreadyInUseException -> {
                                    // Switch to sign-in mode
                                    mode.value = EmailAuthMode.SignIn
                                }

                                else -> Unit
                            }
                        }
                    },
                    onRecover = if (exception is AuthException.DifferentSignInMethodRequiredException) {
                        { ex ->
                            val differentProviderException =
                                ex as AuthException.DifferentSignInMethodRequiredException
                            if (differentProviderException.suggestedSignInMethod ==
                                EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD) {
                                mode.value = EmailAuthMode.EmailLinkSignIn
                            } else {
                                onContinueWithProvider(differentProviderException.suggestedSignInMethod)
                            }
                        }
                    } else {
                        null
                    },
                    onDismiss = {
                        // Dialog dismissed
                    }
                )
                // Consumed immediately so this doesn't leak to a freshly created screen.
                authUI.updateAuthState(AuthState.Idle)
            }

            is AuthState.Cancelled -> {
                onCancel()
                // Consumed so this doesn't leak to a freshly created screen.
                authUI.updateAuthState(AuthState.Idle)
            }

            is AuthState.PasswordResetLinkSent -> {
                resetLinkSentLocal = true
                authUI.updateAuthState(AuthState.Idle)
            }

            is AuthState.Reauthentication.PasswordResetLinkSent -> {
                resetLinkSentLocal = true
                authUI.updateReauthentication(state.requestId) { it.returnedToProviderSelection() }
            }

            is AuthState.EmailSignInLinkSent -> {
                emailSignInLinkSentLocal = true
                authUI.updateAuthState(AuthState.Idle)
            }

            is AuthState.Reauthentication.EmailSignInLinkSent -> {
                emailSignInLinkSentLocal = true
                authUI.updateReauthentication(state.requestId) { it.returnedToProviderSelection() }
            }

            else -> Unit
        }
    }

    val state = EmailAuthContentState(
        mode = mode.value,
        displayName = displayNameValue.value,
        email = emailTextValue.value,
        isEmailLocked = isEmailLocked,
        password = passwordTextValue.value,
        confirmPassword = confirmPasswordTextValue.value,
        isLoading = isLoading,
        error = errorMessage,
        resetLinkSent = resetLinkSentLocal,
        emailSignInLinkSent = emailSignInLinkSentLocal,
        onEmailChange = { email ->
            if (!isEmailLocked) {
                emailTextValue.value = email
            }
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
        onRetrievedCredential = { credential ->
            retrievedCredential.value = credential
        },
        onSignInClick = {
            coroutineScope.launch {
                try {
                    // Check if user is signing in with retrieved credentials
                    val isUsingRetrievedCredential = retrievedCredential.value?.let { (email, password) ->
                        email == emailTextValue.value && password == passwordTextValue.value
                    } ?: false

                    authUI.signInWithEmailAndPassword(
                        context = context,
                        config = configuration,
                        email = emailTextValue.value,
                        password = passwordTextValue.value,
                        credentialForLinking = authCredentialForLinking,
                        skipCredentialSave = isUsingRetrievedCredential
                    )
                } catch (e: Exception) {
                    onError(AuthException.from(e, stringProvider))
                }
            }
        },
        onSignInEmailLinkClick = {
            emailSignInLinkSentLocal = false
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
                    onError(AuthException.from(e, stringProvider))
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
                    onError(AuthException.from(e, stringProvider))
                }
            }
        },
        onSendResetLinkClick = {
            resetLinkSentLocal = false
            coroutineScope.launch {
                try {
                    authUI.sendPasswordResetEmail(
                        email = emailTextValue.value,
                        config = configuration,
                        actionCodeSettings = configuration.passwordResetActionCodeSettings,
                    )
                } catch (e: Exception) {
                    onError(AuthException.from(e, stringProvider))
                }
            }
        },
        onGoToSignUp = {
            if (isSignUpOffered) {
                resetTextValues()
                mode.value = EmailAuthMode.SignUp
            }
        },
        onGoToSignIn = {
            resetTextValues()
            mode.value = EmailAuthMode.SignIn
            emailSignInLinkSentLocal = false
        },
        onGoToResetPassword = {
            // Reauthentication is a modal confirmation of the signed-in account: diverting it to
            // an out-of-band email step strands the pending operation behind something it can't see.
            if (!configuration.isReauthenticationMode) {
                resetTextValues()
                mode.value = EmailAuthMode.ResetPassword
                resetLinkSentLocal = false
            }
        },
        onGoToEmailLinkSignIn = {
            if (!configuration.isReauthenticationMode) {
                resetTextValues()
                mode.value = EmailAuthMode.EmailLinkSignIn
                emailSignInLinkSentLocal = false
            }
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
                onRetrievedCredential = state.onRetrievedCredential,
                onSignInClick = state.onSignInClick,
                onGoToSignUp = state.onGoToSignUp,
                onGoToResetPassword = state.onGoToResetPassword,
                onGoToEmailLinkSignIn = state.onGoToEmailLinkSignIn,
                onNavigateBack = onCancel,
                isEmailLocked = state.isEmailLocked,
            )
        }

        EmailAuthMode.EmailLinkSignIn -> {
            SignInEmailLinkUI(
                configuration = configuration,
                email = state.email,
                isEmailLocked = state.isEmailLocked,
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
                onGoToSignIn = state.onGoToSignIn,
                onNavigateBack = onCancel,
                isEmailLocked = state.isEmailLocked,
            )
        }

        EmailAuthMode.ResetPassword -> {
            ResetPasswordUI(
                configuration = configuration,
                isLoading = state.isLoading,
                email = state.email,
                isEmailLocked = state.isEmailLocked,
                resetLinkSent = state.resetLinkSent,
                onEmailChange = state.onEmailChange,
                onSendResetLink = state.onSendResetLinkClick,
                onGoToSignIn = state.onGoToSignIn,
                onNavigateBack = onCancel
            )
        }
    }
}
