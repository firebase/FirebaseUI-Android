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

package com.firebase.ui.auth.ui.screens.email

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.configuration.AuthUIConfiguration
import com.firebase.ui.auth.configuration.PasswordRule
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.password_policy.PasswordPolicy
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.configuration.theme.AuthUITheme
import com.firebase.ui.auth.configuration.validators.EmailValidator
import com.firebase.ui.auth.configuration.validators.GeneralFieldValidator
import com.firebase.ui.auth.configuration.validators.PasswordValidator
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.firebase.ui.auth.ui.components.AuthTextField
import com.firebase.ui.auth.ui.components.TermsAndPrivacyForm
import com.firebase.ui.auth.ui.exposeTestTagsAsResourceIds
import kotlinx.coroutines.flow.collectLatest

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
    onSignUpClick: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    isEmailLocked: Boolean = false,
) {
    val provider = configuration.providers.filterIsInstance<AuthProvider.Email>().first()
    val context = LocalContext.current
    val stringProvider = LocalAuthUIStringProvider.current
    val displayNameValidator = remember { GeneralFieldValidator(stringProvider) }
    val emailValidator = remember { EmailValidator(stringProvider) }
    // Set on the first edit, so a re-key can tell a cleared field from an untouched one.
    var passwordEdited by rememberSaveable { mutableStateOf(false) }
    // Keyed on the inputs it is built from, so a swapped provider keeps the wording in sync.
    // They are stable while typing, so the validator is not rebuilt per keystroke.
    val passwordValidator = remember(
        stringProvider,
        provider.minimumPasswordLength,
        provider.passwordValidationRules,
    ) {
        PasswordValidator(
            stringProvider = stringProvider,
            // Same order as the submit path: minimumPasswordLength first, then the custom rules.
            // The validator reports only the first failing rule, so there is one message at most.
            rules = listOf(PasswordRule.MinimumLength(provider.minimumPasswordLength)) +
                provider.passwordValidationRules
        ).also {
            // A new instance starts without an error, so re-validate typed input to keep it shown.
            // Only an untouched field stays unseeded, so a re-key adds no error to it.
            if (password.isNotEmpty() || passwordEdited) it.validate(password)
        }
    }
    val passwordPolicySource = LocalPasswordPolicySource.current
    // Hints only (never part of isFormValid). No source, no policy or OFF: nothing is shown.
    // Re-read on each revision, so a refetch after a server rejection updates the checklist.
    val passwordPolicy by produceState<PasswordPolicy?>(null, passwordPolicySource, context) {
        passwordPolicySource?.revision?.collectLatest {
            value = passwordPolicySource.getPolicy(context)
                ?.takeIf { it.enforcementState == PasswordPolicy.EnforcementState.ENFORCE }
        }
    }
    val checklistRequirements = remember(passwordPolicy, password, stringProvider) {
        passwordPolicy?.let { passwordRequirements(it, password, stringProvider) }
    }
    // Only unmet lines: local and policy symbol sets differ, so a met line can share a failing
    // local rule's label, and blanking it would leave no reason shown. For the same reason the
    // special character message is never blanked while a local special character rule exists:
    // the policy may accept a symbol the local rule rejects.
    val hasLocalSpecialRule =
        PasswordRule.RequireSpecialCharacter in provider.passwordValidationRules
    val unmetChecklistLabels =
        remember(checklistRequirements, hasLocalSpecialRule, stringProvider) {
            checklistRequirements.orEmpty()
                .filterNot { it.isMet }
                .map { it.label }
                .filterNot {
                    hasLocalSpecialRule && it == stringProvider.passwordMissingSpecialCharacter
                }
                .toSet()
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

    val isFormValid = remember(
        displayName,
        email,
        password,
        confirmPassword,
        passwordValidator,
    ) {
        derivedStateOf {
            listOf(
                !provider.isDisplayNameRequired || displayNameValidator.validate(displayName),
                emailValidator.validate(email),
                passwordValidator.validate(password),
                confirmPasswordValidator.validate(confirmPassword)
            ).all { it }
        }
    }

    Scaffold(
        modifier = modifier.exposeTestTagsAsResourceIds(),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringProvider.signupPageTitle)
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag(FirebaseAuthTestTags.SignUp.BACK_BUTTON)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringProvider.backAction
                            )
                        }
                    }
                },
                colors = AuthUITheme.resolvedTopAppBarColors
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (provider.isDisplayNameRequired) {
                AuthTextField(
                    modifier = Modifier.testTag(FirebaseAuthTestTags.SignUp.NAME_FIELD),
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
                modifier = Modifier.testTag(FirebaseAuthTestTags.SignUp.EMAIL_FIELD),
                value = email,
                validator = emailValidator,
                enabled = !isLoading,
                readOnly = isEmailLocked,
                label = {
                    Text(stringProvider.emailHint)
                },
                onValueChange = { text ->
                    onEmailChange(text)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(
                modifier = Modifier.testTag(FirebaseAuthTestTags.SignUp.PASSWORD_FIELD),
                value = password,
                validator = passwordValidator,
                // A message an unmet checklist line already shows is not repeated. isError still
                // marks the field, so the error remains in its semantics.
                errorMessage = "".takeIf { passwordValidator.errorMessage in unmetChecklistLabels },
                enabled = !isLoading,
                isSecureTextField = true,
                label = {
                    Text(stringProvider.passwordHint)
                },
                onValueChange = { text ->
                    passwordEdited = true
                    onPasswordChange(text)
                },
                visibilityToggleModifier = Modifier.testTag(
                    FirebaseAuthTestTags.SignUp.PASSWORD_VISIBILITY_TOGGLE
                )
            )
            checklistRequirements?.let { requirements ->
                Spacer(modifier = Modifier.height(8.dp))
                PasswordRequirementsChecklist(requirements = requirements)
            }
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(
                modifier = Modifier.testTag(FirebaseAuthTestTags.SignUp.CONFIRM_PASSWORD_FIELD),
                value = confirmPassword,
                validator = confirmPasswordValidator,
                enabled = !isLoading,
                isSecureTextField = true,
                label = {
                    Text(stringProvider.confirmPasswordHint)
                },
                onValueChange = { text ->
                    onConfirmPasswordChange(text)
                },
                visibilityToggleModifier = Modifier.testTag(
                    FirebaseAuthTestTags.SignUp.CONFIRM_PASSWORD_VISIBILITY_TOGGLE
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag(FirebaseAuthTestTags.SignUp.SIGN_UP_BUTTON),
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
        )
    }
}