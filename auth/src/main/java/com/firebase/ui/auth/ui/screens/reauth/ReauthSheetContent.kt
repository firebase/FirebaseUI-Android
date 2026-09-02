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

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.ui.exposeTestTagsAsResourceIds
import com.firebase.ui.auth.ui.method_picker.AuthMethodPicker
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.authRouteMetadata
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebase.ui.auth.ui.screens.email.emailAuthDestinations
import com.firebase.ui.auth.ui.screens.getStartRoute
import com.firebase.ui.auth.ui.screens.mfa.MfaChallengeScreen
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebase.ui.auth.ui.screens.popOrNull
import com.firebase.ui.auth.ui.screens.pushUnique
import com.firebase.ui.auth.ui.screens.rememberOnProviderSelected
import com.firebase.ui.auth.ui.screens.resetBackStackTo
import com.firebase.ui.auth.ui.screens.toKey
import com.google.firebase.auth.MultiFactorResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReauthSheetContent(
    authUI: FirebaseAuthUI,
    reauthConfig: AuthUIConfiguration,
    requestId: String,
    activity: android.app.Activity?,
    context: android.content.Context,
    prefillEmail: String?,
    emailContent: (@Composable (EmailAuthContentState) -> Unit)?,
    phoneContent: (@Composable (PhoneAuthContentState) -> Unit)?,
    mfaChallengeContent: (@Composable (MfaChallengeContentState) -> Unit)?,
    mfaResolver: MultiFactorResolver?,
    customMethodPickerLayout: (@Composable (List<AuthProvider>, (AuthProvider) -> Unit) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val startRoute = remember(reauthConfig) { getStartRoute(reauthConfig) }
    val skipsMethodPicker = startRoute != AuthRoute.MethodPicker
    val sheetBackStack = rememberNavBackStack(startRoute.toKey())
    val onProviderSelected = authUI.rememberOnProviderSelected(
        context = context,
        activity = activity,
        config = reauthConfig,
        // pushUnique invariant: the picker is this stack's only entry, so nothing can be buried.
        onNavigate = { route -> sheetBackStack.pushUnique(route) },
    )
    val returnToProviderSelection: () -> Unit = {
        sheetBackStack.resetBackStackTo(startRoute)
    }
    LaunchedEffect(mfaResolver) {
        // pushUnique invariant: nothing is pushed on top of the challenge here either.
        if (mfaResolver != null && sheetBackStack.lastOrNull() != AuthRoute.MfaChallenge) {
            sheetBackStack.pushUnique(AuthRoute.MfaChallenge)
        }
    }

    /**
     * Leaving an email or phone sub-flow that has nothing left underneath it.
     *
     * The size test sits *above* the branch, and the dismissing branch never pops at all: on this
     * surface an exhausted stack means the sheet should stop existing, and `NavDisplay` keeps
     * composing until the dismiss animation finishes, so a list emptied on the way out would throw
     * during that window.
     */
    val leaveOrCancel: () -> Unit = {
        if (skipsMethodPicker || sheetBackStack.size <= 1) {
            onDismiss()
        } else {
            sheetBackStack.popOrNull()
            authUI.updateReauthentication(requestId) { it.attemptCancelled() }
        }
    }

    val phoneStep: @Composable () -> Unit = {
        com.firebase.ui.auth.ui.screens.phone.PhoneAuthScreen(
            context = context,
            configuration = reauthConfig,
            authUI = authUI,
            content = phoneContent,
            onSuccess = {},
            onError = {},
            onCancel = leaveOrCancel,
        )
    }

    NavDisplay(
        backStack = sheetBackStack,
        onBack = { sheetBackStack.popOrNull() },
        transitionSpec = {
            fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
        },
        popTransitionSpec = {
            fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
        },
        predictivePopTransitionSpec = {
            fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
        },
        entryProvider = entryProvider {
            entry<AuthRoute.MethodPicker>(
                metadata = authRouteMetadata(AuthRoute.MethodPicker)
            ) {
                if (customMethodPickerLayout != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        customMethodPickerLayout(reauthConfig.providers, onProviderSelected)
                    }
                } else {
                    Scaffold(modifier = Modifier.exposeTestTagsAsResourceIds()) { innerPadding ->
                        AuthMethodPicker(
                            modifier = Modifier.padding(innerPadding),
                            providers = reauthConfig.providers,
                            onProviderSelected = onProviderSelected,
                        )
                    }
                }
            }

            emailAuthDestinations(
                backStack = sheetBackStack,
                context = context,
                configuration = reauthConfig,
                authUI = authUI,
                content = emailContent,
                prefillEmail = { prefillEmail },
                onCancel = leaveOrCancel,
            )

            entry<AuthRoute.Phone.EnterPhoneNumber>(
                metadata = authRouteMetadata(AuthRoute.Phone.EnterPhoneNumber)
            ) { phoneStep() }
            entry<AuthRoute.Phone.EnterVerificationCode>(
                metadata = authRouteMetadata(AuthRoute.Phone.EnterVerificationCode)
            ) { phoneStep() }

            entry<AuthRoute.MfaChallenge>(
                metadata = authRouteMetadata(AuthRoute.MfaChallenge)
            ) {
                if (mfaResolver != null) {
                    MfaChallengeScreen(
                        resolver = mfaResolver,
                        auth = authUI.auth,
                        content = mfaChallengeContent,
                        onSuccess = { authUI.publishReauthenticationSuccess() },
                        onCancel = {
                            returnToProviderSelection()
                            authUI.updateReauthentication(requestId) { it.attemptCancelled() }
                        },
                        onError = { exception ->
                            returnToProviderSelection()
                            authUI.updateAuthState(AuthState.Error(exception))
                        },
                    )
                }
            }
        },
    )
}
