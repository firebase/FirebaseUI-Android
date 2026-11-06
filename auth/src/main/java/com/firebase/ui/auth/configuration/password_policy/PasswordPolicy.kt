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

package com.firebase.ui.auth.configuration.password_policy

import org.json.JSONException
import org.json.JSONObject

/**
 * A Firebase project's password policy, as returned by Identity Toolkit `v2/passwordPolicy`.
 *
 * [minPasswordLength] defaults to 6 when the server omits it. A `null` length or `false`
 * character flag means the policy does not configure that requirement.
 */
internal data class PasswordPolicy(
    val minPasswordLength: Int,
    val maxPasswordLength: Int?,
    val containsLowercaseCharacter: Boolean,
    val containsUppercaseCharacter: Boolean,
    val containsNumericCharacter: Boolean,
    val containsNonAlphanumericCharacter: Boolean,
    val allowedNonAlphanumericCharacters: List<String>,
    val enforcementState: EnforcementState,
) {
    internal enum class EnforcementState { ENFORCE, OFF }

    private val allowedSymbols: Set<Char> by lazy {
        allowedNonAlphanumericCharacters.joinToString("").toSet()
    }

    /**
     * Evaluates [password] the way the JS SDK's `PasswordPolicyImpl.validatePassword` does.
     *
     * Length is UTF-16 code units (`String.length`, same as JS). Character classes are ASCII
     * only and checked independently, so a character in [allowedNonAlphanumericCharacters]
     * counts as a symbol even if it is also alphanumeric. Any other character is neither
     * counted nor rejected.
     */
    fun validate(password: String): PasswordValidationStatus {
        var lower = false
        var upper = false
        var numeric = false
        var symbol = false
        for (c in password) {
            if (c in 'a'..'z') lower = true
            if (c in 'A'..'Z') upper = true
            if (c in '0'..'9') numeric = true
            if (c in allowedSymbols) symbol = true
        }
        return PasswordValidationStatus(
            meetsMinPasswordLength = password.length >= minPasswordLength,
            meetsMaxPasswordLength = maxPasswordLength?.let { password.length <= it },
            containsLowercaseLetter = lower.takeIf { containsLowercaseCharacter },
            containsUppercaseLetter = upper.takeIf { containsUppercaseCharacter },
            containsNumericCharacter = numeric.takeIf { containsNumericCharacter },
            containsNonAlphanumericCharacter = symbol.takeIf { containsNonAlphanumericCharacter },
        )
    }

    companion object {
        internal const val DEFAULT_MIN_PASSWORD_LENGTH = 6
        private const val SUPPORTED_SCHEMA_VERSION = 1

        /**
         * Parses a `v2/passwordPolicy` response body. Returns `null` for malformed JSON or a
         * `schemaVersion` that is not the number 1 (missing, `"1"` and `1.9` are rejected, as with
         * the JS SDK's `!== 1`). Unknown enforcement states are OFF. A JSON `null` length is
         * treated as absent, as is a `maxPasswordLength` of 0 or less, as JS does for a falsy value.
         */
        fun parse(body: String): PasswordPolicy? = try {
            val json = JSONObject(body)
            val schemaVersion = json.opt("schemaVersion") as? Number
            if (schemaVersion?.toDouble() != SUPPORTED_SCHEMA_VERSION.toDouble()) {
                null
            } else {
                val options = json.optJSONObject("customStrengthOptions") ?: JSONObject()
                val symbols = json.optJSONArray("allowedNonAlphanumericCharacters")
                PasswordPolicy(
                    minPasswordLength = if (options.isNull("minPasswordLength")) {
                        DEFAULT_MIN_PASSWORD_LENGTH
                    } else {
                        options.getInt("minPasswordLength")
                    },
                    maxPasswordLength = if (options.isNull("maxPasswordLength")) {
                        null
                    } else {
                        options.getInt("maxPasswordLength").takeIf { it > 0 }
                    },
                    containsLowercaseCharacter = options.optBoolean("containsLowercaseCharacter"),
                    containsUppercaseCharacter = options.optBoolean("containsUppercaseCharacter"),
                    containsNumericCharacter = options.optBoolean("containsNumericCharacter"),
                    containsNonAlphanumericCharacter =
                        options.optBoolean("containsNonAlphanumericCharacter"),
                    allowedNonAlphanumericCharacters = if (symbols == null) {
                        emptyList()
                    } else {
                        List(symbols.length()) { symbols.getString(it) }
                    },
                    enforcementState = if (json.optString("enforcementState") == "ENFORCE") {
                        EnforcementState.ENFORCE
                    } else {
                        EnforcementState.OFF
                    },
                )
            }
        } catch (_: JSONException) {
            null
        }
    }
}

/**
 * Per-requirement result of [PasswordPolicy.validate]. A `null` field is a requirement the
 * policy does not configure; [isValid] is true when every configured requirement is met.
 */
internal data class PasswordValidationStatus(
    val meetsMinPasswordLength: Boolean,
    val meetsMaxPasswordLength: Boolean?,
    val containsLowercaseLetter: Boolean?,
    val containsUppercaseLetter: Boolean?,
    val containsNumericCharacter: Boolean?,
    val containsNonAlphanumericCharacter: Boolean?,
) {
    val isValid: Boolean
        get() = meetsMinPasswordLength &&
            meetsMaxPasswordLength != false &&
            containsLowercaseLetter != false &&
            containsUppercaseLetter != false &&
            containsNumericCharacter != false &&
            containsNonAlphanumericCharacter != false
}
