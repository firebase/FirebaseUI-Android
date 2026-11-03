package com.firebaseui.android.demo.auth.fullcustomization.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.firebaseui.android.demo.R

val BagelFatOne = FontFamily(Font(R.font.bagel_fat_one_regular, FontWeight.Normal))

val Onest = FontFamily(
    Font(R.font.onest_regular, FontWeight.Normal),
    Font(R.font.onest_medium, FontWeight.Medium),
    Font(R.font.onest_semibold, FontWeight.SemiBold),
    Font(R.font.onest_bold, FontWeight.Bold),
)

val Roboto = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_semibold, FontWeight.SemiBold),
    Font(R.font.roboto_bold, FontWeight.Bold),
)

val Rowdies = FontFamily(
    Font(R.font.rowdies_regular, FontWeight.Normal),
    Font(R.font.rowdies_bold, FontWeight.Bold),
)

val ChivoMono = FontFamily(
    Font(R.font.chivo_mono_medium, FontWeight.Medium),
    Font(R.font.chivo_mono_bold, FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

/**
 * Doggo's type scale with a theme's families swapped in: [title] for headlines, [body] for text
 * and fields, [button] for CTA labels. Sizes stay Doggo's on every theme.
 */
fun fullCustomizationTypography(
    title: FontFamily,
    titleWeight: FontWeight,
    body: FontFamily,
    button: FontFamily,
) = Typography(
    headlineSmall = TextStyle(
        fontFamily = title,
        fontWeight = titleWeight,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = title,
        fontWeight = titleWeight,
        fontSize = 36.sp,
        lineHeight = 44.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = body,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = button,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 20.sp,
    ),
)
