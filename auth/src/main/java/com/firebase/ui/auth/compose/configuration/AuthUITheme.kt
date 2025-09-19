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

package com.firebase.ui.auth.compose.configuration

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
class AuthUITheme(
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
     * A class nested within AuthUITheme that defines the visual appearance of a specific
     * provider button, allowing for per-provider branding and customization.
     */
    class ProviderStyle(
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
            providerStyles = defaultProviderStyles
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

        internal val defaultProviderStyles
            get(): Map<String, ProviderStyle> {
                return Provider.entries.associate { provider ->
                    when (provider) {
                        Provider.GOOGLE -> {
                            provider.id to ProviderStyle(
                                backgroundColor = Color.White,
                                contentColor = Color(0xFF757575)
                            )
                        }

                        Provider.FACEBOOK -> {
                            provider.id to ProviderStyle(
                                backgroundColor = Color(0xFF3B5998),
                                contentColor = Color.White
                            )
                        }

                        Provider.TWITTER -> {
                            provider.id to ProviderStyle(
                                backgroundColor = Color(0xFF5BAAF4),
                                contentColor = Color.White
                            )
                        }

                        Provider.GITHUB -> {
                            provider.id to ProviderStyle(
                                backgroundColor = Color(0xFF24292E),
                                contentColor = Color.White
                            )
                        }

                        Provider.EMAIL -> {
                            provider.id to ProviderStyle(
                                backgroundColor = Color(0xFFD0021B),
                                contentColor = Color.White
                            )
                        }

                        Provider.PHONE -> {
                            provider.id to ProviderStyle(
                                backgroundColor = Color(0xFF43C5A5),
                                contentColor = Color.White
                            )
                        }

                        Provider.ANONYMOUS -> {
                            provider.id to ProviderStyle(
                                backgroundColor = Color(0xFFF4B400),
                                contentColor = Color.White
                            )
                        }

                        Provider.MICROSOFT -> {
                            provider.id to ProviderStyle(
                                backgroundColor = Color(0xFF2F2F2F),
                                contentColor = Color.White
                            )
                        }

                        Provider.YAHOO -> {
                            provider.id to ProviderStyle(
                                backgroundColor = Color(0xFF720E9E),
                                contentColor = Color.White
                            )
                        }

                        Provider.APPLE -> {
                            provider.id to ProviderStyle(
                                backgroundColor = Color.Black,
                                contentColor = Color.White
                            )
                        }
                    }
                }
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
