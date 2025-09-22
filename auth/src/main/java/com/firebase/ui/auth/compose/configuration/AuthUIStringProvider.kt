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

import android.content.Context
import com.firebase.ui.auth.R

/**
 * An interface for providing localized string resources. This interface defines methods for all
 * user-facing strings, such as initializing(), signInWithGoogle(), invalidEmail(),
 * passwordsDoNotMatch(), etc., allowing for complete localization of the UI.
 */
interface AuthUIStringProvider {
    /** Loading text displayed during initialization or processing states */
    val initializing: String

    /** Button text for Google sign-in option */
    val signInWithGoogle: String

    /** Error message when email address field is empty */
    val missingEmailAddress: String

    /** Error message when email address format is invalid */
    val invalidEmailAddress: String

    /** Generic error message for incorrect password during sign-in */
    val invalidPassword: String

    /** Error message when password confirmation doesn't match the original password */
    val passwordsDoNotMatch: String

    /** Error message when password doesn't meet minimum length requirement. Should support string formatting with minimum length parameter. */
    val passwordTooShort: String

    /** Error message when password is missing at least one uppercase letter (A-Z) */
    val passwordMissingUppercase: String

    /** Error message when password is missing at least one lowercase letter (a-z) */
    val passwordMissingLowercase: String

    /** Error message when password is missing at least one numeric digit (0-9) */
    val passwordMissingDigit: String

    /** Error message when password is missing at least one special character */
    val passwordMissingSpecialCharacter: String
}

internal class DefaultAuthUIStringProvider(private val context: Context) : AuthUIStringProvider {
    override val initializing: String get() = ""
    override val signInWithGoogle: String
        get() = context.getString(R.string.fui_sign_in_with_google)
    override val missingEmailAddress: String
        get() = context.getString(R.string.fui_missing_email_address)
    override val invalidEmailAddress: String
        get() = context.getString(R.string.fui_invalid_email_address)
    override val invalidPassword: String
        get() = context.getString(R.string.fui_error_invalid_password)
    override val passwordsDoNotMatch: String get() = ""
    override val passwordTooShort: String
        get() = context.getString(R.string.fui_error_password_too_short)
    override val passwordMissingUppercase: String
        get() = context.getString(R.string.fui_error_password_missing_uppercase)
    override val passwordMissingLowercase: String
        get() = context.getString(R.string.fui_error_password_missing_lowercase)
    override val passwordMissingDigit: String
        get() = context.getString(R.string.fui_error_password_missing_digit)
    override val passwordMissingSpecialCharacter: String
        get() = context.getString(R.string.fui_error_password_missing_special_character)
}
