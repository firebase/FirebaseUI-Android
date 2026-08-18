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

package com.firebase.ui.auth.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.R
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.auth_provider.Provider
import com.firebase.ui.auth.configuration.string_provider.AuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.theme.AuthUIAsset
import com.firebase.ui.auth.configuration.theme.AuthUITheme
import com.firebase.ui.auth.configuration.theme.ProviderStyleDefaults
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
    fun `AuthProviderButton displays Apple provider with custom locale correctly`() {
        val provider = AuthProvider.Apple(
            locale = "es", // Spanish locale
            customParameters = emptyMap()
        )

        composeTestRule.setContent {
            AuthProviderButton(
                provider = provider,
                onClick = { clickedProvider = provider },
                stringProvider = stringProvider // Default stringProvider (English)
            )
        }

        // Should display Spanish text despite English stringProvider
        composeTestRule
            .onNodeWithText("Iniciar sesión con Apple")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertIsEnabled()
    }

    @Test
    fun `AuthProviderButton displays GenericOAuth provider with custom label`() {
        val customLabel = "Sign in with Custom Provider"
        val provider = AuthProvider.GenericOAuth(
            providerName = "Generic Provider",
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
        val customStyle = ProviderStyleDefaults.Facebook

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

        val resolvedStyle = resolveProviderStyle(provider, customStyle, ProviderStyleDefaults.default, null)
        assertThat(resolvedStyle.backgroundColor).isEqualTo(customStyle.backgroundColor)
        assertThat(resolvedStyle.contentColor).isEqualTo(customStyle.contentColor)
        assertThat(resolvedStyle.icon).isEqualTo(customStyle.icon)
        assertThat(resolvedStyle.backgroundColor)
            .isNotEqualTo(ProviderStyleDefaults.Google.backgroundColor)
    }

    @Test
    fun `GenericOAuth provider uses custom styling properties`() {
        val customLabel = "Custom Provider"
        val customColor = Color.Green
        val customContentColor = Color.Black
        val customIcon = AuthUIAsset.Vector(Icons.Default.Star)

        val provider = AuthProvider.GenericOAuth(
            providerName = "Generic Provider",
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

        val resolvedStyle = resolveProviderStyle(provider, null, ProviderStyleDefaults.default, null)
        assertThat(resolvedStyle).isNotNull()
        assertThat(resolvedStyle.backgroundColor).isEqualTo(customColor)
        assertThat(resolvedStyle.contentColor).isEqualTo(customContentColor)
        assertThat(resolvedStyle.icon).isEqualTo(customIcon)

        val googleDefaultStyle = ProviderStyleDefaults.Google
        assertThat(resolvedStyle.backgroundColor).isNotEqualTo(googleDefaultStyle.backgroundColor)
    }

    @Test
    fun `GenericOAuth provider falls back to default style when custom properties are null`() {
        val customLabel = "Custom Provider"
        val provider = AuthProvider.GenericOAuth(
            providerName = "Generic Provider",
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

        val resolvedStyle = resolveProviderStyle(provider, null, ProviderStyleDefaults.default, null)
        val googleDefaultStyle = ProviderStyleDefaults.Google

        assertThat(resolvedStyle.backgroundColor).isEqualTo(googleDefaultStyle.backgroundColor)
        assertThat(resolvedStyle.contentColor).isEqualTo(googleDefaultStyle.contentColor)
        assertThat(resolvedStyle.icon).isEqualTo(googleDefaultStyle.icon)
    }

    // =============================================================================================
    // Provider Style Fallback Tests
    // =============================================================================================

    @Test
    fun `AuthProviderButton provides fallback for unknown provider`() {
        val provider = object : AuthProvider(providerId = "unknown.provider", providerName = "Generic Provider",) {}

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
            providerName = "Generic Provider",
            providerId = "google.com",
            scopes = emptyList(),
            customParameters = emptyMap(),
            buttonIcon = AuthUIAsset.Vector(Icons.Default.Star),
            buttonLabel = "Custom",
            buttonColor = customColor,
            contentColor = customContentColor
        )

        val resolvedStyle = resolveProviderStyle(provider, null, ProviderStyleDefaults.default, null)

        assertThat(resolvedStyle).isNotNull()
        assertThat(resolvedStyle.backgroundColor).isEqualTo(customColor)
        assertThat(resolvedStyle.contentColor).isEqualTo(customContentColor)
    }

    @Test
    fun `resolveProviderStyle handles GenericOAuth without icon`() {
        val provider = AuthProvider.GenericOAuth(
            providerName = "Generic Provider",
            providerId = "custom.provider",
            scopes = emptyList(),
            customParameters = emptyMap(),
            buttonIcon = null,
            buttonLabel = "Custom",
            buttonColor = Color.Blue,
            contentColor = Color.White
        )

        val resolvedStyle = resolveProviderStyle(provider, null, ProviderStyleDefaults.default, null)

        assertThat(resolvedStyle).isNotNull()
        assertThat(resolvedStyle.icon).isNull()
        assertThat(resolvedStyle.backgroundColor).isEqualTo(Color.Blue)
        assertThat(resolvedStyle.contentColor).isEqualTo(Color.White)
    }

    @Test
    fun `resolveProviderStyle provides fallback for unknown provider`() {
        val provider = object : AuthProvider(providerId = "unknown.provider", providerName = "Generic Provider") {}

        val resolvedStyle = resolveProviderStyle(provider, null, ProviderStyleDefaults.default, null)

        assertThat(resolvedStyle).isNotNull()
        assertThat(resolvedStyle.backgroundColor).isEqualTo(AuthUITheme.ProviderStyle.Empty.backgroundColor)
        assertThat(resolvedStyle.contentColor).isEqualTo(AuthUITheme.ProviderStyle.Empty.contentColor)
    }

    // =============================================================================================
    // Modifier contract tests
    // =============================================================================================

    /**
     * A composable must apply its `modifier` to exactly one node, its outermost one. This button
     * used to hand the same instance to both the [androidx.compose.material3.Button] and the inner
     * content [androidx.compose.foundation.layout.Row], which duplicated everything the caller
     * passed: a `testTag` landed on two nodes, and padding was applied twice.
     *
     * The unmerged tree is what matters here. `TestTag`'s merge policy keeps the ancestor's value,
     * so the duplicate collapses to a single node in the merged tree and is invisible to an
     * ordinary `onNodeWithTag` lookup — while still being two real nodes, and so two Android
     * resource ids once `testTagsAsResourceId` is enabled.
     */
    @Test
    fun `caller modifier is applied to exactly one node`() {
        composeTestRule.setContent {
            AuthProviderButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CALLER_TAG),
                provider = AuthProvider.Google(scopes = emptyList(), serverClientId = null),
                onClick = { },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onAllNodesWithTag(CALLER_TAG, useUnmergedTree = true)
            .assertCountEquals(1)
    }

    /**
     * The one node the caller's modifier reaches is the button itself, not the content row: the
     * tagged node has to be the clickable one.
     *
     * This queries the merged tree, where `TestTag`'s merge policy keeps the ancestor's value, so
     * the duplicated tag resolved to the button before the fix as well and this assertion held
     * either way. It is a guard on which node owns the tag, not a pin on the change; the unmerged
     * count above is what pins it.
     */
    @Test
    fun `caller modifier lands on the button rather than its content`() {
        composeTestRule.setContent {
            AuthProviderButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CALLER_TAG),
                provider = AuthProvider.Google(scopes = emptyList(), serverClientId = null),
                onClick = { },
                stringProvider = stringProvider
            )
        }

        composeTestRule
            .onNodeWithTag(CALLER_TAG)
            .assertHasClickAction()
    }

    /**
     * The content row now owns its own width instead of inheriting the caller's, so a full-width
     * button still lays its icon and label out from the start edge rather than centring them.
     * This pins the rendered layout that the duplicated modifier used to produce by accident.
     */
    @Test
    fun `full width button keeps its content start aligned`() {
        composeTestRule.setContent {
            Column(modifier = Modifier.fillMaxWidth()) {
                AuthProviderButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(CALLER_TAG),
                    provider = AuthProvider.Google(scopes = emptyList(), serverClientId = null),
                    onClick = { },
                    stringProvider = stringProvider
                )
            }
        }

        val label = context.getString(R.string.fui_sign_in_with_google)
        val buttonBounds = composeTestRule.onNodeWithTag(CALLER_TAG).getUnclippedBoundsInRoot()
        val iconBounds = composeTestRule
            .onNodeWithContentDescription(label, useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        // 12.dp of Button content padding is the only gap expected between the two left edges.
        val inset = iconBounds.left - buttonBounds.left
        assertThat(inset.value).isWithin(TOLERANCE_DP).of(CONTENT_PADDING_DP)
    }

    private companion object {
        const val CALLER_TAG = "caller_supplied_tag"

        /** Horizontal `contentPadding` applied by [AuthProviderButton] to the Material button. */
        const val CONTENT_PADDING_DP = 12f

        const val TOLERANCE_DP = 0.5f
    }
}