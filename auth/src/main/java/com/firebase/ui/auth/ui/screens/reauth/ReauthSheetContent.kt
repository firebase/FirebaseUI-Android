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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.ui.exposeTestTagsAsResourceIds
import com.firebase.ui.auth.ui.method_picker.AuthMethodPicker
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebase.ui.auth.ui.screens.email.emailAuthDestinations
import com.firebase.ui.auth.ui.screens.getStartRoute
import com.firebase.ui.auth.ui.screens.mfa.MfaChallengeScreen
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebase.ui.auth.ui.screens.rememberOnProviderSelected
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
    val sheetNavController = rememberNavController()
    val startRoute = remember(reauthConfig) { getStartRoute(reauthConfig) }
    val skipsMethodPicker = startRoute != AuthRoute.MethodPicker
    val onProviderSelected = authUI.rememberOnProviderSelected(
        context = context,
        activity = activity,
        config = reauthConfig,
        onNavigate = { route -> sheetNavController.navigate(route.route) },
    )
    val returnToProviderSelection: () -> Unit = {
        sheetNavController.navigate(startRoute.route) {
            // popUpTo matches a registered destination, so it takes the pattern, not the route.
            popUpTo(startRoute.routePattern) { inclusive = true }
            launchSingleTop = true
        }
    }
    // Must be the sheet's own NavHost: on the outer one the challenge renders under this modal.
    LaunchedEffect(mfaResolver) {
        if (mfaResolver != null) {
            sheetNavController.navigate(AuthRoute.MfaChallenge.route) { launchSingleTop = true }
        }
    }

    NavHost(
        navController = sheetNavController,
        startDestination = startRoute.routePattern,
        enterTransition = { fadeIn(animationSpec = tween(700)) },
        exitTransition = { fadeOut(animationSpec = tween(700)) },
        popEnterTransition = { fadeIn(animationSpec = tween(700)) },
        popExitTransition = { fadeOut(animationSpec = tween(700)) },
    ) {
        composable(AuthRoute.MethodPicker.routePattern) {
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
            navController = sheetNavController,
            context = context,
            configuration = reauthConfig,
            authUI = authUI,
            content = emailContent,
            prefillEmail = { prefillEmail },
            onCancel = {
                if (skipsMethodPicker || !sheetNavController.popBackStack()) {
                    onDismiss()
                } else {
                    authUI.updateReauthentication(requestId) { it.attemptCancelled() }
                }
            },
        )

        // Every phone step, as on the main graph; registering only the start step strands the rest.
        AuthRoute.Phone.steps.forEach { step ->
            composable(step.routePattern) {
                com.firebase.ui.auth.ui.screens.phone.PhoneAuthScreen(
                    context = context,
                    configuration = reauthConfig,
                    authUI = authUI,
                    content = phoneContent,
                    onSuccess = {},
                    onError = {},
                    onCancel = {
                        if (skipsMethodPicker || !sheetNavController.popBackStack()) {
                            onDismiss()
                        } else {
                            authUI.updateReauthentication(requestId) { it.attemptCancelled() }
                        }
                    }
                )
            }
        }

        composable(AuthRoute.MfaChallenge.routePattern) {
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
    }
}
