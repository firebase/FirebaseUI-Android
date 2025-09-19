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

import com.firebase.ui.auth.compose.configuration.validators.ValidationStatus

/**
 * A sealed class representing password validation rules with embedded validation logic.
 */
sealed class PasswordRule {
    /**
     * Requires the password to have at least a certain number of characters.
     */
    class MinimumLength(val value: Int) : PasswordRule() {
        override fun validate(
            stringProvider: AuthUIStringProvider,
            password: String
        ): ValidationStatus {
            return if (password.length >= this@MinimumLength.value) {
                ValidationStatus(hasError = false)
            } else {
                ValidationStatus(
                    hasError = true,
                    errorMessage = stringProvider.passwordTooShort.format(this@MinimumLength.value)
                )
            }
        }
    }

    /**
     * Requires the password to contain at least one uppercase letter (A-Z).
     */
    object RequireUppercase : PasswordRule() {
        override fun validate(
            stringProvider: AuthUIStringProvider,
            password: String
        ): ValidationStatus {
            return if (password.any { it.isUpperCase() }) {
                ValidationStatus(hasError = false)
            } else {
                ValidationStatus(
                    hasError = true,
                    errorMessage = stringProvider.passwordMissingUppercase
                )
            }
        }
    }

    /**
     * Requires the password to contain at least one lowercase letter (a-z).
     */
    object RequireLowercase : PasswordRule() {
        override fun validate(
            stringProvider: AuthUIStringProvider,
            password: String
        ): ValidationStatus {
            return if (password.any { it.isLowerCase() }) {
                ValidationStatus(hasError = false)
            } else {
                ValidationStatus(
                    hasError = true,
                    errorMessage = stringProvider.passwordMissingLowercase
                )
            }
        }
    }

    /**
     * Requires the password to contain at least one numeric digit (0-9).
     */
    object RequireDigit : PasswordRule() {
        override fun validate(
            stringProvider: AuthUIStringProvider,
            password: String
        ): ValidationStatus {
            return if (password.any { it.isDigit() }) {
                ValidationStatus(hasError = false)
            } else {
                ValidationStatus(
                    hasError = true,
                    errorMessage = stringProvider.passwordMissingDigit
                )
            }
        }
    }

    /**
     * Requires the password to contain at least one special character (e.g., !@#$%^&*).
     */
    object RequireSpecialCharacter : PasswordRule() {
        private val specialCharacters = "!@#$%^&*()_+-=[]{}|;:,.<>?".toSet()

        override fun validate(
            stringProvider: AuthUIStringProvider,
            password: String
        ): ValidationStatus {
            return if (password.any { it in specialCharacters }) {
                ValidationStatus(hasError = false)
            } else {
                ValidationStatus(
                    hasError = true,
                    errorMessage = stringProvider.passwordMissingSpecialCharacter
                )
            }
        }
    }

    /**
     * Defines a custom validation rule using a regular expression.
     */
    class Custom(
        val regex: Regex,
        val errorMessage: String
    ) : PasswordRule() {
        override fun validate(
            stringProvider: AuthUIStringProvider,
            password: String
        ): ValidationStatus {
            return if (regex.matches(password)) {
                ValidationStatus(hasError = false)
            } else {
                ValidationStatus(
                    hasError = true,
                    errorMessage = errorMessage
                )
            }
        }
    }

    internal abstract fun validate(
        stringProvider: AuthUIStringProvider,
        password: String
    ): ValidationStatus
}