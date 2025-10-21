package com.firebase.composeapp.ui.screens

import android.util.Log
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import com.firebase.composeapp.R
import com.firebase.composeapp.Route
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.auth_provider.rememberSignInWithFacebookLauncher
import com.firebase.ui.auth.compose.configuration.auth_provider.signInAnonymously
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.compose.ui.components.ErrorRecoveryDialog
import com.firebase.ui.auth.compose.ui.method_picker.AuthMethodPicker
import kotlinx.coroutines.launch

@Composable
fun FirebaseAuthScreen(
    authUI: FirebaseAuthUI,
    configuration: AuthUIConfiguration,
    backStack: NavBackStack,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val authState by authUI.authStateFlow().collectAsState(AuthState.Idle)
    val stringProvider = DefaultAuthUIStringProvider(context)

    val isErrorDialogVisible = remember(authState) { mutableStateOf(authState is AuthState.Error) }

    val onSignInWithFacebook: () -> Unit =
        authUI.rememberSignInWithFacebookLauncher(
            context = context,
            config = configuration,
            provider = configuration.providers.filterIsInstance<AuthProvider.Facebook>()
                .first()
        )

    val onSignAnonymously: () -> Unit = {
        try {
            coroutineScope.launch {
                authUI.signInAnonymously()
            }
        } catch (e: Exception) {

        }
    }

    Scaffold { innerPadding ->
        Log.d("FirebaseAuthScreen", "Current state: $authState")
        Box {
            when (authState) {
                is AuthState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Authenticated User - (Success): ${authUI.getCurrentUser()?.email}",
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "UID - ${authUI.getCurrentUser()?.uid}",
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "isAnonymous - ${authUI.getCurrentUser()?.isAnonymous}",
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Providers - ${authUI.getCurrentUser()?.providerData?.map { it.providerId }}",
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (authUI.getCurrentUser()?.isAnonymous == true) {
                            Button(
                                onClick = {
                                    onSignInWithFacebook()
                                }
                            ) {
                                Text("Upgrade with Facebook")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    authUI.signOut(context)
                                }
                            }
                        ) {
                            Text("Sign Out")
                        }
                    }
                }

                is AuthState.RequiresEmailVerification -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Authenticated User - " +
                                    "(RequiresEmailVerification): ${authUI.getCurrentUser()?.email}",
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    authUI.signOut(context)
                                }
                            }
                        ) {
                            Text("Sign Out")
                        }
                    }
                }

                else -> {
                    AuthMethodPicker(
                        modifier = Modifier.padding(innerPadding),
                        providers = configuration.providers,
                        logo = AuthUIAsset.Resource(R.drawable.firebase_auth_120dp),
                        termsOfServiceUrl = configuration.tosUrl,
                        privacyPolicyUrl = configuration.privacyPolicyUrl,
                        onProviderSelected = { provider ->
                            Log.d(
                                "MainActivity",
                                "Selected Provider: $provider"
                            )
                            when (provider) {
                                is AuthProvider.Email -> backStack.add(Route.EmailAuth())
                                is AuthProvider.Phone -> backStack.add(Route.PhoneAuth)
                                is AuthProvider.Facebook -> onSignInWithFacebook()
                                is AuthProvider.Anonymous -> onSignAnonymously()
                            }
                        },
                    )
                }
            }

            // Error dialog
            if (isErrorDialogVisible.value && authState is AuthState.Error) {
                ErrorRecoveryDialog(
                    error = when ((authState as AuthState.Error).exception) {
                        is AuthException -> (authState as AuthState.Error).exception as AuthException
                        else -> AuthException.from((authState as AuthState.Error).exception)
                    },
                    stringProvider = stringProvider,
                    onRetry = { exception ->
                        isErrorDialogVisible.value = false
                    },
                    onRecover = { exception ->
                        when (exception) {
                            is AuthException.EmailAlreadyInUseException -> {
                                // Navigate to email sign-in
                                backStack.add(Route.EmailAuth())
                            }

                            is AuthException.AccountLinkingRequiredException -> {
                                backStack.add(Route.EmailAuth(credentialForLinking = exception.credential))
                            }

                            else -> {
                                // For other errors, just dismiss and let user try again
                            }
                        }
                        isErrorDialogVisible.value = false
                    },
                    onDismiss = {
                        isErrorDialogVisible.value = false
                    },
                )
            }

            // TODO(demolaf): We get double error dialog pop ups from FirebaseAuthScreen and other
            //  Screens e.g. EmailAuthScreen because they have dialog logics, is it possible to have
            //  one that pops up above all views?
            // Loading modal
            if (authState is AuthState.Loading) {
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
                                text = (authState as? AuthState.Loading)?.message
                                    ?: "Loading...",
                                textAlign = TextAlign.Center,
                                color = Color.White
                            )
                        }
                    }
                )
            }
        }
    }
}