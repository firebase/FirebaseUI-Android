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

import com.firebase.ui.auth.rememberAuthFlowScope
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
 * Switching modes keeps whatever address the user has typed; only the password, its confirmation
 * and the display name are cleared.
 *
 * @param mode An enum representing the current UI mode. Use a when expression on this to render
 * the correct screen. Inside [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen] every mode is its
 * own navigation destination and this mirrors the active one.
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
 * @param onGoToSignUp A callback to switch the UI to the SignUp mode. Inert when account creation
 * is not on offer: reauthentication, or new accounts disabled in the configuration or on the
 * provider.
 * @param onGoToSignIn A callback to switch the UI to the SignIn mode.
 * @param onGoToResetPassword A callback to switch the UI to the ResetPassword mode.
 * @param onGoToEmailLinkSignIn A callback to switch the UI to the EmailLinkSignIn mode. Inert when
 * email-link sign-in is not on offer: reauthentication, or a provider that does not enable it.
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
 * The mode can be driven from the outside — [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen]
 * gives every mode its own navigation destination and passes [mode] and [onNavigateToMode] — or
 * left to this composable, which then keeps the mode in local state.
 *
 * This composable never changes mode on its own in response to an error. Signing in with an
 * address that has no account leaves the user on the sign-in form rather than moving them to
 * sign-up; acting on an error is the host's job alone.
 *
 * @param mode The mode to render. When null this composable owns the mode itself, starting at
 * [EmailAuthMode.EmailLinkSignIn] for a cross-device email link and [EmailAuthMode.SignIn]
 * otherwise. Goes together with [onNavigateToMode]: passing either without the other is rejected.
 * @param onNavigateToMode Invoked instead of changing local state when the user switches mode,
 * with the address currently typed so the host can carry it over. Goes together with [mode].
 * @param onEmailTyped Invoked with the address as the user edits it, so a host driving [mode] has
 * the live value once this step is disposed. Fires per keystroke, so a host must keep what it
 * hears out of anything read during composition.
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
    prefillEmail: String? = null,
    mode: EmailAuthMode? = null,
    onNavigateToMode: ((mode: EmailAuthMode, email: String) -> Unit)? = null,
    onEmailTyped: (String) -> Unit = {},
    /**
     * Where a consumed one-off notification leaves the flow. Null retracts to [AuthState.Idle];
     * reauthentication passes its own, returning the request to provider selection. Explicit
     * because this screen no longer decides which flow it is in by reading a relabelled state.
     */
    onNotificationConsumed: (() -> Unit)? = null,
    content: @Composable ((EmailAuthContentState) -> Unit)? = null,
) {
    require((mode == null) == (onNavigateToMode == null)) {
        "EmailAuthScreen's mode and onNavigateToMode go together: pass both to drive the mode " +
                "from outside, or neither to let the screen own it. Got mode=$mode and " +
                "onNavigateToMode=${if (onNavigateToMode == null) "null" else "a callback"}."
    }
    val provider = configuration.providers.filterIsInstance<AuthProvider.Email>().first()
    val stringProvider = LocalAuthUIStringProvider.current
    val dialogController = LocalTopLevelDialogController.current
    val coroutineScope = rememberCoroutineScope()

    val initialMode = if (emailLinkFromDifferentDevice != null && provider.isEmailLinkSignInEnabled) {
        EmailAuthMode.EmailLinkSignIn
    } else {
        EmailAuthMode.SignIn
    }
    // Allocated unconditionally: a rememberSaveable must not sit behind a branch on a parameter.
    val localMode = rememberSaveable { mutableStateOf(initialMode) }
    val currentMode = mode ?: localMode.value
    val displayNameValue = rememberSaveable { mutableStateOf("") }
    val emailTextValue = rememberSaveable { mutableStateOf(prefillEmail ?: "") }
    val passwordTextValue = rememberSaveable { mutableStateOf("") }
    val confirmPasswordTextValue = rememberSaveable { mutableStateOf("") }

    val isEmailLocked = remember(prefillEmail, configuration.isReauthenticationMode) {
        configuration.isReauthenticationMode && !prefillEmail.isNullOrEmpty()
    }

    val isSignUpOffered = configuration.isEmailSignUpOffered()
    val isEmailLinkSignInOffered = configuration.isEmailLinkSignInOffered()

    // Cleared on a mode change; the address the user typed is deliberately not among them.
    val secretTextValues = remember {
        listOf(
            displayNameValue,
            passwordTextValue,
            confirmPasswordTextValue
        )
    }

    val authFlowScope = rememberAuthFlowScope(authUI, configuration)
    // Under a reauthentication request this is that request's phase, not the host's state.
    val authState by authFlowScope.state
    val isLoading = authState is AuthState.Loading ||
        authState is AuthState.Reauthentication.Authenticating
    val authCredentialForLinking = remember { credentialForLinking }
    val errorMessage = when (val state = authState) {
        is AuthState.Error -> state.exception.message
        is AuthState.Reauthentication.AttemptFailed -> state.exception.message
        else -> null
    }

    // Latched: these states are consumed to Idle below, so deriving from authState closes dialogs.
    var resetLinkSentLocal by rememberSaveable { mutableStateOf(false) }
    var emailSignInLinkSentLocal by rememberSaveable { mutableStateOf(false) }

    val retrievedCredential = remember { mutableStateOf<Pair<String, String>?>(null) }

    /**
     * The single way this screen changes mode, so every guard lives in one place. Hosted, it asks
     * the host to navigate and hands over the typed address; unhosted, it swaps local state and
     * clears the mode-specific fields itself.
     *
     * Only ever called for a switch the user asked for; error recovery is the host's.
     */
    fun goToMode(target: EmailAuthMode) {
        if (target == EmailAuthMode.SignUp && !isSignUpOffered) return
        if (target == EmailAuthMode.EmailLinkSignIn && !isEmailLinkSignInOffered) return
        if (onNavigateToMode != null) {
            onNavigateToMode(target, emailTextValue.value)
            return
        }
        // Unhosted, the same composition renders the target, so stale "link sent" latches must go.
        when (target) {
            EmailAuthMode.ResetPassword -> resetLinkSentLocal = false
            EmailAuthMode.SignIn, EmailAuthMode.EmailLinkSignIn -> emailSignInLinkSentLocal = false
            EmailAuthMode.SignUp -> Unit
        }
        secretTextValues.forEach { it.value = "" }
        localMode.value = target
    }

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
                // Hosted, the host already shows this error with its own recovery actions.
                if (onNavigateToMode == null) {
                    dialogController?.showErrorDialog(
                        exception = exception,
                        errorState = state,
                        onRetry = null,
                        onRecover = null,
                    )
                }
                // Consumed so the error doesn't leak into a freshly created screen.
                authFlowScope.emit(AuthState.Idle)
            }

            is AuthState.Cancelled -> {
                onCancel()
                authFlowScope.emit(AuthState.Idle)
            }

            is AuthState.PasswordResetLinkSent -> {
                resetLinkSentLocal = true
                onNotificationConsumed?.invoke() ?: authFlowScope.emit(AuthState.Idle)
            }

            is AuthState.EmailSignInLinkSent -> {
                emailSignInLinkSentLocal = true
                onNotificationConsumed?.invoke() ?: authFlowScope.emit(AuthState.Idle)
            }

            else -> Unit
        }
    }

    val state = EmailAuthContentState(
        mode = currentMode,
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
                onEmailTyped(email)
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
                    val isUsingRetrievedCredential = retrievedCredential.value?.let { (email, password) ->
                        email == emailTextValue.value && password == passwordTextValue.value
                    } ?: false

                    authFlowScope.signInWithEmailAndPassword(
                        context = context,
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
                        authFlowScope.signInWithEmailLink(
                            context = context,
                            provider = provider,
                            email = emailTextValue.value,
                            emailLink = emailLinkFromDifferentDevice,
                        )
                    } else {
                        authFlowScope.sendSignInLinkToEmail(
                            context = context,
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
                    authFlowScope.createOrLinkUserWithEmailAndPassword(
                        context = context,
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
                    authFlowScope.sendPasswordResetEmail(
                        email = emailTextValue.value,
                        actionCodeSettings = configuration.passwordResetActionCodeSettings,
                    )
                } catch (e: Exception) {
                    onError(AuthException.from(e, stringProvider))
                }
            }
        },
        onGoToSignUp = { goToMode(EmailAuthMode.SignUp) },
        onGoToSignIn = { goToMode(EmailAuthMode.SignIn) },
        // Offered during reauthentication too; blocking it strands a user who forgot their password.
        onGoToResetPassword = { goToMode(EmailAuthMode.ResetPassword) },
        onGoToEmailLinkSignIn = { goToMode(EmailAuthMode.EmailLinkSignIn) },
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
