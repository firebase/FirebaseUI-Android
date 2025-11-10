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
package com.firebase.ui.auth.util

import androidx.annotation.RestrictTo
import com.firebase.ui.auth.compose.util.CountryUtils

/**
 * Phone number validation utilities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PhoneNumberUtils {

    /**
     * Validates if a string starts with a valid dial code.
     * Accepts dial codes like "+1" or phone numbers like "+14155552671".
     * Does NOT validate the full phone number, only checks if it starts with a valid country code.
     *
     * @param number The dial code or phone number to validate (should start with "+")
     * @return true if the string starts with a valid country dial code, false otherwise
     */
    fun isValid(number: String): Boolean {
        if (!number.startsWith("+")) return false

        // Try to extract country code from the beginning (1-3 digits)
        val digitsOnly = number.drop(1).takeWhile { it.isDigit() }
        if (digitsOnly.isEmpty()) return false

        // Check if any prefix (1-3 digits) is a valid dial code
        for (length in 1..minOf(3, digitsOnly.length)) {
            val dialCode = "+${digitsOnly.take(length)}"
            if (CountryUtils.findByDialCode(dialCode).isNotEmpty()) {
                return true
            }
        }
        return false
    }

    /**
     * Validates if a country ISO code or dial code is valid.
     * Accepts both ISO codes (e.g., "US", "us") and dial codes (e.g., "+1").
     *
     * @param code The ISO 3166-1 alpha-2 country code or E.164 dial code
     * @return true if the code is a valid ISO code or dial code, false otherwise
     */
    fun isValidIso(code: String?): Boolean {
        if (code == null) return false

        // Check if it's a valid ISO country code (e.g., "US", "GB")
        if (CountryUtils.findByCountryCode(code) != null) {
            return true
        }

        // Check if it's a valid dial code (e.g., "+1", "+44")
        if (code.startsWith("+")) {
            return CountryUtils.findByDialCode(code).isNotEmpty()
        }

        return false
    }
}