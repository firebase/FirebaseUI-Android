package com.firebase.composeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.ui.screens.EmailAuthMode
import com.firebase.ui.auth.compose.ui.screens.EmailAuthScreen
import com.firebase.ui.auth.compose.ui.screens.ResetPasswordUI
import com.firebase.ui.auth.compose.ui.screens.SignInUI
import com.firebase.ui.auth.compose.ui.screens.SignUpUI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuthUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val applicationContext = LocalContext.current
                    val provider = AuthProvider.Email(
                        isDisplayNameRequired = true,
                        isEmailLinkSignInEnabled = false,
                        isEmailLinkForceSameDeviceEnabled = true,
                        actionCodeSettings = null,
                        isNewAccountsAllowed = true,
                        minimumPasswordLength = 8,
                        passwordValidationRules = listOf()
                    )

                    EmailAuthScreen(
                        provider = provider,
                        onSuccess = {

                        },
                        onError = {

                        },
                        onCancel = {

                        },
                    ) { state ->
                        when (state.mode) {
                            EmailAuthMode.SignIn -> {
                                SignInUI(
                                    configuration = authUIConfiguration {
                                        context = applicationContext
                                        providers { provider(provider) }
                                        tosUrl = ""
                                        privacyPolicyUrl = ""
                                    },
                                    provider = provider,
                                    email = state.email,
                                    isLoading = false,
                                    password = state.password,
                                    onEmailChange = state.onEmailChange,
                                    onPasswordChange = state.onPasswordChange,
                                    onSignInClick = state.onSignInClick,
                                    onGoToSignUp = state.onGoToSignUp,
                                    onGoToResetPassword = state.onGoToResetPassword,
                                )
                            }
                            EmailAuthMode.SignUp -> {
                                SignUpUI(
                                    configuration = authUIConfiguration {
                                        context = applicationContext
                                        providers { provider(provider) }
                                        tosUrl = ""
                                        privacyPolicyUrl = ""
                                    },
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
                                )
                            }
                            EmailAuthMode.ResetPassword -> {
                                ResetPasswordUI(
                                    email = state.email,
                                    resetLinkSent = state.resetLinkSent,
                                    onEmailChange = state.onEmailChange,
                                    onSendResetLink = state.onSendResetLinkClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}