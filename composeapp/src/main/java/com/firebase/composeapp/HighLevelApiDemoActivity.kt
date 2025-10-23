package com.firebase.composeapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.firebase.ui.auth.compose.AuthException
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.ui.screens.EmailSignInLinkHandlerActivity
import com.firebase.ui.auth.compose.ui.screens.FirebaseAuthScreen
import com.firebase.ui.auth.compose.ui.screens.AuthSuccessUiContext
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.actionCodeSettings

class HighLevelApiDemoActivity : ComponentActivity() {
    companion object {
        private const val USE_AUTH_EMULATOR = true
        private const val AUTH_EMULATOR_HOST = "10.0.2.2"
        private const val AUTH_EMULATOR_PORT = 9099
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(applicationContext)
        val authUI = FirebaseAuthUI.getInstance()

        if (USE_AUTH_EMULATOR) {
            authUI.auth.useEmulator(AUTH_EMULATOR_HOST, AUTH_EMULATOR_PORT)
        }

        val emailLink = intent.getStringExtra(EmailSignInLinkHandlerActivity.EXTRA_EMAIL_LINK)

        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        isDisplayNameRequired = true,
                        isEmailLinkForceSameDeviceEnabled = true,
                        isEmailLinkSignInEnabled = false,
                        emailLinkActionCodeSettings = actionCodeSettings {
                            url = "https://temp-test-aa342.firebaseapp.com"
                            handleCodeInApp = true
                            setAndroidPackageName(
                                "com.firebase.composeapp",
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
                        )
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
                    AuthProvider.Facebook(
                        applicationId = "792556260059222"
                    )
                )
            }
            tosUrl = "https://policies.google.com/terms?hl=en-NG&fg=1"
            privacyPolicyUrl = "https://policies.google.com/privacy?hl=en-NG&fg=1"
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
