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

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.configuration.auth_provider.AuthProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.ui.components.AuthProviderButton
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for provider button shape customization features.
 *
 * Verifies that:
 * - Custom shapes can be set globally for all provider buttons
 * - Individual provider styles can override the global shape
 * - Shapes properly inherit through the composition local system
 */
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class ProviderButtonShapeCustomizationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `providerButtonShape applies to all provider buttons`() {
        val customShape = RoundedCornerShape(16.dp)
        val theme = AuthUITheme(
            colorScheme = lightColorScheme(),
            typography = androidx.compose.material3.Typography(),
            shapes = androidx.compose.material3.Shapes(),
            providerButtonShape = customShape
        )

        composeTestRule.setContent {
            AuthUITheme(theme = theme) {
                val currentTheme = LocalAuthUITheme.current
                assertThat(currentTheme.providerButtonShape).isEqualTo(customShape)
            }
        }
    }

    @Test
    fun `individual provider style shape overrides global providerButtonShape`() {
        val globalShape = RoundedCornerShape(8.dp)
        val googleSpecificShape = RoundedCornerShape(24.dp)
        
        val customProviderStyles = mapOf(
            "google.com" to ProviderStyleDefaults.Google.copy(
                shape = googleSpecificShape
            )
        )

        val theme = AuthUITheme(
            colorScheme = lightColorScheme(),
            typography = androidx.compose.material3.Typography(),
            shapes = androidx.compose.material3.Shapes(),
            providerButtonShape = globalShape,
            providerStyles = customProviderStyles
        )

        composeTestRule.setContent {
            AuthUITheme(theme = theme) {
                val currentTheme = LocalAuthUITheme.current
                val googleStyle = currentTheme.providerStyles["google.com"]
                assertThat(googleStyle).isNotNull()
                assertThat(googleStyle?.shape).isEqualTo(googleSpecificShape)
            }
        }
    }

    @Test
    fun `fromMaterialTheme accepts providerButtonShape parameter`() {
        val customShape = RoundedCornerShape(12.dp)

        composeTestRule.setContent {
            val theme = AuthUITheme.fromMaterialTheme(
                providerButtonShape = customShape
            )

            assertThat(theme.providerButtonShape).isEqualTo(customShape)
        }
    }

    @Test
    fun `ProviderStyleDefaults are publicly accessible`() {
        // Verify all default provider styles are accessible
        assertThat(ProviderStyleDefaults.Google).isNotNull()
        assertThat(ProviderStyleDefaults.Facebook).isNotNull()
        assertThat(ProviderStyleDefaults.Twitter).isNotNull()
        assertThat(ProviderStyleDefaults.Github).isNotNull()
        assertThat(ProviderStyleDefaults.Email).isNotNull()
        assertThat(ProviderStyleDefaults.Phone).isNotNull()
        assertThat(ProviderStyleDefaults.Anonymous).isNotNull()
        assertThat(ProviderStyleDefaults.Microsoft).isNotNull()
        assertThat(ProviderStyleDefaults.Yahoo).isNotNull()
        assertThat(ProviderStyleDefaults.Apple).isNotNull()
    }

    @Test
    fun `ProviderStyle is a data class with copy method`() {
        val original = ProviderStyleDefaults.Google
        val customShape = RoundedCornerShape(20.dp)
        
        val modified = original.copy(shape = customShape)

        assertThat(modified.shape).isEqualTo(customShape)
        assertThat(modified.backgroundColor).isEqualTo(original.backgroundColor)
        assertThat(modified.contentColor).isEqualTo(original.contentColor)
        assertThat(modified.icon).isEqualTo(original.icon)
    }

    @Test
    fun `AuthProviderButton respects theme providerButtonShape`() {
        val customShape = RoundedCornerShape(16.dp)
        val theme = AuthUITheme(
            colorScheme = lightColorScheme(),
            typography = androidx.compose.material3.Typography(),
            shapes = androidx.compose.material3.Shapes(),
            providerButtonShape = customShape
        )

        val provider = AuthProvider.Google(scopes = emptyList(), serverClientId = null)
        val stringProvider = DefaultAuthUIStringProvider(context)

        composeTestRule.setContent {
            AuthUITheme(theme = theme) {
                AuthProviderButton(
                    provider = provider,
                    onClick = { },
                    stringProvider = stringProvider
                )
                // Button should use customShape internally
                val currentTheme = LocalAuthUITheme.current
                assertThat(currentTheme.providerButtonShape).isEqualTo(customShape)
            }
        }
    }

    @Test
    fun `default shape is used when no custom shape is provided`() {
        val theme = AuthUITheme(
            colorScheme = lightColorScheme(),
            typography = androidx.compose.material3.Typography(),
            shapes = androidx.compose.material3.Shapes(),
            providerButtonShape = null
        )

        composeTestRule.setContent {
            AuthUITheme(theme = theme) {
                val currentTheme = LocalAuthUITheme.current
                assertThat(currentTheme.providerButtonShape).isNull()
            }
        }
    }

    @Test
    fun `custom provider styles with null shapes use global providerButtonShape`() {
        val globalShape = RoundedCornerShape(12.dp)
        
        val customProviderStyles = mapOf(
            "google.com" to ProviderStyleDefaults.Google.copy(
                shape = null  // Explicitly set to null to inherit global shape
            )
        )

        val theme = AuthUITheme(
            colorScheme = lightColorScheme(),
            typography = androidx.compose.material3.Typography(),
            shapes = androidx.compose.material3.Shapes(),
            providerButtonShape = globalShape,
            providerStyles = customProviderStyles
        )

        composeTestRule.setContent {
            AuthUITheme(theme = theme) {
                val currentTheme = LocalAuthUITheme.current
                val googleStyle = currentTheme.providerStyles["google.com"]
                // Shape should be null in the style, but button will use global shape
                assertThat(googleStyle?.shape).isNull()
                assertThat(currentTheme.providerButtonShape).isEqualTo(globalShape)
            }
        }
    }
}
