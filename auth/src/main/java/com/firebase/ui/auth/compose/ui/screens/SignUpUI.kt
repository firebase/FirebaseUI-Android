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
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.runtime.derivedStateOf
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
import com.firebase.ui.auth.compose.configuration.validators.GeneralFieldValidator
import com.firebase.ui.auth.compose.configuration.validators.EmailValidator
import com.firebase.ui.auth.compose.configuration.validators.PasswordValidator
import com.firebase.ui.auth.compose.ui.components.AuthTextField
import com.firebase.ui.auth.compose.ui.components.TermsAndPrivacyForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpUI(
    modifier: Modifier = Modifier,
    configuration: AuthUIConfiguration,
    provider: AuthProvider.Email,
    isLoading: Boolean,
    displayName: String,
    email: String,
    password: String,
    confirmPassword: String,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onGoToSignIn: () -> Unit,
    onSignUpClick: () -> Unit,
) {
    val context = LocalContext.current
    val stringProvider = DefaultAuthUIStringProvider(context)
    val displayNameValidator = remember { GeneralFieldValidator(stringProvider) }
    val emailValidator = remember { EmailValidator(stringProvider) }
    val passwordValidator = remember {
        PasswordValidator(
            stringProvider = stringProvider,
            rules = provider.passwordValidationRules
        )
    }
    val confirmPasswordValidator = remember(password) {
        GeneralFieldValidator(
            stringProvider = stringProvider,
            isValid = { value ->
                value == password
            },
            customMessage = stringProvider.passwordsDoNotMatch
        )
    }

    val isFormValid = remember(displayName, email, password, confirmPassword) {
        derivedStateOf {
            listOf(
                displayNameValidator.validate(displayName),
                emailValidator.validate(email),
                passwordValidator.validate(password),
                confirmPasswordValidator.validate(confirmPassword)
            ).all { it }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("Sign up")
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
            Spacer(modifier = Modifier.height(16.dp))
            if (provider.isDisplayNameRequired) {
                AuthTextField(
                    value = displayName,
                    validator = displayNameValidator,
                    enabled = !isLoading,
                    label = {
                        Text("First & last Name")
                    },
                    onValueChange = { text ->
                        onDisplayNameChange(text)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = ""
                        )
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            AuthTextField(
                value = password,
                validator = passwordValidator,
                enabled = !isLoading,
                isSecureTextField = true,
                label = {
                    Text("Password")
                },
                onValueChange = { text ->
                    onPasswordChange(text)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = ""
                    )
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(
                value = confirmPassword,
                validator = confirmPasswordValidator,
                enabled = !isLoading,
                isSecureTextField = true,
                label = {
                    Text("Confirm Password")
                },
                onValueChange = { text ->
                    onConfirmPasswordChange(text)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
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
                        onSignUpClick()
                    },
                    enabled = !isLoading && isFormValid.value,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                        )
                    } else {
                        Text("Sign Up")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TermsAndPrivacyForm(
                modifier = Modifier.align(Alignment.End),
                tosUrl = configuration.tosUrl,
                ppUrl = configuration.privacyPolicyUrl,
            )
        }
    }
}

@Preview
@Composable
fun PreviewSignUpUI() {
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
        SignUpUI(
            configuration = authUIConfiguration {
                context = applicationContext
                providers { provider(provider) }
                tosUrl = ""
                privacyPolicyUrl = ""
            },
            provider = provider,
            isLoading = false,
            displayName = "",
            email = "",
            password = "",
            confirmPassword = "",
            onDisplayNameChange = { name -> },
            onEmailChange = { email -> },
            onPasswordChange = { password -> },
            onConfirmPasswordChange = { confirmPassword -> },
            onSignUpClick = {},
            onGoToSignIn = {}
        )
    }
}