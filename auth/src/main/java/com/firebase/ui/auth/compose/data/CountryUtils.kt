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

import java.text.Normalizer
import java.util.Locale

/**
 * Utility functions for searching and filtering countries.
 */
object CountryUtils {

    // Lazy-initialized maps for fast lookups
    private val countryCodeMap: Map<String, CountryData> by lazy {
        ALL_COUNTRIES.associateBy { it.countryCode.uppercase() }
    }

    private val dialCodeMap: Map<String, List<CountryData>> by lazy {
        ALL_COUNTRIES.groupBy { it.dialCode }
    }

    /**
     * Finds a country by its ISO 3166-1 alpha-2 country code.
     *
     * @param countryCode The two-letter country code (e.g., "US", "GB").
     * @return The CountryData or null if not found.
     */
    fun findByCountryCode(countryCode: String): CountryData? {
        return countryCodeMap[countryCode.uppercase()]
    }

    /**
     * Finds all countries with the given dial code.
     *
     * @param dialCode The international dialing code (e.g., "+1", "+44").
     * @return List of countries with that dial code, or empty list if none found.
     */
    fun findByDialCode(dialCode: String): List<CountryData> {
        return dialCodeMap[dialCode] ?: emptyList()
    }

    /**
     * Searches for countries by name. Supports partial matching and diacritic-insensitive search.
     *
     * @param query The search query.
     * @return List of countries matching the query, or empty list if none found.
     */
    fun searchByName(query: String): List<CountryData> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return emptyList()

        val normalizedQuery = normalizeString(trimmedQuery)

        return ALL_COUNTRIES.filter { country ->
            normalizeString(country.name).contains(normalizedQuery, ignoreCase = true)
        }
    }

    /**
     * Filters countries by allowed country codes.
     *
     * @param allowedCountryCodes Set of allowed ISO 3166-1 alpha-2 country codes.
     * @return List of countries that are in the allowed set.
     */
    fun filterByAllowedCountries(allowedCountryCodes: Set<String>): List<CountryData> {
        if (allowedCountryCodes.isEmpty()) return ALL_COUNTRIES

        val uppercaseAllowed = allowedCountryCodes.map { it.uppercase() }.toSet()
        return ALL_COUNTRIES.filter { it.countryCode.uppercase() in uppercaseAllowed }
    }

    /**
     * Gets the default country based on the device's locale.
     *
     * @return The CountryData for the device's country, or United States as fallback.
     */
    fun getDefaultCountry(): CountryData {
        val deviceCountryCode = Locale.getDefault().country
        return findByCountryCode(deviceCountryCode) ?: findByCountryCode("US")!!
    }

    /**
     * Formats a phone number with the country's dial code.
     *
     * @param dialCode The country dial code (e.g., "+1").
     * @param phoneNumber The local phone number.
     * @return The formatted international phone number.
     */
    fun formatPhoneNumber(dialCode: String, phoneNumber: String): String {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        return "$dialCode$cleanNumber"
    }

    /**
     * Normalizes a string by removing diacritics and converting to lowercase.
     *
     * @param value The string to normalize.
     * @return The normalized string.
     */
    private fun normalizeString(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase()
    }
}
