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

package com.firebase.ui.auth.compose.configuration.theme

import androidx.compose.ui.graphics.Color
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.configuration.auth_provider.Provider

/**
 * Default provider styling configurations for authentication providers.
 *
 * This object provides brand-appropriate visual styling for each supported authentication
 * provider, including background colors, text colors, and other visual properties that
 * match each provider's brand guidelines.
 *
 * The styles are automatically applied when using [AuthUITheme.Default] or can be
 * customized by passing a modified map to [AuthUITheme.fromMaterialTheme].
 */
internal object ProviderStyleDefaults {
    val default: Map<String, AuthUITheme.ProviderStyle>
        get() = Provider.entries.associate { provider ->
            when (provider) {
                Provider.GOOGLE -> {
                    provider.id to AuthUITheme.ProviderStyle(
                        icon = AuthUIAsset.Resource(R.drawable.fui_ic_googleg_color_24dp),
                        backgroundColor = Color.White,
                        contentColor = Color(0xFF757575)
                    )
                }

                Provider.FACEBOOK -> {
                    provider.id to AuthUITheme.ProviderStyle(
                        icon = AuthUIAsset.Resource(R.drawable.fui_ic_facebook_white_22dp),
                        backgroundColor = Color(0xFF3B5998),
                        contentColor = Color.White
                    )
                }

                Provider.TWITTER -> {
                    provider.id to AuthUITheme.ProviderStyle(
                        icon = AuthUIAsset.Resource(R.drawable.fui_ic_twitter_bird_white_24dp),
                        backgroundColor = Color(0xFF5BAAF4),
                        contentColor = Color.White
                    )
                }

                Provider.GITHUB -> {
                    provider.id to AuthUITheme.ProviderStyle(
                        icon = AuthUIAsset.Resource(R.drawable.fui_ic_github_white_24dp),
                        backgroundColor = Color(0xFF24292E),
                        contentColor = Color.White
                    )
                }

                Provider.EMAIL -> {
                    provider.id to AuthUITheme.ProviderStyle(
                        icon = AuthUIAsset.Resource(R.drawable.fui_ic_mail_white_24dp),
                        backgroundColor = Color(0xFFD0021B),
                        contentColor = Color.White
                    )
                }

                Provider.PHONE -> {
                    provider.id to AuthUITheme.ProviderStyle(
                        icon = AuthUIAsset.Resource(R.drawable.fui_ic_phone_white_24dp),
                        backgroundColor = Color(0xFF43C5A5),
                        contentColor = Color.White
                    )
                }

                Provider.ANONYMOUS -> {
                    provider.id to AuthUITheme.ProviderStyle(
                        icon = AuthUIAsset.Resource(R.drawable.fui_ic_anonymous_white_24dp),
                        backgroundColor = Color(0xFFF4B400),
                        contentColor = Color.White
                    )
                }

                Provider.MICROSOFT -> {
                    provider.id to AuthUITheme.ProviderStyle(
                        icon = AuthUIAsset.Resource(R.drawable.fui_ic_microsoft_24dp),
                        backgroundColor = Color(0xFF2F2F2F),
                        contentColor = Color.White
                    )
                }

                Provider.YAHOO -> {
                    provider.id to AuthUITheme.ProviderStyle(
                        icon = AuthUIAsset.Resource(R.drawable.fui_ic_yahoo_24dp),
                        backgroundColor = Color(0xFF720E9E),
                        contentColor = Color.White
                    )
                }

                Provider.APPLE -> {
                    provider.id to AuthUITheme.ProviderStyle(
                        icon = AuthUIAsset.Resource(R.drawable.fui_ic_apple_white_24dp),
                        backgroundColor = Color.Black,
                        contentColor = Color.White
                    )
                }
            }
        }
}