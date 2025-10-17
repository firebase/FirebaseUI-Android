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

package com.firebase.composeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Dialog that prompts the user to re-authenticate with their password.
 * This is required when performing sensitive operations like MFA enrollment.
 */
@Composable
fun ReauthenticationDialog(
    user: FirebaseUser,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Auto-focus the password field when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Verify Your Identity",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "For your security, please re-enter your password to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (user.email != null) {
                    Text(
                        text = "Account: ${user.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (password.isNotBlank() && !isLoading) {
                                coroutineScope.launch {
                                    reauthenticate(
                                        user = user,
                                        password = password,
                                        onLoading = { isLoading = it },
                                        onSuccess = onSuccess,
                                        onError = { error ->
                                            errorMessage = when {
                                                error.message?.contains("password", ignoreCase = true) == true ->
                                                    "Incorrect password. Please try again."
                                                error.message?.contains("network", ignoreCase = true) == true ->
                                                    "Network error. Please check your connection."
                                                else -> "Authentication failed. Please try again."
                                            }
                                            onError(error)
                                        }
                                    )
                                }
                            }
                        }
                    ),
                    enabled = !isLoading,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        reauthenticate(
                            user = user,
                            password = password,
                            onLoading = { isLoading = it },
                            onSuccess = onSuccess,
                            onError = { error ->
                                errorMessage = when {
                                    error.message?.contains("password", ignoreCase = true) == true ->
                                        "Incorrect password. Please try again."
                                    error.message?.contains("network", ignoreCase = true) == true ->
                                        "Network error. Please check your connection."
                                    else -> "Authentication failed. Please try again."
                                }
                                onError(error)
                            }
                        )
                    }
                },
                enabled = password.isNotBlank() && !isLoading
            ) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

private suspend fun reauthenticate(
    user: FirebaseUser,
    password: String,
    onLoading: (Boolean) -> Unit,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    try {
        onLoading(true)

        val email = user.email
        if (email == null) {
            throw IllegalStateException("User email not available")
        }

        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).await()

        onSuccess()
    } catch (e: Exception) {
        onError(e)
    } finally {
        onLoading(false)
    }
}
