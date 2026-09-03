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
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.authRouteMetadata
import com.firebase.ui.auth.ui.screens.mode
import com.firebase.ui.auth.ui.screens.popOrNull
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult

/**
 * Registers the email flow's steps on [this] entry provider. Every host that offers email sign-in
 * installs this same extension, so the destinations and the rules for moving between them are
 * identical in the main [com.firebase.ui.auth.ui.screens.FirebaseAuthScreen] display and in the
 * reauthentication sheet.
 *
 * Each step hosts an [EmailAuthScreen] pinned to its own [EmailAuthMode], turning mode switches
 * into navigation through [navigateToEmailStep]; the address travels as a field on the key
 * ([AuthRoute.Email.Step.email]) so it survives a switch. Errors are not acted on here — moving the
 * flow on an [com.firebase.ui.auth.AuthState.Error] is the host's job alone.
 *
 * [authRouteMetadata] is stamped per key rather than per type, because an email step's key carries
 * the address.
 *
 * @param backStack The host's back stack, which is what a mode switch mutates.
 * @param onCancel Invoked when the flow is *left*, not when stepping between email steps.
 * @param prefillEmail The address already fixed for this flow (e.g. a reauthenticating user's own),
 * seeding a step entered with no address on its key. A getter, not a value, because an entry's
 * content lambda is built once per key — the entry rule at `FirebaseAuthScreen`'s entry provider.
 * @param onEmailTyped Reports the address as the user edits it, so a host-driven recovery that does
 * not know the address (e.g. [com.firebase.ui.auth.AuthException.UserNotFoundException]) can carry
 * the live value.
 */
internal fun EntryProviderScope<NavKey>.emailAuthDestinations(
    backStack: NavBackStack<NavKey>,
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
    /** Passed through to [EmailAuthScreen]: where a consumed notification leaves the flow. */
    onNotificationConsumed: (() -> Unit)? = null,
) {
    val body: @Composable (AuthRoute.Email.Step) -> Unit = { step ->
        EmailAuthStep(
            step = step,
            entryKey = step,
            backStack = backStack,
            context = context,
            configuration = configuration,
            authUI = authUI,
            content = content,
            navigateToStep = { backStack.navigateToEmailStep(it) },
            isStepBelow = { AuthRoute.Email.isStep(it) },
            onCancel = onCancel,
            prefillEmail = prefillEmail,
            credentialForLinking = credentialForLinking,
            emailLinkFromDifferentDevice = emailLinkFromDifferentDevice,
            onEmailTyped = onEmailTyped,
            onNotificationConsumed = onNotificationConsumed,
            onSuccess = onSuccess,
            onError = onError,
        )
    }

    entry<AuthRoute.Email.SignIn>(metadata = { authRouteMetadata(it) }) { body(it) }
    entry<AuthRoute.Email.SignUp>(metadata = { authRouteMetadata(it) }) { body(it) }
    entry<AuthRoute.Email.ResetPassword>(metadata = { authRouteMetadata(it) }) { body(it) }
    entry<AuthRoute.Email.EmailLinkSignIn>(metadata = { authRouteMetadata(it) }) { body(it) }
}

/**
 * One email step, as every host renders it: the reachability bounce, then an [EmailAuthScreen]
 * pinned to [step]'s mode.
 *
 * @param entryKey The key [step] is registered under — [step] itself, or the wrapper carrying it
 * ([AuthRoute.Reauth]). What the bounce removes, so a wrapped step drops its wrapper.
 * @param navigateToStep Moves this host to another email step, wrapping it as that host needs. The
 * bounce goes through it too, so a redirect stays inside the host's own family of keys.
 * @param isStepBelow Whether a key below the top is an email step of this host's, which is what
 * back stepping through the flow rather than leaving it turns on.
 */
@Composable
internal fun EmailAuthStep(
    step: AuthRoute.Email.Step,
    entryKey: NavKey,
    backStack: NavBackStack<NavKey>,
    context: Context,
    configuration: AuthUIConfiguration,
    authUI: FirebaseAuthUI,
    content: (@Composable (EmailAuthContentState) -> Unit)?,
    navigateToStep: (AuthRoute.Email.Step) -> Unit,
    isStepBelow: (NavKey?) -> Boolean,
    onCancel: () -> Unit,
    prefillEmail: () -> String? = { null },
    credentialForLinking: () -> AuthCredential? = { null },
    emailLinkFromDifferentDevice: () -> String? = { null },
    onEmailTyped: (String) -> Unit = {},
    onSuccess: (AuthResult) -> Unit = {},
    onError: (AuthException) -> Unit = {},
    /** Passed through to [EmailAuthScreen]: where a consumed notification leaves the flow. */
    onNotificationConsumed: (() -> Unit)? = null,
) {
    if (!configuration.isEmailStepOffered(step)) {
        LaunchedEffect(entryKey) {
            // Push before dropping: no point in this pair leaves the stack empty for a later edit to stop at.
            navigateToStep(AuthRoute.Email.SignIn(step.email))
            backStack.remove(entryKey)
        }
        RedirectingStep()
    } else {
        EmailAuthScreen(
            context = context,
            configuration = configuration,
            authUI = authUI,
            prefillEmail = step.email?.ifEmpty { null } ?: prefillEmail(),
            credentialForLinking = credentialForLinking(),
            emailLinkFromDifferentDevice = emailLinkFromDifferentDevice(),
            content = content,
            mode = step.mode,
            onNavigateToMode = { targetMode, email ->
                navigateToStep(AuthRoute.Email.stepFor(targetMode, email))
            },
            onEmailTyped = onEmailTyped,
            onNotificationConsumed = onNotificationConsumed,
            onSuccess = onSuccess,
            onError = onError,
            onCancel = {
                if (isStepBelow(backStack.getOrNull(backStack.lastIndex - 1))) {
                    backStack.popOrNull()
                } else {
                    onCancel()
                }
            },
        )
    }
}

/**
 * Moves the flow to [step], carrying the address on the key so the typed value survives the step
 * being left disposing whatever it held.
 *
 * Drops any existing entry *of the same step type* — and everything above it — then leaves a fresh
 * one on top. So a step already on the stack is replaced, and one that is not is pushed, leaving
 * the origin reachable. The fresh entry means the address just typed wins over the stale one the
 * old entry held.
 *
 * Adds before removing, so no single write leaves the stack empty. Compares the step's *type*, not
 * the key: a key carries the address, so `SignIn("a@b")` and `SignIn("c@d")` are different keys.
 */
internal fun NavBackStack<NavKey>.navigateToEmailStep(step: AuthRoute.Email.Step) {
    val existing = indexOfFirst { it::class == step::class }
    add(step)
    if (existing >= 0) {
        // Drops the old entry and all above it, stopping below the one just pushed, so a buried step is reached.
        while (size > existing + 1) removeAt(existing)
    }
}

/** Overload for callers that name the step and the address separately. */
internal fun NavBackStack<NavKey>.navigateToEmailStep(
    step: AuthRoute.Email.Step,
    email: String?,
) = navigateToEmailStep(step.withEmail(email))

/**
 * Shown for as long as a redirect off a step that cannot render itself takes. Rendering nothing
 * leaves a hole on screen for the whole of the configured cross-fade, not for a single frame.
 */
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

/**
 * Whether [step] is reachable at all in this configuration. Asked of every step rather than only
 * sign-up, because `AuthSuccessUiContext.onNavigate` accepts any
 * [com.firebase.ui.auth.ui.screens.AuthRoute].
 */
internal fun AuthUIConfiguration.isEmailStepOffered(step: AuthRoute.Email.Step): Boolean =
    when (step) {
        is AuthRoute.Email.SignUp -> isEmailSignUpOffered()
        is AuthRoute.Email.EmailLinkSignIn -> isEmailLinkSignInOffered()
        is AuthRoute.Email.SignIn, is AuthRoute.Email.ResetPassword -> true
    }

/**
 * Whether the email flow may offer account creation. False while reauthenticating — proving an
 * existing identity can never end in a new account — and whenever the configuration or the email
 * provider itself disables new accounts.
 */
internal fun AuthUIConfiguration.isEmailSignUpOffered(): Boolean {
    if (isReauthenticationMode || !isNewEmailAccountsAllowed) return false
    return providers.filterIsInstance<AuthProvider.Email>()
        .firstOrNull()
        ?.isNewAccountsAllowed == true
}

/**
 * Whether the email flow may offer email-link sign-in. False while reauthenticating: a link
 * reopens the app with nothing armed, so completing one there reports an interruption instead of
 * finishing the pending operation.
 */
internal fun AuthUIConfiguration.isEmailLinkSignInOffered(): Boolean {
    if (isReauthenticationMode) return false
    return providers.filterIsInstance<AuthProvider.Email>()
        .firstOrNull()
        ?.isEmailLinkSignInEnabled == true
}
