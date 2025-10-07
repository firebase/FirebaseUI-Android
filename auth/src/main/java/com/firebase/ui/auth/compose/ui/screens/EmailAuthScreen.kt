package com.firebase.ui.auth.compose.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.auth_provider.createOrLinkUserWithEmailAndPassword
import com.firebase.ui.auth.compose.configuration.auth_provider.sendPasswordResetEmail
import com.firebase.ui.auth.compose.configuration.auth_provider.signInWithEmailAndPassword
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
    val onGoToSignUp: () -> Unit,
    val onGoToSignIn: () -> Unit,
    val onGoToResetPassword: () -> Unit,
)

/**
 * A stateful composable that manages the logic for all email-based authentication flows,
 * including sign-in, sign-up, and password reset. It exposes the state for the current mode to
 * a custom UI via a trailing lambda (slot), allowing for complete visual customization.
 *
 * @param provider The configuration object contains rules for email auth, such as whether a
 * display name is required.
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
    provider: AuthProvider.Email,
    onSuccess: (AuthResult) -> Unit,
    onError: (AuthException) -> Unit,
    onCancel: () -> Unit,
    content: @Composable ((EmailAuthContentState) -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()

    val mode = rememberSaveable { mutableStateOf(EmailAuthMode.SignIn) }
    val displayNameValue = rememberSaveable { mutableStateOf("") }
    val emailTextValue = rememberSaveable { mutableStateOf("") }
    val passwordTextValue = rememberSaveable { mutableStateOf("") }
    val confirmPasswordTextValue = rememberSaveable { mutableStateOf("") }

    val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)

    LaunchedEffect(authState) {
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

    val errorMessage =
        if (authState is AuthState.Error) (authState as AuthState.Error).exception.message else null

    val state = EmailAuthContentState(
        mode = mode.value,
        displayName = displayNameValue.value,
        email = emailTextValue.value,
        password = passwordTextValue.value,
        confirmPassword = confirmPasswordTextValue.value,
        isLoading = authState is AuthState.Loading,
        error = errorMessage,
        resetLinkSent = false,
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
                authUI.signInWithEmailAndPassword(
                    context = context,
                    config = configuration,
                    email = emailTextValue.value,
                    password = passwordTextValue.value,
                    credentialForLinking = null,
                )
            }
        },
        onSignUpClick = {
            coroutineScope.launch {
                authUI.createOrLinkUserWithEmailAndPassword(
                    context = context,
                    config = configuration,
                    provider = provider,
                    name = displayNameValue.value,
                    email = emailTextValue.value,
                    password = passwordTextValue.value,
                )
            }
        },
        onSendResetLinkClick = {
            coroutineScope.launch {
                authUI.sendPasswordResetEmail(
                    email = emailTextValue.value,
                    actionCodeSettings = null,
                )
            }
        },
        onGoToSignUp = {
            mode.value = EmailAuthMode.SignUp
        },
        onGoToSignIn = {
            mode.value = EmailAuthMode.SignIn
        },
        onGoToResetPassword = {
            mode.value = EmailAuthMode.ResetPassword
        }
    )

    content?.invoke(state)
}

//@Preview
//@Composable
//internal fun PreviewEmailAuthScreen() {
//    val applicationContext = LocalContext.current
//    val provider = AuthProvider.Email(
//        isDisplayNameRequired = true,
//        isEmailLinkSignInEnabled = false,
//        isEmailLinkForceSameDeviceEnabled = true,
//        actionCodeSettings = null,
//        isNewAccountsAllowed = true,
//        minimumPasswordLength = 8,
//        passwordValidationRules = listOf()
//    )
//
//    AuthUITheme {
//        EmailAuthScreen(
//            context = applicationContext,
//            configuration = authUIConfiguration {
//                context = applicationContext
//                providers { provider(provider) }
//                tosUrl = ""
//                privacyPolicyUrl = ""
//            },
//            authUI = null,
//            provider = provider,
//            onSuccess = {
//
//            },
//            onError = {
//
//            },
//            onCancel = {
//
//            },
//        ) { state ->
//            when (state.mode) {
//                EmailAuthMode.SignIn -> {
//                    SignInUI(
//                        configuration = authUIConfiguration {
//                            context = applicationContext
//                            providers { provider(provider) }
//                            tosUrl = ""
//                            privacyPolicyUrl = ""
//                        },
//                        provider = provider,
//                        email = state.email,
//                        isLoading = false,
//                        password = state.password,
//                        onEmailChange = state.onEmailChange,
//                        onPasswordChange = state.onPasswordChange,
//                        onSignInClick = state.onSignInClick,
//                        onGoToSignUp = state.onGoToSignUp,
//                        onGoToResetPassword = state.onGoToResetPassword,
//                    )
//                }
//
//                EmailAuthMode.SignUp -> {
//                    SignUpUI(
//                        configuration = authUIConfiguration {
//                            context = applicationContext
//                            providers { provider(provider) }
//                            tosUrl = ""
//                            privacyPolicyUrl = ""
//                        },
//                        isLoading = state.isLoading,
//                        displayName = state.displayName,
//                        email = state.email,
//                        password = state.password,
//                        confirmPassword = state.confirmPassword,
//                        onDisplayNameChange = state.onDisplayNameChange,
//                        onEmailChange = state.onEmailChange,
//                        onPasswordChange = state.onPasswordChange,
//                        onConfirmPasswordChange = state.onConfirmPasswordChange,
//                        onSignUpClick = state.onSignUpClick,
//                        onGoToSignIn = state.onGoToSignIn,
//                    )
//                }
//
//                EmailAuthMode.ResetPassword -> {
//                    ResetPasswordUI(
//                        configuration = authUIConfiguration {
//                            context = applicationContext
//                            providers { provider(provider) }
//                            tosUrl = ""
//                            privacyPolicyUrl = ""
//                        },
//                        isLoading = state.isLoading,
//                        email = state.email,
//                        resetLinkSent = state.resetLinkSent,
//                        onEmailChange = state.onEmailChange,
//                        onSendResetLink = state.onSendResetLinkClick,
//                        onGoToSignIn = state.onGoToSignIn
//                    )
//                }
//            }
//        }
//    }
//}
