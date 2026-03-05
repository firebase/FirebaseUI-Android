# FirebaseUI for Auth

FirebaseUI Auth is a modern, Compose-based authentication library that provides drop-in UI components for Firebase Authentication. It eliminates boilerplate code and promotes best practices for user authentication on Android.

Built entirely with **Jetpack Compose** and **Material Design 3**, FirebaseUI Auth offers:

- **Simple API** - Choose between high-level screens or low-level controllers for maximum flexibility
- **12+ Authentication Methods** - Email/Password, Phone, Google, Facebook, Twitter, GitHub, Microsoft, Yahoo, Apple, Anonymous, and custom OAuth providers
- **Multi-Factor Authentication** - SMS and TOTP (Time-based One-Time Password) with recovery codes
- **Android Credential Manager** - Automatic credential saving and one-tap sign-in
- **Material Design 3** - Beautiful, themeable UI components that integrate seamlessly with your app
- **Localization Support** - Customizable strings for internationalization
- **Security Best Practices** - Email verification, reauthentication, account linking, and more

Equivalent FirebaseUI libraries are available for [iOS](https://github.com/firebase/firebaseui-ios/) and [Web](https://github.com/firebase/firebaseui-web/).

## Table of Contents

1. [Demo](#demo)
1. [Setup](#setup)
   1. [Prerequisites](#prerequisites)
   1. [Installation](#installation)
   1. [Provider Configuration](#provider-configuration)
1. [Quick Start](#quick-start)
   1. [Minimal Example](#minimal-example)
   1. [Check Authentication State](#check-authentication-state)
1. [Core Concepts](#core-concepts)
   1. [FirebaseAuthUI](#firebaseauthui)
   1. [AuthUIConfiguration](#authuiconfiguration)
   1. [AuthFlowController](#authflowcontroller)
   1. [AuthState](#authstate)
1. [Authentication Methods](#authentication-methods)
   1. [Email & Password](#email--password)
   1. [Phone Number](#phone-number)
   1. [Google Sign-In](#google-sign-in)
   1. [Facebook Login](#facebook-login)
   1. [Other OAuth Providers](#other-oauth-providers)
   1. [Anonymous Authentication](#anonymous-authentication)
   1. [Custom OAuth Provider](#custom-oauth-provider)
1. [Usage Patterns](#usage-patterns)
   1. [High-Level API (Recommended)](#high-level-api-recommended)
   1. [Low-Level API (Advanced)](#low-level-api-advanced)
   1. [Custom UI with Slots](#custom-ui-with-slots)
1. [Multi-Factor Authentication](#multi-factor-authentication)
   1. [MFA Configuration](#mfa-configuration)
   1. [MFA Enrollment](#mfa-enrollment)
   1. [MFA Challenge](#mfa-challenge)
1. [Theming & Customization](#theming--customization)
   1. [Material Theme Integration](#material-theme-integration)
   1. [Custom Theme](#custom-theme)
   1. [Provider Button Styling](#provider-button-styling)
1. [Advanced Features](#advanced-features)
   1. [Anonymous User Upgrade](#anonymous-user-upgrade)
   1. [Email Link Sign-In](#email-link-sign-in)
   1. [Password Validation Rules](#password-validation-rules)
   1. [Credential Manager Integration](#credential-manager-integration)
   1. [Sign Out & Account Deletion](#sign-out--account-deletion)
1. [Localization](#localization)
1. [Error Handling](#error-handling)
1. [Migration Guide](#migration-guide)

## Demo

<img src="demo.gif" alt="FirebaseUI Compose Demo" width="300" />

## Setup

### Prerequisites

Ensure your application is configured for use with Firebase. See the [Firebase documentation](https://firebase.google.com/docs/android/setup) for setup instructions.

**Minimum Requirements:**
- Android SDK 21+ (Android 5.0 Lollipop)
- Kotlin 1.9+
- Jetpack Compose (Compiler 1.5+)
- Firebase Auth 22.0.0+

### Installation

Add the FirebaseUI Auth library dependency to your `build.gradle.kts` (Module):

```kotlin
dependencies {
    // FirebaseUI for Auth
    implementation("com.firebaseui:firebase-ui-auth:10.0.0-beta01")

    // Required: Firebase Auth
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")

    // Required: Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // Optional: Facebook Login (if using FacebookAuthProvider)
    implementation("com.facebook.android:facebook-login:16.3.0")
}
```

**Localization Support:**

To optimize APK size, configure resource filtering for only the languages your app supports:

```kotlin
android {
    defaultConfig {
        resourceConfigurations += listOf("en", "es", "fr") // Add your supported languages
    }
}
```

### Provider Configuration

#### Google Sign-In

Google Sign-In configuration is automatically provided by the [google-services Gradle plugin](https://developers.google.com/android/guides/google-services-plugin). Ensure you have enabled Google Sign-In in the [Firebase Console](https://console.firebase.google.com/project/_/authentication/providers).

#### Facebook Login

If using Facebook Login, add your Facebook App ID to `strings.xml`:

```xml
<resources>
    <string name="facebook_application_id" translatable="false">YOUR_FACEBOOK_APP_ID</string>
    <string name="facebook_login_protocol_scheme" translatable="false">fbYOUR_FACEBOOK_APP_ID</string>
</resources>
```

See the [Facebook for Developers](https://developers.facebook.com/) documentation for setup instructions.

#### Other Providers

Twitter, GitHub, Microsoft, Yahoo, and Apple providers require configuration in the Firebase Console but no additional Android-specific setup. See the [Firebase Auth documentation](https://firebase.google.com/docs/auth) for provider-specific instructions.

## Quick Start

### Minimal Example

Here's the simplest way to add authentication to your app with Email and Google Sign-In:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyAppTheme {
                val configuration = authUIConfiguration {
                    providers = listOf(
                        AuthProvider.Email(),
                        AuthProvider.Google()
                    )
                }

                FirebaseAuthScreen(
                    configuration = configuration,
                    onSignInSuccess = { result ->
                        Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
                        // Navigate to main app screen
                    },
                    onSignInFailure = { exception ->
                        Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                    },
                    onSignInCancelled = {
                        finish()
                    }
                )
            }
        }
    }
}
```

That's it! This provides a complete authentication flow with:
- ✅ Email/password sign-in and sign-up
- ✅ Google Sign-In
- ✅ Password reset
- ✅ Display name collection
- ✅ Credential Manager integration
- ✅ Material Design 3 theming
- ✅ Error handling

### Check Authentication State

Before showing the authentication UI, check if a user is already signed in:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authUI = FirebaseAuthUI.getInstance()

        if (authUI.isSignedIn()) {
            // User is already signed in, navigate to main app
            startActivity(Intent(this, MainAppActivity::class.java))
            finish()
        } else {
            // Show authentication UI
            setContent {
                FirebaseAuthScreen(/* ... */)
            }
        }
    }
}
```

Or observe authentication state changes reactively:

```kotlin
@Composable
fun AuthGate() {
    val authUI = remember { FirebaseAuthUI.getInstance() }
    val authState by authUI.authStateFlow().collectAsState(initial = AuthState.Idle)

    when {
        authState is AuthState.Success -> {
            // User is signed in
            MainAppScreen()
        }
        else -> {
            // Show authentication
            FirebaseAuthScreen(/* ... */)
        }
    }
}
```

## Core Concepts

### FirebaseAuthUI

`FirebaseAuthUI` is the central class that coordinates all authentication operations. It manages UI state and provides methods for signing in, signing up, and managing user accounts.

```kotlin
// Get the default instance
val authUI = FirebaseAuthUI.getInstance()

// Or get an instance for a specific Firebase app
val customApp = Firebase.app("secondary")
val authUI = FirebaseAuthUI.getInstance(customApp)

// Or create with custom auth (for multi-tenancy)
val customAuth = Firebase.auth(customApp)
val authUI = FirebaseAuthUI.create(auth = customAuth)
```

**Key Methods:**

| Method | Return Type | Description |
|--------|-------------|-------------|
| `isSignedIn()` | `Boolean` | Checks if a user is currently signed in |
| `getCurrentUser()` | `FirebaseUser?` | Returns the current user, if signed in |
| `authStateFlow()` | `Flow<AuthState>` | Observes authentication state changes |
| `createAuthFlow(config)` | `AuthFlowController` | Creates a sign-in flow controller |
| `signOut(context)` | `suspend fun` | Signs out the current user |
| `delete(context)` | `suspend fun` | Deletes the current user account |

### AuthUIConfiguration

`AuthUIConfiguration` defines all settings for your authentication flow. Use the DSL builder function for easy configuration:

```kotlin
val configuration = authUIConfiguration {
    // Required: List of authentication providers
    providers = listOf(
        AuthProvider.Email(),
        AuthProvider.Google(),
        AuthProvider.Phone()
    )

    // Optional: Theme configuration
    theme = AuthUITheme.fromMaterialTheme()

    // Optional: Terms of Service and Privacy Policy URLs
    tosUrl = "https://example.com/terms"
    privacyPolicyUrl = "https://example.com/privacy"

    // Optional: App logo
    logo = Icons.Default.AccountCircle

    // Optional: Enable MFA (default: true)
    isMfaEnabled = true

    // Optional: Enable Credential Manager (default: true)
    isCredentialManagerEnabled = true

    // Optional: Allow anonymous user upgrade (default: false)
    isAnonymousUpgradeEnabled = true

    // Optional: Require display name on sign-up (default: true)
    isDisplayNameRequired = true

    // Optional: Allow new email accounts (default: true)
    isNewEmailAccountsAllowed = true

    // Optional: Always show provider choice even with one provider (default: false)
    isProviderChoiceAlwaysShown = false

    // Optional: Custom string provider for localization
    stringProvider = MyCustomStringProvider()

    // Optional: Locale override
    locale = Locale.FRENCH
}
```

### AuthFlowController

`AuthFlowController` manages the lifecycle of an authentication flow programmatically. This is the low-level API for advanced use cases.

```kotlin
val controller = authUI.createAuthFlow(configuration)

lifecycleScope.launch {
    // Start the flow
    val state = controller.start()

    when (state) {
        is AuthState.Success -> {
            // Handle success
            val user = state.result.user
        }
        is AuthState.Error -> {
            // Handle error
            Log.e(TAG, "Auth failed", state.exception)
        }
        is AuthState.Cancelled -> {
            // User cancelled
        }
        else -> {
            // Handle other states (RequiresMfa, RequiresEmailVerification, etc.)
        }
    }
}

// Cancel the flow if needed
controller.cancel()

// Clean up when done
override fun onDestroy() {
    super.onDestroy()
    controller.dispose()
}
```

### AuthState

`AuthState` represents the current state of authentication:

```kotlin
sealed class AuthState {
    object Idle : AuthState()
    data class Loading(val message: String?) : AuthState()
    data class Success(val result: AuthResult, val isNewUser: Boolean) : AuthState()
    data class Error(val exception: AuthException, val isRecoverable: Boolean) : AuthState()
    data class RequiresMfa(val resolver: MultiFactorResolver) : AuthState()
    data class RequiresEmailVerification(val user: FirebaseUser) : AuthState()
    data class RequiresProfileCompletion(val user: FirebaseUser) : AuthState()
    object Cancelled : AuthState()
}
```

## Authentication Methods

### Email & Password

Configure email/password authentication with optional customization:

```kotlin
val emailProvider = AuthProvider.Email(
    // Optional: Require display name (default: true)
    isDisplayNameRequired = true,

    // Optional: Enable email link sign-in (default: false)
    isEmailLinkSignInEnabled = true,

    // Optional: Force email link on same device (default: true)
    isEmailLinkForceSameDeviceEnabled = true,

    // Optional: Action code settings for email link
    emailLinkActionCodeSettings = actionCodeSettings {
        url = "https://example.com/auth"
        handleCodeInApp = true
        setAndroidPackageName(packageName, true, null)
    },

    // Optional: Allow new accounts (default: true)
    isNewAccountsAllowed = true,

    // Optional: Minimum password length (default: 6)
    minimumPasswordLength = 8,

    // Optional: Custom password validation rules
    passwordValidationRules = listOf(
        PasswordRule.MinimumLength(8),
        PasswordRule.RequireUppercase,
        PasswordRule.RequireLowercase,
        PasswordRule.RequireDigit,
        PasswordRule.RequireSpecialCharacter
    )
)

val configuration = authUIConfiguration {
    providers = listOf(emailProvider)
}
```

### Phone Number

Configure phone number authentication with SMS verification:

```kotlin
val phoneProvider = AuthProvider.Phone(
    // Optional: Default phone number in international format
    defaultNumber = "+15551234567",

    // Optional: Default country code (ISO alpha-2 format)
    defaultCountryCode = "US",

    // Optional: Allowed countries
    allowedCountries = listOf("US", "CA", "GB"),

    // Optional: SMS code length (default: 6)
    smsCodeLength = 6,

    // Optional: Timeout for SMS delivery in seconds (default: 60)
    timeout = 60L,

    // Optional: Enable instant verification (default: true)
    isInstantVerificationEnabled = true
)

val configuration = authUIConfiguration {
    providers = listOf(phoneProvider)
}
```

### Google Sign-In

Configure Google Sign-In with optional scopes and server client ID:

```kotlin
val googleProvider = AuthProvider.Google(
    // Required: Scopes to request
    scopes = listOf("https://www.googleapis.com/auth/drive.file"),

    // Optional: Server client ID for backend authentication
    serverClientId = "YOUR_SERVER_CLIENT_ID.apps.googleusercontent.com",

    // Optional: Custom OAuth parameters
    customParameters = mapOf("prompt" to "select_account")
)

val configuration = authUIConfiguration {
    providers = listOf(googleProvider)
}
```

### Facebook Login

Configure Facebook Login with optional permissions:

```kotlin
val facebookProvider = AuthProvider.Facebook(
    // Optional: Facebook application ID (reads from strings.xml if not provided)
    applicationId = "YOUR_FACEBOOK_APP_ID",

    // Optional: Permissions to request (default: ["email", "public_profile"])
    scopes = listOf("email", "public_profile", "user_friends"),

    // Optional: Custom OAuth parameters
    customParameters = mapOf("display" to "popup")
)

val configuration = authUIConfiguration {
    providers = listOf(facebookProvider)
}
```

### Other OAuth Providers

FirebaseUI supports Twitter, GitHub, Microsoft, Yahoo, and Apple:

```kotlin
// Twitter
val twitterProvider = AuthProvider.Twitter(
    // Required: Custom OAuth parameters
    customParameters = mapOf("lang" to "en")
)

// GitHub
val githubProvider = AuthProvider.Github(
    // Optional: Scopes to request (default: ["user:email"])
    scopes = listOf("user:email", "read:user"),

    // Required: Custom OAuth parameters
    customParameters = mapOf("allow_signup" to "false")
)

// Microsoft
val microsoftProvider = AuthProvider.Microsoft(
    // Optional: Scopes to request (default: ["openid", "profile", "email"])
    scopes = listOf("openid", "profile", "email", "User.Read"),

    // Optional: Tenant ID for Azure Active Directory
    tenant = "YOUR_TENANT_ID",

    // Required: Custom OAuth parameters
    customParameters = mapOf("prompt" to "consent")
)

// Yahoo
val yahooProvider = AuthProvider.Yahoo(
    // Optional: Scopes to request (default: ["openid", "profile", "email"])
    scopes = listOf("openid", "profile", "email"),

    // Required: Custom OAuth parameters
    customParameters = mapOf("language" to "en-us")
)

// Apple
val appleProvider = AuthProvider.Apple(
    // Optional: Scopes to request (default: ["name", "email"])
    scopes = listOf("name", "email"),

    // Optional: Locale for the sign-in page
    locale = "en_US",

    // Required: Custom OAuth parameters
    customParameters = mapOf("ui_locales" to "en-US")
)

val configuration = authUIConfiguration {
    providers = listOf(
        twitterProvider,
        githubProvider,
        microsoftProvider,
        yahooProvider,
        appleProvider
    )
}
```

### Anonymous Authentication

Enable anonymous authentication to let users use your app without signing in:

```kotlin
val configuration = authUIConfiguration {
    providers = listOf(
        AuthProvider.Anonymous()
    )

    // Enable anonymous user upgrade
    isAnonymousUpgradeEnabled = true
}
```

### Custom OAuth Provider

Support any OAuth provider configured in the Firebase Console:

```kotlin
val lineProvider = AuthProvider.GenericOAuth(
    // Required: Provider name
    providerName = "LINE",

    // Required: Provider ID as configured in Firebase Console
    providerId = "oidc.line",

    // Required: Scopes to request
    scopes = listOf("profile", "openid", "email"),

    // Required: Custom OAuth parameters
    customParameters = mapOf("prompt" to "consent"),

    // Required: Button label
    buttonLabel = "Sign in with LINE",

    // Optional: Custom button icon
    buttonIcon = AuthUIAsset.Resource(R.drawable.ic_line),

    // Optional: Custom button background color
    buttonColor = Color(0xFF06C755),

    // Optional: Custom button content color
    contentColor = Color.White
)

val configuration = authUIConfiguration {
    providers = listOf(lineProvider)
}
```

## Usage Patterns

### High-Level API (Recommended)

The high-level API provides a complete, opinionated authentication experience with minimal code:

```kotlin
@Composable
fun AuthenticationScreen() {
    val configuration = authUIConfiguration {
        providers = listOf(
            AuthProvider.Email(),
            AuthProvider.Google(),
            AuthProvider.Facebook(),
            AuthProvider.Phone()
        )
        tosUrl = "https://example.com/terms"
        privacyPolicyUrl = "https://example.com/privacy"
        logo = Icons.Default.Lock
    }

    FirebaseAuthScreen(
        configuration = configuration,
        onSignInSuccess = { result ->
            val user = result.user
            val isNewUser = result.additionalUserInfo?.isNewUser ?: false

            if (isNewUser) {
                // First-time user
                navigateToOnboarding()
            } else {
                // Returning user
                navigateToHome()
            }
        },
        onSignInFailure = { exception ->
            when (exception) {
                is AuthException.NetworkException -> {
                    showSnackbar("No internet connection")
                }
                is AuthException.TooManyRequestsException -> {
                    showSnackbar("Too many attempts. Please try again later.")
                }
                else -> {
                    showSnackbar("Authentication failed: ${exception.message}")
                }
            }
        },
        onSignInCancelled = {
            navigateBack()
        }
    )
}
```

### Low-Level API (Advanced)

For maximum control, use the `AuthFlowController`:

```kotlin
class AuthActivity : ComponentActivity() {
    private lateinit var controller: AuthFlowController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authUI = FirebaseAuthUI.getInstance()
        val configuration = authUIConfiguration {
            providers = listOf(AuthProvider.Email(), AuthProvider.Google())
        }

        controller = authUI.createAuthFlow(configuration)

        lifecycleScope.launch {
            val state = controller.start()
            handleAuthState(state)
        }
    }

    private fun handleAuthState(state: AuthState) {
        when (state) {
            is AuthState.Success -> {
                // Successfully signed in
                val user = state.result.user
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            is AuthState.Error -> {
                // Handle error
                AlertDialog.Builder(this)
                    .setTitle("Authentication Failed")
                    .setMessage(state.exception.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
            is AuthState.RequiresMfa -> {
                // User needs to complete MFA challenge
                showMfaChallengeDialog(state.resolver)
            }
            is AuthState.RequiresEmailVerification -> {
                // Email verification needed
                showEmailVerificationScreen(state.user)
            }
            is AuthState.Cancelled -> {
                // User cancelled authentication
                finish()
            }
            else -> {
                // Handle other states
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.dispose()
    }
}
```

### Custom UI with Slots

For complete UI control while keeping authentication logic, use content slots:

```kotlin
@Composable
fun CustomEmailAuth() {
    val emailConfig = AuthProvider.Email(
        passwordValidationRules = listOf(
            PasswordRule.MinimumLength(8),
            PasswordRule.RequireDigit
        )
    )

    EmailAuthScreen(
        configuration = emailConfig,
        onSuccess = { /* ... */ },
        onError = { /* ... */ },
        onCancel = { /* ... */ }
    ) { state ->
        // Custom UI with full control
        when (state.mode) {
            EmailAuthMode.SignIn -> {
                CustomSignInUI(state)
            }
            EmailAuthMode.SignUp -> {
                CustomSignUpUI(state)
            }
            EmailAuthMode.ResetPassword -> {
                CustomResetPasswordUI(state)
            }
        }
    }
}

@Composable
fun CustomSignInUI(state: EmailAuthContentState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome Back!",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = state.onEmailChange,
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = state.onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = state.onSignInClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Sign In")
            }
        }

        TextButton(onClick = state.onGoToResetPassword) {
            Text("Forgot Password?")
        }

        TextButton(onClick = state.onGoToSignUp) {
            Text("Create Account")
        }
    }
}
```

Similarly, create custom phone authentication UI:

```kotlin
@Composable
fun CustomPhoneAuth() {
    val phoneConfig = AuthProvider.Phone(defaultCountryCode = "US")

    PhoneAuthScreen(
        configuration = phoneConfig,
        onSuccess = { /* ... */ },
        onError = { /* ... */ },
        onCancel = { /* ... */ }
    ) { state ->
        when (state.step) {
            PhoneAuthStep.EnterPhoneNumber -> {
                CustomPhoneNumberInput(state)
            }
            PhoneAuthStep.EnterVerificationCode -> {
                CustomVerificationCodeInput(state)
            }
        }
    }
}
```

## Multi-Factor Authentication

### MFA Configuration

Enable and configure Multi-Factor Authentication:

```kotlin
val mfaConfig = MfaConfiguration(
    // Allowed MFA factors (default: [Sms, Totp])
    allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp),

    // Optional: Require MFA enrollment (default: false)
    requireEnrollment = false,

    // Optional: Enable recovery codes (default: true)
    enableRecoveryCodes = true
)

val configuration = authUIConfiguration {
    providers = listOf(AuthProvider.Email())
    isMfaEnabled = true
}
```

### MFA Enrollment

Prompt users to enroll in MFA after sign-in:

```kotlin
@Composable
fun MfaEnrollmentFlow() {
    val currentUser = FirebaseAuth.getInstance().currentUser

    if (currentUser != null) {
        val mfaConfig = MfaConfiguration(
            allowedFactors = listOf(MfaFactor.Sms, MfaFactor.Totp)
        )

        MfaEnrollmentScreen(
            user = currentUser,
            configuration = mfaConfig,
            onEnrollmentComplete = {
                Toast.makeText(context, "MFA enrolled successfully!", Toast.LENGTH_SHORT).show()
                navigateToHome()
            },
            onSkip = {
                navigateToHome()
            }
        )
    }
}
```

Or with custom UI:

```kotlin
MfaEnrollmentScreen(
    user = currentUser,
    configuration = mfaConfig,
    onEnrollmentComplete = { /* ... */ },
    onSkip = { /* ... */ }
) { state ->
    when (state.step) {
        MfaEnrollmentStep.SelectFactor -> {
            CustomFactorSelectionUI(state)
        }
        MfaEnrollmentStep.ConfigureSms -> {
            CustomSmsConfigurationUI(state)
        }
        MfaEnrollmentStep.ConfigureTotp -> {
            CustomTotpConfigurationUI(state)
        }
        MfaEnrollmentStep.VerifyFactor -> {
            CustomVerificationUI(state)
        }
        MfaEnrollmentStep.ShowRecoveryCodes -> {
            CustomRecoveryCodesUI(state)
        }
    }
}
```

### MFA Challenge

Handle MFA challenges during sign-in. The challenge is automatically detected:

```kotlin
FirebaseAuthScreen(
    configuration = configuration,
    onSignInSuccess = { result ->
        navigateToHome()
    },
    onSignInFailure = { exception ->
        // MFA challenges are handled automatically by FirebaseAuthScreen
        // But you can also handle them manually:
        if (exception is AuthException.MfaRequiredException) {
            showMfaChallengeScreen(exception.resolver)
        }
    }
)
```

Or handle manually:

```kotlin
@Composable
fun ManualMfaChallenge(resolver: MultiFactorResolver) {
    MfaChallengeScreen(
        resolver = resolver,
        onChallengeComplete = { assertion ->
            // Complete sign-in with the assertion
            lifecycleScope.launch {
                try {
                    val result = resolver.resolveSignIn(assertion)
                    navigateToHome()
                } catch (e: Exception) {
                    showError(e)
                }
            }
        },
        onCancel = {
            navigateBack()
        }
    )
}
```

## Theming & Customization

### Material Theme Integration

FirebaseUI automatically inherits your app's Material Theme:

```kotlin
@Composable
fun App() {
    MyAppTheme {  // Your existing Material3 theme
        val configuration = authUIConfiguration {
            providers = listOf(AuthProvider.Email())
            theme = AuthUITheme.fromMaterialTheme()  // Inherits MyAppTheme
        }

        FirebaseAuthScreen(
            configuration = configuration,
            onSignInSuccess = { /* ... */ }
        )
    }
}
```

### Custom Theme

Create a completely custom theme:

```kotlin
val customTheme = AuthUITheme(
    colorScheme = darkColorScheme(
        primary = Color(0xFF6200EE),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF3700B3),
        secondary = Color(0xFF03DAC6)
    ),
    typography = Typography(
        displayLarge = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Bold),
        bodyLarge = TextStyle(fontSize = 16.sp)
    ),
    shapes = Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(16.dp)
    )
)

val configuration = authUIConfiguration {
    providers = listOf(AuthProvider.Email())
    theme = customTheme
}
```

### Provider Button Styling

Customize individual provider button styling:

```kotlin
val customProviderStyles = mapOf(
    "google.com" to AuthUITheme.ProviderStyle(
        backgroundColor = Color.White,
        contentColor = Color(0xFF757575),
        iconTint = null,  // Use original colors
        shape = RoundedCornerShape(8.dp),
        elevation = 4.dp
    ),
    "facebook.com" to AuthUITheme.ProviderStyle(
        backgroundColor = Color(0xFF1877F2),
        contentColor = Color.White,
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp
    )
)

val customTheme = AuthUITheme.Default.copy(
    providerStyles = customProviderStyles
)

val configuration = authUIConfiguration {
    providers = listOf(AuthProvider.Google(), AuthProvider.Facebook())
    theme = customTheme
}
```

## Advanced Features

### Anonymous User Upgrade

Seamlessly upgrade anonymous users to permanent accounts:

```kotlin
// 1. Configure anonymous authentication with upgrade enabled
val configuration = authUIConfiguration {
    providers = listOf(
        AuthProvider.Anonymous(),
        AuthProvider.Email(),
        AuthProvider.Google()
    )
    isAnonymousUpgradeEnabled = true
}

// 2. When user wants to create a permanent account, show auth UI
// The library automatically upgrades the anonymous account if one exists
FirebaseAuthScreen(
    configuration = configuration,
    onSignInSuccess = { result ->
        // Anonymous account has been upgraded (if user was anonymous)!
        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
    }
)
```

### Email Link Sign-In

Enable passwordless email link authentication:

```kotlin
val emailProvider = AuthProvider.Email(
    isEmailLinkSignInEnabled = true,
    emailLinkActionCodeSettings = actionCodeSettings {
        url = "https://example.com/auth"
        handleCodeInApp = true
        setAndroidPackageName(packageName, true, "12")
    },
    passwordValidationRules = emptyList()
)

val configuration = authUIConfiguration {
    providers = listOf(emailProvider)
}
```

**High-Level API** - Direct `FirebaseAuthScreen` usage:

```kotlin
// In your Activity that handles the deep link:
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val authUI = FirebaseAuthUI.getInstance()
    val emailLink = if (authUI.canHandleIntent(intent)) {
        intent.data?.toString()
    } else {
        null
    }

    if (emailLink != null) {
        setContent {
            FirebaseAuthScreen(
                configuration = configuration,
                emailLink = emailLink,
                onSignInSuccess = { result ->
                    // Email link sign-in successful
                },
                onSignInFailure = { exception ->
                    // Handle error
                },
                onSignInCancelled = {
                    finish()
                }
            )
        }
    }
}
```

**Low-Level API** - Using `AuthFlowController`:

```kotlin
import com.firebase.ui.auth.util.EmailLinkConstants

// In your Activity that handles the deep link:
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val authUI = FirebaseAuthUI.getInstance()
    val emailLink = if (authUI.canHandleIntent(intent)) {
        intent.data?.toString()
    } else {
        null
    }

    if (emailLink != null) {
        val controller = authUI.createAuthFlow(configuration)
        val intent = controller.createIntent(this).apply {
            putExtra(EmailLinkConstants.EXTRA_EMAIL_LINK, emailLink)
        }
        authLauncher.launch(intent)
    }
}

// Handle result
private val authLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    when (result.resultCode) {
        Activity.RESULT_OK -> {
            // Email link sign-in successful
        }
        Activity.RESULT_CANCELED -> {
            // Handle error or cancellation
        }
    }
}
```

Add the intent filter to your `AndroidManifest.xml`:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data
        android:scheme="https"
        android:host="example.com"
        android:pathPrefix="/auth" />
</intent-filter>
```

### Password Validation Rules

Enforce custom password requirements:

```kotlin
val emailProvider = AuthProvider.Email(
    emailLinkActionCodeSettings = null,
    minimumPasswordLength = 10,
    passwordValidationRules = listOf(
        PasswordRule.MinimumLength(10),
        PasswordRule.RequireUppercase,
        PasswordRule.RequireLowercase,
        PasswordRule.RequireDigit,
        PasswordRule.RequireSpecialCharacter,
        PasswordRule.Custom(
            regex = Regex("^(?!.*password).*$"),
            errorMessage = "Password cannot contain the word 'password'"
        )
    )
)
```

### Credential Manager Integration

FirebaseUI automatically integrates with Android's Credential Manager API to save and retrieve credentials. This enables:

- **Automatic sign-in** for returning users
- **One-tap sign-in** across apps
- **Secure credential storage**

Credential Manager is enabled by default. To disable:

```kotlin
val configuration = authUIConfiguration {
    providers = listOf(AuthProvider.Email())
    isCredentialManagerEnabled = false
}
```

### Sign Out & Account Deletion

**Sign Out:**

```kotlin
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val authUI = remember { FirebaseAuthUI.getInstance() }

    Button(
        onClick = {
            lifecycleScope.launch {
                authUI.signOut(context)
                // User is signed out, navigate to auth screen
                navigateToAuth()
            }
        }
    ) {
        Text("Sign Out")
    }
}
```

**Delete Account:**

```kotlin
Button(
    onClick = {
        lifecycleScope.launch {
            try {
                authUI.delete(context)
                // Account deleted successfully
                navigateToAuth()
            } catch (e: Exception) {
                when (e) {
                    is FirebaseAuthRecentLoginRequiredException -> {
                        // User needs to reauthenticate
                        showReauthenticationDialog()
                    }
                    else -> {
                        showError("Failed to delete account: ${e.message}")
                    }
                }
            }
        }
    }
) {
    Text("Delete Account")
}
```

## Localization

FirebaseUI includes default English strings. To add custom localization:

```kotlin
class SpanishStringProvider(context: Context) : AuthUIStringProvider {
    override fun signInWithEmail() = "Iniciar sesión con correo"
    override fun signInWithGoogle() = "Iniciar sesión con Google"
    override fun signInWithFacebook() = "Iniciar sesión con Facebook"
    override fun invalidEmail() = "Correo inválido"
    override fun weakPassword() = "Contraseña débil"
    // ... implement all other required methods
}

val configuration = authUIConfiguration {
    providers = listOf(AuthProvider.Email())
    stringProvider = SpanishStringProvider(context)
    locale = Locale("es", "ES")
}
```

Or override individual strings in your `strings.xml`:

```xml
<resources>
    <!-- Override FirebaseUI strings -->
    <string name="fui_sign_in_with_google">Sign in with Google</string>
    <string name="fui_sign_in_with_email">Sign in with Email</string>
    <string name="fui_invalid_email_address">Invalid email address</string>
    <!-- See auth/src/main/res/values/strings.xml for all available strings -->
</resources>
```

## Error Handling

FirebaseUI provides a comprehensive exception hierarchy:

```kotlin
FirebaseAuthScreen(
    configuration = configuration,
    onSignInFailure = { exception ->
        when (exception) {
            is AuthException.NetworkException -> {
                showSnackbar("No internet connection. Please check your network.")
            }
            is AuthException.InvalidCredentialsException -> {
                showSnackbar("Invalid email or password.")
            }
            is AuthException.UserNotFoundException -> {
                showSnackbar("No account found with this email.")
            }
            is AuthException.WeakPasswordException -> {
                showSnackbar("Password is too weak. Please use a stronger password.")
            }
            is AuthException.EmailAlreadyInUseException -> {
                showSnackbar("An account already exists with this email.")
            }
            is AuthException.TooManyRequestsException -> {
                showSnackbar("Too many attempts. Please try again later.")
            }
            is AuthException.MfaRequiredException -> {
                // Handled automatically by FirebaseAuthScreen
                // or show custom MFA challenge
            }
            is AuthException.AccountLinkingRequiredException -> {
                // Account needs to be linked
                showAccountLinkingDialog(exception)
            }
            is AuthException.AuthCancelledException -> {
                // User cancelled the flow
                navigateBack()
            }
            is AuthException.UnknownException -> {
                showSnackbar("An unexpected error occurred: ${exception.message}")
                Log.e(TAG, "Auth error", exception)
            }
        }
    }
)
```

Use the `ErrorRecoveryDialog` for automatic error handling:

```kotlin
var errorState by remember { mutableStateOf<AuthException?>(null) }

errorState?.let { error ->
    ErrorRecoveryDialog(
        error = error,
        onRetry = {
            // Retry the authentication
            errorState = null
            retryAuthentication()
        },
        onDismiss = {
            errorState = null
        },
        onRecover = { exception ->
            // Custom recovery logic for specific errors
            when (exception) {
                is AuthException.AccountLinkingRequiredException -> {
                    linkAccounts(exception)
                }
            }
        }
    )
}
```

## Migration Guide

### From FirebaseUI Auth 9.x (View-based)

The new Compose library has a completely different architecture. Here's how to migrate:

**Old (9.x - View/Activity based):**

```java
// Old approach with startActivityForResult
Intent signInIntent = AuthUI.getInstance()
    .createSignInIntentBuilder()
    .setAvailableProviders(Arrays.asList(
        new AuthUI.IdpConfig.EmailBuilder().build(),
        new AuthUI.IdpConfig.GoogleBuilder().build()
    ))
    .setTheme(R.style.AppTheme)
    .build();

signInLauncher.launch(signInIntent);
```

**New (10.x - Compose based):**

```kotlin
// New approach with Composable
val configuration = authUIConfiguration {
    providers = listOf(
        AuthProvider.Email(),
        AuthProvider.Google()
    )
    theme = AuthUITheme.fromMaterialTheme()
}

FirebaseAuthScreen(
    configuration = configuration,
    onSignInSuccess = { result -> /* ... */ },
    onSignInFailure = { exception -> /* ... */ },
    onSignInCancelled = { /* ... */ }
)
```

**Key Changes:**

1. **Pure Compose** - No more Activities or Intents, everything is Composable
2. **Configuration DSL** - Use `authUIConfiguration {}` instead of `createSignInIntentBuilder()`
3. **Provider Builders** - `AuthProvider.Email()` instead of `IdpConfig.EmailBuilder().build()`
4. **Callbacks** - Direct callback parameters instead of `ActivityResultLauncher`
5. **Theming** - `AuthUITheme` instead of `R.style` theme resources
6. **State Management** - Reactive `Flow<AuthState>` instead of `AuthStateListener`

**Migration Checklist:**

- [ ] Update dependency to `firebase-ui-auth:10.0.0-beta01`
- [ ] Convert Activities to Composables
- [ ] Replace Intent-based flow with `FirebaseAuthScreen`
- [ ] Update configuration from builder pattern to DSL
- [ ] Replace theme resources with `AuthUITheme`
- [ ] Update error handling from result codes to `AuthException`
- [ ] Remove `ActivityResultLauncher` and use direct callbacks
- [ ] Update sign-out/delete to use suspend functions

For a complete migration example, see the [migration guide](../docs/upgrade-to-10.0.md).

---

## Contributing

Contributions are welcome! Please read our [contribution guidelines](../CONTRIBUTING.md) before submitting PRs.

## License

FirebaseUI Auth is available under the [Apache 2.0 license](../LICENSE).

## Support

- [Firebase Documentation](https://firebase.google.com/docs/auth)
- [GitHub Issues](https://github.com/firebase/FirebaseUI-Android/issues)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/firebaseui)
