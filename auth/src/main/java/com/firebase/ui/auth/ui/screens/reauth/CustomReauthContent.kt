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

import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebase.ui.auth.ui.screens.email.EmailAuthScreen
import com.firebase.ui.auth.ui.screens.mfa.MfaChallengeScreen
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthScreen
import com.firebase.ui.auth.ui.screens.rememberOnProviderSelected
import com.google.firebase.auth.MultiFactorResolver

/**
 * Custom reauth UI — renders the caller's [content] slot, and *replaces* it with the library's own
 * email/phone sub-flow while the user is in one, i.e. after selecting [AuthProvider.Email] or
 * [AuthProvider.Phone]. Cancelling the sub-flow composes [content] again from scratch, so any state
 * the caller `remember`ed inside the slot is lost — the slot is a stateless provider chooser by
 * design. Every other provider runs the library credential exchange in place, which routes to
 * `reauthenticateWithCredential` because [reauthConfig] is in reauthentication mode.
 *
 * Only [onDismiss] abandons reauthentication; cancelling a sub-flow merely returns to [content].
 *
 * @param activeSubRoute Which sub-flow, if any, currently replaces [content].
 * @param onActiveSubRouteChange Invoked when the active sub-flow opens or closes.
 * @param onAttemptStarted Invoked just before an in-place credential attempt begins.
 * @param mfaResolver Non-null while the reauthentication needs a second factor resolved.
 * @param onMfaError Invoked when resolving the second factor fails.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomReauthContent(
    authUI: FirebaseAuthUI,
    reauthConfig: AuthUIConfiguration,
    reauthState: AuthState.Reauthentication.Required,
    activity: android.app.Activity?,
    context: android.content.Context,
    emailContent: (@Composable (EmailAuthContentState) -> Unit)?,
    phoneContent: (@Composable (PhoneAuthContentState) -> Unit)?,
    mfaChallengeContent: (@Composable (MfaChallengeContentState) -> Unit)?,
    mfaResolver: MultiFactorResolver?,
    isLoading: Boolean,
    error: String?,
    exception: Exception?,
    activeSubRoute: AuthRoute?,
    onActiveSubRouteChange: (AuthRoute?) -> Unit,
    onAttemptStarted: () -> Unit,
    onMfaError: (Exception) -> Unit,
    onDismiss: () -> Unit,
    content: @Composable (ReauthContentState) -> Unit,
) {
    val openSubFlow: (AuthRoute) -> Unit = remember(onActiveSubRouteChange) {
        { route -> onActiveSubRouteChange(route) }
    }
    val onProviderSelected = authUI.rememberOnProviderSelected(
        context = context,
        activity = activity,
        config = reauthConfig,
        onNavigate = openSubFlow,
    )
    // rememberOnProviderSelected returns a fresh lambda per recomposition, so read it through a
    // holder rather than keying on it — otherwise this remember would never hit.
    val currentOnProviderSelected = rememberUpdatedState(onProviderSelected)
    val onProviderSelectedFromSlot: (AuthProvider) -> Unit = remember(onAttemptStarted) {
        { provider ->
            // Email and Phone only open a sub-flow; clearing the latched error there would wipe a
            // real failure on a mis-tap, and `error` is documented to survive backing out.
            if (provider !is AuthProvider.Email && provider !is AuthProvider.Phone) {
                onAttemptStarted()
            }
            currentOnProviderSelected.value(provider)
        }
    }
    val closeSubFlow: () -> Unit = remember(
        authUI,
        reauthState.requestId,
        onActiveSubRouteChange,
    ) {
        {
            authUI.updateReauthentication(reauthState.requestId) { it.attemptCancelled() }
            onActiveSubRouteChange(null)
        }
    }

    val slotState = ReauthContentState(
        user = reauthState.user,
        reason = reauthState.reason,
        providers = reauthConfig.providers,
        onProviderSelected = onProviderSelectedFromSlot,
        isLoading = isLoading,
        error = error,
        onDismiss = onDismiss,
        exception = exception,
    )

    when (val subRoute = activeSubRoute) {
        null -> content(slotState)

        AuthRoute.Email -> ModalBottomSheet(
            onDismissRequest = closeSubFlow,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            EmailAuthScreen(
                context = context,
                configuration = reauthConfig,
                authUI = authUI,
                prefillEmail = reauthState.user.email,
                content = emailContent,
                onSuccess = {},
                onError = {},
                onCancel = closeSubFlow,
            )
        }

        AuthRoute.Phone -> ModalBottomSheet(
            onDismissRequest = closeSubFlow,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            PhoneAuthScreen(
                context = context,
                configuration = reauthConfig,
                authUI = authUI,
                content = phoneContent,
                onSuccess = {},
                onError = {},
                onCancel = closeSubFlow,
            )
        }

        AuthRoute.MfaChallenge -> if (mfaResolver != null) {
            ModalBottomSheet(
                onDismissRequest = closeSubFlow,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                MfaChallengeScreen(
                    resolver = mfaResolver,
                    auth = authUI.auth,
                    content = mfaChallengeContent,
                    // Resolving the challenge is what completed the reauthentication, so this is
                    // where the stamped Success for it is published.
                    onSuccess = { authUI.publishReauthenticationSuccess() },
                    onCancel = closeSubFlow,
                    onError = onMfaError,
                )
            }
        } else {
            content(slotState)
        }

        else -> {
            // No sub-flow for this route: keep the caller's slot rather than crashing
            // composition. Add a branch when a new provider gains its own screen.
            LaunchedEffect(subRoute) {
                Log.w(
                    "FirebaseAuthScreen",
                    "No reauth sub-flow for ${subRoute.route}; staying on the slot"
                )
                onActiveSubRouteChange(null)
            }
            content(slotState)
        }
    }
}
