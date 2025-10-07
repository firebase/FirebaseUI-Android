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

package com.firebase.ui.auth.compose.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [CountryData] and related utilities.
 *
 * @suppress Internal test class
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CountryDataTest {

    // =============================================================================================
    // CountryData Tests
    // =============================================================================================

    @Test
    fun `CountryData has correct properties`() {
        val country = CountryData(
            name = "United States",
            dialCode = "+1",
            countryCode = "US",
            flagEmoji = "🇺🇸"
        )

        assertThat(country.name).isEqualTo("United States")
        assertThat(country.dialCode).isEqualTo("+1")
        assertThat(country.countryCode).isEqualTo("US")
        assertThat(country.flagEmoji).isEqualTo("🇺🇸")
    }

    @Test
    fun `getDisplayName returns formatted name with flag`() {
        val country = CountryData(
            name = "United Kingdom",
            dialCode = "+44",
            countryCode = "GB",
            flagEmoji = "🇬🇧"
        )

        assertThat(country.getDisplayName()).isEqualTo("🇬🇧 United Kingdom")
    }

    @Test
    fun `getDisplayNameWithDialCode returns formatted name with flag and dial code`() {
        val country = CountryData(
            name = "France",
            dialCode = "+33",
            countryCode = "FR",
            flagEmoji = "🇫🇷"
        )

        assertThat(country.getDisplayNameWithDialCode()).isEqualTo("🇫🇷 France (+33)")
    }

    // =============================================================================================
    // Flag Emoji Tests
    // =============================================================================================

    @Test
    fun `countryCodeToFlagEmoji generates correct emoji for US`() {
        val emoji = countryCodeToFlagEmoji("US")
        assertThat(emoji).isEqualTo("🇺🇸")
    }

    @Test
    fun `countryCodeToFlagEmoji generates correct emoji for GB`() {
        val emoji = countryCodeToFlagEmoji("GB")
        assertThat(emoji).isEqualTo("🇬🇧")
    }

    @Test
    fun `countryCodeToFlagEmoji generates correct emoji for FR`() {
        val emoji = countryCodeToFlagEmoji("FR")
        assertThat(emoji).isEqualTo("🇫🇷")
    }

    @Test
    fun `countryCodeToFlagEmoji works with lowercase codes`() {
        val emoji = countryCodeToFlagEmoji("de")
        assertThat(emoji).isEqualTo("🇩🇪")
    }

    @Test
    fun `countryCodeToFlagEmoji returns empty string for invalid code length`() {
        val emoji = countryCodeToFlagEmoji("USA")
        assertThat(emoji).isEmpty()
    }

    @Test
    fun `countryCodeToFlagEmoji returns empty string for empty code`() {
        val emoji = countryCodeToFlagEmoji("")
        assertThat(emoji).isEmpty()
    }

    @Test
    fun `countryCodeToFlagEmoji returns empty string for single character`() {
        val emoji = countryCodeToFlagEmoji("U")
        assertThat(emoji).isEmpty()
    }

    // =============================================================================================
    // Country List Tests
    // =============================================================================================

    @Test
    fun `ALL_COUNTRIES contains expected number of countries`() {
        assertThat(ALL_COUNTRIES).isNotEmpty()
        assertThat(ALL_COUNTRIES.size).isGreaterThan(200)
    }

    @Test
    fun `ALL_COUNTRIES contains United States`() {
        val us = ALL_COUNTRIES.find { it.countryCode == "US" }
        assertThat(us).isNotNull()
        assertThat(us?.name).isEqualTo("United States")
        assertThat(us?.dialCode).isEqualTo("+1")
    }

    @Test
    fun `ALL_COUNTRIES contains United Kingdom`() {
        val uk = ALL_COUNTRIES.find { it.countryCode == "GB" }
        assertThat(uk).isNotNull()
        assertThat(uk?.name).isEqualTo("United Kingdom")
        assertThat(uk?.dialCode).isEqualTo("+44")
    }

    @Test
    fun `ALL_COUNTRIES contains France`() {
        val france = ALL_COUNTRIES.find { it.countryCode == "FR" }
        assertThat(france).isNotNull()
        assertThat(france?.name).isEqualTo("France")
        assertThat(france?.dialCode).isEqualTo("+33")
    }

    @Test
    fun `ALL_COUNTRIES has no duplicate country codes`() {
        val countryCodes = ALL_COUNTRIES.map { it.countryCode }
        val uniqueCodes = countryCodes.toSet()
        assertThat(countryCodes.size).isEqualTo(uniqueCodes.size)
    }

    @Test
    fun `ALL_COUNTRIES all entries have valid flag emojis`() {
        ALL_COUNTRIES.forEach { country ->
            assertThat(country.flagEmoji).isNotEmpty()
        }
    }

    @Test
    fun `ALL_COUNTRIES all entries have dial codes starting with plus`() {
        ALL_COUNTRIES.forEach { country ->
            assertThat(country.dialCode).startsWith("+")
        }
    }

    @Test
    fun `ALL_COUNTRIES all entries have two-letter country codes`() {
        ALL_COUNTRIES.forEach { country ->
            assertThat(country.countryCode).hasLength(2)
        }
    }

    // =============================================================================================
    // CountryUtils - Lookup Tests
    // =============================================================================================

    @Test
    fun `findByCountryCode returns correct country for US`() {
        val country = CountryUtils.findByCountryCode("US")
        assertThat(country).isNotNull()
        assertThat(country?.name).isEqualTo("United States")
        assertThat(country?.dialCode).isEqualTo("+1")
    }

    @Test
    fun `findByCountryCode is case insensitive`() {
        val country = CountryUtils.findByCountryCode("us")
        assertThat(country).isNotNull()
        assertThat(country?.countryCode).isEqualTo("US")
    }

    @Test
    fun `findByCountryCode returns null for invalid code`() {
        val country = CountryUtils.findByCountryCode("XX")
        assertThat(country).isNull()
    }

    @Test
    fun `findByDialCode returns countries with +1 dial code`() {
        val countries = CountryUtils.findByDialCode("+1")
        assertThat(countries).isNotEmpty()
        assertThat(countries.map { it.countryCode }).contains("US")
        assertThat(countries.map { it.countryCode }).contains("CA")
    }

    @Test
    fun `findByDialCode returns countries with +44 dial code`() {
        val countries = CountryUtils.findByDialCode("+44")
        assertThat(countries).isNotEmpty()
        val countryCodes = countries.map { it.countryCode }
        assertThat(countryCodes).contains("GB")
    }

    @Test
    fun `findByDialCode returns empty list for non-existent dial code`() {
        val countries = CountryUtils.findByDialCode("+9999")
        assertThat(countries).isEmpty()
    }

    // =============================================================================================
    // CountryUtils - Search Tests
    // =============================================================================================

    @Test
    fun `searchByName finds United States`() {
        val countries = CountryUtils.searchByName("United States")
        assertThat(countries).isNotEmpty()
        assertThat(countries[0].countryCode).isEqualTo("US")
    }

    @Test
    fun `searchByName finds countries with partial match`() {
        val countries = CountryUtils.searchByName("United")
        assertThat(countries).isNotEmpty()
        val names = countries.map { it.name }
        assertThat(names).contains("United States")
        assertThat(names).contains("United Kingdom")
        assertThat(names).contains("United Arab Emirates")
    }

    @Test
    fun `searchByName is case insensitive`() {
        val countries = CountryUtils.searchByName("france")
        assertThat(countries).isNotEmpty()
        assertThat(countries[0].countryCode).isEqualTo("FR")
    }

    @Test
    fun `searchByName handles diacritics`() {
        val countries = CountryUtils.searchByName("Cote d'Ivoire")
        assertThat(countries).isNotEmpty()
        assertThat(countries[0].countryCode).isEqualTo("CI")
    }

    @Test
    fun `searchByName returns empty list for empty query`() {
        val countries = CountryUtils.searchByName("")
        assertThat(countries).isEmpty()
    }

    @Test
    fun `searchByName returns empty list for whitespace query`() {
        val countries = CountryUtils.searchByName("   ")
        assertThat(countries).isEmpty()
    }

    // =============================================================================================
    // CountryUtils - Filter Tests
    // =============================================================================================

    @Test
    fun `filterByAllowedCountries returns only allowed countries`() {
        val allowedCodes = setOf("US", "GB", "FR")
        val filtered = CountryUtils.filterByAllowedCountries(allowedCodes)

        assertThat(filtered).hasSize(3)
        val countryCodes = filtered.map { it.countryCode }
        assertThat(countryCodes).containsExactly("US", "GB", "FR")
    }

    @Test
    fun `filterByAllowedCountries is case insensitive`() {
        val allowedCodes = setOf("us", "gb")
        val filtered = CountryUtils.filterByAllowedCountries(allowedCodes)

        assertThat(filtered).hasSize(2)
        val countryCodes = filtered.map { it.countryCode }
        assertThat(countryCodes).containsExactly("US", "GB")
    }

    @Test
    fun `filterByAllowedCountries returns all countries when set is empty`() {
        val filtered = CountryUtils.filterByAllowedCountries(emptySet())
        assertThat(filtered).hasSize(ALL_COUNTRIES.size)
    }

    @Test
    fun `filterByAllowedCountries returns empty list for non-existent codes`() {
        val allowedCodes = setOf("XX", "YY")
        val filtered = CountryUtils.filterByAllowedCountries(allowedCodes)
        assertThat(filtered).isEmpty()
    }

    // =============================================================================================
    // CountryUtils - Default Country Tests
    // =============================================================================================

    @Test
    fun `getDefaultCountry returns a valid country`() {
        val country = CountryUtils.getDefaultCountry()
        assertThat(country).isNotNull()
        assertThat(country.countryCode).isNotEmpty()
        assertThat(country.dialCode).isNotEmpty()
    }

    // =============================================================================================
    // CountryUtils - Formatting Tests
    // =============================================================================================

    @Test
    fun `formatPhoneNumber combines dial code and phone number`() {
        val formatted = CountryUtils.formatPhoneNumber("+1", "5551234567")
        assertThat(formatted).isEqualTo("+15551234567")
    }

    @Test
    fun `formatPhoneNumber removes non-numeric characters from phone number`() {
        val formatted = CountryUtils.formatPhoneNumber("+1", "(555) 123-4567")
        assertThat(formatted).isEqualTo("+15551234567")
    }

    @Test
    fun `formatPhoneNumber handles phone number with spaces`() {
        val formatted = CountryUtils.formatPhoneNumber("+44", "20 1234 5678")
        assertThat(formatted).isEqualTo("+442012345678")
    }
}
