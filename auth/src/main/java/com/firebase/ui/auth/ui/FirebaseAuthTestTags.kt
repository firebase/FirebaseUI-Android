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

package com.firebase.ui.auth.ui

/**
 * Stable Compose test tags applied by the FirebaseUI Auth screens. These are public API — renaming
 * or removing a constant is a breaking change, not an internal refactor.
 */
object FirebaseAuthTestTags {

    /** Tags on the auth method picker screen. */
    object MethodPicker {

        /** The scrollable list of provider buttons. */
        const val PROVIDER_LIST = "fui_method_picker_provider_list"

        /**
         * The "Continue as ..." button, shown when a previous sign-in preference is available.
         */
        const val CONTINUE_AS_BUTTON = "fui_method_picker_continue_as_button"
    }

    /** Tags on the phone number country selector bottom sheet. */
    object CountrySelector {

        /** The scrollable country list. */
        const val COUNTRY_LIST = "fui_country_selector_country_list"
    }

    /** Tags on the email/password sign-in screen. */
    object SignIn {

        /** The email address input. */
        const val EMAIL_FIELD = "fui_sign_in_email_field"

        /** The password input. */
        const val PASSWORD_FIELD = "fui_sign_in_password_field"

        /** The button that submits the entered credentials. */
        const val SIGN_IN_BUTTON = "fui_sign_in_sign_in_button"

        /** The button that navigates to the sign-up screen. */
        const val SIGN_UP_BUTTON = "fui_sign_in_sign_up_button"

        /** The "trouble signing in" button that navigates to password recovery. */
        const val FORGOT_PASSWORD_BUTTON = "fui_sign_in_forgot_password_button"

        /** The button that switches to email link sign-in. */
        const val EMAIL_LINK_BUTTON = "fui_sign_in_email_link_button"

        /** The toggle that shows or hides the entered password. */
        const val PASSWORD_VISIBILITY_TOGGLE = "fui_sign_in_password_visibility_toggle"

        /** The top app bar's back navigation button. */
        const val BACK_BUTTON = "fui_sign_in_back_button"

        /** The notice explaining that reauthentication here needs the account's password. */
        const val REAUTH_PASSWORD_NOTICE = "fui_sign_in_reauth_password_notice"
    }

    /** Tags on the email/password sign-up screen. */
    object SignUp {

        /** The display name input, shown when the provider requires a name. */
        const val NAME_FIELD = "fui_sign_up_name_field"

        /** The email address input. */
        const val EMAIL_FIELD = "fui_sign_up_email_field"

        /** The password input. */
        const val PASSWORD_FIELD = "fui_sign_up_password_field"

        /** The password confirmation input. */
        const val CONFIRM_PASSWORD_FIELD = "fui_sign_up_confirm_password_field"

        /** The button that submits the new account. */
        const val SIGN_UP_BUTTON = "fui_sign_up_sign_up_button"

        /** The button that navigates back to the sign-in screen. */
        const val SIGN_IN_BUTTON = "fui_sign_up_sign_in_button"

        /** The toggle that shows or hides the entered password. */
        const val PASSWORD_VISIBILITY_TOGGLE = "fui_sign_up_password_visibility_toggle"

        /** The toggle that shows or hides the entered password confirmation. */
        const val CONFIRM_PASSWORD_VISIBILITY_TOGGLE =
            "fui_sign_up_confirm_password_visibility_toggle"

        /** The top app bar's back navigation button. */
        const val BACK_BUTTON = "fui_sign_up_back_button"
    }

    /** Tags on the password recovery screen. */
    object ResetPassword {

        /** The email address input. */
        const val EMAIL_FIELD = "fui_reset_password_email_field"

        /** The button that sends the password reset link. */
        const val SEND_BUTTON = "fui_reset_password_send_button"

        /** The button that navigates back to the sign-in screen. */
        const val SIGN_IN_BUTTON = "fui_reset_password_sign_in_button"

        /** The dismiss button of the "reset link sent" dialog. */
        const val DISMISS_BUTTON = "fui_reset_password_dismiss_button"

        /** The top app bar's back navigation button. */
        const val BACK_BUTTON = "fui_reset_password_back_button"
    }

    /** Tags on the email link ("magic link") sign-in screen. */
    object EmailLink {

        /** The email address input. */
        const val EMAIL_FIELD = "fui_email_link_email_field"

        /** The button that sends the sign-in link. */
        const val SEND_LINK_BUTTON = "fui_email_link_send_link_button"

        /** The button that switches back to password sign-in. */
        const val PASSWORD_SIGN_IN_BUTTON = "fui_email_link_password_sign_in_button"

        /** The dismiss button of the "sign-in link sent" dialog. */
        const val DISMISS_BUTTON = "fui_email_link_dismiss_button"

        /** The "trouble signing in" button that navigates to password recovery. */
        const val FORGOT_PASSWORD_BUTTON = "fui_email_link_forgot_password_button"

        /** The top app bar's back navigation button. */
        const val BACK_BUTTON = "fui_email_link_back_button"
    }

    /** Tags on the phone number entry screen. */
    object PhoneNumber {

        /** The phone number input. */
        const val PHONE_NUMBER_FIELD = "fui_phone_number_phone_number_field"

        /** The control that opens the country selector bottom sheet. */
        const val COUNTRY_SELECTOR_BUTTON = "fui_phone_number_country_selector_button"

        /** The button that requests an SMS verification code. */
        const val SEND_CODE_BUTTON = "fui_phone_number_send_code_button"

        /** The top app bar's back navigation button. */
        const val BACK_BUTTON = "fui_phone_number_back_button"
    }

    /** Tags on the SMS verification code screen. */
    object VerificationCode {

        /**
         * The verification code input group (one box per digit). The group itself is the
         * editable node, so a single action enters the whole code.
         */
        const val CODE_FIELD = "fui_verification_code_code_field"

        /** The button that submits the entered code. */
        const val VERIFY_BUTTON = "fui_verification_code_verify_button"

        /** The button that requests a new code. */
        const val RESEND_CODE_BUTTON = "fui_verification_code_resend_code_button"

        /** The button that returns to phone number entry. */
        const val CHANGE_PHONE_NUMBER_BUTTON = "fui_verification_code_change_phone_number_button"

        /** The top app bar's back navigation button. */
        const val BACK_BUTTON = "fui_verification_code_back_button"
    }

    /**
     * Tags on the multi-factor sign-in challenge screen (the second factor requested during
     * sign-in, distinct from MFA enrollment).
     */
    object MfaChallenge {

        /** The verification code input, for both the SMS and TOTP factors. */
        const val CODE_FIELD = "fui_mfa_challenge_code_field"

        /** The button that submits the entered code. */
        const val VERIFY_BUTTON = "fui_mfa_challenge_verify_button"

        /** The button that requests a new code. SMS factor only. */
        const val RESEND_CODE_BUTTON = "fui_mfa_challenge_resend_code_button"

        /**
         * The button that cancels the challenge and returns to sign-in. Shared by the SMS and
         * TOTP variants, which never compose at the same time.
         */
        const val CANCEL_BUTTON = "fui_mfa_challenge_cancel_button"
    }

    /**
     * Tags on the re-authentication dialog. Kept separate from [SignIn] because the dialog and
     * the flow behind it can be composed at the same time.
     */
    object Reauth {

        /** The password input. */
        const val PASSWORD_FIELD = "fui_reauth_password_field"

        /** The button that submits the password. */
        const val VERIFY_BUTTON = "fui_reauth_verify_button"

        /** The button that dismisses the dialog without re-authenticating. */
        const val DISMISS_BUTTON = "fui_reauth_dismiss_button"
    }

    /** Tags on the error recovery dialog. Only one instance is ever composed at a time. */
    object ErrorRecovery {

        /** The recovery/retry action; its label varies with the error being recovered from. */
        const val RETRY_BUTTON = "fui_error_recovery_retry_button"

        /** The button that dismisses the dialog without recovering. */
        const val DISMISS_BUTTON = "fui_error_recovery_dismiss_button"
    }

    /**
     * Tags on the terms-of-service and privacy-policy links shown at the bottom of several
     * screens. Component-scoped since call sites never compose more than one instance at once.
     */
    object TermsAndPrivacy {

        /** The link that opens the terms-of-service URL. */
        const val TOS_LINK = "fui_terms_and_privacy_tos_link"

        /** The link that opens the privacy-policy URL. */
        const val PRIVACY_LINK = "fui_terms_and_privacy_privacy_link"
    }

    /**
     * Tags on the multi-factor enrollment flow. Enroll/remove buttons are keyed per-factor since
     * more than one can be shown at once.
     */
    object MfaEnrollment {

        /** The button that starts SMS enrollment, shown on the factor-selection step. */
        const val ENROLL_SMS_BUTTON = "fui_mfa_enrollment_enroll_sms_button"

        /** The button that starts TOTP enrollment, shown on the factor-selection step. */
        const val ENROLL_TOTP_BUTTON = "fui_mfa_enrollment_enroll_totp_button"

        /** The button that removes an already-enrolled SMS factor. */
        const val REMOVE_SMS_BUTTON = "fui_mfa_enrollment_remove_sms_button"

        /** The button that removes an already-enrolled TOTP factor. */
        const val REMOVE_TOTP_BUTTON = "fui_mfa_enrollment_remove_totp_button"

        /** The button that skips enrollment, shown on the factor-selection step when optional. */
        const val SKIP_BUTTON = "fui_mfa_enrollment_skip_button"

        /** The button that returns from the TOTP secret/QR step to factor selection. */
        const val CONFIGURE_TOTP_BACK_BUTTON = "fui_mfa_enrollment_configure_totp_back_button"

        /** The button that advances from the TOTP secret/QR step to code verification. */
        const val CONFIGURE_TOTP_CONTINUE_BUTTON =
            "fui_mfa_enrollment_configure_totp_continue_button"

        /**
         * The input for the code generated by the user's authenticator app, on the TOTP
         * verification step.
         */
        const val VERIFY_TOTP_CODE_FIELD = "fui_mfa_enrollment_verify_totp_code_field"

        /** The button that returns from TOTP code verification to the secret/QR step. */
        const val VERIFY_TOTP_BACK_BUTTON = "fui_mfa_enrollment_verify_totp_back_button"

        /** The button that submits the entered TOTP code to complete verification. */
        const val VERIFY_TOTP_BUTTON = "fui_mfa_enrollment_verify_totp_button"
    }
}
