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
    // Provider selection for this sheet, which is where a consumed challenge returns to. With a
    // single provider that is its own screen, so the credential attempt can simply be repeated.
    val returnToProviderSelection: () -> Unit = {
        sheetNavController.navigate(startRoute.route) {
            popUpTo(startRoute.route) { inclusive = true }
            launchSingleTop = true
        }
    }
    // Inside the sheet's own NavHost: on the outer one the challenge would render underneath
    // this modal, where the user cannot reach it.
    LaunchedEffect(mfaResolver) {
        if (mfaResolver != null) {
            sheetNavController.navigate(AuthRoute.MfaChallenge.route) { launchSingleTop = true }
        }
    }

    NavHost(
        navController = sheetNavController,
        startDestination = startRoute.route,
        enterTransition = { fadeIn(animationSpec = tween(700)) },
        exitTransition = { fadeOut(animationSpec = tween(700)) },
        popEnterTransition = { fadeIn(animationSpec = tween(700)) },
        popExitTransition = { fadeOut(animationSpec = tween(700)) },
    ) {
        composable(AuthRoute.MethodPicker.route) {
            if (customMethodPickerLayout != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    customMethodPickerLayout(reauthConfig.providers, onProviderSelected)
                }
            } else {
                // Same reasoning as FirebaseAuthScreen's Scaffold: flagged even though the
                // enclosing ModalBottomSheet already covers this subtree.
                Scaffold(modifier = Modifier.exposeTestTagsAsResourceIds()) { innerPadding ->
                    AuthMethodPicker(
                        modifier = Modifier.padding(innerPadding),
                        providers = reauthConfig.providers,
                        onProviderSelected = onProviderSelected,
                    )
                }
            }
        }

        composable(AuthRoute.Email.route) {
            com.firebase.ui.auth.ui.screens.email.EmailAuthScreen(
                context = context,
                configuration = reauthConfig,
                authUI = authUI,
                prefillEmail = prefillEmail,
                content = emailContent,
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

        composable(AuthRoute.Phone.route) {
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

        composable(AuthRoute.MfaChallenge.route) {
            if (mfaResolver != null) {
                MfaChallengeScreen(
                    resolver = mfaResolver,
                    auth = authUI.auth,
                    content = mfaChallengeContent,
                    // Resolving the challenge is what completed the reauthentication, so this is
                    // where the stamped Success for it is published.
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
