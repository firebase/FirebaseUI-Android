package com.firebaseui.android.demo.auth.fullcustomization.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.firebase.ui.auth.configuration.theme.AuthUITheme

private val LightPrimary = Color(0xFF864B6F)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFFFD8EB)
private val LightOnPrimaryContainer = Color(0xFF7B3B73)
private val LightInversePrimary = Color(0xFFFAB1DA)
private val LightSecondary = Color(0xFF4C8BFF)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFCCE5FF)
private val LightTertiaryContainer = Color(0xFFFFDDB4)
private val LightSurface = Color(0xFFFFF8F8)
private val LightSurfaceBright = Color(0xFFFFF8F8)
private val LightOnSurface = Color(0xFF211A1D)
private val LightOnSurfaceVariant = Color(0xFFA08B95)
private val LightSurfaceContainer = Color(0xFFF9EAEF)
private val LightSurfaceContainerLow = Color(0xFFFDF0F6)
private val LightOutline = Color(0xFF81737A)
private val LightOutlineVariant = Color(0xFFD3C2C9)
private val LightInverseSurface = Color(0xFF322F35)
private val LightInverseOnSurface = Color(0xFFF5EFF7)

val FullCustomizationLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    inversePrimary = LightInversePrimary,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    tertiaryContainer = LightTertiaryContainer,
    surface = LightSurface,
    surfaceBright = LightSurfaceBright,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerLow = LightSurfaceContainerLow,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
)

/**
 * Doggo's palette sits on Material 3 tones (primary 40, containers 90, surface 98), so its dark
 * scheme takes M3's dark tone for each role: same HCT hue and chroma, tone moved (primary 40 to 80,
 * containers 90 to 30, surface 98 to 6, text 10 to 90).
 */
val FullCustomizationDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFAB1DA),
    onPrimary = Color(0xFF511D3F),
    primaryContainer = Color(0xFF6B3457),
    onPrimaryContainer = Color(0xFFFFD8EB),
    inversePrimary = LightPrimary,
    secondary = Color(0xFFAFC6FF),
    onSecondary = Color(0xFF002D6C),
    surface = Color(0xFF171214),
    surfaceBright = Color(0xFF3D383A),
    surfaceContainerLowest = Color(0xFF110D0F),
    surfaceContainerLow = Color(0xFF1F1A1C),
    surfaceContainer = Color(0xFF231E20),
    surfaceContainerHigh = Color(0xFF2E282B),
    surfaceContainerHighest = Color(0xFF393336),
    background = Color(0xFF171214),
    onSurface = Color(0xFFEDDFE4),
    onSurfaceVariant = Color(0xFFD8C0CB),
    outline = Color(0xFF9C8D94),
    outlineVariant = Color(0xFF4F4349),
    inverseSurface = Color(0xFFEDDFE4),
    inverseOnSurface = Color(0xFF322F35),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun FullCustomizationTheme(
    themeId: FullCustomizationThemeId,
    content: @Composable () -> Unit,
) {
    val style = themeId.style(darkTheme = isSystemInDarkTheme())
    CompositionLocalProvider(LocalFullCustomizationStyle provides style) {
        AuthUITheme(
            theme = AuthUITheme.Default.copy(
                colorScheme = style.colorScheme,
                typography = style.typography,
                providerButtonShape = style.providerButtonShape,
            ),
            content = content,
        )
    }
}
