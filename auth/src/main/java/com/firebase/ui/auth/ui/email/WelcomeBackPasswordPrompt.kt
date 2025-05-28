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

package com.firebase.ui.auth.ui.email

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.systemBarsPadding
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.Resource
import com.firebase.ui.auth.data.model.State
import com.firebase.ui.auth.ui.idp.TermsAndPrivacyText
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils
import com.firebase.ui.auth.viewmodel.email.WelcomeBackPasswordViewModel
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.firebase.ui.auth.FirebaseAuthAnonymousUpgradeException

private fun validateAndSignIn(
    password: String,
    context: android.content.Context,
    viewModel: WelcomeBackPasswordViewModel,
    email: String,
    idpResponse: com.firebase.ui.auth.IdpResponse,
    onError: (String) -> Unit
) {
    if (password.isBlank()) {
        onError(context.getString(R.string.fui_error_invalid_password))
        return
    }

    viewModel.signIn(email, password, idpResponse)
}

@Composable
fun WelcomeBackPasswordPrompt(
    modifier: Modifier = Modifier,
    flowParameters: FlowParameters,
    email: String,
    idpResponse: com.firebase.ui.auth.IdpResponse,
    onSignInSuccess: () -> Unit,
    onSignInError: (Exception) -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: WelcomeBackPasswordViewModel
) {
    var password by remember { mutableStateOf("") }
    var isPasswordError by remember { mutableStateOf(false) }
    var passwordErrorText by remember { mutableStateOf("") }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    LaunchedEffect(flowParameters) {
        viewModel.init(flowParameters)
    }

    val signInState by viewModel.signInState.collectAsState()
    val isLoading = signInState?.getState() == State.LOADING

    LaunchedEffect(signInState) {
        when (signInState?.getState()) {
            State.SUCCESS -> {
                onSignInSuccess()
            }
            State.FAILURE -> {
                signInState?.getException()?.let { error ->
                    when (error) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            isPasswordError = true
                            passwordErrorText = context.getString(R.string.fui_error_invalid_password)
                        }
                        else -> {
                            onSignInError(error)
                        }
                    }
                }
            }
            else -> {}
        }
    }

    val welcomeText = buildAnnotatedString {
        val baseText = context.getString(R.string.fui_welcome_back_password_prompt_body, email)
        val emailIndex = baseText.indexOf(email)
        append(baseText.substring(0, emailIndex))
        withStyle(SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) {
            append(email)
        }
        append(baseText.substring(emailIndex + email.length))
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(24.dp))
            }

            Text(
                text = welcomeText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    isPasswordError = false
                },
                label = { Text(stringResource(R.string.fui_password_hint)) },
                isError = isPasswordError,
                supportingText = if (isPasswordError) {
                    { Text(passwordErrorText) }
                } else null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        validateAndSignIn(
                            password = password,
                            context = context,
                            viewModel = viewModel,
                            email = email,
                            idpResponse = idpResponse,
                            onError = { error ->
                                isPasswordError = true
                                passwordErrorText = error
                            }
                        )
                    }
                ),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorLabelColor = MaterialTheme.colorScheme.error,
                    errorSupportingTextColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isPasswordError) 8.dp else 0.dp)
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    validateAndSignIn(
                        password = password,
                        context = context,
                        viewModel = viewModel,
                        email = email,
                        idpResponse = idpResponse,
                        onError = { error ->
                            isPasswordError = true
                            passwordErrorText = error
                        }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.fui_sign_in_default))
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = onForgotPassword,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.fui_trouble_signing_in))
            }

            Spacer(Modifier.weight(1f))

            if (flowParameters.isPrivacyPolicyUrlProvided() &&
                flowParameters.isTermsOfServiceUrlProvided()
            ) {
                TermsAndPrivacyText(
                    tosUrl = flowParameters.termsOfServiceUrl!!,
                    ppUrl = flowParameters.privacyPolicyUrl!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        }
    }
} 