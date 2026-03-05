# Migration Guide: FirebaseUI Auth 9.x to 10.x

This guide helps you migrate from FirebaseUI Auth 9.x (View-based) to 10.x (Compose-based).
Note that 10.x is a beta release.

## Overview

FirebaseUI Auth 10.x is a complete rewrite built with Jetpack Compose and Material Design 3. The architecture has changed significantly from the View-based system to a modern, declarative UI framework.

## Key Architectural Changes

### 1. UI Framework
- **9.x**: Android Views, Activities, Fragments
- **10.x**: Jetpack Compose, Composables

### 2. Configuration
- **9.x**: Builder pattern (`createSignInIntentBuilder()`)
- **10.x**: Kotlin DSL (`authUIConfiguration {}`)

### 3. Providers
- **9.x**: `IdpConfig.EmailBuilder().build()`
- **10.x**: `AuthProvider.Email()`

### 4. Flow Control
- **9.x**: Activity-based with `startActivityForResult()` and `ActivityResultLauncher`
- **10.x**: Composable screens with direct callbacks OR `AuthFlowController` for Activity-based apps

### 5. Theming
- **9.x**: XML theme resources (`R.style.AppTheme`)
- **10.x**: `AuthUITheme` with Material 3 color schemes

### 6. State Management
- **9.x**: `AuthStateListener` callbacks
- **10.x**: Reactive `Flow<AuthState>`

## Migration Steps

### Step 1: Update Dependencies

**Old (9.x):**
```kotlin
dependencies {
    implementation("com.firebaseui:firebase-ui-auth:9.1.1")
}
```

**New (10.x):**
```kotlin
dependencies {
    // FirebaseUI Auth
    implementation("com.firebaseui:firebase-ui-auth:10.0.0-beta01")

    // Required: Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
```

### Step 2: Migrate to Compose

**Old (9.x) - Activity-based:**
```java
public class SignInActivity extends AppCompatActivity {
    private final ActivityResultLauncher<Intent> signInLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                IdpResponse response = IdpResponse.fromResultIntent(result.getData());

                if (result.getResultCode() == RESULT_OK) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    // User signed in
                } else {
                    // Sign in failed
                }
            }
        );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        Intent signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()
            ))
            .setTheme(R.style.AppTheme)
            .build();

        signInLauncher.launch(signInIntent);
    }
}
```

**New (10.x) - Compose-based:**
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
                    theme = AuthUITheme.fromMaterialTheme()
                }

                FirebaseAuthScreen(
                    configuration = configuration,
                    onSignInSuccess = { result ->
                        val user = result.user
                        // User signed in
                    },
                    onSignInFailure = { exception ->
                        // Sign in failed
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

### Step 3: Update Provider Configuration

**Old (9.x):**
```java
List<AuthUI.IdpConfig> providers = Arrays.asList(
    new AuthUI.IdpConfig.EmailBuilder()
        .setRequireName(true)
        .build(),
    new AuthUI.IdpConfig.GoogleBuilder()
        .build(),
    new AuthUI.IdpConfig.PhoneBuilder()
        .build()
);
```

**New (10.x):**
```kotlin
val configuration = authUIConfiguration {
    providers = listOf(
        AuthProvider.Email(
            isDisplayNameRequired = true
        ),
        AuthProvider.Google(
            scopes = listOf("email"),
            serverClientId = "YOUR_CLIENT_ID"
        ),
        AuthProvider.Phone(
            defaultCountryCode = "US"
        )
    )
}
```

### Step 4: Update Theming

**Old (9.x) - XML styles:**
```xml
<style name="AppTheme" parent="FirebaseUI">
    <item name="colorPrimary">@color/primary</item>
    <item name="colorPrimaryDark">@color/primary_dark</item>
    <item name="colorAccent">@color/accent</item>
</style>
```

```java
.setTheme(R.style.AppTheme)
```

**New (10.x) - Material 3:**
```kotlin
val configuration = authUIConfiguration {
    providers = listOf(AuthProvider.Email())
    theme = AuthUITheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6200EE),
            onPrimary = Color.White,
            secondary = Color(0xFF03DAC6)
        )
    )
}
```

Or inherit from your app theme:
```kotlin
MyAppTheme {
    val configuration = authUIConfiguration {
        providers = listOf(AuthProvider.Email())
        theme = AuthUITheme.fromMaterialTheme()
    }

    FirebaseAuthScreen(configuration = configuration, ...)
}
```

### Step 5: Update Sign Out

**Old (9.x):**
```java
AuthUI.getInstance()
    .signOut(this)
    .addOnCompleteListener(task -> {
        // User signed out
    });
```

**New (10.x):**
```kotlin
lifecycleScope.launch {
    FirebaseAuthUI.getInstance().signOut(context)
    // User signed out
}
```

### Step 6: Update Account Deletion

**Old (9.x):**
```java
AuthUI.getInstance()
    .delete(this)
    .addOnCompleteListener(task -> {
        if (task.isSuccessful()) {
            // Account deleted
        } else {
            // Deletion failed
        }
    });
```

**New (10.x):**
```kotlin
lifecycleScope.launch {
    try {
        FirebaseAuthUI.getInstance().delete(context)
        // Account deleted
    } catch (e: Exception) {
        // Deletion failed
    }
}
```

### Step 7: Auth State Observation

**Old (9.x):**
```java
FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
    FirebaseUser user = firebaseAuth.getCurrentUser();
    if (user != null) {
        // User is signed in
    } else {
        // User is signed out
    }
});
```

**New (10.x):**
```kotlin
@Composable
fun AuthGate() {
    val authUI = remember { FirebaseAuthUI.getInstance() }
    val authState by authUI.authStateFlow().collectAsState(initial = AuthState.Idle)

    when (authState) {
        is AuthState.Success -> {
            // User is signed in
            MainAppScreen()
        }
        else -> {
            // Show authentication
            FirebaseAuthScreen(...)
        }
    }
}
```

## Provider-Specific Migration

### Email Provider

**Old (9.x):**
```java
new AuthUI.IdpConfig.EmailBuilder()
    .setRequireName(true)
    .setAllowNewAccounts(true)
    .enableEmailLinkSignIn()
    .setActionCodeSettings(actionCodeSettings)
    .build()
```

**New (10.x):**
```kotlin
AuthProvider.Email(
    isDisplayNameRequired = true,
    isNewAccountsAllowed = true,
    isEmailLinkSignInEnabled = true,
    emailLinkActionCodeSettings = actionCodeSettings {
        url = "https://example.com/auth"
        handleCodeInApp = true
        setAndroidPackageName(packageName, true, null)
    }
)
```

### Google Provider

**Old (9.x):**
```java
new AuthUI.IdpConfig.GoogleBuilder()
    .setScopes(Arrays.asList("email", "profile"))
    .build()
```

**New (10.x):**
```kotlin
AuthProvider.Google(
    scopes = listOf("email", "profile"),
    serverClientId = "YOUR_CLIENT_ID"
)
```

### Phone Provider

**Old (9.x):**
```java
new AuthUI.IdpConfig.PhoneBuilder()
    .setDefaultNumber("US", "+1 123-456-7890")
    .build()
```

**New (10.x):**
```kotlin
AuthProvider.Phone(
    defaultCountryCode = "US",
    defaultNumber = "+11234567890"
)
```

## Advanced Migration Scenarios

### Custom UI (Activity-based apps that can't use Compose everywhere)

If you have an existing Activity-based app and want to keep using Activities:

**New (10.x) - Low-Level API:**
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
            when (state) {
                is AuthState.Success -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is AuthState.Error -> {
                    // Handle error
                }
                else -> {
                    // Handle other states
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.dispose()
    }
}
```

## Common Issues and Solutions

### Issue: "Unresolved reference: authUIConfiguration"

**Solution:** Ensure you have the correct import:
```kotlin
import com.firebase.ui.auth.configuration.authUIConfiguration
```

### Issue: "ActivityResultLauncher is deprecated"

**Solution:** In 10.x, you no longer need `ActivityResultLauncher`. Use direct callbacks with `FirebaseAuthScreen` or `AuthFlowController`.

### Issue: "How do I customize the UI?"

**Solution:** Use content slots for custom UI:
```kotlin
EmailAuthScreen(
    configuration = emailConfig,
    onSuccess = { /* ... */ },
    onError = { /* ... */ },
    onCancel = { /* ... */ }
) { state ->
    // Your custom UI here
    CustomSignInUI(state)
}
```

### Issue: "My XML themes aren't working"

**Solution:** Convert XML themes to Kotlin code using `AuthUITheme`:
```kotlin
val configuration = authUIConfiguration {
    theme = AuthUITheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6200EE),
            // ... other colors
        )
    )
}
```

## Testing Your Migration

1. **Build the app** - Ensure it compiles without errors
2. **Test all auth flows** - Sign in, sign up, password reset
3. **Test all providers** - Email, Google, Phone, etc.
4. **Test sign out** - Verify users can sign out
5. **Test account deletion** - Verify accounts can be deleted
6. **Test error handling** - Verify errors are handled gracefully
7. **Test theming** - Verify UI matches your design

## Checklist

- [ ] Updated dependency to `firebase-ui-auth:10.0.0-beta01`
- [ ] Migrated to Jetpack Compose
- [ ] Converted Activities to ComponentActivities with `setContent {}`
- [ ] Replaced `createSignInIntentBuilder()` with `authUIConfiguration {}`
- [ ] Updated all provider configurations
- [ ] Converted XML themes to `AuthUITheme`
- [ ] Updated error handling from result codes to exceptions
- [ ] Removed `ActivityResultLauncher` code
- [ ] Updated sign-out to use suspend functions
- [ ] Updated account deletion to use suspend functions
- [ ] Tested all authentication flows
- [ ] Tested on multiple Android versions

## Need Help?

- [FirebaseUI Auth Documentation](https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md)
- [GitHub Issues](https://github.com/firebase/FirebaseUI-Android/issues)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/firebaseui)
