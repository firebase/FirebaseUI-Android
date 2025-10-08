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

import android.content.Context
import android.content.res.Configuration
import com.firebase.ui.auth.R
import java.util.Locale

class DefaultAuthUIStringProvider(
    private val context: Context,
    private val locale: Locale? = null,
) : AuthUIStringProvider {
    /**
     * Allows overriding locale.
     */
    private val localizedContext = locale?.let { locale ->
        context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                setLocale(locale)
            }
        )
    } ?: context

    /**
     * Common Strings
     */
    override val initializing: String
        get() = "Initializing"

    /**
     * Auth Provider strings
     */
    override val googleProvider: String
        get() = localizedContext.getString(R.string.fui_idp_name_google)
    override val facebookProvider: String
        get() = localizedContext.getString(R.string.fui_idp_name_facebook)
    override val twitterProvider: String
        get() = localizedContext.getString(R.string.fui_idp_name_twitter)
    override val githubProvider: String
        get() = localizedContext.getString(R.string.fui_idp_name_github)
    override val phoneProvider: String
        get() = localizedContext.getString(R.string.fui_idp_name_phone)
    override val emailProvider: String
        get() = localizedContext.getString(R.string.fui_idp_name_email)

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

    /**
     * Email Authentication Strings
     */
    override val titleRegisterEmail: String
        get() = localizedContext.getString(R.string.fui_title_register_email)
    override val emailHint: String
        get() = localizedContext.getString(R.string.fui_email_hint)
    override val passwordHint: String
        get() = localizedContext.getString(R.string.fui_password_hint)
    override val newPasswordHint: String
        get() = localizedContext.getString(R.string.fui_new_password_hint)
    override val nameHint: String
        get() = localizedContext.getString(R.string.fui_name_hint)
    override val buttonTextSave: String
        get() = localizedContext.getString(R.string.fui_button_text_save)
    override val welcomeBackEmailHeader: String
        get() = localizedContext.getString(R.string.fui_welcome_back_email_header)
    override val troubleSigningIn: String
        get() = localizedContext.getString(R.string.fui_trouble_signing_in)

    /**
     * Phone Authentication Strings
     */
    override val verifyPhoneNumberTitle: String
        get() = localizedContext.getString(R.string.fui_verify_phone_number_title)
    override val phoneHint: String
        get() = localizedContext.getString(R.string.fui_phone_hint)
    override val countryHint: String
        get() = localizedContext.getString(R.string.fui_country_hint)
    override val invalidPhoneNumber: String
        get() = localizedContext.getString(R.string.fui_invalid_phone_number)
    override val enterConfirmationCode: String
        get() = localizedContext.getString(R.string.fui_enter_confirmation_code)
    override val verifyPhoneNumber: String
        get() = localizedContext.getString(R.string.fui_verify_phone_number)
    override val resendCodeIn: String
        get() = localizedContext.getString(R.string.fui_resend_code_in)
    override val resendCode: String
        get() = localizedContext.getString(R.string.fui_resend_code)
    override val verifying: String
        get() = localizedContext.getString(R.string.fui_verifying)
    override val incorrectCodeDialogBody: String
        get() = localizedContext.getString(R.string.fui_incorrect_code_dialog_body)
    override val smsTermsOfService: String
        get() = localizedContext.getString(R.string.fui_sms_terms_of_service)

    /**
     * Multi-Factor Authentication Strings
     */
    override val enterTOTPCode: String
        get() = "Enter TOTP Code"

    /**
     * Provider Picker Strings
     */
    override val signInDefault: String
        get() = localizedContext.getString(R.string.fui_sign_in_default)
    override val continueText: String
        get() = localizedContext.getString(R.string.fui_continue)
    override val nextDefault: String
        get() = localizedContext.getString(R.string.fui_next_default)

    /**
     * General Error Messages
     */
    override val errorUnknown: String
        get() = localizedContext.getString(R.string.fui_error_unknown)
    override val requiredField: String
        get() = localizedContext.getString(R.string.fui_required_field)
    override val progressDialogLoading: String
        get() = localizedContext.getString(R.string.fui_progress_dialog_loading)
    override val noInternet: String
        get() = localizedContext.getString(R.string.fui_no_internet)

    /**
     * Error Recovery Dialog Strings
     */
    override val errorDialogTitle: String
        get() = localizedContext.getString(R.string.fui_error_dialog_title)
    override val retryAction: String
        get() = localizedContext.getString(R.string.fui_error_retry_action)
    override val dismissAction: String
        get() = localizedContext.getString(R.string.fui_email_link_dismiss_button)
    override val networkErrorRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_no_internet)
    override val invalidCredentialsRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_invalid_password)
    override val userNotFoundRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_email_does_not_exist)
    override val weakPasswordRecoveryMessage: String
        get() = localizedContext.resources.getQuantityString(
            R.plurals.fui_error_weak_password,
            6,
            6
        )
    override val emailAlreadyInUseRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_email_account_creation_error)
    override val tooManyRequestsRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_too_many_attempts)
    override val mfaRequiredRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_mfa_required_message)
    override val accountLinkingRequiredRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_account_linking_required_message)
    override val authCancelledRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_auth_cancelled_message)
    override val unknownErrorRecoveryMessage: String
        get() = localizedContext.getString(R.string.fui_error_unknown)
}
