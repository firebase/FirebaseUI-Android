package com.firebase.composeapp.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.ui.screens.EmailAuthMode
import com.firebase.ui.auth.compose.ui.screens.EmailAuthScreen
import com.firebase.ui.auth.compose.ui.screens.ResetPasswordUI
import com.firebase.ui.auth.compose.ui.screens.SignInUI
import com.firebase.ui.auth.compose.ui.screens.SignUpUI
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.launch

@Composable
fun EmailAuthMain(
    context: Context,
    configuration: AuthUIConfiguration,
    authUI: FirebaseAuthUI,
    onSetupMfa: () -> Unit = {},
    credentialForLinking: AuthCredential? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)

    when (authState) {
        is AuthState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Authenticated User - (Success): ${authUI.getCurrentUser()?.email}",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSetupMfa
                ) {
                    Text("Setup MFA")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authUI.signOut(context)
                        }
                    }
                ) {
                    Text("Sign Out")
                }
            }
        }

        is AuthState.RequiresEmailVerification -> {
            val verificationState = authState as AuthState.RequiresEmailVerification
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Authenticated User - " +
                            "(RequiresEmailVerification): " +
                            "${verificationState.user.email}",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Please verify your email to continue.",
                    textAlign = TextAlign.Center,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                verificationState.user.sendEmailVerification()
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            android.util.Log.d("EmailAuthMain", "Verification email sent")
                                        } else {
                                            android.util.Log.e("EmailAuthMain", "Failed to send verification email", task.exception)
                                        }
                                    }
                            } catch (e: Exception) {
                                android.util.Log.e("EmailAuthMain", "Error sending verification email", e)
                            }
                        }
                    }
                ) {
                    Text("Send Verification Email")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                // Reload the user to refresh the authentication token
                                verificationState.user.reload().addOnCompleteListener { reloadTask ->
                                    if (reloadTask.isSuccessful) {
                                        // Force a token refresh to trigger the AuthStateListener
                                        verificationState.user.getIdToken(true).addOnCompleteListener { tokenTask ->
                                            if (tokenTask.isSuccessful) {
                                                val currentUser = authUI.getCurrentUser()
                                                android.util.Log.d("EmailAuthMain", "User reloaded. isEmailVerified: ${currentUser?.isEmailVerified}")
                                                // The AuthStateListener should fire automatically after token refresh
                                            } else {
                                                android.util.Log.e("EmailAuthMain", "Failed to refresh token", tokenTask.exception)
                                            }
                                        }
                                    } else {
                                        android.util.Log.e("EmailAuthMain", "Failed to reload user", reloadTask.exception)
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("EmailAuthMain", "Error reloading user", e)
                            }
                        }
                    }
                ) {
                    Text("Check Verification Status")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authUI.signOut(context)
                        }
                    }
                ) {
                    Text("Sign Out")
                }
            }
        }

        else -> {
            EmailAuthScreen(
                context = context,
                configuration = configuration,
                authUI = authUI,
                credentialForLinking = credentialForLinking,
                onSuccess = { result -> },
                onError = { exception -> },
                onCancel = { },
            ) { state ->
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
}