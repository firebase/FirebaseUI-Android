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

package com.firebase.ui.auth.compose.configuration.string_provider

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
    fun passwordTooShort(minimumLength: Int): String

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
    val signupPageTitle: String

    /** Hint for email input field */
    val emailHint: String

    /** Hint for password input field */
    val passwordHint: String

    /** Hint for confirm password input field */
    val confirmPasswordHint: String

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

    /** Title for recover password page */
    val recoverPasswordPageTitle: String

    /** Button text for reset password */
    val sendButtonText: String

    /** Title for recover password link sent dialog */
    val recoverPasswordLinkSentDialogTitle: String

    /** Body for recover password link sent dialog */
    fun recoverPasswordLinkSentDialogBody(email: String): String

    /** Title for email sign in link sent dialog */
    val emailSignInLinkSentDialogTitle: String

    /** Body for email sign in link sent dialog */
    fun emailSignInLinkSentDialogBody(email: String): String

    // Phone Authentication Strings
    /** Phone number entry form title */
    val verifyPhoneNumberTitle: String

    /** Hint for phone input field */
    val phoneHint: String

    /** Hint for country input field */
    val countryHint: String

    /** Invalid phone number error */
    val invalidPhoneNumber: String

    /** Missing phone number error */
    val missingPhoneNumber: String

    /** Phone verification code entry form title */
    val enterConfirmationCode: String

    /** Button text to verify phone number */
    val verifyPhoneNumber: String

    /** Resend code countdown timer */
    val resendCodeIn: String

    /** Resend code link text */
    val resendCode: String

    /** Resend code with timer */
    fun resendCodeTimer(timeFormatted: String): String

    /** Verifying progress text */
    val verifying: String

    /** Wrong verification code error */
    val incorrectCodeDialogBody: String

    /** SMS terms of service warning */
    val smsTermsOfService: String

    /** Enter phone number title */
    val enterPhoneNumberTitle: String

    /** Phone number hint */
    val phoneNumberHint: String

    /** Send verification code button text */
    val sendVerificationCode: String

    /** Enter verification code title with phone number */
    fun enterVerificationCodeTitle(phoneNumber: String): String

    /** Verification code hint */
    val verificationCodeHint: String

    /** Change phone number link text */
    val changePhoneNumber: String

    /** Missing verification code error */
    val missingVerificationCode: String

    /** Invalid verification code error */
    val invalidVerificationCode: String

    /** Select country modal sheet title */
    val countrySelectorModalTitle: String

    /** Select country modal sheet input field hint */
    val searchCountriesHint: String

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

    // Error Recovery Dialog Strings
    /** Error dialog title */
    val errorDialogTitle: String

    /** Retry action button text */
    val retryAction: String

    /** Dismiss action button text */
    val dismissAction: String

    /** Network error recovery message */
    val networkErrorRecoveryMessage: String

    /** Invalid credentials recovery message */
    val invalidCredentialsRecoveryMessage: String

    /** User not found recovery message */
    val userNotFoundRecoveryMessage: String

    /** Weak password recovery message */
    val weakPasswordRecoveryMessage: String

    /** Email already in use recovery message */
    val emailAlreadyInUseRecoveryMessage: String

    /** Too many requests recovery message */
    val tooManyRequestsRecoveryMessage: String

    /** MFA required recovery message */
    val mfaRequiredRecoveryMessage: String

    /** Account linking required recovery message */
    val accountLinkingRequiredRecoveryMessage: String

    /** Auth cancelled recovery message */
    val authCancelledRecoveryMessage: String

    /** Unknown error recovery message */
    val unknownErrorRecoveryMessage: String

    // MFA Enrollment Step Titles
    /** Title for MFA factor selection step */
    val mfaStepSelectFactorTitle: String

    /** Title for SMS MFA configuration step */
    val mfaStepConfigureSmsTitle: String

    /** Title for TOTP MFA configuration step */
    val mfaStepConfigureTotpTitle: String

    /** Title for MFA verification step */
    val mfaStepVerifyFactorTitle: String

    /** Title for recovery codes step */
    val mfaStepShowRecoveryCodesTitle: String

    // MFA Enrollment Helper Text
    /** Helper text for selecting MFA factor */
    val mfaStepSelectFactorHelper: String

    /** Helper text for SMS configuration */
    val mfaStepConfigureSmsHelper: String

    /** Helper text for TOTP configuration */
    val mfaStepConfigureTotpHelper: String

    /** Helper text for SMS verification */
    val mfaStepVerifyFactorSmsHelper: String

    /** Helper text for TOTP verification */
    val mfaStepVerifyFactorTotpHelper: String

    /** Generic helper text for factor verification */
    val mfaStepVerifyFactorGenericHelper: String

    /** Helper text for recovery codes */
    val mfaStepShowRecoveryCodesHelper: String
}
