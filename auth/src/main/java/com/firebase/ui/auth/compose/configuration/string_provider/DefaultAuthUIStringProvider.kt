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
    context: Context,
    locale: Locale? = null,
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
    override val signInWithLine: String
        get() = localizedContext.getString(R.string.fui_sign_in_with_line)

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

    override fun passwordTooShort(minimumLength: Int): String =
        localizedContext.getString(R.string.fui_error_password_too_short, minimumLength)

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
    override val signupPageTitle: String
        get() = localizedContext.getString(R.string.fui_title_register_email)
    override val emailHint: String
        get() = localizedContext.getString(R.string.fui_email_hint)
    override val passwordHint: String
        get() = localizedContext.getString(R.string.fui_password_hint)
    override val confirmPasswordHint: String
        get() = localizedContext.getString(R.string.fui_confirm_password_hint)
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

    override val recoverPasswordPageTitle: String
        get() = localizedContext.getString(R.string.fui_title_recover_password_activity)

    override val sendButtonText: String
        get() = localizedContext.getString(R.string.fui_button_text_send)

    override val recoverPasswordLinkSentDialogTitle: String
        get() = localizedContext.getString(R.string.fui_title_confirm_recover_password)

    override fun recoverPasswordLinkSentDialogBody(email: String): String =
        localizedContext.getString(R.string.fui_confirm_recovery_body, email)

    override val emailSignInLinkSentDialogTitle: String
        get() = localizedContext.getString(R.string.fui_email_link_header)

    override fun emailSignInLinkSentDialogBody(email: String): String =
        localizedContext.getString(R.string.fui_email_link_email_sent, email)

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
    override val missingPhoneNumber: String
        get() = localizedContext.getString(R.string.fui_required_field)
    override val enterConfirmationCode: String
        get() = localizedContext.getString(R.string.fui_enter_confirmation_code)
    override val verifyPhoneNumber: String
        get() = localizedContext.getString(R.string.fui_verify_phone_number)
    override val resendCodeIn: String
        get() = localizedContext.getString(R.string.fui_resend_code_in)
    override val resendCode: String
        get() = localizedContext.getString(R.string.fui_resend_code)

    override fun resendCodeTimer(timeFormatted: String): String =
        localizedContext.getString(R.string.fui_resend_code_in, timeFormatted)

    override val verifying: String
        get() = localizedContext.getString(R.string.fui_verifying)
    override val incorrectCodeDialogBody: String
        get() = localizedContext.getString(R.string.fui_incorrect_code_dialog_body)
    override val smsTermsOfService: String
        get() = localizedContext.getString(R.string.fui_sms_terms_of_service)

    override val enterPhoneNumberTitle: String
        get() = localizedContext.getString(R.string.fui_verify_phone_number_title)

    override val phoneNumberHint: String
        get() = localizedContext.getString(R.string.fui_phone_hint)

    override val sendVerificationCode: String
        get() = localizedContext.getString(R.string.fui_next_default)

    override fun enterVerificationCodeTitle(phoneNumber: String): String =
        localizedContext.getString(R.string.fui_enter_confirmation_code) + " " + phoneNumber

    override val verificationCodeHint: String
        get() = localizedContext.getString(R.string.fui_enter_confirmation_code)

    override val changePhoneNumber: String
        get() = localizedContext.getString(R.string.fui_change_phone_number)

    override val missingVerificationCode: String
        get() = localizedContext.getString(R.string.fui_required_field)

    override val invalidVerificationCode: String
        get() = localizedContext.getString(R.string.fui_incorrect_code_dialog_body)

    override val countrySelectorModalTitle: String
        get() = localizedContext.getString(R.string.fui_country_selector_title)

    override val searchCountriesHint: String
        get() = localizedContext.getString(R.string.fui_search_country_field_hint)

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

    override fun signedInAs(userIdentifier: String): String =
        localizedContext.getString(R.string.fui_signed_in_as, userIdentifier)

    override val manageMfaAction: String
        get() = localizedContext.getString(R.string.fui_manage_mfa_action)

    override val signOutAction: String
        get() = localizedContext.getString(R.string.fui_sign_out_action)

    override fun verifyEmailInstruction(email: String): String =
        localizedContext.getString(R.string.fui_verify_email_instruction, email)

    override val resendVerificationEmailAction: String
        get() = localizedContext.getString(R.string.fui_resend_verification_email_action)

    override val verifiedEmailAction: String
        get() = localizedContext.getString(R.string.fui_verified_email_action)

    override val profileCompletionMessage: String
        get() = localizedContext.getString(R.string.fui_profile_completion_message)

    override fun profileMissingFieldsMessage(fields: String): String =
        localizedContext.getString(R.string.fui_profile_missing_fields_message, fields)

    override val skipAction: String
        get() = localizedContext.getString(R.string.fui_skip_action)

    override val removeAction: String
        get() = localizedContext.getString(R.string.fui_remove_action)

    override val backAction: String
        get() = localizedContext.getString(R.string.fui_back_action)

    override val verifyAction: String
        get() = localizedContext.getString(R.string.fui_verify_action)

    override val useDifferentMethodAction: String
        get() = localizedContext.getString(R.string.fui_use_different_method_action)

    override val recoveryCodesSavedAction: String
        get() = localizedContext.getString(R.string.fui_recovery_codes_saved_action)

    override val secretKeyLabel: String
        get() = localizedContext.getString(R.string.fui_secret_key_label)

    override val verificationCodeLabel: String
        get() = localizedContext.getString(R.string.fui_verification_code_label)

    override val identityVerifiedMessage: String
        get() = localizedContext.getString(R.string.fui_identity_verified_message)

    override val mfaManageFactorsTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_manage_factors_title)

    override val mfaManageFactorsDescription: String
        get() = localizedContext.getString(R.string.fui_mfa_manage_factors_description)

    override val mfaActiveMethodsTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_active_methods_title)

    override val mfaAddNewMethodTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_add_new_method_title)

    override val mfaAllMethodsEnrolledMessage: String
        get() = localizedContext.getString(R.string.fui_mfa_all_methods_enrolled_message)

    override val smsAuthenticationLabel: String
        get() = localizedContext.getString(R.string.fui_mfa_label_sms_authentication)

    override val totpAuthenticationLabel: String
        get() = localizedContext.getString(R.string.fui_mfa_label_totp_authentication)

    override val unknownMethodLabel: String
        get() = localizedContext.getString(R.string.fui_mfa_label_unknown_method)

    override fun enrolledOnDateLabel(date: String): String =
        localizedContext.getString(R.string.fui_mfa_enrolled_on, date)

    override val setupAuthenticatorDescription: String
        get() = localizedContext.getString(R.string.fui_mfa_setup_authenticator_description)
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

    /**
     * MFA Enrollment Step Titles
     */
    override val mfaStepSelectFactorTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_step_select_factor_title)
    override val mfaStepConfigureSmsTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_step_configure_sms_title)
    override val mfaStepConfigureTotpTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_step_configure_totp_title)
    override val mfaStepVerifyFactorTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_step_verify_factor_title)
    override val mfaStepShowRecoveryCodesTitle: String
        get() = localizedContext.getString(R.string.fui_mfa_step_show_recovery_codes_title)

    /**
     * MFA Enrollment Helper Text
     */
    override val mfaStepSelectFactorHelper: String
        get() = localizedContext.getString(R.string.fui_mfa_step_select_factor_helper)
    override val mfaStepConfigureSmsHelper: String
        get() = localizedContext.getString(R.string.fui_mfa_step_configure_sms_helper)
    override val mfaStepConfigureTotpHelper: String
        get() = localizedContext.getString(R.string.fui_mfa_step_configure_totp_helper)
    override val mfaStepVerifyFactorSmsHelper: String
        get() = localizedContext.getString(R.string.fui_mfa_step_verify_factor_sms_helper)
    override val mfaStepVerifyFactorTotpHelper: String
        get() = localizedContext.getString(R.string.fui_mfa_step_verify_factor_totp_helper)
    override val mfaStepVerifyFactorGenericHelper: String
        get() = localizedContext.getString(R.string.fui_mfa_step_verify_factor_generic_helper)
    override val mfaStepShowRecoveryCodesHelper: String
        get() = localizedContext.getString(R.string.fui_mfa_step_show_recovery_codes_helper)

    // MFA Enrollment Screen Titles
    override val mfaEnrollmentEnterPhoneNumber: String
        get() = localizedContext.getString(R.string.fui_mfa_enrollment_enter_phone_number)
    override val mfaEnrollmentVerifySmsCode: String
        get() = localizedContext.getString(R.string.fui_mfa_enrollment_verify_sms_code)

    // MFA Error Messages
    override val mfaErrorRecentLoginRequired: String
        get() = localizedContext.getString(R.string.fui_mfa_error_recent_login_required)
    override val mfaErrorInvalidVerificationCode: String
        get() = localizedContext.getString(R.string.fui_mfa_error_invalid_verification_code)
    override val mfaErrorNetwork: String
        get() = localizedContext.getString(R.string.fui_mfa_error_network)
    override val mfaErrorGeneric: String
        get() = localizedContext.getString(R.string.fui_mfa_error_generic)

    override val reauthDialogTitle: String
        get() = localizedContext.getString(R.string.fui_reauth_dialog_title)

    override val reauthDialogMessage: String
        get() = localizedContext.getString(R.string.fui_reauth_dialog_message)

    override fun reauthAccountLabel(email: String): String =
        localizedContext.getString(R.string.fui_reauth_account_label, email)

    override val incorrectPasswordError: String
        get() = localizedContext.getString(R.string.fui_incorrect_password_error)

    override val reauthGenericError: String
        get() = localizedContext.getString(R.string.fui_reauth_generic_error)

    override val termsOfService: String
        get() = localizedContext.getString(R.string.fui_terms_of_service)

    override val privacyPolicy: String
        get() = localizedContext.getString(R.string.fui_privacy_policy)

    override fun tosAndPrivacyPolicy(termsOfServiceLabel: String, privacyPolicyLabel: String): String =
        localizedContext.getString(R.string.fui_tos_and_pp, termsOfServiceLabel, privacyPolicyLabel)
}
