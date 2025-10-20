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

package com.firebase.ui.auth.compose.ui.components

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.R
import com.firebase.ui.auth.compose.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.compose.configuration.auth_provider.Provider
import com.firebase.ui.auth.compose.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.compose.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.compose.configuration.theme.AuthUITheme
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [AuthProviderButton] covering UI interactions, styling,
 * and provider-specific behavior.
 *
 * @suppress Internal test class
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AuthProviderButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var stringProvider: AuthUIStringProvider
    private var clickedProvider: AuthProvider? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        stringProvider = DefaultAuthUIStringProvider(context)
        clickedProvider = null
    }

    // =============================================================================================
    // Basic UI Tests
    // =============================================================================================

    @Test
    fun `AuthProviderButton displays Google provider correctly`() {
        val provider = AuthProvider.Google(scopes = emptyList(), serverClientId = null)

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun `AuthProviderButton displays Facebook provider correctly`() {
        val provider = AuthProvider.Facebook()

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_facebook))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun `AuthProviderButton displays Email provider correctly`() {
        val provider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            passwordValidationRules = emptyList()
        )

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_email))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun `AuthProviderButton displays Phone provider correctly`() {
        val provider = AuthProvider.Phone(
            defaultNumber = null,
            defaultCountryCode = null,
            allowedCountries = null
        )

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_phone))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun `AuthProviderButton displays Anonymous provider correctly`() {
        val provider = AuthProvider.Anonymous

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_anonymously))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun `AuthProviderButton displays Twitter provider correctly`() {
        val provider = AuthProvider.Twitter(customParameters = emptyMap())

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_twitter))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun `AuthProviderButton displays Github provider correctly`() {
        val provider = AuthProvider.Github(customParameters = emptyMap())

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_github))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun `AuthProviderButton displays Microsoft provider correctly`() {
        val provider = AuthProvider.Microsoft(tenant = null, customParameters = emptyMap())

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_microsoft))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun `AuthProviderButton displays Yahoo provider correctly`() {
        val provider = AuthProvider.Yahoo(customParameters = emptyMap())

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_yahoo))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun `AuthProviderButton displays Apple provider correctly`() {
        val provider = AuthProvider.Apple(locale = null, customParameters = emptyMap())

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_apple))
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun `AuthProviderButton displays GenericOAuth provider with custom label`() {
        val customLabel = "Sign in with Custom Provider"
        val provider = AuthProvider.GenericOAuth(
            name = "Generic Provider",
            providerId = "google.com",
            scopes = emptyList(),
            customParameters = emptyMap(),
            buttonLabel = customLabel,
            buttonIcon = AuthUIAsset.Vector(Icons.Default.Star),
            buttonColor = Color.Blue,
            contentColor = Color.White
        )

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(customLabel)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    // =============================================================================================
    // Click Interaction Tests
    // =============================================================================================

    @Test
    fun `AuthProviderButton onClick is called when clicked`() {
        val provider = AuthProvider.Google(scopes = emptyList(), serverClientId = null)

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .performClick()

        assertThat(clickedProvider).isEqualTo(provider)
    }

    @Test
    fun `AuthProviderButton respects enabled state`() {
        val provider = AuthProvider.Google(scopes = emptyList(), serverClientId = null)

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                enabled = false,
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .assertIsNotEnabled()
            .performClick()

        assertThat(clickedProvider).isNull()
    }

    // =============================================================================================
    // Style Resolution Tests
    // =============================================================================================

    @Test
    fun `AuthProviderButton uses custom style when provided`() {
        val provider = AuthProvider.Google(scopes = emptyList(), serverClientId = null)
        val customStyle = AuthUITheme.Default.providerStyles[Provider.FACEBOOK.id]

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { },
                style = customStyle,
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.fui_sign_in_with_google))
            .assertIsDisplayed()

        val resolvedStyle = resolveProviderStyle(provider, customStyle)
        assertThat(resolvedStyle).isEqualTo(customStyle)
        assertThat(resolvedStyle)
            .isNotEqualTo(AuthUITheme.Default.providerStyles[Provider.GOOGLE.id])
    }

    @Test
    fun `GenericOAuth provider uses custom styling properties`() {
        val customLabel = "Custom Provider"
        val customColor = Color.Green
        val customContentColor = Color.Black
        val customIcon = AuthUIAsset.Vector(Icons.Default.Star)

        val provider = AuthProvider.GenericOAuth(
            name = "Generic Provider",
            providerId = "google.com",
            scopes = emptyList(),
            customParameters = emptyMap(),
            buttonLabel = customLabel,
            buttonIcon = customIcon,
            buttonColor = customColor,
            contentColor = customContentColor
        )

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { },
                stringProvider = stringProvider
            )
        }

        composeTestRule.onNodeWithText(customLabel)
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(customLabel)
            .assertIsDisplayed()

        val resolvedStyle = resolveProviderStyle(provider, null)
        assertThat(resolvedStyle).isNotNull()
        assertThat(resolvedStyle.backgroundColor).isEqualTo(customColor)
        assertThat(resolvedStyle.contentColor).isEqualTo(customContentColor)
        assertThat(resolvedStyle.icon).isEqualTo(customIcon)

        val googleDefaultStyle = AuthUITheme.Default.providerStyles["google.com"]
        assertThat(resolvedStyle).isNotEqualTo(googleDefaultStyle)
    }

    @Test
    fun `GenericOAuth provider falls back to default style when custom properties are null`() {
        val customLabel = "Custom Provider"
        val provider = AuthProvider.GenericOAuth(
            name = "Generic Provider",
            providerId = "google.com",
            scopes = emptyList(),
            customParameters = emptyMap(),
            buttonLabel = customLabel,
            buttonIcon = null,
            buttonColor = null,
            contentColor = null
        )

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { },
                stringProvider = stringProvider
            )
        }

        composeTestRule.onNodeWithText(customLabel)
            .assertIsDisplayed()

        val resolvedStyle = resolveProviderStyle(provider, null)
        val googleDefaultStyle = AuthUITheme.Default.providerStyles["google.com"]

        assertThat(googleDefaultStyle).isNotNull()
        assertThat(resolvedStyle.backgroundColor).isEqualTo(googleDefaultStyle!!.backgroundColor)
        assertThat(resolvedStyle.contentColor).isEqualTo(googleDefaultStyle.contentColor)
        assertThat(resolvedStyle.icon).isEqualTo(googleDefaultStyle.icon)
    }

    // =============================================================================================
    // Provider Style Fallback Tests
    // =============================================================================================

    @Test
    fun `AuthProviderButton provides fallback for unknown provider`() {
        val provider = object : AuthProvider(providerId = "unknown.provider", name = "Generic Provider",) {}

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { },
                stringProvider = stringProvider
            )
        }

        composeTestRule.onNodeWithText("Unknown Provider")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }


    @Test
    fun `resolveProviderStyle applies custom colors for GenericOAuth with icon`() {
        val customColor = Color.Red
        val customContentColor = Color.White

        val provider = AuthProvider.GenericOAuth(
            name = "Generic Provider",
            providerId = "google.com",
            scopes = emptyList(),
            customParameters = emptyMap(),
            buttonIcon = AuthUIAsset.Vector(Icons.Default.Star),
            buttonLabel = "Custom",
            buttonColor = customColor,
            contentColor = customContentColor
        )

        val resolvedStyle = resolveProviderStyle(provider, null)

        assertThat(resolvedStyle).isNotNull()
        assertThat(resolvedStyle.backgroundColor).isEqualTo(customColor)
        assertThat(resolvedStyle.contentColor).isEqualTo(customContentColor)
    }

    @Test
    fun `resolveProviderStyle handles GenericOAuth without icon`() {
        val provider = AuthProvider.GenericOAuth(
            name = "Generic Provider",
            providerId = "custom.provider",
            scopes = emptyList(),
            customParameters = emptyMap(),
            buttonIcon = null,
            buttonLabel = "Custom",
            buttonColor = Color.Blue,
            contentColor = Color.White
        )

        val resolvedStyle = resolveProviderStyle(provider, null)

        assertThat(resolvedStyle).isNotNull()
        assertThat(resolvedStyle.icon).isNull()
        assertThat(resolvedStyle.backgroundColor).isEqualTo(Color.Blue)
        assertThat(resolvedStyle.contentColor).isEqualTo(Color.White)
    }

    @Test
    fun `resolveProviderStyle provides fallback for unknown provider`() {
        val provider = object : AuthProvider(providerId = "unknown.provider", name = "Generic Provider") {}

        val resolvedStyle = resolveProviderStyle(provider, null)

        assertThat(resolvedStyle).isNotNull()
        assertThat(resolvedStyle).isEqualTo(AuthUITheme.ProviderStyle.Empty)
    }
}