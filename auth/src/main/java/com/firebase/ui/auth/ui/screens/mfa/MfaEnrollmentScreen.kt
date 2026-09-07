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

package com.firebase.ui.auth.ui.screens.mfa

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.mfa.SmsEnrollmentHandler
import com.firebase.ui.auth.mfa.TotpEnrollmentHandler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A stateful composable that manages the Multi-Factor Authentication (MFA) enrollment flow.
 *
 * This screen handles all steps of MFA enrollment including factor selection, configuration,
 * and verification. It uses the provided handlers to communicate with Firebase Authentication
 * and exposes state through a content slot for custom UI rendering.
 *
 * **Enrollment Flow:**
 * 1. **SelectFactor** - User chooses between SMS or TOTP
 * 2. **ConfigureSms** or **ConfigureTotp** - User sets up their chosen factor
 * 3. **VerifyFactor** - User verifies with a code
 *
 * The step is always driven from the outside: a host gives every step its own navigation
 * destination and passes [step], [onNavigateToStep] and [onNavigateBack], so each step gets a real
 * back-stack entry and the configured screen transitions, and a step switch does not dispose what a
 * previous step held, because that lives in [flowState].
 *
 * @param user The currently authenticated [FirebaseUser] to enroll in MFA
 * @param auth The [FirebaseAuth] instance
 * @param configuration MFA configuration controlling available factors and behavior
 * @param onComplete Callback invoked when enrollment completes successfully
 * @param onSkip Callback invoked when user skips enrollment (only if not required)
 * @param onError Callback invoked when an error occurs during enrollment
 * @param step The step to render. A flow starts at [MfaEnrollmentStep.SelectFactor], or straight at
 * the single allowed factor's configuration step when [MfaConfiguration.allowedFactors] holds only
 * one. Give each step its own navigation destination: a host that instead re-renders this screen
 * in place leaves system back with nothing to pop.
 * @param onNavigateToStep Invoked when the flow moves forward — selecting a factor, or continuing
 * from a configured one to verification. Always a push.
 * @param onNavigateBack Invoked when the user backs out of a step — a pop, which returns to
 * whichever of [MfaEnrollmentStep.ConfigureSms] or [MfaEnrollmentStep.ConfigureTotp] was actually
 * pushed before [MfaEnrollmentStep.VerifyFactor].
 * @param flowState The data a step switch must not dispose — see
 * [com.firebase.ui.auth.ui.screens.mfa.MfaEnrollmentFlowState]. Build one with
 * [com.firebase.ui.auth.ui.screens.mfa.rememberMfaEnrollmentFlowState].
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
    step: MfaEnrollmentStep,
    onNavigateToStep: (MfaEnrollmentStep) -> Unit,
    onNavigateBack: () -> Unit,
    flowState: MfaEnrollmentFlowState,
    content: @Composable ((MfaEnrollmentContentState) -> Unit)? = null,
) {
    val activity = requireNotNull(LocalActivity.current) {
        "MfaEnrollmentScreen must be used within an Activity context for SMS verification"
    }

    val smsHandler = remember(activity, auth, user) { SmsEnrollmentHandler(activity, auth, user) }
    val totpHandler = remember(auth, user) { TotpEnrollmentHandler(auth, user) }

    MfaEnrollmentScreenInternal(
        user = user,
        auth = auth,
        configuration = configuration,
        smsHandler = smsHandler,
        totpHandler = totpHandler,
        authConfiguration = authConfiguration,
        onComplete = onComplete,
        onSkip = onSkip,
        onError = onError,
        step = step,
        onNavigateToStep = onNavigateToStep,
        onNavigateBack = onNavigateBack,
        flowState = flowState,
        content = content
    )
}

/**
 * Handler-injection seam behind [MfaEnrollmentScreen].
 *
 * Holds the entire enrollment state machine while taking [smsHandler] and [totpHandler] as
 * parameters, so unit tests can substitute stubbed handlers instead of hitting real Firebase
 * statics. Not part of the public API.
 */
@Composable
internal fun MfaEnrollmentScreenInternal(
    user: FirebaseUser,
    auth: FirebaseAuth,
    configuration: MfaConfiguration,
    smsHandler: SmsEnrollmentHandler,
    totpHandler: TotpEnrollmentHandler,
    authConfiguration: AuthUIConfiguration? = null,
    onComplete: () -> Unit,
    onSkip: () -> Unit = {},
    onError: (Exception) -> Unit = {},
    step: MfaEnrollmentStep,
    onNavigateToStep: (MfaEnrollmentStep) -> Unit,
    onNavigateBack: () -> Unit,
    flowState: MfaEnrollmentFlowState,
    content: @Composable ((MfaEnrollmentContentState) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val applicationContext = LocalContext.current.applicationContext

    val selectedFactor = flowState.selectedFactor
    val phoneNumber = flowState.phoneNumber
    val verificationCode = flowState.verificationCode
    val resendTimerSeconds = flowState.resendTimerSeconds
    val smsSession = flowState.smsSession
    val totpSecret = flowState.totpSecret
    val totpQrCodeUrl = flowState.totpQrCodeUrl
    val selectedCountry = flowState.selectedCountry
    val totpSecretExpiredMessage = flowState.totpSecretExpiredMessage

    // Transient per-step UI state: never part of flowState, so a step switch resets it.
    val isLoading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }
    val lastException = remember { mutableStateOf<Exception?>(null) }
    val enrolledFactors = remember { mutableStateOf(user.multiFactor.enrolledFactors) }

    val phoneAuthConfiguration = remember(authConfiguration, applicationContext) {
        authConfiguration ?: authUIConfiguration {
            context = applicationContext
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

    LaunchedEffect(resendTimerSeconds.intValue) {
        if (resendTimerSeconds.intValue > 0) {
            delay(1000)
            resendTimerSeconds.intValue--
        }
    }

    // The null-secret guard fetches once per entry, so a back-and-forward must not re-fetch.
    LaunchedEffect(step) {
        when (step) {
            MfaEnrollmentStep.ConfigureSms -> selectedFactor.value = MfaFactor.Sms
            MfaEnrollmentStep.ConfigureTotp -> {
                selectedFactor.value = MfaFactor.Totp
                if (totpSecret.value == null) {
                    isLoading.value = true
                    try {
                        val secret = totpHandler.generateSecret()
                        totpSecret.value = secret
                        totpQrCodeUrl.value = secret.generateQrCodeUrl(
                            accountName = user.email ?: user.phoneNumber ?: "User",
                            issuer = auth.app.name
                        )
                        // Non-null only via the VerifyFactor redirect below; a first fetch clears.
                        error.value = totpSecretExpiredMessage.value
                        totpSecretExpiredMessage.value = null
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
            MfaEnrollmentStep.VerifyFactor -> {
                // A null secret here means Activity recreation dropped it: recover on ConfigureTotp.
                if (selectedFactor.value == MfaFactor.Totp && totpSecret.value == null) {
                    totpSecretExpiredMessage.value = TOTP_SECRET_EXPIRED_MESSAGE
                    onNavigateBack()
                }
            }
            MfaEnrollmentStep.SelectFactor -> Unit
        }
    }

    /**
     * Moves the flow forward one step, by asking the host to navigate. Forward moves only — a
     * backward move goes through `onBackClick`.
     */
    fun goToStep(target: MfaEnrollmentStep) {
        onNavigateToStep(target)
    }

    val state = MfaEnrollmentContentState(
        step = step,
        isLoading = isLoading.value,
        error = error.value,
        exception = lastException.value,
        // flowState is deliberately not cleared: a step still on the stack keeps it.
        onBackClick = onNavigateBack,
        availableFactors = configuration.allowedFactors,
        enrolledFactors = enrolledFactors.value,
        onFactorSelected = { factor ->
            goToStep(
                when (factor) {
                    MfaFactor.Sms -> MfaEnrollmentStep.ConfigureSms
                    MfaFactor.Totp -> MfaEnrollmentStep.ConfigureTotp
                }
            )
        },
        onUnenrollFactor = { factorInfo ->
            coroutineScope.launch {
                isLoading.value = true
                try {
                    user.multiFactor.unenroll(factorInfo).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
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
                    goToStep(MfaEnrollmentStep.VerifyFactor)
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
        onContinueToVerifyClick = { goToStep(MfaEnrollmentStep.VerifyFactor) },
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

                    enrolledFactors.value = user.multiFactor.enrolledFactors

                    onComplete()
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
        } else null
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
 * Surfaced via [MfaEnrollmentContentState.error] on [MfaEnrollmentStep.ConfigureTotp] after the
 * user is bounced back from [MfaEnrollmentStep.VerifyFactor] because Activity recreation dropped
 * the TOTP secret. The regenerated secret has a new `sharedSecretKey`, so the QR code on screen is
 * a different one the user has to re-scan.
 */
internal const val TOTP_SECRET_EXPIRED_MESSAGE =
    "Your authenticator setup session expired. Scan the new QR code to continue."
