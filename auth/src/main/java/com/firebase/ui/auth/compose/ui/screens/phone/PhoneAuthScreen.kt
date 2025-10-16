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

package com.firebase.ui.auth.compose.ui.screens.phone

import android.content.Context
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.auth_provider.signInWithPhoneAuthCredential
import com.firebase.ui.auth.compose.configuration.auth_provider.submitVerificationCode
import com.firebase.ui.auth.compose.configuration.auth_provider.verifyPhoneNumber
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.data.CountryData
import com.firebase.ui.auth.compose.data.CountryUtils
import com.firebase.ui.auth.compose.ui.components.ErrorRecoveryDialog
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PhoneAuthStep {
    /**
     * An enum representing a view requiring a phone number which needs to be entered.
     */
    EnterPhoneNumber,

    /**
     * An enum representing a view requiring a phone number verification code which needs to
     * be entered.
     */
    EnterVerificationCode
}

/**
 * A class passed to the content slot, containing all the necessary information to render a custom
 * UI for every step of the phone authentication process.
 *
 * @param step An enum representing the current step in the flow. Use a when expression on this
 * to render the correct UI.
 * @param isLoading true when an asynchronous operation (like sending or verifying a code) is in
 * progress.
 * @param error An optional error message to display to the user.
 * @param phoneNumber (Step: [PhoneAuthStep.EnterPhoneNumber]) The current value of the phone
 * number input field.
 * @param onPhoneNumberChange (Step: [PhoneAuthStep.EnterPhoneNumber]) A callback to be invoked
 * when the phone number input changes.
 * @param selectedCountry (Step: [PhoneAuthStep.EnterPhoneNumber]) The currently selected country
 * object, containing its name, dial code, and flag.
 * @param onCountrySelected (Step: [PhoneAuthStep.EnterPhoneNumber]) A callback to be invoked when
 * the user selects a new country.
 * @param onSendCodeClick (Step: [PhoneAuthStep.EnterPhoneNumber]) A callback to be invoked to
 * send the verification code to the entered number.
 * @param verificationCode (Step: [PhoneAuthStep.EnterVerificationCode]) The current value of the
 * 6-digit code input field.
 * @param onVerificationCodeChange (Step: [PhoneAuthStep.EnterVerificationCode]) A callback to be
 * invoked when the verification code input changes.
 * @param onVerifyCodeClick (Step: [PhoneAuthStep.EnterVerificationCode]) A callback to be invoked
 * to submit the verification code.
 * @param fullPhoneNumber (Step: [PhoneAuthStep.EnterVerificationCode]) The formatted full phone
 * number to display for user confirmation.
 * @param onResendCodeClick (Step: [PhoneAuthStep.EnterVerificationCode]) A callback to be invoked
 * when the user clicks "Resend Code".
 * @param resendTimer (Step: [PhoneAuthStep.EnterVerificationCode]) The number of seconds remaining
 * before the "Resend" action is available.
 * @param onChangeNumberClick (Step: [PhoneAuthStep.EnterVerificationCode]) A callback to navigate
 * back to the [PhoneAuthStep.EnterPhoneNumber] step.
 */
class PhoneAuthContentState(
    val step: PhoneAuthStep,
    val isLoading: Boolean = false,
    val error: String? = null,
    val phoneNumber: String,
    val onPhoneNumberChange: (String) -> Unit,
    val selectedCountry: CountryData,
    val onCountrySelected: (CountryData) -> Unit,
    val onSendCodeClick: () -> Unit,
    val verificationCode: String,
    val onVerificationCodeChange: (String) -> Unit,
    val onVerifyCodeClick: () -> Unit,
    val fullPhoneNumber: String,
    val onResendCodeClick: () -> Unit,
    val resendTimer: Int = 0,
    val onChangeNumberClick: () -> Unit,
)

/**
 * A stateful composable that manages the complete logic for phone number authentication. It handles
 * the multi-step flow of sending and verifying an SMS code, exposing the state for each step to a
 * custom UI via a trailing lambda (slot). This component renders no UI itself.
 *
 * @param context The Android context.
 * @param configuration The authentication UI configuration containing the phone provider settings.
 * @param authUI The FirebaseAuthUI instance used for authentication operations.
 * @param onSuccess Callback invoked when authentication succeeds with the [AuthResult].
 * @param onError Callback invoked when an authentication error occurs.
 * @param onCancel Callback invoked when the user cancels the authentication flow.
 * @param modifier Optional [Modifier] for the composable.
 * @param content A composable lambda that receives [PhoneAuthContentState] to render the UI for
 * each step. If null, no UI will be rendered.
 */
@Composable
fun PhoneAuthScreen(
    context: Context,
    configuration: AuthUIConfiguration,
    authUI: FirebaseAuthUI,
    onSuccess: (AuthResult) -> Unit,
    onError: (AuthException) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ((PhoneAuthContentState) -> Unit)? = null,
) {
    val activity = LocalActivity.current
    val provider = configuration.providers.filterIsInstance<AuthProvider.Phone>().first()
    val stringProvider = DefaultAuthUIStringProvider(context)
    val coroutineScope = rememberCoroutineScope()

    val step = rememberSaveable { mutableStateOf(PhoneAuthStep.EnterPhoneNumber) }
    val phoneNumberValue = rememberSaveable { mutableStateOf(provider.defaultNumber ?: "") }
    val verificationCodeValue = rememberSaveable { mutableStateOf("") }
    val selectedCountry = remember {
        mutableStateOf(
            provider.defaultCountryCode?.let { code ->
                CountryUtils.findByCountryCode(code)
            } ?: CountryUtils.getDefaultCountry()
        )
    }
    val fullPhoneNumber = remember(selectedCountry.value, phoneNumberValue.value) {
        CountryUtils.formatPhoneNumber(selectedCountry.value.dialCode, phoneNumberValue.value)
    }
    val verificationId = rememberSaveable { mutableStateOf<String?>(null) }
    val forceResendingToken =
        rememberSaveable { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    val resendTimerSeconds = rememberSaveable { mutableIntStateOf(0) }

    val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
    val isLoading = authState is AuthState.Loading
    val errorMessage =
        if (authState is AuthState.Error) (authState as AuthState.Error).exception.message else null

    val isErrorDialogVisible =
        remember(authState) { mutableStateOf(authState is AuthState.Error) }

    // Handle resend timer countdown
    LaunchedEffect(resendTimerSeconds.intValue) {
        if (resendTimerSeconds.intValue > 0) {
            delay(1000)
            resendTimerSeconds.intValue--
        }
    }

    LaunchedEffect(authState) {
        Log.d("PhoneAuthScreen", "Current state: $authState")
        when (val state = authState) {
            is AuthState.Success -> {
                state.result?.let { result ->
                    onSuccess(result)
                }
            }

            is AuthState.PhoneNumberVerificationRequired -> {
                verificationId.value = state.verificationId
                forceResendingToken.value = state.forceResendingToken
                step.value = PhoneAuthStep.EnterVerificationCode
                resendTimerSeconds.intValue = provider.timeout.toInt() // Start 60-second countdown
            }

            is AuthState.SMSAutoVerified -> {
                // Auto-verification succeeded, sign in with the credential
                coroutineScope.launch {
                    try {
                        authUI.signInWithPhoneAuthCredential(
                            config = configuration,
                            credential = state.credential
                        )
                    } catch (e: Exception) {
                        // Error will be handled by authState flow
                    }
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

    val state = PhoneAuthContentState(
        step = step.value,
        isLoading = isLoading,
        error = errorMessage,
        phoneNumber = phoneNumberValue.value,
        onPhoneNumberChange = { number ->
            phoneNumberValue.value = number
        },
        selectedCountry = selectedCountry.value,
        onCountrySelected = { country ->
            selectedCountry.value = country
        },
        onSendCodeClick = {
            coroutineScope.launch {
                try {
                    authUI.verifyPhoneNumber(
                        provider = provider,
                        activity = activity,
                        phoneNumber = fullPhoneNumber,
                    )
                } catch (e: Exception) {
                    // Error will be handled by authState flow
                }
            }
        },
        verificationCode = verificationCodeValue.value,
        onVerificationCodeChange = { code ->
            verificationCodeValue.value = code
        },
        onVerifyCodeClick = {
            coroutineScope.launch {
                try {
                    verificationId.value?.let { id ->
                        authUI.submitVerificationCode(
                            config = configuration,
                            verificationId = id,
                            code = verificationCodeValue.value
                        )
                    }
                } catch (e: Exception) {
                    // Error will be handled by authState flow
                }
            }
        },
        fullPhoneNumber = fullPhoneNumber,
        onResendCodeClick = {
            if (resendTimerSeconds.intValue == 0) {
                coroutineScope.launch {
                    try {
                        authUI.verifyPhoneNumber(
                            activity = activity,
                            provider = provider,
                            phoneNumber = fullPhoneNumber,
                            forceResendingToken = forceResendingToken.value,
                        )
                        resendTimerSeconds.intValue = provider.timeout.toInt() // Restart timer
                    } catch (e: Exception) {
                        // Error will be handled by authState flow
                    }
                }
            }
        },
        resendTimer = resendTimerSeconds.intValue,
        onChangeNumberClick = {
            step.value = PhoneAuthStep.EnterPhoneNumber
            verificationCodeValue.value = ""
            verificationId.value = null
            forceResendingToken.value = null
            resendTimerSeconds.intValue = 0
        }
    )

    if (isErrorDialogVisible.value) {
        ErrorRecoveryDialog(
            error = when ((authState as AuthState.Error).exception) {
                is AuthException -> (authState as AuthState.Error).exception as AuthException
                else -> AuthException.from((authState as AuthState.Error).exception)
            },
            stringProvider = stringProvider,
            onRetry = { exception ->
                when (exception) {
                    is AuthException.InvalidCredentialsException -> {
                        if (step.value == PhoneAuthStep.EnterVerificationCode) {
                            state.onVerifyCodeClick()
                        } else {
                            state.onSendCodeClick()
                        }
                    }

                    else -> Unit
                }
                isErrorDialogVisible.value = false
            },
            onDismiss = {
                isErrorDialogVisible.value = false
            },
        )
    }

    content?.invoke(state)
}

