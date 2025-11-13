package com.firebaseui.android.demo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.PasswordRule
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.configuration.theme.AuthUITheme
import com.firebase.ui.auth.ui.screens.AuthSuccessUiContext
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen
import com.firebase.ui.auth.util.EmailLinkConstants
import com.google.firebase.auth.actionCodeSettings

class HighLevelApiDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authUI = FirebaseAuthUI.getInstance()
        val emailLink = intent.getStringExtra(EmailLinkConstants.EXTRA_EMAIL_LINK)

        val configuration = authUIConfiguration {
            context = applicationContext
            logo = AuthUIAsset.Resource(R.drawable.firebase_auth)
            tosUrl = "https://policies.google.com/terms"
            privacyPolicyUrl = "https://policies.google.com/privacy"
            isAnonymousUpgradeEnabled = false
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
                        authenticatedContent = { state, uiContext ->
                            AppAuthenticatedContent(state, uiContext)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppAuthenticatedContent(
    state: AuthState,
    uiContext: AuthSuccessUiContext
) {
    val stringProvider = uiContext.stringProvider
    when (state) {
        is AuthState.Success -> {
            val user = uiContext.authUI.getCurrentUser()
            val identifier = user?.email ?: user?.phoneNumber ?: user?.uid.orEmpty()
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
                Button(onClick = uiContext.onManageMfa) {
                    Text(stringProvider.manageMfaAction)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = uiContext.onSignOut) {
                    Text(stringProvider.signOutAction)
                }
            }
        }

        is AuthState.RequiresEmailVerification -> {
            val email = uiContext.authUI.getCurrentUser()?.email ?: stringProvider.emailProvider
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
