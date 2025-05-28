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

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.Resource
import com.firebase.ui.auth.data.model.State
import com.firebase.ui.auth.ui.idp.TermsAndPrivacyText
import com.firebase.ui.auth.viewmodel.email.RecoverPasswordHandler
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

@Composable
fun RecoverPasswordScreen(
    modifier: Modifier = Modifier,
    flowParameters: FlowParameters,
    initialEmail: String? = null,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit,
    viewModel: RecoverPasswordHandler
) {
    val context = LocalContext.current  

    var email by remember { mutableStateOf(initialEmail.orEmpty()) }
    var isEmailError by remember { mutableStateOf(false) }
    var emailErrorText by remember { mutableStateOf("") }

    var showSentDialog by remember { mutableStateOf(false) }
    var sentToEmail by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(flowParameters) {
        viewModel.init(flowParameters)
    }

    val resetState: Resource<String>? by viewModel.operation.observeAsState(null)

    val isLoading = resetState?.state == State.LOADING

    LaunchedEffect(resetState) {
        val resource = resetState ?: return@LaunchedEffect

        when (resource.state) {
            State.SUCCESS -> {
                resource.value?.let { address ->
                    sentToEmail = address
                    showSentDialog = true
                }
            }
            State.FAILURE -> {
                resource.exception?.let { error ->
                    if (error is FirebaseAuthInvalidUserException ||
                        error is FirebaseAuthInvalidCredentialsException
                    ) {
                        isEmailError = true
                        emailErrorText = context.getString(R.string.fui_error_email_does_not_exist)
                    } else {
                        onError(error)
                    }
                }
            }
            else -> { /* no-op */ }
        }
    }

    if (showSentDialog) {
        BackHandler {
            showSentDialog = false
            onSuccess()
        }
    }

    if (showSentDialog) {
        AlertDialog(
            onDismissRequest = {
                showSentDialog = false
                onSuccess()
            },
            confirmButton = {
                TextButton(onClick = {
                    showSentDialog = false
                    onSuccess()
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            title = {
                Text(stringResource(R.string.fui_title_confirm_recover_password))
            },
            text = {
                Text(stringResource(R.string.fui_confirm_recovery_body, sentToEmail))
            }
        )
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(if (isLoading) 16.dp else 24.dp))

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            Text(
                text = stringResource(R.string.fui_title_confirm_recover_password),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    isEmailError = false
                },
                label = { Text(stringResource(R.string.fui_email_hint)) },
                isError = isEmailError,
                supportingText = {
                    if (isEmailError) Text(emailErrorText)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (email.isNotBlank()) {
                            viewModel.startReset(
                                email,
                                flowParameters.passwordResetSettings
                            )
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isEmailError) 8.dp else 0.dp)
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    if (email.isNotBlank()) {
                        viewModel.startReset(
                            email,
                            flowParameters.passwordResetSettings
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.fui_button_text_send))
            }

            Spacer(Modifier.weight(1f))

            if (flowParameters.isPrivacyPolicyUrlProvided() &&
                flowParameters.isTermsOfServiceUrlProvided()
            ) {
                Spacer(Modifier.height(16.dp))
                TermsAndPrivacyText(
                    tosUrl = flowParameters.termsOfServiceUrl!!,
                    ppUrl = flowParameters.privacyPolicyUrl!!,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}