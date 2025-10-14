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

package com.firebase.ui.auth.compose.ui.screens.phone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.firebase.ui.auth.compose.configuration.validators.VerificationCodeValidator
import com.firebase.ui.auth.compose.ui.components.TermsAndPrivacyForm
import com.firebase.ui.auth.compose.ui.components.VerificationCodeInputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterVerificationCodeUI(
    modifier: Modifier = Modifier,
    configuration: AuthUIConfiguration,
    isLoading: Boolean,
    verificationCode: String,
    fullPhoneNumber: String,
    resendTimer: Int,
    onVerificationCodeChange: (String) -> Unit,
    onVerifyCodeClick: () -> Unit,
    onResendCodeClick: () -> Unit,
    onChangeNumberClick: () -> Unit,
) {
    val context = LocalContext.current
    val stringProvider = DefaultAuthUIStringProvider(context)
    val verificationCodeValidator = remember {
        VerificationCodeValidator(stringProvider)
    }

    val isFormValid = remember(verificationCode) {
        derivedStateOf {
            verificationCodeValidator.validate(verificationCode)
        }
    }

    val resendEnabled = resendTimer == 0 && !isLoading

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringProvider.verifyPhoneNumber)
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
            Text(
                text = stringProvider.enterVerificationCodeTitle(fullPhoneNumber),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                modifier = Modifier.align(Alignment.Start),
                onClick = onChangeNumberClick,
                enabled = !isLoading,
                contentPadding = PaddingValues.Zero
            ) {
                Text(
                    text = stringProvider.changePhoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            VerificationCodeInputField(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                validator = verificationCodeValidator,
                onCodeChange = onVerificationCodeChange
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                modifier = Modifier.align(Alignment.Start),
                onClick = onResendCodeClick,
                enabled = resendEnabled,
                contentPadding = PaddingValues.Zero
            ) {
                Text(
                    text = if (resendTimer > 0) {
                        stringProvider.resendCodeTimer(resendTimer)
                    } else {
                        stringProvider.resendCode
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    textDecoration = if (resendEnabled) TextDecoration.Underline else TextDecoration.None
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .align(Alignment.End),
            ) {
                Button(
                    onClick = onVerifyCodeClick,
                    enabled = !isLoading && isFormValid.value,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                        )
                    } else {
                        Text(stringProvider.verifyCode.uppercase())
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
fun PreviewEnterVerificationCodeUI() {
    val applicationContext = LocalContext.current
    val provider = AuthProvider.Phone(
        defaultNumber = null,
        defaultCountryCode = null,
        allowedCountries = null,
        timeout = 60L,
        isInstantVerificationEnabled = true
    )

    AuthUITheme {
        EnterVerificationCodeUI(
            configuration = authUIConfiguration {
                context = applicationContext
                providers { provider(provider) }
                tosUrl = ""
                privacyPolicyUrl = ""
            },
            isLoading = false,
            verificationCode = "",
            fullPhoneNumber = "+1234567890",
            resendTimer = 30,
            onVerificationCodeChange = {},
            onVerifyCodeClick = {},
            onResendCodeClick = {},
            onChangeNumberClick = {},
        )
    }
}
