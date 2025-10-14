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

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorAssertion
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Handler for SMS multi-factor authentication enrollment.
 *
 * This class manages the complete SMS enrollment flow, including:
 * - Sending SMS verification codes to phone numbers
 * - Resending codes with timer support
 * - Verifying SMS codes entered by users
 * - Finalizing enrollment with Firebase Authentication
 *
 * **Usage:**
 * ```kotlin
 * val handler = SmsEnrollmentHandler(auth, user, activity)
 *
 * // Step 1: Send verification code
 * val session = handler.sendVerificationCode("+1234567890")
 *
 * // Step 2: Display masked phone number and wait for user input
 * val masked = session.getMaskedPhoneNumber()
 *
 * // Step 3: If needed, resend code after timer expires
 * val newSession = handler.resendVerificationCode(session)
 *
 * // Step 4: Verify the code entered by the user
 * val verificationCode = "123456" // From user input
 * handler.enrollWithVerificationCode(session, verificationCode, "My Phone")
 * ```
 *
 * @property auth The [FirebaseAuth] instance
 * @property user The [FirebaseUser] to enroll in SMS MFA
 * @property activity The [Activity] context required for phone authentication
 *
 * @since 10.0.0
 */
class SmsEnrollmentHandler(
    private val auth: FirebaseAuth,
    private val user: FirebaseUser,
    private val activity: Activity
) {
    /**
     * Sends an SMS verification code to the specified phone number.
     *
     * This method initiates the SMS enrollment process by sending a verification code
     * to the provided phone number. The code will be sent via SMS and should be
     * displayed to the user for entry.
     *
     * **Important:** The user must re-authenticate before calling this method if their
     * session is not recent. Use [FirebaseUser.reauthenticate] if needed.
     *
     * @param phoneNumber The phone number in E.164 format (e.g., "+1234567890")
     * @return An [SmsEnrollmentSession] containing the verification ID and metadata
     * @throws Exception if the user needs to re-authenticate, phone number is invalid,
     *                   or SMS sending fails
     *
     * @see resendVerificationCode
     * @see SmsEnrollmentSession.getMaskedPhoneNumber
     */
    suspend fun sendVerificationCode(phoneNumber: String): SmsEnrollmentSession {
        require(isValidPhoneNumber(phoneNumber)) {
            "Phone number must be in E.164 format (e.g., +1234567890)"
        }

        // Get the multi-factor session
        val multiFactorSession = user.multiFactor.session.await()

        return suspendCoroutine { continuation ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-verification succeeded - create session with credential
                    // Note: Auto-verification rarely happens for MFA enrollment
                    continuation.resume(
                        SmsEnrollmentSession(
                            verificationId = credential.smsCode ?: "",
                            phoneNumber = phoneNumber,
                            forceResendingToken = null,
                            sentAt = System.currentTimeMillis(),
                            autoVerifiedCredential = credential
                        )
                    )
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    continuation.resumeWithException(e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    continuation.resume(
                        SmsEnrollmentSession(
                            verificationId = verificationId,
                            phoneNumber = phoneNumber,
                            forceResendingToken = token,
                            sentAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .setMultiFactorSession(multiFactorSession)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    /**
     * Resends the SMS verification code to the phone number.
     *
     * This method uses the force resending token from the original session to
     * explicitly request a new SMS code. This should only be called after the
     * [RESEND_DELAY_SECONDS] has elapsed to respect rate limits.
     *
     * @param session The original [SmsEnrollmentSession] from [sendVerificationCode]
     * @return A new [SmsEnrollmentSession] with updated verification ID and timestamp
     * @throws Exception if resending fails or if the session doesn't have a resend token
     *
     * @see sendVerificationCode
     */
    suspend fun resendVerificationCode(session: SmsEnrollmentSession): SmsEnrollmentSession {
        require(session.forceResendingToken != null) {
            "Cannot resend code without a force resending token"
        }

        // Get a fresh multi-factor session
        val multiFactorSession = user.multiFactor.session.await()

        return suspendCoroutine { continuation ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    continuation.resume(
                        SmsEnrollmentSession(
                            verificationId = credential.smsCode ?: session.verificationId,
                            phoneNumber = session.phoneNumber,
                            forceResendingToken = session.forceResendingToken,
                            sentAt = System.currentTimeMillis(),
                            autoVerifiedCredential = credential
                        )
                    )
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    continuation.resumeWithException(e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    continuation.resume(
                        SmsEnrollmentSession(
                            verificationId = verificationId,
                            phoneNumber = session.phoneNumber,
                            forceResendingToken = token,
                            sentAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(session.phoneNumber)
                .setTimeout(VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .setMultiFactorSession(multiFactorSession)
                .setForceResendingToken(session.forceResendingToken)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    /**
     * Verifies an SMS code and completes the enrollment process.
     *
     * This method creates a multi-factor assertion using the provided session and
     * verification code, then enrolls the user in SMS MFA with Firebase Authentication.
     *
     * @param session The [SmsEnrollmentSession] from [sendVerificationCode] or [resendVerificationCode]
     * @param verificationCode The 6-digit code from the SMS message
     * @param displayName Optional friendly name for this MFA factor (e.g., "My Phone")
     * @throws Exception if the verification code is invalid or if enrollment fails
     *
     * @see sendVerificationCode
     * @see resendVerificationCode
     */
    suspend fun enrollWithVerificationCode(
        session: SmsEnrollmentSession,
        verificationCode: String,
        displayName: String? = null
    ) {
        require(isValidCodeFormat(verificationCode)) {
            "Verification code must be 6 digits"
        }

        // Use auto-verified credential if available, otherwise create from code
        val credential = session.autoVerifiedCredential
            ?: PhoneAuthProvider.getCredential(session.verificationId, verificationCode)

        // Create the multi-factor assertion for enrollment
        val multiFactorAssertion: MultiFactorAssertion =
            PhoneMultiFactorGenerator.getAssertion(credential)

        // Enroll the user with the SMS factor
        user.multiFactor.enroll(multiFactorAssertion, displayName).await()
    }

    /**
     * Validates that a verification code has the correct format for SMS.
     *
     * This method performs basic client-side validation to ensure the code:
     * - Is not null or empty
     * - Contains only digits
     * - Has exactly 6 digits (the standard SMS code length)
     *
     * **Note:** This does not verify the code against the server. Use
     * [enrollWithVerificationCode] to perform actual verification with Firebase.
     *
     * @param code The verification code to validate
     * @return `true` if the code has a valid format, `false` otherwise
     */
    fun isValidCodeFormat(code: String): Boolean {
        return code.isNotBlank() &&
                code.length == SMS_CODE_LENGTH &&
                code.all { it.isDigit() }
    }

    /**
     * Validates that a phone number is in the correct E.164 format.
     *
     * E.164 format requirements:
     * - Starts with "+"
     * - Followed by 1-15 digits
     * - No spaces, hyphens, or other characters
     * - Minimum 4 digits total (country code + subscriber number)
     *
     * Examples of valid numbers:
     * - +1234567890 (US)
     * - +447911123456 (UK)
     * - +33612345678 (France)
     *
     * @param phoneNumber The phone number to validate
     * @return `true` if the phone number is in E.164 format, `false` otherwise
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.matches(Regex("^\\+[1-9]\\d{3,14}$"))
    }

    companion object {
        /**
         * The standard length for SMS verification codes.
         */
        const val SMS_CODE_LENGTH = 6

        /**
         * The verification timeout in seconds for phone authentication.
         * This is how long Firebase will wait for auto-verification before
         * falling back to manual code entry.
         */
        const val VERIFICATION_TIMEOUT_SECONDS = 60L

        /**
         * The recommended delay in seconds before allowing code resend.
         * This prevents users from spamming the resend functionality and
         * respects carrier rate limits.
         */
        const val RESEND_DELAY_SECONDS = 30

        /**
         * The Firebase factor ID for SMS multi-factor authentication.
         */
        const val FACTOR_ID = PhoneMultiFactorGenerator.FACTOR_ID
    }
}

/**
 * Represents an active SMS enrollment session with verification state.
 *
 * This class holds all the information needed to complete an SMS enrollment,
 * including the verification ID, phone number, and resend token.
 *
 * @property verificationId The verification ID from Firebase
 * @property phoneNumber The phone number being verified in E.164 format
 * @property forceResendingToken Optional token for resending the SMS code
 * @property sentAt Timestamp in milliseconds when the code was sent
 * @property autoVerifiedCredential Optional credential if auto-verification succeeded
 *
 * @since 10.0.0
 */
data class SmsEnrollmentSession(
    val verificationId: String,
    val phoneNumber: String,
    val forceResendingToken: PhoneAuthProvider.ForceResendingToken?,
    val sentAt: Long,
    val autoVerifiedCredential: PhoneAuthCredential? = null
) {
    /**
     * Returns a masked version of the phone number for display purposes.
     *
     * Masks the middle digits of the phone number while keeping the country code
     * and last few digits visible for user confirmation.
     *
     * Examples:
     * - "+1234567890" → "+1••••••890"
     * - "+447911123456" → "+44•••••••456"
     *
     * @return The masked phone number string
     */
    fun getMaskedPhoneNumber(): String {
        return maskPhoneNumber(phoneNumber)
    }

    /**
     * Checks if the resend delay has elapsed since the code was sent.
     *
     * @param delaySec The delay in seconds (default: [SmsEnrollmentHandler.RESEND_DELAY_SECONDS])
     * @return `true` if enough time has passed to allow resending
     */
    fun canResend(delaySec: Int = SmsEnrollmentHandler.RESEND_DELAY_SECONDS): Boolean {
        val elapsed = (System.currentTimeMillis() - sentAt) / 1000
        return elapsed >= delaySec
    }

    /**
     * Returns the remaining seconds until resend is allowed.
     *
     * @param delaySec The delay in seconds (default: [SmsEnrollmentHandler.RESEND_DELAY_SECONDS])
     * @return The number of seconds remaining, or 0 if resend is already allowed
     */
    fun getRemainingResendSeconds(delaySec: Int = SmsEnrollmentHandler.RESEND_DELAY_SECONDS): Int {
        val elapsed = (System.currentTimeMillis() - sentAt) / 1000
        return maxOf(0, delaySec - elapsed.toInt())
    }
}

/**
 * Masks the middle digits of a phone number for privacy.
 *
 * The function keeps the country code (first 1-3 characters after +) and
 * the last 2-4 digits visible, masking everything in between with bullets.
 * Longer phone numbers show more last digits for better user confirmation.
 *
 * Examples:
 * - "+1234567890" → "+1••••••890" (11 chars, last 3 digits)
 * - "+447911123456" → "+44•••••••456" (13 chars, last 3 digits)
 * - "+33612345678" → "+33•••••••678" (12 chars, last 3 digits)
 * - "+8861234567890" → "+88••••••••7890" (14+ chars, last 4 digits)
 *
 * @param phoneNumber The phone number to mask in E.164 format
 * @return The masked phone number string
 */
fun maskPhoneNumber(phoneNumber: String): String {
    if (!phoneNumber.startsWith("+") || phoneNumber.length < 8) {
        return phoneNumber
    }

    // Determine country code length (typically 1-3 digits after +)
    val digitsOnly = phoneNumber.substring(1) // Remove +
    val countryCodeLength = when {
        digitsOnly.length > 10 -> 2 // Likely 2-digit country code
        digitsOnly[0] == '1' -> 1 // North America
        else -> 2 // Most other countries
    }

    val countryCode = phoneNumber.substring(0, countryCodeLength + 1) // Include +
    // Keep last 3-4 digits visible, with longer numbers showing more
    val lastDigitsCount = when {
        phoneNumber.length >= 14 -> 4 // Long numbers show 4 digits
        phoneNumber.length >= 11 -> 3 // Medium numbers show 3 digits
        else -> 2 // Short numbers show 2 digits
    }
    val lastDigits = phoneNumber.takeLast(lastDigitsCount)
    val maskedLength = phoneNumber.length - countryCode.length - lastDigitsCount

    return "$countryCode${"•".repeat(maskedLength)}$lastDigits"
}
