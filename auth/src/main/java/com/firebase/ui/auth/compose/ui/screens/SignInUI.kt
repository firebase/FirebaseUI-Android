/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.compose.ui.screens

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.configuration.validators.EmailValidator
import com.firebase.ui.auth.compose.configuration.validators.PasswordValidator
import com.firebase.ui.auth.compose.ui.components.AuthTextField
import com.firebase.ui.auth.compose.ui.components.TermsAndPrivacyForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInUI(
    modifier: Modifier = Modifier,
    configuration: AuthUIConfiguration,
    isLoading: Boolean,
    emailSignInLinkSent: Boolean,
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClick: () -> Unit,
    onGoToSignUp: () -> Unit,
    onGoToResetPassword: () -> Unit,
) {
    val context = LocalContext.current
    val provider = configuration.providers.filterIsInstance<AuthProvider.Email>().first()
    val stringProvider = DefaultAuthUIStringProvider(context)
    val emailValidator = remember { EmailValidator(stringProvider) }
    val passwordValidator = remember {
        PasswordValidator(stringProvider = stringProvider, rules = emptyList())
    }

    val isFormValid = remember(email, password) {
        derivedStateOf {
            listOf(
                emailValidator.validate(email),
                if (!provider.isEmailLinkSignInEnabled)
                    passwordValidator.validate(password) else true,
            ).all { it }
        }
    }

    val isDialogVisible =
        remember(emailSignInLinkSent) { mutableStateOf(emailSignInLinkSent) }

    if (isDialogVisible.value) {
        AlertDialog(
            title = {
                Text(
                    text = "Email Sign In Link Sent",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "Check your email $email",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDialogVisible.value = false
                    }
                ) {
                    Text("Dismiss")
                }
            },
            onDismissRequest = {
                isDialogVisible.value = false
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringProvider.signInDefault)
                },
                colors = AuthUITheme.topAppBarColors
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
                    Text(stringProvider.emailHint)
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
            if (!provider.isEmailLinkSignInEnabled) {
                AuthTextField(
                    value = password,
                    validator = passwordValidator,
                    enabled = !isLoading,
                    isSecureTextField = true,
                    label = {
                        Text(stringProvider.passwordHint)
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
                Spacer(modifier = Modifier.height(8.dp))
            }
            TextButton(
                modifier = Modifier
                    .align(Alignment.Start),
                onClick = {
                    onGoToResetPassword()
                },
                enabled = !isLoading,
                contentPadding = PaddingValues.Zero
            ) {
                Text(
                    modifier = modifier,
                    text = stringProvider.troubleSigningIn,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .align(Alignment.End),
            ) {
                // Signup is hidden for email link sign in
                if (!provider.isEmailLinkSignInEnabled) {
                    Button(
                        onClick = {
                            onGoToSignUp()
                        },
                        enabled = !isLoading,
                    ) {
                        Text(stringProvider.titleRegisterEmail)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Button(
                    onClick = {
                        // TODO(demolaf): When signIn is fired if Exception is UserNotFound
                        //  then we check if provider.isNewAccountsAllowed then we show signUp
                        //  else we show an error dialog stating signup is not allowed
                        onSignInClick()
                    },
                    enabled = !isLoading && isFormValid.value,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                        )
                    } else {
                        Text(stringProvider.signInDefault)
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
fun PreviewSignInUI() {
    val applicationContext = LocalContext.current
    val provider = AuthProvider.Email(
        isDisplayNameRequired = true,
        isEmailLinkSignInEnabled = false,
        isEmailLinkForceSameDeviceEnabled = true,
        emailLinkActionCodeSettings = null,
        isNewAccountsAllowed = true,
        minimumPasswordLength = 8,
        passwordValidationRules = listOf()
    )

    AuthUITheme {
        SignInUI(
            configuration = authUIConfiguration {
                context = applicationContext
                providers { provider(provider) }
                tosUrl = ""
                privacyPolicyUrl = ""
            },
            email = "",
            password = "",
            isLoading = false,
            emailSignInLinkSent = false,
            onEmailChange = { email -> },
            onPasswordChange = { password -> },
            onSignInClick = {},
            onGoToSignUp = {},
            onGoToResetPassword = {},
        )
    }
}
