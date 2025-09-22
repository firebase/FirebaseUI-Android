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

package com.firebase.ui.auth.compose.configuration.stringprovider

/**
 * An interface for providing localized string resources. This interface defines methods for all
 * user-facing strings, such as initializing(), signInWithGoogle(), invalidEmailAddress(),
 * passwordsDoNotMatch(), etc., allowing for complete localization of the UI.
 *
 * @sample AuthUIStringProviderSample
 */
interface AuthUIStringProvider {
    /** Loading text displayed during initialization or processing states */
    val initializing: String

    /** Text for Google Provider */
    val googleProvider: String

    /** Text for Facebook Provider */
    val facebookProvider: String

    /** Text for Twitter Provider */
    val twitterProvider: String

    /** Text for Github Provider */
    val githubProvider: String

    /** Text for Phone Provider */
    val phoneProvider: String

    /** Text for Email Provider */
    val emailProvider: String

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

    // Email Authentication Strings
    /** Title for email signup form */
    val titleRegisterEmail: String

    /** Hint for email input field */
    val emailHint: String

    /** Hint for password input field */
    val passwordHint: String

    /** Hint for new password input field */
    val newPasswordHint: String

    /** Hint for name input field */
    val nameHint: String

    /** Button text to save form */
    val buttonTextSave: String

    /** Welcome back header for email users */
    val welcomeBackEmailHeader: String

    /** Trouble signing in link text */
    val troubleSigningIn: String

    // Phone Authentication Strings
    /** Phone number entry form title */
    val verifyPhoneNumberTitle: String

    /** Hint for phone input field */
    val phoneHint: String

    /** Hint for country input field */
    val countryHint: String

    /** Invalid phone number error */
    val invalidPhoneNumber: String

    /** Phone verification code entry form title */
    val enterConfirmationCode: String

    /** Button text to verify phone number */
    val verifyPhoneNumber: String

    /** Resend code countdown timer */
    val resendCodeIn: String

    /** Resend code link text */
    val resendCode: String

    /** Verifying progress text */
    val verifying: String

    /** Wrong verification code error */
    val incorrectCodeDialogBody: String

    /** SMS terms of service warning */
    val smsTermsOfService: String

    // Provider Picker Strings
    /** Common button text for sign in */
    val signInDefault: String

    /** Common button text for continue */
    val continueText: String

    /** Common button text for next */
    val nextDefault: String

    // General Error Messages
    /** General unknown error message */
    val errorUnknown: String

    /** Required field error */
    val requiredField: String

    /** Loading progress text */
    val progressDialogLoading: String

    /** Network error message */
    val noInternet: String

    /** TOTP Code prompt */
    val enterTOTPCode: String
}
