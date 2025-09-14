package com.firebase.ui.auth.configuration

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LocalAuthUITheme = staticCompositionLocalOf { AuthUITheme.Default }

/**
 * Theming configuration for the entire Auth UI.
 */
data class AuthUITheme(
    /**
     * The color scheme to use.
     */
    val colorScheme: ColorScheme,

    /**
     * The typography to use.
     */
    val typography: Typography,

    /**
     * The shapes to use for UI elements.
     */
    val shapes: Shapes,

    /**
     * A map of provider IDs to custom styling.
     */
    val providerStyles: Map<String, ProviderStyle> = emptyMap()
) {

    /**
     * A data class nested within AuthUITheme that defines the visual appearance of a specific
     * provider button, allowing for per-provider branding and customization.
     */
    data class ProviderStyle(
        /**
         * The background color of the button.
         */
        val backgroundColor: Color,

        /**
         * The color of the text label on the button.
         */
        val contentColor: Color,

        /**
         * An optional tint color for the provider's icon. If null,
         * the icon's intrinsic color is used.
         */
        var iconTint: Color? = null,

        /**
         * The shape of the button container. Defaults to RoundedCornerShape(4.dp).
         */
        val shape: Shape = RoundedCornerShape(4.dp),

        /**
         * The shadow elevation for the button. Defaults to 2.dp.
         */
        val elevation: Dp = 2.dp
    )

    companion object {
        /**
         * A standard light theme with Material 3 defaults and
         * pre-configured provider styles.
         */
        val Default = AuthUITheme(
            colorScheme = lightColorScheme(),
            typography = Typography(),
            shapes = Shapes(),
            // TODO(demolaf): do we provide default styles for each provider?
            providerStyles = mapOf<String, ProviderStyle>(
                "google.com" to ProviderStyle(
                    backgroundColor = Color.White,
                    contentColor = Color.Black
                )
            )
        )

        /**
         * Creates a theme inheriting the app's current Material
         * Theme settings.
         */
        @Composable
        fun fromMaterialTheme(
            providerStyles: Map<String, ProviderStyle> = Default.providerStyles
        ): AuthUITheme {
            return AuthUITheme(
                colorScheme = MaterialTheme.colorScheme,
                typography = MaterialTheme.typography,
                shapes = MaterialTheme.shapes,
                providerStyles = providerStyles
            )
        }
    }
}

@Composable
fun AuthUITheme(
    theme: AuthUITheme = AuthUITheme.Default,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAuthUITheme provides theme) {
        content()
    }
}
