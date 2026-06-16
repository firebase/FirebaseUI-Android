package com.firebaseui.android.demo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUITransitions
import com.firebase.ui.auth.configuration.PasswordRule
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.configuration.theme.AuthUITheme
import com.firebase.ui.auth.ui.screens.AuthSuccessUiContext
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.firebase.ui.auth.util.EmailLinkConstants
import com.firebase.ui.auth.util.displayIdentifier
import com.firebase.ui.auth.util.getDisplayEmail
import com.google.firebase.auth.actionCodeSettings

class HighLevelApiDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authUI = FirebaseAuthUI.getInstance()
        val emailLink = intent.getStringExtra(EmailLinkConstants.EXTRA_EMAIL_LINK)

        val customTheme = AuthUITheme.Default.copy(
            providerButtonShape = ShapeDefaults.ExtraLarge
        )

        val configuration = authUIConfiguration {
            context = applicationContext
            theme = customTheme
            logo = AuthUIAsset.Resource(R.drawable.firebase_auth)
            tosUrl = "https://policies.google.com/terms"
            privacyPolicyUrl = "https://policies.google.com/privacy"
            isAnonymousUpgradeEnabled = false
            isMfaEnabled = false
            transitions = AuthUITransitions(
                enterTransition = { slideInHorizontally { it } },
                exitTransition = { slideOutHorizontally { -it } },
                popEnterTransition = { slideInHorizontally { -it } },
                popExitTransition = { slideOutHorizontally { it } }
            )
            providers {
                provider(AuthProvider.Anonymous)
                provider(
                    AuthProvider.Google(
                        scopes = listOf("email"),
                        serverClientId = "406099696497-a12gakvts4epfk5pkio7dphc1anjiggc.apps.googleusercontent.com",
                    )
                )
                provider(
                    AuthProvider.Email(
                        isDisplayNameRequired = true,
                        isEmailLinkForceSameDeviceEnabled = false,
                        isEmailLinkSignInEnabled = true,
                        emailLinkActionCodeSettings = actionCodeSettings {
                            url = "https://flutterfire-e2e-tests.firebaseapp.com"
                            handleCodeInApp = true
                            setAndroidPackageName(
                                "com.firebaseui.android.demo",
                                true,
                                null
                            )
                        },
                        isNewAccountsAllowed = true,
                        minimumPasswordLength = 8,
                        passwordValidationRules = listOf(
                            PasswordRule.MinimumLength(8),
                            PasswordRule.RequireLowercase,
                            PasswordRule.RequireUppercase,
                        ),
                    )
                )
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = emptyList(),
                        smsCodeLength = 6,
                        timeout = 120L,
                        isInstantVerificationEnabled = true
                    )
                )
                provider(
                    AuthProvider.Facebook()
                )
                provider(
                    AuthProvider.Twitter(
                        customParameters = emptyMap()
                    )
                )
                provider(
                    AuthProvider.Apple(
                        customParameters = emptyMap(),
                        locale = null
                    )
                )
                provider(
                    AuthProvider.Microsoft(
                        scopes = emptyList(),
                        tenant = "",
                        customParameters = emptyMap(),
                    )
                )
                provider(
                    AuthProvider.Github(
                        scopes = emptyList(),
                        customParameters = emptyMap(),
                    )
                )
                provider(
                    AuthProvider.Yahoo(
                        scopes = emptyList(),
                        customParameters = emptyMap(),
                    )
                )
                provider(
                    AuthProvider.GenericOAuth(
                        providerName = "LINE",
                        providerId = "oidc.line",
                        scopes = emptyList(),
                        customParameters = emptyMap(),
                        buttonLabel = "Sign in with LINE",
                        buttonIcon = AuthUIAsset.Resource(R.drawable.ic_line_logo_24dp),
                        buttonColor = Color(0xFF06C755),
                        contentColor = Color.White
                    )
                )
                provider(
                    AuthProvider.GenericOAuth(
                        providerName = "Discord",
                        providerId = "oidc.discord",
                        scopes = emptyList(),
                        customParameters = emptyMap(),
                        buttonLabel = "Sign in with Discord",
                        buttonIcon = AuthUIAsset.Resource(R.drawable.ic_discord_24dp),
                        buttonColor = Color(0xFF5865F2),
                        contentColor = Color.White
                    )
                )
            }
        }

        setContent {
            AuthUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FirebaseAuthScreen(
                        configuration = configuration,
                        authUI = authUI,
                        emailLink = emailLink,
                        onSignInSuccess = { result ->
                            Log.d("HighLevelApiDemoActivity", "Authentication success: ${result.user?.uid}")
                        },
                        onSignInFailure = { exception: AuthException ->
                            Log.e("HighLevelApiDemoActivity", "Authentication failed", exception)
                        },
                        onSignInCancelled = {
                            Log.d("HighLevelApiDemoActivity", "Authentication cancelled")
                        },
                        reauthContent = { state, onDismiss ->
                            ReauthDialog(
                                authUI = authUI,
                                state = state,
                                onDismiss = onDismiss,
                            )
                        },
                        authenticatedContent = { state, uiContext ->
                            AppAuthenticatedContent(state, uiContext)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppAuthenticatedContent(
    state: AuthState,
    uiContext: AuthSuccessUiContext
) {
    val stringProvider = uiContext.stringProvider
    val configuration = uiContext.configuration
    when (state) {
        is AuthState.Success -> {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            var isDeletingAccount by remember { mutableStateOf(false) }
            val user = uiContext.authUI.getCurrentUser()
            val identifier = user.displayIdentifier()
            var showChangePasswordDialog by remember { mutableStateOf(false) }

            if (showChangePasswordDialog) {
                ChangePasswordDialog(
                    authUI = uiContext.authUI,
                    configuration = uiContext.configuration,
                    stringProvider = uiContext.stringProvider,
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    onDismiss = { showChangePasswordDialog = false },
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (identifier.isNotBlank()) {
                    Text(
                        text = stringProvider.signedInAs(identifier),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    "isAnonymous - ${state.user.isAnonymous}",
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Providers - ${state.user.providerData.map { it.providerId }}",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
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
                        onClick = uiContext.onManageMfa,
                        enabled = configuration.isMfaEnabled
                    ) {
                        Text(stringProvider.manageMfaAction)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = uiContext.onSignOut) {
                    Text(stringProvider.signOutAction)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showChangePasswordDialog = true }) {
                    Text("Change password (withReauth)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        lifecycleOwner.lifecycleScope.launch {
                            isDeletingAccount = true
                            try {
                                uiContext.authUI.delete(context)
                            } catch (e: AuthException.InvalidCredentialsException) {
                                // ReauthenticationRequired state was emitted —
                                // FirebaseAuthScreen navigates to the reauth flow automatically.
                                Log.d("HighLevelApiDemoActivity", "Reauth required before delete")
                            } catch (e: AuthException) {
                                Log.e("HighLevelApiDemoActivity", "Delete failed", e)
                            } finally {
                                isDeletingAccount = false
                            }
                        }
                    },
                    enabled = !isDeletingAccount
                ) {
                    if (isDeletingAccount) CircularProgressIndicator() else Text("Delete account")
                }
            }
        }

        is AuthState.RequiresEmailVerification -> {
            val email = uiContext.authUI.getCurrentUser().getDisplayEmail(stringProvider.emailProvider)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringProvider.verifyEmailInstruction(email),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { uiContext.authUI.getCurrentUser()?.sendEmailVerification() }) {
                    Text(stringProvider.resendVerificationEmailAction)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = uiContext.onReloadUser) {
                    Text(stringProvider.verifiedEmailAction)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = uiContext.onSignOut) {
                    Text(stringProvider.signOutAction)
                }
            }
        }

        is AuthState.RequiresProfileCompletion -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringProvider.profileCompletionMessage,
                    textAlign = TextAlign.Center
                )
                if (state.missingFields.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringProvider.profileMissingFieldsMessage(state.missingFields.joinToString()),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = uiContext.onSignOut) {
                    Text(stringProvider.signOutAction)
                }
            }
        }

        else -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun ReauthDialog(
    authUI: FirebaseAuthUI,
    state: AuthState.ReauthenticationRequired,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val email = state.user.email.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Verify your identity")
                state.reason?.let { reason ->
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Signing in as $email",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                com.firebase.ui.auth.ui.components.AuthTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    isSecureTextField = true,
                    isError = errorMessage != null,
                    errorMessage = errorMessage,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isVerifying = true
                        errorMessage = null
                        try {
                            val result = authUI.auth
                                .signInWithEmailAndPassword(email, password)
                                .await()
                            result.user?.let { user ->
                                authUI.updateAuthState(AuthState.Success(result, user))
                            }
                        } catch (e: Exception) {
                            errorMessage = "Incorrect password. Please try again."
                        } finally {
                            isVerifying = false
                        }
                    }
                },
                enabled = password.isNotBlank() && !isVerifying,
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Verify")
                }
            }
        },
    )
}

@Composable
private fun ChangePasswordDialog(
    authUI: FirebaseAuthUI,
    configuration: com.firebase.ui.auth.configuration.AuthUIConfiguration,
    stringProvider: com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider,
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onDismiss: () -> Unit,
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf<String?>(null) }

    val emailProvider = remember(configuration) {
        configuration.providers.filterIsInstance<com.firebase.ui.auth.configuration.auth_provider.AuthProvider.Email>().firstOrNull()
    }
    val passwordValidator = remember(emailProvider, stringProvider) {
        com.firebase.ui.auth.configuration.validators.PasswordValidator(
            stringProvider = stringProvider,
            rules = emailProvider?.passwordValidationRules ?: emptyList(),
        )
    }
    val confirmValidator = remember(stringProvider) {
        com.firebase.ui.auth.configuration.validators.PasswordValidator(
            stringProvider = stringProvider,
            rules = emptyList(),
        )
    }

    val passwordsMatch = newPassword == confirmPassword
    val isValid = !passwordValidator.hasError && newPassword.isNotBlank() &&
            passwordsMatch && confirmPassword.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                com.firebase.ui.auth.ui.components.AuthTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        updateError = null
                    },
                    label = { Text("New password") },
                    isSecureTextField = true,
                    validator = passwordValidator,
                )
                com.firebase.ui.auth.ui.components.AuthTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        updateError = null
                    },
                    label = { Text("Confirm password") },
                    isSecureTextField = true,
                    isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                    errorMessage = if (confirmPassword.isNotEmpty() && !passwordsMatch) "Passwords do not match" else null,
                    validator = confirmValidator,
                )
                if (updateError != null) {
                    Text(
                        updateError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = {
                    lifecycleOwner.lifecycleScope.launch {
                        isUpdating = true
                        updateError = null
                        try {
                            authUI.withReauth(
                                context,
                                reason = "Verify your identity to change your password",
                            ) {
                                authUI.getCurrentUser()?.updatePassword(newPassword)?.await()
                                Log.d("HighLevelApiDemoActivity", "Password changed successfully")
                                onDismiss()
                            }
                        } catch (e: Exception) {
                            updateError = "Failed to update password. Please try again."
                        } finally {
                            isUpdating = false
                        }
                    }
                },
                enabled = isValid && !isUpdating,
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Update")
                }
            }
        },
    )
}
