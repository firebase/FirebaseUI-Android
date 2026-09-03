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

package com.firebase.ui.auth.ui.screens

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.BuildConfig
import com.firebase.ui.auth.FirebaseAuthActivity
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.R
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.DefaultAuthContentTransform
import com.firebase.ui.auth.configuration.DefaultAuthPredictivePopContentTransform
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.auth_provider.rememberAnonymousSignInHandler
import com.firebase.ui.auth.configuration.auth_provider.rememberGoogleSignInHandler
import com.firebase.ui.auth.configuration.auth_provider.rememberOAuthSignInHandler
import com.firebase.ui.auth.configuration.auth_provider.rememberSignInWithFacebookLauncher
import com.firebase.ui.auth.configuration.auth_provider.signInWithEmailLink
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.configuration.theme.LocalAuthUITheme
import com.firebase.ui.auth.ui.components.LocalTopLevelDialogController
import com.firebase.ui.auth.ui.components.getRecoveryMessage
import com.firebase.ui.auth.ui.components.rememberTopLevelDialogController
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentStep
import com.firebase.ui.auth.ui.exposeTestTagsAsResourceIds
import com.firebase.ui.auth.ui.method_picker.AuthMethodPicker
import com.firebase.ui.auth.ui.method_picker.MethodPickerTermsConfiguration
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebase.ui.auth.ui.screens.email.EmailAuthMode
import com.firebase.ui.auth.ui.screens.email.emailAuthDestinations
import com.firebase.ui.auth.ui.screens.email.isEmailLinkSignInOffered
import com.firebase.ui.auth.ui.screens.email.isEmailSignUpOffered
import com.firebase.ui.auth.ui.screens.email.navigateToEmailStep
import com.firebase.ui.auth.ui.screens.mfa.MfaChallengeScreen
import com.firebase.ui.auth.ui.screens.mfa.enterMfaEnrollment
import com.firebase.ui.auth.ui.screens.mfa.entersMfaEnrollment
import com.firebase.ui.auth.ui.screens.mfa.exitMfaEnrollment
import com.firebase.ui.auth.ui.screens.mfa.mfaEnrollmentDestinations
import com.firebase.ui.auth.ui.screens.mfa.rememberMfaEnrollmentFlowState
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebase.ui.auth.ui.screens.phone.exitPhoneAuth
import com.firebase.ui.auth.ui.screens.phone.phoneAuthDestinations
import com.firebase.ui.auth.ui.screens.phone.rememberPhoneAuthFlowState
import com.firebase.ui.auth.ui.screens.reauth.ReauthContentState
import com.firebase.ui.auth.ui.screens.reauth.ReauthSceneStrategy
import com.firebase.ui.auth.ui.screens.reauth.armedReauth
import com.firebase.ui.auth.ui.screens.reauth.rememberReauthFlowState
import com.firebase.ui.auth.ui.screens.reauth.clearReauth
import com.firebase.ui.auth.ui.screens.reauth.navigateReauth
import com.firebase.ui.auth.ui.screens.reauth.returnToReauthStart
import com.firebase.ui.auth.ui.screens.reauth.reauthDestinations
import com.firebase.ui.auth.ui.screens.reauth.toReauthSurface
import com.firebase.ui.auth.ui.screens.reauth.toReauthConfiguration
import com.firebase.ui.auth.util.EmailLinkPersistenceManager
import com.firebase.ui.auth.util.SignInPreferenceManager
import com.firebase.ui.auth.util.displayIdentifier
import com.firebase.ui.auth.util.getDisplayEmail
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.MultiFactorResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * High-level authentication screen that wires together provider selection, individual provider
 * flows, error handling, and multi-factor enrollment/challenge flows. Back navigation is driven by
 * the Jetpack Navigation stack so presses behave like native Android navigation.
 *
 * @param modifier Applied once to the root [Surface]; it does not reach dialogs/sheets, which are
 * separate semantics owners the library flags for test-tag exposure on its own.
 * @param authenticatedContent Optional slot that allows callers to render the authenticated
 * state themselves. When provided, it receives the current [AuthState] alongside an
 * [AuthSuccessUiContext] containing common callbacks (sign out, manage MFA, reload user).
 * @param customMethodPickerLayout Optional slot that fully replaces the method-picker screen.
 * When provided, it renders as the *entire* screen content — edge-to-edge, with no logo, no
 * Terms of Service/Privacy Policy footer, and no automatic system-inset handling. The caller is
 * responsible for its own insets (e.g. `Modifier.safeDrawingPadding()`) and for displaying any
 * required legal disclosures. [customMethodPickerTermsConfiguration] is ignored when this is set.
 * @param customMethodPickerTermsConfiguration Optional custom Terms of Service/Privacy Policy
 * footer for the *default* method-picker layout. Ignored when [customMethodPickerLayout] is
 * provided, since that slot takes over the whole screen.
 * @param reauthContent Optional slot that replaces the default reauthentication bottom sheet,
 * receiving a [ReauthContentState]. The library owns the credential exchange. An armed
 * reauthentication survives Activity recreation (rotation) but not process death; if it is lost
 * the flow surfaces an error rather than dropping the pending operation silently. An enrolled
 * second factor is challenged over the slot, honouring [mfaChallengeContent].
 *
 * @since 10.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseAuthScreen(
    configuration: AuthUIConfiguration,
    onSignInSuccess: (AuthResult) -> Unit,
    onSignInFailure: (AuthException) -> Unit,
    onSignInCancelled: () -> Unit,
    modifier: Modifier = Modifier,
    authUI: FirebaseAuthUI = FirebaseAuthUI.getInstance(),
    emailLink: String? = null,
    mfaConfiguration: MfaConfiguration = MfaConfiguration(),
    customMethodPickerLayout: (@Composable (List<AuthProvider>, (AuthProvider) -> Unit) -> Unit)? = null,
    customMethodPickerTermsConfiguration: MethodPickerTermsConfiguration? = null,
    emailContent: (@Composable (EmailAuthContentState) -> Unit)? = null,
    phoneContent: (@Composable (PhoneAuthContentState) -> Unit)? = null,
    mfaEnrollmentContent: (@Composable (MfaEnrollmentContentState) -> Unit)? = null,
    mfaChallengeContent: (@Composable (MfaChallengeContentState) -> Unit)? = null,
    reauthContent: (@Composable (ReauthContentState) -> Unit)? = null,
    authenticatedContent: (@Composable (state: AuthState, uiContext: AuthSuccessUiContext) -> Unit)? = null,
) {
    LaunchedEffect(authUI.auth) {
        authUI.auth.setFirebaseUIVersion(BuildConfig.VERSION_NAME)
    }

    val activity = LocalActivity.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val stringProvider = remember(context) { DefaultAuthUIStringProvider(context) }

    val observedAuthState by remember(authUI) { authUI.authStateFlow() }
        .collectAsState(initial = null as AuthState?)
    val rawAuthState = observedAuthState ?: AuthState.Idle
    // Composition-scoped, so its existence *is* the answer to "is there a screen able to drive an
    // armed request to completion?" — no counter on the singleton, and a phase that cannot outlive
    // the Activity and re-arm an unrelated sign-in.
    val reauthFlowState = rememberReauthFlowState()
    val reauthState = reauthFlowState.phase
    /**
     * What the host may act on. While a request is armed, an ordinary state published by provider
     * code belongs to the credential exchange and the phase is what reports it — but `fold` runs
     * in an effect, so the raw state is on the flow for a frame first. Without this the host's own
     * dialogs act on it in between, putting a sign-in error dialog, retry action and all, over the
     * reauthentication sheet. The effects below read `observedAuthState` directly, so the states
     * `fold` declines still reach them.
     */
    val authState = reauthState?.takeIf { rawAuthState !is AuthState.Reauthentication }
        ?: rawAuthState
    val dialogController = rememberTopLevelDialogController(stringProvider) { authState }
    val lastSuccessfulUserId = remember { mutableStateOf<String?>(null) }
    val pendingLinkingCredential = remember { mutableStateOf<AuthCredential?>(null) }
    val pendingResolver = remember { mutableStateOf<MultiFactorResolver?>(null) }
    val mfaEnrollmentFlowState = rememberMfaEnrollmentFlowState()
    val phoneAuthFlowState = rememberPhoneAuthFlowState(configuration)
    val reauthRequest = reauthState?.request
    val reauthConfig = reauthRequest?.let { configuration.toReauthConfiguration(it.user) }
    // Keyed to the request, never the host flow's: another operation, maybe another user.
    val reauthPhoneFlowState = key(reauthRequest?.requestId) {
        rememberPhoneAuthFlowState(reauthConfig ?: configuration)
    }
    /**
     * The reauthentication surface, or null when there is none. One signal: [ReauthSceneStrategy]
     * decides whether the sheet exists on it and the entry renders what it resolves to.
     *
     * Held as `State` because the entry reads it, and a `NavEntry`'s content lambda is built once
     * per key — anything passed by value there never updates.
     */
    val reauthSurface = remember(reauthState, configuration) {
        reauthState.toReauthSurface(configuration)
    }
    val reauthSurfaceHolder = rememberUpdatedState(reauthSurface)
    val reauthException = (reauthState as? AuthState.Reauthentication.AttemptFailed)
        ?.exception
        ?.let { throwable ->
            when (throwable) {
                is AuthException -> throwable
                else -> AuthException.from(throwable, stringProvider)
            }
        }
    val emailLinkFromDifferentDevice = remember { mutableStateOf<String?>(null) }
    val typedEmail = rememberSaveable { mutableStateOf<String?>(null) }
    val reauthPrefillEmail = remember(authUI, configuration.isReauthenticationMode) {
        if (configuration.isReauthenticationMode) authUI.auth.currentUser?.email else null
    }
    val lastSignInPreference =
        remember { mutableStateOf<SignInPreferenceManager.SignInPreference?>(null) }
    val previousAuthState = remember { mutableStateOf<AuthState?>(null) }
    val startRoute = remember(configuration.providers, configuration.isProviderChoiceAlwaysShown) {
        getStartRoute(configuration)
    }
    val skipsMethodPicker = startRoute != AuthRoute.MethodPicker
    val backStack = rememberNavBackStack(startRoute.toKey())
    // The stack is the arming marker: a Reauth entry persists with it, across recreation and death.
    val armedReauth = backStack.armedReauth()
    val clearReauthPresentation: () -> Unit = remember(backStack) { { backStack.clearReauth() } }
    /**
     * Ends the armed request: clears its presentation, clears the phase, publishes [terminal], and
     * only then resolves the caller waiting on it.
     *
     * One helper because every terminal site does the same four things in the same order, and the
     * order carries two rules. The phase goes before [terminal] is published, or `fold` folds the
     * terminal state straight back into the request it is ending. The caller is resolved last, or
     * a fast-resuming retry's real outcome is overwritten by this stale one.
     */
    val finishReauth: (AuthState, Boolean) -> Unit =
        remember(authUI, clearReauthPresentation, reauthFlowState) {
            { terminal, retryOperation ->
                clearReauthPresentation()
                reauthFlowState.finish(retryOperation)
                authUI.updateAuthState(terminal)
            }
        }
    val currentOnSignInCancelled = rememberUpdatedState(onSignInCancelled)
    val onReauthDismiss: () -> Unit = remember(finishReauth) {
        {
            // The user backed out, so the pending operation is not retried.
            finishReauth(AuthState.Idle, false)
            currentOnSignInCancelled.value()
        }
    }
    /**
     * Leaving one reauthentication step. The stack decides which of the two it is: another
     * reauthentication entry underneath means step back and cancel the attempt; nothing underneath
     * means the surface itself is being left.
     */
    val onLeaveReauthStep: (AuthRoute.Reauth) -> Unit =
        remember(reauthFlowState, backStack, onReauthDismiss) {
        { marker ->
            val below = backStack.getOrNull(backStack.lastIndex - 1)
            if (below is AuthRoute.Reauth) {
                backStack.popOrNull()
                reauthFlowState.update(marker.requestId) { it.attemptCancelled() }
            } else {
                onReauthDismiss()
            }
        }
    }
    // The slot *is* the provider chooser, even for one provider, so it always starts at the picker
    // step. The default sheet skips straight into a lone provider's flow, as it always did.
    val reauthStartStepFor: (AuthUIConfiguration?) -> AuthRoute.Destination =
        remember(reauthContent) {
            { config ->
                when {
                    // The slot *is* the provider chooser, even for one provider, so it always
                    // starts at the picker step. The default sheet skips straight into a lone
                    // provider's flow, as it always did.
                    reauthContent != null -> AuthRoute.MethodPicker
                    config != null -> getStartRoute(config).toKey()
                    else -> AuthRoute.MethodPicker
                }
            }
        }
    val stepTransitionSpec = configuration.transitions?.transitionSpec
        ?: DefaultAuthContentTransform
    val stepPopTransitionSpec = configuration.transitions?.popTransitionSpec
        ?: DefaultAuthContentTransform
    val reauthSceneStrategy =
        remember(onReauthDismiss, stepTransitionSpec, stepPopTransitionSpec) {
            ReauthSceneStrategy(
                surface = reauthSurfaceHolder,
                onDismissRequest = onReauthDismiss,
                transitionSpec = stepTransitionSpec,
                popTransitionSpec = stepPopTransitionSpec,
            )
        }

    LaunchedEffect(authState) {
        lastSignInPreference.value = SignInPreferenceManager.getLastSignIn(context)
    }

    val emailProvider = configuration.providers.filterIsInstance<AuthProvider.Email>().firstOrNull()
    val logoAsset = configuration.logo
    val onOuterProviderSelected = authUI.rememberOnProviderSelected(
        context = context,
        activity = activity,
        config = configuration,
        onNavigate = { route ->
            if (route == AuthRoute.Email) {
                backStack.navigateToEmailStep(AuthRoute.Email.SignIn(typedEmail.value))
            } else {
                // pushUnique invariant: the picker is the only entry, so nothing can be buried.
                backStack.pushUnique(route)
            }
        },
        onUnknownProvider = { provider ->
            onSignInFailure(
                AuthException.UnknownException(
                    message = "Provider ${provider.providerId} is not supported in FirebaseAuthScreen",
                    cause = IllegalArgumentException(
                        "Provider ${provider.providerId} is not supported in FirebaseAuthScreen"
                    )
                )
            )
        },
        onSignInFailure = onSignInFailure,
    )
    val currentOuterProviderSelected = rememberUpdatedState(onOuterProviderSelected)
    val currentReauthState = rememberUpdatedState(reauthState)
    val onProviderSelected: (AuthProvider) -> Unit = remember {
        { provider ->
            if (currentReauthState.value == null) {
                currentOuterProviderSelected.value(provider)
            }
        }
    }
    val continueWithProvider: (String) -> Unit = { providerId ->
        configuration.providers.find { it.providerId == providerId }?.let { onProviderSelected(it) }
    }

    CompositionLocalProvider(
        LocalAuthUIStringProvider provides configuration.stringProvider,
        LocalTopLevelDialogController provides dialogController,
        LocalAuthUITheme provides (configuration.theme ?: LocalAuthUITheme.current)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .exposeTestTagsAsResourceIds()
        ) {
            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(reauthSceneStrategy),
                onBack = {
                    val top = backStack.lastOrNull()
                    if (top is AuthRoute.Reauth) onLeaveReauthStep(top) else backStack.popOrNull()
                },
                transitionSpec = stepTransitionSpec,
                popTransitionSpec = stepPopTransitionSpec,
                predictivePopTransitionSpec =
                    configuration.transitions?.predictivePopTransitionSpec
                        ?: DefaultAuthPredictivePopContentTransform,
                // Every entry's content lambda below is built once per key: a value passed into
                // one is captured at that first composition and never updates again. Anything
                // that changes while an entry is on screen has to arrive as a `State` or a
                // getter lambda, never by value.
                entryProvider = entryProvider {
                entry<AuthRoute.MethodPicker>(
                    metadata = authRouteMetadata(AuthRoute.MethodPicker)
                ) {
                    if (customMethodPickerLayout != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            customMethodPickerLayout(configuration.providers, onProviderSelected)
                        }
                    } else {
                        Scaffold(modifier = Modifier.exposeTestTagsAsResourceIds()) { innerPadding ->
                            AuthMethodPicker(
                                modifier = Modifier
                                    .padding(innerPadding),
                                providers = configuration.providers,
                                logo = logoAsset,
                                termsOfServiceUrl = configuration.tosUrl,
                                privacyPolicyUrl = configuration.privacyPolicyUrl,
                                lastSignInPreference = lastSignInPreference.value,
                                termsConfiguration = customMethodPickerTermsConfiguration,
                                onProviderSelected = { provider ->
                                    typedEmail.value = null
                                    onProviderSelected(provider)
                                },
                                onContinueAsSelected = { provider, identifier ->
                                    typedEmail.value =
                                        if (provider is AuthProvider.Email) identifier else null
                                    onProviderSelected(provider)
                                },
                            )
                        }
                    }
                }

                emailAuthDestinations(
                    backStack = backStack,
                    context = context,
                    configuration = configuration,
                    authUI = authUI,
                    content = emailContent,
                    prefillEmail = { reauthPrefillEmail },
                    credentialForLinking = { pendingLinkingCredential.value },
                    emailLinkFromDifferentDevice = { emailLinkFromDifferentDevice.value },
                    onEmailTyped = { typedEmail.value = it },
                    onSuccess = { pendingLinkingCredential.value = null },
                    onError = { exception -> onSignInFailure(exception) },
                    onCancel = {
                        pendingLinkingCredential.value = null
                        if (!skipsMethodPicker && !backStack.popOrNull()) {
                            backStack.resetBackStackTo(AuthRoute.MethodPicker)
                        }
                    },
                )

                phoneAuthDestinations(
                    backStack = backStack,
                    context = context,
                    configuration = configuration,
                    authUI = authUI,
                    flowState = phoneAuthFlowState,
                    content = phoneContent,
                    onError = { exception -> onSignInFailure(exception) },
                    onCancel = {
                        if (!skipsMethodPicker && !backStack.exitPhoneAuth()) {
                            backStack.resetBackStackTo(AuthRoute.MethodPicker)
                        }
                    },
                )

                entry<AuthRoute.Success>(metadata = authRouteMetadata(AuthRoute.Success)) {
                    val uiContext = remember(authState, stringProvider) {
                        AuthSuccessUiContext(
                            authUI = authUI,
                            stringProvider = stringProvider,
                            configuration = configuration,
                            onSignOut = {
                                coroutineScope.launch {
                                    try {
                                        authUI.signOut(context)
                                    } catch (e: Exception) {
                                        onSignInFailure(AuthException.from(e, stringProvider))
                                    } finally {
                                        pendingLinkingCredential.value = null
                                        pendingResolver.value = null
                                    }
                                }
                            },
                            onManageMfa = {
                                if (reauthState == null) {
                                    if (configuration.isMfaEnabled) {
                                        // pushUnique invariant: Success is one entry — nothing to bury.
                                        backStack.enterMfaEnrollment(
                                            route = AuthRoute.MfaEnrollment,
                                            configuration = mfaConfiguration,
                                            flowState = mfaEnrollmentFlowState,
                                        )
                                    } else {
                                        val exception = AuthException.AuthCancelledException(
                                            message = "Multi-factor authentication is disabled in the configuration. " +
                                                    "Enable MFA in AuthUIConfiguration to use this feature."
                                        )
                                        authUI.updateAuthState(AuthState.Error(exception))
                                    }
                                }
                            },
                            onReloadUser = {
                                coroutineScope.launch {
                                    try {
                                        authUI.reloadUser()
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        // Signing out mid-reload fails the token refresh. That is
                                        // the sign-out landing, not a refresh the user can retry.
                                        if (authUI.getCurrentUser() == null) {
                                            Log.d(
                                                "FirebaseAuthScreen",
                                                "Reload abandoned, user signed out",
                                                e
                                            )
                                        } else {
                                            Log.e("FirebaseAuthScreen", "Failed to refresh user", e)
                                        }
                                    }
                                }
                            },
                            onNavigate = { route ->
                                if (reauthState == null) {
                                    // Naming a step of the enrolment flow enters it just as
                                    // naming the flow does, so both clear the previous attempt.
                                    if (route.entersMfaEnrollment) {
                                        backStack.enterMfaEnrollment(
                                            route = route,
                                            configuration = mfaConfiguration,
                                            flowState = mfaEnrollmentFlowState,
                                        )
                                    } else {
                                        // pushUnique invariant: `route` is consumer-supplied; safe only as Success is one entry.
                                        backStack.pushUnique(route)
                                    }
                                }
                            }
                        )
                    }

                    if (authenticatedContent != null) {
                        authenticatedContent(authState, uiContext)
                    } else {
                        SuccessDestination(
                            authState = authState,
                            stringProvider = stringProvider,
                            configuration = configuration,
                            uiContext = uiContext
                        )
                    }
                }

                mfaEnrollmentDestinations(
                    backStack = backStack,
                    configuration = mfaConfiguration,
                    authConfiguration = configuration,
                    authUI = authUI,
                    flowState = mfaEnrollmentFlowState,
                    content = mfaEnrollmentContent,
                    onComplete = { backStack.exitMfaEnrollment() },
                    onSkip = { backStack.exitMfaEnrollment() },
                    onError = { exception ->
                        onSignInFailure(AuthException.from(exception, stringProvider))
                    }
                )

                reauthDestinations(
                    backStack = backStack,
                    authUI = authUI,
                    activity = activity,
                    context = context,
                    configuration = configuration,
                    stringProvider = stringProvider,
                    reauthFlowState = reauthFlowState,
                    surface = reauthSurfaceHolder,
                    phoneFlowState = reauthPhoneFlowState,
                    emailContent = emailContent,
                    phoneContent = phoneContent,
                    mfaChallengeContent = mfaChallengeContent,
                    reauthContent = reauthContent,
                    customMethodPickerLayout = customMethodPickerLayout,
                    onDismiss = onReauthDismiss,
                    onLeaveStep = onLeaveReauthStep,
                )

                entry<AuthRoute.MfaChallenge>(
                    metadata = authRouteMetadata(AuthRoute.MfaChallenge)
                ) {
                    val resolver = remember { pendingResolver.value }
                    if (resolver != null) {
                        MfaChallengeScreen(
                            resolver = resolver,
                            auth = authUI.auth,
                            content = mfaChallengeContent,
                            onSuccess = { result ->
                                pendingResolver.value = null
                                authUI.updateAuthStateWithResult(result)
                            },
                            // Load-bearing pop: Cancelled below then sees the start step, so it skips a reset that blanks the address.
                            onCancel = {
                                pendingResolver.value = null
                                authUI.updateAuthState(AuthState.Cancelled)
                                backStack.popOrNull()
                            },
                            onError = { exception ->
                                onSignInFailure(AuthException.from(exception, stringProvider))
                            }
                        )
                    } else {
                        LaunchedEffect(Unit) { backStack.popOrNull() }
                    }
                }
                },
            )

            LaunchedEffect(emailLink) {
                if (emailLink != null && emailProvider != null && reauthState == null) {
                    try {
                        val savedEmail =
                            EmailLinkPersistenceManager.default.retrieveSessionRecord(context)?.email

                        if (savedEmail != null) {
                            authUI.signInWithEmailLink(
                                context = context,
                                config = configuration,
                                provider = emailProvider,
                                email = savedEmail,
                                emailLink = emailLink
                            )
                        } else {
                            authUI.signInWithEmailLink(
                                context = context,
                                config = configuration,
                                provider = emailProvider,
                                email = "",
                                emailLink = emailLink
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseAuthScreen", "Failed to complete email link sign-in", e)
                    }
                }
            }

            LaunchedEffect(observedAuthState) {
                val state = observedAuthState ?: return@LaunchedEffect
                val previous = previousAuthState.value
                previousAuthState.value = state
                // Guards below use `isAt` (runtime class), not `==`: keys carry arguments, so `==` blanks a live form.
                val currentKey = backStack.lastOrNull()
                val savedPresentation = armedReauth

                // A marker that outlived its phase: the Activity was recreated with the request
                // still armed. Nothing here can be driven, so report it and clear up.
                if (savedPresentation != null &&
                    reauthFlowState.phase == null &&
                    state !is AuthState.Reauthentication &&
                    state !is AuthState.Aborted
                ) {
                    clearReauthPresentation()
                    authUI.updateAuthState(
                        AuthState.Error(
                            AuthException.UnknownException(
                                context.getString(R.string.fui_error_reauth_interrupted)
                            )
                        )
                    )
                    return@LaunchedEffect
                }

                // A latched reauthentication state with no phase: this screen was recreated while
                // the request was armed. The phase is composition-scoped and gone, but its value
                // is still on the flow, so the exchange is re-armed from it rather than abandoned.
                if (state is AuthState.Reauthentication && reauthFlowState.phase == null) {
                    val request = state.request
                    if (request == null || !request.isResumable) {
                        clearReauthPresentation()
                        authUI.updateAuthState(
                            AuthState.Error(
                                AuthException.UnknownException(
                                    context.getString(R.string.fui_error_reauth_interrupted)
                                )
                            )
                        )
                        return@LaunchedEffect
                    }
                    // Two phases must not come back: `Authenticating`'s network call died with the
                    // Activity, and re-entering `Succeeded` would resolve the caller a second time
                    // for an operation that may already have committed. Both restart at provider
                    // selection. Everything else is the user's own position in the exchange, and a
                    // surfaced failure is the only report they got, so it is restored as it was.
                    when (state) {
                        is AuthState.Reauthentication.Authenticating,
                        is AuthState.Reauthentication.Succeeded,
                            -> authUI.updateAuthState(
                            AuthState.Reauthentication.Required(request)
                        )

                        is AuthState.Reauthentication.Required -> reauthFlowState.arm(state)

                        else -> reauthFlowState.moveTo(state)
                    }
                }

                // Ordinary states published by provider code while a request is armed belong to
                // the credential exchange, not to the host flow. The holder folds them into its
                // phase and publishes that, which is what the setter used to do on the singleton's
                // behalf; the branches below then only ever see states that are the host's.
                reauthFlowState.fold(state)?.let { folded ->
                    authUI.updateAuthState(folded)
                    return@LaunchedEffect
                }

                // The challenge entry is on the stack exactly while the state is RequiresMfa: it
                // has no resolver to render otherwise, and this is the only place that pops it, so
                // no attempt path can strand the user on a dead challenge.
                if (state !is AuthState.Reauthentication.RequiresMfa &&
                    backStack.armedReauth()?.step is AuthRoute.MfaChallenge
                ) {
                    backStack.returnToReauthStart()
                }

                when (state) {
                    is AuthState.Success -> {
                        pendingResolver.value = null
                        pendingLinkingCredential.value = null

                        state.result?.let { result ->
                            if (state.user.uid != lastSuccessfulUserId.value) {
                                onSignInSuccess(result)
                                lastSuccessfulUserId.value = state.user.uid

                                coroutineScope.launch {
                                    lastSignInPreference.value =
                                        SignInPreferenceManager.getLastSignIn(context)
                                }
                            }
                        }

                        if (currentKey != AuthRoute.Success) {
                            backStack.resetBackStackTo(AuthRoute.Success)
                        }
                    }

                    is AuthState.Reauthentication.Required -> {
                        val armingConfig = configuration.toReauthConfiguration(state.user)
                        if (armingConfig == null) {
                            finishReauth(
                                AuthState.Error(
                                    AuthException.UnknownException(
                                        context.getString(R.string.fui_error_reauth_no_linked_providers)
                                    )
                                ),
                                false,
                            )
                            return@LaunchedEffect
                        }
                        // A request whose caller died with its scope cannot be completed however
                        // well the exchange goes, so it is reported rather than presented. This is
                        // what tells a rotation that kept its caller from one that lost it.
                        if (!state.request.isResumable) {
                            finishReauth(
                                AuthState.Error(
                                    AuthException.UnknownException(
                                        context.getString(R.string.fui_error_reauth_interrupted)
                                    )
                                ),
                                false,
                            )
                            return@LaunchedEffect
                        }
                        reauthFlowState.arm(state)
                        if (armedReauth?.requestId != state.requestId) {
                            backStack.clearReauth()
                            backStack.add(
                                AuthRoute.Reauth(
                                    requestId = state.requestId,
                                    userUid = state.userUid,
                                    // From the arming state, not the composition value: this
                                    // effect is what writes the phase, so anything derived from
                                    // it in composition is still a frame behind here.
                                    step = reauthStartStepFor(armingConfig),
                                )
                            )
                        }
                    }

                    is AuthState.Reauthentication -> {
                        val marker = armedReauth?.takeIf { it.requestId == state.requestId }
                            ?: AuthRoute.Reauth(
                                requestId = state.requestId,
                                userUid = state.userUid,
                                step = state.request
                                    ?.let { configuration.toReauthConfiguration(it.user) }
                                    ?.let { reauthStartStepFor(it) }
                                    ?: AuthRoute.MethodPicker,
                            ).also {
                                backStack.clearReauth()
                                backStack.add(it)
                            }
                        // A real entry, so the challenge is pushed rather than derived; the pop
                        // for every other state is handled above.
                        if (state is AuthState.Reauthentication.RequiresMfa &&
                            marker.step !is AuthRoute.MfaChallenge
                        ) {
                            backStack.navigateReauth(marker, AuthRoute.MfaChallenge)
                        }
                    }

                    is AuthState.RequiresEmailVerification,
                    is AuthState.RequiresProfileCompletion,
                        -> {
                        pendingResolver.value = null
                        pendingLinkingCredential.value = null
                        if (currentKey != AuthRoute.Success) {
                            backStack.resetBackStackTo(AuthRoute.Success)
                        }
                    }

                    is AuthState.RequiresMfa -> {
                        pendingResolver.value = state.resolver
                        // pushUnique invariant: nothing is pushed on top of the challenge, so this covers the buried case.
                        if (currentKey != AuthRoute.MfaChallenge) {
                            backStack.pushUnique(AuthRoute.MfaChallenge)
                        }
                    }

                    is AuthState.Cancelled -> {
                        clearReauthPresentation()
                        pendingResolver.value = null
                        pendingLinkingCredential.value = null
                        lastSuccessfulUserId.value = null
                        typedEmail.value = null
                        if (!currentKey.isAt(startRoute)) {
                            backStack.resetBackStackTo(startRoute)
                        }
                        onSignInCancelled()
                        authUI.updateAuthState(AuthState.Idle)
                    }

                    is AuthState.Aborted -> {
                        // Outside the host guard on purpose. `fold` declines Aborted, so nothing
                        // else clears the phase or resolves the caller — and under the activity
                        // host FirebaseAuthActivity owns the rest of the teardown, so a clear
                        // placed inside the guard would leave that host holding an armed request
                        // and a caller suspended forever. An activity-scoped caller has its own
                        // cancellation to fall back on, an unscoped one has nothing, and this
                        // cannot tell them apart, so it resolves unconditionally.
                        clearReauthPresentation()
                        reauthFlowState.finish(false)
                        if (activity !is FirebaseAuthActivity) {
                            pendingResolver.value = null
                            pendingLinkingCredential.value = null
                            lastSuccessfulUserId.value = null
                            typedEmail.value = null
                            authUI.updateAuthState(AuthState.Idle)
                        }
                    }

                    is AuthState.Idle -> {
                        if (previous != null && !previous.isNotification) {
                            clearReauthPresentation()
                            pendingResolver.value = null
                            pendingLinkingCredential.value = null
                            lastSuccessfulUserId.value = null
                            typedEmail.value = null
                            // Number entry is a real position in the flow, so a retraction
                            // reached there stays put rather than resetting out of it.
                            if (!currentKey.isAt(startRoute) &&
                                currentKey !is AuthRoute.Phone.EnterPhoneNumber
                            ) {
                                backStack.resetBackStackTo(startRoute)
                            }
                        }
                    }

                    else -> Unit
                }
            }

            /**
             * The phase's own effect. The effect above is keyed on the flow, so it never sees a
             * transition the destinations make straight on the holder — an MFA proof, a cancelled
             * attempt, a consumed notification. Keying on the phase catches all of them.
             *
             * Mirroring the phase onto the flow keeps one story for the sub-screens and for app
             * code: provider screens read their loading and error state from there, and a phase
             * that only ever existed in this holder would show them neither.
             */
            LaunchedEffect(reauthFlowState.phase) {
                val phase = reauthFlowState.phase ?: return@LaunchedEffect
                if (observedAuthState != phase) authUI.updateAuthState(phase)

                if (phase is AuthState.Reauthentication.Succeeded) {
                    val request = phase.request
                    val success = phase.success
                    if (success.reauthenticatedUid != phase.userUid ||
                        success.user.uid != phase.userUid
                    ) {
                        // Proof for the wrong user is a failed attempt, not a dead request: the
                        // surface stays up reporting it so the user can try the right account.
                        reauthFlowState.update(phase.requestId) {
                            AuthState.Reauthentication.AttemptFailed(
                                request,
                                AuthException.UnknownException(
                                    context.getString(R.string.fui_error_reauth_incomplete)
                                ),
                            )
                        }
                        return@LaunchedEffect
                    }
                    // The retry runs on the caller's own coroutine, so this publishes the handover
                    // rather than an outcome: a Success here would claim the pending operation had
                    // already succeeded. A standalone flow has no operation behind it, so for that
                    // one reauthenticating *is* the outcome.
                    val terminal = if (request.hasPendingOperation) {
                        AuthState.Loading(
                            context.getString(R.string.fui_loading_reauth_retrying)
                        )
                    } else {
                        AuthState.Success(result = null, user = request.user)
                    }
                    finishReauth(terminal, true)
                }
            }

            // The slot owns the error and loading presentation while it is what is on screen.
            val reauthSlotActive = reauthContent != null &&
                    reauthSurface != null &&
                    armedReauth?.step is AuthRoute.MethodPicker

            val reauthAttemptFailure =
                reauthState as? AuthState.Reauthentication.AttemptFailed
            if (reauthAttemptFailure != null && !reauthSlotActive) {
                LaunchedEffect(reauthAttemptFailure) {
                    val exception = reauthException ?: return@LaunchedEffect
                    dialogController.showErrorDialog(
                        exception = exception,
                        errorState = AuthState.Error(reauthAttemptFailure.exception),
                        onRetry = null,
                        onRecover = null,
                    )
                }
            }

            fun navigateToEmailStep(target: AuthRoute.Email.Step, address: String? = null) {
                if (backStack.lastOrNull().isAt(target)) return
                val carriedEmail = address?.takeIf { it.isNotEmpty() } ?: typedEmail.value
                backStack.navigateToEmailStep(target, carriedEmail)
            }

            val emailLinkRecoveryStep: AuthRoute.Email.Step =
                if (configuration.isEmailLinkSignInOffered()) {
                    AuthRoute.Email.EmailLinkSignIn()
                } else {
                    AuthRoute.Email.SignIn()
                }

            val errorState = authState as? AuthState.Error
            if (errorState != null) {
                LaunchedEffect(errorState) {
                    val exception = when (val throwable = errorState.exception) {
                        is AuthException -> throwable
                        else -> AuthException.from(throwable, stringProvider)
                    }

                    dialogController.showErrorDialog(
                        exception = exception,
                        errorState = errorState,
                        onRetry = null,
                        onRecover = if (configuration.isReauthenticationMode) {
                            null
                        } else when (exception) {
                            is AuthException.UserNotFoundException -> {
                                if (configuration.isEmailSignUpOffered()) {
                                    { navigateToEmailStep(AuthRoute.Email.SignUp()) }
                                } else {
                                    null
                                }
                            }

                            is AuthException.EmailAlreadyInUseException -> {
                                { navigateToEmailStep(AuthRoute.Email.SignIn(), exception.email) }
                            }

                            is AuthException.AccountLinkingRequiredException -> {
                                {
                                    pendingLinkingCredential.value = exception.credential
                                    navigateToEmailStep(AuthRoute.Email.SignIn(), exception.email)
                                }
                            }

                            is AuthException.EmailLinkPromptForEmailException -> {
                                {
                                    emailLinkFromDifferentDevice.value = exception.emailLink
                                    navigateToEmailStep(emailLinkRecoveryStep)
                                }
                            }

                            is AuthException.EmailLinkCrossDeviceLinkingException -> {
                                {
                                    emailLinkFromDifferentDevice.value = exception.emailLink
                                    navigateToEmailStep(emailLinkRecoveryStep)
                                }
                            }

                            is AuthException.DifferentSignInMethodRequiredException -> {
                                {
                                    val providerId = exception.suggestedSignInMethod
                                    if (providerId == EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD) {
                                        navigateToEmailStep(
                                            emailLinkRecoveryStep,
                                            exception.email,
                                        )
                                    } else {
                                        continueWithProvider(providerId)
                                    }
                                }
                            }

                            else -> null
                        },
                        onDismiss = {
                        }
                    )
                    authUI.updateAuthState(AuthState.Idle)
                }
            }

            dialogController.CurrentDialog()

            val loadingMessage = when (val state = authState) {
                is AuthState.Loading -> state.message
                is AuthState.Reauthentication.Authenticating -> state.message
                else -> null
            }
            val isLoading = authState is AuthState.Loading ||
                    authState is AuthState.Reauthentication.Authenticating
            if (isLoading && !reauthSlotActive) {
                LoadingDialog(loadingMessage ?: stringProvider.progressDialogLoading)
            }

        }
    }
}

/**
 * Where the flow starts, from the configuration alone: a single email or phone provider opens
 * that flow directly, anything else opens the method picker.
 *
 * Returns an [AuthRoute] rather than an [AuthRoute.Destination], because a single-provider
 * configuration names a *flow*; [toKey] is what resolves it to the step to actually push.
 */
internal fun getStartRoute(configuration: AuthUIConfiguration): AuthRoute {
    if (configuration.isProviderChoiceAlwaysShown || configuration.providers.size != 1) {
        return AuthRoute.MethodPicker
    }

    return when (configuration.providers.single()) {
        is AuthProvider.Email -> AuthRoute.Email
        is AuthProvider.Phone -> AuthRoute.Phone
        else -> AuthRoute.MethodPicker
    }
}

data class AuthSuccessUiContext(
    val authUI: FirebaseAuthUI,
    val stringProvider: AuthUIStringProvider,
    val configuration: AuthUIConfiguration,
    val onSignOut: () -> Unit,
    val onManageMfa: () -> Unit,
    val onReloadUser: () -> Unit,
    val onNavigate: (AuthRoute) -> Unit,
)

@Composable
private fun SuccessDestination(
    authState: AuthState,
    stringProvider: AuthUIStringProvider,
    configuration: AuthUIConfiguration,
    uiContext: AuthSuccessUiContext,
) {
    when (authState) {
        is AuthState.Success -> {
            AuthSuccessContent(
                authUI = uiContext.authUI,
                stringProvider = stringProvider,
                configuration = configuration,
                onSignOut = uiContext.onSignOut,
                onManageMfa = uiContext.onManageMfa
            )
        }

        is AuthState.RequiresEmailVerification -> {
            EmailVerificationContent(
                authUI = uiContext.authUI,
                stringProvider = stringProvider,
                onCheckStatus = uiContext.onReloadUser,
                onSignOut = uiContext.onSignOut
            )
        }

        is AuthState.RequiresProfileCompletion -> {
            ProfileCompletionContent(
                missingFields = authState.missingFields,
                stringProvider = stringProvider
            )
        }

        else -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthSuccessContent(
    authUI: FirebaseAuthUI,
    stringProvider: AuthUIStringProvider,
    configuration: AuthUIConfiguration,
    onSignOut: () -> Unit,
    onManageMfa: () -> Unit,
) {
    val user = authUI.getCurrentUser()
    val userIdentifier = user.displayIdentifier()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (userIdentifier.isNotBlank()) {
            Text(
                text = stringProvider.signedInAs(userIdentifier),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        if (user != null && authUI.auth.app.options.projectId != null) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above
                ),
                tooltip = {
                    PlainTooltip(modifier = Modifier.exposeTestTagsAsResourceIds()) {
                        Text(stringProvider.mfaDisabledTooltip)
                    }
                },
                state = rememberTooltipState(
                    initialIsVisible = false
                )
            ) {
                Button(
                    onClick = onManageMfa,
                    enabled = configuration.isMfaEnabled
                ) {
                    Text(stringProvider.manageMfaAction)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = onSignOut) {
            Text(stringProvider.signOutAction)
        }
    }
}

@Composable
private fun EmailVerificationContent(
    authUI: FirebaseAuthUI,
    stringProvider: AuthUIStringProvider,
    onCheckStatus: () -> Unit,
    onSignOut: () -> Unit,
) {
    val user = authUI.getCurrentUser()
    val emailLabel = user.getDisplayEmail(stringProvider.emailProvider)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringProvider.verifyEmailInstruction(emailLabel),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { user?.sendEmailVerification() }) {
            Text(stringProvider.resendVerificationEmailAction)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onCheckStatus) {
            Text(stringProvider.verifiedEmailAction)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSignOut) {
            Text(stringProvider.signOutAction)
        }
    }
}

@Composable
private fun ProfileCompletionContent(
    missingFields: List<String>,
    stringProvider: AuthUIStringProvider,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringProvider.profileCompletionMessage,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (missingFields.isNotEmpty()) {
            Text(
                text = stringProvider.profileMissingFieldsMessage(missingFields.joinToString()),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LoadingDialog(message: String) {
    AlertDialog(
        modifier = Modifier.exposeTestTagsAsResourceIds(),
        onDismissRequest = {},
        confirmButton = {},
        containerColor = Color.Transparent,
        text = {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
        }
    )
}

@Composable
internal fun FirebaseAuthUI.rememberOnProviderSelected(
    context: android.content.Context,
    activity: android.app.Activity?,
    config: AuthUIConfiguration,
    onNavigate: (AuthRoute) -> Unit,
    onUnknownProvider: ((AuthProvider) -> Unit)? = null,
    onSignInFailure: (AuthException) -> Unit = {},
): (AuthProvider) -> Unit {
    val anonymousProvider = config.providers.filterIsInstance<AuthProvider.Anonymous>().firstOrNull()
    val googleProvider = config.providers.filterIsInstance<AuthProvider.Google>().firstOrNull()
    val facebookProvider = config.providers.filterIsInstance<AuthProvider.Facebook>().firstOrNull()
    val appleProvider = config.providers.filterIsInstance<AuthProvider.Apple>().firstOrNull()
    val githubProvider = config.providers.filterIsInstance<AuthProvider.Github>().firstOrNull()
    val microsoftProvider = config.providers.filterIsInstance<AuthProvider.Microsoft>().firstOrNull()
    val yahooProvider = config.providers.filterIsInstance<AuthProvider.Yahoo>().firstOrNull()
    val twitterProvider = config.providers.filterIsInstance<AuthProvider.Twitter>().firstOrNull()
    val genericOAuthProviders = config.providers.filterIsInstance<AuthProvider.GenericOAuth>()

    val onSignInAnonymously = anonymousProvider?.let { rememberAnonymousSignInHandler(config, onSignInFailure) }
    val onSignInWithGoogle = googleProvider?.let { rememberGoogleSignInHandler(context, config, it, onSignInFailure) }
    val onSignInWithFacebook = facebookProvider?.let {
        rememberSignInWithFacebookLauncher(context, config, it, onSignInFailure = onSignInFailure)
    }
    val onSignInWithApple = appleProvider?.let { rememberOAuthSignInHandler(context, activity, config, it, onSignInFailure) }
    val onSignInWithGithub = githubProvider?.let { rememberOAuthSignInHandler(context, activity, config, it, onSignInFailure) }
    val onSignInWithMicrosoft = microsoftProvider?.let { rememberOAuthSignInHandler(context, activity, config, it, onSignInFailure) }
    val onSignInWithYahoo = yahooProvider?.let { rememberOAuthSignInHandler(context, activity, config, it, onSignInFailure) }
    val onSignInWithTwitter = twitterProvider?.let { rememberOAuthSignInHandler(context, activity, config, it, onSignInFailure) }
    val genericOAuthHandlers = genericOAuthProviders.associateWith {
        rememberOAuthSignInHandler(context, activity, config, it, onSignInFailure)
    }

    return { provider ->
        when (provider) {
            is AuthProvider.Anonymous -> onSignInAnonymously?.invoke()
            is AuthProvider.Email -> onNavigate(AuthRoute.Email)
            is AuthProvider.Phone -> onNavigate(AuthRoute.Phone)
            is AuthProvider.Google -> onSignInWithGoogle?.invoke()
            is AuthProvider.Facebook -> onSignInWithFacebook?.invoke()
            is AuthProvider.Apple -> onSignInWithApple?.invoke()
            is AuthProvider.Github -> onSignInWithGithub?.invoke()
            is AuthProvider.Microsoft -> onSignInWithMicrosoft?.invoke()
            is AuthProvider.Yahoo -> onSignInWithYahoo?.invoke()
            is AuthProvider.Twitter -> onSignInWithTwitter?.invoke()
            is AuthProvider.GenericOAuth -> genericOAuthHandlers[provider]?.invoke()
            else -> onUnknownProvider?.invoke(provider)
        }
    }
}
