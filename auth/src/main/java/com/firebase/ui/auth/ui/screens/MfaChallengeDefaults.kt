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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.configuration.MfaFactor
import com.firebase.ui.auth.compose.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.validators.VerificationCodeValidator
import com.firebase.ui.auth.compose.mfa.MfaChallengeContentState
import com.firebase.ui.auth.compose.ui.components.VerificationCodeInputField

@Composable
internal fun DefaultMfaChallengeContent(state: MfaChallengeContentState) {
    val isSms = state.factorType == MfaFactor.Sms
    val stringProvider = LocalAuthUIStringProvider.current
    val verificationCodeValidator = remember {
        VerificationCodeValidator(stringProvider)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isSms) {
                val phoneLabel = state.maskedPhoneNumber ?: ""
                stringProvider.enterVerificationCodeTitle(phoneLabel)
            } else {
                stringProvider.mfaStepVerifyFactorTitle
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        if (isSms && state.maskedPhoneNumber != null) {
            Text(
                text = stringProvider.mfaStepVerifyFactorSmsHelper,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        VerificationCodeInputField(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            codeLength = 6,
            validator = verificationCodeValidator,
            isError = state.error != null,
            errorMessage = state.error,
            onCodeChange = state.onVerificationCodeChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isSms) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { state.onResendCodeClick?.invoke() },
                    enabled = state.onResendCodeClick != null && !state.isLoading && state.resendTimer == 0
                ) {
                    Text(
                        text = if (state.resendTimer > 0) {
                            val minutes = state.resendTimer / 60
                            val seconds = state.resendTimer % 60
                            val formatted = "$minutes:${String.format(java.util.Locale.ROOT, "%02d", seconds)}"
                            stringProvider.resendCodeTimer(formatted)
                        } else {
                            stringProvider.resendCode
                        }
                    )
                }

                TextButton(
                    onClick = state.onCancelClick,
                    enabled = !state.isLoading
                ) {
                    Text(stringProvider.useDifferentMethodAction)
                }
            }
        } else {
            OutlinedButton(
                onClick = state.onCancelClick,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringProvider.dismissAction)
            }
        }

        Button(
            onClick = state.onVerifyClick,
            enabled = state.isValid && !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(stringProvider.verifyAction)
        }
    }
}
