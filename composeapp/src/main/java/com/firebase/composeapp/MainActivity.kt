package com.firebase.composeapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.firebase.composeapp.ui.screens.EmailAuthMain
import com.firebase.composeapp.ui.screens.MfaEnrollmentMain
import com.firebase.composeapp.ui.screens.PhoneAuthMain
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.auth_provider.signInWithEmailLink
import com.firebase.ui.auth.compose.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.ui.method_picker.AuthMethodPicker
import com.firebase.ui.auth.compose.ui.screens.EmailSignInLinkHandlerActivity
import com.firebase.ui.auth.compose.util.EmailLinkPersistenceManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.actionCodeSettings
import kotlinx.serialization.Serializable

@Serializable
sealed class Route : NavKey {
    @Serializable
    object MethodPicker : Route()

    @Serializable
    object EmailAuth : Route()

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

        val configuration = authUIConfiguration {
            context = applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        isDisplayNameRequired = true,
                        isEmailLinkForceSameDeviceEnabled = true,
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
            }
            tosUrl = "https://policies.google.com/terms?hl=en-NG&fg=1"
            privacyPolicyUrl = "https://policies.google.com/privacy?hl=en-NG&fg=1"
        }

        setContent {
            val backStack = rememberNavBackStack(Route.MethodPicker)

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
                                    Scaffold { innerPadding ->
                                        AuthMethodPicker(
                                            modifier = Modifier.padding(innerPadding),
                                            providers = configuration.providers,
                                            logo = AuthUIAsset.Resource(R.drawable.firebase_auth_120dp),
                                            termsOfServiceUrl = configuration.tosUrl,
                                            privacyPolicyUrl = configuration.privacyPolicyUrl,
                                            onProviderSelected = { provider ->
                                                Log.d(
                                                    "MainActivity",
                                                    "Selected Provider: $provider"
                                                )
                                                when (provider) {
                                                    is AuthProvider.Email -> backStack.add(Route.EmailAuth)
                                                    is AuthProvider.Phone -> backStack.add(Route.PhoneAuth)
                                                }
                                            },
                                        )
                                    }
                                }

                                is Route.EmailAuth -> NavEntry(entry) {
                                    val emailProvider = configuration.providers
                                        .filterIsInstance<AuthProvider.Email>()
                                        .first()
                                    LaunchEmailAuth(authUI, configuration, emailProvider, backStack)
                                }

                                is Route.PhoneAuth -> NavEntry(entry) {
                                    val phoneProvider = configuration.providers
                                        .filterIsInstance<AuthProvider.Phone>()
                                        .first()
                                    LaunchPhoneAuth(authUI, configuration, phoneProvider)
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
        selectedProvider: AuthProvider.Email,
        backStack: androidx.compose.runtime.snapshots.SnapshotStateList<androidx.navigation3.runtime.NavKey>
    ) {
        // Check if this is an email link sign-in flow
        val emailLink = intent.getStringExtra(
            EmailSignInLinkHandlerActivity.EXTRA_EMAIL_LINK
        )

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
                            provider = selectedProvider,
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
            onSetupMfa = {
                backStack.add(Route.MfaEnrollment)
            }
        )
    }

    @Composable
    private fun LaunchPhoneAuth(
        authUI: FirebaseAuthUI,
        configuration: AuthUIConfiguration,
        selectedProvider: AuthProvider.Phone,
    ) {
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