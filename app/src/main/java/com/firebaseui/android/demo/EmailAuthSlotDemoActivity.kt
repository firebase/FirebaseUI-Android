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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.PasswordRule
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.configuration.theme.AuthUITheme
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebase.ui.auth.ui.screens.email.EmailAuthMode
import com.firebase.ui.auth.ui.screens.email.EmailAuthScreen
import com.google.firebase.auth.AuthResult

class EmailAuthSlotDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authUI = FirebaseAuthUI.getInstance()
        val appContext = applicationContext

        val configuration = authUIConfiguration {
            context = appContext
            providers {
                provider(
                    AuthProvider.Email(
                        isDisplayNameRequired = true,
                        isNewAccountsAllowed = true,
                        isEmailLinkSignInEnabled = false,
                        emailLinkActionCodeSettings = null,
                        isEmailLinkForceSameDeviceEnabled = false,
                        minimumPasswordLength = 8,
                        passwordValidationRules = listOf(
                            PasswordRule.MinimumLength(8),
                            PasswordRule.RequireLowercase,
                            PasswordRule.RequireUppercase,
                            PasswordRule.RequireDigit
                        )
                    )
                )
            }
            tosUrl = "https://policies.google.com/terms"
            privacyPolicyUrl = "https://policies.google.com/privacy"
        }

        setContent {
            CustomAuthUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        EmailAuthDemo(
                            authUI = authUI,
                            configuration = configuration,
                            context = appContext
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomAuthUITheme(content: @Composable () -> Unit) {
    MaterialTheme {
        val authTheme = AuthUITheme.fromMaterialTheme(
            providerButtonShape = RoundedCornerShape(12.dp)
        )
        AuthUITheme(theme = authTheme) {
            content()
        }
    }
}

@Composable
fun EmailAuthDemo(
    authUI: FirebaseAuthUI,
    configuration: AuthUIConfiguration,
    context: android.content.Context
) {
    var currentUser by remember { mutableStateOf(authUI.getCurrentUser()) }

    LaunchedEffect(Unit) {
        authUI.authStateFlow().collect { _ ->
            currentUser = authUI.getCurrentUser()
        }
    }

    if (currentUser != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Successfully Authenticated!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentUser?.email ?: "Signed in",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { authUI.auth.signOut() }) {
                Text("Sign Out")
            }
        }
    } else {
        CompositionLocalProvider(LocalAuthUIStringProvider provides configuration.stringProvider) {
            EmailAuthScreen(
                context = context,
                configuration = configuration,
                authUI = authUI,
                onSuccess = { result: AuthResult ->
                    Log.d("EmailAuthSlotDemo", "Auth success: ${result.user?.uid}")
                },
                onError = { exception: AuthException ->
                    Log.e("EmailAuthSlotDemo", "Auth error", exception)
                },
                onCancel = {
                    Log.d("EmailAuthSlotDemo", "Auth cancelled")
                }
            ) { state: EmailAuthContentState ->
                CustomEmailAuthUI(state)
            }
        }
    }
}

@Composable
fun CustomEmailAuthUI(state: EmailAuthContentState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when (state.mode) {
                EmailAuthMode.SignIn, EmailAuthMode.EmailLinkSignIn -> "Welcome Back"
                EmailAuthMode.SignUp -> "Create Account"
                EmailAuthMode.ResetPassword -> "Reset Password"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        state.error?.let { errorMessage ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        when (state.mode) {
            EmailAuthMode.SignIn, EmailAuthMode.EmailLinkSignIn -> SignInUI(state)
            EmailAuthMode.SignUp -> SignUpUI(state)
            EmailAuthMode.ResetPassword -> ResetPasswordUI(state)
        }
    }
}

@Composable
fun SignInUI(state: EmailAuthContentState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.email,
            onValueChange = state.onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = state.onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        if (state.emailSignInLinkSent) {
            Text(
                text = "Sign-in link sent! Check your email.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = state.onSignInClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign In")
            }
        }

        TextButton(
            onClick = state.onGoToResetPassword,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Forgot Password?")
        }

        HorizontalDivider()

        TextButton(
            onClick = state.onGoToSignUp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Don't have an account? Sign Up")
        }
    }
}

@Composable
fun SignUpUI(state: EmailAuthContentState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.displayName,
            onValueChange = state.onDisplayNameChange,
            label = { Text("Display Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = state.onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = state.onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = state.onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = state.onSignUpClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create Account")
            }
        }

        HorizontalDivider()

        TextButton(
            onClick = state.onGoToSignIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Already have an account? Sign In")
        }
    }
}

@Composable
fun ResetPasswordUI(state: EmailAuthContentState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Enter your email address and we'll send you a link to reset your password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = state.onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        if (state.resetLinkSent) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Password reset link sent! Check your email.",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = state.onSendResetLinkClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && !state.resetLinkSent
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Send Reset Link")
            }
        }

        HorizontalDivider()

        TextButton(
            onClick = state.onGoToSignIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Sign In")
        }
    }
}
