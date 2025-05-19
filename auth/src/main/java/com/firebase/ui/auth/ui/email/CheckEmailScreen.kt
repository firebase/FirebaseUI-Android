package com.firebase.ui.auth.ui.email

import android.annotation.SuppressLint
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.systemBarsPadding
import com.firebase.ui.auth.R
import com.firebase.ui.auth.data.model.FlowParameters
import com.firebase.ui.auth.data.model.User
import com.firebase.ui.auth.ui.idp.TermsAndPrivacyText
import com.google.firebase.auth.EmailAuthProvider

@SuppressLint("WrongConstant")
@Composable
fun CheckEmailScreen(
    modifier: Modifier = Modifier,
    flowParameters: FlowParameters,
    initialEmail: String? = null,
    onExistingEmailUser: (User) -> Unit,
    onExistingIdpUser: (User) -> Unit,
    onNewUser: (User) -> Unit,
    onDeveloperFailure: (Exception) -> Unit,
) {
    var email by remember { mutableStateOf(initialEmail ?: "") }
    var isEmailError by remember { mutableStateOf(false) }
    var emailErrorText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    LaunchedEffect(initialEmail) {
        if (!initialEmail.isNullOrEmpty()) {
            email = initialEmail
        }
    }

    fun validateEmail(): Boolean {
        return when {
            TextUtils.isEmpty(email) -> {
                isEmailError = true
                emailErrorText = context.getString(R.string.fui_required_field)
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                isEmailError = true
                emailErrorText = context.getString(R.string.fui_invalid_email_address)
                false
            }
            else -> true
        }
    }

    fun getEmailProvider(): String {
        flowParameters.providers.forEach { config ->
            if (EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD == config.providerId) {
                return EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD
            }
        }
        return EmailAuthProvider.PROVIDER_ID
    }

    val signIn = {
        if (validateEmail()) {
            isLoading = true
            val provider = getEmailProvider()
            val user = User.Builder(provider, email).build()
            onExistingEmailUser(user)
        }
    }

    val signUp = {
        if (validateEmail()) {
            isLoading = true
            val provider = getEmailProvider()
            val user = User.Builder(provider, email).build()
            onNewUser(user)
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
                text = stringResource(R.string.fui_email_link_confirm_email_message),
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
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        signIn()
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
                    .padding(bottom = if (isEmailError) 8.dp else 0.dp)
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = signUp,
                    enabled = !isLoading,
                    modifier = Modifier
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.fui_title_register_email))
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = signIn,
                    enabled = !isLoading,
                    modifier = Modifier
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.fui_sign_in_default))
                }
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
