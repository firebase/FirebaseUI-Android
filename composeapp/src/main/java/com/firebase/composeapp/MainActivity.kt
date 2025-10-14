package com.firebase.composeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.firebase.composeapp.ui.screens.EmailAuthMain
import com.firebase.composeapp.ui.screens.PhoneAuthMain
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.auth_provider.signInWithEmailLink
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.ui.screens.EmailSignInLinkHandlerActivity
import com.firebase.ui.auth.compose.util.EmailLinkPersistenceManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.actionCodeSettings
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(applicationContext)
        val authUI = FirebaseAuthUI.getInstance()
        authUI.auth.useEmulator("10.0.2.2", 9099)

        //initializeEmailAuth(authUI)
        initializePhoneAuth(authUI)
    }

    fun initializePhoneAuth(authUI: FirebaseAuthUI) {
        val provider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = emptyList(),
            smsCodeLength = 6,
            timeout = 60L,
            isInstantVerificationEnabled = true
        )

        val configuration = authUIConfiguration {
            context = applicationContext
            providers { provider(provider) }
            tosUrl = "https://policies.google.com/terms?hl=en-NG&fg=1"
            privacyPolicyUrl = "https://policies.google.com/privacy?hl=en-NG&fg=1"
        }

        setContent {
            AuthUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhoneAuthMain(
                        context = applicationContext,
                        configuration = configuration,
                        authUI = authUI,
                        provider = provider
                    )
                }
            }
        }
    }

    fun initializeEmailAuth(authUI: FirebaseAuthUI) {
        // Check if this is an email link sign-in flow
        val emailLink = intent.getStringExtra(
            EmailSignInLinkHandlerActivity.EXTRA_EMAIL_LINK
        )

        val provider = AuthProvider.Email(
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

        val configuration = authUIConfiguration {
            context = applicationContext
            providers { provider(provider) }
            tosUrl = "https://policies.google.com/terms?hl=en-NG&fg=1"
            privacyPolicyUrl = "https://policies.google.com/privacy?hl=en-NG&fg=1"
        }

        if (emailLink != null) {
            lifecycleScope.launch {
                try {
                    val emailFromSession = EmailLinkPersistenceManager
                        .retrieveSessionRecord(applicationContext)?.email

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

        setContent {
            AuthUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EmailAuthMain(
                        context = applicationContext,
                        configuration = configuration,
                        authUI = authUI,
                        provider = provider
                    )
                }
            }
        }
    }
}