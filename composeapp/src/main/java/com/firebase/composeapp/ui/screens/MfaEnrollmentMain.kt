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

package com.firebase.composeapp.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.MfaConfiguration
import com.firebase.ui.auth.compose.configuration.MfaFactor
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.compose.mfa.getHelperText
import com.firebase.ui.auth.compose.mfa.getTitle
import com.firebase.ui.auth.compose.mfa.toMfaErrorMessage
import com.firebase.ui.auth.compose.ui.components.CountrySelector
import com.firebase.ui.auth.compose.ui.screens.MfaEnrollmentScreen
import com.firebase.ui.auth.compose.ui.screens.phone.EnterPhoneNumberUI
import com.firebase.ui.auth.compose.ui.screens.phone.EnterVerificationCodeUI
import com.firebase.composeapp.ui.components.ReauthenticationDialog
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.auth.TotpMultiFactorInfo

@Composable
fun MfaEnrollmentMain(
    context: Context,
    authUI: FirebaseAuthUI,
    user: FirebaseUser,
    authConfiguration: AuthUIConfiguration,
    mfaConfiguration: MfaConfiguration,
    onComplete: () -> Unit,
    onSkip: () -> Unit = {},
) {
    val stringProvider = DefaultAuthUIStringProvider(LocalContext.current)
    val snackbarHostState = remember { SnackbarHostState() }
    val currentError = remember { androidx.compose.runtime.mutableStateOf<Exception?>(null) }
    val showReauthDialog = remember { androidx.compose.runtime.mutableStateOf(false) }
    val retryAction = remember { androidx.compose.runtime.mutableStateOf<(() -> Unit)?>(null) }
    val successMessage = remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val reauthErrorMessage = remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    // Show error in snackbar when error occurs
    LaunchedEffect(currentError.value) {
        currentError.value?.let { exception ->
            // Don't show snackbar for recent login required - we'll show re-auth dialog instead
            if (exception !is FirebaseAuthRecentLoginRequiredException) {
                val errorMessage = exception.toMfaErrorMessage(stringProvider)
                snackbarHostState.showSnackbar(errorMessage)
            }
            currentError.value = null // Clear error after showing
        }
    }

    // Show success message after re-authentication
    LaunchedEffect(successMessage.value) {
        successMessage.value?.let { message ->
            snackbarHostState.showSnackbar(message)
            successMessage.value = null
        }
    }

    // Show re-auth error message
    LaunchedEffect(reauthErrorMessage.value) {
        reauthErrorMessage.value?.let { message ->
            snackbarHostState.showSnackbar(message)
            reauthErrorMessage.value = null
        }
    }

    // Show re-authentication dialog when needed
    if (showReauthDialog.value) {
        ReauthenticationDialog(
            user = user,
            onDismiss = {
                showReauthDialog.value = false
                retryAction.value = null
            },
            onSuccess = {
                showReauthDialog.value = false
                // Trigger success message
                successMessage.value = "Identity verified. Please try your action again."
                retryAction.value = null
            },
            onError = { exception ->
                android.util.Log.e("MfaEnrollmentMain", "Re-authentication failed", exception)
                // Trigger error message
                reauthErrorMessage.value = when {
                    exception.message?.contains("password", ignoreCase = true) == true ->
                        "Incorrect password. Please try again."
                    else -> "Re-authentication failed. Please try again."
                }
            }
        )
    }

    MfaEnrollmentScreen(
        user = user,
        auth = authUI.auth,
        configuration = mfaConfiguration,
        onComplete = onComplete,
        onSkip = onSkip,
        onError = { exception ->
            android.util.Log.e("MfaEnrollmentMain", "MFA enrollment error", exception)

            // Check if re-authentication is required
            if (exception is FirebaseAuthRecentLoginRequiredException) {
                showReauthDialog.value = true
                // Store the retry action - we'll need to trigger it manually from state
                // For now, we'll just show the dialog and let the user know to try again
            } else if (exception is FirebaseAuthException &&
                       exception.message?.contains("already enrolled", ignoreCase = true) == true) {
                // Handle "already enrolled" error with a friendlier message
                currentError.value = Exception("This authentication method is already enrolled. Please go back to remove it first or choose a different method.")
            } else {
                currentError.value = exception
            }
        }
    ) { state ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            // Step-specific UI - EnterPhoneNumberUI and EnterVerificationCodeUI have their own Scaffold
            when (state.step) {
            MfaEnrollmentStep.SelectFactor -> {
                SelectFactorUI(
                    availableFactors = state.availableFactors,
                    enrolledFactors = state.enrolledFactors,
                    onFactorSelected = state.onFactorSelected,
                    onUnenrollFactor = state.onUnenrollFactor,
                    onSkipClick = state.onSkipClick,
                    isLoading = state.isLoading,
                    error = state.error
                )
            }

            MfaEnrollmentStep.ConfigureSms -> {
                state.selectedCountry?.let { country ->
                    val stringProvider = DefaultAuthUIStringProvider(LocalContext.current)
                    EnterPhoneNumberUI(
                        configuration = authConfiguration,
                        isLoading = state.isLoading,
                        phoneNumber = state.phoneNumber,
                        selectedCountry = country,
                        onPhoneNumberChange = state.onPhoneNumberChange,
                        onCountrySelected = state.onCountrySelected,
                        onSendCodeClick = state.onSendSmsCodeClick,
                        title = stringProvider.mfaEnrollmentEnterPhoneNumber
                    )
                }
            }

            MfaEnrollmentStep.ConfigureTotp -> {
                ConfigureTotpUI(
                    totpSecret = state.totpSecret?.sharedSecretKey,
                    totpQrCodeUrl = state.totpQrCodeUrl,
                    onContinueClick = state.onContinueToVerifyClick,
                    onBackClick = state.onBackClick,
                    isLoading = state.isLoading,
                    isValid = state.isValid,
                    error = state.error
                )
            }

            MfaEnrollmentStep.VerifyFactor -> {
                when (state.selectedFactor) {
                    MfaFactor.Sms -> {
                        val stringProvider = DefaultAuthUIStringProvider(LocalContext.current)
                        EnterVerificationCodeUI(
                            configuration = authConfiguration,
                            isLoading = state.isLoading,
                            verificationCode = state.verificationCode,
                            fullPhoneNumber = "${state.selectedCountry?.dialCode ?: ""}${state.phoneNumber}",
                            resendTimer = state.resendTimer,
                            onVerificationCodeChange = state.onVerificationCodeChange,
                            onVerifyCodeClick = state.onVerifyClick,
                            onResendCodeClick = state.onResendCodeClick ?: {},
                            onChangeNumberClick = state.onBackClick,
                            title = stringProvider.mfaEnrollmentVerifySmsCode
                        )
                    }
                    MfaFactor.Totp -> {
                        VerifyTotpUI(
                            verificationCode = state.verificationCode,
                            onVerificationCodeChange = state.onVerificationCodeChange,
                            onVerifyClick = state.onVerifyClick,
                            onBackClick = state.onBackClick,
                            isLoading = state.isLoading,
                            isValid = state.isValid,
                            error = state.error
                        )
                    }
                    null -> {}
                }
            }

            MfaEnrollmentStep.ShowRecoveryCodes -> {
                ShowRecoveryCodesUI(
                    recoveryCodes = state.recoveryCodes ?: emptyList(),
                    onDoneClick = state.onCodesSavedClick,
                    isLoading = state.isLoading,
                    error = state.error
                )
            }
        }

            // Snackbar for error messages
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun SelectFactorUI(
    availableFactors: List<MfaFactor>,
    enrolledFactors: List<MultiFactorInfo>,
    onFactorSelected: (MfaFactor) -> Unit,
    onUnenrollFactor: (MultiFactorInfo) -> Unit,
    onSkipClick: (() -> Unit)?,
    isLoading: Boolean,
    error: String?
) {
    // Filter out already enrolled factors
    val enrolledFactorIds = enrolledFactors.map {
        when (it) {
            is PhoneMultiFactorInfo -> MfaFactor.Sms
            is TotpMultiFactorInfo -> MfaFactor.Totp
            else -> null
        }
    }.filterNotNull().toSet()

    val factorsToEnroll = availableFactors.filter { it !in enrolledFactorIds }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Manage Two-Factor Authentication",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Add or remove authentication methods for your account",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            // Show enrolled factors
            if (enrolledFactors.isNotEmpty()) {
                Text(
                    text = "Active Methods",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                enrolledFactors.forEach { factorInfo ->
                    EnrolledFactorItem(
                        factorInfo = factorInfo,
                        onRemove = { onUnenrollFactor(factorInfo) },
                        enabled = !isLoading
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Show available factors to enroll
            if (factorsToEnroll.isNotEmpty()) {
                Text(
                    text = "Add New Method",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                factorsToEnroll.forEach { factor ->
                    Button(
                        onClick = { onFactorSelected(factor) },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when (factor) {
                                MfaFactor.Sms -> "Add SMS Authentication"
                                MfaFactor.Totp -> "Add Authenticator App"
                            }
                        )
                    }
                }
            }

            if (factorsToEnroll.isEmpty() && enrolledFactors.isNotEmpty()) {
                Text(
                    text = "All available authentication methods are enrolled",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            onSkipClick?.let {
                TextButton(
                    onClick = it,
                    enabled = !isLoading
                ) {
                    Text("Skip for now")
                }
            }
        }
    }
}

@Composable
private fun EnrolledFactorItem(
    factorInfo: MultiFactorInfo,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (factorInfo) {
                        is PhoneMultiFactorInfo -> "SMS Authentication"
                        is TotpMultiFactorInfo -> "Authenticator App"
                        else -> "Unknown Method"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = when (factorInfo) {
                        is PhoneMultiFactorInfo -> factorInfo.phoneNumber ?: "Phone"
                        is TotpMultiFactorInfo -> factorInfo.displayName ?: "TOTP"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Enrolled on ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(factorInfo.enrollmentTimestamp * 1000))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onRemove,
                enabled = enabled,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Remove")
            }
        }
    }
}

@Composable
private fun ConfigureTotpUI(
    totpSecret: String?,
    totpQrCodeUrl: String?,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean,
    isValid: Boolean,
    error: String?
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Setup Authenticator App",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Scan the QR code or enter the secret key in your authenticator app",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            totpSecret?.let { secret ->
                Text(
                    text = "Secret Key:",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = secret,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }

            totpQrCodeUrl?.let { url ->
                Text(
                    text = "Scan this with your authenticator app:",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "(QR code would be displayed here)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = onContinueClick,
                    enabled = !isLoading && isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
private fun VerifyTotpUI(
    verificationCode: String,
    onVerificationCodeChange: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean,
    isValid: Boolean,
    error: String?
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Verify Your Code",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Enter the code from your authenticator app",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            OutlinedTextField(
                value = verificationCode,
                onValueChange = onVerificationCodeChange,
                label = { Text("Verification code") },
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }

                Button(
                    onClick = onVerifyClick,
                    enabled = !isLoading && isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Verify")
                }
            }
        }
    }
}

@Composable
private fun ShowRecoveryCodesUI(
    recoveryCodes: List<String>,
    onDoneClick: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Recovery Codes",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Save these recovery codes in a safe place. You can use them to sign in if you lose access to your authentication method.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recoveryCodes.forEach { code ->
                    Text(
                        text = code,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = onDoneClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I've saved these codes")
            }
        }
    }
}
