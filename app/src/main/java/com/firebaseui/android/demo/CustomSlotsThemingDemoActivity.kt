package com.firebaseui.android.demo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.firebase.ui.auth.configuration.theme.ProviderStyleDefaults
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.ui.components.AuthProviderButton
import com.firebase.ui.auth.ui.screens.email.EmailAuthContentState
import com.firebase.ui.auth.ui.screens.email.EmailAuthMode
import com.firebase.ui.auth.ui.screens.email.EmailAuthScreen
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthContentState
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthScreen
import com.firebase.ui.auth.ui.screens.phone.PhoneAuthStep
import com.google.firebase.auth.AuthResult

/**
 * Demo activity showcasing custom slots and theming capabilities:
 * - EmailAuthScreen with custom slot UI
 * - PhoneAuthScreen with custom slot UI
 * - Provider button shape customization with global and per-provider overrides
 * - AuthUITheme.fromMaterialTheme() with custom ProviderStyle overrides
 */
class CustomSlotsThemingDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authUI = FirebaseAuthUI.getInstance()
        val appContext = applicationContext

        // Configuration for email authentication
        val emailConfiguration = authUIConfiguration {
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

        // Configuration for phone authentication
        val phoneConfiguration = authUIConfiguration {
            context = appContext
            providers {
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = "US",
                        allowedCountries = emptyList(),
                        smsCodeLength = 6,
                        timeout = 60L,
                        isInstantVerificationEnabled = true
                    )
                )
            }
        }

        setContent {
            // Custom theme using fromMaterialTheme() with custom provider styles
            CustomAuthUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var selectedDemo by remember { mutableStateOf(DemoType.Email) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        // Demo selector tabs
                        DemoSelector(
                            selectedDemo = selectedDemo,
                            onDemoSelected = { selectedDemo = it }
                        )

                        // Show selected demo
                        when (selectedDemo) {
                            DemoType.Email -> EmailAuthDemo(
                                authUI = authUI,
                                configuration = emailConfiguration,
                                context = appContext
                            )
                            DemoType.Phone -> PhoneAuthDemo(
                                authUI = authUI,
                                configuration = phoneConfiguration,
                                context = appContext
                            )
                            DemoType.ShapeCustomization -> ShapeCustomizationDemo()
                        }
                    }
                }
            }
        }
    }
}

enum class DemoType {
    Email,
    Phone,
    ShapeCustomization
}

@Composable
fun CustomAuthUITheme(content: @Composable () -> Unit) {
    // Use Material Theme colors
    MaterialTheme {
        // UPDATED: Now uses ProviderStyleDefaults and the new providerButtonShape API
        // Apply custom theme using fromMaterialTheme with global button shape
        val authTheme = AuthUITheme.fromMaterialTheme(
            providerButtonShape = RoundedCornerShape(12.dp)  // Global shape for all buttons
        )

        AuthUITheme(theme = authTheme) {
            content()
        }
    }
}

@Composable
fun DemoSelector(
    selectedDemo: DemoType,
    onDemoSelected: (DemoType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Custom Slots & Theming Demo",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Select a demo to see custom UI implementations using slot APIs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedDemo == DemoType.Email,
                        onClick = { onDemoSelected(DemoType.Email) },
                        label = { Text("Email Auth") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedDemo == DemoType.Phone,
                        onClick = { onDemoSelected(DemoType.Phone) },
                        label = { Text("Phone Auth") },
                        modifier = Modifier.weight(1f)
                    )
                }
                FilterChip(
                    selected = selectedDemo == DemoType.ShapeCustomization,
                    onClick = { onDemoSelected(DemoType.ShapeCustomization) },
                    label = { Text("Shape Customization") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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

    // Monitor auth state changes
    LaunchedEffect(Unit) {
        authUI.authStateFlow().collect { _ ->
            currentUser = authUI.getCurrentUser()
        }
    }

    if (currentUser != null) {
        // Show success screen
        val successScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(successScrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
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
            Button(onClick = {
                authUI.auth.signOut()
            }) {
                Text("Sign Out")
            }
        }
    } else {
        // Show custom email auth UI using slot API
        // Provide the string provider required by EmailAuthScreen
        CompositionLocalProvider(LocalAuthUIStringProvider provides configuration.stringProvider) {
            EmailAuthScreen(
                context = context,
                configuration = configuration,
                authUI = authUI,
                onSuccess = { result: AuthResult ->
                    Log.d("CustomSlotsDemo", "Email auth success: ${result.user?.uid}")
                },
                onError = { exception: AuthException ->
                    Log.e("CustomSlotsDemo", "Email auth error", exception)
                },
                onCancel = {
                    Log.d("CustomSlotsDemo", "Email auth cancelled")
                }
            ) { state: EmailAuthContentState ->
                // Custom UI using the slot API
                CustomEmailAuthUI(state)
            }
        }
    }
}

@Composable
fun CustomEmailAuthUI(state: EmailAuthContentState) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title based on mode
        Text(
            text = when (state.mode) {
                EmailAuthMode.SignIn, EmailAuthMode.EmailLinkSignIn -> "📧 Welcome Back"
                EmailAuthMode.SignUp -> "📧 Create Account"
                EmailAuthMode.ResetPassword -> "📧 Reset Password"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Error display
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

        // Render UI based on mode
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
                text = "✓ Sign-in link sent! Check your email.",
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
                    text = "✓ Password reset link sent! Check your email.",
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

@Composable
fun PhoneAuthDemo(
    authUI: FirebaseAuthUI,
    configuration: AuthUIConfiguration,
    context: android.content.Context
) {
    var currentUser by remember { mutableStateOf(authUI.getCurrentUser()) }

    // Monitor auth state changes
    LaunchedEffect(Unit) {
        authUI.authStateFlow().collect { _ ->
            currentUser = authUI.getCurrentUser()
        }
    }

    if (currentUser != null) {
        // Show success screen
        val successScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(successScrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📱",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Phone Verified!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentUser?.phoneNumber ?: "Signed in",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = {
                authUI.auth.signOut()
            }) {
                Text("Sign Out")
            }
        }
    } else {
        // Show custom phone auth UI using slot API
        // Provide the string provider required by PhoneAuthScreen
        CompositionLocalProvider(LocalAuthUIStringProvider provides configuration.stringProvider) {
            PhoneAuthScreen(
                context = context,
                configuration = configuration,
                authUI = authUI,
                onSuccess = { result: AuthResult ->
                    Log.d("CustomSlotsDemo", "Phone auth success: ${result.user?.uid}")
                },
                onError = { exception: AuthException ->
                    Log.e("CustomSlotsDemo", "Phone auth error", exception)
                },
                onCancel = {
                    Log.d("CustomSlotsDemo", "Phone auth cancelled")
                }
            ) { state: PhoneAuthContentState ->
                // Custom UI using the slot API
                CustomPhoneAuthUI(state)
            }
        }
    }
}

@Composable
fun CustomPhoneAuthUI(state: PhoneAuthContentState) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title based on step
        Text(
            text = when (state.step) {
                PhoneAuthStep.EnterPhoneNumber -> "📱 Phone Verification"
                PhoneAuthStep.EnterVerificationCode -> "📱 Enter Code"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Error display
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

        // Render UI based on step
        when (state.step) {
            PhoneAuthStep.EnterPhoneNumber -> EnterPhoneNumberUI(state)
            PhoneAuthStep.EnterVerificationCode -> EnterVerificationCodeUI(state)
        }
    }
}

@Composable
fun EnterPhoneNumberUI(state: PhoneAuthContentState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Enter your phone number to receive a verification code",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Country selector (simplified for demo)
        OutlinedCard(
            onClick = { /* In real app, open country selector */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.selectedCountry.flagEmoji} ${state.selectedCountry.dialCode}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = state.selectedCountry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = state.phoneNumber,
            onValueChange = state.onPhoneNumberChange,
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = state.onSendCodeClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.phoneNumber.isNotBlank()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Send Code")
            }
        }
    }
}

@Composable
fun EnterVerificationCodeUI(state: PhoneAuthContentState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "We sent a verification code to:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = state.fullPhoneNumber,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.verificationCode,
            onValueChange = state.onVerificationCodeChange,
            label = { Text("6-Digit Code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = state.onVerifyCodeClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.verificationCode.length == 6
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Verify Code")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = state.onChangeNumberClick) {
                Text("Change Number")
            }

            TextButton(
                onClick = state.onResendCodeClick,
                enabled = state.resendTimer == 0
            ) {
                Text(
                    if (state.resendTimer > 0)
                        "Resend (${state.resendTimer}s)"
                    else
                        "Resend Code"
                )
            }
        }
    }
}

/**
 * Demo showcasing provider button shape customization capabilities.
 * Demonstrates:
 * - Global shape configuration for all buttons
 * - Per-provider shape overrides
 * - Using ProviderStyleDefaults with .copy()
 */
@Composable
fun ShapeCustomizationDemo() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val stringProvider = DefaultAuthUIStringProvider(context)
    var selectedPreset by remember { mutableStateOf(ShapePreset.DEFAULT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title and description
        Text(
            text = "Provider Button Shape Customization",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "This demo showcases the new shape customization API for provider buttons. " +
                    "You can set a global shape for all buttons or customize individual providers.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        // Preset selector
        Text(
            text = "Select Shape Preset:",
            style = MaterialTheme.typography.titleMedium
        )

        ShapePreset.entries.forEach { preset ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedPreset == preset,
                    onClick = { selectedPreset = preset }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = preset.displayName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider()

        // Preview section
        Text(
            text = "Preview:",
            style = MaterialTheme.typography.titleMedium
        )

        // Render buttons with the selected preset
        when (selectedPreset) {
            ShapePreset.DEFAULT -> DefaultShapeButtons(stringProvider)
            ShapePreset.DEFAULT_COPY -> DefaultCopyShapeButtons(stringProvider)
            ShapePreset.DARK_COPY -> DarkCopyShapeButtons(stringProvider)
            ShapePreset.FROM_MATERIAL -> FromMaterialThemeButtons(stringProvider)
            ShapePreset.PILL -> PillShapeButtons(stringProvider)
            ShapePreset.MIXED -> MixedShapeButtons(stringProvider)
        }

        // Code example
        HorizontalDivider()

        Text(
            text = "Code Example:",
            style = MaterialTheme.typography.titleMedium
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = selectedPreset.codeExample,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

enum class ShapePreset(
    val displayName: String,
    val description: String,
    val codeExample: String
) {
    DEFAULT(
        "Default Shapes",
        "Uses the standard 4dp rounded corners",
        """
// No customization needed
val theme = AuthUITheme.Default
        """.trimIndent()
    ),
    DEFAULT_COPY(
        "Default.copy()",
        "Customize default light theme with .copy()",
        """
val theme = AuthUITheme.Default.copy(
    providerButtonShape = RoundedCornerShape(12.dp)
)
        """.trimIndent()
    ),
    DARK_COPY(
        "DefaultDark.copy()",
        "Customize default dark theme with .copy()",
        """
val theme = AuthUITheme.DefaultDark.copy(
    providerButtonShape = RoundedCornerShape(16.dp)
)
        """.trimIndent()
    ),
    FROM_MATERIAL(
        "fromMaterialTheme()",
        "Inherit from Material Theme",
        """
val theme = AuthUITheme.fromMaterialTheme(
    providerButtonShape = RoundedCornerShape(12.dp)
)
        """.trimIndent()
    ),
    PILL(
        "Pill Shape",
        "Creates pill-shaped buttons (Default.copy)",
        """
val theme = AuthUITheme.Default.copy(
    providerButtonShape = RoundedCornerShape(28.dp)
)
        """.trimIndent()
    ),
    MIXED(
        "Mixed Shapes",
        "Different shapes per provider (Default.copy)",
        """
val customStyles = mapOf(
    "google.com" to ProviderStyleDefaults.Google.copy(
        shape = RoundedCornerShape(24.dp)
    ),
    "facebook.com" to ProviderStyleDefaults.Facebook.copy(
        shape = RoundedCornerShape(8.dp)
    )
)

val theme = AuthUITheme.Default.copy(
    providerButtonShape = RoundedCornerShape(12.dp),
    providerStyles = customStyles
)
        """.trimIndent()
    )
}

@Composable
fun DefaultShapeButtons(stringProvider: DefaultAuthUIStringProvider) {
    // Default theme - no customization
    AuthUITheme {
        ButtonPreviewColumn(stringProvider)
    }
}

@Composable
fun DefaultCopyShapeButtons(stringProvider: DefaultAuthUIStringProvider) {
    // Using AuthUITheme.Default.copy() to customize the light theme
    val theme = AuthUITheme.Default.copy(
        providerButtonShape = RoundedCornerShape(12.dp)
    )
    AuthUITheme(theme = theme) {
        ButtonPreviewColumn(stringProvider)
    }
}

@Composable
fun DarkCopyShapeButtons(stringProvider: DefaultAuthUIStringProvider) {
    // Using AuthUITheme.DefaultDark.copy() to customize the dark theme
    val theme = AuthUITheme.DefaultDark.copy(
        providerButtonShape = RoundedCornerShape(16.dp)
    )
    AuthUITheme(theme = theme) {
        ButtonPreviewColumn(stringProvider)
    }
}

@Composable
fun FromMaterialThemeButtons(stringProvider: DefaultAuthUIStringProvider) {
    // Using AuthUITheme.fromMaterialTheme() to inherit from Material Theme
    val theme = AuthUITheme.fromMaterialTheme(
        providerButtonShape = RoundedCornerShape(12.dp)
    )
    AuthUITheme(theme = theme) {
        ButtonPreviewColumn(stringProvider)
    }
}

@Composable
fun PillShapeButtons(stringProvider: DefaultAuthUIStringProvider) {
    // Pill-shaped buttons using Default.copy()
    val theme = AuthUITheme.Default.copy(
        providerButtonShape = RoundedCornerShape(28.dp)
    )
    AuthUITheme(theme = theme) {
        ButtonPreviewColumn(stringProvider)
    }
}

@Composable
fun MixedShapeButtons(stringProvider: DefaultAuthUIStringProvider) {
    // Mixed shapes per provider using Default.copy()
    val customStyles = mapOf(
        "google.com" to ProviderStyleDefaults.Google.copy(
            shape = RoundedCornerShape(24.dp) // Pill shape for Google
        ),
        "facebook.com" to ProviderStyleDefaults.Facebook.copy(
            shape = RoundedCornerShape(8.dp) // Medium rounded for Facebook
        )
        // Email uses global default (12dp)
    )

    val theme = AuthUITheme.Default.copy(
        providerButtonShape = RoundedCornerShape(12.dp),
        providerStyles = customStyles
    )

    AuthUITheme(theme = theme) {
        ButtonPreviewColumn(stringProvider)
    }
}

@Composable
fun ButtonPreviewColumn(stringProvider: DefaultAuthUIStringProvider) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AuthProviderButton(
            provider = AuthProvider.Google(scopes = emptyList(), serverClientId = null),
            onClick = { },
            stringProvider = stringProvider,
            modifier = Modifier.fillMaxWidth()
        )

        AuthProviderButton(
            provider = AuthProvider.Facebook(),
            onClick = { },
            stringProvider = stringProvider,
            modifier = Modifier.fillMaxWidth()
        )

        AuthProviderButton(
            provider = AuthProvider.Email(
                emailLinkActionCodeSettings = null,
                passwordValidationRules = emptyList()
            ),
            onClick = { },
            stringProvider = stringProvider,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
