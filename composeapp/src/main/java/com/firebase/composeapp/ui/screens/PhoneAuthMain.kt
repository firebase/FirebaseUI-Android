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
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.ui.screens.phone.EnterPhoneNumberUI
import com.firebase.ui.auth.compose.ui.screens.phone.EnterVerificationCodeUI
import com.firebase.ui.auth.compose.ui.screens.phone.PhoneAuthScreen
import com.firebase.ui.auth.compose.ui.screens.phone.PhoneAuthStep
import kotlinx.coroutines.launch

@Composable
fun PhoneAuthMain(
    context: Context,
    configuration: AuthUIConfiguration,
    authUI: FirebaseAuthUI,
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
                    "Authenticated User - (Success): ${authUI.getCurrentUser()?.phoneNumber}",
                    textAlign = TextAlign.Center
                )
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
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Authenticated User - " +
                            "(RequiresEmailVerification): " +
                            "${(authState as AuthState.RequiresEmailVerification).user.email}",
                    textAlign = TextAlign.Center
                )
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
            PhoneAuthScreen(
                context = context,
                configuration = configuration,
                authUI = authUI,
                onSuccess = { result -> },
                onError = { exception -> },
                onCancel = { },
            ) { state ->
                when (state.step) {
                    PhoneAuthStep.EnterPhoneNumber -> {
                        EnterPhoneNumberUI(
                            configuration = configuration,
                            isLoading = state.isLoading,
                            phoneNumber = state.phoneNumber,
                            useInstantVerificationEnabled = state.useInstantVerificationEnabled,
                            onUseInstantVerificationChange = state.onUseInstantVerificationChange,
                            selectedCountry = state.selectedCountry,
                            onPhoneNumberChange = state.onPhoneNumberChange,
                            onCountrySelected = state.onCountrySelected,
                            onSendCodeClick = state.onSendCodeClick,
                        )
                    }

                    PhoneAuthStep.EnterVerificationCode -> {
                        EnterVerificationCodeUI(
                            configuration = configuration,
                            isLoading = state.isLoading,
                            verificationCode = state.verificationCode,
                            fullPhoneNumber = state.fullPhoneNumber,
                            resendTimer = state.resendTimer,
                            onVerificationCodeChange = state.onVerificationCodeChange,
                            onVerifyCodeClick = state.onVerifyCodeClick,
                            onResendCodeClick = state.onResendCodeClick,
                            onChangeNumberClick = state.onChangeNumberClick,
                        )
                    }
                }
            }
        }
    }
}