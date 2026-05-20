package com.firebaseui.android.demo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.AuthException
import com.firebase.ui.auth.FirebaseAuthUI
import com.firebase.ui.auth.configuration.authUIConfiguration
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.configuration.theme.AuthUITheme
import com.firebase.ui.auth.configuration.theme.ProviderStyleDefaults
import com.firebase.ui.auth.ui.components.AuthProviderButton
import com.firebase.ui.auth.ui.screens.FirebaseAuthScreen

class CustomMethodPickerDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authUI = FirebaseAuthUI.getInstance()

        val configuration = authUIConfiguration {
            context = applicationContext
            logo = AuthUIAsset.Resource(R.drawable.firebase_auth)
            tosUrl = "https://policies.google.com/terms"
            privacyPolicyUrl = "https://policies.google.com/privacy"
            providers {
                provider(
                    AuthProvider.Google(
                        scopes = listOf("email"),
                        serverClientId = "406099696497-a12gakvts4epfk5pkio7dphc1anjiggc.apps.googleusercontent.com",
                    )
                )
                provider(AuthProvider.Apple(customParameters = emptyMap(), locale = null))
                provider(AuthProvider.Facebook())
                provider(AuthProvider.Twitter(customParameters = emptyMap()))
                provider(AuthProvider.Github(customParameters = emptyMap()))
                provider(AuthProvider.Microsoft(tenant = null, customParameters = emptyMap()))
                provider(AuthProvider.Yahoo(customParameters = emptyMap()))
                provider(
                    AuthProvider.GenericOAuth(
                        providerName = "Discord",
                        providerId = "oidc.discord",
                        scopes = emptyList(),
                        customParameters = emptyMap(),
                        buttonLabel = "Sign in with Discord",
                        buttonIcon = AuthUIAsset.Resource(R.drawable.ic_discord_24dp),
                        buttonColor = Color(0xFF5865F2),
                        contentColor = Color.White
                    )
                )
                provider(
                    AuthProvider.GenericOAuth(
                        providerName = "LINE",
                        providerId = "oidc.line",
                        scopes = emptyList(),
                        customParameters = emptyMap(),
                        buttonLabel = "Sign in with LINE",
                        buttonIcon = AuthUIAsset.Resource(R.drawable.ic_line_logo_24dp),
                        buttonColor = Color(0xFF06C755),
                        contentColor = Color.White
                    )
                )
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
                provider(
                    AuthProvider.Phone(
                        defaultNumber = null,
                        defaultCountryCode = null,
                        allowedCountries = null
                    )
                )
                provider(AuthProvider.Anonymous)
            }
        }

        setContent {
            AuthUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FirebaseAuthScreen(
                        configuration = configuration,
                        authUI = authUI,
                        onSignInSuccess = { result ->
                            Log.d("CustomMethodPickerDemo", "Auth success: ${result.user?.uid}")
                        },
                        onSignInFailure = { exception: AuthException ->
                            Log.e("CustomMethodPickerDemo", "Auth failed", exception)
                        },
                        onSignInCancelled = {
                            Log.d("CustomMethodPickerDemo", "Auth cancelled")
                        },
                        customMethodPickerLayout = { providers, onProviderSelected ->
                            SpotlightMethodPicker(
                                providers = providers,
                                onProviderSelected = onProviderSelected
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SpotlightMethodPicker(
    providers: List<AuthProvider>,
    onProviderSelected: (AuthProvider) -> Unit,
) {
    val stringProvider = LocalAuthUIStringProvider.current

    val groups = providers.groupBy {
        when (it) {
            is AuthProvider.Google, is AuthProvider.Apple -> "featured"
            is AuthProvider.Email, is AuthProvider.Phone -> "credential"
            is AuthProvider.Anonymous -> "anonymous"
            else -> "social"
        }
    }
    val featured = groups.getOrElse("featured") { emptyList() }
    val social = groups.getOrElse("social") { emptyList() }
    val credential = groups.getOrElse("credential") { emptyList() }
    val anonymous = groups["anonymous"]?.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose how you'd like to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        items(featured) { provider ->
            AuthProviderButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                provider = provider,
                onClick = { onProviderSelected(provider) },
                stringProvider = stringProvider
            )
        }

        if (social.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(social) { provider ->
                        val style = styleForProvider(provider)
                        ProviderIconButton(
                            style = style,
                            contentDescription = provider.providerId,
                            onClick = { onProviderSelected(provider) }
                        )
                    }
                }
            }
        }

        if (credential.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            items(credential) { provider ->
                AuthProviderButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    provider = provider,
                    onClick = { onProviderSelected(provider) },
                    stringProvider = stringProvider
                )
            }
        }

        anonymous?.let {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onProviderSelected(it) }) {
                    Text("Continue as guest")
                }
            }
        }
    }
}

@Composable
private fun ProviderIconButton(
    style: AuthUITheme.ProviderStyle,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = style.backgroundColor),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = style.elevation)
    ) {
        style.icon?.let { asset ->
            val painter = asset.asPainter()
            val tint = style.iconTint
            if (tint != null) {
                Icon(
                    painter = painter,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun AuthUIAsset.asPainter(): Painter = when (this) {
    is AuthUIAsset.Resource -> painterResource(resId)
    is AuthUIAsset.Vector -> rememberVectorPainter(image)
}

private fun styleForProvider(provider: AuthProvider): AuthUITheme.ProviderStyle = when (provider) {
    is AuthProvider.Facebook -> ProviderStyleDefaults.Facebook
    is AuthProvider.Twitter -> ProviderStyleDefaults.Twitter
    is AuthProvider.Github -> ProviderStyleDefaults.Github
    is AuthProvider.Microsoft -> ProviderStyleDefaults.Microsoft
    is AuthProvider.Yahoo -> ProviderStyleDefaults.Yahoo
    is AuthProvider.GenericOAuth -> AuthUITheme.ProviderStyle(
        icon = provider.buttonIcon,
        backgroundColor = provider.buttonColor ?: Color(0xFF666666),
        contentColor = provider.contentColor ?: Color.White
    )
    else -> AuthUITheme.ProviderStyle(
        icon = null,
        backgroundColor = Color(0xFF666666),
        contentColor = Color.White
    )
}
