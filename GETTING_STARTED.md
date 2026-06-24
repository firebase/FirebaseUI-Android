# Easily add sign-in to your Android app with FirebaseUI

[FirebaseUI](https://github.com/firebase/firebaseui-android) Auth is a library built on top of the Firebase Authentication SDK that provides drop-in UI flows for use in your app.

Caution: Version 10.x is currently a **beta release**. This means that the functionality might change in backward-incompatible ways or have limited support. A beta release is not subject to any SLA or deprecation policy.

The recommended sign-in flow uses Compose screens. For apps that still use Activities, see the [Existing Activity-based apps](#existing-activity-based-apps) section.

FirebaseUI Auth provides the following benefits:

- **Multiple Providers** — sign-in flows for email/password, phone, Google, Facebook, Apple, GitHub, Microsoft, Yahoo, Twitter, anonymous auth, and custom OAuth.
- **Account Management** — flows to handle account management tasks, such as account creation and password resets.
- **Account Linking** — flows to safely link user accounts across identity providers.
- **Anonymous User Upgrading** — flows to safely upgrade anonymous users.
- **Custom Themes** — Material 3 UI support that can inherit your app theme. Also, because FirebaseUI is open source, you can fork the project and customize it exactly to your needs.
- **Credential Manager** — automatic integration with [Credential Manager](https://developer.android.com/identity/sign-in/credential-manager) for fast cross-device sign-in.
- **Multi-Factor Authentication** — SMS and TOTP support for additional security.

## Before you begin

1. If you haven't already, [add Firebase to your Android project](https://firebase.google.com/docs/android/setup).
2. In the [Firebase console](https://console.firebase.google.com/), enable the sign-in methods you want to support.
3. Add FirebaseUI Auth to your app module:

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

## Set up sign-in methods

### Google Sign-In

Google Sign-In configuration is automatically provided by the [google-services Gradle plugin](https://developers.google.com/android/guides/google-services-plugin). Ensure you have enabled Google Sign-In in the [Firebase Console](https://console.firebase.google.com/project/_/authentication/providers).

### Facebook Login

If using Facebook Login, add your Facebook App ID to `strings.xml`:

```xml
<resources>
    <string name="facebook_application_id" translatable="false">YOUR_FACEBOOK_APP_ID</string>
    <string name="facebook_login_protocol_scheme" translatable="false">fbYOUR_FACEBOOK_APP_ID</string>
    <string name="facebook_client_token" translatable="false">CHANGE-ME</string>
</resources>
```

See the [Facebook for Developers](https://developers.facebook.com/) documentation for setup instructions.

### Other Providers

Twitter, GitHub, Microsoft, Yahoo, and Apple providers require configuration in the Firebase Console but no additional Android-specific setup. See the [Firebase Auth documentation](https://firebase.google.com/docs/auth) for provider-specific instructions.

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

Email link sign-in lives in the email provider configuration:

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

For the full deep-link handling flow, see [the Email Link Sign-In section of the README in GitHub](https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#email-link-sign-in).

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

For full theming and customization details, including theme precedence, provider button styling, and custom themes, see [the Theming and Customization section of the readme in GitHub](https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#theming--customization).

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
