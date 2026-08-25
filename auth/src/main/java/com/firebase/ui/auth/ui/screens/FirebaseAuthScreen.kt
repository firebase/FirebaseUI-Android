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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.BuildConfig
import com.firebase.ui.auth.FirebaseAuthActivity
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.R
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.MfaConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.auth_provider.filterToLinkedProviders
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
import com.firebase.ui.auth.ui.components.rememberTopLevelDialogController
import com.firebase.ui.auth.mfa.MfaChallengeContentState
import com.firebase.ui.auth.mfa.MfaEnrollmentContentState
import com.firebase.ui.auth.ui.method_picker.AuthMethodPicker
import com.firebase.ui.auth.ui.method_picker.MethodPickerTermsConfiguration
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebase.ui.auth.ui.screens.email.EmailAuthScreen
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthScreen
import com.firebase.ui.auth.util.EmailLinkPersistenceManager
import com.firebase.ui.auth.util.SignInPreferenceManager
import com.firebase.ui.auth.util.displayIdentifier
import com.firebase.ui.auth.util.getDisplayEmail
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.MultiFactorResolver
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * High-level authentication screen that wires together provider selection, individual provider
 * flows, error handling, and multi-factor enrollment/challenge flows. Back navigation is driven by
 * the Jetpack Navigation stack so presses behave like native Android navigation.
 *
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
 * receiving a [ReauthContentState]. The library owns the credential exchange.
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
    // Set FirebaseUI version
    LaunchedEffect(authUI.auth) {
        authUI.auth.setFirebaseUIVersion(BuildConfig.VERSION_NAME)
    }

    val activity = LocalActivity.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val stringProvider = remember(context) { DefaultAuthUIStringProvider(context) }
    val navController = rememberNavController()

    val authState by remember(authUI) { authUI.authStateFlow() }.collectAsState(AuthState.Idle)
    val dialogController = rememberTopLevelDialogController(stringProvider) { authState }
    val lastSuccessfulUserId = remember { mutableStateOf<String?>(null) }
    val pendingLinkingCredential = remember { mutableStateOf<AuthCredential?>(null) }
    val pendingResolver = remember { mutableStateOf<MultiFactorResolver?>(null) }
    val pendingReauthConfig = remember { mutableStateOf<AuthUIConfiguration?>(null) }
    val pendingReauthState = remember { mutableStateOf<AuthState.ReauthenticationRequired?>(null) }
    val pendingReauthOperation = remember { mutableStateOf<(suspend (android.content.Context) -> Unit)?>(null) }
    val reauthError = remember { mutableStateOf<String?>(null) }
    val reauthSubRoute = remember { mutableStateOf<AuthRoute?>(null) }
    val emailLinkFromDifferentDevice = remember { mutableStateOf<String?>(null) }
    val prefillEmail = remember { mutableStateOf<String?>(null) }
    val reauthPrefillEmail = remember(authUI, configuration.isReauthenticationMode) {
        if (configuration.isReauthenticationMode) authUI.auth.currentUser?.email else null
    }
    val lastSignInPreference =
        remember { mutableStateOf<SignInPreferenceManager.SignInPreference?>(null) }
    // Last-processed AuthState, so the Idle branch below can tell a genuine reset apart from
    // Idle-as-a-side-effect of consuming a notification (see AuthState.isNotification).
    val previousAuthState = remember { mutableStateOf<AuthState>(AuthState.Idle) }
    val startRoute = remember(configuration.providers, configuration.isProviderChoiceAlwaysShown) {
        getStartRoute(configuration)
    }
    val skipsMethodPicker = startRoute != AuthRoute.MethodPicker

    // Load last sign-in preference on launch
    LaunchedEffect(authState) {
        lastSignInPreference.value = SignInPreferenceManager.getLastSignIn(context)
    }

    val emailProvider = configuration.providers.filterIsInstance<AuthProvider.Email>().firstOrNull()
    val logoAsset = configuration.logo
    val onOuterProviderSelected = authUI.rememberOnProviderSelected(
        context = context,
        activity = activity,
        config = configuration,
        onNavigate = { route -> navController.navigate(route.route) },
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
    val onProviderSelected: (AuthProvider) -> Unit = { provider ->
        if (pendingReauthState.value == null) {
            onOuterProviderSelected(provider)
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
            modifier = Modifier
                .fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = startRoute.route,
                enterTransition = configuration.transitions?.enterTransition ?: {
                    fadeIn(animationSpec = tween(700))
                },
                exitTransition = configuration.transitions?.exitTransition ?: {
                    fadeOut(animationSpec = tween(700))
                },
                popEnterTransition = configuration.transitions?.popEnterTransition ?: {
                    fadeIn(animationSpec = tween(700))
                },
                popExitTransition = configuration.transitions?.popExitTransition ?: {
                    fadeOut(animationSpec = tween(700))
                }
            ) {
                composable(AuthRoute.MethodPicker.route) {
                    if (customMethodPickerLayout != null) {
                        Box(modifier = modifier.fillMaxSize()) {
                            customMethodPickerLayout(configuration.providers, onProviderSelected)
                        }
                    } else {
                        Scaffold { innerPadding ->
                            AuthMethodPicker(
                                modifier = modifier
                                    .padding(innerPadding),
                                providers = configuration.providers,
                                logo = logoAsset,
                                termsOfServiceUrl = configuration.tosUrl,
                                privacyPolicyUrl = configuration.privacyPolicyUrl,
                                lastSignInPreference = lastSignInPreference.value,
                                termsConfiguration = customMethodPickerTermsConfiguration,
                                onProviderSelected = { provider ->
                                    prefillEmail.value = null
                                    onProviderSelected(provider)
                                },
                                onContinueAsSelected = { provider, identifier ->
                                    prefillEmail.value = if (provider is AuthProvider.Email) identifier else null
                                    onProviderSelected(provider)
                                },
                            )
                        }
                    }
                }

                composable(AuthRoute.Email.route) {
                    EmailAuthScreen(
                        context = context,
                        configuration = configuration,
                        authUI = authUI,
                        // The reauth user's own address wins: a stale "Continue as" identifier
                        // would lock the field to an account that cannot be re-proved here.
                        prefillEmail = reauthPrefillEmail ?: prefillEmail.value,
                        credentialForLinking = pendingLinkingCredential.value,
                        emailLinkFromDifferentDevice = emailLinkFromDifferentDevice.value,
                        onContinueWithProvider = continueWithProvider,
                        content = emailContent,
                        onSuccess = {
                            pendingLinkingCredential.value = null
                        },
                        onError = { exception ->
                            onSignInFailure(exception)
                        },
                        onCancel = {
                            pendingLinkingCredential.value = null
                            if (!skipsMethodPicker && !navController.popBackStack()) {
                                navController.navigate(AuthRoute.MethodPicker.route) {
                                    popUpTo(AuthRoute.MethodPicker.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                composable(AuthRoute.Phone.route) {
                    PhoneAuthScreen(
                        context = context,
                        configuration = configuration,
                        authUI = authUI,
                        content = phoneContent,
                        onSuccess = {},
                        onError = { exception ->
                            onSignInFailure(exception)
                        },
                        onCancel = {
                            if (!skipsMethodPicker && !navController.popBackStack()) {
                                navController.navigate(AuthRoute.MethodPicker.route) {
                                    popUpTo(AuthRoute.MethodPicker.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                composable(AuthRoute.Success.route) {
                    val uiContext = remember(authState, stringProvider) {
                        AuthSuccessUiContext(
                            authUI = authUI,
                            stringProvider = stringProvider,
                            configuration = configuration,
                            onSignOut = {
                                coroutineScope.launch {
                                    try {
                                        authUI.signOut(context)
                                        // Keep sign-in preference for "Continue as..." on next launch
                                    } catch (e: Exception) {
                                        onSignInFailure(AuthException.from(e, stringProvider))
                                    } finally {
                                        pendingLinkingCredential.value = null
                                        pendingResolver.value = null
                                    }
                                }
                            },
                            onManageMfa = {
                                // Inert while armed: this content stays composed beneath the slot.
                                if (pendingReauthState.value == null) {
                                    if (configuration.isMfaEnabled) {
                                        navController.navigate(AuthRoute.MfaEnrollment.route)
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
                                        // Reload user to get fresh data from server
                                        authUI.getCurrentUser()?.let {
                                            it.reload().await()
                                            it.getIdToken(true).await()
                                            if (it.isEmailVerified) {
                                                authUI.updateAuthState(
                                                    AuthState.Success(
                                                        result = null,
                                                        user = it,
                                                        isNewUser = false
                                                    )
                                                )
                                            } else {
                                                authUI.updateAuthState(
                                                    AuthState.RequiresEmailVerification(
                                                        user = it,
                                                        email = it.email ?: ""
                                                    )
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("FirebaseAuthScreen", "Failed to refresh user", e)
                                    }
                                }
                            },
                            onNavigate = { route ->
                                // Inert while armed: this content stays composed beneath the slot.
                                if (pendingReauthState.value == null) {
                                    navController.navigate(route.route)
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

                composable(AuthRoute.MfaEnrollment.route) {
                    val user = authUI.getCurrentUser()
                    if (user != null) {
                        MfaEnrollmentScreen(
                            user = user,
                            auth = authUI.auth,
                            configuration = mfaConfiguration,
                            authConfiguration = configuration,
                            content = mfaEnrollmentContent,
                            onComplete = { navController.popBackStack() },
                            onSkip = { navController.popBackStack() },
                            onError = { exception ->
                                onSignInFailure(AuthException.from(exception, stringProvider))
                            }
                        )
                    } else {
                        navController.popBackStack()
                    }
                }

                composable(AuthRoute.MfaChallenge.route) {
                    val resolver = pendingResolver.value
                    if (resolver != null) {
                        MfaChallengeScreen(
                            resolver = resolver,
                            auth = authUI.auth,
                            content = mfaChallengeContent,
                            onSuccess = {
                                pendingResolver.value = null
                                // Reset auth state to Idle so the firebaseAuthFlow Success state takes over
                                authUI.updateAuthState(AuthState.Idle)
                            },
                            onCancel = {
                                pendingResolver.value = null
                                authUI.updateAuthState(AuthState.Cancelled)
                                navController.popBackStack()
                            },
                            onError = { exception ->
                                onSignInFailure(AuthException.from(exception, stringProvider))
                            }
                        )
                    } else {
                        navController.popBackStack()
                    }
                }
            }

            // Handle email link sign-in (deep links)
            LaunchedEffect(emailLink) {
                // A link arriving while armed would sign in on the non-reauth configuration, and
                // could sign in a different user the armed operation can then never match.
                if (emailLink != null && emailProvider != null && pendingReauthState.value == null) {
                    try {
                        // Try to retrieve saved email from DataStore (same-device flow)
                        val savedEmail =
                            EmailLinkPersistenceManager.default.retrieveSessionRecord(context)?.email

                        if (savedEmail != null) {
                            // Same device - we have the email, sign in automatically
                            authUI.signInWithEmailLink(
                                context = context,
                                config = configuration,
                                provider = emailProvider,
                                email = savedEmail,
                                emailLink = emailLink
                            )
                        } else {
                            // Different device - no saved email
                            // Call signInWithEmailLink with empty email to trigger validation
                            // This will throw EmailLinkPromptForEmailException or EmailLinkWrongDeviceException
                            authUI.signInWithEmailLink(
                                context = context,
                                config = configuration,
                                provider = emailProvider,
                                email = "", // Empty email triggers cross-device detection
                                emailLink = emailLink
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseAuthScreen", "Failed to complete email link sign-in", e)
                    }
                }
            }

            // Synchronise auth state changes with navigation stack.
            LaunchedEffect(authState) {
                val state = authState
                val previous = previousAuthState.value
                previousAuthState.value = state
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                when (state) {
                    is AuthState.Success -> {
                        pendingResolver.value = null
                        pendingLinkingCredential.value = null

                        val expectedReauthUid = pendingReauthState.value?.user?.uid
                        if (expectedReauthUid != null) {
                            if (state.reauthenticatedUid == expectedReauthUid) {
                                val retry = pendingReauthOperation.value
                                pendingReauthOperation.value = null
                                pendingReauthConfig.value = null
                                pendingReauthState.value = null
                                reauthSubRoute.value = null
                                reauthError.value = null
                                if (retry != null) {
                                    authUI.updateAuthState(AuthState.Loading())
                                    coroutineScope.launch {
                                        try {
                                            retry(context)
                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            authUI.updateAuthState(AuthState.Error(e))
                                        }
                                    }
                                } else if (currentRoute != AuthRoute.Success.route) {
                                    // Nothing to resume, but the slot is gone: land on Success
                                    // rather than whatever route it was covering.
                                    navController.navigate(AuthRoute.Success.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                }
                                // A reauthentication is never a sign-in: onSignInSuccess must not
                                // fire for it, whether or not an operation was attached.
                                return@LaunchedEffect
                            } else {
                                // Only the ambient re-emission for the armed user is benign; a
                                // stamp for another account leaves the slot inert, unexplained.
                                if (state.reauthenticatedUid != null ||
                                    authUI.auth.currentUser?.uid != expectedReauthUid
                                ) {
                                    reauthError.value =
                                        context.getString(R.string.fui_error_reauth_incomplete)
                                }
                                return@LaunchedEffect
                            }
                        }

                        state.result?.let { result ->
                            if (state.user.uid != lastSuccessfulUserId.value) {
                                onSignInSuccess(result)
                                lastSuccessfulUserId.value = state.user.uid

                                // Reload sign-in preference (may have been updated by provider)
                                coroutineScope.launch {
                                    lastSignInPreference.value =
                                        SignInPreferenceManager.getLastSignIn(context)
                                }
                            }
                        }

                        if (currentRoute != AuthRoute.Success.route) {
                            navController.navigate(AuthRoute.Success.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    is AuthState.ReauthenticationRequired -> {
                        val linked = configuration.providers.filterToLinkedProviders(state.user)
                        if (linked.isEmpty()) {
                            pendingReauthOperation.value = null
                            pendingReauthConfig.value = null
                            pendingReauthState.value = null
                            reauthSubRoute.value = null
                            reauthError.value = null
                            authUI.updateAuthState(
                                AuthState.Error(
                                    AuthException.UnknownException(
                                        context.getString(R.string.fui_error_reauth_no_linked_providers)
                                    )
                                )
                            )
                            return@LaunchedEffect
                        }
                        pendingReauthOperation.value = state.retryOperation
                        reauthSubRoute.value = null
                        reauthError.value = null
                        pendingReauthState.value = state
                        pendingReauthConfig.value = configuration.copy(
                            providers = linked,
                            isNewEmailAccountsAllowed = false,
                            isReauthenticationMode = true,
                        )
                    }

                    is AuthState.RequiresEmailVerification,
                    is AuthState.RequiresProfileCompletion,
                        -> {
                        // Reachable while armed (a wrong password in the sub-flow falls back to
                        // this): navigating would wipe the back stack out from under the slot.
                        if (pendingReauthState.value != null) return@LaunchedEffect
                        pendingResolver.value = null
                        pendingLinkingCredential.value = null
                        if (currentRoute != AuthRoute.Success.route) {
                            navController.navigate(AuthRoute.Success.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    is AuthState.RequiresMfa -> {
                        pendingResolver.value = state.resolver
                        if (currentRoute != AuthRoute.MfaChallenge.route) {
                            navController.navigate(AuthRoute.MfaChallenge.route) {
                                launchSingleTop = true
                            }
                        }
                    }

                    is AuthState.Cancelled -> {
                        if (pendingReauthState.value != null) {
                            authUI.updateAuthState(AuthState.Idle)
                            return@LaunchedEffect
                        }
                        pendingReauthOperation.value = null
                        pendingReauthConfig.value = null
                        pendingReauthState.value = null
                        reauthSubRoute.value = null
                        reauthError.value = null
                        pendingResolver.value = null
                        pendingLinkingCredential.value = null
                        lastSuccessfulUserId.value = null
                        if (currentRoute != startRoute.route) {
                            navController.navigate(startRoute.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        onSignInCancelled()
                        authUI.updateAuthState(AuthState.Idle)
                    }

                    is AuthState.Aborted -> {
                        // Hosted by FirebaseAuthActivity: its own authStateFlow collector
                        // independently finishes the activity and resets state on Aborted.
                        if (activity !is FirebaseAuthActivity) {
                            pendingReauthOperation.value = null
                            pendingReauthConfig.value = null
                            pendingReauthState.value = null
                            reauthSubRoute.value = null
                            reauthError.value = null
                            pendingResolver.value = null
                            pendingLinkingCredential.value = null
                            lastSuccessfulUserId.value = null
                            authUI.updateAuthState(AuthState.Idle)
                        }
                    }

                    is AuthState.Idle -> {
                        // A notification resets to Idle purely to avoid leaking to a freshly
                        // created screen — that's not a request to leave the current one.
                        if (!previous.isNotification) {
                            pendingReauthOperation.value = null
                            pendingReauthConfig.value = null
                            pendingReauthState.value = null
                            reauthSubRoute.value = null
                            reauthError.value = null
                            pendingResolver.value = null
                            pendingLinkingCredential.value = null
                            lastSuccessfulUserId.value = null
                            if (currentRoute != startRoute.route) {
                                navController.navigate(startRoute.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                    else -> Unit
                }
            }

            val reauthSlotActive = reauthContent != null &&
                    pendingReauthState.value != null &&
                    reauthSubRoute.value == null

            // Handle errors using top-level dialog controller
            val errorState = authState as? AuthState.Error
            if (errorState != null) {
                LaunchedEffect(errorState) {
                    val exception = when (val throwable = errorState.exception) {
                        is AuthException -> throwable
                        else -> AuthException.from(throwable, stringProvider)
                    }

                    if (reauthSlotActive) {
                        if (exception !is AuthException.AuthCancelledException) {
                            reauthError.value = exception.message
                        }
                        authUI.updateAuthState(AuthState.Idle)
                        return@LaunchedEffect
                    }

                    dialogController.showErrorDialog(
                        exception = exception,
                        errorState = errorState,
                        // Child screens own their retry logic, so there is nothing to retry here.
                        onRetry = null,
                        onRecover = if (pendingReauthState.value != null) null else when (exception) {
                            is AuthException.EmailAlreadyInUseException -> {
                                {
                                    navController.navigate(AuthRoute.Email.route) {
                                        launchSingleTop = true
                                    }
                                }
                            }

                            is AuthException.AccountLinkingRequiredException -> {
                                {
                                    pendingLinkingCredential.value = exception.credential
                                    navController.navigate(AuthRoute.Email.route) {
                                        launchSingleTop = true
                                    }
                                }
                            }

                            is AuthException.EmailLinkPromptForEmailException -> {
                                {
                                    emailLinkFromDifferentDevice.value = exception.emailLink
                                    navController.navigate(AuthRoute.Email.route) {
                                        launchSingleTop = true
                                    }
                                }
                            }

                            is AuthException.EmailLinkCrossDeviceLinkingException -> {
                                {
                                    emailLinkFromDifferentDevice.value = exception.emailLink
                                    navController.navigate(AuthRoute.Email.route) {
                                        launchSingleTop = true
                                    }
                                }
                            }

                            is AuthException.DifferentSignInMethodRequiredException -> {
                                {
                                    val providerId = exception.suggestedSignInMethod
                                    if (providerId == EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD) {
                                        navController.navigate(AuthRoute.Email.route) {
                                            launchSingleTop = true
                                        }
                                    } else {
                                        continueWithProvider(providerId)
                                    }
                                }
                            }

                            else -> null
                        },
                        onDismiss = {
                            // Dialog dismissed
                        }
                    )
                    // Consumed immediately so this doesn't leak to a freshly created screen.
                    authUI.updateAuthState(AuthState.Idle)
                }
            }

            // Render the top-level dialog (only one instance)
            dialogController.CurrentDialog()

            val loadingState = authState as? AuthState.Loading
            if (loadingState != null && !reauthSlotActive) {
                LoadingDialog(loadingState.message ?: stringProvider.progressDialogLoading)
            }

            val onReauthDismiss: () -> Unit = remember(authUI, onSignInCancelled) {
                {
                    pendingReauthOperation.value = null
                    pendingReauthConfig.value = null
                    pendingReauthState.value = null
                    reauthSubRoute.value = null
                    reauthError.value = null
                    authUI.updateAuthState(AuthState.Idle)
                    // Abandoning reauthentication drops the pending operation for good, so the
                    // host has to learn it will never run. A cancelled provider attempt does not.
                    onSignInCancelled()
                }
            }
            val onReauthAttemptStarted: () -> Unit = remember { { reauthError.value = null } }
            val onReauthSubRouteChange: (AuthRoute?) -> Unit =
                remember { { route -> reauthSubRoute.value = route } }

            val reauthConfig = pendingReauthConfig.value
            val pendingReauth = pendingReauthState.value
            if (reauthConfig != null && pendingReauth != null) {
                if (reauthContent != null) {
                    CustomReauthContent(
                        authUI = authUI,
                        reauthConfig = reauthConfig,
                        reauthState = pendingReauth,
                        activity = activity,
                        context = context,
                        emailContent = emailContent,
                        phoneContent = phoneContent,
                        isLoading = loadingState != null,
                        error = reauthError.value,
                        activeSubRoute = reauthSubRoute.value,
                        onActiveSubRouteChange = onReauthSubRouteChange,
                        onAttemptStarted = onReauthAttemptStarted,
                        onDismiss = onReauthDismiss,
                        content = reauthContent,
                    )
                } else {
                    ModalBottomSheet(
                        onDismissRequest = onReauthDismiss,
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        ReauthSheetContent(
                            authUI = authUI,
                            reauthConfig = reauthConfig,
                            activity = activity,
                            context = context,
                            prefillEmail = pendingReauth.user.email,
                            emailContent = emailContent,
                            phoneContent = phoneContent,
                            customMethodPickerLayout = customMethodPickerLayout,
                            onDismiss = onReauthDismiss,
                        )
                    }
                }
            }
        }
    }
}

sealed class AuthRoute(val route: String) {
    object MethodPicker : AuthRoute("auth_method_picker")
    object Email : AuthRoute("auth_email")
    object Phone : AuthRoute("auth_phone")
    object Success : AuthRoute("auth_success")
    object MfaEnrollment : AuthRoute("auth_mfa_enrollment")
    object MfaChallenge : AuthRoute("auth_mfa_challenge")
}

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
                    PlainTooltip {
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReauthSheetContent(
    authUI: FirebaseAuthUI,
    reauthConfig: AuthUIConfiguration,
    activity: android.app.Activity?,
    context: android.content.Context,
    prefillEmail: String?,
    emailContent: (@Composable (EmailAuthContentState) -> Unit)?,
    phoneContent: (@Composable (PhoneAuthContentState) -> Unit)?,
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
                Scaffold { innerPadding ->
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
                    if (skipsMethodPicker || !sheetNavController.popBackStack()) onDismiss()
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
                    if (skipsMethodPicker || !sheetNavController.popBackStack()) onDismiss()
                }
            )
        }
    }
}

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
 * @param onAttemptStarted Invoked just before a provider attempt begins.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomReauthContent(
    authUI: FirebaseAuthUI,
    reauthConfig: AuthUIConfiguration,
    reauthState: AuthState.ReauthenticationRequired,
    activity: android.app.Activity?,
    context: android.content.Context,
    emailContent: (@Composable (EmailAuthContentState) -> Unit)?,
    phoneContent: (@Composable (PhoneAuthContentState) -> Unit)?,
    isLoading: Boolean,
    error: String?,
    activeSubRoute: AuthRoute?,
    onActiveSubRouteChange: (AuthRoute?) -> Unit,
    onAttemptStarted: () -> Unit,
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
            onAttemptStarted()
            currentOnProviderSelected.value(provider)
        }
    }
    val closeSubFlow: () -> Unit =
        remember(onActiveSubRouteChange) { { onActiveSubRouteChange(null) } }

    when (val subRoute = activeSubRoute) {
        null -> content(
            ReauthContentState(
                user = reauthState.user,
                reason = reauthState.reason,
                providers = reauthConfig.providers,
                onProviderSelected = onProviderSelectedFromSlot,
                isLoading = isLoading,
                error = error,
                onDismiss = onDismiss,
            )
        )

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

        else -> throw IllegalStateException(
            "rememberOnProviderSelected navigated to ${subRoute.route}, which has no reauth " +
                "sub-flow. Add a branch here when a new provider gains its own screen."
        )
    }
}

@Composable
private fun FirebaseAuthUI.rememberOnProviderSelected(
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
