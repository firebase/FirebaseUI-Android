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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.MfaFactor
import com.firebase.ui.auth.compose.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.compose.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.compose.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.compose.mfa.toMfaErrorMessage
import com.firebase.ui.auth.compose.ui.components.QrCodeImage
import com.firebase.ui.auth.compose.ui.components.ReauthenticationDialog
import com.firebase.ui.auth.compose.ui.screens.phone.EnterPhoneNumberUI
import com.firebase.ui.auth.compose.ui.screens.phone.EnterVerificationCodeUI
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.auth.TotpMultiFactorInfo

@Composable
internal fun DefaultMfaEnrollmentContent(
    state: MfaEnrollmentContentState,
    authConfiguration: AuthUIConfiguration,
    user: FirebaseUser
) {
    val stringProvider = LocalAuthUIStringProvider.current
    val snackbarHostState = remember { SnackbarHostState() }
    val showReauthDialog = remember { mutableStateOf(false) }
    val reauthErrorMessage = remember { mutableStateOf<String?>(null) }
    val successMessage = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.error, state.exception) {
        val exception = state.exception
        when {
            exception is FirebaseAuthRecentLoginRequiredException -> {
                showReauthDialog.value = true
            }
            exception != null -> {
                snackbarHostState.showSnackbar(exception.toMfaErrorMessage(stringProvider))
            }
            !state.error.isNullOrBlank() -> {
                snackbarHostState.showSnackbar(state.error!!)
            }
        }
    }

    LaunchedEffect(successMessage.value) {
        successMessage.value?.let { message ->
            snackbarHostState.showSnackbar(message)
            successMessage.value = null
        }
    }

    LaunchedEffect(reauthErrorMessage.value) {
        reauthErrorMessage.value?.let { message ->
            snackbarHostState.showSnackbar(message)
            reauthErrorMessage.value = null
        }
    }

    if (showReauthDialog.value) {
        ReauthenticationDialog(
            user = user,
            onDismiss = {
                showReauthDialog.value = false
            },
            onSuccess = {
                showReauthDialog.value = false
                successMessage.value = stringProvider.identityVerifiedMessage
            },
            onError = { exception ->
                reauthErrorMessage.value = when {
                    exception.message?.contains("password", ignoreCase = true) == true ->
                        stringProvider.incorrectPasswordError
                    exception.message?.contains("network", ignoreCase = true) == true ->
                        stringProvider.noInternet
                    else -> stringProvider.reauthGenericError
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.step) {
            MfaEnrollmentStep.SelectFactor -> {
                SelectFactorUI(
                    availableFactors = state.availableFactors,
                    enrolledFactors = state.enrolledFactors,
                    onFactorSelected = state.onFactorSelected,
                    onUnenrollFactor = state.onUnenrollFactor,
                    onSkipClick = state.onSkipClick,
                    isLoading = state.isLoading,
                    error = state.error,
                    stringProvider = stringProvider
                )
            }

            MfaEnrollmentStep.ConfigureSms -> {
                state.selectedCountry?.let { country ->
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
                    error = state.error,
                    stringProvider = stringProvider
                )
            }

            MfaEnrollmentStep.VerifyFactor -> {
                when (state.selectedFactor) {
                    MfaFactor.Sms -> {
                        val formattedPhone =
                            "${state.selectedCountry?.dialCode ?: ""}${state.phoneNumber}"
                        EnterVerificationCodeUI(
                            configuration = authConfiguration,
                            isLoading = state.isLoading,
                            verificationCode = state.verificationCode,
                            fullPhoneNumber = formattedPhone,
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
                            error = state.error,
                            stringProvider = stringProvider
                        )
                    }

                    null -> Unit
                }
            }

            MfaEnrollmentStep.ShowRecoveryCodes -> {
                ShowRecoveryCodesUI(
                    recoveryCodes = state.recoveryCodes.orEmpty(),
                    onDoneClick = state.onCodesSavedClick,
                    isLoading = state.isLoading,
                    error = state.error,
                    stringProvider = stringProvider
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SelectFactorUI(
    availableFactors: List<MfaFactor>,
    enrolledFactors: List<MultiFactorInfo>,
    onFactorSelected: (MfaFactor) -> Unit,
    onUnenrollFactor: (MultiFactorInfo) -> Unit,
    onSkipClick: (() -> Unit)?,
    isLoading: Boolean,
    error: String?,
    stringProvider: com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
) {
    val enrolledFactorIds = enrolledFactors.mapNotNull {
        when (it) {
            is PhoneMultiFactorInfo -> MfaFactor.Sms
            is TotpMultiFactorInfo -> MfaFactor.Totp
            else -> null
        }
    }.toSet()

    val factorsToEnroll = availableFactors.filter { it !in enrolledFactorIds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringProvider.mfaManageFactorsTitle) }
            )
        }
    ) { innerPadding ->
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
                text = stringProvider.mfaManageFactorsDescription,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (enrolledFactors.isNotEmpty()) {
                Text(
                    text = stringProvider.mfaActiveMethodsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                enrolledFactors.forEach { factorInfo ->
                    EnrolledFactorItem(
                        factorInfo = factorInfo,
                        onRemove = { onUnenrollFactor(factorInfo) },
                        enabled = !isLoading,
                        stringProvider = stringProvider
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (factorsToEnroll.isNotEmpty()) {
                Text(
                    text = stringProvider.mfaAddNewMethodTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                factorsToEnroll.forEach { factor ->
                    Button(
                        onClick = { onFactorSelected(factor) },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (factor) {
                            MfaFactor.Sms -> Text(stringProvider.mfaStepConfigureSmsTitle)
                            MfaFactor.Totp -> Text(stringProvider.mfaStepConfigureTotpTitle)
                        }
                    }
                }
            } else if (enrolledFactors.isNotEmpty()) {
                Text(
                    text = stringProvider.mfaAllMethodsEnrolledMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            onSkipClick?.let {
                TextButton(
                    onClick = it,
                    enabled = !isLoading
                ) {
                    Text(stringProvider.skipAction)
                }
            }
        }
    }
}

@Composable
private fun EnrolledFactorItem(
    factorInfo: MultiFactorInfo,
    onRemove: () -> Unit,
    enabled: Boolean,
    stringProvider: com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
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
                        is PhoneMultiFactorInfo -> stringProvider.smsAuthenticationLabel
                        is TotpMultiFactorInfo -> stringProvider.totpAuthenticationLabel
                        else -> stringProvider.unknownMethodLabel
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = when (factorInfo) {
                        is PhoneMultiFactorInfo -> factorInfo.phoneNumber ?: stringProvider.smsAuthenticationLabel
                        is TotpMultiFactorInfo -> factorInfo.displayName ?: stringProvider.totpAuthenticationLabel
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringProvider.enrolledOnDateLabel(
                        java.text.SimpleDateFormat(
                            "MMM dd, yyyy",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(factorInfo.enrollmentTimestamp * 1000))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onRemove,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringProvider.removeAction)
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
    error: String?,
    stringProvider: com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
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
                text = stringProvider.mfaStepConfigureTotpTitle,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringProvider.setupAuthenticatorDescription,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            totpQrCodeUrl?.let { url ->
                QrCodeImage(
                    content = url,
                    size = 220.dp
                )
            }

            totpSecret?.let { secret ->
                Text(
                    text = stringProvider.secretKeyLabel,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = secret,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onBackClick,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringProvider.backAction)
                }

                Button(
                    onClick = onContinueClick,
                    enabled = !isLoading && isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringProvider.continueText)
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
    error: String?,
    stringProvider: com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
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
                text = stringProvider.mfaStepVerifyFactorTitle,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringProvider.mfaStepVerifyFactorTotpHelper,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                label = { Text(stringProvider.verificationCodeLabel) },
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
                    Text(stringProvider.backAction)
                }

                Button(
                    onClick = onVerifyClick,
                    enabled = !isLoading && isValid,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringProvider.verifyAction)
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
    error: String?,
    stringProvider: com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
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
                text = stringProvider.mfaStepShowRecoveryCodesTitle,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringProvider.mfaStepShowRecoveryCodesHelper,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )

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
                Text(stringProvider.recoveryCodesSavedAction)
            }
        }
    }
}
