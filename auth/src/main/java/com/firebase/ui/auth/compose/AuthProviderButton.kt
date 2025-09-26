package com.firebase.ui.auth.compose

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.compose.configuration.AuthProvider
import com.firebase.ui.auth.compose.configuration.Provider
import com.firebase.ui.auth.compose.configuration.stringprovider.AuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.stringprovider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme

/**
 * A customizable button for an authentication provider.
 *
 * This button displays the icon and name of an authentication provider (e.g., Google, Facebook).
 * It is designed to be used within a list of sign-in options. The button's appearance can be
 * customized using the [style] parameter, and its text is localized via the [stringProvider].
 *
 * **Example usage:**
 * ```kotlin
 * AuthProviderButton(
 *     provider = AuthProvider.Facebook(),
 *     onClick = { /* Handle Facebook sign-in */ },
 *     stringProvider = DefaultAuthUIStringProvider(LocalContext.current)
 * )
 * ```
 *
 * @param modifier A modifier for the button
 * @param provider The provider to represent.
 * @param onClick A callback when the button is clicked
 * @param enabled If the button is enabled. Defaults to true.
 * @param style Optional custom styling for the button.
 * @param stringProvider The [AuthUIStringProvider] for localized strings
 *
 * @since 10.0.0
 */
@Composable
fun AuthProviderButton(
    modifier: Modifier = Modifier,
    provider: AuthProvider,
    onClick: () -> Unit,
    enabled: Boolean = true,
    style: AuthUITheme.ProviderStyle? = null,
    stringProvider: AuthUIStringProvider,
) {
    val providerStyle = resolveProviderStyle(provider, style)
    val providerText = resolveProviderLabel(provider, stringProvider)

    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = providerStyle.backgroundColor,
            contentColor = providerStyle.contentColor,
        ),
        shape = providerStyle.shape,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = providerStyle.elevation
        ),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val providerIcon = providerStyle.icon
            if (providerIcon != null) {
                val iconTint = providerStyle.iconTint
                if (iconTint != null) {
                    Icon(
                        painter = providerIcon.painter,
                        contentDescription = providerText,
                        tint = iconTint
                    )
                } else {
                    Image(
                        painter = providerIcon.painter,
                        contentDescription = providerText
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = providerText
            )
        }
    }
}

internal fun resolveProviderStyle(
    provider: AuthProvider,
    style: AuthUITheme.ProviderStyle?,
): AuthUITheme.ProviderStyle {
    if (style != null) return style

    val defaultStyle =
        AuthUITheme.Default.providerStyles[provider.providerId] ?: AuthUITheme.ProviderStyle.Empty

    return if (provider is AuthProvider.GenericOAuth) {
        AuthUITheme.ProviderStyle(
            icon = provider.buttonIcon ?: defaultStyle.icon,
            backgroundColor = provider.buttonColor ?: defaultStyle.backgroundColor,
            contentColor = provider.contentColor ?: defaultStyle.contentColor,
        )
    } else {
        defaultStyle
    }
}

internal fun resolveProviderLabel(
    provider: AuthProvider,
    stringProvider: AuthUIStringProvider
): String = when (provider) {
    is AuthProvider.GenericOAuth -> provider.buttonLabel
    else -> when (Provider.fromId(provider.providerId)) {
        Provider.GOOGLE -> stringProvider.signInWithGoogle
        Provider.FACEBOOK -> stringProvider.signInWithFacebook
        Provider.TWITTER -> stringProvider.signInWithTwitter
        Provider.GITHUB -> stringProvider.signInWithGithub
        Provider.EMAIL -> stringProvider.signInWithEmail
        Provider.PHONE -> stringProvider.signInWithPhone
        Provider.ANONYMOUS -> stringProvider.signInAnonymously
        Provider.MICROSOFT -> stringProvider.signInWithMicrosoft
        Provider.YAHOO -> stringProvider.signInWithYahoo
        Provider.APPLE -> stringProvider.signInWithApple
        null -> "Unknown Provider"
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAuthProviderButton() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthProviderButton(
            provider = AuthProvider.Email(
                actionCodeSettings = null,
                passwordValidationRules = emptyList()
            ),
            onClick = {},
            stringProvider = DefaultAuthUIStringProvider(context)
        )
        AuthProviderButton(
            provider = AuthProvider.Phone(
                defaultNumber = null,
                defaultCountryCode = null,
                allowedCountries = null,
            ),
            onClick = {},
            stringProvider = DefaultAuthUIStringProvider(context)
        )
        AuthProviderButton(
            provider = AuthProvider.Google(
                scopes = emptyList(),
                serverClientId = null
            ),
            onClick = {},
            stringProvider = DefaultAuthUIStringProvider(context)
        )
        AuthProviderButton(
            provider = AuthProvider.Facebook(),
            onClick = {},
            stringProvider = DefaultAuthUIStringProvider(context)
        )
        AuthProviderButton(
            provider = AuthProvider.Twitter(
                customParameters = emptyMap()
            ),
            onClick = {},
            stringProvider = DefaultAuthUIStringProvider(context)
        )
        AuthProviderButton(
            provider = AuthProvider.Github(
                customParameters = emptyMap()
            ),
            onClick = {},
            stringProvider = DefaultAuthUIStringProvider(context)
        )
        AuthProviderButton(
            provider = AuthProvider.Microsoft(
                tenant = null,
                customParameters = emptyMap()
            ),
            onClick = {},
            stringProvider = DefaultAuthUIStringProvider(context)
        )
        AuthProviderButton(
            provider = AuthProvider.Yahoo(
                customParameters = emptyMap()
            ),
            onClick = {},
            stringProvider = DefaultAuthUIStringProvider(context)
        )
        AuthProviderButton(
            provider = AuthProvider.Apple(
                locale = null,
                customParameters = emptyMap()
            ),
            onClick = {},
            stringProvider = DefaultAuthUIStringProvider(context)
        )
        AuthProviderButton(
            provider = AuthProvider.Anonymous,
            onClick = {},
            stringProvider = DefaultAuthUIStringProvider(context)
        )
        AuthProviderButton(
            provider = AuthProvider.GenericOAuth(
                providerId = "google.com",
                scopes = emptyList(),
                customParameters = emptyMap(),
                buttonLabel = "Sign in with Generic",
                buttonIcon = AuthUIAsset.Vector(Icons.Default.Star),
                buttonColor = Color.Gray,
                contentColor = Color.White
            ),
            onClick = {},
            stringProvider = DefaultAuthUIStringProvider(context)
        )
        AuthProviderButton(
            provider = AuthProvider.Google(
                scopes = emptyList(),
                serverClientId = null
            ),
            onClick = {},
            style = AuthUITheme.Default.providerStyles[Provider.MICROSOFT.id],
            stringProvider = DefaultAuthUIStringProvider(context)
        )
        AuthProviderButton(
            provider = AuthProvider.GenericOAuth(
                providerId = "unknown_provider",
                scopes = emptyList(),
                customParameters = emptyMap(),
                buttonLabel = "Sign in with Lego",
                buttonIcon = null,
                buttonColor = null,
                contentColor = null,
            ),
            onClick = {},
            stringProvider = DefaultAuthUIStringProvider(context)
        )
    }
}
