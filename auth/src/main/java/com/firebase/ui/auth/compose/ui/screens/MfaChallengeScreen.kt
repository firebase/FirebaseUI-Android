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
import com.firebase.ui.auth.compose.configuration.MfaFactor
import com.firebase.ui.auth.compose.mfa.MfaChallengeContentState
import com.firebase.ui.auth.compose.mfa.SmsEnrollmentHandler
import com.firebase.ui.auth.compose.mfa.maskPhoneNumber
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.auth.TotpMultiFactorGenerator
import com.google.firebase.auth.TotpMultiFactorInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * A stateful composable that manages the Multi-Factor Authentication (MFA) challenge flow
 * when a user attempts to sign in with MFA enabled.
 *
 * This screen is displayed when an [AuthState.RequiresMfa] state is encountered during sign-in.
 * It handles the verification of the user's second factor (SMS or TOTP) and completes the
 * sign-in process upon successful verification.
 *
 * **Challenge Flow:**
 * 1. Screen detects available MFA factors from the resolver
 * 2. For SMS: automatically sends verification code and shows masked phone number
 * 3. For TOTP: prompts user to enter code from authenticator app
 * 4. User enters verification code
 * 5. System verifies code and completes sign-in
 *
 * @param resolver The [MultiFactorResolver] containing MFA session and available factors
 * @param auth The [FirebaseAuth] instance
 * @param onSuccess Callback invoked when MFA challenge completes successfully
 * @param onCancel Callback invoked when user cancels the MFA challenge
 * @param onError Callback invoked when an error occurs during verification
 * @param content A composable lambda that receives [MfaChallengeContentState] to render custom UI
 *
 * @since 10.0.0
 */
@Composable
fun MfaChallengeScreen(
    resolver: MultiFactorResolver,
    auth: FirebaseAuth,
    onSuccess: (AuthResult) -> Unit,
    onCancel: () -> Unit,
    onError: (Exception) -> Unit = {},
    content: @Composable (MfaChallengeContentState) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val isLoading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }
    val verificationCode = rememberSaveable { mutableStateOf("") }
    val verificationId = remember { mutableStateOf<String?>(null) }
    val resendTimerSeconds = rememberSaveable { mutableIntStateOf(0) }

    // Handle resend timer countdown
    LaunchedEffect(resendTimerSeconds.intValue) {
        if (resendTimerSeconds.intValue > 0) {
            delay(1000)
            resendTimerSeconds.intValue--
        }
    }

    val hints = resolver.hints
    val firstHint = hints.firstOrNull()

    val factorType = remember {
        when (firstHint?.factorId) {
            PhoneMultiFactorGenerator.FACTOR_ID -> MfaFactor.Sms
            TotpMultiFactorGenerator.FACTOR_ID -> MfaFactor.Totp
            else -> MfaFactor.Sms
        }
    }

    val maskedPhoneNumber = remember {
        if (firstHint is PhoneMultiFactorInfo) {
            maskPhoneNumber(firstHint.phoneNumber)
        } else null
    }

    LaunchedEffect(firstHint) {
        if (firstHint is PhoneMultiFactorInfo) {
            isLoading.value = true
            try {
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                        coroutineScope.launch {
                            try {
                                val assertion = PhoneMultiFactorGenerator.getAssertion(credential)
                                val result = resolver.resolveSignIn(assertion).await()
                                onSuccess(result)
                            } catch (e: Exception) {
                                error.value = e.message
                                onError(e)
                            }
                        }
                    }

                    override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                        error.value = e.message
                        onError(e)
                        isLoading.value = false
                    }

                    override fun onCodeSent(
                        verId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        verificationId.value = verId
                        resendTimerSeconds.intValue = SmsEnrollmentHandler.RESEND_DELAY_SECONDS
                        isLoading.value = false
                    }
                }

                val options = PhoneAuthOptions.newBuilder()
                    .setMultiFactorHint(firstHint)
                    .setMultiFactorSession(resolver.session)
                    .setCallbacks(callbacks)
                    .setTimeout(SmsEnrollmentHandler.VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(options)
            } catch (e: Exception) {
                error.value = e.message
                onError(e)
                isLoading.value = false
            }
        }
    }

    val state = MfaChallengeContentState(
        factorType = factorType,
        maskedPhoneNumber = maskedPhoneNumber,
        isLoading = isLoading.value,
        error = error.value,
        verificationCode = verificationCode.value,
        resendTimer = resendTimerSeconds.intValue,
        onVerificationCodeChange = { code ->
            verificationCode.value = code
            error.value = null
        },
        onVerifyClick = {
            coroutineScope.launch {
                isLoading.value = true
                try {
                    val assertion = when (factorType) {
                        MfaFactor.Sms -> {
                            val verId = verificationId.value
                            require(verId != null) { "No verification ID available" }
                            val credential = PhoneAuthProvider.getCredential(
                                verId,
                                verificationCode.value
                            )
                            PhoneMultiFactorGenerator.getAssertion(credential)
                        }
                        MfaFactor.Totp -> {
                            val totpInfo = firstHint as? TotpMultiFactorInfo
                            require(totpInfo != null) { "No TOTP info available" }
                            TotpMultiFactorGenerator.getAssertionForSignIn(
                                totpInfo.uid,
                                verificationCode.value
                            )
                        }
                    }

                    val result = resolver.resolveSignIn(assertion).await()
                    onSuccess(result)
                    error.value = null
                } catch (e: Exception) {
                    error.value = e.message
                    onError(e)
                } finally {
                    isLoading.value = false
                }
            }
        },
        onResendCodeClick = if (factorType == MfaFactor.Sms && firstHint is PhoneMultiFactorInfo) {
            {
                if (resendTimerSeconds.intValue == 0) {
                    coroutineScope.launch {
                        isLoading.value = true
                        try {
                            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                                    coroutineScope.launch {
                                        try {
                                            val assertion = PhoneMultiFactorGenerator.getAssertion(credential)
                                            val result = resolver.resolveSignIn(assertion).await()
                                            onSuccess(result)
                                        } catch (e: Exception) {
                                            error.value = e.message
                                            onError(e)
                                        }
                                    }
                                }

                                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                                    error.value = e.message
                                    onError(e)
                                    isLoading.value = false
                                }

                                override fun onCodeSent(
                                    verId: String,
                                    token: PhoneAuthProvider.ForceResendingToken
                                ) {
                                    verificationId.value = verId
                                    resendTimerSeconds.intValue = SmsEnrollmentHandler.RESEND_DELAY_SECONDS
                                    error.value = null
                                    isLoading.value = false
                                }
                            }

                            val options = PhoneAuthOptions.newBuilder()
                                .setMultiFactorHint(firstHint)
                                .setMultiFactorSession(resolver.session)
                                .setCallbacks(callbacks)
                                .setTimeout(SmsEnrollmentHandler.VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                .build()

                            PhoneAuthProvider.verifyPhoneNumber(options)
                        } catch (e: Exception) {
                            error.value = e.message
                            onError(e)
                            isLoading.value = false
                        }
                    }
                }
            }
        } else null,
        onCancelClick = onCancel
    )

    content(state)
}
