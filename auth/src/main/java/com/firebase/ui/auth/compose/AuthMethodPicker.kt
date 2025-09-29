package com.firebase.ui.auth.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.AuthProviderButton
import com.firebase.ui.auth.compose.configuration.AuthProvider
import com.firebase.ui.auth.compose.configuration.stringprovider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUIAsset

//AuthMethodPicker(
//   providers = listOf(GoogleAuthProvider(), EmailAuthProvider()),
//   onProviderSelected = { provider -> /* ... */ }
//)

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
) {
    val context = LocalContext.current

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
                contentDescription = "AuthMethodPicker logo",
            )
        }
        if (customLayout != null) {
            customLayout(providers, onProviderSelected)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 64.dp) // Space for text
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
            Text(
                "By continuing, you are indicating that you accept our " +
                        "Terms of Service and Privacy Policy.",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
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
            AuthProvider.GenericOAuth(
                providerId = "google.com",
                scopes = emptyList(),
                customParameters = emptyMap(),
                buttonLabel = "Generic Provider",
                buttonIcon = AuthUIAsset.Vector(Icons.Default.Star),
                buttonColor = Color.Gray,
                contentColor = Color.White
            )
        ),
        logo = AuthUIAsset.Resource(R.drawable.fui_ic_check_circle_black_128dp),
        onProviderSelected = { provider ->

        },
    )
}