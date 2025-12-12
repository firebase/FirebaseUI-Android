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

import androidx.compose.ui.graphics.Color
import com.firebase.ui.auth.R
import com.firebase.ui.auth.configuration.auth_provider.Provider

/**
 * Default provider styling configurations for authentication providers.
 *
 * This object provides brand-appropriate visual styling for each supported authentication
 * provider, including background colors, text colors, and other visual properties that
 * match each provider's brand guidelines.
 *
 * The styles are automatically applied when using [AuthUITheme.Default] or can be
 * customized by passing a modified map to [AuthUITheme.fromMaterialTheme].
 *
 * Individual provider styles can be accessed and customized using the public properties
 * (e.g., [Google], [Facebook]) and then modified using the [AuthUITheme.ProviderStyle.copy] method.
 */
object ProviderStyleDefaults {
    val Google = AuthUITheme.ProviderStyle(
        icon = AuthUIAsset.Resource(R.drawable.fui_ic_googleg_color_24dp),
        backgroundColor = Color.White,
        contentColor = Color(0xFF757575)
    )

    val Facebook = AuthUITheme.ProviderStyle(
        icon = AuthUIAsset.Resource(R.drawable.fui_ic_facebook_white_22dp),
        backgroundColor = Color(0xFF1877F2),
        contentColor = Color.White
    )

    val Twitter = AuthUITheme.ProviderStyle(
        icon = AuthUIAsset.Resource(R.drawable.fui_ic_twitter_x_white_24dp),
        backgroundColor = Color.Black,
        contentColor = Color.White
    )

    val Github = AuthUITheme.ProviderStyle(
        icon = AuthUIAsset.Resource(R.drawable.fui_ic_github_white_24dp),
        backgroundColor = Color(0xFF24292E),
        contentColor = Color.White
    )

    val Email = AuthUITheme.ProviderStyle(
        icon = AuthUIAsset.Resource(R.drawable.fui_ic_mail_white_24dp),
        backgroundColor = Color(0xFFD0021B),
        contentColor = Color.White
    )

    val Phone = AuthUITheme.ProviderStyle(
        icon = AuthUIAsset.Resource(R.drawable.fui_ic_phone_white_24dp),
        backgroundColor = Color(0xFF43C5A5),
        contentColor = Color.White
    )

    val Anonymous = AuthUITheme.ProviderStyle(
        icon = AuthUIAsset.Resource(R.drawable.fui_ic_anonymous_white_24dp),
        backgroundColor = Color(0xFFF4B400),
        contentColor = Color.White
    )

    val Microsoft = AuthUITheme.ProviderStyle(
        icon = AuthUIAsset.Resource(R.drawable.fui_ic_microsoft_24dp),
        backgroundColor = Color(0xFF2F2F2F),
        contentColor = Color.White
    )

    val Yahoo = AuthUITheme.ProviderStyle(
        icon = AuthUIAsset.Resource(R.drawable.fui_ic_yahoo_24dp),
        backgroundColor = Color(0xFF720E9E),
        contentColor = Color.White
    )

    val Apple = AuthUITheme.ProviderStyle(
        icon = AuthUIAsset.Resource(R.drawable.fui_ic_apple_white_24dp),
        backgroundColor = Color.Black,
        contentColor = Color.White
    )

    val default: Map<String, AuthUITheme.ProviderStyle>
        get() = mapOf(
            Provider.GOOGLE.id to Google,
            Provider.FACEBOOK.id to Facebook,
            Provider.TWITTER.id to Twitter,
            Provider.GITHUB.id to Github,
            Provider.EMAIL.id to Email,
            Provider.PHONE.id to Phone,
            Provider.ANONYMOUS.id to Anonymous,
            Provider.MICROSOFT.id to Microsoft,
            Provider.YAHOO.id to Yahoo,
            Provider.APPLE.id to Apple
        )
}