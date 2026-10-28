package com.firebaseui.android.demo.auth.fullcustomization.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * The demo's three looks. Doggo is the Android Figma section; Grid and Coffee Spark come from the
 * iOS Landing frames (20:5467 and 20:5769) by way of the iOS demo's values, laid out on Doggo's
 * Android sizing.
 */
enum class FullCustomizationThemeId(val label: String) {
    Doggo("Doggo"),
    Grid("Grid"),
    Spark("Coffee Spark"),
}

/** A hard offset shadow: Figma's zero-blur drop shadow. */
@Immutable
data class HardShadow(val color: Color, val offset: DpOffset)

@Immutable
data class ButtonPalette(val container: Color, val content: Color)

/** What a theme changes beyond [colorScheme] and [typography], read via [LocalFullCustomizationStyle]. */
@Immutable
data class FullCustomizationStyle(
    val id: FullCustomizationThemeId,
    val colorScheme: ColorScheme,
    val typography: Typography,
    val titleColor: Color,
    /** Doggo's mascots; the other themes have none. */
    val showsMascot: Boolean,
    /** The form card's fill and shadow. Doggo draws a card; Grid and Coffee Spark leave the form bare. */
    val cardFill: Color,
    val cardShadow: HardShadow?,
    val fieldShape: Shape,
    val fieldBorderWidth: Dp,
    val buttonShape: Shape,
    val buttonBorder: BorderStroke?,
    val buttonShadow: HardShadow?,
    val primaryButton: ButtonPalette,
    val secondaryButton: ButtonPalette,
    val providerButtonShape: Shape,
    val phoneProvider: ButtonPalette,
)

val LocalFullCustomizationStyle = staticCompositionLocalOf<FullCustomizationStyle> {
    error("FullCustomizationStyle is provided by FullCustomizationTheme")
}

fun FullCustomizationThemeId.style(darkTheme: Boolean): FullCustomizationStyle = when (this) {
    FullCustomizationThemeId.Doggo -> doggo(
        if (darkTheme) FullCustomizationDarkColorScheme else FullCustomizationLightColorScheme
    )
    // Grid and Coffee Spark are light-only: their background art is drawn for a light page.
    FullCustomizationThemeId.Grid -> Grid
    FullCustomizationThemeId.Spark -> Spark
}

private fun doggo(scheme: ColorScheme) = FullCustomizationStyle(
    id = FullCustomizationThemeId.Doggo,
    colorScheme = scheme,
    typography = fullCustomizationTypography(
        title = BagelFatOne,
        titleWeight = FontWeight.Normal,
        body = Onest,
        button = Onest,
    ),
    titleColor = scheme.primary,
    showsMascot = true,
    cardFill = scheme.surface,
    cardShadow = HardShadow(scheme.primaryContainer, DpOffset(3.dp, 6.dp)),
    fieldShape = RoundedCornerShape(24.dp),
    fieldBorderWidth = 1.dp,
    buttonShape = RoundedCornerShape(36.dp),
    buttonBorder = null,
    buttonShadow = HardShadow(Color(0xFF5D0B47), DpOffset(2.dp, 4.dp)),
    primaryButton = ButtonPalette(scheme.primary, scheme.onPrimary),
    secondaryButton = ButtonPalette(scheme.primaryContainer, scheme.onPrimaryContainer),
    providerButtonShape = RoundedCornerShape(percent = 50),
    phoneProvider = ButtonPalette(scheme.primary, scheme.onPrimary),
)

private val GridNavy = Color(0xFF2C1067)
private val GridPink = Color(0xFFFF52B3)
private val GridCyan = Color(0xFF00DCE2)
private val GridPage = Color(0xFFF3FBFB)

private val Grid = FullCustomizationStyle(
    id = FullCustomizationThemeId.Grid,
    colorScheme = lightColorScheme(
        primary = GridPink,
        onPrimary = GridNavy,
        primaryContainer = GridPage,
        onPrimaryContainer = GridNavy,
        secondary = GridCyan,
        onSecondary = GridNavy,
        surface = GridPage,
        surfaceBright = GridPage,
        surfaceContainerLowest = GridPage,
        surfaceContainerLow = GridPage,
        surfaceContainer = GridPage,
        surfaceContainerHigh = GridPage,
        surfaceContainerHighest = GridPage,
        onSurface = GridNavy,
        onSurfaceVariant = GridNavy,
        outline = GridNavy,
        outlineVariant = GridCyan,
        background = GridPage,
        error = Color(0xFFBA1A1A),
    ),
    typography = fullCustomizationTypography(
        title = Rowdies,
        titleWeight = FontWeight.Normal,
        body = ChivoMono,
        button = Rowdies,
    ),
    titleColor = GridNavy,
    showsMascot = false,
    cardFill = Color.Transparent,
    cardShadow = null,
    fieldShape = RoundedCornerShape(16.dp),
    fieldBorderWidth = 2.dp,
    buttonShape = RoundedCornerShape(24.dp),
    buttonBorder = BorderStroke(2.dp, GridCyan),
    buttonShadow = HardShadow(GridCyan, DpOffset(3.dp, 6.dp)),
    primaryButton = ButtonPalette(GridPink, GridNavy),
    secondaryButton = ButtonPalette(GridPage, GridNavy),
    providerButtonShape = RoundedCornerShape(12.dp),
    phoneProvider = ButtonPalette(GridCyan, GridNavy),
)

private val SparkGreen = Color(0xFF34A853)
private val SparkPage = Color(0xFFF1F1F1)

private val Spark = FullCustomizationStyle(
    id = FullCustomizationThemeId.Spark,
    colorScheme = lightColorScheme(
        primary = SparkGreen,
        onPrimary = Color.Black,
        primaryContainer = Color(0xFFF3FBFB),
        onPrimaryContainer = Color.Black,
        secondary = Color.Black,
        onSecondary = Color.White,
        surface = SparkPage,
        surfaceBright = SparkPage,
        surfaceContainerLowest = SparkPage,
        surfaceContainerLow = SparkPage,
        surfaceContainer = SparkPage,
        surfaceContainerHigh = SparkPage,
        surfaceContainerHighest = SparkPage,
        onSurface = Color.Black,
        onSurfaceVariant = Color.Black,
        outline = Color.Black,
        outlineVariant = Color.Black,
        background = SparkPage,
        error = Color(0xFFBA1A1A),
    ),
    typography = fullCustomizationTypography(
        title = JetBrainsMono,
        titleWeight = FontWeight.Medium,
        body = JetBrainsMono,
        button = JetBrainsMono,
    ),
    titleColor = Color(0xFF1C1B1B),
    showsMascot = false,
    cardFill = Color.Transparent,
    cardShadow = null,
    fieldShape = RoundedCornerShape(16.dp),
    fieldBorderWidth = 2.dp,
    buttonShape = RoundedCornerShape(40.dp),
    buttonBorder = BorderStroke(2.dp, Color.Black),
    buttonShadow = null,
    primaryButton = ButtonPalette(Color(0xFFFDADEE), Color.Black),
    secondaryButton = ButtonPalette(Color(0xFFF3FBFB), Color.Black),
    providerButtonShape = RoundedCornerShape(40.dp),
    phoneProvider = ButtonPalette(Color(0xFFC6FF00), SparkGreen),
)
