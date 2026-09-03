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

package com.firebase.ui.auth.ui.screens.reauth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.AuthStateSink

/**
 * The reauthentication phase machine of one
 * [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen].
 *
 * Scoped to the composition that created it, which is what makes it the answer to "is there a
 * screen able to drive an armed request to completion?" — the question
 * `FirebaseAuthUI.addReauthenticationDrainer` used to answer with a counter on the singleton.
 * Every transition below runs from a composed screen, so an arming with nothing composed stays
 * inert without anything having to count screens.
 *
 * @since 10.0.0
 */
internal class ReauthFlowState internal constructor(
    private val phaseState: MutableState<AuthState.Reauthentication?>,
) {
    /** The live phase, or null when no request is armed. */
    val phase: AuthState.Reauthentication? get() = phaseState.value

    /** The live request, or null when none is armed. */
    val request: AuthState.Reauthentication.Request? get() = phaseState.value?.request

    /** Arms [required], replacing any request already held. */
    fun arm(required: AuthState.Reauthentication.Required) {
        phaseState.value = required
    }

    /**
     * Drops the armed request and tells its awaiting caller whether to retry.
     *
     * Resolving here rather than at each call site is what stops a caller being left suspended
     * forever: every way a request ends comes through this, including the ones that end it because
     * the user backed out.
     */
    fun finish(retryOperation: Boolean) {
        val request = phaseState.value?.request
        phaseState.value = null
        request?.resolve(retryOperation)
    }

    /**
     * Applies [transition] to the live phase while [requestId] still names it.
     *
     * The id check is what `FirebaseAuthUI.updateReauthentication` needed against a shared flow
     * any caller could have overwritten. Here it only guards a back stack entry one composition
     * behind the phase, so it compares a key the caller already holds rather than arbitrating
     * between writers.
     */
    fun update(requestId: String, transition: (AuthState.Reauthentication) -> AuthState?) {
        val current = phaseState.value ?: return
        if (current.requestId != requestId) return
        val next = transition(current) as? AuthState.Reauthentication ?: return
        phaseState.value = next
    }

    /** Moves the live phase to [phase] unconditionally. */
    fun moveTo(phase: AuthState.Reauthentication) {
        phaseState.value = phase
    }

    /**
     * This request's own state sink, for the provider code driving its credential exchange.
     *
     * Everything the exchange publishes becomes a phase here rather than a state on the public
     * flow, which is what stops an app's collector acting on a `Loading` or `Error` belonging to a
     * conversation that is not theirs. [hostFallback] takes what [fold] declines: those states are
     * not part of the exchange, so they are still the host's to handle.
     */
    fun sink(hostFallback: AuthStateSink): AuthStateSink = AuthStateSink { state ->
        if (fold(state) == null) hostFallback.emit(state)
    }

    /**
     * Folds an ordinary [state] published by provider code into the live phase, returning the
     * phase it became, or null when [state] is not part of the credential exchange.
     *
     * This is `FirebaseAuthUI.contextualizeReauthenticationState` relocated off the singleton's
     * setter. Provider implementations still publish only ordinary states and still need no
     * parallel session storage of their own, but the mapping now reads the phase it owns instead
     * of read-modify-writing the flow it is being written to.
     */
    fun fold(state: AuthState): AuthState.Reauthentication? {
        if (state is AuthState.Reauthentication) return null
        val current = phaseState.value ?: return null
        val request = current.request ?: return null

        val next = when (state) {
            is AuthState.Loading -> AuthState.Reauthentication.Authenticating(request, state.message)

            is AuthState.Error ->
                if (state.exception is AuthException.AuthCancelledException) {
                    AuthState.Reauthentication.Required(request)
                } else {
                    AuthState.Reauthentication.AttemptFailed(request, state.exception)
                }

            is AuthState.Cancelled -> AuthState.Reauthentication.Required(request)

            is AuthState.RequiresMfa ->
                AuthState.Reauthentication.RequiresMfa(request, state.resolver, state.hint)

            is AuthState.PhoneNumberVerificationRequired ->
                AuthState.Reauthentication.PhoneNumberVerificationRequired(
                    request = request,
                    verificationId = state.verificationId,
                    forceResendingToken = state.forceResendingToken,
                )

            is AuthState.SMSAutoVerified ->
                AuthState.Reauthentication.SmsAutoVerified(request, state.credential)

            is AuthState.PasswordResetLinkSent ->
                AuthState.Reauthentication.PasswordResetLinkSent(request)

            is AuthState.EmailSignInLinkSent ->
                AuthState.Reauthentication.EmailSignInLinkSent(request)

            // Only a stamped Success proves this user was re-verified. An unstamped one is an
            // ambient FirebaseAuth emission and leaves the phase alone.
            is AuthState.Success ->
                if (state.reauthenticatedUid != null) {
                    AuthState.Reauthentication.Succeeded(request, state)
                } else {
                    current
                }

            // Ambient emissions and notification cleanup while a request is armed. They must not
            // detach the request from the caller waiting on it.
            is AuthState.Idle,
            is AuthState.RequiresEmailVerification,
            is AuthState.RequiresProfileCompletion,
                -> current

            // Not part of the credential exchange: let the host flow handle it.
            else -> return null
        }
        phaseState.value = next
        return next
    }
}

/**
 * Creates and remembers the [ReauthFlowState] for one
 * [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen].
 *
 * Called once, above the `NavDisplay`, alongside `rememberPhoneAuthFlowState` and
 * `rememberMfaEnrollmentFlowState`, and composition-scoped like both: a phase that outlived its
 * Activity would let the next screen re-arm from it, putting a reauthentication sheet into an
 * unrelated sign-in. What survives recreation is the back stack's
 * [com.firebase.ui.auth.ui.screens.AuthRoute.Reauth] marker and the armed
 * [AuthState.Reauthentication.Required] itself, which is enough to arm again — and the request's
 * resolver is what says whether the caller behind it is still there to resume.
 */
@Composable
internal fun rememberReauthFlowState(): ReauthFlowState =
    remember { ReauthFlowState(mutableStateOf(null)) }
