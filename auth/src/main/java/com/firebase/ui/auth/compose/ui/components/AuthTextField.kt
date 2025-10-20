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

package com.firebase.ui.auth.compose.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.configuration.PasswordRule
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.validators.EmailValidator
import com.firebase.ui.auth.compose.configuration.validators.FieldValidator
import com.firebase.ui.auth.compose.configuration.validators.PasswordValidator

/**
 * A customizable input field with built-in validation display.
 *
 * **Example usage:**
 * ```kotlin
 * val emailTextValue = remember { mutableStateOf("") }
 *
 * val emailValidator = remember {
 *     EmailValidator(stringProvider = DefaultAuthUIStringProvider(context))
 * }
 *
 * AuthTextField(
 *     value = emailTextValue,
 *     onValueChange = { emailTextValue.value = it },
 *     label = {
 *         Text("Email")
 *     },
 *     validator = emailValidator
 * )
 * ```
 *
 * @param modifier A modifier for the field.
 * @param value The current value of the text field.
 * @param onValueChange A callback when the value changes.
 * @param label The label for the text field.
 * @param enabled If the field is enabled.
 * @param isError Manually set the error state.
 * @param errorMessage A custom error message to display.
 * @param validator A validator to automatically handle error state and messages.
 * @param keyboardOptions Keyboard options for the field.
 * @param keyboardActions Keyboard actions for the field.
 * @param visualTransformation Visual transformation for the input (e.g., password).
 * @param leadingIcon An optional icon to display at the start of the field.
 * @param trailingIcon An optional icon to display at the start of the field.
 */
@Composable
fun AuthTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isSecureTextField: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean? = null,
    errorMessage: String? = null,
    validator: FieldValidator? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    TextField(
        modifier = modifier
            .fillMaxWidth(),
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)
            validator?.validate(newValue)
        },
        label = label,
        singleLine = true,
        enabled = enabled,
        isError = isError ?: validator?.hasError ?: false,
        supportingText = {
            if (validator?.hasError ?: false) {
                Text(text = errorMessage ?: validator.errorMessage)
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = if (isSecureTextField && !passwordVisible)
            PasswordVisualTransformation() else visualTransformation,
        leadingIcon = leadingIcon ?: when {
            validator is EmailValidator -> {
                {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Input Icon"
                    )
                }
            }

            isSecureTextField -> {
                {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password Input Icon"
                    )
                }
            }

            else -> null
        },
        trailingIcon = trailingIcon ?: {
            if (isSecureTextField) {
                IconButton(
                    onClick = {
                        passwordVisible = !passwordVisible
                    }
                ) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
internal fun PreviewAuthTextField() {
    val context = LocalContext.current
    val nameTextValue = remember { mutableStateOf("") }
    val emailTextValue = remember { mutableStateOf("") }
    val passwordTextValue = remember { mutableStateOf("") }
    val emailValidator = remember {
        EmailValidator(stringProvider = DefaultAuthUIStringProvider(context))
    }
    val passwordValidator = remember {
        PasswordValidator(
            stringProvider = DefaultAuthUIStringProvider(context),
            rules = listOf(
                PasswordRule.MinimumLength(8),
                PasswordRule.RequireUppercase,
                PasswordRule.RequireLowercase,
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthTextField(
            value = nameTextValue.value,
            label = {
                Text("Name")
            },
            onValueChange = { text ->
                nameTextValue.value = text
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(
            value = emailTextValue.value,
            validator = emailValidator,
            label = {
                Text("Email")
            },
            onValueChange = { text ->
                emailTextValue.value = text
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = ""
                )
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(
            value = passwordTextValue.value,
            validator = passwordValidator,
            isSecureTextField = true,
            label = {
                Text("Password")
            },
            onValueChange = { text ->
                passwordTextValue.value = text
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = ""
                )
            }
        )
    }
}