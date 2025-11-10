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

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorAssertion
import com.google.firebase.auth.TotpMultiFactorGenerator
import kotlinx.coroutines.tasks.await

/**
 * Handler for TOTP (Time-based One-Time Password) multi-factor authentication enrollment.
 *
 * This class manages the complete TOTP enrollment flow, including:
 * - Generating TOTP secrets
 * - Creating QR codes for authenticator apps
 * - Verifying TOTP codes with clock drift tolerance
 * - Finalizing enrollment with Firebase Authentication
 *
 * **Usage:**
 * ```kotlin
 * val handler = TotpEnrollmentHandler(auth, user)
 *
 * // Step 1: Generate a TOTP secret
 * val totpSecret = handler.generateSecret()
 *
 * // Step 2: Display QR code to user
 * val qrCodeUrl = totpSecret.generateQrCodeUrl(user.email, "My App")
 *
 * // Step 3: Verify the code entered by the user
 * val verificationCode = "123456" // From user input
 * handler.enrollWithVerificationCode(totpSecret, verificationCode, "My Authenticator")
 * ```
 *
 * @property auth The [FirebaseAuth] instance
 * @property user The [FirebaseUser] to enroll in TOTP MFA
 *
 * @since 10.0.0
 */
class TotpEnrollmentHandler(
    private val auth: FirebaseAuth,
    private val user: FirebaseUser
) {
    /**
     * Generates a new TOTP secret for the current user.
     *
     * This method initiates the TOTP enrollment process by creating a new secret that
     * can be shared with an authenticator app. The secret must be displayed to the user
     * (either as text or a QR code) so they can add it to their authenticator app.
     *
     * **Important:** The user must re-authenticate before calling this method if their
     * session is not recent. Use [FirebaseUser.reauthenticate] if needed.
     *
     * @return A [TotpSecret] containing the shared secret and configuration parameters
     * @throws Exception if the user needs to re-authenticate or if secret generation fails
     *
     * @see TotpSecret.generateQrCodeUrl
     * @see TotpSecret.openInOtpApp
     */
    suspend fun generateSecret(): TotpSecret {
        // Get the multi-factor session
        val multiFactorSession = user.multiFactor.session.await()

        // Generate the TOTP secret
        val firebaseTotpSecret = TotpMultiFactorGenerator.generateSecret(multiFactorSession).await()

        return TotpSecret.from(firebaseTotpSecret)
    }

    /**
     * Verifies a TOTP code and completes the enrollment process.
     *
     * This method creates a multi-factor assertion using the provided TOTP secret and
     * verification code, then enrolls the user in TOTP MFA with Firebase Authentication.
     *
     * The verification includes clock drift tolerance as configured in your Firebase project,
     * allowing codes from adjacent time windows to be accepted. This accommodates minor
     * time synchronization differences between the server and the user's device.
     *
     * @param totpSecret The [TotpSecret] generated in the first step
     * @param verificationCode The 6-digit code from the user's authenticator app
     * @param displayName Optional friendly name for this MFA factor (e.g., "Google Authenticator")
     * @throws Exception if the verification code is invalid or if enrollment fails
     *
     * @see generateSecret
     */
    suspend fun enrollWithVerificationCode(
        totpSecret: TotpSecret,
        verificationCode: String,
        displayName: String? = null
    ) {
        // Create the multi-factor assertion for enrollment
        val multiFactorAssertion: MultiFactorAssertion =
            TotpMultiFactorGenerator.getAssertionForEnrollment(
                totpSecret.getFirebaseTotpSecret(),
                verificationCode
            )

        // Enroll the user with the TOTP factor
        user.multiFactor.enroll(multiFactorAssertion, displayName).await()
    }

    /**
     * Validates that a verification code has the correct format for TOTP.
     *
     * This method performs basic client-side validation to ensure the code:
     * - Is not null or empty
     * - Contains only digits
     * - Has exactly 6 digits (the standard TOTP code length)
     *
     * **Note:** This does not verify the code against the TOTP secret. Use
     * [enrollWithVerificationCode] to perform actual verification with Firebase.
     *
     * @param code The verification code to validate
     * @return `true` if the code has a valid format, `false` otherwise
     */
    fun isValidCodeFormat(code: String): Boolean {
        return code.isNotBlank() &&
                code.length == 6 &&
                code.all { it.isDigit() }
    }

    companion object {
        /**
         * The standard length for TOTP verification codes.
         */
        const val TOTP_CODE_LENGTH = 6

        /**
         * The standard time interval in seconds for TOTP codes.
         */
        const val TOTP_TIME_INTERVAL_SECONDS = 30

        /**
         * The Firebase factor ID for TOTP multi-factor authentication.
         */
        const val FACTOR_ID = TotpMultiFactorGenerator.FACTOR_ID
    }
}
