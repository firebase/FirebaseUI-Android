package com.firebaseui.android.demo.auth.fullcustomization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.configuration.theme.ProviderStyleDefaults

@Composable
fun SheetProviderButton(
    provider: AuthProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = providerSheetLabel(provider)
    val style = when (provider) {
        is AuthProvider.Google -> ProviderStyleDefaults.Google
        is AuthProvider.Facebook -> ProviderStyleDefaults.Facebook
        is AuthProvider.Twitter -> ProviderStyleDefaults.Twitter
        is AuthProvider.Github -> ProviderStyleDefaults.Github
        is AuthProvider.Microsoft -> ProviderStyleDefaults.Microsoft
        is AuthProvider.Yahoo -> ProviderStyleDefaults.Yahoo
        is AuthProvider.Apple -> ProviderStyleDefaults.Apple
        is AuthProvider.Anonymous -> ProviderStyleDefaults.Anonymous
        else -> ProviderStyleDefaults.Email
    }
    val backgroundColor = if (provider is AuthProvider.Phone) {
        MaterialTheme.colorScheme.primary
    } else {
        style.backgroundColor
    }
    val contentColor = if (provider is AuthProvider.Google) Color.Black else style.contentColor
    val hasWhiteBackground = backgroundColor == Color.White

    Button(
        onClick = onClick,
        shape = ProviderButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
        ),
        border = if (hasWhiteBackground) BorderStroke(1.dp, Color.Black) else null,
        contentPadding = PaddingValues(horizontal = 36.dp, vertical = 12.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (provider is AuthProvider.Phone) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                style.icon?.let { icon ->
                    Image(
                        painter = icon.asPainter(),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun providerSheetLabel(provider: AuthProvider): String = when (provider) {
    is AuthProvider.Google -> "Sign in with Google"
    is AuthProvider.Facebook -> "Sign in with Facebook"
    is AuthProvider.Twitter -> "Sign in with X"
    is AuthProvider.Github -> "Sign in with GitHub"
    is AuthProvider.Microsoft -> "Sign in with Microsoft"
    is AuthProvider.Yahoo -> "Sign in with Yahoo"
    is AuthProvider.Apple -> "Sign in with Apple"
    is AuthProvider.Phone -> "Sign in with phone"
    is AuthProvider.Anonymous -> "Continue as guest"
    else -> "Continue"
}

@Composable
private fun AuthUIAsset.asPainter() = when (this) {
    is AuthUIAsset.Resource -> painterResource(resId)
    is AuthUIAsset.Vector -> rememberVectorPainter(image)
}
