# Easily add sign-in to your Android app with FirebaseUI

[FirebaseUI](https://github.com/firebase/firebaseui-android) Auth is a library built on top of the Firebase Authentication SDK that provides drop-in UI flows for use in your app.

Caution: Version 10.x is currently a **beta release**. This means that the functionality might change in backward-incompatible ways or have limited support. A beta release is not subject to any SLA or deprecation policy.

The recommended sign-in flow uses Compose screens. For apps that still use Activities, see the [Existing Activity-based apps](#existing-activity-based-apps) section.

FirebaseUI Auth provides the following benefits:

- Credential Manager integration for faster sign-in on Android.
- Material 3 UI that can inherit your app theme.
- Multiple authentication providers, including email/password, phone, Google, Facebook, Apple, GitHub, Microsoft, Yahoo, Twitter, anonymous auth, and custom OAuth.
- Multi-factor authentication support, including SMS and TOTP.
- Built-in flows for account management, account linking, and anonymous user upgrade.

## Before you begin

1. If you haven't already, [add Firebase to your Android project](https://firebase.google.com/docs/android/setup).
2. In the [Firebase console](https://console.firebase.google.com/), enable the sign-in methods you want to support.
3. Add FirebaseUI Auth to your app module

Add FirebaseUI Auth to your app module:

```kotlin
dependencies {
    // Check Maven Central for the latest version:
    // https://central.sonatype.com/artifact/com.firebaseui/firebase-ui-auth/versions
    implementation("com.firebaseui:firebase-ui-auth:10.0.0-beta02")

    // Required only if Facebook login support is required
    // Find the latest Facebook SDK releases here: https://goo.gl/Ce5L94
    implementation("com.facebook.android:facebook-android-sdk:8.x")
}
```

## Provider configuration

Some providers need additional setup before you can sign users in:
- [Sign in with Google](https://firebase.google.com/docs/auth/android/google-signin)
- [Facebook Login](https://firebase.google.com/docs/auth/android/facebook-login)
- [Sign in with Apple](https://firebase.google.com/docs/auth/android/apple)
- [Sign in with Twitter](https://firebase.google.com/docs/auth/android/twitter-login)
- [Sign in with Github](https://firebase.google.com/docs/auth/android/github-auth)
- [Sign in with Microsoft](https://firebase.google.com/docs/auth/android/microsoft-oauth)
- [Sign in with Yahoo](https://firebase.google.com/docs/auth/android/yahoo-oauth)

### Providers

Apple, GitHub, Microsoft, Yahoo, Twitter and custom OAuth providers are configured in Firebase Authentication. Most of them do not require extra Android-specific resources.

## Sign in

Create an `AuthUIConfiguration`, then show `FirebaseAuthScreen`.

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authUI = FirebaseAuthUI.getInstance()

        setContent {
            MyAppTheme {
                val configuration = authUIConfiguration {
                    context = applicationContext
                    theme = AuthUITheme.fromMaterialTheme()
                    providers {
                        provider(AuthProvider.Email())
                        provider(
                            AuthProvider.Google(
                                scopes = listOf("email"),
                                serverClientId = null,
                            )
                        )
                    }
                }

                if (authUI.isSignedIn()) {
                    HomeScreen()
                } else {
                    FirebaseAuthScreen(
                        configuration = configuration,
                        authUI = authUI,
                        onSignInSuccess = { result ->
                            // User signed in successfully
                        },
                        onSignInFailure = { exception ->
                            // Sign in failed
                        },
                        onSignInCancelled = {
                            finish()
                        },
                    )
                }
            }
        }
    }
}
```

This gives you a complete authentication flow with:

- Password Authentication.
- Google Sign-In.
- Password reset.
- Material 3 styling.
- Credential Manager support.
- Error handling through direct callbacks.

## Configure providers

Choose the providers you want inside `authUIConfiguration`:

```kotlin
val configuration = authUIConfiguration {
    context = applicationContext
    providers {
        provider(AuthProvider.Email())
        provider(
            AuthProvider.Phone(
                defaultCountryCode = "US",
            )
        )
        provider(
            AuthProvider.Google(
                scopes = listOf("email"),
                serverClientId = null,
            )
        )
        provider(AuthProvider.Facebook())
    }
}
```

### Email link sign-in

Email link sign-in now lives in the email provider configuration:

```kotlin
val configuration = authUIConfiguration {
    context = applicationContext
    providers {
        provider(
            AuthProvider.Email(
                isEmailLinkSignInEnabled = true,
                emailLinkActionCodeSettings = actionCodeSettings {
                    url = "https://example.com/auth"
                    handleCodeInApp = true
                    setAndroidPackageName(
                        "com.example.app",
                        true,
                        null,
                    )
                },
            )
        )
    }
}
```

For the full deep-link handling flow, see `auth/README.md`.

## Sign out

FirebaseUI Auth provides convenience methods for sign-out and account deletion:

```kotlin
lifecycleScope.launch {
    FirebaseAuthUI.getInstance().signOut(applicationContext)
}
```

```kotlin
lifecycleScope.launch {
    FirebaseAuthUI.getInstance().delete(applicationContext)
}
```

## Customization

FirebaseUI Auth is customizable, and the simplest way to get started is to set a theme directly in `authUIConfiguration`:

```kotlin
val configuration = authUIConfiguration {
    context = applicationContext
    providers {
        provider(AuthProvider.Email())
        provider(AuthProvider.Google(scopes = listOf("email"), serverClientId = null))
    }
    theme = AuthUITheme.Adaptive
}
```

You can also:

- Use `AuthUITheme.Default`, `AuthUITheme.DefaultDark`, or `AuthUITheme.Adaptive`.
- Inherit your app theme with `AuthUITheme.fromMaterialTheme()`.
- Customize the default theme with `.copy()`.
- Build a fully custom `AuthUITheme`.
- Set a logo, Terms of Service URL, and Privacy Policy URL in `authUIConfiguration`.

For full theming and customization details, including theme precedence, provider button styling, and custom themes, see `auth/README.md`.

## Existing Activity-based apps

If your app still uses Activities and the Activity Result API, you can keep an Activity-based launch flow by using `AuthFlowController`:

```kotlin
private val authLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
) { result ->
    if (result.resultCode == RESULT_OK) {
        val user = FirebaseAuth.getInstance().currentUser
        // ...
    } else {
        // User cancelled or sign-in failed
    }
}

val configuration = authUIConfiguration {
    context = applicationContext
    providers {
        provider(AuthProvider.Email())
        provider(
            AuthProvider.Google(
                scopes = listOf("email"),
                serverClientId = null,
            )
        )
    }
}

val controller = FirebaseAuthUI.getInstance().createAuthFlow(configuration)
authLauncher.launch(controller.createIntent(this))
```
