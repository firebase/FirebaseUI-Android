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
import android.content.res.Configuration
import com.firebase.ui.auth.R
import java.util.Locale

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

    /** Button text for Facebook sign-in option */
    val signInWithFacebook: String

    /** Button text for Twitter sign-in option */
    val signInWithTwitter: String

    /** Button text for Github sign-in option */
    val signInWithGithub: String

    /** Button text for Email sign-in option */
    val signInWithEmail: String

    /** Button text for Phone sign-in option */
    val signInWithPhone: String

    /** Button text for Anonymous sign-in option */
    val signInAnonymously: String

    /** Button text for Apple sign-in option */
    val signInWithApple: String

    /** Button text for Microsoft sign-in option */
    val signInWithMicrosoft: String

    /** Button text for Yahoo sign-in option */
    val signInWithYahoo: String

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

internal class DefaultAuthUIStringProvider(
    private val context: Context,
    private val locale: Locale? = null,
) : AuthUIStringProvider {

    private val localizedContext = locale?.let { locale ->
        context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                setLocale(locale)
            }
        )
    } ?: context

    /**
     * General Strings
     */
    override val initializing: String
        get() = ""

    /**
     * Auth Provider Button Strings
     */
    override val signInWithGoogle: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_google)
    override val signInWithFacebook: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_facebook)
    override val signInWithTwitter: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_twitter)
    override val signInWithGithub: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_github)
    override val signInWithEmail: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_email)
    override val signInWithPhone: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_phone)
    override val signInAnonymously: String
        get() = localizedContext.getString(R.string.fui_sign_in_anonymously)
    override val signInWithApple: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_apple)
    override val signInWithMicrosoft: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_microsoft)
    override val signInWithYahoo: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_yahoo)

    /**
     * Email Validator Strings
     */
    override val missingEmailAddress: String
        get() = localizedContext.getString(R.string.fui_missing_email_address)
    override val invalidEmailAddress: String
        get() = localizedContext.getString(R.string.fui_invalid_email_address)

    /**
     * Password Validator Strings
     */
    override val invalidPassword: String
        get() = localizedContext.getString(R.string.fui_error_invalid_password)
    override val passwordsDoNotMatch: String
        get() = localizedContext.getString(R.string.fui_passwords_do_not_match)
    override val passwordTooShort: String
        get() = localizedContext.getString(R.string.fui_error_password_too_short)
    override val passwordMissingUppercase: String
        get() = localizedContext.getString(R.string.fui_error_password_missing_uppercase)
    override val passwordMissingLowercase: String
        get() = localizedContext.getString(R.string.fui_error_password_missing_lowercase)
    override val passwordMissingDigit: String
        get() = localizedContext.getString(R.string.fui_error_password_missing_digit)
    override val passwordMissingSpecialCharacter: String
        get() = localizedContext.getString(R.string.fui_error_password_missing_special_character)
}
