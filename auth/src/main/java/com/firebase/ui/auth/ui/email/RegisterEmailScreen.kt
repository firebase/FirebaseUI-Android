package com.firebase.ui.auth.ui.email

import android.os.Build
import android.text.TextUtils
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.systemBarsPadding
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.ui.idp.TermsAndPrivacyText
import com.firebase.ui.auth.util.data.PrivacyDisclosureUtils
import com.firebase.ui.auth.util.ui.fieldvalidators.EmailFieldValidator
import com.firebase.ui.auth.util.ui.fieldvalidators.PasswordFieldValidator
import com.firebase.ui.auth.util.ui.fieldvalidators.RequiredFieldValidator
import com.google.firebase.auth.EmailAuthProvider

@Composable
fun RegisterEmailScreen(
    modifier: Modifier = Modifier,
    flowParameters: FlowParameters,
    user: User,
    onRegisterSuccess: (User, String) -> Unit,
    onRegisterError: (Exception) -> Unit,
) {
    var email by remember { mutableStateOf(user.email ?: "") }
    var name by remember { mutableStateOf(user.name ?: "") }
    var password by remember { mutableStateOf("") }
    
    var isEmailError by remember { mutableStateOf(false) }
    var emailErrorText by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    var nameErrorText by remember { mutableStateOf("") }
    var isPasswordError by remember { mutableStateOf(false) }
    var passwordErrorText by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // Get configuration
    val emailConfig = flowParameters.providers.find { it.providerId == EmailAuthProvider.PROVIDER_ID }
    val requireName = emailConfig?.getParams()?.getBoolean("require_name", true) ?: true
    val minPasswordLength = context.resources.getInteger(R.integer.fui_min_password_length)

    // Validate fields
    fun validateFields(): Boolean {
        var isValid = true

        // Validate email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            isEmailError = true
            emailErrorText = context.getString(R.string.fui_invalid_email_address)
            isValid = false
        }

        // Validate name if required
        if (requireName && name.isBlank()) {
            isNameError = true
            nameErrorText = context.getString(R.string.fui_missing_first_and_last_name)
            isValid = false
        }

        // Validate password
        if (password.length < minPasswordLength) {
            isPasswordError = true
            passwordErrorText = context.resources.getQuantityString(
                R.plurals.fui_error_weak_password,
                minPasswordLength,
                minPasswordLength
            )
            isValid = false
        }

        return isValid
    }

    // Register callback
    val register = {
        if (validateFields()) {
            isLoading = true
            val newUser = User.Builder(EmailAuthProvider.PROVIDER_ID, email)
                .setName(name)
                .setPhotoUri(user.photoUri)
                .build()
            onRegisterSuccess(newUser, password)
        }
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
                text = stringResource(R.string.fui_title_register_email),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Start,
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
                supportingText = if (isEmailError) {
                    { Text(emailErrorText) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
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
                    .padding(bottom = if (isEmailError) 8.dp else 0.dp)
            )

            Spacer(Modifier.height(16.dp))

            if (requireName) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isNameError = false
                    },
                    label = { Text(stringResource(R.string.fui_name_hint)) },
                    isError = isNameError,
                    supportingText = if (isNameError) {
                        { Text(nameErrorText) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
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
                        .padding(bottom = if (isNameError) 8.dp else 0.dp)
                )

                Spacer(Modifier.height(16.dp))
            }

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
                        register()
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
                onClick = register,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.fui_title_register_email))
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