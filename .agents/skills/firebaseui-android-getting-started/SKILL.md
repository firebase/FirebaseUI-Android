---
name: firebaseui-android-getting-started
description: Set up FirebaseUI Android Auth with predefined Compose screens in a consumer Android app. Use when adding Firebase Authentication UI, FirebaseUI Auth, FirebaseAuthScreen, authUIConfiguration, or sign-in providers to an Android/Kotlin project.
---

# FirebaseUI Android Auth Setup

Use this skill when the user wants to add FirebaseUI Auth to an existing Android app. Assume you are working in the user's app repo, not the FirebaseUI source repo.

Default to the high-level predefined screen API: `FirebaseAuthScreen` with `authUIConfiguration {}`. Only use low-level controllers or custom slot UIs when the user explicitly asks for custom auth screens.

## Source References

Use these when details are needed beyond this skill:

- FirebaseUI Auth docs: `https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md`
- Firebase Android setup: `https://firebase.google.com/docs/android/setup`
- Firebase Auth provider setup: `https://firebase.google.com/docs/auth`

## Setup Workflow

Track this checklist while working:

- [ ] Inspect the Android project structure and identify the app module.
- [ ] Verify Firebase project configuration: `google-services.json`, Google Services Gradle plugin, package name, and enabled Auth providers.
- [ ] Verify Compose, Kotlin, min SDK, and FirebaseUI Auth dependencies.
- [ ] Add or adapt a predefined auth screen using `FirebaseAuthScreen`.
- [ ] Wire success, failure, cancel, and signed-in state handling into the app's navigation.
- [ ] Run the smallest relevant Gradle build or test task.

## Firebase Project Configuration

Do not invent Firebase config values. The consumer must get them from their Firebase project.

1. In the Firebase Console, add an Android app using the app's real `applicationId`.
2. Download `google-services.json` and place it in the app module, usually `app/google-services.json`.
3. Enable each provider the app will expose in Firebase Console > Authentication > Sign-in method.
4. For Google Sign-In, ensure the Google Services Gradle plugin is applied and the app's package name/SHA certificates are configured in Firebase.
5. For phone auth, verify the user's Firebase project supports the target regions and test numbers if needed.
6. For OAuth providers such as Facebook, Twitter/X, GitHub, Microsoft, Yahoo, Apple, or custom OIDC, configure provider credentials in Firebase Console before wiring the Android UI.

## Gradle Defaults

Prefer Kotlin DSL snippets when the project uses `build.gradle.kts`; translate to Groovy only if the project already uses Groovy.

App module essentials:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("com.firebaseui:firebase-ui-auth:10.0.0-beta02")

    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-auth")

    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
```

Root/plugin configuration varies by project. If `com.google.gms.google-services` is not already available, add the Google Services plugin using the project's existing convention: `plugins { ... apply false }`, `buildscript` classpath, or version catalog.

Add provider-specific dependencies only when used. Facebook login needs:

```kotlin
implementation("com.facebook.android:facebook-login:18.0.3")
```

Minimum expectations from the FirebaseUI Auth docs: Android SDK 21+, Kotlin 1.9+, Compose compiler 1.5+, and Firebase Auth 22.0.0+. Respect stricter versions already present in the user repo.

## Predefined Auth Screen Template

Adapt names, theme, navigation, and provider list to the app. Keep provider setup aligned with what is enabled in Firebase Console.

```kotlin
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.theme.AuthUITheme
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen

class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authUI = FirebaseAuthUI.getInstance()

        if (authUI.isSignedIn()) {
            navigateToHome()
            finish()
            return
        }

        setContent {
            AppTheme {
                val configuration = authUIConfiguration {
                    context = applicationContext
                    theme = AuthUITheme.fromMaterialTheme()
                    providers {
                        provider(AuthProvider.Email())
                        provider(AuthProvider.Google())
                    }
                }

                FirebaseAuthScreen(
                    configuration = configuration,
                    authUI = authUI,
                    onSignInSuccess = { result ->
                        navigateToHome()
                    },
                    onSignInFailure = { exception: AuthException ->
                        Toast.makeText(
                            this,
                            exception.message ?: "Authentication failed",
                            Toast.LENGTH_SHORT
                        ).show()
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

If the project uses Navigation Compose, prefer making `FirebaseAuthScreen` a destination and call the existing `NavController` from callbacks instead of creating a new Activity.

## Provider Notes

- Email/password works with `AuthProvider.Email()` and includes sign-in, sign-up, password reset, and optional display name collection.
- Google works with `AuthProvider.Google()` once `google-services.json`, Firebase provider enablement, and SHA certificates are correct.
- Anonymous sign-in uses `AuthProvider.Anonymous`; enable anonymous auth in Firebase Console first.
- Phone uses `AuthProvider.Phone(...)`; configure country defaults only when the product has a clear country policy.
- Facebook uses `AuthProvider.Facebook()` plus the Facebook SDK dependency and these string resources:

```xml
<string name="facebook_application_id" translatable="false">YOUR_FACEBOOK_APP_ID</string>
<string name="facebook_login_protocol_scheme" translatable="false">fbYOUR_FACEBOOK_APP_ID</string>
<string name="facebook_client_token" translatable="false">CHANGE-ME</string>
```

- Generic OIDC providers use `AuthProvider.GenericOAuth(...)` with the provider ID exactly as configured in Firebase Console, for example `oidc.line`.

## Gotchas

- Never commit a real `google-services.json` unless the user's repo already treats Firebase config as committable and they explicitly want it included.
- Do not hard-code demo package names, server client IDs, OAuth IDs, policy URLs, or Firebase project values from FirebaseUI samples.
- Google Sign-In failures are often Firebase Console or SHA certificate issues, not Kotlin code issues.
- Keep the provider list small at first. Add only providers the user has configured and can test.
- Set `theme` in `authUIConfiguration` for clarity. Use an `AuthUITheme` wrapper only if surrounding UI must share that theme.
- For email-link sign-in, configure `actionCodeSettings`, handle the incoming deep link, and add the matching manifest intent filter. Do not add email-link support unless requested.

## Validation

After edits, run the smallest command that proves the integration compiles, such as:

```bash
./gradlew :app:assembleDebug
```

If the build fails, first check dependency versions, Compose enablement, the Google Services plugin, and whether `google-services.json` is in the app module. Report any Firebase Console steps the agent cannot complete locally.