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

package com.firebase.ui.auth.ui.screens.email

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.EMAIL_ARG
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult

/**
 * Registers the email flow's steps on [this] graph.
 *
 * Each step hosts an [EmailAuthScreen] pinned to its own [EmailAuthMode], turning mode switches
 * into navigation through [navigateToEmailStep]; the address travels as [EMAIL_ARG] so it survives
 * a switch. Errors are not acted on here: moving the flow on an
 * [com.firebase.ui.auth.AuthState.Error] is the host's job alone.
 *
 * @param onCancel Invoked when the flow is *left*, not when stepping between email steps.
 * @param prefillEmail The address already fixed for this flow, used to seed a step entered without
 * [EMAIL_ARG]. Read during a step's composition, so it must not be state that changes while that
 * step is on screen.
 * @param onEmailTyped Reports the address as the user edits it, so a host-driven recovery that
 * does not itself know the address can carry the live value.
 */
internal fun NavGraphBuilder.emailAuthDestinations(
    navController: NavHostController,
    context: Context,
    configuration: AuthUIConfiguration,
    authUI: FirebaseAuthUI,
    content: (@Composable (EmailAuthContentState) -> Unit)?,
    onCancel: () -> Unit,
    prefillEmail: () -> String? = { null },
    credentialForLinking: () -> AuthCredential? = { null },
    emailLinkFromDifferentDevice: () -> String? = { null },
    onEmailTyped: (String) -> Unit = {},
    onSuccess: (AuthResult) -> Unit = {},
    onError: (AuthException) -> Unit = {},
) {
    AuthRoute.Email.steps.forEach { step ->
        composable(
            route = step.routePattern,
            arguments = listOf(
                navArgument(EMAIL_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            ),
        ) { entry ->
            val routeEmail = entry.arguments?.getString(EMAIL_ARG).orEmpty()

            if (!configuration.isEmailStepOffered(step)) {
                // Keyed on Unit: the redirect must run at most once per back-stack entry.
                LaunchedEffect(Unit) {
                    // Pop first; navigating alone leaves the unreachable step for back to return to.
                    navController.popBackStack(step.routePattern, inclusive = true)
                    navController.navigateToEmailStep(AuthRoute.Email.SignIn, routeEmail)
                }
                RedirectingStep()
                return@composable
            }

            EmailAuthScreen(
                context = context,
                configuration = configuration,
                authUI = authUI,
                prefillEmail = routeEmail.ifEmpty { prefillEmail() },
                credentialForLinking = credentialForLinking(),
                emailLinkFromDifferentDevice = emailLinkFromDifferentDevice(),
                content = content,
                mode = step.mode,
                onNavigateToMode = { targetMode, email ->
                    navController.navigateToEmailStep(AuthRoute.Email.stepFor(targetMode), email)
                },
                onEmailTyped = onEmailTyped,
                onSuccess = onSuccess,
                onError = onError,
                onCancel = {
                    val previous = navController.previousBackStackEntry
                    if (previous != null && AuthRoute.Email.isStep(previous.destination.route)) {
                        navController.popBackStack()
                    } else {
                        onCancel()
                    }
                },
            )
        }
    }
}

/**
 * Moves the flow to [step], carrying [email] so the typed address survives the step being left.
 *
 * Pops any existing entry for [step], then pushes a fresh one:
 *
 * * **Already on the stack — replaces.** Toggling sign-in ↔ sign-up cannot grow the stack, and a
 *   recovery removes the form that just failed rather than leaving it for back to return to.
 * * **Not on the stack — pushes.** The origin stays reachable.
 *
 * Pushing rather than only popping means the address just typed wins over the stale one the old
 * entry held, and saved state is not restored, so a mode switch's password clear survives. System
 * back is the one case that does restore it.
 *
 * The push always leaves an entry for [step], so the graph's start destination can never be popped
 * out from under it.
 */
internal fun NavHostController.navigateToEmailStep(step: AuthRoute.Email.Step, email: String?) {
    navigate(step.withEmail(email)) {
        popUpTo(step.routePattern) { inclusive = true }
    }
}

/** Shown for as long as a redirect off a step that cannot render itself takes. */
@Composable
internal fun RedirectingStep() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** Whether [step] is reachable at all in this configuration. */
internal fun AuthUIConfiguration.isEmailStepOffered(step: AuthRoute.Email.Step): Boolean =
    when (step) {
        AuthRoute.Email.SignUp -> isEmailSignUpOffered()
        AuthRoute.Email.EmailLinkSignIn -> isEmailLinkSignInOffered()
        AuthRoute.Email.SignIn, AuthRoute.Email.ResetPassword -> true
    }

/**
 * Whether the email flow may offer account creation. False while reauthenticating, and whenever
 * the configuration or the email provider itself disables new accounts.
 */
internal fun AuthUIConfiguration.isEmailSignUpOffered(): Boolean {
    if (isReauthenticationMode || !isNewEmailAccountsAllowed) return false
    return providers.filterIsInstance<AuthProvider.Email>()
        .firstOrNull()
        ?.isNewAccountsAllowed == true
}

/**
 * Whether the email flow may offer email-link sign-in. False while reauthenticating: a link
 * reopens the app with nothing armed, so completing one there reports an interruption.
 */
internal fun AuthUIConfiguration.isEmailLinkSignInOffered(): Boolean {
    if (isReauthenticationMode) return false
    return providers.filterIsInstance<AuthProvider.Email>()
        .firstOrNull()
        ?.isEmailLinkSignInEnabled == true
}
