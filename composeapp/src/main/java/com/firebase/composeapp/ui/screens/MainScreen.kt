package com.firebase.composeapp.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.ui.screens.EmailAuthMode
import com.firebase.ui.auth.compose.ui.screens.EmailAuthScreen
import com.firebase.ui.auth.compose.ui.screens.ResetPasswordUI
import com.firebase.ui.auth.compose.ui.screens.sign_in.SignInUI
import com.firebase.ui.auth.compose.ui.screens.SignUpUI

@Composable
fun MainScreen(
    context: Context,
    configuration: AuthUIConfiguration,
    authUI: FirebaseAuthUI,
    provider: AuthProvider.Email,
) {
    val isAuthenticated = remember { mutableStateOf(false) }

    if (isAuthenticated.value) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Authenticated User: ${authUI.getCurrentUser()}")
        }
    } else {
        EmailAuthScreen(
            context = context,
            configuration = configuration,
            authUI = authUI,
            provider = provider,
            onSuccess = { result ->
                isAuthenticated.value = result.user != null
            },
            onError = { exception ->

            },
            onCancel = {

            },
        ) { state ->
            when (state.mode) {
                EmailAuthMode.SignIn -> {
                    SignInUI(
                        configuration = configuration,
                        provider = provider,
                        email = state.email,
                        isLoading = state.isLoading,
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
    }
}