package com.firebase.composeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.firebase.composeapp.ui.screens.MainScreen
import com.firebase.ui.auth.compose.FirebaseAuthUI
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.actionCodeSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(applicationContext)
        val authUI = FirebaseAuthUI.getInstance()

        val provider = AuthProvider.Email(
            isDisplayNameRequired = true,
            // isEmailLinkSignInEnabled = true,
            isEmailLinkForceSameDeviceEnabled = true,
            actionCodeSettings = actionCodeSettings {
                url = "https://example.com/verify"
                handleCodeInApp = true
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
            tosUrl = "https://www.google.com"
            privacyPolicyUrl = "https://www.google.com"
        }

        setContent {
            AuthUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
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