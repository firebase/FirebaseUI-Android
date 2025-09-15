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
 * A sealed class representing a set of validation rules that can be applied to a password field,
 * typically within the [AuthProvider.Email] configuration.
 */
sealed class PasswordRule {
    /**
     * Requires the password to have at least a certain number of characters.
     */
    data class MinimumLength(val value: Int) : PasswordRule()

    /**
     * Requires the password to contain at least one uppercase letter (A-Z).
     */
    object RequireUppercase : PasswordRule()

    /**
     * Requires the password to contain at least one lowercase letter (a-z).
     */
    object RequireLowercase: PasswordRule()

    /**
     * Requires the password to contain at least one numeric digit (0-9).
     */
    object RequireDigit: PasswordRule()

    /**
     * Requires the password to contain at least one special character (e.g., !@#$%^&*).
     */
    object RequireSpecialCharacter: PasswordRule()

    /**
     * Defines a custom validation rule using a regular expression and provides a specific error
     * message on failure.
     */
    data class Custom(val regex: Regex, val errorMessage: String)
}