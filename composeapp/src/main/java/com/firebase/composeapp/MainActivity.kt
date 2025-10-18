package com.firebase.composeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.firebase.composeapp.ui.screens.EmailAuthMain
import com.firebase.composeapp.ui.screens.MfaEnrollmentMain
import com.firebase.composeapp.ui.screens.FirebaseAuthScreen
import com.firebase.composeapp.ui.screens.PhoneAuthMain
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.auth_provider.signInWithEmailLink
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.ui.screens.EmailSignInLinkHandlerActivity
import com.firebase.ui.auth.compose.util.EmailLinkPersistenceManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.actionCodeSettings
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class Route : NavKey {
    @Serializable
    object MethodPicker : Route()

    @Serializable
    class EmailAuth(@Contextual val credentialForLinking: AuthCredential? = null) : Route()

    @Serializable
    object PhoneAuth : Route()

    @Serializable
    object MfaEnrollment : Route()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(applicationContext)
        val authUI = FirebaseAuthUI.getInstance()
        // authUI.auth.useEmulator("10.0.2.2", 9099)

        // Check if this activity was launched from an email link deep link
        val emailLink = intent.getStringExtra(EmailSignInLinkHandlerActivity.EXTRA_EMAIL_LINK)

        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        isDisplayNameRequired = true,
                        isEmailLinkForceSameDeviceEnabled = true,
                        isEmailLinkSignInEnabled = true,
                        emailLinkActionCodeSettings = actionCodeSettings {
                            // The continue URL - where to redirect after email link is clicked
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
            // If there's an email link, navigate to EmailAuth screen
            val initialRoute = if (emailLink != null) {
                Route.EmailAuth(credentialForLinking = null)
            } else {
                Route.MethodPicker
            }

            val backStack = rememberNavBackStack(initialRoute)

            AuthUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        },
                        entryProvider = { entry ->
                            val route = entry as Route
                            when (route) {
                                is Route.MethodPicker -> NavEntry(entry) {
                                    FirebaseAuthScreen(
                                        authUI = authUI,
                                        configuration = configuration,
                                        backStack = backStack
                                    )
                                }

                                is Route.EmailAuth -> NavEntry(entry) {
                                    LaunchEmailAuth(
                                        authUI = authUI,
                                        configuration = configuration,
                                        backStack = backStack,
                                        credentialForLinking = route.credentialForLinking,
                                        emailLink = emailLink
                                    )
                                }

                                is Route.PhoneAuth -> NavEntry(entry) {
                                    LaunchPhoneAuth(
                                        authUI = authUI,
                                        configuration = configuration,
                                    )
                                }

                                is Route.MfaEnrollment -> NavEntry(entry) {
                                    LaunchMfaEnrollment(authUI, backStack)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun LaunchEmailAuth(
        authUI: FirebaseAuthUI,
        configuration: AuthUIConfiguration,
        credentialForLinking: AuthCredential? = null,
        backStack: NavBackStack,
        emailLink: String? = null
    ) {
        val provider = configuration.providers
            .filterIsInstance<AuthProvider.Email>()
            .first()

        // Handle email link sign-in if present
        if (emailLink != null) {
            LaunchedEffect(emailLink) {

                try {
                    val emailFromSession =
                        EmailLinkPersistenceManager
                            .retrieveSessionRecord(
                                applicationContext
                            )?.email

                    if (emailFromSession != null) {
                        authUI.signInWithEmailLink(
                            context = applicationContext,
                            config = configuration,
                            provider = provider,
                            email = emailFromSession,
                            emailLink = emailLink,
                        )
                    }
                } catch (e: Exception) {
                    // Error handling is done via AuthState.Error in the auth flow
                }
            }
        }

        EmailAuthMain(
            context = applicationContext,
            configuration = configuration,
            authUI = authUI,
            credentialForLinking = credentialForLinking,
            onSetupMfa = {
                backStack.add(Route.MfaEnrollment)
            }
        )
    }

    @Composable
    private fun LaunchPhoneAuth(
        authUI: FirebaseAuthUI,
        configuration: AuthUIConfiguration,
    ) {
        val provider = configuration.providers
            .filterIsInstance<AuthProvider.Phone>()
            .first()

        PhoneAuthMain(
            context = applicationContext,
            configuration = configuration,
            authUI = authUI,
        )
    }

    @Composable
    private fun LaunchMfaEnrollment(
        authUI: FirebaseAuthUI,
        backStack: androidx.compose.runtime.snapshots.SnapshotStateList<androidx.navigation3.runtime.NavKey>
    ) {
        val user = authUI.getCurrentUser()
        if (user != null) {
            val authConfiguration = authUIConfiguration {
                context = applicationContext
                providers {
                    provider(
                        com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider.Phone(
                            defaultNumber = null,
                            defaultCountryCode = null,
                            allowedCountries = emptyList(),
                            smsCodeLength = 6,
                            timeout = 120L,
                            isInstantVerificationEnabled = true
                        )
                    )
                }
            }

            val mfaConfiguration = com.firebase.ui.auth.compose.configuration.MfaConfiguration(
                allowedFactors = listOf(
                    com.firebase.ui.auth.compose.configuration.MfaFactor.Sms,
                    com.firebase.ui.auth.compose.configuration.MfaFactor.Totp
                ),
                requireEnrollment = false,
                enableRecoveryCodes = true
            )

            MfaEnrollmentMain(
                context = applicationContext,
                authUI = authUI,
                user = user,
                authConfiguration = authConfiguration,
                mfaConfiguration = mfaConfiguration,
                onComplete = {
                    // Navigate back to the previous screen after successful enrollment
                    backStack.removeLastOrNull()
                },
                onSkip = {
                    // Navigate back if user skips enrollment
                    backStack.removeLastOrNull()
                }
            )
        } else {
            // No user signed in, navigate back
            backStack.removeLastOrNull()
        }
    }
}