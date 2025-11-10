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

/**
 * State class containing all the necessary information to render a custom UI for the
 * Multi-Factor Authentication (MFA) challenge flow during sign-in.
 *
 * This class is passed to the content slot of the MfaChallengeScreen composable, providing
 * access to the current factor, user input values, callbacks for actions, and loading/error states.
 *
 * The challenge flow is simpler than enrollment as the user has already configured their MFA:
 * 1. User enters their verification code (SMS or TOTP)
 * 2. System verifies the code and completes sign-in
 *
 * ```kotlin
 * MfaChallengeScreen(resolver, onSuccess, onCancel, onError) { state ->
 *     Column {
 *         Text("Enter your ${state.factorType} code")
 *         TextField(
 *             value = state.verificationCode,
 *             onValueChange = state.onVerificationCodeChange
 *         )
 *         if (state.canResend) {
 *             TextButton(onClick = state.onResendCodeClick) {
 *                 Text("Resend code")
 *             }
 *         }
 *         Button(
 *             onClick = state.onVerifyClick,
 *             enabled = !state.isLoading && state.isValid
 *         ) {
 *             Text("Verify")
 *         }
 *     }
 * }
 * ```
 *
 * @property factorType The type of MFA factor being challenged (SMS or TOTP)
 * @property maskedPhoneNumber For SMS factors, the masked phone number (e.g., "+1••••••890")
 * @property isLoading `true` when verification is in progress. Use this to show loading indicators.
 * @property error An optional error message to display to the user. Will be `null` if there's no error.
 * @property verificationCode The current value of the verification code input field.
 * @property resendTimer The number of seconds remaining before the "Resend" action is available. Will be 0 when resend is allowed.
 * @property onVerificationCodeChange Callback invoked when the verification code input changes.
 * @property onVerifyClick Callback to verify the entered code and complete sign-in.
 * @property onResendCodeClick For SMS only: Callback to resend the verification code. `null` for TOTP.
 * @property onCancelClick Callback to cancel the MFA challenge and return to sign-in.
 *
 * @since 10.0.0
 */
data class MfaChallengeContentState(
    /** The type of MFA factor being challenged (SMS or TOTP). */
    val factorType: MfaFactor,

    /** For SMS: the masked phone number. For TOTP: null. */
    val maskedPhoneNumber: String? = null,

    /** `true` when verification is in progress. Use to show loading indicators. */
    val isLoading: Boolean = false,

    /** Optional error message to display. `null` if no error. */
    val error: String? = null,

    /** The current value of the verification code input field. */
    val verificationCode: String = "",

    /** The number of seconds remaining before resend is available. 0 when ready. */
    val resendTimer: Int = 0,

    /** Callback invoked when the verification code input changes. */
    val onVerificationCodeChange: (String) -> Unit = {},

    /** Callback to verify the code and complete sign-in. */
    val onVerifyClick: () -> Unit = {},

    /** For SMS only: Callback to resend the code. `null` for TOTP. */
    val onResendCodeClick: (() -> Unit)? = null,

    /** Callback to cancel the challenge and return to sign-in. */
    val onCancelClick: () -> Unit = {}
) {
    /**
     * Returns true if the current state is valid for verification.
     * The code must be 6 digits long.
     */
    val isValid: Boolean
        get() = verificationCode.length == 6 && verificationCode.all { it.isDigit() }

    /**
     * Returns true if there is an error in the current state.
     */
    val hasError: Boolean
        get() = !error.isNullOrBlank()

    /**
     * Returns true if the resend action is available (SMS only).
     */
    val canResend: Boolean
        get() = factorType == MfaFactor.Sms && onResendCodeClick != null
}
