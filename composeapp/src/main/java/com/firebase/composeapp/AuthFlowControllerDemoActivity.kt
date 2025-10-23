package com.firebase.composeapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.AuthFlowController
import com.firebase.ui.auth.compose.AuthState
import com.firebase.ui.auth.compose.FirebaseAuthActivity
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.actionCodeSettings
import kotlinx.coroutines.launch

/**
 * Demo activity showcasing the AuthFlowController API for managing
 * Firebase authentication with lifecycle-safe control.
 *
 * This demonstrates:
 * - Creating an AuthFlowController with configuration
 * - Starting the auth flow using ActivityResultLauncher
 * - Observing auth state changes
 * - Handling results (success, cancelled, error)
 * - Proper lifecycle management with dispose()
 */
class AuthFlowControllerDemoActivity : ComponentActivity() {

    private lateinit var authController: AuthFlowController

    // Modern ActivityResultLauncher for auth flow
    private val authLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                // Get user data from result
                val userId = result.data?.getStringExtra(FirebaseAuthActivity.EXTRA_USER_ID)
                val isNewUser = result.data?.getBooleanExtra(
                    FirebaseAuthActivity.EXTRA_IS_NEW_USER,
                    false
                ) ?: false

                val user = FirebaseAuth.getInstance().currentUser
                val message = if (isNewUser) {
                    "Welcome new user! ${user?.email ?: userId}"
                } else {
                    "Welcome back! ${user?.email ?: userId}"
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
            Activity.RESULT_CANCELED -> {
                Toast.makeText(this, "Auth cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(applicationContext)

        // Initialize FirebaseAuthUI
        val authUI = FirebaseAuthUI.getInstance()

        // Use emulator if needed
        if (USE_AUTH_EMULATOR) {
            authUI.auth.useEmulator(AUTH_EMULATOR_HOST, AUTH_EMULATOR_PORT)
        }

        // Create auth configuration
        val configuration = AuthUIConfiguration(
            context = applicationContext,
            providers = listOf(
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
                ),
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = null,
                    allowedCountries = emptyList(),
                    smsCodeLength = 6,
                    timeout = 120L,
                    isInstantVerificationEnabled = true
                ),
                AuthProvider.Facebook(
                    applicationId = "792556260059222"
                )
            ),
            tosUrl = "https://policies.google.com/terms?hl=en-NG&fg=1",
            privacyPolicyUrl = "https://policies.google.com/privacy?hl=en-NG&fg=1"
        )

        // Create AuthFlowController
        authController = authUI.createAuthFlow(configuration)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthFlowDemo(
                        authController = authController,
                        onStartAuth = { startAuthFlow() },
                        onCancelAuth = { cancelAuthFlow() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        authController.dispose()
    }

    private fun startAuthFlow() {
        val intent = authController.createIntent(this)
        authLauncher.launch(intent)
    }

    private fun cancelAuthFlow() {
        authController.cancel()
        Toast.makeText(this, "Auth flow cancelled", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val USE_AUTH_EMULATOR = true
        private const val AUTH_EMULATOR_HOST = "10.0.2.2"
        private const val AUTH_EMULATOR_PORT = 9099

        fun createIntent(context: Context): Intent {
            return Intent(context, AuthFlowControllerDemoActivity::class.java)
        }
    }
}

@Composable
fun AuthFlowDemo(
    authController: AuthFlowController,
    onStartAuth: () -> Unit,
    onCancelAuth: () -> Unit
) {
    val authState by authController.authStateFlow.collectAsState(AuthState.Idle)
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    // Observe Firebase auth state changes
    DisposableEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)

        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "⚙️ Low-Level API Demo",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Text(
            text = "AuthFlowController with ActivityResultLauncher",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "This demonstrates manual control over the authentication flow with lifecycle-safe management.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Current Auth State Card
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
                Text(
                    text = "Current State:",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = when (authState) {
                        is AuthState.Idle -> "Idle"
                        is AuthState.Loading -> "Loading: ${(authState as AuthState.Loading).message}"
                        is AuthState.Success -> "Success - User: ${(authState as AuthState.Success).user.email}"
                        is AuthState.Error -> "Error: ${(authState as AuthState.Error).exception.message}"
                        is AuthState.Cancelled -> "Cancelled"
                        is AuthState.RequiresMfa -> "MFA Required"
                        is AuthState.RequiresEmailVerification -> "Email Verification Required"
                        else -> "Unknown"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (authState) {
                        is AuthState.Success -> MaterialTheme.colorScheme.primary
                        is AuthState.Error -> MaterialTheme.colorScheme.error
                        is AuthState.Loading -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // Current User Card
        currentUser?.let { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Signed In User:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "Email: ${user.email ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "UID: ${user.uid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        if (currentUser == null) {
            Button(
                onClick = onStartAuth,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Auth Flow")
            }

            if (authState is AuthState.Loading) {
                OutlinedButton(
                    onClick = onCancelAuth,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Auth Flow")
                }
            }
        } else {
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
            }
        }

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Features:",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "• Lifecycle-safe auth flow management",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Observable auth state with Flow",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Modern ActivityResultLauncher API",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• Automatic resource cleanup",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
