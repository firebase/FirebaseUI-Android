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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.MfaFactor
import com.firebase.ui.auth.data.CountryData
import com.firebase.ui.auth.data.CountryDataSaver
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.mfa.SmsEnrollmentSession
import com.firebase.ui.auth.mfa.SmsEnrollmentSessionSaver
import com.firebase.ui.auth.mfa.TotpSecret
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.email.RedirectingStep
import com.firebase.ui.auth.ui.screens.resetBackStackTo
import com.firebase.ui.auth.util.CountryUtils

/**
 * Everything an [MfaEnrollmentScreen] step needs that must outlive the step being left.
 *
 * Remembered by the host *above* the [androidx.navigation.compose.NavHost] and handed to every
 * step through [mfaEnrollmentDestinations], so a step reads and writes this instead of its own
 * local state and the data is still there when the flow returns to it.
 *
 * [selectedFactor], [phoneNumber], [verificationCode], [resendTimerSeconds], [smsSession] and
 * [selectedCountry] are backed by [rememberSaveable] in [rememberMfaEnrollmentFlowState] and
 * survive Activity recreation. [totpSecret], [totpQrCodeUrl] and [totpSecretExpiredMessage] are
 * backed by plain [remember] and are lost: [TotpSecret] wraps `com.google.firebase.auth.TotpSecret`,
 * an interface with no public reconstruction API, so there is no `Saver` to write for it.
 *
 * That loss is recovered rather than fatal: [MfaEnrollmentScreen]'s `LaunchedEffect(currentStep)`
 * fetches a fresh secret when [totpSecret] is null on [MfaEnrollmentStep.ConfigureTotp], and
 * bounces back to `ConfigureTotp` with [totpSecretExpiredMessage] set when the user was already
 * past it. A new secret means a new QR code, so this is a user-visible re-scan, not a seamless
 * recovery.
 *
 * @since 10.0.0
 */
class MfaEnrollmentFlowState internal constructor(
    val selectedFactor: MutableState<MfaFactor?>,
    val phoneNumber: MutableState<String>,
    val verificationCode: MutableState<String>,
    val resendTimerSeconds: MutableIntState,
    val smsSession: MutableState<SmsEnrollmentSession?>,
    val totpSecret: MutableState<TotpSecret?>,
    val totpQrCodeUrl: MutableState<String?>,
    val selectedCountry: MutableState<CountryData>,
    val totpSecretExpiredMessage: MutableState<String?>,
) {
    /**
     * Returns every field to the value [rememberMfaEnrollmentFlowState] starts it at.
     *
     * Called on flow *entry* — [enterMfaEnrollment] — not on completion: a flow that has been
     * left is still composed for the length of the exit transition, and clearing underneath it
     * leaves a rendered step whose controls act on state that is already gone.
     *
     * @since 10.0.0
     */
    fun reset() {
        selectedFactor.value = null
        phoneNumber.value = ""
        verificationCode.value = ""
        resendTimerSeconds.intValue = 0
        smsSession.value = null
        totpSecret.value = null
        totpQrCodeUrl.value = null
        selectedCountry.value = CountryUtils.getDefaultCountry()
        totpSecretExpiredMessage.value = null
    }
}

/**
 * Creates and remembers the [MfaEnrollmentFlowState] a host installs [mfaEnrollmentDestinations]
 * with. Call once, above the `NavHost`, so the same instance is handed to every step — see
 * [MfaEnrollmentFlowState] for which of its fields survive Activity recreation.
 */
@Composable
fun rememberMfaEnrollmentFlowState(): MfaEnrollmentFlowState {
    val selectedFactor = rememberSaveable { mutableStateOf<MfaFactor?>(null) }
    val phoneNumber = rememberSaveable { mutableStateOf("") }
    val verificationCode = rememberSaveable { mutableStateOf("") }
    val resendTimerSeconds = rememberSaveable { mutableIntStateOf(0) }
    val smsSession = rememberSaveable(stateSaver = SmsEnrollmentSessionSaver) {
        mutableStateOf<SmsEnrollmentSession?>(null)
    }
    val totpSecret = remember { mutableStateOf<TotpSecret?>(null) }
    val totpQrCodeUrl = remember { mutableStateOf<String?>(null) }
    val selectedCountry = rememberSaveable(stateSaver = CountryDataSaver) {
        mutableStateOf(CountryUtils.getDefaultCountry())
    }
    val totpSecretExpiredMessage = remember { mutableStateOf<String?>(null) }
    return remember {
        MfaEnrollmentFlowState(
            selectedFactor = selectedFactor,
            phoneNumber = phoneNumber,
            verificationCode = verificationCode,
            resendTimerSeconds = resendTimerSeconds,
            smsSession = smsSession,
            totpSecret = totpSecret,
            totpQrCodeUrl = totpQrCodeUrl,
            selectedCountry = selectedCountry,
            totpSecretExpiredMessage = totpSecretExpiredMessage,
        )
    }
}

/**
 * Registers the MFA enrolment flow's steps on [this] graph.
 *
 * Every move between steps **pushes**; this flow never replaces a destination. No enrolment or
 * verification failure moves the user off the step they are on — each sets
 * [MfaEnrollmentContentState.error] and stays put.
 *
 * @param flowState The state that must outlive a step switch — see [MfaEnrollmentFlowState].
 * Shared by every step this registers, and expected to be `remember`-ed by the host once, above
 * the `NavHost`.
 */
internal fun NavGraphBuilder.mfaEnrollmentDestinations(
    navController: NavHostController,
    configuration: MfaConfiguration,
    authConfiguration: AuthUIConfiguration?,
    authUI: FirebaseAuthUI,
    flowState: MfaEnrollmentFlowState,
    content: (@Composable (MfaEnrollmentContentState) -> Unit)?,
    onComplete: () -> Unit,
    onSkip: () -> Unit = {},
    onError: (Exception) -> Unit = {},
) {
    AuthRoute.MfaEnrollment.steps.forEach { step ->
        composable(route = step.routePattern) {
            val user = authUI.getCurrentUser()
            if (user == null) {
                // Every step is registered, so one reached with no signed-in user leaves the flow.
                // An effect, since leaving mutates the back stack; Unit runs it once per entry.
                LaunchedEffect(Unit) { navController.exitMfaEnrollment() }
                RedirectingStep()
                return@composable
            }

            MfaEnrollmentScreen(
                user = user,
                auth = authUI.auth,
                configuration = configuration,
                authConfiguration = authConfiguration,
                content = content,
                step = step.enrollmentStep,
                onNavigateToStep = { target ->
                    navController.navigateToMfaStep(AuthRoute.MfaEnrollment.stepFor(target))
                },
                onNavigateBack = { navController.popBackStack() },
                flowState = flowState,
                onComplete = onComplete,
                onSkip = onSkip,
                onError = onError,
            )
        }
    }
}

/** Pushes [step] onto the back stack. Always a push, never a pop-then-push. */
internal fun NavHostController.navigateToMfaStep(step: AuthRoute.MfaEnrollment.Step) {
    navigate(step.route)
}

/**
 * Enters the enrolment flow on a cleared [flowState], landing on [step] or, without one, on
 * [mfaEnrollmentStartStep].
 *
 * The clear happens here rather than when the flow completes because [flowState] outlives the
 * flow — six of its fields are `rememberSaveable`, so an enrolment's phone number and verification
 * code otherwise survive both the exit and process death, and the next entry lands on a filled-in
 * form whose Verify button re-submits a code Firebase has already consumed. Clearing on the way
 * out instead would mutate state a leaving step still reads while it is composed for the exit
 * transition; on the way in there is no such step.
 *
 * Both of the host's flow entry points go through here: `AuthSuccessUiContext.onManageMfa`, and
 * `onNavigate` given [AuthRoute.MfaEnrollment] or any one of its steps.
 *
 * @param step Where a caller that named a step rather than the whole flow asked to land; null
 * means it named the flow, which resolves through [mfaEnrollmentStartStep]. Entry takes the step
 * as a parameter rather than leaving that caller to navigate for itself, because
 * [AuthRoute.MfaEnrollment] and [AuthRoute.MfaEnrollment.SelectFactor] report the same
 * [AuthRoute.route]: a caller doing its own `navigate` cannot honour a named step without also
 * skipping the clear.
 */
internal fun NavHostController.enterMfaEnrollment(
    configuration: MfaConfiguration,
    flowState: MfaEnrollmentFlowState,
    step: AuthRoute.MfaEnrollment.Step? = null,
) {
    flowState.reset()
    navigate((step ?: mfaEnrollmentStartStep(configuration)).route)
}

/**
 * Leaves the MFA enrolment flow, popping a step at a time until the top of the back stack is not
 * one.
 *
 * Pops nothing when the flow is already left, so exiting twice leaves what is underneath — and
 * its entry-scoped state — untouched. Resets to [AuthRoute.Success] only when the flow was the
 * entire back stack and popping it emptied the stack.
 */
internal fun NavHostController.exitMfaEnrollment() {
    var onStep = AuthRoute.MfaEnrollment.isStep(currentDestination?.route)
    while (onStep && popBackStack()) {
        onStep = AuthRoute.MfaEnrollment.isStep(currentDestination?.route)
    }
    if (onStep) resetBackStackTo(AuthRoute.Success)
}

/**
 * Where entering the MFA enrolment flow should land, resolved once at flow entry.
 *
 * A configuration offering exactly one factor has nothing to choose, so the flow starts on that
 * factor's own configuration step directly, never visiting
 * [AuthRoute.MfaEnrollment.SelectFactor].
 */
internal fun mfaEnrollmentStartStep(configuration: MfaConfiguration): AuthRoute.MfaEnrollment.Step {
    return when (configuration.allowedFactors.singleOrNull()) {
        MfaFactor.Sms -> AuthRoute.MfaEnrollment.ConfigureSms
        MfaFactor.Totp -> AuthRoute.MfaEnrollment.ConfigureTotp
        null -> AuthRoute.MfaEnrollment.SelectFactor
    }
}
