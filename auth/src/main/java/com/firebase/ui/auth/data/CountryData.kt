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

/**
 * Represents country information for phone number authentication.
 *
 * @property name The display name of the country (e.g., "United States").
 * @property dialCode The international dialing code (e.g., "+1").
 * @property countryCode The ISO 3166-1 alpha-2 country code (e.g., "US").
 * @property flagEmoji The flag emoji for the country (e.g., "🇺🇸").
 */
data class CountryData(
    val name: String,
    val dialCode: String,
    val countryCode: String,
    val flagEmoji: String
) {
    /**
     * Returns a formatted display string combining flag emoji and country name.
     */
    fun getDisplayName(): String = "$flagEmoji $name"

    /**
     * Returns a formatted string with dial code.
     */
    fun getDisplayNameWithDialCode(): String = "$flagEmoji $name ($dialCode)"
}

/**
 * Converts an ISO 3166-1 alpha-2 country code to its corresponding flag emoji.
 *
 * @param countryCode The two-letter country code (e.g., "US", "GB", "FR").
 * @return The flag emoji string, or an empty string if the code is invalid.
 */
fun countryCodeToFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return ""

    val uppercaseCode = countryCode.uppercase()
    val baseCodePoint = 0x1F1E6 // Regional Indicator Symbol Letter A
    val charCodeOffset = 'A'.code

    val firstChar = uppercaseCode[0].code
    val secondChar = uppercaseCode[1].code

    val firstCodePoint = baseCodePoint + (firstChar - charCodeOffset)
    val secondCodePoint = baseCodePoint + (secondChar - charCodeOffset)

    return String(intArrayOf(firstCodePoint, secondCodePoint), 0, 2)
}
