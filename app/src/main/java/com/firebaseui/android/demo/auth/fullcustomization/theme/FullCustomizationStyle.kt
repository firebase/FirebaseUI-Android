package com.firebaseui.android.demo.auth.fullcustomization.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The demo's looks. Doggo is the Android Figma section; Grid and Coffee Spark come from the iOS
 * Landing frames (20:5467 and 20:5769) by way of the iOS demo's values, laid out on Doggo's
 * Android sizing. Professional has no Figma frame: its palette is Material 3's fidelity scheme for
 * a navy seed (#1E3A8A), with the seed itself kept for buttons and links.
 *
 * Figma has no dark frames. Each dark variant keeps a colour's HCT hue and chroma and moves only
 * its tone: pages to tone 6 (never pure black), text to 90, Doggo's M3 roles to M3's dark tones.
 * Grid's and Coffee Spark's neon accents keep their own tone, since each already clears 4.5:1 on
 * the dark page. On their light pages the same rule runs the other way: accents used as text keep
 * hue and chroma and darken until they clear 4.5:1, while button fills stay Figma's neon.
 */
enum class FullCustomizationThemeId(val label: String) {
    Doggo("Doggo"),
    Grid("Grid"),
    Spark("Coffee Spark"),
    Professional("Professional"),
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
    /** Fields and the chips drawn like them, such as the country code. */
    val fieldContainer: Color,
    val buttonShape: Shape,
    val buttonBorder: BorderStroke?,
    val buttonShadow: HardShadow?,
    val primaryButton: ButtonPalette,
    val secondaryButton: ButtonPalette,
    val providerButtonShape: Shape,
    val phoneProvider: ButtonPalette,
    /** The floating pills: the theme switcher and Coffee Spark's headline. */
    val pill: ButtonPalette,
    /** A wash of the page colour over the background art, so light art sits back in dark mode. */
    val artScrim: Color,
    val buttonHeight: Dp = 80.dp,
)

val LocalFullCustomizationStyle = staticCompositionLocalOf<FullCustomizationStyle> {
    error("FullCustomizationStyle is provided by FullCustomizationTheme")
}

fun FullCustomizationThemeId.style(darkTheme: Boolean): FullCustomizationStyle = when (this) {
    FullCustomizationThemeId.Doggo -> if (darkTheme) DoggoDark else DoggoLight
    FullCustomizationThemeId.Grid -> if (darkTheme) GridDark else GridLight
    FullCustomizationThemeId.Spark -> if (darkTheme) SparkDark else SparkLight
    FullCustomizationThemeId.Professional -> if (darkTheme) ProfessionalDark else ProfessionalLight
}

/** A theme's page and its tone-mapped neighbours, light or dark. */
private class Neutrals(
    val page: Color,
    val ink: Color,
    /** Fields, secondary buttons and other surfaces one step above the page. */
    val raised: Color,
    /** surfaceContainerLowest to surfaceContainerHighest, for sheets and dialogs. */
    val containers: List<Color>,
    val error: Color,
)

private fun ColorScheme.withNeutrals(n: Neutrals) = copy(
    surface = n.page,
    surfaceBright = n.page,
    surfaceContainerLowest = n.containers[0],
    surfaceContainerLow = n.containers[1],
    surfaceContainer = n.containers[2],
    surfaceContainerHigh = n.containers[3],
    surfaceContainerHighest = n.containers[4],
    background = n.page,
    onSurface = n.ink,
    onSurfaceVariant = n.ink,
    outline = n.ink,
    primaryContainer = n.raised,
    onPrimaryContainer = n.ink,
    error = n.error,
)

private fun doggo(
    scheme: ColorScheme,
    cardFill: Color,
    fieldContainer: Color,
    buttonShadow: Color,
    pill: ButtonPalette,
    artScrim: Color,
) = FullCustomizationStyle(
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
    cardFill = cardFill,
    cardShadow = HardShadow(scheme.primaryContainer, DpOffset(3.dp, 6.dp)),
    fieldShape = RoundedCornerShape(24.dp),
    fieldBorderWidth = 1.dp,
    fieldContainer = fieldContainer,
    buttonShape = RoundedCornerShape(36.dp),
    buttonBorder = null,
    buttonShadow = HardShadow(buttonShadow, DpOffset(2.dp, 4.dp)),
    primaryButton = ButtonPalette(scheme.primary, scheme.onPrimary),
    secondaryButton = ButtonPalette(scheme.primaryContainer, scheme.onPrimaryContainer),
    providerButtonShape = RoundedCornerShape(percent = 50),
    phoneProvider = ButtonPalette(scheme.primary, scheme.onPrimary),
    pill = pill,
    artScrim = artScrim,
)

private val DoggoLight = doggo(
    scheme = FullCustomizationLightColorScheme,
    cardFill = FullCustomizationLightColorScheme.surface,
    fieldContainer = Color.White,
    buttonShadow = Color(0xFF5D0B47),
    pill = ButtonPalette(Color.White, Color(0xFF211A1D)),
    artScrim = Color.Transparent,
)

private val DoggoDark = doggo(
    scheme = FullCustomizationDarkColorScheme,
    // Raised surfaces get lighter in dark mode: the card one step above the page, fields one above it.
    cardFill = Color(0xFF231E20),
    fieldContainer = Color(0xFF2E282B),
    // Tone 30 of the light shadow's hue, so it still sits below the tone 80 button.
    buttonShadow = Color(0xFF79265F),
    pill = ButtonPalette(Color(0xFF2E282B), Color(0xFFEDDFE4)),
    artScrim = Color(0xFF171214).copy(alpha = 0.92f),
)

private val GridNavy = Color(0xFF2C1067)
private val GridPink = Color(0xFFFF52B3)
private val GridCyan = Color(0xFF00DCE2)

private fun grid(
    n: Neutrals,
    scheme: ColorScheme,
    /** Links and accent text; the pink button fill stays [GridPink]. */
    link: Color,
    fieldContainer: Color,
    pill: Color,
    artScrim: Color,
) =
    FullCustomizationStyle(
        id = FullCustomizationThemeId.Grid,
        colorScheme = scheme.withNeutrals(n).copy(
            primary = link,
            onPrimary = GridNavy,
            secondary = GridCyan,
            onSecondary = GridNavy,
            outlineVariant = GridCyan,
        ),
        typography = fullCustomizationTypography(
            title = Rowdies,
            titleWeight = FontWeight.Normal,
            body = ChivoMono,
            button = Rowdies,
        ),
        titleColor = n.ink,
        showsMascot = false,
        cardFill = Color.Transparent,
        cardShadow = null,
        fieldShape = RoundedCornerShape(16.dp),
        fieldBorderWidth = 2.dp,
        fieldContainer = fieldContainer,
        buttonShape = RoundedCornerShape(24.dp),
        buttonBorder = BorderStroke(2.dp, GridCyan),
        buttonShadow = HardShadow(GridCyan, DpOffset(3.dp, 6.dp)),
        // Navy on the pink fill reads in both modes, so the primary button doesn't change.
        primaryButton = ButtonPalette(GridPink, GridNavy),
        secondaryButton = ButtonPalette(n.raised, n.ink),
        providerButtonShape = RoundedCornerShape(12.dp),
        phoneProvider = ButtonPalette(GridCyan, GridNavy),
        pill = ButtonPalette(pill, n.ink),
        artScrim = artScrim,
    )

private val GridLight = grid(
    n = Neutrals(
        page = Color(0xFFF3FBFB),
        ink = GridNavy,
        raised = Color(0xFFF3FBFB),
        containers = List(5) { Color(0xFFF3FBFB) },
        error = Color(0xFFBA1A1A),
    ),
    scheme = lightColorScheme(),
    link = Color(0xFFD0288D),
    fieldContainer = Color.White,
    pill = Color.White,
    artScrim = Color.Transparent,
)

private val GridDark = grid(
    n = Neutrals(
        page = Color(0xFF141218),
        ink = Color(0xFFE7DFF2),
        raised = Color(0xFF211E26),
        containers = listOf(
            Color(0xFF0F0D15),
            Color(0xFF1D1A22),
            Color(0xFF211E26),
            Color(0xFF2B2931),
            Color(0xFF36333C),
        ),
        error = Color(0xFFFFB4AB),
    ),
    scheme = darkColorScheme(),
    link = GridPink,
    fieldContainer = Color(0xFF211E26),
    pill = Color(0xFF2B2931),
    // The cyan lines are loud on the dark page; a wash keeps them behind the text.
    artScrim = Color(0xFF141218).copy(alpha = 0.4f),
)

private val SparkGreen = Color(0xFF34A853)
// SparkGreen darkened to clear 4.5:1 on the light page and on the lime phone button.
private val SparkGreenInk = Color(0xFF007C33)

private fun spark(
    n: Neutrals,
    scheme: ColorScheme,
    /** Links, accent text and the focused field border. */
    link: Color,
    title: Color,
    fieldContainer: Color,
    pill: Color,
    artScrim: Color,
) =
    FullCustomizationStyle(
        id = FullCustomizationThemeId.Spark,
        colorScheme = scheme.withNeutrals(n).copy(
            primary = link,
            onPrimary = Color.Black,
            // The field border is ink unfocused, so focus takes the green.
            secondary = link,
            onSecondary = n.page,
            outlineVariant = n.ink,
        ),
        typography = fullCustomizationTypography(
            title = JetBrainsMono,
            titleWeight = FontWeight.Medium,
            body = JetBrainsMono,
            button = JetBrainsMono,
        ),
        titleColor = title,
        showsMascot = false,
        cardFill = Color.Transparent,
        cardShadow = null,
        fieldShape = RoundedCornerShape(16.dp),
        fieldBorderWidth = 2.dp,
        fieldContainer = fieldContainer,
        buttonShape = RoundedCornerShape(40.dp),
        // The black outlines become the light ink in dark mode.
        buttonBorder = BorderStroke(2.dp, n.ink),
        buttonShadow = null,
        primaryButton = ButtonPalette(Color(0xFFFDADEE), Color.Black),
        secondaryButton = ButtonPalette(n.raised, n.ink),
        providerButtonShape = RoundedCornerShape(40.dp),
        phoneProvider = ButtonPalette(Color(0xFFC6FF00), SparkGreenInk),
        pill = ButtonPalette(pill, title),
        artScrim = artScrim,
    )

private val SparkLight = spark(
    n = Neutrals(
        page = Color(0xFFF1F1F1),
        ink = Color.Black,
        raised = Color(0xFFF3FBFB),
        containers = List(5) { Color(0xFFF1F1F1) },
        error = Color(0xFFBA1A1A),
    ),
    scheme = lightColorScheme(),
    link = SparkGreenInk,
    title = Color(0xFF1C1B1B),
    fieldContainer = Color.White,
    pill = Color.White,
    artScrim = Color.Transparent,
)

private val SparkDark = spark(
    n = Neutrals(
        page = Color(0xFF151313),
        ink = Color(0xFFE8E1E1),
        raised = Color(0xFF221F20),
        containers = listOf(
            Color(0xFF100E0E),
            Color(0xFF1E1B1C),
            Color(0xFF221F20),
            Color(0xFF2C292A),
            Color(0xFF373435),
        ),
        error = Color(0xFFFFB4AB),
    ),
    scheme = darkColorScheme(),
    link = SparkGreen,
    title = Color(0xFFE8E1E1),
    fieldContainer = Color(0xFF221F20),
    pill = Color(0xFF2C292A),
    // The blob is bright enough to fight the text over it on a dark page.
    artScrim = Color(0xFF151313).copy(alpha = 0.55f),
)

private val ProfessionalNavy = Color(0xFF1E3A8A)

/**
 * An enterprise-style sign-in: no art, mascot or hard shadows, one sans-serif, 8 to 12dp corners and
 * 48dp controls. Its screens are laid out differently too (see the `professional` package).
 */
private fun professional(
    scheme: ColorScheme,
    title: Color,
    cardFill: Color,
    fieldContainer: Color,
    secondaryButton: ButtonPalette,
    pill: ButtonPalette,
) = FullCustomizationStyle(
    id = FullCustomizationThemeId.Professional,
    colorScheme = scheme,
    // Doggo's 20sp button label is sized for an 80dp button; this theme's are 48dp.
    typography = fullCustomizationTypography(
        title = Roboto,
        titleWeight = FontWeight.SemiBold,
        body = Roboto,
        button = Roboto,
    ).let { it.copy(titleMedium = it.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)) },
    titleColor = title,
    showsMascot = false,
    cardFill = cardFill,
    cardShadow = null,
    fieldShape = RoundedCornerShape(8.dp),
    fieldBorderWidth = 1.dp,
    fieldContainer = fieldContainer,
    buttonShape = RoundedCornerShape(8.dp),
    buttonBorder = null,
    buttonShadow = null,
    primaryButton = ButtonPalette(scheme.primary, scheme.onPrimary),
    secondaryButton = secondaryButton,
    providerButtonShape = RoundedCornerShape(8.dp),
    phoneProvider = ButtonPalette(scheme.primary, scheme.onPrimary),
    pill = pill,
    artScrim = Color.Transparent,
    buttonHeight = 48.dp,
)

private val ProfessionalLight = professional(
    scheme = lightColorScheme(
        primary = ProfessionalNavy,
        onPrimary = Color.White,
        // The field focus border; primaryContainer is the other-methods sheet.
        secondary = ProfessionalNavy,
        onSecondary = Color.White,
        primaryContainer = Color.White,
        onPrimaryContainer = Color(0xFF1A1B21),
        surface = Color(0xFFF4F3FA),
        surfaceBright = Color(0xFFF4F3FA),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF4F3FA),
        surfaceContainer = Color(0xFFEEEDF4),
        surfaceContainerHigh = Color(0xFFE9E7EF),
        surfaceContainerHighest = Color(0xFFE3E1E9),
        background = Color(0xFFF4F3FA),
        onSurface = Color(0xFF1A1B21),
        onSurfaceVariant = Color(0xFF444651),
        outline = Color(0xFF757682),
        outlineVariant = Color(0xFFC5C5D3),
        error = Color(0xFFBA1A1A),
    ),
    title = Color(0xFF1A1B21),
    cardFill = Color.White,
    fieldContainer = Color.White,
    secondaryButton = ButtonPalette(Color(0xFFE3E7FF), ProfessionalNavy),
    pill = ButtonPalette(Color.White, Color(0xFF1A1B21)),
)

private val ProfessionalDark = professional(
    scheme = darkColorScheme(
        primary = Color(0xFFB6C4FF),
        onPrimary = Color(0xFF05297A),
        secondary = Color(0xFFB6C4FF),
        onSecondary = Color(0xFF05297A),
        primaryContainer = Color(0xFF1F1F24),
        onPrimaryContainer = Color(0xFFE3E1E9),
        surface = Color(0xFF131316),
        surfaceBright = Color(0xFF131316),
        surfaceContainerLowest = Color(0xFF0D0E13),
        surfaceContainerLow = Color(0xFF1A1B21),
        surfaceContainer = Color(0xFF1E1F25),
        surfaceContainerHigh = Color(0xFF292A2F),
        surfaceContainerHighest = Color(0xFF34343A),
        background = Color(0xFF131316),
        onSurface = Color(0xFFE3E1E9),
        onSurfaceVariant = Color(0xFFC5C5D3),
        outline = Color(0xFF8F909D),
        outlineVariant = Color(0xFF444651),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    ),
    title = Color(0xFFE3E1E9),
    cardFill = Color(0xFF1F1F24),
    fieldContainer = Color(0xFF292A2E),
    secondaryButton = ButtonPalette(Color(0xFF183585), Color(0xFFDCE1FF)),
    pill = ButtonPalette(Color(0xFF292A2F), Color(0xFFE3E1E9)),
)
