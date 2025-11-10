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

package com.firebase.ui.auth.compose.mfa

import com.firebase.ui.auth.compose.configuration.MfaFactor
import com.firebase.ui.auth.compose.data.CountryData
import com.google.firebase.auth.MultiFactorInfo

/**
 * State class containing all the necessary information to render a custom UI for the
 * Multi-Factor Authentication (MFA) enrollment flow.
 *
 * This class is passed to the content slot of the MfaEnrollmentScreen composable, providing
 * access to the current step, user input values, callbacks for actions, and loading/error states.
 *
 * Use a `when` expression on [step] to determine which UI to render:
 *
 * ```kotlin
 * MfaEnrollmentScreen(user, config, onComplete, onSkip) { state ->
 *     when (state.step) {
 *         MfaEnrollmentStep.SelectFactor -> {
 *             // Render factor selection UI using state.availableFactors
 *         }
 *         MfaEnrollmentStep.ConfigureTotp -> {
 *             // Render TOTP setup UI using state.totpSecret and state.totpQrCodeUrl
 *         }
 *         MfaEnrollmentStep.VerifyFactor -> {
 *             // Render verification UI using state.verificationCode
 *         }
 *         // ... other steps
 *     }
 * }
 * ```
 *
 * @property step The current step in the enrollment flow. Use this to determine which UI to display.
 * @property isLoading `true` when an asynchronous operation (like generating a secret or verifying a code) is in progress. Use this to show loading indicators.
 * @property error An optional error message to display to the user. Will be `null` if there's no error.
 * @property onBackClick Callback to navigate to the previous step in the flow. Invoked when the user clicks a back button.
 *
 * @property availableFactors (Step: [MfaEnrollmentStep.SelectFactor]) A list of MFA factors the user can choose from (e.g., SMS, TOTP). Determined by [com.firebase.ui.auth.compose.configuration.MfaConfiguration.allowedFactors].
 * @property onFactorSelected (Step: [MfaEnrollmentStep.SelectFactor]) Callback invoked when the user selects an MFA factor. Receives the selected [MfaFactor].
 * @property onSkipClick (Step: [MfaEnrollmentStep.SelectFactor]) Callback for the "Skip" action. Will be `null` if MFA enrollment is required via [com.firebase.ui.auth.compose.configuration.MfaConfiguration.requireEnrollment].
 *
 * @property phoneNumber (Step: [MfaEnrollmentStep.ConfigureSms]) The current value of the phone number input field. Does not include country code prefix.
 * @property onPhoneNumberChange (Step: [MfaEnrollmentStep.ConfigureSms]) Callback invoked when the phone number input changes. Receives the new phone number string.
 * @property selectedCountry (Step: [MfaEnrollmentStep.ConfigureSms]) The currently selected country for phone number formatting. Contains dial code, country code, and flag.
 * @property onCountrySelected (Step: [MfaEnrollmentStep.ConfigureSms]) Callback invoked when the user selects a different country. Receives the new [CountryData].
 * @property onSendSmsCodeClick (Step: [MfaEnrollmentStep.ConfigureSms]) Callback to send the SMS verification code to the entered phone number.
 *
 * @property totpSecret (Step: [MfaEnrollmentStep.ConfigureTotp]) The TOTP secret containing the shared key and configuration. Use this to display the secret key or access the underlying Firebase TOTP secret.
 * @property totpQrCodeUrl (Step: [MfaEnrollmentStep.ConfigureTotp]) A URI that can be rendered as a QR code or used as a deep link to open authenticator apps. Generated via [TotpSecret.generateQrCodeUrl].
 * @property onContinueToVerifyClick (Step: [MfaEnrollmentStep.ConfigureTotp]) Callback to proceed to the verification step after the user has scanned the QR code or entered the secret.
 *
 * @property verificationCode (Step: [MfaEnrollmentStep.VerifyFactor]) The current value of the verification code input field. Should be a 6-digit string.
 * @property onVerificationCodeChange (Step: [MfaEnrollmentStep.VerifyFactor]) Callback invoked when the verification code input changes. Receives the new code string.
 * @property onVerifyClick (Step: [MfaEnrollmentStep.VerifyFactor]) Callback to verify the entered code and finalize MFA enrollment.
 * @property selectedFactor (Step: [MfaEnrollmentStep.VerifyFactor]) The MFA factor being verified (SMS or TOTP). Use this to customize UI messages.
 * @property resendTimer (Step: [MfaEnrollmentStep.VerifyFactor], SMS only) The number of seconds remaining before the "Resend" action is available. Will be 0 when resend is allowed.
 * @property onResendCodeClick (Step: [MfaEnrollmentStep.VerifyFactor], SMS only) Callback to resend the SMS verification code. Will be `null` for TOTP verification.
 *
 * @property recoveryCodes (Step: [MfaEnrollmentStep.ShowRecoveryCodes]) A list of one-time backup codes the user should save. Only present if [com.firebase.ui.auth.compose.configuration.MfaConfiguration.enableRecoveryCodes] is `true`.
 * @property onCodesSavedClick (Step: [MfaEnrollmentStep.ShowRecoveryCodes]) Callback invoked when the user confirms they have saved their recovery codes. Completes the enrollment flow.
 *
 * @since 10.0.0
 */
data class MfaEnrollmentContentState(
    /** The current step in the enrollment flow. Use this to determine which UI to display. */
    val step: MfaEnrollmentStep,

    /** `true` when an async operation is in progress. Use to show loading indicators. */
    val isLoading: Boolean = false,

    /** Optional error message to display. `null` if no error. */
    val error: String? = null,

    /** The last exception encountered during enrollment, if available. */
    val exception: Exception? = null,

    /** Callback to navigate to the previous step. */
    val onBackClick: () -> Unit = {},

    // SelectFactor step
    val availableFactors: List<MfaFactor> = emptyList(),

    val enrolledFactors: List<MultiFactorInfo> = emptyList(),

    val onFactorSelected: (MfaFactor) -> Unit = {},

    val onUnenrollFactor: (MultiFactorInfo) -> Unit = {},

    val onSkipClick: (() -> Unit)? = null,

    // ConfigureSms step
    val phoneNumber: String = "",

    val onPhoneNumberChange: (String) -> Unit = {},

    val selectedCountry: CountryData? = null,

    val onCountrySelected: (CountryData) -> Unit = {},

    val onSendSmsCodeClick: () -> Unit = {},

    // ConfigureTotp step
    val totpSecret: TotpSecret? = null,

    val totpQrCodeUrl: String? = null,

    val onContinueToVerifyClick: () -> Unit = {},

    // VerifyFactor step
    val verificationCode: String = "",

    val onVerificationCodeChange: (String) -> Unit = {},

    val onVerifyClick: () -> Unit = {},

    val selectedFactor: MfaFactor? = null,

    val resendTimer: Int = 0,

    val onResendCodeClick: (() -> Unit)? = null,

    // ShowRecoveryCodes step
    val recoveryCodes: List<String>? = null,

    val onCodesSavedClick: () -> Unit = {}
) {
    /**
     * Returns true if the current state is valid for the current step.
     *
     * This can be used to enable/disable action buttons in the UI.
     */
    val isValid: Boolean
        get() = when (step) {
            MfaEnrollmentStep.SelectFactor -> availableFactors.isNotEmpty()
            MfaEnrollmentStep.ConfigureSms -> phoneNumber.isNotBlank()
            MfaEnrollmentStep.ConfigureTotp -> totpSecret != null && totpQrCodeUrl != null
            MfaEnrollmentStep.VerifyFactor -> verificationCode.length == 6
            MfaEnrollmentStep.ShowRecoveryCodes -> !recoveryCodes.isNullOrEmpty()
        }

    /**
     * Returns true if there is an error in the current state.
     */
    val hasError: Boolean
        get() = !error.isNullOrBlank()

    /**
     * Returns true if the skip action is available (only for SelectFactor step when not required).
     */
    val canSkip: Boolean
        get() = step == MfaEnrollmentStep.SelectFactor && onSkipClick != null

    /**
     * Returns true if the back action is available (for all steps except SelectFactor).
     */
    val canGoBack: Boolean
        get() = step != MfaEnrollmentStep.SelectFactor
}
