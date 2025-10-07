package com.firebase.ui.auth.compose.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.configuration.validators.EmailValidator
import com.firebase.ui.auth.compose.configuration.validators.PasswordValidator
import com.firebase.ui.auth.compose.ui.components.AuthTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordUI(
    modifier: Modifier = Modifier,
    configuration: AuthUIConfiguration,
    isLoading: Boolean,
    email: String,
    resetLinkSent: Boolean,
    onEmailChange: (String) -> Unit,
    onSendResetLink: () -> Unit,
    onGoToSignIn: () -> Unit,
) {
    val context = LocalContext.current
    val emailValidator = remember {
        EmailValidator(stringProvider = DefaultAuthUIStringProvider(context))
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("Recover Password")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp),
        ) {
            AuthTextField(
                value = email,
                validator = emailValidator,
                enabled = !isLoading,
                label = {
                    Text("Email")
                },
                onValueChange = { text ->
                    onEmailChange(text)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = ""
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .align(Alignment.End),
            ) {
                Button(
                    onClick = {
                        onGoToSignIn()
                    },
                    enabled = !isLoading,
                ) {
                    Text("Sign In")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        onSendResetLink()
                    },
                    enabled = !isLoading,
                ) {
                    Text("Send")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .align(Alignment.End),
            ) {
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            configuration.tosUrl?.toUri()
                        )
                        context.startActivity(intent)
                    },
                    contentPadding = PaddingValues.Zero,
                    enabled = !isLoading,
                ) {
                    Text(
                        modifier = modifier,
                        text = "Terms of Service",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            configuration.privacyPolicyUrl?.toUri()
                        )
                        context.startActivity(intent)
                    },
                    contentPadding = PaddingValues.Zero,
                    enabled = !isLoading,
                ) {
                    Text(
                        modifier = modifier,
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewResetPasswordUI() {
    val applicationContext = LocalContext.current
    val provider = AuthProvider.Email(
        isDisplayNameRequired = true,
        isEmailLinkSignInEnabled = false,
        isEmailLinkForceSameDeviceEnabled = true,
        actionCodeSettings = null,
        isNewAccountsAllowed = true,
        minimumPasswordLength = 8,
        passwordValidationRules = listOf()
    )

    AuthUITheme {
        ResetPasswordUI(
            configuration = authUIConfiguration {
                context = applicationContext
                providers { provider(provider) }
                tosUrl = ""
                privacyPolicyUrl = ""
            },
            email = "",
            isLoading = false,
            resetLinkSent = false,
            onEmailChange = { email -> },
            onSendResetLink = {},
            onGoToSignIn = {},
        )
    }
}