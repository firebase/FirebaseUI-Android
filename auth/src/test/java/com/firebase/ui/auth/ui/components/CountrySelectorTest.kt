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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.firebase.ui.auth.configuration.string_provider.DefaultAuthUIStringProvider
import com.firebase.ui.auth.configuration.string_provider.LocalAuthUIStringProvider
import com.firebase.ui.auth.data.CountryData
import com.firebase.ui.auth.ui.FirebaseAuthTestTags
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [CountrySelector] covering the bottom sheet country list, including the stable
 * test tag host applications and Robo directives target it by.
 *
 * @suppress Internal test class
 */
@Config(manifest = Config.NONE, sdk = [34])
@RunWith(RobolectricTestRunner::class)
class CountrySelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private var selectedCountry: CountryData? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        selectedCountry = null
    }

    private fun setContentWithStringProvider(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalAuthUIStringProvider provides DefaultAuthUIStringProvider(context)
            ) {
                content()
            }
        }
    }

    private fun setCountrySelectorContent() {
        setContentWithStringProvider {
            CountrySelector(
                selectedCountry = CountryData(
                    name = "United States",
                    dialCode = "+1",
                    countryCode = "US",
                    flagEmoji = "🇺🇸"
                ),
                onCountrySelected = { selectedCountry = it }
            )
        }
    }

    // =============================================================================================
    // Test Tag Tests
    // =============================================================================================

    @Test
    fun `CountrySelector tags the country list once the bottom sheet is open`() {
        setCountrySelectorContent()

        composeTestRule.onNodeWithContentDescription("Country selector").performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.CountrySelector.COUNTRY_LIST)
            .assertIsDisplayed()
    }

    @Test
    fun `CountrySelector does not tag a country list while the bottom sheet is closed`() {
        setCountrySelectorContent()

        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.CountrySelector.COUNTRY_LIST)
            .assertDoesNotExist()
    }

    // =============================================================================================
    // Selection Tests
    // =============================================================================================

    @Test
    fun `CountrySelector reports the country picked from the tagged list`() {
        setCountrySelectorContent()

        composeTestRule.onNodeWithContentDescription("Country selector").performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(FirebaseAuthTestTags.CountrySelector.COUNTRY_LIST)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(FIRST_COUNTRY_NAME).performClick()
        composeTestRule.waitForIdle()

        assertThat(selectedCountry?.countryCode).isEqualTo(FIRST_COUNTRY_CODE)
    }

    private companion object {
        /** First entry of `ALL_COUNTRIES`, so it needs no scrolling to reach. */
        const val FIRST_COUNTRY_NAME = "Afghanistan"
        const val FIRST_COUNTRY_CODE = "AF"
    }
}
