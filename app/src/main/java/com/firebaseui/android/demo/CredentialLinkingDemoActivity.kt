package com.firebaseui.android.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.AuthState
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.ui.screens.AuthRoute
import com.firebase.ui.auth.ui.screens.AuthSuccessUiContext
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen

class CredentialLinkingDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authUI = FirebaseAuthUI.getInstance()

        val configuration = authUIConfiguration {
            context = applicationContext
            isCredentialLinkingEnabled = true
            providers {
                provider(
                    AuthProvider.Email(
                        isNewAccountsAllowed = true,
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList(),
                    )
                )
                provider(
                    AuthProvider.Google(
                        scopes = listOf("email"),
                        serverClientId = "406099696497-a12gakvts4epfk5pkio7dphc1anjiggc.apps.googleusercontent.com",
                    )
                )
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = emptyList(),
                        timeout = 120L,
                    )
                )
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FirebaseAuthScreen(
                        configuration = configuration,
                        authUI = authUI,
                        onSignInSuccess = {},
                        onSignInFailure = { _: AuthException -> },
                        onSignInCancelled = {},
                        authenticatedContent = { state, uiContext ->
                            CredentialLinkingAuthenticatedContent(state, uiContext)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CredentialLinkingAuthenticatedContent(
    state: AuthState,
    uiContext: AuthSuccessUiContext,
) {
    when (state) {
        is AuthState.Success -> {
            val user = state.user
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Signed in",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("UID: ${user.uid}", style = MaterialTheme.typography.bodySmall)
                        Text("Email: ${user.email ?: "—"}")
                        Text("Phone: ${user.phoneNumber ?: "—"}")
                        Text(
                            "Providers: ${user.providerData.map { it.providerId }}",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { uiContext.onNavigate(AuthRoute.MethodPicker) }
                ) {
                    Text("Add sign-in method")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = uiContext.onSignOut
                ) {
                    Text("Sign out")
                }
            }
        }

        is AuthState.RequiresEmailVerification -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Verify your email",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A verification link was sent to ${state.email}. Once verified, tap the button below.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = uiContext.onReloadUser
                ) {
                    Text("I've verified my email")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = uiContext.onSignOut
                ) {
                    Text("Sign out")
                }
            }
        }

        else -> {}
    }
}
