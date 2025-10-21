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

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.MfaConfiguration
import com.firebase.ui.auth.compose.configuration.MfaFactor
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.data.CountryData
import com.firebase.ui.auth.compose.data.CountryUtils
import com.firebase.ui.auth.compose.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.compose.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.compose.mfa.SmsEnrollmentHandler
import com.firebase.ui.auth.compose.mfa.SmsEnrollmentSession
import com.firebase.ui.auth.compose.mfa.TotpEnrollmentHandler
import com.firebase.ui.auth.compose.mfa.TotpSecret
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A stateful composable that manages the Multi-Factor Authentication (MFA) enrollment flow.
 *
 * This screen handles all steps of MFA enrollment including factor selection, configuration,
 * verification, and recovery code display. It uses the provided handlers to communicate with
 * Firebase Authentication and exposes state through a content slot for custom UI rendering.
 *
 * **Enrollment Flow:**
 * 1. **SelectFactor** - User chooses between SMS or TOTP
 * 2. **ConfigureSms** or **ConfigureTotp** - User sets up their chosen factor
 * 3. **VerifyFactor** - User verifies with a code
 * 4. **ShowRecoveryCodes** - (Optional) User receives backup codes
 *
 * @param user The currently authenticated [FirebaseUser] to enroll in MFA
 * @param auth The [FirebaseAuth] instance
 * @param configuration MFA configuration controlling available factors and behavior
 * @param onComplete Callback invoked when enrollment completes successfully
 * @param onSkip Callback invoked when user skips enrollment (only if not required)
 * @param onError Callback invoked when an error occurs during enrollment
 * @param content A composable lambda that receives [MfaEnrollmentContentState] to render custom UI
 *
 * @since 10.0.0
 */
@Composable
fun MfaEnrollmentScreen(
    user: FirebaseUser,
    auth: FirebaseAuth,
    configuration: MfaConfiguration,
    authConfiguration: AuthUIConfiguration? = null,
    onComplete: () -> Unit,
    onSkip: () -> Unit = {},
    onError: (Exception) -> Unit = {},
    content: @Composable ((MfaEnrollmentContentState) -> Unit)? = null
) {
    val activity = requireNotNull(LocalActivity.current) {
        "MfaEnrollmentScreen must be used within an Activity context for SMS verification"
    }
    val coroutineScope = rememberCoroutineScope()

    val smsHandler = remember(activity, auth, user) { SmsEnrollmentHandler(activity, auth, user) }
    val totpHandler = remember(auth, user) { TotpEnrollmentHandler(auth, user) }

    val currentStep = rememberSaveable { mutableStateOf(MfaEnrollmentStep.SelectFactor) }
    val selectedFactor = rememberSaveable { mutableStateOf<MfaFactor?>(null) }
    val isLoading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }
    val lastException = remember { mutableStateOf<Exception?>(null) }
    val enrolledFactors = remember { mutableStateOf(user.multiFactor.enrolledFactors) }

    val phoneNumber = rememberSaveable { mutableStateOf("") }
    val selectedCountry = remember { mutableStateOf(CountryUtils.getDefaultCountry()) }
    val smsSession = remember { mutableStateOf<SmsEnrollmentSession?>(null) }

    val totpSecret = remember { mutableStateOf<TotpSecret?>(null) }
    val totpQrCodeUrl = remember { mutableStateOf<String?>(null) }

    val verificationCode = rememberSaveable { mutableStateOf("") }

    val recoveryCodes = remember { mutableStateOf<List<String>?>(null) }

    val resendTimerSeconds = rememberSaveable { mutableIntStateOf(0) }

    val phoneAuthConfiguration = remember(authConfiguration) {
        authConfiguration ?: authUIConfiguration {
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = null
                    )
                )
            }
        }
    }

    // Handle resend timer countdown
    LaunchedEffect(resendTimerSeconds.intValue) {
        if (resendTimerSeconds.intValue > 0) {
            delay(1000)
            resendTimerSeconds.intValue--
        }
    }

    LaunchedEffect(Unit) {
        if (configuration.allowedFactors.size == 1) {
            selectedFactor.value = configuration.allowedFactors.first()
            when (selectedFactor.value) {
                MfaFactor.Sms -> currentStep.value = MfaEnrollmentStep.ConfigureSms
                MfaFactor.Totp -> {
                    currentStep.value = MfaEnrollmentStep.ConfigureTotp
                    isLoading.value = true
                    try {
                        val secret = totpHandler.generateSecret()
                        totpSecret.value = secret
                        totpQrCodeUrl.value = secret.generateQrCodeUrl(
                            accountName = user.email ?: user.phoneNumber ?: "User",
                            issuer = auth.app.name
                        )
                        error.value = null
                        lastException.value = null
                    } catch (e: Exception) {
                        error.value = e.message
                        lastException.value = e
                        onError(e)
                    } finally {
                        isLoading.value = false
                    }
                }
                null -> {}
            }
        }
    }

    val state = MfaEnrollmentContentState(
        step = currentStep.value,
        isLoading = isLoading.value,
        error = error.value,
        exception = lastException.value,
        onBackClick = {
            when (currentStep.value) {
                MfaEnrollmentStep.SelectFactor -> {}
                MfaEnrollmentStep.ConfigureSms, MfaEnrollmentStep.ConfigureTotp -> {
                    currentStep.value = MfaEnrollmentStep.SelectFactor
                    selectedFactor.value = null
                    phoneNumber.value = ""
                    totpSecret.value = null
                    totpQrCodeUrl.value = null
                }
                MfaEnrollmentStep.VerifyFactor -> {
                    verificationCode.value = ""
                    when (selectedFactor.value) {
                        MfaFactor.Sms -> currentStep.value = MfaEnrollmentStep.ConfigureSms
                        MfaFactor.Totp -> currentStep.value = MfaEnrollmentStep.ConfigureTotp
                        null -> currentStep.value = MfaEnrollmentStep.SelectFactor
                    }
                }
                MfaEnrollmentStep.ShowRecoveryCodes -> {
                    currentStep.value = MfaEnrollmentStep.VerifyFactor
                }
            }
            error.value = null
            lastException.value = null
        },
        availableFactors = configuration.allowedFactors,
        enrolledFactors = enrolledFactors.value,
        onFactorSelected = { factor ->
            selectedFactor.value = factor
            when (factor) {
                MfaFactor.Sms -> {
                    currentStep.value = MfaEnrollmentStep.ConfigureSms
                }
                MfaFactor.Totp -> {
                    currentStep.value = MfaEnrollmentStep.ConfigureTotp
                    coroutineScope.launch {
                        isLoading.value = true
                        try {
                            val secret = totpHandler.generateSecret()
                            totpSecret.value = secret
                            totpQrCodeUrl.value = secret.generateQrCodeUrl(
                                accountName = user.email ?: user.phoneNumber ?: "User",
                                issuer = auth.app.name
                            )
                            error.value = null
                            lastException.value = null
                        } catch (e: Exception) {
                            error.value = e.message
                            lastException.value = e
                            onError(e)
                        } finally {
                            isLoading.value = false
                        }
                    }
                }
            }
        },
        onUnenrollFactor = { factorInfo ->
            coroutineScope.launch {
                isLoading.value = true
                try {
                    user.multiFactor.unenroll(factorInfo).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Refresh the enrolled factors list
                            enrolledFactors.value = user.multiFactor.enrolledFactors
                            error.value = null
                        } else {
                            error.value = task.exception?.message
                            task.exception?.let {
                                lastException.value = it
                                onError(it)
                            }
                        }
                        isLoading.value = false
                    }
                } catch (e: Exception) {
                    error.value = e.message
                    lastException.value = e
                    onError(e)
                    isLoading.value = false
                }
            }
        },
        onSkipClick = if (!configuration.requireEnrollment) {
            { onSkip() }
        } else null,
        phoneNumber = phoneNumber.value,
        onPhoneNumberChange = { phone ->
            phoneNumber.value = phone
            error.value = null
        },
        selectedCountry = selectedCountry.value,
        onCountrySelected = { country ->
            selectedCountry.value = country
        },
        onSendSmsCodeClick = {
            coroutineScope.launch {
                isLoading.value = true
                try {
                    val fullPhoneNumber = "${selectedCountry.value.dialCode}${phoneNumber.value}"
                    val session = smsHandler.sendVerificationCode(fullPhoneNumber)
                    smsSession.value = session
                    currentStep.value = MfaEnrollmentStep.VerifyFactor
                    resendTimerSeconds.intValue = SmsEnrollmentHandler.RESEND_DELAY_SECONDS
                    error.value = null
                    lastException.value = null
                } catch (e: Exception) {
                    error.value = e.message
                    lastException.value = e
                    onError(e)
                } finally {
                    isLoading.value = false
                }
            }
        },
        totpSecret = totpSecret.value,
        totpQrCodeUrl = totpQrCodeUrl.value,
        onContinueToVerifyClick = {
            currentStep.value = MfaEnrollmentStep.VerifyFactor
        },
        verificationCode = verificationCode.value,
        onVerificationCodeChange = { code ->
            verificationCode.value = code
            error.value = null
        },
        onVerifyClick = {
            coroutineScope.launch {
                isLoading.value = true
                try {
                    when (selectedFactor.value) {
                        MfaFactor.Sms -> {
                            val session = smsSession.value
                            if (session != null) {
                                smsHandler.enrollWithVerificationCode(
                                    session = session,
                                    verificationCode = verificationCode.value,
                                    displayName = "SMS"
                                )
                            } else {
                                throw IllegalStateException("No SMS session available")
                            }
                        }
                        MfaFactor.Totp -> {
                            val secret = totpSecret.value
                            if (secret != null) {
                                totpHandler.enrollWithVerificationCode(
                                    totpSecret = secret,
                                    verificationCode = verificationCode.value,
                                    displayName = "Authenticator App"
                                )
                            } else {
                                throw IllegalStateException("No TOTP secret available")
                            }
                        }
                        null -> throw IllegalStateException("No factor selected")
                    }

                    // Refresh enrolled factors after successful enrollment
                    enrolledFactors.value = user.multiFactor.enrolledFactors

                    if (configuration.enableRecoveryCodes) {
                        recoveryCodes.value = generateRecoveryCodes()
                        currentStep.value = MfaEnrollmentStep.ShowRecoveryCodes
                    } else {
                        onComplete()
                    }
                    error.value = null
                    lastException.value = null
                } catch (e: Exception) {
                    error.value = e.message
                    lastException.value = e
                    onError(e)
                } finally {
                    isLoading.value = false
                }
            }
        },
        selectedFactor = selectedFactor.value,
        resendTimer = resendTimerSeconds.intValue,
        onResendCodeClick = if (selectedFactor.value == MfaFactor.Sms) {
            {
                if (resendTimerSeconds.intValue == 0) {
                    coroutineScope.launch {
                        val session = smsSession.value
                        if (session != null) {
                            isLoading.value = true
                            try {
                                val newSession = smsHandler.resendVerificationCode(session)
                                smsSession.value = newSession
                                resendTimerSeconds.intValue = SmsEnrollmentHandler.RESEND_DELAY_SECONDS
                                error.value = null
                                lastException.value = null
                            } catch (e: Exception) {
                                error.value = e.message
                                lastException.value = e
                                onError(e)
                            } finally {
                                isLoading.value = false
                            }
                        }
                    }
                }
            }
        } else null,
        recoveryCodes = recoveryCodes.value,
        onCodesSavedClick = {
            onComplete()
        }
    )

    if (content != null) {
        content(state)
    } else {
        DefaultMfaEnrollmentContent(
            state = state,
            authConfiguration = phoneAuthConfiguration,
            user = user
        )
    }
}

/**
 * Generates placeholder recovery codes.
 * In a production implementation, these would come from Firebase or a backend service.
 */
private fun generateRecoveryCodes(): List<String> {
    return List(10) { index ->
        List(4) { (0..9).random() }
            .joinToString("")
            .let { if (index % 2 == 0) "$it-${(1000..9999).random()}" else it }
    }
}
