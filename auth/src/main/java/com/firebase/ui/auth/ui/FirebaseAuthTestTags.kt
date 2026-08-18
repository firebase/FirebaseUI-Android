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
 * Stable Compose test tags applied by the FirebaseUI Auth screens.
 *
 * These values are **public API**. Host applications reference them from their own UI tests, and
 * they are intended to be surfaced as Android resource ids so that Firebase Test Lab Robo
 * directives and Play pre-launch reports can target the auth surfaces. Renaming or removing a
 * constant — or changing the string a constant resolves to — is a **breaking change**, not an
 * internal refactor.
 *
 * Constants are grouped by the screen or surface that owns them, and every value repeats that
 * surface as a segment so a `By.res` prefix match selects exactly one surface. Within a group the
 * element type is the **last** token of both the constant name and the value, so sibling nodes sort
 * and complete together.
 *
 * Values are lowercase `snake_case` with a `fui_` prefix. The prefix is not decoration: once these
 * tags are exposed as resource ids they land in the host application's `id` namespace, so it
 * namespaces our tags away from the host app's own resource ids and keeps `By.res()` lookups
 * unambiguous.
 *
 * ## Usage Example:
 *
 * ```kotlin
 * composeTestRule
 *     .onNodeWithTag(FirebaseAuthTestTags.MethodPicker.PROVIDER_LIST)
 *     .performScrollToNode(hasText("Sign in with Google"))
 * ```
 *
 * @since 10.0.0
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
    }

    /** Tags on the phone number entry screen. */
    object PhoneNumber {

        /** The phone number input. */
        const val PHONE_NUMBER_FIELD = "fui_phone_number_phone_number_field"

        /** The control that opens the country selector bottom sheet. */
        const val COUNTRY_SELECTOR_BUTTON = "fui_phone_number_country_selector_button"

        /** The button that requests an SMS verification code. */
        const val SEND_CODE_BUTTON = "fui_phone_number_send_code_button"
    }

    /** Tags on the SMS verification code screen. */
    object VerificationCode {

        /**
         * The verification code input.
         *
         * The code is drawn as one box per digit, and this names the group rather than any one box.
         * The group is the editable node: it accepts a whole code in a single `ACTION_SET_TEXT` or
         * `performTextInput` and spreads it across the boxes, so one Robo directive
         * (`{"resourceName": "fui_verification_code_code_field", "inputText": "123456"}`) enters the
         * whole thing. See
         * [com.firebase.ui.auth.ui.components.VerificationCodeInputField] for why the boxes
         * themselves are not addressable.
         */
        const val CODE_FIELD = "fui_verification_code_code_field"

        /** The button that submits the entered code. */
        const val VERIFY_BUTTON = "fui_verification_code_verify_button"

        /** The button that requests a new code. */
        const val RESEND_CODE_BUTTON = "fui_verification_code_resend_code_button"

        /** The button that returns to phone number entry. */
        const val CHANGE_PHONE_NUMBER_BUTTON = "fui_verification_code_change_phone_number_button"
    }

    /**
     * Tags on the multi-factor sign-in challenge screen — the second factor a user is asked for
     * after their password, which is part of a plain sign-in and not of MFA enrollment.
     *
     * Separate from [VerificationCode] even though both screens show the same code input: the SMS
     * step of a phone sign-in and the second-factor challenge are different screens reached by
     * different routes, and giving them one value would leave a `By.res` match unable to say which
     * screen it landed on.
     */
    object MfaChallenge {

        /**
         * The verification code input, for both the SMS and TOTP factors.
         *
         * Accepts a whole code in one action, exactly as
         * [VerificationCode.CODE_FIELD] does — the two screens share the input widget.
         */
        const val CODE_FIELD = "fui_mfa_challenge_code_field"

        /** The button that submits the entered code. */
        const val VERIFY_BUTTON = "fui_mfa_challenge_verify_button"
    }

    /**
     * Tags on the re-authentication dialog.
     *
     * Deliberately a group of its own rather than reusing [SignIn]: the re-authentication surface
     * and the flow behind it are composed at the same time while the dialog is open, so a shared
     * value would match two nodes and neither could be addressed.
     *
     * That reasoning covers the tags declared here, but not everything the re-authentication surface
     * can show. The default re-authentication bottom sheet re-enters the ordinary email flow to
     * collect a password, so [SignIn.EMAIL_FIELD] and its siblings appear *inside* the sheet, under
     * the same values they carry on the sign-in screen. Nothing collides today, because the sheet is
     * only raised from the post-sign-in surface and no sign-in screen is composed behind it — so
     * each value still resolves to one node. What a crawler cannot do is tell from the resource id
     * alone whether it is looking at the sign-in screen or at the re-authentication sheet; a test
     * that needs to distinguish them has to key off something else on the surface, and anything
     * that raises the sheet over a live sign-in screen would turn this into a real collision.
     */
    object Reauth {

        /** The password input. */
        const val PASSWORD_FIELD = "fui_reauth_password_field"

        /** The button that submits the password. */
        const val VERIFY_BUTTON = "fui_reauth_verify_button"

        /** The button that dismisses the dialog without re-authenticating. */
        const val DISMISS_BUTTON = "fui_reauth_dismiss_button"
    }
}
