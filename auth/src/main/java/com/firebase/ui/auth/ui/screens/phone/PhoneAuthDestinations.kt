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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.data.CountryData
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.authRouteMetadata
import com.firebase.ui.auth.ui.screens.phoneStep
import com.firebase.ui.auth.ui.screens.popOrNull
import com.firebase.ui.auth.ui.screens.pushUnique
import com.firebase.ui.auth.util.CountryUtils
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * Everything a [PhoneAuthScreen] step needs that must outlive the step being left.
 *
 * Moving between steps disposes whatever the step being left held in composition, which would
 * otherwise take the typed number, the verification id and the live verification attempt with it.
 * Remembered by the host *above* the [androidx.navigation3.ui.NavDisplay] and handed to every step
 * through [phoneAuthDestinations], this is what a step reads and writes instead of its own local
 * state.
 *
 * [phoneNumber], [verificationCode], [verificationId], [forceResendingToken] and
 * [resendTimerSeconds] are backed by [rememberSaveable] and survive Activity recreation.
 * [selectedCountry] is not, matching what the un-hosted screen always did.
 *
 * @since 10.0.0
 */
class PhoneAuthFlowState internal constructor(
    val phoneNumber: MutableState<String>,
    val verificationCode: MutableState<String>,
    val selectedCountry: MutableState<CountryData>,
    val verificationId: MutableState<String?>,
    val forceResendingToken: MutableState<PhoneAuthProvider.ForceResendingToken?>,
    val resendTimerSeconds: MutableIntState,
    /** The number and start time of the attempt the cooldown check rejects a duplicate of. */
    internal val pendingVerificationPhoneNumber: MutableState<String?>,
    internal val verificationStartTime: MutableState<Long?>,
    /**
     * The live verification attempt, and a scope outliving the step that started it: the attempt
     * stays open until Firebase's auto-retrieval timeout, so a step-scoped scope would cancel it
     * on the way to code entry.
     */
    internal val verificationJob: MutableState<Job?>,
    internal val verificationScope: CoroutineScope,
    /**
     * The verification id already navigated on, and the auto-verified credential already signed in
     * with. Both steps observe the same auth state and are composed together for the length of a
     * transition, so both would otherwise act on the same emission twice.
     */
    internal val navigatedVerificationId: MutableState<String?>,
    internal val consumedAutoCredential: MutableState<PhoneAuthCredential?>,
)

/**
 * Creates and remembers the [PhoneAuthFlowState] a host installs [phoneAuthDestinations] with.
 * Called once, above the `NavDisplay`, so the same instance is handed to every step.
 *
 * Seeds [PhoneAuthFlowState.phoneNumber] and [PhoneAuthFlowState.selectedCountry] from
 * [configuration]'s phone provider, and from the platform default when it offers none.
 */
@Composable
fun rememberPhoneAuthFlowState(configuration: AuthUIConfiguration): PhoneAuthFlowState {
    val provider = configuration.providers.filterIsInstance<AuthProvider.Phone>().firstOrNull()
    val phoneNumber = rememberSaveable { mutableStateOf(provider?.defaultNumber ?: "") }
    val verificationCode = rememberSaveable { mutableStateOf("") }
    val selectedCountry = remember {
        mutableStateOf(
            provider?.defaultCountryCode?.let { code -> CountryUtils.findByCountryCode(code) }
                ?: CountryUtils.getDefaultCountry()
        )
    }
    val verificationId = rememberSaveable { mutableStateOf<String?>(null) }
    val forceResendingToken =
        rememberSaveable { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    val resendTimerSeconds = rememberSaveable { mutableIntStateOf(0) }
    val pendingVerificationPhoneNumber = remember { mutableStateOf<String?>(null) }
    val verificationStartTime = remember { mutableStateOf<Long?>(null) }
    val verificationJob = remember { mutableStateOf<Job?>(null) }
    val verificationScope = rememberCoroutineScope()
    val navigatedVerificationId = remember { mutableStateOf<String?>(null) }
    val consumedAutoCredential = remember { mutableStateOf<PhoneAuthCredential?>(null) }
    return remember {
        PhoneAuthFlowState(
            phoneNumber = phoneNumber,
            verificationCode = verificationCode,
            selectedCountry = selectedCountry,
            verificationId = verificationId,
            forceResendingToken = forceResendingToken,
            resendTimerSeconds = resendTimerSeconds,
            pendingVerificationPhoneNumber = pendingVerificationPhoneNumber,
            verificationStartTime = verificationStartTime,
            verificationJob = verificationJob,
            verificationScope = verificationScope,
            navigatedVerificationId = navigatedVerificationId,
            consumedAutoCredential = consumedAutoCredential,
        )
    }
}

/**
 * Registers the phone flow's two steps on [this] entry provider, each rendering its own step of a
 * [PhoneAuthScreen] driven from the outside.
 *
 * Reaching code entry is a push, so back returns to number entry. Leaving goes through
 * [exitPhoneAuth], which drops both entries at once.
 *
 * @param flowState The state that must outlive a step switch — see [PhoneAuthFlowState]. Shared by
 * every step this registers, and expected to be `remember`-ed by the host once, above the
 * `NavDisplay`.
 * @param onCancel Invoked when the flow is *left*, not when stepping back to number entry.
 */
internal fun EntryProviderScope<NavKey>.phoneAuthDestinations(
    backStack: NavBackStack<NavKey>,
    context: Context,
    configuration: AuthUIConfiguration,
    authUI: FirebaseAuthUI,
    flowState: PhoneAuthFlowState,
    content: (@Composable (PhoneAuthContentState) -> Unit)?,
    onCancel: () -> Unit,
    onError: (AuthException) -> Unit = {},
) {
    val body: @Composable (AuthRoute.Phone.Step) -> Unit = { key ->
        PhoneAuthScreen(
            context = context,
            configuration = configuration,
            authUI = authUI,
            // The host's own auth-state observer owns where a completed sign-in navigates.
            onSuccess = {},
            onError = onError,
            onCancel = onCancel,
            step = key.phoneStep,
            onNavigateToStep = { target ->
                backStack.navigateToPhoneStep(AuthRoute.Phone.stepFor(target))
            },
            onNavigateBack = { backStack.popOrNull() },
            flowState = flowState,
            content = content,
        )
    }

    entry<AuthRoute.Phone.EnterPhoneNumber>(
        metadata = authRouteMetadata(AuthRoute.Phone.EnterPhoneNumber)
    ) { body(it) }
    entry<AuthRoute.Phone.EnterVerificationCode>(
        metadata = authRouteMetadata(AuthRoute.Phone.EnterVerificationCode)
    ) { body(it) }
}

/**
 * Pushes [step], leaving the step below reachable, and does nothing when it is already on top.
 *
 * Upholds [pushUnique]'s precondition: the flow only moves forward through here — number entry to
 * code entry — and every backward move goes through [popOrNull] instead.
 */
internal fun NavBackStack<NavKey>.navigateToPhoneStep(step: AuthRoute.Phone.Step) {
    if (lastOrNull() == step) return
    pushUnique(step)
}

/**
 * Leaves the phone flow from whatever depth it reached, in one write: truncates to the lowest phone
 * step on the stack rather than popping repeatedly, so it does not matter how deep the flow went or
 * whether it was entered more than once.
 *
 * Returns whether anything was removed. Changes nothing, and returns false, when no step is on the
 * stack or the flow is the whole of it — `NavDisplay` throws on an empty back stack, so the caller
 * decides what replaces it.
 */
internal fun NavBackStack<NavKey>.exitPhoneAuth(): Boolean {
    val lowestStep = indexOfFirst { it is AuthRoute.Phone.Step }
    if (lowestStep <= 0) return false
    while (size > lowestStep) removeAt(size - 1)
    return true
}
