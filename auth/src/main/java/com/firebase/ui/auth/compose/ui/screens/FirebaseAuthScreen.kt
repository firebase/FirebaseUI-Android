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

package com.firebase.ui.auth.compose.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.MfaConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.auth_provider.rememberSignInWithFacebookLauncher
import com.firebase.ui.auth.compose.configuration.auth_provider.signInWithEmailLink
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.compose.ui.components.ErrorRecoveryDialog
import com.firebase.ui.auth.compose.ui.method_picker.AuthMethodPicker
import com.firebase.ui.auth.compose.ui.screens.phone.PhoneAuthScreen
import com.firebase.ui.auth.compose.util.EmailLinkPersistenceManager
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.MultiFactorResolver
import kotlinx.coroutines.launch

/**
 * High-level authentication screen that wires together provider selection, individual provider
 * flows, error handling, and multi-factor enrollment/challenge flows. Back navigation is driven by
 * the Jetpack Navigation stack so presses behave like native Android navigation.
 *
 * @param authenticatedContent Optional slot that allows callers to render the authenticated
 * state themselves. When provided, it receives the current [AuthState] alongside an
 * [AuthSuccessUiContext] containing common callbacks (sign out, manage MFA, reload user).
 *
 * @since 10.0.0
 */
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
    authenticatedContent: (@Composable (state: AuthState, uiContext: AuthSuccessUiContext) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val stringProvider = DefaultAuthUIStringProvider(context)
    val navController = rememberNavController()

    val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
    val isErrorDialogVisible = remember(authState) { mutableStateOf(authState is AuthState.Error) }
    val lastSuccessfulUserId = remember { mutableStateOf<String?>(null) }
    val pendingLinkingCredential = remember { mutableStateOf<AuthCredential?>(null) }
    val pendingResolver = remember { mutableStateOf<MultiFactorResolver?>(null) }

    val emailProvider = configuration.providers.filterIsInstance<AuthProvider.Email>().firstOrNull()
    val facebookProvider = configuration.providers.filterIsInstance<AuthProvider.Facebook>().firstOrNull()
    val logoAsset = configuration.logo?.let { AuthUIAsset.Vector(it) }

    val onSignInWithFacebook = facebookProvider?.let {
        authUI.rememberSignInWithFacebookLauncher(
            context = context,
            config = configuration,
            provider = it
        )
    }

    // Handle email link sign-in (deep links)
    LaunchedEffect(emailLink) {
        if (emailLink != null && emailProvider != null) {
            try {
                EmailLinkPersistenceManager.retrieveSessionRecord(context)?.email?.let { email ->
                    authUI.signInWithEmailLink(
                        context = context,
                        config = configuration,
                        provider = emailProvider,
                        email = email,
                        emailLink = emailLink
                    )
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuthScreen", "Failed to complete email link sign-in", e)
            }

            if (navController.currentBackStackEntry?.destination?.route != AuthRoute.Email.route) {
                navController.navigate(AuthRoute.Email.route)
            }
        }
    }

    // Synchronise auth state changes with navigation stack.
    LaunchedEffect(authState) {
        val state = authState
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        when (state) {
            is AuthState.Success -> {
                pendingResolver.value = null
                pendingLinkingCredential.value = null

                state.result?.let { result ->
                    if (state.user.uid != lastSuccessfulUserId.value) {
                        onSignInSuccess(result)
                        lastSuccessfulUserId.value = state.user.uid
                    }
                }

                if (currentRoute != AuthRoute.Success.route) {
                    navController.navigate(AuthRoute.Success.route) {
                        popUpTo(AuthRoute.MethodPicker.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            is AuthState.RequiresEmailVerification,
            is AuthState.RequiresProfileCompletion -> {
                pendingResolver.value = null
                pendingLinkingCredential.value = null
                if (currentRoute != AuthRoute.Success.route) {
                    navController.navigate(AuthRoute.Success.route) {
                        popUpTo(AuthRoute.MethodPicker.route) { inclusive = true }
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
                pendingResolver.value = null
                pendingLinkingCredential.value = null
                lastSuccessfulUserId.value = null
                if (currentRoute != AuthRoute.MethodPicker.route) {
                    navController.navigate(AuthRoute.MethodPicker.route) {
                        popUpTo(AuthRoute.MethodPicker.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                onSignInCancelled()
            }

            is AuthState.Idle -> {
                pendingResolver.value = null
                pendingLinkingCredential.value = null
                lastSuccessfulUserId.value = null
                if (currentRoute != AuthRoute.MethodPicker.route) {
                    navController.navigate(AuthRoute.MethodPicker.route) {
                        popUpTo(AuthRoute.MethodPicker.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            else -> Unit
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = AuthRoute.MethodPicker.route
            ) {
                composable(AuthRoute.MethodPicker.route) {
                    AuthMethodPicker(
                        providers = configuration.providers,
                        logo = logoAsset,
                        termsOfServiceUrl = configuration.tosUrl,
                        privacyPolicyUrl = configuration.privacyPolicyUrl,
                        onProviderSelected = { provider ->
                            when (provider) {
                                is AuthProvider.Email -> {
                                    navController.navigate(AuthRoute.Email.route)
                                }

                                is AuthProvider.Phone -> {
                                    navController.navigate(AuthRoute.Phone.route)
                                }

                                is AuthProvider.Facebook -> onSignInWithFacebook?.invoke()

                                else -> {
                                    onSignInFailure(
                                        AuthException.UnknownException(
                                            message = "Provider ${provider.providerId} is not supported in FirebaseAuthScreen",
                                            cause = IllegalArgumentException(
                                                "Provider ${provider.providerId} is not supported in FirebaseAuthScreen"
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    )
                }

                composable(AuthRoute.Email.route) {
                    EmailAuthScreen(
                        context = context,
                        configuration = configuration,
                        authUI = authUI,
                        credentialForLinking = pendingLinkingCredential.value,
                        onSuccess = {
                            pendingLinkingCredential.value = null
                        },
                        onError = { exception ->
                            onSignInFailure(exception)
                        },
                        onCancel = {
                            pendingLinkingCredential.value = null
                            if (!navController.popBackStack()) {
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
                        onSuccess = {},
                        onError = { exception ->
                            onSignInFailure(exception)
                        },
                        onCancel = {
                            if (!navController.popBackStack()) {
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
                            onSignOut = {
                                coroutineScope.launch {
                                    try {
                                        authUI.signOut(context)
                                    } catch (e: Exception) {
                                        onSignInFailure(AuthException.from(e))
                                    } finally {
                                        pendingLinkingCredential.value = null
                                        pendingResolver.value = null
                                    }
                                }
                            },
                            onManageMfa = {
                                navController.navigate(AuthRoute.MfaEnrollment.route)
                            },
                            onReloadUser = {
                                coroutineScope.launch {
                                    try {
                                        authUI.getCurrentUser()?.reload()
                                        authUI.getCurrentUser()?.getIdToken(true)
                                    } catch (e: Exception) {
                                        Log.e("FirebaseAuthScreen", "Failed to refresh user", e)
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
                            onComplete = { navController.popBackStack() },
                            onSkip = { navController.popBackStack() },
                            onError = { exception ->
                                onSignInFailure(AuthException.from(exception))
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
                            onSuccess = {
                                pendingResolver.value = null
                            },
                            onCancel = {
                                pendingResolver.value = null
                                navController.popBackStack()
                            },
                            onError = { exception ->
                                onSignInFailure(AuthException.from(exception))
                            }
                        )
                    } else {
                        navController.popBackStack()
                    }
                }
            }

            val errorState = authState as? AuthState.Error
            if (isErrorDialogVisible.value && errorState != null) {
                ErrorRecoveryDialog(
                    error = when (val throwable = errorState.exception) {
                        is AuthException -> throwable
                        else -> AuthException.from(throwable)
                    },
                    stringProvider = stringProvider,
                    onRetry = { exception ->
                        when (exception) {
                            is AuthException.InvalidCredentialsException -> Unit
                            else -> Unit
                        }
                        isErrorDialogVisible.value = false
                    },
                    onRecover = { exception ->
                        when (exception) {
                            is AuthException.EmailAlreadyInUseException -> {
                                navController.navigate(AuthRoute.Email.route) {
                                    launchSingleTop = true
                                }
                            }

                            is AuthException.AccountLinkingRequiredException -> {
                                pendingLinkingCredential.value = exception.credential
                                navController.navigate(AuthRoute.Email.route) {
                                    launchSingleTop = true
                                }
                            }

                            else -> Unit
                        }
                        isErrorDialogVisible.value = false
                    },
                    onDismiss = {
                        isErrorDialogVisible.value = false
                    }
                )
            }

            val loadingState = authState as? AuthState.Loading
            if (loadingState != null) {
                LoadingDialog(loadingState.message ?: stringProvider.progressDialogLoading)
            }
        }
    }
}

private sealed class AuthRoute(val route: String) {
    object MethodPicker : AuthRoute("auth_method_picker")
    object Email : AuthRoute("auth_email")
    object Phone : AuthRoute("auth_phone")
    object Success : AuthRoute("auth_success")
    object MfaEnrollment : AuthRoute("auth_mfa_enrollment")
    object MfaChallenge : AuthRoute("auth_mfa_challenge")
}

data class AuthSuccessUiContext(
    val authUI: FirebaseAuthUI,
    val stringProvider: com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider,
    val onSignOut: () -> Unit,
    val onManageMfa: () -> Unit,
    val onReloadUser: () -> Unit
)

@Composable
private fun SuccessDestination(
    authState: AuthState,
    stringProvider: com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider,
    uiContext: AuthSuccessUiContext
) {
    when (authState) {
        is AuthState.Success -> {
            AuthSuccessContent(
                authUI = uiContext.authUI,
                stringProvider = stringProvider,
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

@Composable
private fun AuthSuccessContent(
    authUI: FirebaseAuthUI,
    stringProvider: com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider,
    onSignOut: () -> Unit,
    onManageMfa: () -> Unit
) {
    val user = authUI.getCurrentUser()
    val userIdentifier = user?.email ?: user?.phoneNumber ?: user?.uid.orEmpty()
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
            Button(onClick = onManageMfa) {
                Text(stringProvider.manageMfaAction)
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
    stringProvider: com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider,
    onCheckStatus: () -> Unit,
    onSignOut: () -> Unit
) {
    val user = authUI.getCurrentUser()
    val emailLabel = user?.email ?: stringProvider.emailProvider
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
    stringProvider: com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
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
        text = {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}
