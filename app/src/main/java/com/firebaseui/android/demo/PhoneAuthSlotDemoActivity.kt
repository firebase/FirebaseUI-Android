package com.firebaseui.android.demo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthScreen
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthStep
import com.google.firebase.auth.AuthResult

class PhoneAuthSlotDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authUI = FirebaseAuthUI.getInstance()
        val appContext = applicationContext

        val configuration = authUIConfiguration {
            context = appContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = "US",
                        allowedCountries = emptyList(),
                        smsCodeLength = 6,
                        timeout = 60L,
                        isInstantVerificationEnabled = true
                    )
                )
            }
        }

        setContent {
            CustomAuthUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        PhoneAuthDemo(
                            authUI = authUI,
                            configuration = configuration,
                            context = appContext
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneAuthDemo(
    authUI: FirebaseAuthUI,
    configuration: AuthUIConfiguration,
    context: android.content.Context
) {
    var currentUser by remember { mutableStateOf(authUI.getCurrentUser()) }

    LaunchedEffect(Unit) {
        authUI.authStateFlow().collect { _ ->
            currentUser = authUI.getCurrentUser()
        }
    }

    if (currentUser != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Phone Verified!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentUser?.phoneNumber ?: "Signed in",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { authUI.auth.signOut() }) {
                Text("Sign Out")
            }
        }
    } else {
        CompositionLocalProvider(LocalAuthUIStringProvider provides configuration.stringProvider) {
            PhoneAuthScreen(
                context = context,
                configuration = configuration,
                authUI = authUI,
                onSuccess = { result: AuthResult ->
                    Log.d("PhoneAuthSlotDemo", "Auth success: ${result.user?.uid}")
                },
                onError = { exception: AuthException ->
                    Log.e("PhoneAuthSlotDemo", "Auth error", exception)
                },
                onCancel = {
                    Log.d("PhoneAuthSlotDemo", "Auth cancelled")
                }
            ) { state: PhoneAuthContentState ->
                CustomPhoneAuthUI(state)
            }
        }
    }
}

@Composable
fun CustomPhoneAuthUI(state: PhoneAuthContentState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when (state.step) {
                PhoneAuthStep.EnterPhoneNumber -> "Phone Verification"
                PhoneAuthStep.EnterVerificationCode -> "Enter Code"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        state.error?.let { errorMessage ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        when (state.step) {
            PhoneAuthStep.EnterPhoneNumber -> EnterPhoneNumberUI(state)
            PhoneAuthStep.EnterVerificationCode -> EnterVerificationCodeUI(state)
        }
    }
}

@Composable
fun EnterPhoneNumberUI(state: PhoneAuthContentState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Enter your phone number to receive a verification code",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedCard(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.selectedCountry.flagEmoji} ${state.selectedCountry.dialCode}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = state.selectedCountry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = state.phoneNumber,
            onValueChange = state.onPhoneNumberChange,
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = state.onSendCodeClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.phoneNumber.isNotBlank()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Send Code")
            }
        }
    }
}

@Composable
fun EnterVerificationCodeUI(state: PhoneAuthContentState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "We sent a verification code to:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = state.fullPhoneNumber,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.verificationCode,
            onValueChange = state.onVerificationCodeChange,
            label = { Text("6-Digit Code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = state.onVerifyCodeClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.verificationCode.length == 6
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Verify Code")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = state.onChangeNumberClick) {
                Text("Change Number")
            }

            TextButton(
                onClick = state.onResendCodeClick,
                enabled = state.resendTimer == 0
            ) {
                Text(
                    if (state.resendTimer > 0) "Resend (${state.resendTimer}s)"
                    else "Resend Code"
                )
            }
        }
    }
}
