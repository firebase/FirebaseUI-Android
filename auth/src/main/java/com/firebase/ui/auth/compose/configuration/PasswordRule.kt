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

package com.firebase.ui.auth.compose.configuration

/**
 * A abstract class representing password validation rules with embedded validation logic.
 */
abstract class PasswordRule {
    /**
     * Requires the password to have at least a certain number of characters.
     */
    class MinimumLength(val value: Int) : PasswordRule() {
        override fun isValid(password: String): Boolean {
            return password.length >= this@MinimumLength.value
        }

        override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
            return stringProvider.passwordTooShort.format(value)
        }
    }

    /**
     * Requires the password to contain at least one uppercase letter (A-Z).
     */
    object RequireUppercase : PasswordRule() {
        override fun isValid(password: String): Boolean {
            return password.any { it.isUpperCase() }
        }

        override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
            return stringProvider.passwordMissingUppercase
        }
    }

    /**
     * Requires the password to contain at least one lowercase letter (a-z).
     */
    object RequireLowercase : PasswordRule() {
        override fun isValid(password: String): Boolean {
            return password.any { it.isLowerCase() }
        }

        override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
            return stringProvider.passwordMissingLowercase
        }
    }

    /**
     * Requires the password to contain at least one numeric digit (0-9).
     */
    object RequireDigit : PasswordRule() {
        override fun isValid(password: String): Boolean {
            return password.any { it.isDigit() }
        }

        override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
            return stringProvider.passwordMissingDigit
        }
    }

    /**
     * Requires the password to contain at least one special character (e.g., !@#$%^&*).
     */
    object RequireSpecialCharacter : PasswordRule() {
        private val specialCharacters = "!@#$%^&*()_+-=[]{}|;:,.<>?".toSet()

        override fun isValid(password: String): Boolean {
            return password.any { it in specialCharacters }
        }

        override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
            return stringProvider.passwordMissingSpecialCharacter
        }
    }

    /**
     * Defines a custom validation rule using a regular expression.
     */
    class Custom(
        val regex: Regex,
        val errorMessage: String
    ) : PasswordRule() {
        override fun isValid(password: String): Boolean {
            return regex.matches(password)
        }

        override fun getErrorMessage(stringProvider: AuthUIStringProvider): String {
            return errorMessage
        }
    }

    /**
     * Validates whether the given password meets this rule's requirements.
     *
     * @param password The password to validate
     * @return true if the password meets this rule's requirements, false otherwise
     */
    internal abstract fun isValid(password: String): Boolean

    /**
     * Returns the appropriate error message for this rule when validation fails.
     *
     * @param stringProvider The string provider for localized error messages
     * @return The localized error message for this rule
     */
    internal abstract fun getErrorMessage(stringProvider: AuthUIStringProvider): String
}