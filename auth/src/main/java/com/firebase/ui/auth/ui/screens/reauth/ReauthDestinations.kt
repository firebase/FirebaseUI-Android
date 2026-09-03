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

import com.firebase.ui.auth.LocalAuthFlowScope
import com.firebase.ui.auth.AuthFlowScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.auth_provider.filterToLinkedProviders
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.ui.components.getRecoveryMessage
import com.firebase.ui.auth.ui.exposeTestTagsAsResourceIds
import com.firebase.ui.auth.ui.method_picker.AuthMethodPicker
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.authRouteMetadata
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebase.ui.auth.ui.screens.email.EmailAuthStep
import com.firebase.ui.auth.ui.screens.mfa.MfaChallengeScreen
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthFlowState
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthScreen
import com.firebase.ui.auth.ui.screens.phoneStep
import com.firebase.ui.auth.ui.screens.rememberOnProviderSelected
import com.firebase.ui.auth.ui.screens.toKey
import com.google.firebase.auth.FirebaseUser

/**
 * What the reauthentication surface shows: the phase, its request, and the providers the user may
 * reauthenticate with.
 *
 * Null means there is nothing to show and therefore no surface — [ReauthSceneStrategy] composes
 * the sheet only while this resolves, so existence and content answer to one condition and a
 * content-less sheet cannot be built.
 */
internal class ReauthSurface(
    val state: AuthState.Reauthentication,
    val request: AuthState.Reauthentication.Request,
    val configuration: AuthUIConfiguration,
)

/**
 * [this] resolved against [configuration], or null when there is no surface: this phase has none,
 * its request is gone, or nothing configured is linked to the user.
 */
internal fun AuthState.Reauthentication?.toReauthSurface(
    configuration: AuthUIConfiguration,
): ReauthSurface? {
    val state = this ?: return null
    val request = when (state) {
        is AuthState.Reauthentication.Required,
        is AuthState.Reauthentication.Authenticating,
        is AuthState.Reauthentication.AttemptFailed,
        is AuthState.Reauthentication.RequiresMfa,
        is AuthState.Reauthentication.PhoneNumberVerificationRequired,
        is AuthState.Reauthentication.SmsAutoVerified,
        is AuthState.Reauthentication.PasswordResetLinkSent,
        is AuthState.Reauthentication.EmailSignInLinkSent,
        // Momentary: the screen validates the proof and ends the request on it. The surface stays
        // up for that rather than flashing the flow underneath.
        is AuthState.Reauthentication.Succeeded,
            -> state.request
    } ?: return null
    val reauthConfiguration = configuration.toReauthConfiguration(request.user) ?: return null
    return ReauthSurface(state, request, reauthConfiguration)
}

/** The reauthentication entries currently on [this], topmost last. */
internal fun NavBackStack<NavKey>.reauthEntries(): List<AuthRoute.Reauth> =
    filterIsInstance<AuthRoute.Reauth>()

/** The reauthentication currently presented, or null. The back stack *is* the presentation marker. */
internal fun NavBackStack<NavKey>.presentedReauth(): AuthRoute.Reauth? =
    reauthEntries().lastOrNull()

/** Removes every reauthentication entry. Index 0 is always a non-reauth entry, so never empties. */
internal fun NavBackStack<NavKey>.clearReauth() {
    removeAll { it is AuthRoute.Reauth }
}

/** Drops every reauthentication entry above the first one — the surface's own start step. */
internal fun NavBackStack<NavKey>.returnToReauthStart() {
    val first = indexOfFirst { it is AuthRoute.Reauth }
    if (first < 0) return
    while (size > first + 1) removeAt(size - 1)
}

/**
 * Moves reauthentication to [step], replacing any entry already showing that step type — the same
 * rule [com.firebase.ui.auth.ui.screens.email.navigateToEmailStep] applies, scoped to the wrapper.
 */
internal fun NavBackStack<NavKey>.navigateReauth(
    marker: AuthRoute.Reauth,
    step: AuthRoute.Destination,
) {
    val target = marker.copy(step = step)
    val existing = indexOfFirst { it is AuthRoute.Reauth && it.step::class == step::class }
    add(target)
    if (existing >= 0) {
        while (size > existing + 1) removeAt(existing)
    }
}

/**
 * Registers reauthentication as a single wrapped destination on the host's own back stack.
 *
 * One entry type rather than a parallel route family: the same key in one stack cannot mean two
 * configurations, and [AuthRoute.Reauth] says "this step, in reauthentication mode" without
 * duplicating the hierarchy. [ReauthSceneStrategy] is what turns the entry into a modal sheet, or
 * leaves it bare when [reauthContent] owns presentation.
 *
 * @param surface The one condition for the reauthentication surface. [ReauthSceneStrategy] gates
 * the sheet on it and the entry renders what it resolves to, so an entry with no request outstanding is
 * never composed at all.
 * @param phoneFlowState What the reauthentication phone steps share across a step switch — see
 * [PhoneAuthFlowState]. Reauthentication's own instance, whose lifetime is the request's: nothing
 * the host flow typed reaches it, and nothing it holds outlives the request.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal fun EntryProviderScope<NavKey>.reauthDestinations(
    backStack: NavBackStack<NavKey>,
    authUI: FirebaseAuthUI,
    activity: android.app.Activity?,
    context: android.content.Context,
    configuration: AuthUIConfiguration,
    stringProvider: AuthUIStringProvider,
    surface: State<ReauthSurface?>,
    reauthFlowState: ReauthFlowState,
    phoneFlowState: PhoneAuthFlowState,
    emailContent: (@Composable (EmailAuthContentState) -> Unit)?,
    phoneContent: (@Composable (PhoneAuthContentState) -> Unit)?,
    mfaChallengeContent: (@Composable (MfaChallengeContentState) -> Unit)?,
    reauthContent: (@Composable (ReauthContentState) -> Unit)?,
    customMethodPickerLayout: (@Composable (List<AuthProvider>, (AuthProvider) -> Unit) -> Unit)?,
    onDismiss: () -> Unit,
    onLeaveStep: (AuthRoute.Reauth) -> Unit,
) {
    entry<AuthRoute.Reauth>(
        metadata = { key ->
            val presentation =
                if (reauthContent != null && key.step is AuthRoute.MethodPicker) {
                    ReauthPresentation.Bare
                } else {
                    ReauthPresentation.Sheet
                }
            authRouteMetadata(key.step) + reauthOverlayMetadata(key.requestId, presentation)
        },
    ) { key ->
        // Read through the snapshot, never through captured values — the entry rule at
        // FirebaseAuthScreen's entryProvider. No request outstanding is the same condition the sheet exists
        // on; a key naming an older request is the host's stack one composition behind the state,
        // and this entry writes to the id it names, so it renders nothing rather than the wrong one.
        val reauthSurface = surface.value ?: return@entry
        if (reauthSurface.request.requestId != key.requestId) return@entry
        val reauthState = reauthSurface.state
        val request = reauthSurface.request
        val reauthConfig = reauthSurface.configuration
        val reauthRequired = AuthState.Reauthentication.Required(request)
        val mfaResolver = (reauthState as? AuthState.Reauthentication.RequiresMfa)?.resolver
        val isLoading = reauthState is AuthState.Reauthentication.Authenticating ||
                reauthState is AuthState.Reauthentication.Succeeded
        val exception = (reauthState as? AuthState.Reauthentication.AttemptFailed)
            ?.exception
            ?.let { if (it is AuthException) it else AuthException.from(it, stringProvider) }
        val error = exception?.let { getRecoveryMessage(it, stringProvider) }

        // This request's own flow. Everything the credential exchange publishes lands on the
        // phase rather than on the public state channel, so an app collecting `authStateFlow()`
        // never sees a Loading or an Error belonging to a conversation that is not theirs.
        // The phase is both what this scope publishes into and what the screens under it render,
        // so the request's conversation never has to travel the public channel to be seen.
        val reauthStateHolder = remember(reauthFlowState) {
            derivedStateOf { reauthFlowState.phase ?: AuthState.Idle }
        }
        val reauthScope = remember(authUI, reauthConfig, reauthFlowState, reauthStateHolder) {
            AuthFlowScope(
                auth = authUI.auth,
                config = reauthConfig,
                credentialManagerProvider = authUI.testCredentialManagerProvider,
                loginManagerProvider = authUI.testLoginManagerProvider,
                state = reauthStateHolder,
                sink = reauthFlowState.sink(hostFallback = { authUI.updateAuthState(it) }),
            )
        }

        val onProviderSelected = reauthScope.rememberOnProviderSelected(
            context = context,
            activity = activity,
            onNavigate = { route -> backStack.navigateReauth(key, route.toKey()) },
        )

        CompositionLocalProvider(LocalAuthFlowScope provides reauthScope) {
        when (val step = key.step) {
            is AuthRoute.MethodPicker -> {
                if (reauthContent != null) {
                    reauthContent(
                        ReauthContentState(
                            user = reauthRequired.user,
                            reason = reauthRequired.reason,
                            providers = reauthConfig.providers,
                            onProviderSelected = { provider ->
                                if (provider !is AuthProvider.Email &&
                                    provider !is AuthProvider.Phone
                                ) {
                                    reauthFlowState.update(key.requestId) {
                                        it.attemptStarted()
                                    }
                                }
                                onProviderSelected(provider)
                            },
                            isLoading = isLoading,
                            error = error,
                            onDismiss = onDismiss,
                            exception = exception,
                        )
                    )
                } else if (customMethodPickerLayout != null) {
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

            is AuthRoute.Email.Step -> EmailAuthStep(
                step = step,
                // The wrapper, not the bare step: it is what is actually on the stack.
                entryKey = key,
                backStack = backStack,
                context = context,
                configuration = reauthConfig,
                authUI = authUI,
                content = emailContent,
                navigateToStep = { backStack.navigateReauth(key, it) },
                // onLeaveStep owns pop-vs-dismiss, and cancels the attempt with it.
                isStepBelow = { false },
                onCancel = { onLeaveStep(key) },
                prefillEmail = { reauthRequired.user.email },
                onNotificationConsumed = {
                    reauthFlowState.update(key.requestId) { it.returnedToProviderSelection() }
                },
            )

            is AuthRoute.Phone.Step -> PhoneAuthScreen(
                context = context,
                configuration = reauthConfig,
                authUI = authUI,
                content = phoneContent,
                onSuccess = {},
                onError = {},
                // onLeaveStep owns pop-vs-dismiss, and cancels the attempt with it.
                onCancel = { onLeaveStep(key) },
                step = step.phoneStep,
                onNavigateToStep = { target ->
                    backStack.navigateReauth(key, AuthRoute.Phone.stepFor(target))
                },
                // Number entry inside the surface: a pop while it is below, a move to it when not.
                onNavigateBack = {
                    backStack.navigateReauth(key, AuthRoute.Phone.EnterPhoneNumber)
                },
                flowState = phoneFlowState,
                onNotificationConsumed = {
                    reauthFlowState.update(key.requestId) { it.returnedToProviderSelection() }
                },
                onAttemptStarted = {
                    reauthFlowState.update(key.requestId) { it.attemptStarted() }
                },
            )

            // Only the state moves: the host pops the entry off whatever the state becomes, so
            // the challenge is on the stack exactly while the request needs one.
            is AuthRoute.MfaChallenge -> if (mfaResolver != null) {
                MfaChallengeScreen(
                    resolver = mfaResolver,
                    auth = authUI.auth,
                    content = mfaChallengeContent,
                    // The one credential exchange no provider owns, so the stamp is made here:
                    // no current user means nothing was re-proved, and the attempt is reported as
                    // a failure rather than moved on as an unstamped success.
                    onSuccess = {
                        val reauthenticated = authUI.auth.currentUser
                        if (reauthenticated == null) {
                            reauthFlowState.update(key.requestId) {
                                AuthState.Reauthentication.AttemptFailed(
                                    request,
                                    AuthException.UserNotFoundException(
                                        message = "No user is currently signed in for reauthentication"
                                    ),
                                )
                            }
                        } else {
                            reauthFlowState.update(key.requestId) {
                                AuthState.Reauthentication.Succeeded(
                                    request,
                                    AuthState.Success(
                                        result = null,
                                        user = reauthenticated,
                                        reauthenticatedUid = reauthenticated.uid,
                                    ),
                                )
                            }
                        }
                    },
                    onCancel = {
                        reauthFlowState.update(key.requestId) { it.attemptCancelled() }
                    },
                    onError = { e -> authUI.updateAuthState(AuthState.Error(e)) },
                )
            }

            else -> Unit
        }
        }
    }
}

/**
 * [this] narrowed to what [user] is actually linked to, in reauthentication mode, or null when
 * nothing configured is linked and there is therefore no reauthentication UI to show.
 */
internal fun AuthUIConfiguration.toReauthConfiguration(user: FirebaseUser): AuthUIConfiguration? =
    providers.filterToLinkedProviders(user)
        .takeIf { it.isNotEmpty() }
        ?.let {
            copy(
                providers = it,
                isAnonymousUpgradeEnabled = false,
                isCredentialLinkingEnabled = false,
                isNewEmailAccountsAllowed = false,
                isReauthenticationMode = true,
            )
        }
