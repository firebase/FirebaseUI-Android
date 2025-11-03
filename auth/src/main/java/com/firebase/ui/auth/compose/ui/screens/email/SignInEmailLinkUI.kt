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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.configuration.validators.EmailValidator
import com.firebase.ui.auth.compose.ui.components.AuthTextField
import com.firebase.ui.auth.compose.ui.components.TermsAndPrivacyForm
import com.google.firebase.auth.actionCodeSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInEmailLinkUI(
    modifier: Modifier = Modifier,
    configuration: AuthUIConfiguration,
    isLoading: Boolean,
    emailSignInLinkSent: Boolean,
    email: String,
    onEmailChange: (String) -> Unit,
    onSignInWithEmailLink: () -> Unit,
    onGoToSignIn: () -> Unit,
    onGoToResetPassword: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
) {
    val provider = configuration.providers.filterIsInstance<AuthProvider.Email>().first()
    val stringProvider = LocalAuthUIStringProvider.current
    val emailValidator = remember { EmailValidator(stringProvider) }

    val isFormValid = remember(email) {
        derivedStateOf {
            emailValidator.validate(email)
        }
    }

    if (provider.isEmailLinkSignInEnabled) {
        val isDialogVisible =
            remember(emailSignInLinkSent) { mutableStateOf(emailSignInLinkSent) }

        if (isDialogVisible.value) {
            AlertDialog(
                title = {
                    Text(
                        text = stringProvider.emailSignInLinkSentDialogTitle,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Text(
                        text = stringProvider.emailSignInLinkSentDialogBody(email),
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
                        Text(stringProvider.dismissAction)
                    }
                },
                onDismissRequest = {
                    isDialogVisible.value = false
                },
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringProvider.signInDefault,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringProvider.backAction
                            )
                        }
                    }
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
            Button(
                onClick = {
                    onSignInWithEmailLink()
                },
                modifier = Modifier.align(Alignment.End),
                enabled = !isLoading && isFormValid.value,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(stringProvider.signInDefault.uppercase())
                }
            }

            // Show toggle to go back to password mode
            Spacer(modifier = Modifier.height(64.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringProvider.orContinueWith,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    onGoToSignIn()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(stringProvider.signInWithPassword.uppercase())
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
fun PreviewSignInEmailLinkUI() {
    val applicationContext = LocalContext.current
    val provider = AuthProvider.Email(
        isDisplayNameRequired = true,
        isEmailLinkSignInEnabled = true,
        isEmailLinkForceSameDeviceEnabled = true,
        emailLinkActionCodeSettings = actionCodeSettings {
            url = "https://fake-project-id.firebaseapp.com"
            handleCodeInApp = true
            setAndroidPackageName(
                "fake.project.id",
                true,
                null
            )
        },
        isNewAccountsAllowed = true,
        minimumPasswordLength = 8,
        passwordValidationRules = listOf()
    )

    AuthUITheme {
        SignInEmailLinkUI(
            configuration = authUIConfiguration {
                context = applicationContext
                providers { provider(provider) }
                tosUrl = ""
                privacyPolicyUrl = ""
            },
            email = "",
            isLoading = false,
            emailSignInLinkSent = false,
            onEmailChange = { email -> },
            onSignInWithEmailLink = {},
            onGoToSignIn = {},
            onGoToResetPassword = {},
        )
    }
}
