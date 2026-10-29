package com.firebaseui.android.demo.auth.fullcustomization.professional

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebaseui.android.demo.auth.fullcustomization.common.providerIconPainter
import com.firebaseui.android.demo.auth.fullcustomization.theme.FullCustomizationThemeId
import com.firebaseui.android.demo.auth.fullcustomization.theme.LocalFullCustomizationStyle

/** The configuration's Terms and Privacy URLs, for the footer the professional screens pin to the bottom. */
@Immutable
data class LegalLinks(val tosUrl: String?, val privacyPolicyUrl: String?)

val LocalLegalLinks = staticCompositionLocalOf { LegalLinks(null, null) }

/** Whether the screens should take the professional layout rather than the shared one. */
@Composable
fun isProfessional(): Boolean =
    LocalFullCustomizationStyle.current.id == FullCustomizationThemeId.Professional

/**
 * The page every professional screen sits on: a bar at the top, a left-aligned title, the content
 * in one column, and Terms · Privacy pinned to the bottom. Capped at 480dp so it stays a form on a
 * tablet.
 */
@Composable
fun ProPage(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    topBar: @Composable () -> Unit = { ProBrandBar() },
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // heightIn(min = maxHeight) pins the footer to the bottom when everything fits.
                .heightIn(min = maxHeight)
                // The top inset clears the floating theme switcher.
                .padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 16.dp),
        ) {
            topBar()
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = LocalFullCustomizationStyle.current.titleColor,
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))
            ProFooter()
        }
    }
}

/** The product mark and name, top-left, in place of a mascot or a centred greeting. */
@Composable
fun ProBrandBar() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(48.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = "FirebaseUI Demo",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** A "Back" link in the top bar, in place of a full-width "Use a different email" button. */
@Composable
fun ProBackBar(onBack: () -> Unit, enabled: Boolean = true, label: String = "Back") {
    TextButton(
        onClick = onBack,
        enabled = enabled,
        contentPadding = PaddingValues(start = 0.dp, end = 12.dp),
        modifier = Modifier
            .height(48.dp)
            .semantics { contentDescription = "button - back" },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Terms · Privacy, pinned to the bottom of every professional screen. */
@Composable
fun ProFooter() {
    val links = LocalLegalLinks.current
    val uriHandler = LocalUriHandler.current
    val items = listOfNotNull(
        links.tosUrl?.let { "Terms" to it },
        links.privacyPolicyUrl?.let { "Privacy" to it },
    )
    if (items.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.forEachIndexed { index, (label, url) ->
            if (index > 0) {
                Text(text = "·", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            TextButton(onClick = { uriHandler.openUri(url) }) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A full-width outlined "Continue with …" button, for the providers the landing shows inline. */
@Composable
fun ProProviderButton(
    provider: AuthProvider,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val style = LocalFullCustomizationStyle.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = style.providerButtonShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = style.fieldContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(style.buttonHeight)
            .semantics { contentDescription = "button - $label" },
    ) {
        if (provider is AuthProvider.Phone) {
            Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
        } else {
            providerIconPainter(provider)?.let { painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    // Apple's mark is drawn white for its black button; ink it for this outlined one.
                    colorFilter = if (provider is AuthProvider.Apple) {
                        ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                    } else {
                        null
                    },
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(modifier = Modifier.size(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** "or", between the providers and the email form. */
@Composable
fun ProOrDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(text = "or", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
