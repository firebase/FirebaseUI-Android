/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.auth.configuration.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal providing access to the current AuthUITheme.
 * This allows components to access theme configuration including provider styles and shapes.
 */
val LocalAuthUITheme = staticCompositionLocalOf { AuthUITheme.Default }

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
     * The shapes to use for UI elements (text fields, cards, etc.).
     */
    val shapes: Shapes,

    /**
     * A map of provider IDs to custom styling. Use this to customize individual
     * provider buttons with specific colors, icons, shapes, and elevation.
     *
     * Example:
     * ```kotlin
     * providerStyles = mapOf(
     *     "google.com" to ProviderStyleDefaults.Google.copy(
     *         shape = RoundedCornerShape(12.dp)
     *     )
     * )
     * ```
     */
    val providerStyles: Map<String, ProviderStyle> = emptyMap(),

    /**
     * Default shape for all provider buttons. If not set, defaults to RoundedCornerShape(4.dp).
     * Individual provider styles can override this shape.
     *
     * Example:
     * ```kotlin
     * providerButtonShape = RoundedCornerShape(12.dp)
     * ```
     */
    val providerButtonShape: Shape? = null,
) {

    /**
     * A class nested within AuthUITheme that defines the visual appearance of a specific
     * provider button, allowing for per-provider branding and customization.
     */
    data class ProviderStyle(
        /**
         * The provider's icon.
         */
        val icon: AuthUIAsset?,

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
        val iconTint: Color? = null,

        /**
         * The shape of the button container. If null, uses the theme's providerButtonShape
         * or falls back to RoundedCornerShape(4.dp).
         */
        val shape: Shape? = null,

        /**
         * The shadow elevation for the button. Defaults to 2.dp.
         */
        val elevation: Dp = 2.dp,
    ) {
        internal companion object {
            /**
             * A fallback style for unknown providers with no icon, white background,
             * and black text.
             */
            val Empty = ProviderStyle(
                icon = null,
                backgroundColor = Color.White,
                contentColor = Color.Black,
            )
        }
    }

    companion object {
        /**
         * A standard light theme with Material 3 defaults and
         * pre-configured provider styles.
         */
        val Default = AuthUITheme(
            colorScheme = lightColorScheme(),
            typography = Typography(),
            shapes = Shapes(),
            providerStyles = ProviderStyleDefaults.default
        )

        val DefaultDark = AuthUITheme(
            colorScheme = darkColorScheme(),
            typography = Typography(),
            shapes = Shapes(),
            providerStyles = ProviderStyleDefaults.default
        )

        /**
         * Creates a theme inheriting the app's current Material Theme settings.
         *
         * @param providerStyles Custom styling for individual providers. Defaults to standard provider styles.
         * @param providerButtonShape Default shape for all provider buttons. If null, uses RoundedCornerShape(4.dp).
         */
        @Composable
        fun fromMaterialTheme(
            providerStyles: Map<String, ProviderStyle> = ProviderStyleDefaults.default,
            providerButtonShape: Shape? = null,
        ): AuthUITheme {
            return AuthUITheme(
                colorScheme = MaterialTheme.colorScheme,
                typography = MaterialTheme.typography,
                shapes = MaterialTheme.shapes,
                providerStyles = providerStyles,
                providerButtonShape = providerButtonShape
            )
        }

        @OptIn(ExperimentalMaterial3Api::class)
        @get:Composable
        val topAppBarColors
            get() = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            )
    }
}

@Composable
fun AuthUITheme(
    theme: AuthUITheme = if (isSystemInDarkTheme())
        AuthUITheme.DefaultDark else AuthUITheme.Default,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAuthUITheme provides theme
    ) {
        MaterialTheme(
            colorScheme = theme.colorScheme,
            typography = theme.typography,
            shapes = theme.shapes,
            content = content
        )
    }
}
