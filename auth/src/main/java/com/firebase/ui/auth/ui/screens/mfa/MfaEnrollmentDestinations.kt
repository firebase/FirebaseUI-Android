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
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
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
import com.firebase.ui.auth.ui.screens.authRouteMetadata
import com.firebase.ui.auth.ui.screens.email.RedirectingStep
import com.firebase.ui.auth.ui.screens.enrollmentStep
import com.firebase.ui.auth.ui.screens.popOrNull
import com.firebase.ui.auth.ui.screens.pushUnique
import com.firebase.ui.auth.ui.screens.resetBackStackTo
import com.firebase.ui.auth.ui.screens.toKey
import com.firebase.ui.auth.util.CountryUtils

/**
 * Everything an [MfaEnrollmentScreen] step needs that must outlive the step being left.
 *
 * Moving between steps disposes whatever the step being left held in composition, which would
 * otherwise take the typed phone number and the fetched TOTP secret with it. Remembered by the host
 * *above* the [androidx.navigation3.ui.NavDisplay] and handed to every step through
 * [mfaEnrollmentDestinations], this is what a step reads and writes instead of its own local state.
 *
 * [selectedFactor], [phoneNumber], [verificationCode], [resendTimerSeconds], [smsSession] and
 * [selectedCountry] are backed by [rememberSaveable] in [rememberMfaEnrollmentFlowState] and
 * survive Activity recreation. [totpSecret] and [totpQrCodeUrl] do not — they are backed by plain
 * [remember], since `com.google.firebase.auth.TotpSecret` cannot be reconstructed. That loss is
 * recovered: [MfaEnrollmentScreen] detects a null [totpSecret] on [MfaEnrollmentStep.ConfigureTotp]
 * or [MfaEnrollmentStep.VerifyFactor] and fetches a fresh one, bouncing back to `ConfigureTotp`
 * with [totpSecretExpiredMessage] set when the user was already past it, since a new secret means a
 * new QR code to scan.
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
     * Puts every field back to the value [rememberMfaEnrollmentFlowState] creates it with.
     *
     * Nothing else clears this state: the host remembers one instance above the
     * [androidx.navigation3.ui.NavDisplay] and hands it to every step, and six of the nine fields
     * are [rememberSaveable], so a value typed during one enrolment outlives both the step that
     * took it and the flow it belonged to.
     *
     * Called on **entry** to the flow rather than on completion, because completion is not the only
     * way out — a skip, a back press off the lowest step, a signed-out step redirecting, or a
     * process death mid-flow all leave state behind, and only entry is reached by every one of
     * them. Entry is also the point where a stale value is actually harmful: across a sign-out, the
     * phone number and verification code the next enrolment would open pre-filled with belong to
     * the previous user.
     *
     * Public because an external host driving [MfaEnrollmentScreen] with its own `step` and
     * `flowState` has the same state to clear and no access to the internal
     * `NavBackStack.enterMfaEnrollment` helper [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen]
     * funnels its own entries through.
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
 * with. Called once, above the `NavDisplay`, so the same instance is handed to every step — see
 * [MfaEnrollmentFlowState] for which of its fields actually survive Activity recreation.
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
 * Every move between steps **pushes**; there is no destination this flow ever *replaces* another
 * with. No enrolment or verification failure here moves the user off the step they are on — every
 * one sets [MfaEnrollmentContentState.error] and stays put.
 *
 * @param flowState The state that must outlive a step switch — see [MfaEnrollmentFlowState].
 * Shared by every step this registers, and expected to be `remember`-ed by the host once, above
 * the `NavDisplay`.
 */
internal fun EntryProviderScope<NavKey>.mfaEnrollmentDestinations(
    backStack: NavBackStack<NavKey>,
    configuration: MfaConfiguration,
    authConfiguration: AuthUIConfiguration?,
    authUI: FirebaseAuthUI,
    flowState: MfaEnrollmentFlowState,
    content: (@Composable (MfaEnrollmentContentState) -> Unit)?,
    onComplete: () -> Unit,
    onSkip: () -> Unit = {},
    onError: (Exception) -> Unit = {},
) {
    val body: @Composable (AuthRoute.MfaEnrollment.Step) -> Unit = { step ->
        val user = authUI.getCurrentUser()
        if (user != null) {
            MfaEnrollmentScreen(
                user = user,
                auth = authUI.auth,
                configuration = configuration,
                authConfiguration = authConfiguration,
                content = content,
                step = step.enrollmentStep,
                onNavigateToStep = { target ->
                    backStack.navigateToMfaStep(AuthRoute.MfaEnrollment.stepFor(target))
                },
                onNavigateBack = { backStack.popOrNull() },
                flowState = flowState,
                onComplete = onComplete,
                onSkip = onSkip,
                onError = onError,
            )
        } else {
            // A single pop here cascades: the step below composes with a null user and pops again.
            LaunchedEffect(Unit) { backStack.exitMfaEnrollment() }
            RedirectingStep()
        }
    }

    entry<AuthRoute.MfaEnrollment.SelectFactor>(
        metadata = authRouteMetadata(AuthRoute.MfaEnrollment.SelectFactor)
    ) { body(it) }
    entry<AuthRoute.MfaEnrollment.ConfigureSms>(
        metadata = authRouteMetadata(AuthRoute.MfaEnrollment.ConfigureSms)
    ) { body(it) }
    entry<AuthRoute.MfaEnrollment.ConfigureTotp>(
        metadata = authRouteMetadata(AuthRoute.MfaEnrollment.ConfigureTotp)
    ) { body(it) }
    entry<AuthRoute.MfaEnrollment.VerifyFactor>(
        metadata = authRouteMetadata(AuthRoute.MfaEnrollment.VerifyFactor)
    ) { body(it) }
}

/**
 * Pushes [step] onto the back stack. Always a push, never a pop-then-push.
 *
 * Upholds [pushUnique]'s precondition: this flow only ever moves *forward* through here —
 * `SelectFactor` → a `Configure…` step → `VerifyFactor` — and every backward move goes through
 * [popOrNull] instead, so a step this pushes is never already buried. A step that could be
 * re-entered from above would take [pushUnique]'s buried-case trim.
 */
internal fun NavBackStack<NavKey>.navigateToMfaStep(step: AuthRoute.MfaEnrollment.Step) {
    pushUnique(step)
}

/**
 * Whether [this] names the MFA enrolment flow — the [AuthRoute.MfaEnrollment] flow entry, or one of
 * its [AuthRoute.MfaEnrollment.Step]s named directly.
 *
 * One clause covers both spellings because [AuthRoute.toKey] already collapses the
 * [AuthRoute.FlowEntry] / [AuthRoute.Destination] split: a flow entry resolves to its own start
 * step and a destination resolves to itself, so `MfaEnrollment` and its steps are exactly the
 * routes whose key is a step of this flow.
 */
internal val AuthRoute.entersMfaEnrollment: Boolean
    get() = toKey() is AuthRoute.MfaEnrollment.Step

/**
 * Enters the MFA enrolment flow at the step [route] asks for, clearing [flowState] first.
 *
 * The reset and the push are one call so that a host cannot enter the flow without clearing it —
 * the defect this exists for was two entry call sites in
 * [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen] that both pushed a step directly. Call it
 * for any [route] [entersMfaEnrollment] accepts.
 *
 * Honours the step [route] names instead of always resolving through [mfaEnrollmentStartStep]: a
 * host naming [AuthRoute.MfaEnrollment.SelectFactor] under a single-factor configuration is asking
 * for the picker, and redirecting it to that factor's configuration step would be a behavior
 * change. Only [AuthRoute.MfaEnrollment], which names the flow rather than a step, is resolved.
 *
 * `pushUnique` invariant: both host call sites enter from a stack that cannot already hold a step
 * of this flow, so the buried-case trim never fires.
 */
internal fun NavBackStack<NavKey>.enterMfaEnrollment(
    route: AuthRoute,
    configuration: MfaConfiguration,
    flowState: MfaEnrollmentFlowState,
) {
    flowState.reset()
    pushUnique(
        when (route) {
            is AuthRoute.FlowEntry -> mfaEnrollmentStartStep(configuration)
            is AuthRoute.Destination -> route
        }
    )
}

/**
 * Leaves the MFA enrolment flow from whatever depth it reached, in one write: truncates to the
 * lowest step on the stack rather than popping repeatedly, so it does not matter how deep the flow
 * went, which step it started on, or whether it was entered more than once.
 *
 * A no-op when no step is on the stack, so leaving twice cannot disturb what is underneath. Resets
 * to [AuthRoute.Success] when the flow is the whole stack — `NavDisplay` throws on an empty one.
 */
internal fun NavBackStack<NavKey>.exitMfaEnrollment() {
    val lowestStep = indexOfFirst { it is AuthRoute.MfaEnrollment.Step }
    when {
        lowestStep < 0 -> return
        lowestStep == 0 -> resetBackStackTo(AuthRoute.Success)
        else -> while (size > lowestStep) removeAt(size - 1)
    }
}

/**
 * Where entering the MFA enrolment flow should land, resolved once at flow entry.
 *
 * A configuration offering exactly one factor has nothing to choose, so the flow starts on that
 * factor's own configuration step directly. [MfaEnrollmentScreen] still fetches the TOTP secret the
 * first time [AuthRoute.MfaEnrollment.ConfigureTotp] is entered, whichever way it was reached.
 */
internal fun mfaEnrollmentStartStep(configuration: MfaConfiguration): AuthRoute.MfaEnrollment.Step {
    return when (configuration.allowedFactors.singleOrNull()) {
        MfaFactor.Sms -> AuthRoute.MfaEnrollment.ConfigureSms
        MfaFactor.Totp -> AuthRoute.MfaEnrollment.ConfigureTotp
        null -> AuthRoute.MfaEnrollment.SelectFactor
    }
}
