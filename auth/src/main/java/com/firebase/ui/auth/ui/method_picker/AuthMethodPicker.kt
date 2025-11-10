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

package com.firebase.ui.auth.compose.ui.method_picker

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.compose.ui.components.AuthProviderButton

/**
 * Renders the provider selection screen.
 *
 * **Example usage:**
 * ```kotlin
 * AuthMethodPicker(
 *     providers = listOf(
 *      AuthProvider.Google(),
 *      AuthProvider.Email(),
 *     ),
 *     onProviderSelected = { provider -> /* ... */ }
 * )
 * ```
 *
 * @param modifier A modifier for the screen layout.
 * @param providers The list of providers to display.
 * @param logo An optional logo to display.
 * @param onProviderSelected A callback when a provider is selected.
 * @param customLayout An optional custom layout composable for the provider buttons.
 * @param termsOfServiceUrl The URL for the Terms of Service.
 * @param privacyPolicyUrl The URL for the Privacy Policy.
 *
 * @since 10.0.0
 */
@Composable
fun AuthMethodPicker(
    modifier: Modifier = Modifier,
    providers: List<AuthProvider>,
    logo: AuthUIAsset? = null,
    onProviderSelected: (AuthProvider) -> Unit,
    customLayout: @Composable ((List<AuthProvider>, (AuthProvider) -> Unit) -> Unit)? = null,
    termsOfServiceUrl: String? = null,
    privacyPolicyUrl: String? = null,
) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val stringProvider = LocalAuthUIStringProvider.current

    Column(
        modifier = modifier
    ) {
        logo?.let {
            Image(
                modifier = Modifier
                    .weight(0.4f)
                    .align(Alignment.CenterHorizontally),
                painter = it.painter,
                contentDescription = if (inPreview) ""
                else stringResource(R.string.fui_auth_method_picker_logo)
            )
        }
        if (customLayout != null) {
            customLayout(providers, onProviderSelected)
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f),
            ) {
                val paddingWidth = maxWidth.value * 0.23
                LazyColumn(
                    modifier = Modifier
                        .padding(horizontal = paddingWidth.dp)
                        .testTag("AuthMethodPicker LazyColumn"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    itemsIndexed(providers) { index, provider ->
                        Box(
                            modifier = Modifier
                                .padding(bottom = if (index < providers.lastIndex) 16.dp else 0.dp)
                        ) {
                            AuthProviderButton(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                onClick = {
                                    onProviderSelected(provider)
                                },
                                provider = provider,
                                stringProvider = LocalAuthUIStringProvider.current
                            )
                        }
                    }
                }
            }
        }
        AnnotatedStringResource(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp),
            context = context,
            inPreview = inPreview,
            previewText = "By continuing, you accept our Terms of Service and Privacy Policy.",
            text = stringProvider.tosAndPrivacyPolicy(
                termsOfServiceLabel = stringProvider.termsOfService,
                privacyPolicyLabel = stringProvider.privacyPolicy
            ),
            links = arrayOf(
                stringProvider.termsOfService to (termsOfServiceUrl ?: ""),
                stringProvider.privacyPolicy to (privacyPolicyUrl ?: "")
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAuthMethodPicker() {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AuthMethodPicker(
            providers = listOf(
                AuthProvider.Email(
                    emailLinkActionCodeSettings = null,
                    passwordValidationRules = emptyList()
                ),
                AuthProvider.Phone(
                    defaultNumber = null,
                    defaultCountryCode = null,
                    allowedCountries = null,
                ),
                AuthProvider.Google(
                    scopes = emptyList(),
                    serverClientId = null
                ),
                AuthProvider.Facebook(),
                AuthProvider.Twitter(
                    customParameters = emptyMap()
                ),
                AuthProvider.Github(
                    customParameters = emptyMap()
                ),
                AuthProvider.Microsoft(
                    tenant = null,
                    customParameters = emptyMap()
                ),
                AuthProvider.Yahoo(
                    customParameters = emptyMap()
                ),
                AuthProvider.Apple(
                    locale = null,
                    customParameters = emptyMap()
                ),
                AuthProvider.Anonymous,
            ),
            logo = AuthUIAsset.Resource(R.drawable.fui_ic_check_circle_black_128dp),
            onProviderSelected = { provider ->

            },
            termsOfServiceUrl = "https://example.com/terms",
            privacyPolicyUrl = "https://example.com/privacy"
        )
    }
}