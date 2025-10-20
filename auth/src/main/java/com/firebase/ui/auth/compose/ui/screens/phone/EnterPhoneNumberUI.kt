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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.configuration.AuthUIConfiguration
import com.firebase.ui.auth.compose.configuration.authUIConfiguration
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.firebase.ui.auth.compose.configuration.validators.PhoneNumberValidator
import com.firebase.ui.auth.compose.data.CountryData
import com.firebase.ui.auth.compose.data.CountryUtils
import com.firebase.ui.auth.compose.ui.components.AuthTextField
import com.firebase.ui.auth.compose.ui.components.CountrySelector
import com.firebase.ui.auth.compose.ui.components.TermsAndPrivacyForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterPhoneNumberUI(
    modifier: Modifier = Modifier,
    configuration: AuthUIConfiguration,
    isLoading: Boolean,
    phoneNumber: String,
    selectedCountry: CountryData,
    onPhoneNumberChange: (String) -> Unit,
    onCountrySelected: (CountryData) -> Unit,
    onSendCodeClick: () -> Unit,
    title: String? = null,
) {
    val context = LocalContext.current
    val provider = configuration.providers.filterIsInstance<AuthProvider.Phone>().first()
    val stringProvider = DefaultAuthUIStringProvider(context)
    val phoneNumberValidator = remember(selectedCountry) {
        PhoneNumberValidator(stringProvider, selectedCountry)
    }

    val isFormValid = remember(selectedCountry, phoneNumber) {
        derivedStateOf {
            phoneNumberValidator.validate(phoneNumber)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(title ?: stringProvider.signInWithPhone)
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
            Text(stringProvider.enterPhoneNumberTitle)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(
                value = phoneNumber,
                validator = phoneNumberValidator,
                enabled = !isLoading,
                label = {
                    Text(stringProvider.phoneNumberHint)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                leadingIcon = {
                    CountrySelector(
                        selectedCountry = selectedCountry,
                        onCountrySelected = onCountrySelected,
                        enabled = !isLoading,
                        allowedCountries = provider.allowedCountries?.toSet()
                    )
                },
                onValueChange = {
                    onPhoneNumberChange(it)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .align(Alignment.End),
            ) {
                Button(
                    onClick = onSendCodeClick,
                    enabled = !isLoading && isFormValid.value,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(16.dp)
                        )
                    } else {
                        Text(stringProvider.sendVerificationCode.uppercase())
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
fun PreviewEnterPhoneNumberUI() {
    val applicationContext = LocalContext.current
    val provider = AuthProvider.Phone(
        defaultNumber = null,
        defaultCountryCode = null,
        allowedCountries = null,
        timeout = 60L,
        isInstantVerificationEnabled = true
    )

    AuthUITheme {
        EnterPhoneNumberUI(
            configuration = authUIConfiguration {
                context = applicationContext
                providers { provider(provider) }
                tosUrl = ""
                privacyPolicyUrl = ""
            },
            isLoading = false,
            phoneNumber = "",
            selectedCountry = CountryUtils.getDefaultCountry(),
            onPhoneNumberChange = {},
            onCountrySelected = {},
            onSendCodeClick = {},
        )
    }
}
