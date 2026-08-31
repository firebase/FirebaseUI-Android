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

package com.firebase.ui.auth.ui.screens.phone

import android.content.Context
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.auth_provider.signInWithPhoneAuthCredential
import com.firebase.ui.auth.configuration.auth_provider.submitVerificationCode
import com.firebase.ui.auth.configuration.auth_provider.verifyPhoneNumber
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.data.CountryData
import com.firebase.ui.auth.ui.components.LocalTopLevelDialogController
import com.firebase.ui.auth.util.CountryUtils
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Job
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
    val stringProvider = LocalAuthUIStringProvider.current
    val dialogController = LocalTopLevelDialogController.current
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
    val pendingVerificationPhoneNumber = remember { mutableStateOf<String?>(null) }
    val verificationStartTime = remember { mutableStateOf<Long?>(null) }

    // Verification is a long-lived collection: it stays open until Firebase's auto-retrieval
    // timeout, so a superseded attempt must be cancelled or it keeps writing auth state.
    val verificationJob = remember { mutableStateOf<Job?>(null) }
    // Not rememberSaveable: the coroutine that clears this dies with the composition, so a value
    // restored after rotation would latch forever and permanently disable auto sign-in.
    val isSubmittingCode = remember { mutableStateOf(false) }

    // Logged, not silent: which attempt was torn down and why is the first thing needed from a
    // field report of a stuck or duplicated phone sign-in.
    val cancelVerification: (String) -> Unit = { reason ->
        verificationJob.value?.let { job ->
            Log.d("PhoneAuthScreen", "Cancelling verification attempt ($reason)")
            job.cancel()
        }
    }

    val currentAuthState = remember(authUI) { authUI.authStateFlow() }.collectAsState(AuthState.Idle)
    val authState by currentAuthState
    val isLoading = authState is AuthState.Loading ||
        authState is AuthState.Reauthentication.Authenticating

    // A cancelled Loading outlives this composition on the process-scoped FirebaseAuthUI, and
    // currentAuthState is re-remembered per authUI, so onDispose reads the right instance.
    //
    // Only an ordinary Loading is retracted here. Under a reauthentication request the pending
    // Loading is published as Reauthentication.Authenticating, which the reauth flow's own teardown
    // owns; and were this to write anyway, updateAuthState folds Idle back into the armed request
    // rather than dropping it.
    DisposableEffect(authUI) {
        onDispose {
            if (currentAuthState.value is AuthState.Loading) {
                authUI.updateAuthState(AuthState.Idle)
            }
        }
    }
    val errorMessage = when (val state = authState) {
        is AuthState.Error -> state.exception.message
        is AuthState.Reauthentication.AttemptFailed -> state.exception.message
        else -> null
    }

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
                // Sign-in is done, so nothing is left to verify. Hosts that keep this screen
                // composed would otherwise let a late emission start a redundant sign-in.
                cancelVerification("sign-in complete")
                state.result?.let { result ->
                    onSuccess(result)
                }
            }

            is AuthState.PhoneNumberVerificationRequired,
            is AuthState.Reauthentication.PhoneNumberVerificationRequired -> {
                verificationId.value = when (state) {
                    is AuthState.PhoneNumberVerificationRequired -> state.verificationId
                    is AuthState.Reauthentication.PhoneNumberVerificationRequired -> {
                        state.verificationId
                    }
                    else -> error("Unreachable phone verification state")
                }
                forceResendingToken.value = when (state) {
                    is AuthState.PhoneNumberVerificationRequired -> state.forceResendingToken
                    is AuthState.Reauthentication.PhoneNumberVerificationRequired -> {
                        state.forceResendingToken
                    }
                    else -> error("Unreachable phone verification state")
                }
                step.value = PhoneAuthStep.EnterVerificationCode
                resendTimerSeconds.intValue = provider.timeout.toInt() // Start 60-second countdown
            }

            is AuthState.SMSAutoVerified,
            is AuthState.Reauthentication.SmsAutoVerified -> {
                val credential = when (state) {
                    is AuthState.SMSAutoVerified -> state.credential
                    is AuthState.Reauthentication.SmsAutoVerified -> state.credential
                    else -> error("Unreachable SMS verification state")
                }
                // Auto-verification succeeded, sign in with the credential
                // and clear pending verification tracking
                pendingVerificationPhoneNumber.value = null
                verificationStartTime.value = null

                // A manually submitted code is already signing in: auto-verifying now would run a
                // second concurrent sign-in with the same phone number.
                if (isSubmittingCode.value) {
                    Log.d("PhoneAuthScreen", "Suppressed auto sign-in: manual submit in flight")
                    // Restoring the submit's Loading both consumes the credential (so it can't
                    // leak to a freshly composed screen) and keeps Verify/Resend disabled.
                    authUI.updateAuthState(
                        AuthState.Loading(configuration.stringProvider.loadingSigningInWithPhone)
                    )
                } else {
                    // Consumed before the async sign-in call so it can't be clobbered by that
                    // call's own state.
                    if (state is AuthState.Reauthentication.SmsAutoVerified) {
                        authUI.updateReauthentication(state.requestId) { it.attemptStarted() }
                    } else {
                        authUI.updateAuthState(AuthState.Idle)
                    }
                    coroutineScope.launch {
                        try {
                            authUI.signInWithPhoneAuthCredential(
                                context = context,
                                config = configuration,
                                credential = credential
                            )
                        } catch (e: Exception) {
                            // Error will be handled by authState flow
                        }
                    }
                }
            }

            is AuthState.Error -> {
                val exception = AuthException.from(state.exception, stringProvider)
                // A cooldown rejection is about the duplicate tap, not the attempt in flight.
                // Every other error ends the attempt, so stop holding Firebase's callbacks.
                if (exception !is AuthException.PhoneVerificationCooldownException) {
                    cancelVerification("verification failed")
                }
                // Sign-in and code submission report cancellation as an Error, but this screen
                // cancels them as routine bookkeeping, so that is not a host-facing failure.
                if (exception !is AuthException.AuthCancelledException) {
                    onError(exception)

                    // Show dialog for phone-specific errors using top-level controller
                    dialogController?.showErrorDialog(
                        exception = exception,
                        errorState = state,
                        onRetry = { ex ->
                            when (ex) {
                                is AuthException.InvalidCredentialsException -> {
                                    // User can retry with corrected code or phone number
                                }
                                else -> Unit
                            }
                        },
                        onDismiss = {
                            // Dialog dismissed
                        }
                    )
                }
                // Consumed immediately so this doesn't leak to a freshly created screen.
                authUI.updateAuthState(AuthState.Idle)
            }

            is AuthState.Cancelled -> {
                onCancel()
                // Consumed so this doesn't leak to a freshly created screen.
                authUI.updateAuthState(AuthState.Idle)
            }

            is AuthState.Reauthentication.AttemptFailed -> {
                // Same teardown as the ordinary Error branch above: the attempt is over, so stop
                // holding Firebase's callbacks. The phase itself is left latched for the reauth UI
                // to render, so nothing is consumed here.
                val exception = AuthException.from(state.exception, stringProvider)
                if (exception !is AuthException.PhoneVerificationCooldownException) {
                    cancelVerification("reauthentication attempt failed")
                }
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
            val currentTime = System.currentTimeMillis()
            val timeoutMs = provider.timeout * 1000
            val timeSinceLastVerification = verificationStartTime.value?.let {
                currentTime - it
            } ?: Long.MAX_VALUE

            // Check if the same phone number is being verified again within the cooldown period
            val storedNumber = pendingVerificationPhoneNumber.value
            val isSameNumber = storedNumber != null && fullPhoneNumber == storedNumber

            // Check cooldown: same number and still within timeout period
            if (isSameNumber && timeSinceLastVerification < timeoutMs) {
                // Calculate remaining cooldown time in seconds
                val remainingCooldownSeconds =
                    ((timeoutMs - timeSinceLastVerification) / 1000).coerceAtLeast(1)
                val plural = if (remainingCooldownSeconds != 1L) "s" else ""
                // Rejected before anything is cancelled: a duplicate tap must not tear down the
                // healthy in-flight verification it was rejected in favour of.
                authUI.updateAuthState(
                    AuthState.Error(
                        AuthException.PhoneVerificationCooldownException(
                            message = "Please wait $remainingCooldownSeconds second$plural " +
                                "before verifying the same phone number again. The cooldown " +
                                "period is ${provider.timeout} seconds.",
                            cooldownSeconds = remainingCooldownSeconds
                        )
                    )
                )
            } else {
                cancelVerification("new verification requested")

                // Track the phone number and start time for cooldown checking
                pendingVerificationPhoneNumber.value = fullPhoneNumber
                verificationStartTime.value = currentTime

                verificationJob.value = coroutineScope.launch {
                    try {
                        authUI.verifyPhoneNumber(
                            provider = provider,
                            activity = activity,
                            phoneNumber = fullPhoneNumber,
                            config = configuration,
                        )
                    } catch (e: Exception) {
                        // Error will be handled by authState flow
                    }
                }
            }
        },
        verificationCode = verificationCodeValue.value,
        onVerificationCodeChange = { code ->
            verificationCodeValue.value = code
        },
        onVerifyCodeClick = {
            // Latched before the launch so an auto-verification arriving in between can't slip
            // past the guard.
            isSubmittingCode.value = true
            coroutineScope.launch {
                try {
                    verificationId.value?.let { id ->
                        authUI.submitVerificationCode(
                            context = context,
                            config = configuration,
                            verificationId = id,
                            code = verificationCodeValue.value
                        )
                    }
                } catch (e: Exception) {
                    // Error will be handled by authState flow
                } finally {
                    // Cleared in finally, not catch: submitVerificationCode also returns null
                    // without throwing (MFA and reauth paths).
                    isSubmittingCode.value = false
                }
            }
        },
        fullPhoneNumber = fullPhoneNumber,
        onResendCodeClick = {
            if (resendTimerSeconds.intValue == 0) {
                cancelVerification("code resent")
                verificationJob.value = coroutineScope.launch {
                    try {
                        // The timer is restarted by the PhoneNumberVerificationRequired branch
                        // above: this call only returns once the verification window closes.
                        authUI.verifyPhoneNumber(
                            activity = activity,
                            provider = provider,
                            phoneNumber = fullPhoneNumber,
                            config = configuration,
                            forceResendingToken = forceResendingToken.value,
                        )
                    } catch (e: Exception) {
                        // Error will be handled by authState flow
                    }
                }
            }
        },
        resendTimer = resendTimerSeconds.intValue,
        onChangeNumberClick = {
            cancelVerification("changing phone number")
            // Nothing replaces the cancelled attempt here, so this handler retracts its Loading -
            // as the armed request's provider-selection phase when one is running, Idle otherwise.
            val currentReauthentication = authState as? AuthState.Reauthentication
            if (currentReauthentication != null) {
                authUI.updateReauthentication(currentReauthentication.requestId) {
                    it.returnedToProviderSelection()
                }
            } else {
                authUI.updateAuthState(AuthState.Idle)
            }
            verificationJob.value = null
            isSubmittingCode.value = false
            step.value = PhoneAuthStep.EnterPhoneNumber
            verificationCodeValue.value = ""
            verificationId.value = null
            forceResendingToken.value = null
            resendTimerSeconds.intValue = 0
        }
    )

    if (content != null) {
        content(state)
    } else {
        DefaultPhoneAuthContent(
            configuration = configuration,
            state = state,
            onCancel = onCancel
        )
    }
}

@Composable
private fun DefaultPhoneAuthContent(
    configuration: AuthUIConfiguration,
    state: PhoneAuthContentState,
    onCancel: () -> Unit,
) {
    when (state.step) {
        PhoneAuthStep.EnterPhoneNumber -> {
            EnterPhoneNumberUI(
                configuration = configuration,
                isLoading = state.isLoading,
                phoneNumber = state.phoneNumber,
                selectedCountry = state.selectedCountry,
                onPhoneNumberChange = state.onPhoneNumberChange,
                onCountrySelected = state.onCountrySelected,
                onSendCodeClick = state.onSendCodeClick,
                onNavigateBack = onCancel
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
                onNavigateBack = onCancel
            )
        }
    }
}
