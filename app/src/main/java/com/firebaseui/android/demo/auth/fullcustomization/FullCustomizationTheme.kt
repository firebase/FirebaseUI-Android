package com.firebaseui.android.demo.auth.fullcustomization

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.firebase.ui.auth.configuration.theme.AuthUITheme
import kotlin.math.max
import kotlin.math.min

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

val FullCustomizationDarkColorScheme = darkColorScheme(
    primary = LightPrimary.withLightness(0.78f),
    onPrimary = LightOnPrimary.withLightness(0.18f),
    primaryContainer = LightPrimaryContainer.withLightness(0.28f),
    onPrimaryContainer = LightOnPrimaryContainer.withLightness(0.88f),
    inversePrimary = LightPrimary,
    secondary = LightSecondary.withLightness(0.78f),
    onSecondary = LightOnSecondary.withLightness(0.18f),
    secondaryContainer = LightSecondaryContainer.withLightness(0.28f),
    tertiaryContainer = LightTertiaryContainer.withLightness(0.28f),
    surface = LightSurface.withLightness(0.10f),
    surfaceBright = LightSurfaceBright.withLightness(0.20f),
    onSurface = LightOnSurface.withLightness(0.88f),
    onSurfaceVariant = LightOnSurfaceVariant.withLightness(0.75f),
    surfaceContainer = LightSurfaceContainer.withLightness(0.13f),
    surfaceContainerLow = LightSurfaceContainerLow.withLightness(0.11f),
    outline = LightOutline.withLightness(0.55f),
    outlineVariant = LightOutlineVariant.withLightness(0.30f),
    inverseSurface = LightSurface.withLightness(0.90f),
    inverseOnSurface = LightOnSurface.withLightness(0.15f),
)

@Composable
fun FullCustomizationTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) {
        FullCustomizationDarkColorScheme
    } else {
        FullCustomizationLightColorScheme
    }
    AuthUITheme(
        theme = AuthUITheme.Default.copy(
            colorScheme = colorScheme,
            typography = FullCustomizationTypography,
            providerButtonShape = ProviderButtonShape,
        ),
        content = content,
    )
}

private fun Color.withLightness(newLightness: Float): Color {
    val (h, s, _) = toHsl()
    return hslToColor(h, s, newLightness.coerceIn(0f, 1f), alpha)
}

private fun Color.toHsl(): Triple<Float, Float, Float> {
    val r = red
    val g = green
    val b = blue
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val l = (maxC + minC) / 2f
    if (maxC == minC) return Triple(0f, 0f, l)
    val d = maxC - minC
    val s = if (l > 0.5f) d / (2f - maxC - minC) else d / (maxC + minC)
    val h = when (maxC) {
        r -> ((g - b) / d + (if (g < b) 6f else 0f))
        g -> ((b - r) / d + 2f)
        else -> ((r - g) / d + 4f)
    } / 6f
    return Triple(h, s, l)
}

private fun hslToColor(h: Float, s: Float, l: Float, alpha: Float): Color {
    if (s == 0f) return Color(l, l, l, alpha)
    fun hueToRgb(p: Float, q: Float, tIn: Float): Float {
        var t = tIn
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    val r = hueToRgb(p, q, h + 1f / 3f)
    val g = hueToRgb(p, q, h)
    val b = hueToRgb(p, q, h - 1f / 3f)
    return Color(r, g, b, alpha)
}
