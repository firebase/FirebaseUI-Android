package com.firebase.ui.auth.compose.ui.method_picker

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.firebase.ui.auth.compose.configuration.AuthProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        logo?.let {
            Image(
                modifier = Modifier
                    .weight(0.4f),
                painter = it.painter,
                contentDescription = if (inPreview) ""
                else stringResource(R.string.fui_auth_method_picker_logo)
            )
        }
        if (customLayout != null) {
            customLayout(providers, onProviderSelected)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("AuthMethodPicker LazyColumn"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(providers.size) { index ->
                    val provider = providers[index]
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                    ) {
                        AuthProviderButton(
                            onClick = {
                                onProviderSelected(provider)
                            },
                            provider = provider,
                            stringProvider = DefaultAuthUIStringProvider(context)
                        )
                    }
                }
            }
        }
        AnnotatedStringResource(
            context = context,
            inPreview = inPreview,
            previewText = "By continuing, you accept our Terms of Service and Privacy Policy.",
            modifier = Modifier.padding(vertical = 16.dp),
            id = R.string.fui_tos_and_pp,
            links = arrayOf(
                "Terms of Service" to (termsOfServiceUrl ?: ""),
                "Privacy Policy" to (privacyPolicyUrl ?: "")
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAuthMethodPicker() {
    AuthMethodPicker(
        providers = listOf(
            AuthProvider.Email(
                actionCodeSettings = null,
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