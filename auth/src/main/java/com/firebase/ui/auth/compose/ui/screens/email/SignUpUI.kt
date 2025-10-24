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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.configuration.validators.EmailValidator
import com.firebase.ui.auth.compose.configuration.validators.GeneralFieldValidator
import com.firebase.ui.auth.compose.configuration.validators.PasswordValidator
import com.firebase.ui.auth.compose.ui.components.AuthTextField
import com.firebase.ui.auth.compose.ui.components.TermsAndPrivacyForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpUI(
    modifier: Modifier = Modifier,
    configuration: AuthUIConfiguration,
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
    val provider = configuration.providers.filterIsInstance<AuthProvider.Email>().first()
    val context = LocalContext.current
    val stringProvider = LocalAuthUIStringProvider.current
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
                    Text(stringProvider.signupPageTitle)
                },
                colors = AuthUITheme.topAppBarColors
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .safeDrawingPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
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
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (provider.isDisplayNameRequired) {
                AuthTextField(
                    value = displayName,
                    validator = displayNameValidator,
                    enabled = !isLoading,
                    label = {
                        Text(stringProvider.nameHint)
                    },
                    onValueChange = { text ->
                        onDisplayNameChange(text)
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
                    Text(stringProvider.passwordHint)
                },
                onValueChange = { text ->
                    onPasswordChange(text)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(
                value = confirmPassword,
                validator = confirmPasswordValidator,
                enabled = !isLoading,
                isSecureTextField = true,
                label = {
                    Text(stringProvider.confirmPasswordHint)
                },
                onValueChange = { text ->
                    onConfirmPasswordChange(text)
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
                    Text(stringProvider.signInDefault.uppercase())
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
                        Text(stringProvider.signupPageTitle.uppercase())
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
        emailLinkActionCodeSettings = null,
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